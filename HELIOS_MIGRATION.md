# HELIOS Gateway migration boundary

This repository is the independent HELIOS gateway. It routes the new public
prefixes without changing or deploying any legacy repository:

- `/helios/pulse/**` -> `HELIOS_PULSE_API_URI`
- `/helios/sentinel/**` -> `HELIOS_SENTINEL_API_URI`
- `/helios/admin/**` -> `HELIOS_SUPER_ADMIN_API_URI`

Sentinel receives the route with `/helios/sentinel` removed. Pulse currently
retains its full public prefix. This difference is explicit and must be
revisited when service context paths are standardized.

The tag `pre-helios-rebrand-2026-07-13` identifies the imported stable baseline.
