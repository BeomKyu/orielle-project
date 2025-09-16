#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <service-domain> [--with-db]" >&2
  exit 1
fi

DOMAIN="$1"; shift || true
WITH_DB=false
for arg in "$@"; do
  if [[ "$arg" == "--with-db" ]]; then WITH_DB=true; fi
done

CAP_DOMAIN="$(tr '[:lower:]' '[:upper:]' <<< ${DOMAIN:0:1})${DOMAIN:1}"
SERVICE_NAME="orielle-${DOMAIN}-service"
GRADLE_PATH="backend/${SERVICE_NAME}"
BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
MOD_DIR="$BASE_DIR/$GRADLE_PATH"

if [[ -d "$MOD_DIR" ]]; then
  echo "[warn] module already exists: $GRADLE_PATH" >&2
  exit 2
fi

echo "[info] creating module structure: $GRADLE_PATH"
mkdir -p "$MOD_DIR/src/main/kotlin/com/orielle/${DOMAIN}"
mkdir -p "$MOD_DIR/src/main/resources"

cat > "$MOD_DIR/build.gradle.kts" <<'EOF'
group = "com.orielle"
version = "0.1.0"

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinSpring)
}

repositories { mavenCentral() }

dependencies {
    implementation(libs.bundles.springBootWebfluxEcosystemInKotiln)
EOF
if $WITH_DB; then
cat >> "$MOD_DIR/build.gradle.kts" <<'EOF'
    implementation(libs.bundles.springBootR2dbcEcosystemInKotlin)
EOF
fi
cat >> "$MOD_DIR/build.gradle.kts" <<'EOF'
    testImplementation(libs.springBootStarterTest)
    testImplementation(kotlin("test"))
}
EOF

cat > "$MOD_DIR/src/main/kotlin/com/orielle/${DOMAIN}/Main.kt" <<EOF
package com.orielle.${DOMAIN}

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ${CAP_DOMAIN}Application

fun main(args: Array<String>) {
    runApplication<${CAP_DOMAIN}Application>(*args)
}
EOF

cat > "$MOD_DIR/src/main/resources/application.yml" <<EOF
spring:
  application:
    name: ${SERVICE_NAME}
  main:
    web-application-type: reactive
server:
  port: 0
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: ${SERVICE_NAME}
EOF

# add to settings.gradle.kts
SETTINGS_FILE="$BASE_DIR/settings.gradle.kts"
if ! grep -q "$SERVICE_NAME" "$SETTINGS_FILE"; then
  echo "include(\"backend:${SERVICE_NAME}\")" >> "$SETTINGS_FILE"
  echo "[info] appended to settings.gradle.kts"
fi

echo "[done] service module scaffolded: $SERVICE_NAME"
