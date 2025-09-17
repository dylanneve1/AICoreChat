#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi

usage() {
  cat <<USAGE
Usage: ./run_all.sh [all|qa|assemble|connected|tests]

  all        Run clean build, formatting, static analysis, unit tests, and assembleDebug
  qa         Run formatting + static analysis + lint + unit tests (same as scripts/quality.sh)
  assemble   Build debug APK (assembleDebug)
  connected  Run connectedDebugAndroidTest (requires a device/emulator)
  tests      Run unit tests only (test)
USAGE
}

COMMAND=${1:-all}

case "$COMMAND" in
  all)
    TASKS=(clean spotlessCheck detekt lint test assembleDebug)
    ;;
  qa)
    ./scripts/quality.sh
    exit 0
    ;;
  assemble)
    TASKS=(assembleDebug)
    ;;
  connected)
    TASKS=(connectedDebugAndroidTest)
    ;;
  tests)
    TASKS=(test)
    ;;
  -h|--help|help)
    usage
    exit 0
    ;;
  *)
    echo "Unknown command: $COMMAND" >&2
    usage
    exit 1
    ;;
 esac

./gradlew --no-daemon "${TASKS[@]}"
