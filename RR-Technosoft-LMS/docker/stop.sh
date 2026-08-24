#!/usr/bin/env bash
#
# RR Technosoft LMS — stop and remove containers started by run.sh.
#
# Usage:
#   ./docker/stop.sh              # stop + remove app + monitoring containers
#   ./docker/stop.sh --purge      # also remove volumes (postgres data, prometheus/grafana data)

set -euo pipefail

CONTAINERS=(rr-lms-frontend rr-lms-backend rr-lms-redis rr-lms-postgres rr-lms-grafana rr-lms-prometheus rr-lms-alertmanager rr-lms-postgres-exporter rr-lms-redis-exporter)
VOLUMES=(rr-technosoft-lms_pgdata rr-technosoft-lms_prometheus-data rr-technosoft-lms_grafana-data rr-technosoft-lms_alertmanager-data)
NETWORK="rr-technosoft-lms_lms-net"

for c in "${CONTAINERS[@]}"; do
  if docker inspect "$c" >/dev/null 2>&1; then
    echo "[stop.sh] Removing container $c"
    docker rm -f "$c" >/dev/null
  fi
done

if [[ "${1:-}" == "--purge" ]]; then
  for v in "${VOLUMES[@]}"; do
    if docker volume inspect "$v" >/dev/null 2>&1; then
      echo "[stop.sh] Removing volume $v"
      docker volume rm "$v" >/dev/null
    fi
  done
fi

if docker network inspect "$NETWORK" >/dev/null 2>&1; then
  echo "[stop.sh] Removing network $NETWORK"
  docker network rm "$NETWORK" >/dev/null 2>&1 || echo "[stop.sh] Network still in use, skipping"
fi

echo "[stop.sh] Done."
