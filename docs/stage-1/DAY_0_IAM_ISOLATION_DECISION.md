# Día 0 — Decisión de aislamiento IAM para la EC2 compartida

Estado: propuesta pendiente de aprobación
Alcance: nueva EC2 HELIOS en `us-east-1`

## Problema

Sentinel, CBT y Super Admin compartirán inicialmente una EC2. Un Instance Profile representa a la instancia, no a cada proceso. IMDSv2 exige un token de sesión, pero cualquier aplicación local que pueda comunicarse con IMDS puede solicitar ese token y obtener las credenciales temporales del mismo rol.

Por esa razón, crear rutas distintas en Secrets Manager y una política con varios ARN mejora el alcance del host, pero no impide que un servicio comprometido lea el secreto autorizado para otro servicio mediante el mismo rol.

AWS documenta que:

- el software que corre en EC2 obtiene las credenciales del rol desde IMDS: [credenciales desde instance metadata](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-metadata-security-credentials.html);
- potencialmente cualquier software con acceso a la instancia puede ver metadata: [uso de instance metadata](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-instance-metadata.html);
- se puede limitar IMDS por usuario mediante reglas locales: [limitar acceso a IMDS](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/instance-metadata-limiting-access.html);
- Secrets Manager permite limitar `GetSecretValue` y `DescribeSecret` a ARN exactos: [políticas de identidad](https://docs.aws.amazon.com/secretsmanager/latest/userguide/auth-and-access_iam-policies.html).

## Invariante de seguridad

El gateway no consume secretos de las APIs y no debe poder recuperar ninguno. Ningún proceso de API debe poder usar directamente las credenciales del Instance Profile compartido si se adopta aislamiento por usuario local.

## Opciones

### A. Instance Profile directo para las tres APIs

Cada API usa el SDK/agent y obtiene el mismo rol desde IMDS.

- Ventaja: menor complejidad y compatibilidad directa con Spring Cloud AWS.
- Límite: una API comprometida puede usar todas las autorizaciones del rol.
- Veredicto: no aceptable como estado final; solamente útil para una prueba aislada sin secretos de otros servicios.

### B. EC2 compartida con materializador privilegiado

Un proceso root dedicado es el único autorizado localmente para acceder a IMDS. Recupera por ARN exacto los secretos aprobados y los entrega como credenciales efímeras separadas a unidades systemd que ejecutan con usuarios Linux distintos.

Controles obligatorios:

1. Usuarios sin login: `helios-sentinel`, `helios-cbt` y `helios-super-admin`.
2. `helios-gateway` usa su propio usuario sin acceso a IMDS.
3. Regla de salida que rechaza `169.254.169.254` para todos los usuarios excepto el materializador.
4. Secretos materializados únicamente bajo `/run`, nunca bajo `/home`, `/opt`, Git o el artefacto.
5. Directorio/archivo por servicio, propietario exclusivo y permisos mínimos.
6. Las unidades usan hardening systemd y no comparten `EnvironmentFile`.
7. El materializador acepta una lista cerrada de ARN; no acepta un `secretId` arbitrario desde las APIs.
8. Una rotación actualiza la credencial efímera, reinicia solamente el consumidor y ejecuta prueba sintética.
9. La regla de firewall se hace persistente y se prueba después de reiniciar la instancia.
10. CloudTrail y journald no registran valores.

- Ventaja: mantiene una EC2 y evita claves AWS estáticas en las aplicaciones.
- Límite: el host/root sigue siendo el límite de confianza; CloudTrail atribuye lecturas al rol de la instancia, no a cada API.
- Veredicto: mínimo privilegio viable para la transición de siete días, sujeto a pruebas de escape y persistencia.

El AWS Secrets Manager Agent puede reducir llamadas y cachear valores, pero no se considerará por sí solo una frontera de autorización entre aplicaciones. El componente que exponga secretos localmente debe imponer una lista cerrada por consumidor o permanecer detrás del materializador privilegiado.

### C. Identidades de cómputo independientes

Ejecutar cada servicio como tarea con rol propio o en una instancia separada.

AWS permite roles por tarea ECS y recomienda uno por servicio. También advierte que los contenedores sobre EC2 comparten host y pueden acceder a metadata o datos de tareas vecinas si no se aplican controles; Fargate ofrece una frontera de aislamiento más fuerte: [IAM task roles](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-iam-roles.html) y [recomendaciones de roles ECS](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/security-iam-roles.html).

- Ventaja: IAM, auditoría y permisos específicos por servicio.
- Límite: mayor trabajo operativo y posible costo; ECS sobre una misma EC2 aún requiere bloquear IMDS y endurecer el host.
- Veredicto: objetivo recomendado antes de considerar cerrada una arquitectura con aislamiento estricto; Fargate o instancias separadas cuando el riesgo exija frontera de cómputo.

## Recomendación para el ciclo actual

Adoptar la opción B como transición explícita, sin confundirla con aislamiento IAM por aplicación. Diseñar desde ahora cada secreto y unidad para migrar después a la opción C sin cambiar contratos de configuración.

Mientras solamente corre el gateway:

- retirar del Instance Role la política de lectura de Sentinel o bloquear IMDS para el usuario del gateway;
- conservar el secreto de Telegram sin consumidor;
- no ampliar la política a CBT ni Super Admin.

Antes de desplegar la primera API se debe probar:

1. El materializador puede leer solamente los ARN aprobados.
2. El usuario de Sentinel no puede obtener un token IMDS.
3. El usuario de Sentinel no puede leer archivos de CBT o Super Admin.
4. El gateway no puede acceder a IMDS ni a archivos de credenciales.
5. Las restricciones sobreviven un reinicio.

## Política base del host

La identidad de runtime tendrá exclusivamente:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadApprovedHeliosSecrets",
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue",
        "secretsmanager:DescribeSecret"
      ],
      "Resource": [
        "<FULL_SECRET_ARN_1>",
        "<FULL_SECRET_ARN_2>"
      ]
    }
  ]
}
```

No se agregan `ListSecrets`, `BatchGetSecretValue`, `secretsmanager:*` ni `Resource: "*"`. Si se usa una KMS key administrada por el cliente, `kms:Decrypt` se concede solamente para su ARN; con la clave administrada por AWS no se añade un permiso KMS innecesario.

Los ARN completos se incorporan después de crear cada secreto. Para recursos aún inexistentes, el patrón de seis caracteres `??????` requiere revisión explícita; no se usará el sufijo abierto `-*`.

## Puerta de decisión

El Día 0 no se cierra hasta registrar una de estas decisiones:

- `B — host compartido como frontera temporal`, con aceptación del riesgo residual y fecha objetivo para reevaluar; o
- `C — identidades de cómputo separadas`, con plataforma elegida.

La opción A no satisface la puerta de mínimo privilegio del plan.
