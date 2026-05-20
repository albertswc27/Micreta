# Micreta - Guion de pruebas en parado

Documento para grabar una revision funcional de Micreta con el coche parado.
El objetivo es narrar en audio todas las acciones, lo que se ve en pantalla,
lo que se oye, lo que funciona, lo que genera dudas y lo que queda pendiente
por hardware.

## Contexto de la prueba

Condiciones actuales:

- Prueba con el coche parado y sin iniciar marcha.
- Sin OBD2 ELM327 disponible.
- Sin pantalla o modulo fisico externo.
- Sin cargador de mechero a USB-C.
- Validacion centrada en app Android, modo demo, voz, permisos, Waze, musica,
  ajustes, mantenimiento, repostajes, parking, debug y salud del sistema.

Clasifica cada bloque como:

- OK: funciona como esperabas.
- Duda: funciona, pero hay algo poco claro.
- Bug: falla, se bloquea, no responde o hace algo inesperado.
- Pendiente hardware: no se puede validar todavia sin accesorio o movimiento real.

## Preparacion

Antes de grabar:

1. Instala la APK desde el README de GitHub.
2. Abre la app una vez y confirma que no crashea.
3. Ten Waze instalado si quieres probar navegacion.
4. Ten una app de musica instalada, por ejemplo Spotify o YouTube Music.
5. No pruebes una llamada real a emergencias. Para SOS, configura un numero de
   prueba y no pulses el boton final de llamar.
6. Si aparece un permiso, narra que permiso pide, en que momento y si tiene
   sentido.

Frase inicial sugerida:

> Empiezo revision funcional de Micreta v0.2.0 con el coche parado. No tengo
> todavia OBD2, pantalla externa ni cargador de mechero USB-C, asi que voy a
> validar todo lo posible desde la app, modo demo, voz, permisos, navegacion,
> musica, mantenimiento, viajes, parking, debug y salud del sistema.

## 1. Instalacion y primera apertura

Accion:

1. Abre Micreta.
2. Observa la pantalla inicial.
3. Comprueba si pide permisos inmediatamente.

Di en voz alta:

> Abro Micreta por primera vez. Veo si aparece el avatar, el estado de la app,
> los botones principales y si me deja explorar sin pedir todos los permisos.

Esperado:

- Home visible con avatar, nombre de Micreta, estado y botones principales.
- No deberia pedir todos los permisos al inicio.
- Avatar animado sin parpadeos raros.
- Sin cierre inesperado.

Marca resultado: OK / Duda / Bug.

## 2. Pantalla principal

Accion:

1. Revisa los botones de Home.
2. Lee en voz alta los botones que ves.

Elementos a revisar:

- Activar Micreta / Salir del modo conduccion.
- Preguntar destino.
- Destino.
- Coche.
- Historial.
- Aparcado.
- Repostajes.
- Mantenimiento.

Di:

> Estoy en Home. Reviso si los botones son claros para usar en parado y si
> alguno podria confundirme durante la conduccion.

Esperado:

- Destino no abre Waze vacio.
- Coche abre estado del vehiculo.
- Historial, aparcado, repostajes y mantenimiento abren su pantalla.
- Textos legibles y sin cortes.

## 3. Modo conduccion manual

Accion:

1. Pulsa Activar Micreta.
2. Observa si cambia el estado.
3. Mira si aparece notificacion persistente.
4. Escucha si Micreta habla.
5. Pulsa Salir del modo conduccion.

Di:

> Activo modo conduccion manualmente. Compruebo notificacion persistente,
> cambio de estado, saludo y que el modo se pueda detener sin quedarse colgado.

Esperado:

- El estado cambia a modo conduccion o conectado.
- Hay notificacion persistente.
- No se duplican voces.
- Al salir, el servicio se detiene.
- Puede dar un resumen de viaje corto o indicar parada.

Marca resultado: OK / Duda / Bug.

## 4. Voz y permisos

Accion:

1. Pulsa Preguntar destino.
2. Si pide microfono, concede permiso.
3. Prueba frases de voz una por una.

Frases:

- llevame al taller
- pon musica
- pausa la musica
- siguiente cancion
- diagnostico
- que tiempo hace
- que tengo hoy
- si
- no
- esto no deberia entenderlo

Por cada frase, di:

> He dicho: [frase]. La app ha entendido: [lo que aparece]. Resultado visible
> o sonoro: [descripcion]. Lo marco como OK, duda o bug.

Esperado:

- Microfono se pide solo al usar voz.
- La app muestra lo que ha oido.
- Los comandos conocidos se clasifican.
- Las frases desconocidas no se inventan y piden repetir.

## 5. Navegacion y Waze

Accion:

1. En Ajustes, revisa o crea favoritos.
2. Crea favoritos de prueba si hace falta:
   - Casa.
   - Taller.
   - UAB, con alias uni o universidad.
3. Vuelve a voz.

Frases:

- llevame al taller
- vamos a la UAB
- a casa
- volver
- donde he aparcado

Di:

> Pruebo navegacion sin mover el coche. Solo verifico que Micreta resuelve el
> destino y abre Waze o el fallback, no inicio conduccion real.

Esperado:

- Waze se abre con destino cuando hay destino.
- Si no hay parking guardado, lo comunica de forma clara.
- No abre Waze con destino vacio.

## 6. OBD sin adaptador

Accion:

1. Ve a Ajustes.
2. Activa Modo demo (mock OBD2).
3. Ve a Coche.
4. Pulsa Lectura unica.
5. Pulsa Monitorizar.
6. Espera 10 o 15 segundos.
7. Pulsa Detener monitorizacion.

Di:

> No tengo OBD2 todavia, asi que activo modo demo. Compruebo que la app deja
> claro que los datos son simulados y no reales.

Esperado:

- Banner demo o mock visible.
- Muestra datos simulados.
- No dice OBD real.
- No obliga a tener adaptador real en modo demo.

Marca:

- Modo demo: OK / Duda / Bug.
- OBD real: Pendiente hardware.

## 7. Viajes, eco-score y sensores en parado

Accion:

1. Activa modo conduccion durante un minuto.
2. Deja el movil quieto.
3. Si quieres, mueve suavemente el movil con la mano, sin golpes.
4. Sal del modo conduccion.
5. Abre Historial.

Di:

> Pruebo registro de viaje en parado. No espero distancia real, pero si que no
> crashee y que gestione bien una sesion corta.

Esperado:

- Puede aparecer viaje con distancia cero o muy baja.
- No deberia inventar kilometros.
- El resumen no debe sonar absurdo.
- Eco-score puede ser parcial.

Marca:

- Historial de viajes: OK / Duda / Bug.
- Eco-score: OK / Duda / Bug / Parcial.
- Acelerometro: Parcial en parado.

## 8. Repostajes

Accion:

1. Abre Repostajes.
2. Introduce datos ficticios:
   - Litros: 35.
   - Coste total: 55.
   - Cuentakilometros: 123456.
   - Gasolinera: Prueba.
3. Guarda.
4. Opcional: crea un segundo repostaje con kilometros superiores.

Di:

> Pruebo log de repostajes con datos ficticios. Compruebo campos, calculos y
> listado historico.

Esperado:

- Guarda la entrada.
- Calcula precio por litro.
- Calcula consumo medio solo cuando hay datos suficientes.

## 9. Mantenimiento

Accion:

1. Abre Mantenimiento.
2. Introduce kilometros actuales.
3. Revisa aceite, ITV, seguro u otros recordatorios.

Di:

> Reviso recordatorios de mantenimiento. Compruebo si se entienden los estados
> y si el kilometraje actual actualiza la informacion.

Esperado:

- Recordatorios visibles.
- Estados claros.
- No se bloquea con campos vacios o numeros de prueba.

## 10. Parking

Accion:

1. Abre Aparcado.
2. Observa si hay ubicacion guardada.
3. Si hay ubicacion, prueba abrir mapa o compartir.

Di:

> Reviso memoria de aparcamiento. Como no he hecho desconexion Bluetooth real
> del coche, compruebo si hay ubicacion guardada o si lo comunica bien.

Esperado:

- Si no hay parking, mensaje claro.
- Si hay parking, muestra latitud y longitud.
- Abrir mapa o compartir no crashea.

Pendiente:

- Guardado automatico al desconectar Bluetooth real del coche.

## 11. Ajustes

Accion:

Recorre Ajustes completo y narra cada bloque.

Revisar:

- Nombre del asistente.
- Nombre del coche.
- Personalidad.
- Bluetooth coche.
- Activar al conectar Bluetooth.
- Activar al conectar cargador.
- Activar por velocidad GPS.
- Modo demo.
- App de musica.
- Audio ducking.
- Reanudar musica.
- Aviso de exceso de velocidad.
- No molestar estricto.
- Telefono SOS.
- Contacto ETA.
- Modo nocturno.
- Registrar viajes.
- Comandos personalizados.
- Favoritos.

Di:

> Reviso Ajustes. Compruebo si los cambios se guardan al instante, si los
> textos son claros y si los permisos se piden solo cuando tienen sentido.

## 12. Bluetooth

Accion:

1. Ajustes.
2. Configurar Bluetooth.
3. Si aparece el coche emparejado, seleccionalo como Mi coche.
4. Si no aparece, dilo en voz alta.

Di:

> Reviso configuracion Bluetooth. No tengo OBD2 todavia. Si el coche aparece
> emparejado, lo selecciono. Si no aparece, lo marco pendiente.

Esperado:

- Si Bluetooth esta apagado, mensaje claro.
- Si falta permiso, lo pide.
- Lista dispositivos emparejados.
- Permite elegir coche y OBD por separado.

Pendiente:

- Activacion automatica real al conectar Bluetooth del coche.
- Adaptador OBD2 real.

## 13. Cargador

Como no tienes cargador de mechero USB-C:

Di:

> La activacion por cargador queda pendiente de accesorio de coche. Como prueba
> parcial, puedo enchufar el movil a un cargador normal, pero eso no valida el
> contexto coche.

Esperado:

- El toggle se guarda.
- Activacion real en coche queda pendiente.

## 14. Activacion por velocidad GPS

Accion:

1. Activa Activar por velocidad GPS.
2. Concede ubicacion si la pide.
3. Desactiva la opcion si no quieres dejarla activa.

Di:

> Pruebo solo el flujo de permiso y configuracion de activacion por velocidad
> GPS. No valido el disparo real porque estoy parado.

Esperado:

- Pide ubicacion al activar la funcion.
- No arranca modo conduccion estando parado.
- Si Android exige permisos adicionales, anotarlo.

Pendiente:

- Prueba real a mas de 15 km/h durante unos 12 segundos.

## 15. Musica y audio ducking

Accion:

1. Configura la app musical en Ajustes.
2. Reproduce musica.
3. Haz hablar a Micreta con voz o desde Debug.

Frases:

- pon musica
- pausa la musica
- siguiente cancion
- mas volumen
- baja el volumen

Di:

> Pruebo control multimedia y ducking. Con musica sonando, hago hablar a
> Micreta y escucho si baja el volumen y vuelve despues.

Esperado:

- Abre la app musical configurada.
- Play, pausa y siguiente responden si Android lo permite.
- La musica baja cuando habla Micreta y vuelve al terminar.

## 16. SOS seguro

Antes:

1. En Ajustes, cambia Telefono SOS a un numero de prueba.
2. No uses una llamada real a emergencias.

Accion:

1. Di llama a emergencias.
2. Durante countdown, di cancela.
3. Repite dejando terminar countdown, pero no pulses llamar.

Di:

> Pruebo SOS sin hacer llamada real. Compruebo countdown, cancelacion y apertura
> del marcador con ACTION_DIAL.

Esperado:

- Countdown visible o sonoro.
- Cancelar funciona.
- Si termina, abre el marcador.
- No llama automaticamente.

## 17. Calendario y tiempo

Frases:

- que tengo hoy
- que tiempo hace

Di:

> Pruebo permisos de calendario y ubicacion bajo demanda. Compruebo si la
> respuesta es util y si falla de forma clara.

Esperado:

- Calendario pide permiso solo al usar agenda.
- Tiempo pide ubicacion si hace falta.
- Si no hay eventos o ubicacion, mensaje claro.

## 18. Comandos personalizados

Accion:

1. Ajustes.
2. Comandos personalizados.
3. Crea:
   - Frase: modo prueba.
   - Accion: SPEAK.
   - Payload: Comando personalizado funcionando.
4. Vuelve a voz y di modo prueba.

Di:

> Creo un comando personalizado y verifico que Micreta lo reconoce y ejecuta.

Esperado:

- Se guarda.
- Al decir la frase, ejecuta la accion.

## 19. Salud del sistema

Accion:

1. Ajustes.
2. Salud del sistema.

Di:

> Reviso salud del sistema. Leo permisos, Bluetooth, OBD, Waze, musica, modo
> demo y estado general.

Esperado:

- Refleja permisos reales.
- OBD aparece como mock, no disponible o desconectado si no hay adaptador.
- Waze instalado o no instalado se detecta correctamente.

## 20. Debug

Accion:

1. Ajustes.
2. Debug.
3. Pulsa Probar TTS.
4. Revisa Eventos recientes.
5. Pulsa Limpiar logs.

Di:

> Reviso Debug. Pruebo TTS, compruebo logs recientes y limpio eventos.

Esperado:

- TTS habla.
- Logs muestran acciones recientes.
- Limpiar logs funciona.

## Cierre de la grabacion

Frase final sugerida:

> Resumen final. Lo que considero OK es: [lista]. Lo que me genera dudas es:
> [lista]. Bugs detectados: [lista]. Queda pendiente por hardware: OBD2 real,
> pantalla externa, cargador de mechero USB-C, desconexion Bluetooth real del
> coche y activacion GPS en movimiento. Mis tres prioridades para la siguiente
> version son: [lista].

## Plantilla final de notas

```text
OK:
- 

Dudas:
- 

Bugs:
- 

Pendiente hardware:
- OBD2 ELM327 real
- Pantalla externa / modulo fisico
- Cargador de mechero USB-C
- Activacion por velocidad real
- Conexion/desconexion Bluetooth real del coche

Mejoras UX:
- 

Prioridades propuestas:
1. 
2. 
3. 
```

