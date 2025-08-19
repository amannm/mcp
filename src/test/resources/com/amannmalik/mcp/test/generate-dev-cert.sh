#!/usr/bin/env bash
set -euo pipefail
KEYSTORE=${1:-keystore.p12}
ALIAS=${2:-mcp}
STOREPASS=${3:-password}
keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS" -alias "$ALIAS" \
  -dname "CN=localhost" -keyalg RSA -keysize 2048 -validity 365 -ext SAN=dns:localhost
