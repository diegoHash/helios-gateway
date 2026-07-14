# Estándar de nombres y acceso a secretos HELIOS

Estado: propuesta del Día 0

## Nombre

```text
helios/<environment>/<service>/<secret-group>
```

Valores permitidos inicialmente:

- `environment`: `prod`, `staging`, `dev`.
- `service`: `sentinel`, `cbt`, `super-admin`.
- `secret-group`: un proveedor o ciclo de rotación, por ejemplo `database`, `jwt`, `encryption`, `telegram`, `whatsapp`, `mail`, `gemini` u `openweather`.

No se incluyen región, cuenta, versión o identificadores personales en el nombre. La región y la cuenta ya forman parte del ARN; las versiones se gestionan con stages como `AWSCURRENT` y `AWSPREVIOUS`.

## Tags obligatorios

| Tag | Valor |
|---|---|
| `Application` | `helios-platform` |
| `Environment` | ambiente del secreto |
| `Service` | servicio consumidor |
| `SecretGroup` | grupo del secreto |
| `RotationStatus` | `pending`, `manual` o `automatic` |
| `ManagedBy` | `manual`, `terraform` o mecanismo aprobado |
| `DataClassification` | `credential`, `token`, `cryptographic-key` o `bootstrap` |

No se colocan valores secretos, nombres de personas, correos, usuarios DB ni identificadores confidenciales en tags o descripciones.

## Reglas de agrupación

1. Solamente se agrupan claves con el mismo consumidor, proveedor, política y ventana de rotación.
2. DB URL/host, usuario y contraseña pueden compartir un secreto porque se activan juntos.
3. Access y refresh JWT pueden compartir un secreto si siempre rotan en la misma operación; los secretos de compatibilidad se mantienen identificados por versión.
4. Telegram token, chat ID y username comparten grupo por consumidor, como en `helios/prod/sentinel/telegram`.
5. Gemini y OpenWeather permanecen separados porque tienen proveedores, revocación y cuotas distintas.
6. Un secreto compartido no se copia en rutas de varios servicios. Se crea una sola fuente con consumidores documentados o se elimina el acoplamiento mediante credenciales nuevas.
7. Bootstrap es temporal: debe tener fecha de expiración operacional, uso auditable y eliminación posterior.

## Permisos

- Runtime: `GetSecretValue` y, solo si el cliente lo requiere, `DescribeSecret` sobre ARN completos.
- Rotación humana/automatizada: identidad distinta del runtime.
- El runtime no crea, modifica, etiqueta, rota, lista ni elimina secretos.
- El rol de despliegue no recibe lectura de valores salvo que el mecanismo aprobado lo requiera expresamente.
- No se concede acceso por prefijo a producción cuando ya existen los ARN exactos.

## Versiones y rollback

1. La nueva versión comienza como candidata sin reemplazar silenciosamente el valor conocido.
2. Se valida el consumidor en un entorno no productivo o mediante canario.
3. `AWSCURRENT` identifica la versión activa y `AWSPREVIOUS` se conserva solo durante la ventana de rollback.
4. El rollback mueve el stage a una versión comprobada y reinicia únicamente el consumidor.
5. Después de validar, se revoca el valor en el proveedor; conservar `AWSPREVIOUS` en AWS no revive una credencial ya revocada.
6. La evidencia registra ARN, VersionId, fecha y resultado, nunca `SecretString`.

## Catálogo inicial

| Servicio | Grupos previstos |
|---|---|
| Sentinel | `database`, `jwt`, `encryption`, `mail`, `telegram`, bootstrap temporal |
| CBT | `database`, `jwt`, `encryption`, `mail`, `telegram`, `whatsapp`, `gemini`, `openweather` |
| Super Admin | `database`, `jwt`, `encryption-compat`, bootstrap temporal |

El catálogo se reduce cuando una integración se confirme inactiva. No se crea un secreto vacío ni se migra una credencial sin consumidor.
