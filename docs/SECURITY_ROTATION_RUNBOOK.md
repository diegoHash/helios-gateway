# Runbook de rotación requerido por la Etapa 1A

Los cambios de código detienen nuevas exposiciones, pero no invalidan credenciales ya conocidas. Este runbook debe ejecutarse en los proveedores correspondientes antes de cerrar 1A. No se deben copiar valores nuevos a tickets, chat, Git o logs.

## Orden de ejecución

1. Confirmar acceso de emergencia y respaldo probado para cada plataforma.
2. Crear una credencial nueva con privilegios mínimos y almacenarla en el gestor de secretos del ambiente.
3. Actualizar un servicio a la vez, desplegar y ejecutar health checks y una transacción sintética.
4. Revocar la credencial anterior y verificar que deja de funcionar.
5. Invalidar sesiones o tokens derivados cuando aplique.
6. Registrar sólo identificador/versión, responsable, fecha y evidencia; nunca el valor.

## Inventario mínimo

| Sistema | Elemento por rotar | Acción de verificación |
|---|---|---|
| CAC | Usuario/contraseña MySQL expuestos | Conexión nueva correcta; credencial anterior rechazada |
| CBT | Usuario/contraseña MySQL del script y archivos locales | Script y servicio usan el gestor; credencial anterior rechazada |
| CAC/CBT | Secretos de firma JWT | Tokens anteriores rechazados tras ventana controlada |
| Super Admin | Secretos JWT de acceso y refresh | Access/refresh antiguos rechazados y sesiones revocadas |
| CAC | Token del bot Telegram | Token anterior revocado en BotFather y envío sintético correcto |
| CBT | Token de WhatsApp/Meta | Token anterior revocado y plantilla sintética aceptada |
| CAC/CBT | Credenciales SMTP | Login anterior rechazado y correo sintético correcto |
| APIs | Claves de cifrado de datos | Se realizará con la migración versionada de 1C; no rotar a ciegas |
| Infraestructura | Keystores/contraseñas TLS, Redis y otras variables | Arranque/health check correctos; valor anterior invalidado |

## Controles adicionales

- Restringir los usuarios de DB a las tablas y operaciones necesarias y, cuando sea posible, a la red/origen de cada servicio.
- Mantener secretos separados por ambiente y propósito.
- Revisar imágenes, backups, artefactos CI y hosts de despliegue, no sólo el árbol Git actual.
- Tratar el historial Git como comprometido aun después de limpiar el `HEAD`.

## Limpieza del historial Git

No se ejecuta automáticamente. Sólo después de completar las rotaciones se debe acordar una ventana para `git filter-repo` o equivalente, respaldar referencias, coordinar force-push y exigir que todos los clones sean renovados. Limpiar el historial antes de rotar no elimina el riesgo.

## Evidencia de cierre

1A permanece abierta hasta que exista evidencia de revocación para cada secreto aplicable, el escaneo del árbol y artefactos resulte limpio y las pruebas de arranque/integración pasen con la nueva configuración.
