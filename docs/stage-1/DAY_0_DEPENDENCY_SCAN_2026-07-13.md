# Día 0 — Evidencia inicial de dependencias

Fecha local: 2026-07-13
Estado: Super Admin cubierto; cobertura Java pendiente

## Herramienta

- OSV-Scanner `2.4.0` / OSV-SCALIBR `0.4.5`.
- Binario oficial `osv-scanner_windows_amd64.exe`.
- SHA-256 verificado: `0cdd113610126d5dfd5e12ad0e0b4f3e879291ff19bb43b0c52ed2f2c2df1a37`.
- Reportes JSON almacenados fuera de Git en el directorio temporal local.
- El escaneo respetó `.gitignore`; no se solicitó inspeccionar `.env` ignorados.

## Resultados demostrables

| Servicio | Fuente | Paquetes resueltos | Vulnerabilidades OSV |
|---|---|---:|---:|
| Super Admin | `package-lock.json` | 227 | 0 |
| Sentinel/CAC | Gradle sin lockfile/SBOM | 0 | cobertura no disponible |
| CBT | Gradle sin lockfile/SBOM | 0 | cobertura no disponible |

El resultado de Super Admin significa que OSV no informó vulnerabilidades conocidas para las 227 dependencias resueltas en el momento del escaneo. No demuestra ausencia de vulnerabilidades desconocidas, errores de configuración o código vulnerable propio.

Los resultados vacíos de Sentinel y CBT no significan cero vulnerabilidades. OSV no encontró un lockfile o SBOM capaz de representar sus dependencias transitivas.

## Pendientes obligatorios

1. Generar un SBOM CycloneDX o lock reproducible para cada proyecto Gradle.
2. Confirmar que el inventario incluye dependencias transitivas y runtime.
3. Repetir OSV-Scanner y conservar IDs/severidad sin publicar artefactos sensibles.
4. Añadir la generación/validación del SBOM a CI.
5. Evaluar explotabilidad antes de actualizar dependencias; un upgrade no se mezcla con la rotación de secretos sin pruebas de regresión.

Esta evidencia adelanta parte de 1D, pero no se usa para aprobar la puerta 1A ni para afirmar que los servicios Java están libres de CVE.
