# Plan progresivo de seguridad y consolidación del API Gateway

## Alcance

Este programa cubre Sentinel API, CBT API, Super Admin API y `helios-gateway` dentro de HELIOS Platform. El repositorio remoto del gateway conserva temporalmente su nombre legacy hasta una etapa posterior. La línea base recuperable es la etiqueta local anotada `stable-pre-security-hardening-2026-07-13` en cada repositorio.

La etiqueta representa la última versión funcional previa al endurecimiento. No representa una certificación de seguridad.

## Topología de transición confirmada

- `us-west-1`: EC2 legacy y RDS MySQL compartida por las tres APIs. Esta plataforma continúa atendiendo producción y no se modifica durante la construcción paralela.
- `us-east-1`: nueva EC2 HELIOS. Actualmente sólo ejecuta el gateway; Sentinel, CBT y Super Admin aún no están desplegados allí.
- Runtime nuevo del gateway: `helios-gateway`. El repositorio, DNS, rutas y orígenes CORS legacy conservan temporalmente sus nombres para una migración independiente.
- Mapeo de nombre confirmado: `cac_api` evolucionará a Sentinel API. El cambio completo de paquetes, rutas, DNS y contratos no se presume realizado.
- RDS continúa en `us-west-1`. Las futuras identidades de base de datos serán separadas por API; no se duplicará una credencial compartida en varios secretos.

## Matriz de ejecución actual

| Hito | Estado | Evidencia o condición pendiente |
|---|---|---|
| Etapa 0: commits y etiqueta estable | Completado | `stable-pre-security-hardening-2026-07-13` en los cuatro repositorios |
| 1A.1 Contención en código | Completado localmente | `.env` fuera de Git, defaults y tokens embebidos retirados, builds verificados |
| 1A.2 Base IAM en nueva EC2 | Completado | Instance role, IMDSv2 obligatorio, hop limit 1 e identidad STS comprobada |
| 1A.3 Piloto Secrets Manager | Completado en AWS | `helios/prod/sentinel/telegram`, lectura por ARN exacto y `ListSecrets` denegado |
| 1A.4 Consumo de Telegram por Sentinel | Completado localmente, no desplegado | Build/pruebas pasan; Sentinel no existe aún en la nueva EC2 |
| 1A.5 Renombrar gateway a `helios-gateway` | Completado y desplegado | Workflow `29299638952`, servicio activo y `/actuator/health` en `UP` el 2026-07-13 |
| 1A.6 Catálogo y rotación restantes | Pendiente | DB, JWT, cifrado, correo, Telegram, WhatsApp y APIs externas |
| 1A.7 Puerta operativa | En progreso local | Escaneo, suites locales y barrera preventiva preparados; faltan rotaciones/revocaciones, publicación del CI y pruebas operacionales desplegadas |
| 1B Autenticación/autorización | Pendiente | Bloqueada para despliegue hasta cerrar 1A |
| 1C Criptografía/CORS/CSRF | Pendiente | No iniciada |
| 1D Abuso/cadena de suministro | Preparación local | OSV cubrió 227 paquetes de Super Admin sin hallazgos; falta SBOM/lock y escaneo real de dependencias Java |
| Etapa 2 Gateway y dominio único | Pendiente | No comienza hasta aprobar la puerta completa de Etapa 1 |

El renombrado del runtime del gateway es preparación reversible de la plataforma HELIOS dentro de 1A. No representa el inicio de la reingeniería de rutas, DNS o tráfico de la Etapa 2.

## Siguiente secuencia controlada

1. Completar y validar operacionalmente el inventario del Día 0 en `docs/stage-1/DAY_0_SECRET_INVENTORY_2026-07-13.md`.
2. Resolver el límite de confianza del Instance Profile compartido antes de ampliar la política del rol a secretos de CBT y Super Admin.
3. Crear el catálogo HELIOS definitivo y las identidades MySQL por API, sin desplegar todavía las APIs en la nueva EC2.
4. Migrar y validar una API por vez, conservando producción legacy y un rollback probado.
5. Cerrar rotaciones, escaneo y pruebas de integración antes de habilitar 1B en cualquier ambiente desplegado.

## Reglas de ejecución

1. Cada cambio se desarrolla y valida por servicio antes de integrarse con el gateway.
2. No se incorporan secretos reales al repositorio, artefactos, logs, imágenes ni archivos de ejemplo.
3. Cada subetapa produce: cambio versionado, prueba automatizada, evidencia de ejecución, procedimiento de reversión y riesgos pendientes.
4. Una puerta fallida bloquea la subetapa siguiente. No se aceptan excepciones silenciosas.
5. Producción conserva los orígenes actuales hasta que el canario del gateway cumpla sus criterios de salida.
6. Los cambios de DNS, firewall, credenciales externas y limpieza destructiva de historial requieren una ventana y autorización operacional explícitas.

## Etapa 0 — Línea base y recuperación

Estado: completada localmente el 2026-07-13.

- Commit de instantánea en cada repositorio, incluso cuando no había cambios pendientes.
- Etiqueta anotada común: `stable-pre-security-hardening-2026-07-13`.
- Sin `push`, despliegue ni modificación de infraestructura.
- Reversión: desplegar el commit señalado por la etiqueta del servicio afectado; no usar reescrituras destructivas del árbol de trabajo.

## Etapa 1 — Endurecimiento previo a la reingeniería

### 1A. Contención de secretos y credenciales bootstrap

Objetivo: detener nuevas exposiciones antes de modificar controles de acceso.

- Retirar del seguimiento archivos `.env` con valores reales y publicar únicamente plantillas sin secretos.
- Sustituir tokens, contraseñas, claves y usuarios hardcodeados por configuración externa obligatoria o funciones deshabilitadas por defecto.
- Eliminar tokens de logs y sanear respuestas de proveedores externos.
- Desactivar los seeders de usuarios conocidos; cualquier bootstrap deberá ser explícito, de un solo uso y auditable.
- Preparar un inventario de rotación para DB, JWT, correo, Telegram, WhatsApp, TLS y cifrado.
- Mantener como tarea operacional la rotación real y la invalidación de los valores expuestos.
- Evaluar la limpieza del historial únicamente después de rotar; requiere respaldo, coordinación y aprobación porque reescribe Git.

Puerta 1A:

- Escaneo del árbol Git sin secretos de alta entropía ni credenciales conocidas.
- Compilación y pruebas de los tres servicios.
- Arranque falla de forma segura o deshabilita la integración cuando falta una credencial opcional.
- Evidencia operacional de rotación antes de declarar cerrada la exposición.

### 1B. Autenticación y autorización

Objetivo: impedir accesos anónimos, escalamiento horizontal/vertical y confianza JWT ambigua.

- CAC: eliminar la regla global `permitAll` y definir una matriz explícita por ruta y método.
- CBT: reemplazar actualizaciones de entidades completas por DTOs permitidos; restringir rol, permisos, estado y eliminación a una política administrativa explícita.
- CBT: hacer efectivos `enabled`, bloqueo y revocación de sesiones.
- Los tres servicios: cerrar el registro público del primer ROOT y adoptar bootstrap administrativo de un solo uso.
- Super Admin y emisores JWT: validar algoritmo, firma, `iss`, `aud`, `exp`, `nbf`, identificador de token y tipo de token.
- Reemplazar permisos por coincidencia exacta normalizada; nunca por subcadenas.
- Incorporar pruebas negativas de acceso anónimo, IDOR, escalamiento de rol y reutilización cruzada de tokens.

Puerta 1B: ninguna operación sensible se autoriza sin identidad y permiso exactos; la batería negativa debe pasar en CI.

### 1C. Criptografía, sesiones y política de navegador

Objetivo: asegurar datos y credenciales en tránsito, reposo y navegador.

- Diseñar migración versionada de AES-ECB/defaults a cifrado autenticado (AES-GCM o equivalente) con claves externas, identificador de versión y rotación.
- Separar claves por finalidad y servicio; no reutilizar JWT, cifrado y proveedores.
- Endurecer cookies (`Secure`, `HttpOnly`, `SameSite` coherente), expiración, rotación y revocación.
- Activar protección CSRF cuando se autentique mediante cookies; convertir operaciones mutantes expuestas por GET a métodos correctos.
- Definir una lista exacta de orígenes CORS por ambiente. El gateway será el único emisor de CORS para tráfico consolidado; los backends no responderán directamente a navegadores en producción.
- Probar preflight y respuesta real para GET/POST/PUT/PATCH/DELETE, headers autorizados, credenciales y respuestas de error.

Puerta 1C: pruebas de cifrado/migración, cookie/CSRF y matriz CORS completas; no se permite wildcard con credenciales.

### 1D. Superficie operativa, abuso y cadena de suministro

Objetivo: controlar vectores de disponibilidad, fraude y operación.

- Firmar y verificar webhooks con timestamp, nonce y protección de replay.
- Autenticar y limitar ingestión de logs y endpoints de costo; aplicar límites por identidad además de IP.
- Configurar `trusted proxies` exactos y rechazar cabeceras de cliente no confiables.
- Validar tamaño de body, content type, timeouts, circuit breakers y límites de concurrencia.
- Actualizar dependencias con CVE explotables y generar SBOM/escaneo reproducible.
- Redactar PII, tokens y credenciales en logs; definir trazabilidad con correlation ID.
- Restringir puertos de backend a red privada/gateway y verificar que no sean alcanzables desde Internet.
- Ejecutar SAST, análisis de dependencias, DAST autenticado y pruebas de regresión.

Puerta de salida de Etapa 1:

- Cero hallazgos críticos o altos abiertos; medios aceptados sólo con responsable y fecha.
- Rotaciones externas verificadas y sesiones/tokens anteriores invalidados.
- Pruebas funcionales, negativas y de abuso aprobadas.
- Rollback probado en un ambiente no productivo.
- Revisión manual de la matriz de permisos y de la configuración productiva.

## Etapa 2 — Reingeniería hacia un único dominio y gateway

La Etapa 2 comienza sólo después de aprobar la puerta de salida de Etapa 1.

### 2A. Contratos y topología

- Inventariar rutas, métodos, cuerpos, headers, cookies, códigos de error, uploads, streaming y timeouts.
- Reservar prefijos HELIOS sin solapamiento: `/helios-platform/sentinel/**`, `/helios-platform/cbt/**` y `/helios-platform/admin/**`.
- Decidir explícitamente si cada backend recibe el prefijo completo o una ruta reescrita.
- Congelar contratos con pruebas de caracterización y OpenAPI.

### 2B. Endurecimiento del gateway

- Lista cerrada de rutas; una ruta desconocida responde 404 sin fallback.
- TLS moderno, headers de seguridad, límites de body, timeouts y circuit breakers.
- CORS centralizado por ambiente y manejo uniforme de `OPTIONS`.
- Autenticación en defensa en profundidad: el gateway puede rechazar tokens inválidos, pero cada API conserva autorización propia.
- Sanitización de `Forwarded`/`X-Forwarded-*`, correlation ID, métricas y logs sin secretos.
- Rate limits por ruta e identidad; health checks separados de endpoints públicos.

### 2C. Preparación de backends

- Aceptar tráfico productivo sólo desde el gateway o red privada/mTLS.
- Configurar correctamente IP original y esquema mediante proxies conocidos.
- Desactivar CORS productivo duplicado en los backends.
- Conservar temporalmente los dominios antiguos para rollback, sin ampliar permisos.

### 2D. Integración en ambiente de prueba

- Ejecutar pruebas contractuales directas y vía gateway y comparar resultados.
- Matriz CORS de navegador real, incluyendo preflight fallido intencional.
- Pruebas de autenticación, autorización, cookies, uploads, errores, límites y caídas de upstream.
- Pruebas DAST y de confusión de rutas (`..`, encoding, slash duplicado, method override y headers reenviados).

### 2E. Shadow y canario

- Observar tráfico o reproducir muestras sanitizadas sin duplicar mutaciones.
- Activar canario progresivo: equipo interno, porcentaje pequeño y aumento por ventanas.
- Comparar latencia, tasas 4xx/5xx, CORS, autenticación y resultados funcionales por servicio.
- Rollback inmediato a los orígenes previos mediante configuración/DNS cuando se exceda un umbral.

### 2F. Corte controlado

- Reducir TTL con antelación, congelar cambios incompatibles y abrir ventana operacional.
- Cambiar clientes al dominio único sin eliminar aún los orígenes anteriores.
- Monitoreo reforzado y responsable de rollback disponible durante toda la ventana.

### 2G. Estabilización y retiro

- Mantener observación durante el periodo acordado y resolver desvíos.
- Retirar DNS y certificados antiguos sólo cuando no exista tráfico legítimo.
- Cerrar acceso público directo a backends y eliminar compatibilidad temporal.
- Actualizar diagramas, runbooks, inventario, alertas y plan de recuperación.

Puerta de salida de Etapa 2: cero errores CORS reproducibles en la matriz soportada, ausencia de bypass directo, SLOs cumplidos, DAST aprobado y rollback ensayado.

## Registro de evidencias por subetapa

Cada cierre debe registrar: commit por repositorio, comandos y resultados de pruebas, ambiente y fecha, artefacto de escaneo, riesgos aceptados, responsable operacional, instrucciones de despliegue y reversión. Un cambio implementado pero no desplegado o no rotado se marca como pendiente, nunca como resuelto.
