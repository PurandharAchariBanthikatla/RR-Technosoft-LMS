# Running RR Technosoft LMS with plain Docker

`docker-compose.yml` and `docker-compose.monitoring.yml` have been removed.
The stack now runs as individually managed containers, wired together with
a dedicated bridge network — same services, same env vars, same ports.

## Prerequisites

- Docker Engine 24+ (no `docker compose` / Compose plugin required)
- `bash`

## Start the app stack (postgres, redis, backend, frontend)

```bash
cp .env.example .env      # fill in real values — see comments in the file
chmod +x docker/run.sh docker/stop.sh
./docker/run.sh
```

## Start with monitoring (+ prometheus, grafana)

```bash
./docker/run.sh --monitoring
```

## Stop everything

```bash
./docker/stop.sh            # stop + remove containers, keep data volumes
./docker/stop.sh --purge    # also delete postgres/prometheus/grafana volumes
```

## What `run.sh` does, service by service

| Service | Image / build context | Port | Notes |
|---|---|---|---|
| postgres | `postgres:16-alpine` | 5432 | Named volume `rr-technosoft-lms_pgdata`, healthchecked with `pg_isready` |
| redis | `redis:7-alpine` | 6379 | Password from `REDIS_PASSWORD` |
| backend | built from `backend/Dockerfile` | 8081→8080 | Waits for postgres + redis healthchecks before starting; itself healthchecked via `/actuator/health` |
| frontend | built from `rr-technosoft-lms/Dockerfile` | 3000 | `NEXT_PUBLIC_API_BASE_URL` baked in at build time (Next.js inlines `NEXT_PUBLIC_*` vars) — override with `FRONTEND_API_BASE_URL` env var before running the script |
| prometheus (`--monitoring`) | `prom/prometheus:v2.55.1` | 9090 | Mounts `monitoring/prometheus/*.yml` read-only |
| grafana (`--monitoring`) | `grafana/grafana:11.2.2` | 3001→3000 | Mounts `monitoring/grafana/provisioning` read-only |
| postgres-exporter (`--monitoring`) | `quay.io/prometheuscommunity/postgres-exporter:v0.15.0` | 9187 | Feeds the "Database & Cache" Grafana dashboard |
| redis-exporter (`--monitoring`) | `oliver006/redis_exporter:v1.62.0` | 9121 | Feeds the "Database & Cache" Grafana dashboard |

All containers join the `rr-technosoft-lms_lms-net` bridge network and refer
to each other by container name (`rr-lms-postgres`, `rr-lms-redis`,
`rr-lms-backend`), matching the old compose service-name DNS resolution.

`run.sh` is idempotent — it removes and recreates each container it manages
on every run, so re-running after a code change rebuilds and redeploys
cleanly without needing `docker compose` at all.

## Populating demo data

By default a fresh environment only has the one Super Admin bootstrap
account (see `DataSeeder`). To also get a full demo dataset — an admin, 6
students, 3 published courses with modules/lessons, enrollments, daily
tasks (with some marked done), practice problems (with a submission or
two), fee records including a paid invoice + receipt, notifications, and
one sample AI Assistant conversation — set `SEED_DEMO_DATA=true` in `.env`
before running `./docker/run.sh` (see `DemoDataSeeder`). It's idempotent
(skips itself if any course already exists) and off by default — **do not
enable it in production**. Credentials are printed to the backend
container's logs (`docker logs rr-lms-backend`) on first startup.

## Building images without running them (e.g. in CI)

The `Dockerfile`s are unchanged and work standalone:

```bash
docker build -t rr-lms-backend:local ./backend
docker build --build-arg NEXT_PUBLIC_API_BASE_URL=https://api.example.com/api/v1 \
  -t rr-lms-frontend:local ./rr-technosoft-lms
```

This is what the Jenkins `Build Docker Images` stage already does — that
pipeline never depended on Compose, so it's unaffected by this change.
