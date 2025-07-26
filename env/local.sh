#!/usr/bin/env bash
set -eu

# https://github.com/modelcontextprotocol/modelcontextprotocol

SPEC_ROOT="${HOME}/IdeaProjects/modelcontextprotocol"


latest() {
  mkdir -p spec/2025-06-18
  ln -s "${SPEC_ROOT}"/docs/specification/2025-06-18/* spec/2025-06-18/
  ln -s "${SPEC_ROOT}"/schema/2025-06-18/* spec/2025-06-18/
}

all() {
  mkdir -p spec/2024-11-05
  ln -s "${SPEC_ROOT}"/docs/specification/2024-11-05/* spec/2024-11-05/
  ln -s "${SPEC_ROOT}"/schema/2024-11-05/* spec/2024-11-05/

  mkdir -p spec/2025-03-26
  ln -s "${SPEC_ROOT}"/docs/specification/2025-03-26/* spec/2025-03-26/
  ln -s "${SPEC_ROOT}"/schema/2025-03-26/* spec/2025-03-26/

  mkdir -p spec/2025-06-18
  ln -s "${SPEC_ROOT}"/docs/specification/2025-06-18/* spec/2025-06-18/
  ln -s "${SPEC_ROOT}"/schema/2025-06-18/* spec/2025-06-18/
}

clear() {
  rm -rf spec
}

command="${1:-latest}"
case "$command" in
    clear|latest|all)
        "$command"
        ;;
    *)
        echo "Usage: $0 {clear|latest|all}" >&2
        exit 1
        ;;
esac