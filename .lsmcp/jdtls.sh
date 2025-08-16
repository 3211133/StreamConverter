#!/usr/bin/env bash
set -euo pipefail
JDTLS_HOME="/opt/jdtls"
CONFIG="$HOME/.cache/jdtls-config"
DATA="$PWD/.lsmcp/workspace"
mkdir -p "$CONFIG" "$DATA"

# Copy config.ini from system location if it doesn't exist
if [ ! -f "$CONFIG/config.ini" ]; then
  cp "/opt/jdtls/config_linux/config.ini" "$CONFIG/"
fi

exec "$JDTLS_HOME/bin/jdtls" \
  --jvm-arg=-Dlog.level=WARNING \
  --jvm-arg=-Xmx1G \
  -configuration "$CONFIG" \
  -data "$DATA" \
  "$@"
