package com.micreta.app.data.obd

import android.content.Context
import com.micreta.app.core.logging.EventLogger
import com.micreta.app.domain.model.ObdAlert
import com.micreta.app.domain.model.VehicleStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single entry point for OBD data — both mock and real.
 *
 * **OBD is on-demand only** (v0.2.0 feedback): connecting and polling the
 * ELM327 is expensive (BT socket, ECU latency) and Albert wants telemetry
 * to surface only when explicitly asked.
 *
 * Three usage patterns are supported:
 *  - [snapshot]        — one-shot read. Connects, polls every PID once, disconnects.
 *                        Used by the "diagnóstico" voice command.
 *  - [startContinuous] — long-lived polling at 1 Hz. Used by the Vehicle Status
 *                        screen while it's visible, or by the user's explicit
 *                        "monitoriza el coche" command.
 *  - [startMock]       — emits MockObdSource data in demo mode.
 *
 * Two top-level flows the UI/services can collect:
 *  - [status] : latest [VehicleStatus] snapshot
 *  - [alerts] : derived alerts whenever a threshold is crossed
 *
 * Polling cadence is intentionally slow (1 Hz) — ELM327 clones can't keep up
 * with much more, and faster polling doesn't add value while driving.
 */
class ObdRepository(private val context: Context) {

    private val _status = MutableStateFlow(VehicleStatus())
    val status: StateFlow<VehicleStatus> = _status

    private val _alerts = MutableSharedFlow<ObdAlert>(extraBufferCapacity = 8)
    val alerts: SharedFlow<ObdAlert> = _alerts.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _source = MutableStateFlow(VehicleStatus.Source.NONE)
    val source: StateFlow<VehicleStatus.Source> = _source

    private val _isContinuous = MutableStateFlow(false)
    val isContinuous: StateFlow<Boolean> = _isContinuous

    private val mock = MockObdSource()
    private val elm = Elm327Client(context)

    private var pollJob: Job? = null
    private var scope: CoroutineScope? = null
    private var lastDtcSnapshot: List<String> = emptyList()

    // ---- Mock mode (demo) -------------------------------------------------

    fun startMock() {
        stop()
        EventLogger.info(TAG, "Starting OBD mock source.")
        _source.value = VehicleStatus.Source.MOCK
        _isConnected.value = true
        _isContinuous.value = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        pollJob = scope!!.launch {
            while (true) {
                val snapshot = mock.nextTick()
                emit(snapshot)
                delay(1000)
            }
        }
    }

    // ---- Continuous real polling (Vehicle Status screen) -----------------

    fun startContinuous(macAddress: String) {
        stop()
        EventLogger.info(TAG, "Starting OBD continuous polling on $macAddress.")
        _source.value = VehicleStatus.Source.OBD_BLUETOOTH
        _isContinuous.value = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        pollJob = scope!!.launch {
            val connectRes = elm.connect(macAddress)
            if (connectRes.isFailure) {
                EventLogger.error(TAG, "OBD connect failed: ${connectRes.exceptionOrNull()?.message}")
                _isConnected.value = false
                _isContinuous.value = false
                _alerts.tryEmit(ObdAlert.ObdConnectionLost)
                return@launch
            }
            _isConnected.value = true

            while (elm.isConnected) {
                val snapshot = pollOnce()
                emit(snapshot)
                delay(1000)
            }
            _isConnected.value = false
            _isContinuous.value = false
            _alerts.tryEmit(ObdAlert.ObdConnectionLost)
        }
    }

    // Backwards-compatible alias kept so older call sites keep compiling.
    @Deprecated("Use startContinuous() — name made the on-demand semantics clearer.", ReplaceWith("startContinuous(macAddress)"))
    fun startReal(macAddress: String) = startContinuous(macAddress)

    // ---- One-shot snapshot (voice "diagnóstico") --------------------------

    /**
     * Connect once, read every PID, disconnect. Designed for the
     * "diagnóstico" voice command — no lingering BT socket.
     *
     * The result is also pushed into [status] so the rest of the app (voice,
     * widgets) sees the same snapshot.
     *
     * @param macAddress null → use mock data (useful in demo mode)
     */
    suspend fun snapshot(macAddress: String?): VehicleStatus = withContext(Dispatchers.IO) {
        if (macAddress.isNullOrBlank()) {
            EventLogger.info(TAG, "OBD snapshot (mock fallback, no MAC configured).")
            val s = mock.nextTick()
            emit(s)
            return@withContext s
        }
        EventLogger.info(TAG, "OBD snapshot (one-shot) on $macAddress.")
        _source.value = VehicleStatus.Source.OBD_BLUETOOTH
        val connectRes = elm.connect(macAddress)
        if (connectRes.isFailure) {
            EventLogger.warn(TAG, "OBD snapshot connect failed: ${connectRes.exceptionOrNull()?.message}")
            _alerts.tryEmit(ObdAlert.ObdConnectionLost)
            return@withContext VehicleStatus(source = VehicleStatus.Source.NONE)
        }
        _isConnected.value = true
        val s = try {
            pollOnce()
        } finally {
            elm.close()
            _isConnected.value = false
        }
        emit(s)
        s
    }

    // ---- Stop -------------------------------------------------------------

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        scope?.cancel()
        scope = null
        elm.close()
        _isConnected.value = false
        _isContinuous.value = false
        _source.value = VehicleStatus.Source.NONE
        EventLogger.info(TAG, "OBD repository stopped.")
    }

    private suspend fun pollOnce(): VehicleStatus {
        val rpm = elm.query("010C").getOrNull()?.let(ObdPidParser::parseRpm)
        val speed = elm.query("010D").getOrNull()?.let(ObdPidParser::parseSpeed)
        val coolant = elm.query("0105").getOrNull()?.let(ObdPidParser::parseCoolantTemp)
        val load = elm.query("0104").getOrNull()?.let(ObdPidParser::parseEngineLoad)
        val throttle = elm.query("0111").getOrNull()?.let(ObdPidParser::parseThrottle)
        val fuel = elm.query("012F").getOrNull()?.let(ObdPidParser::parseFuelLevel)
        val iat = elm.query("010F").getOrNull()?.let(ObdPidParser::parseIntakeAirTemp)
        val volts = elm.query("0142").getOrNull()?.let(ObdPidParser::parseControlModuleVoltage)
        val dtcs = elm.query("03").getOrNull()?.let(ObdPidParser::parseDtcCodes).orEmpty()

        return VehicleStatus(
            rpm = rpm,
            speedKmh = speed,
            coolantTempC = coolant,
            engineLoadPct = load,
            throttlePct = throttle,
            fuelLevelPct = fuel,
            intakeAirTempC = iat,
            batteryVoltage = volts,
            dtcCodes = dtcs,
            estimatedRangeKm = null, // K13 doesn't expose this directly via standard PIDs
            odometerKm = null,       // odometer not in OBD-II standard PIDs
            source = VehicleStatus.Source.OBD_BLUETOOTH
        )
    }

    private fun emit(status: VehicleStatus) {
        _status.value = status
        deriveAlerts(status)
    }

    /**
     * Thresholds chosen for the K13:
     *  - coolant > 105°C is the danger zone (gauge red on cluster ≈ 110)
     *  - battery < 11.8V usually means alternator not charging or battery dying
     *  - fuel < 10% is conventionally where the warning light comes on
     *  - any new DTC count > previous triggers an alert
     */
    private fun deriveAlerts(s: VehicleStatus) {
        s.coolantTempC?.let { if (it >= 105) _alerts.tryEmit(ObdAlert.HighCoolantTemp(it)) }
        s.batteryVoltage?.let { if (it <= 11.8) _alerts.tryEmit(ObdAlert.LowBattery(it)) }
        s.fuelLevelPct?.let { if (it <= 10) _alerts.tryEmit(ObdAlert.LowFuel(it)) }
        if (s.dtcCodes.isNotEmpty() && s.dtcCodes != lastDtcSnapshot) {
            _alerts.tryEmit(ObdAlert.DtcDetected(s.dtcCodes))
        }
        lastDtcSnapshot = s.dtcCodes
    }

    companion object {
        private const val TAG = "ObdRepo"
    }
}
