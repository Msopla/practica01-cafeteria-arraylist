#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes build/test-classes
find src/main/java -name '*.java' > build/sources-main.txt
release='-source 8 -target 8'
if javac --help 2>&1 | grep -q -- '--release'; then release='--release 8'; fi
javac $release -Xlint:all,-options -encoding UTF-8 -d build/classes @build/sources-main.txt
jar cfe build/cafeteria.jar ec.edu.ups.cafeteria.Main -C build/classes .
if [ "${1:-}" = '--test' ]; then
    find src/test/java -name '*.java' > build/sources-test.txt
    javac $release -Xlint:all,-options -encoding UTF-8 -cp build/classes -d build/test-classes @build/sources-test.txt
    java -Dfile.encoding=UTF-8 -cp build/classes:build/test-classes ec.edu.ups.cafeteria.PruebasCafeteria
else
    java -Dfile.encoding=UTF-8 -jar build/cafeteria.jar "$@"
fi
