# Día 0 — Evidencia de validación local

Fecha local: 2026-07-13
Estado: compilación y pruebas locales aprobadas con limitaciones registradas

## Controles de seguridad de la ejecución

- No se usaron credenciales AWS ni se recuperaron secretos.
- No se modificó infraestructura o producción legacy.
- Sentinel no tiene un `.env` real en el checkout inspeccionado.
- Super Admin no tiene un `.env` real en el checkout inspeccionado.
- CBT sí tiene `.env` locales con configuración sensible. La primera ejecución excluyó las dos clases `@SpringBootTest`; después se incorporó un classpath de prueba aislado que reemplaza la configuración principal y permitió ejecutar la suite completa sin leer esos archivos.
- No se imprimieron valores de configuración.

## Resultados

| Servicio | Validación | Resultado |
|---|---|---|
| Sentinel/CAC | `gradlew clean test --no-daemon` | aprobado: 1 suite, 3 tests, 0 fallos, 0 errores, 0 omitidos |
| CBT | `gradlew clean test --no-daemon` con perfil aislado | aprobado: 22 suites, 112 tests, 0 fallos, 0 errores, 0 omitidos |
| Super Admin | `npm run typecheck` | aprobado |
| Super Admin | `npm run lint` | aprobado |
| Super Admin | `npm run build` | aprobado |
| Super Admin | `npm test` y `npm run test:typecheck` | aprobado: 6 tests, 0 fallos, 0 errores, 0 omitidos |

Las tres ramas quedaron con árboles de trabajo limpios después de la validación.

## CBT: aislamiento implementado

El commit `f9420ba` incorpora:

1. H2 únicamente como dependencia de runtime de pruebas.
2. `src/test/resources/application.properties`, que reemplaza la configuración principal durante tests y no importa `.env`.
3. Base en memoria con `create-drop`, sin host o credenciales RDS.
4. Cache simple y Redis deshabilitado.
5. Telegram, WhatsApp, correo, Gemini y OpenWeather sin credenciales ni endpoints productivos.
6. JWT y cifrado con material deliberadamente ficticio y de baja entropía.
7. Un `contextLoadsWithoutExternalInfrastructure` que reemplaza diagnósticos legacy capaces de consultar e imprimir datos reales.
8. Prueba JWT que genera un token bien formado con una clave de test incorrecta, sin literals que parezcan credenciales.

`CbtApiApplicationTests` y `SecurityExploitationTests` se ejecutaron correctamente dentro de la suite completa. El rango `origin/master..HEAD` también pasó Gitleaks después del cambio.

## Advertencias no bloqueantes registradas

- Sentinel usa una API deprecada en `TelegramNotificationError`.
- CBT usa una API deprecada en `NotificationService`.
- Ambos builds Java usan características de Gradle marcadas como incompatibles con Gradle 9.0.
- La suite inicial de Super Admin cubre CORS y cifrado compatible, pero todavía no cubre autenticación, autorización, rutas con repositorios o integración MySQL.

Estas advertencias no invalidan la contención de secretos, pero deben entrar en el backlog de 1D o corregirse antes si interfieren con la migración.

## Evidencia de CI preventivo

Los tres workflows `Secret Scan` pasaron:

- Actionlint `1.7.12` con binario verificado por SHA-256.
- Gitleaks `8.30.1` sobre el archivo del workflow.
- Gitleaks sobre el rango completo del futuro PR: cinco commits por servicio, sin hallazgos.

Las puertas CI locales preparadas además garantizan:

- Sentinel y CBT ejecutan `clean test bootJar` antes de cualquier copia o reinicio del deploy legacy; se retiró `-x test`.
- Cada repositorio Java tiene un workflow de PR independiente con permisos `contents: read`.
- Super Admin ejecuta typecheck de aplicación y tests, lint, seis pruebas y build antes del deploy.
- Las acciones oficiales y las acciones SSH/SCP existentes se fijaron por SHA completo en los workflows modificados.
- CBT espera un health check real contra MySQL efímero antes de iniciar ZAP.
- El ZAP API Scan usa la imagen oficial `2.17.0` fijada por digest y ya no convierte cualquier salida en éxito con `|| true`.
- Los reportes ZAP se conservan con `if: always()` aunque el control bloquee el job.

Los workflows completos pasaron Actionlint. ZAP no pudo ejecutarse localmente porque Docker no está instalado; su primera ejecución real en GitHub sigue siendo evidencia obligatoria y puede revelar hallazgos que deban corregirse.

Los workflows siguen sin publicar y por tanto todavía no protegen las ramas remotas.

## Puerta pendiente

La validación local de código queda aprobada. La validación operacional permanece abierta hasta completar:

- despliegue controlado de cada consumidor de secretos;
- prueba sintética y rollback por cada rotación.
