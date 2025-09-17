#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi

./gradlew --no-daemon spotlessCheck detekt lint test
