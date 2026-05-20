package com.micreta.app.domain.model

data class CalendarEventInfo(
    val title: String,
    val startMs: Long,
    val location: String?
)
