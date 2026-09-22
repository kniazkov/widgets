#!/usr/bin/env bash
set -euo pipefail

if [[ $# -eq 0 || -z "${1:-}" || "${1:-}" == -* ]]; then
    echo "Usage: $0 <fully.qualified.MainClass> [application arguments...]" >&2
    exit 2
fi

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd -- "$project_dir"

./build.sh

classpath="target/classes"
dependencies="$(< target/runtime-classpath.txt)"
if [[ -n "$dependencies" ]]; then
    classpath+=":$dependencies"
fi

java_command=java
if [[ -n "${JAVA_HOME:-}" ]]; then
    java_command="$JAVA_HOME/bin/java"
fi
exec "$java_command" -cp "$classpath" "$@"
