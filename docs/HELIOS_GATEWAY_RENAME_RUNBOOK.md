# Rename runbook: Legacy Gateway to HELIOS Gateway

## Scope

This migration applies only to the new EC2 instance in `us-east-1`. It does not modify or stop the legacy production EC2 instance in `us-west-1`, and it does not deploy Sentinel, CBT, or Super Admin.

## HELIOS resources

| Resource | HELIOS name |
|---|---|
| Spring application | `helios-gateway` |
| Artifact base | `helios-gateway` |
| Deployment directory | `/home/ec2-user/helios-gateway` |
| Systemd unit | `helios-gateway.service` |
| Deployed JAR | `helios-gateway.jar` |

The repository directory and remote repository name are not changed by this commit. DNS names and accepted legacy CORS origins remain unchanged until their own controlled migration.

## Cutover behavior

The deployment workflow copies the new artifact and unit without removing the previous installation. Immediately before binding port `9590`, it records whether `caribbean-gateway` is active and stops it. It starts `helios-gateway` and checks `https://127.0.0.1:9590/actuator/health` for up to 60 seconds.

If the health check fails, the workflow stops `helios-gateway`, restarts `caribbean-gateway` when it was previously active, prints the last service logs, and fails the deployment. After a successful health check, it disables the old unit but keeps its files for manual rollback.

## Manual rollback

```bash
sudo systemctl stop helios-gateway
sudo systemctl disable helios-gateway
sudo systemctl enable caribbean-gateway
sudo systemctl start caribbean-gateway
sudo systemctl status caribbean-gateway --no-pager
```

Do not remove the old unit or directory until the HELIOS gateway has completed its stabilization window.
