#!/usr/bin/env bash
# Build probe servers without running tests or publishing artifacts.
set -euo pipefail

build_log=$(mktemp)
trap 'rm -f "$build_log"' EXIT

for attempt in 1 2 3; do
    # -U also invalidates cached "artifact not found" responses from Central.
    if mvn -B -ntp -U -Dstyle.color=never "$@" -DskipTests test-compile \
        dependency:build-classpath -Dmdep.outputFile=target/runtime-classpath.txt \
        >"$build_log" 2>&1; then
        cat "$build_log"
        exit 0
    else
        status=$?
    fi
    cat "$build_log"

    # Retry repository resolution failures only, never compiler or test failures.
    if (( attempt == 3 )) || ! grep -Eq \
        '^\[ERROR\].*(Could not (find|transfer) artifact|PluginResolutionException|DependencyResolutionException|Unresolveable build extension)' \
        "$build_log"; then
        exit "$status"
    fi
    echo "Maven artifact resolution failed; retrying ($attempt/3)." >&2
    sleep "$((attempt * 5))"
done
