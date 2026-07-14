# HELIOS Gateway

Punto de entrada único de HELIOS Platform. Enruta cada prefijo público al servicio
interno correspondiente y ofrece una interfaz Swagger agregada para consultar los
contratos OpenAPI sin fusionarlos.

## Enrutamiento

| Ruta pública | Servicio | Variable de destino | Transformación |
|---|---|---|---|
| `/helios/pulse/**` | HELIOS Pulse | `HELIOS_PULSE_API_URI` | Conserva la ruta completa |
| `/helios/sentinel/**` | HELIOS Sentinel | `HELIOS_SENTINEL_API_URI` | Retira `/helios/sentinel` |
| `/helios/admin/**` | Super Admin | `HELIOS_SUPER_ADMIN_API_URI` | Retira `/helios/admin` |

Valores locales predeterminados: Pulse `127.0.0.1:9595`, Sentinel
`127.0.0.1:8080` y Super Admin `127.0.0.1:9591`.

## Requisitos y verificación

- Java 17 o superior
- Certificado y clave TLS configurados en `application.yml`
- Servicios internos accesibles únicamente desde la red autorizada

```powershell
.\gradlew.bat clean test bootJar
```

El artefacto resultante es `build/libs/helios-gateway.jar` y el servicio escucha
en HTTPS por el puerto `9590`.

## Swagger agregado

El Gateway publica Swagger únicamente cuando `HELIOS_SWAGGER_ENABLED=true` y los
servicios de destino tienen habilitada la misma variable:

- UI: `https://<gateway>:9590/swagger-ui.html`
- Pulse OpenAPI: `https://<gateway>:9590/docs/pulse/v3/api-docs`
- Sentinel OpenAPI: `https://<gateway>:9590/docs/sentinel/v3/api-docs`

La lista desplegable de Swagger permite seleccionar **HELIOS Pulse API** o
**HELIOS Sentinel API**. Super Admin se añadirá cuando su repositorio exponga un
contrato OpenAPI verificado.

## Política de seguridad

- Swagger permanece deshabilitado por defecto.
- El Gateway no debe almacenar secretos de las APIs.
- TLS termina en el Gateway; los archivos de certificado no se versionan.
- CORS debe configurarse con orígenes explícitos, nunca con `*` cuando se usan credenciales.
- Las APIs internas no deben quedar expuestas directamente a Internet.
- `/actuator/health` es el único endpoint Actuator publicado actualmente.

## Despliegue

El workflow construye, prueba y copia `helios-gateway.jar` sin renombrarlo como
`app.jar`. No promociones una rama a `main` hasta validar health checks, rutas,
CORS, autenticación y rollback. Consulta [`HELIOS_MIGRATION.md`](HELIOS_MIGRATION.md)
y la documentación de seguridad incluida en `docs/`.
