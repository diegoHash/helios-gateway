# Día 0 — Inventario de secretos y fuentes de configuración

Fecha local: 2026-07-13
Alcance: Sentinel/CAC, CBT y Super Admin
Estado: inventario de código completado; validación operacional pendiente

## Reglas aplicadas durante el inventario

- Se analizaron únicamente nombres de variables, referencias en código, plantillas seguras y archivos versionados.
- No se leyó ni registró el contenido de archivos `.env` reales.
- No se recuperó ningún `SecretString` de AWS Secrets Manager.
- Los valores de GitHub Actions y de los proveedores externos no fueron consultados.
- Una variable detectada en código prueba que existe una dependencia de configuración; no prueba que la integración esté activa en producción.

## Evidencia de repositorios

| Servicio | Rama local | Estado del árbol | Integración con Secrets Manager |
|---|---|---|---|
| Sentinel/CAC | `master`, tres commits delante de `origin/master` | limpio | piloto implementado para Telegram |
| CBT | `master`, tres commits delante de `origin/master` | limpio | no implementada |
| Super Admin | `main`, tres commits delante de `origin/main` | limpio | no implementada |

En los tres repositorios el único archivo de entorno versionado es `.env.example`. Los archivos de entorno reales permanecen excluidos del inventario de contenido.

## Convención propuesta para AWS

Cada secreto tendrá la forma `helios/prod/<servicio>/<grupo>`. Un grupo contiene solamente valores que comparten consumidor, permisos y ciclo de rotación. No se duplicará una misma credencial compartida en varios secretos.

Los nombres JSON actuales se conservan durante la primera migración para reducir cambios simultáneos. La adopción de nombres canónicos HELIOS se hará mediante compatibilidad explícita y retirada posterior de los aliases legacy.

## Sentinel/CAC

| Grupo propuesto | Identificadores actuales | Condición observada | Estado |
|---|---|---|---|
| `helios/prod/sentinel/database` | `CAC_DDBB_URL`, `CAC_DDBB_USERNAME`, `CAC_DDBB_PASS` | contraseña obligatoria; URL y usuario conservan defaults de desarrollo | pendiente |
| `helios/prod/sentinel/jwt` | `ETECC_JWT_SECRET` | obligatorio | pendiente |
| `helios/prod/sentinel/encryption` | `CAC_ENCRYPTION_KEY` | obligatorio | pendiente |
| `helios/prod/sentinel/mail` | `ETECC_MAIL_USERNAME`, `ETECC_MAIL_PASSWORD` | contraseña opcional; host y puerto son configuración no secreta | pendiente de confirmar uso |
| `helios/prod/sentinel/telegram` | `HELIOS_TELEGRAM_BOT_TOKEN`, `HELIOS_TELEGRAM_CHAT_ID`, `HELIOS_TELEGRAM_BOT_USERNAME` | integración opcional durante transición | creado y lectura por ARN validada |
| secreto temporal de bootstrap | `CAC_BOOTSTRAP_PASSWORD` y datos del operador | bootstrap deshabilitado por defecto | no crear hasta una operación autorizada |

Aliases de transición detectados para Telegram: `ETECC_TELEGRAM_BOT_TOKEN`, `ETECC_TELEGRAM_CHAT_ID` y `ETECC_TELEGRAM_BOT_USERNAME`. Deben retirarse después de la prueba sintética y la rotación del bot.

## CBT

| Grupo propuesto | Identificadores actuales | Condición observada | Estado |
|---|---|---|---|
| `helios/prod/cbt/database` | `ETECC_DDBB_URL`, `ETECC_DDBB_USERNAME`, `ETECC_DDBB_PASS` | contraseña obligatoria; URL y usuario conservan defaults de desarrollo | pendiente |
| aliases de utilidad DB | `CBT_DDBB_HOST`, `CBT_DDBB_NAME`, `CBT_DDBB_USERNAME`, `CBT_DDBB_PASS` | usados solamente por `scratch/query_db.py` | retirar o alinear antes del despliegue |
| `helios/prod/cbt/jwt` | `ETECC_JWT_SECRET` | obligatorio | pendiente |
| `helios/prod/cbt/encryption` | `CBT_ENCRYPTION_KEY` | obligatorio | pendiente |
| `helios/prod/cbt/mail` | `ETECC_MAIL_USERNAME`, `ETECC_MAIL_PASSWORD` | contraseña opcional | pendiente de confirmar uso |
| `helios/prod/cbt/telegram` | `ETECC_TELEGRAM_BOT_TOKEN`, `ETECC_TELEGRAM_BOT_CHAT_ID` | opcional | pendiente de confirmar uso |
| `helios/prod/cbt/whatsapp` | `ETECC_WHATSAPP_TOKEN`, `ETECC_WHATSAPP_PHONE_ID` | opcional | pendiente de confirmar uso |
| `helios/prod/cbt/gemini` | `GEMINI_API_KEY` | opcional | pendiente de confirmar uso |
| `helios/prod/cbt/openweather` | `OPENWEATHERMAP_API_KEY` | opcional | pendiente de confirmar uso |

CBT todavía carga `optional:file:.env[.properties]` y no contiene dependencias ni configuración de AWS Secrets Manager.

## Super Admin

| Grupo propuesto | Identificadores actuales | Condición observada | Estado |
|---|---|---|---|
| `helios/prod/super-admin/database` | `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` | base, usuario y host se resuelven mediante esquema y fallback | pendiente |
| aliases legacy DB | `ETECC_DDBB_URL`, `ETECC_DDBB_USERNAME`, `ETECC_DDBB_PASS` | fallback de compatibilidad | retirar después de validar `MYSQL_*` |
| `helios/prod/super-admin/jwt` | `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`, `JWT_COMPAT_SECRETS`, `ETECC_JWT_SECRET` | access/refresh usan el secreto legacy como fallback | pendiente de diseñar transición |
| `helios/prod/super-admin/encryption-compat` | `CBT_AES_COMPAT_KEY` | obligatorio y de longitud fija | pendiente |
| secreto temporal de bootstrap | `ROOT_PASSWORD` | opcional en el esquema actual | no crear hasta una operación autorizada |

La unidad systemd actual carga `/home/ec2-user/.env`, ejecuta como `ec2-user` y tiene `ProtectHome=false`. Esa unidad no se reutilizará en HELIOS sin aislamiento, endurecimiento y una fuente de configuración específica del servicio.

## Configuración no secreta que debe separarse

- Puertos, hosts de escucha y nombres de aplicación.
- Región AWS, issuer, audience y TTL de tokens.
- Dominios de cookies, política `Secure`, proxy confiable y orígenes CORS.
- Hosts y puertos de SMTP/Redis, límites de pool y URLs públicas.
- Flags de bootstrap, cache e integraciones opcionales.

Estos valores pueden vivir en configuración versionada por ambiente o en parámetros; no deben mezclarse con contraseñas y claves solo por conveniencia.

## Riesgos confirmados para resolver en 1A

1. Las tres APIs en una sola EC2 comparten el límite de confianza del Instance Profile. Separar rutas de secretos no crea por sí solo aislamiento IAM entre procesos.
2. Sentinel es el único servicio con carga desde Secrets Manager; CBT y Super Admin todavía dependen de archivos de entorno.
3. `ETECC_JWT_SECRET` funciona como nombre legacy en los tres servicios y puede representar una clave compartida. No debe rotarse hasta confirmar quién emite y quién valida cada token.
4. Los usuarios y permisos MySQL por API aún no están separados. RDS continúa en `us-west-1` y la EC2 nueva en `us-east-1`.
5. Sentinel y CBT mantienen defaults de desarrollo para URL/usuario DB y logging de Spring Security en `DEBUG`; producción deberá fallar de forma segura y reducir el nivel de log.
6. El servicio legacy de Super Admin usa un `.env` global en el home de `ec2-user`, incompatible con separación clara por servicio.
7. Los tres repositorios tienen commits de seguridad locales pendientes de publicación; esto debe resolverse con ramas/PRs antes de depender de esos cambios en despliegues.
8. El escaneo verificable encontró secretos históricos en Sentinel y CBT, además de configuración sensible local de CBT. La evidencia y las acciones obligatorias están en `DAY_0_SECRET_SCAN_2026-07-13.md`.

## Decisiones requeridas para cerrar el Día 0

1. Confirmar cuáles integraciones opcionales están activas en producción: correo, Telegram de CBT, WhatsApp, Gemini y OpenWeather.
2. Confirmar si tokens emitidos por una API son aceptados actualmente por otra API.
3. Confirmar que existe acceso administrativo controlado a RDS para crear usuarios y otorgar permisos sin compartir la contraseña maestra.
4. Elegir la estrategia de aislamiento para la EC2 compartida: límite de confianza a nivel host como transición, o identidades de cómputo separadas para aislamiento IAM real.
5. Identificar responsable y ventana de rotación para cada proveedor externo, sin registrar credenciales en este documento.

## Criterio de salida del Día 0

El Día 0 se cierra cuando las cinco decisiones anteriores están registradas, cada secreto tiene propietario y consumidor confirmados, y no quedan dependencias desconocidas. No se crea ni rota ningún secreto adicional antes de ese cierre.

La evaluación y recomendación de aislamiento están documentadas en `DAY_0_IAM_ISOLATION_DECISION.md`. La convención propuesta de recursos y tags está en `HELIOS_SECRET_NAMING_STANDARD.md`.
