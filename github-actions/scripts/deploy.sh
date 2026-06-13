#!/usr/bin/env bash
set -euo pipefail

HOST=""
USER=""
KEY=""
JAR=""
REMOTE_PATH=""
APP_NAME=""
ENV_NAME="dev"

usage() {
  echo "Uso: $0 --host HOST --user USER --key KEY --jar JAR --remote-path PATH --app-name NAME [--env ENV]"
  exit 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --host) HOST="$2"; shift 2 ;;
    --user) USER="$2"; shift 2 ;;
    --key) KEY="$2"; shift 2 ;;
    --jar) JAR="$2"; shift 2 ;;
    --remote-path) REMOTE_PATH="$2"; shift 2 ;;
    --app-name) APP_NAME="$2"; shift 2 ;;
    --env) ENV_NAME="$2"; shift 2 ;;
    *) usage ;;
  esac
done

[[ -z "$HOST" || -z "$USER" || -z "$KEY" || -z "$JAR" || -z "$REMOTE_PATH" || -z "$APP_NAME" ]] && usage
[[ ! -f "$JAR" ]] && { echo "JAR no encontrado: $JAR"; exit 1; }

if [[ -f "$KEY" ]]; then
  KEY_FILE="$KEY"
else
  KEY_FILE=$(mktemp)
  trap 'rm -f "$KEY_FILE"' EXIT
  printf '%s\n' "$KEY" > "$KEY_FILE"
  chmod 600 "$KEY_FILE"
fi

SSH_OPTS=(-i "$KEY_FILE" -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null)

echo "[deploy] Copiando artefacto a ${USER}@${HOST}:${REMOTE_PATH}/releases/"
ssh "${SSH_OPTS[@]}" "${USER}@${HOST}" "mkdir -p ${REMOTE_PATH}/releases ${REMOTE_PATH}/current"

scp "${SSH_OPTS[@]}" "$JAR" "${USER}@${HOST}:${REMOTE_PATH}/releases/${APP_NAME}.jar"

echo "[deploy] Reiniciando servicio en ${ENV_NAME}"
ssh "${SSH_OPTS[@]}" "${USER}@${HOST}" bash -s <<EOF
set -euo pipefail
ln -sfn ${REMOTE_PATH}/releases/${APP_NAME}.jar ${REMOTE_PATH}/current/${APP_NAME}.jar

if command -v systemctl >/dev/null 2>&1 && systemctl list-units --type=service | grep -q ${APP_NAME}; then
  sudo systemctl restart ${APP_NAME}
else
  pkill -f "${APP_NAME}.jar" || true
  nohup java -jar ${REMOTE_PATH}/current/${APP_NAME}.jar \\
    --spring.profiles.active=${ENV_NAME} \\
    > ${REMOTE_PATH}/app.log 2>&1 &
fi

echo "Despliegue completado: ${APP_NAME} (${ENV_NAME})"
EOF
