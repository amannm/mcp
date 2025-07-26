#!/usr/bin/env bash
set -eu

# https://github.com/modelcontextprotocol/modelcontextprotocol

SPEC_ROOT="${HOME}/IdeaProjects/modelcontextprotocol"

attach() {
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

detach() {
  rm -rf spec
}

command="${1:-attach}"
case "$command" in
    attach|detach)
        "$command"
        ;;
    *)
        echo "Usage: $0 {attach|detach}" >&2
        exit 1
        ;;
esac