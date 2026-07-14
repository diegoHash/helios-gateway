# Día 0 — Evidencia de escaneo de secretos

Fecha local: 2026-07-13
Estado: ejecutado; remediación y rotación pendientes

## Herramienta y garantías

- Herramienta: Gitleaks `8.30.1`, obtenida del release oficial de `gitleaks/gitleaks`.
- Archivo: `gitleaks_8.30.1_windows_x64.zip`.
- SHA-256 verificado: `d29144deff3a68aa93ced33dddf84b7fdc26070add4aa0f4513094c8332afc4e`.
- Todos los escaneos usaron redacción del 100 %.
- Los reportes completos permanecen fuera de los repositorios, en almacenamiento temporal local.
- La evidencia versionada contiene solamente regla, ubicación, commit y clasificación; nunca contiene la coincidencia.

## Alcance ejecutado

1. Historial Git completo (`--all`) de Sentinel/CAC, CBT y Super Admin.
2. Árboles locales, incluyendo archivos ignorados.
3. Artefactos de hasta 100 MB y archivos comprimidos con profundidad máxima de dos niveles.

Gitleaks informó errores de lectura sobre algunos reportes HTML/XML comprimidos y metadatos internos de IntelliJ en CBT. No se usan como evidencia de ausencia. Estos elementos deberán excluirse de artefactos desplegables y someterse a una comprobación independiente si se conservan.

## Hallazgos en historial Git

### Sentinel/CAC

| Severidad | Regla | Ubicación histórica | Commit | Clasificación |
|---|---|---|---|---|
| Crítica | `generic-api-key` | `.env.prod`, `ETECC_JWT_SECRET` | `896515645224` | credencial real o indistinguible de una real; rotación obligatoria |
| Alta | `telegram-bot-api-token` | `.env.prod`, token Telegram | `896515645224` | token real o indistinguible de uno real; rotación obligatoria |
| Crítica | `generic-api-key` | `docker-compose.yml`, `AUTH_KEY` | `9375579f95f3` | material criptográfico histórico; identificar consumidor y rotar |
| Informativa | `generic-api-key` | `docs/AWS_SECRETS_MANAGER.md` | `2337250a065b` | falso positivo de texto documental |

### CBT

| Severidad | Regla | Ubicación histórica | Commit | Clasificación |
|---|---|---|---|---|
| Crítica | `private-key` | `key.pem` | `7a5c25a5d3a4` | clave privada comprometida; reemplazar certificado/par y revocar cuando aplique |
| Alta | `telegram-bot-api-token` | `application.properties` | `7a5c25a5d3a4` | token real o indistinguible de uno real; rotación obligatoria |
| Alta | `generic-api-key` | `application.properties`, Gemini | `8130f634b3e3` | clave externa expuesta; rotación obligatoria |
| Alta | `generic-api-key` | `application.properties`, OpenWeather | `8130f634b3e3` | clave externa expuesta; rotación obligatoria |
| Informativa | `generic-api-key` | `SecurityExploitationTests.java` | `2fdfef7b4eb2` | literal de prueba; sustituir por marcador inequívocamente ficticio si continúa generando ruido |

### Super Admin

No se detectaron coincidencias en el historial. Esto no demuestra que sus secretos nunca hayan sido expuestos por otros canales ni elimina la obligación de rotarlos si fueron compartidos con otro servicio.

## Hallazgos en árboles locales

### Sentinel/CAC

- Un caché ignorado de Gradle/NetBeans contiene cuatro coincidencias de `aws-access-token` correspondientes a un único identificador con formato de Access Key.
- El identificador no coincide con la clave de ejemplo pública comprobada.
- No se ha probado todavía si pertenece a la cuenta, si está activo o si es una coincidencia aleatoria dentro del archivo serializado.
- Se trata como crítico hasta identificarlo en IAM y desactivarlo si existe.
- La coincidencia documental de `AWS_SECRETS_MANAGER.md` es un falso positivo.

### CBT

Los archivos locales ignorados `.env` y `.env.prod` contienen configuración real o indistinguible de producción. No están versionados, pero constituyen almacenamiento local de texto plano.

Grupos confirmados como configurados:

- Credenciales y URL de la base de datos compartida.
- Dos JWT distintos entre `.env` y `.env.prod`.
- Credenciales SMTP en `.env.prod`.
- Gemini y OpenWeather; los valores son iguales entre ambos archivos.
- Telegram en `.env.prod`.
- Dos tokens/phone IDs distintos de WhatsApp entre ambos archivos.
- Una contraseña SSL legacy no referenciada por la configuración actual.

Gitleaks no detecta necesariamente contraseñas DB cortas o claves arbitrarias. La enumeración por nombre confirma que hay más secretos locales que las nueve coincidencias automáticas.

### Super Admin

No existe un `.env` real en el repositorio local inspeccionado y no se detectaron coincidencias en árbol o artefactos.

## Estado del árbol versionado actual

- No se confirmó ningún secreto real en archivos actualmente seguidos por Git.
- Los secretos históricos siguen recuperables desde commits anteriores y deben considerarse comprometidos hasta rotación/revocación.
- Eliminar archivos de la punta de la rama no invalida tokens, JWT, claves privadas ni credenciales externas.

## Acciones obligatorias derivadas

1. Identificar de forma segura el Access Key detectado en el caché de Sentinel mediante IAM, sin copiar su identificador en tickets o documentación; desactivar y eliminar si pertenece a la cuenta.
2. Rotar los JWT históricos de Sentinel y CBT mediante una ventana de compatibilidad controlada.
3. Rotar Telegram de Sentinel y CBT, Gemini, OpenWeather y los tokens de WhatsApp activos.
4. Crear usuarios MySQL separados por API, migrar y revocar la credencial compartida anterior.
5. Sustituir el certificado/par asociado a `key.pem` y revocarlo cuando el emisor lo permita.
6. Migrar SMTP y la contraseña SSL legacy o demostrar que ya no tienen consumidor; revocar en ambos casos si fueron expuestas.
7. Después de validar las nuevas fuentes, retirar de forma segura los `.env` locales de CBT. La eliminación requiere autorización porque destruye la copia local de configuración.
8. Decidir la reescritura de historial únicamente después de completar las rotaciones; reescribir Git no sustituye la revocación.

## Criterio para cerrar el hallazgo

Cada credencial requiere evidencia de nuevo identificador/versión, prueba sintética del consumidor, fecha de revocación del valor anterior y responsable. Los valores no se incorporarán a esta evidencia.

## Barrera preventiva preparada

Se agregó un workflow `Secret Scan` en una rama local separada de cada API:

| Servicio | Rama | Commit |
|---|---|---|
| Sentinel/CAC | `codex/stage-1a-secret-scan` | `a74ee8c` |
| CBT | `codex/stage-1a-secret-scan` | `7facab1` |
| Super Admin | `codex/stage-1a-secret-scan` | `8673f84` |

El workflow:

- fija Gitleaks `8.30.1` y comprueba el SHA-256 del binario oficial antes de usarlo;
- fija `actions/checkout` por SHA completo;
- concede solamente `contents: read`;
- en PR y push analiza el rango nuevo para impedir otra exposición;
- en ejecución manual analiza `--all`, por lo que seguirá fallando mientras existan hallazgos históricos sin remediar;
- redacta el 100 % de cualquier coincidencia y no publica reportes con valores.

Las definiciones completas de CI pasaron Actionlint `1.7.12`, verificado por SHA-256. También se simuló el rango completo de cada futuro PR contra su rama remota: cinco commits por servicio terminaron sin hallazgos. Un falso positivo documental de Sentinel fue corregido dentro del commit local original, sin añadir allowlists y antes de publicar la rama.

Esta barrera todavía no está activa en GitHub: las ramas no se han publicado ni integrado. No se añadieron allowlists o baselines para silenciar las exposiciones conocidas.
