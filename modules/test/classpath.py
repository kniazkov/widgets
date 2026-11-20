"""Compare the runtime dependencies Maven actually resolved for both consumers."""
from os import pathsep
from pathlib import Path
from subprocess import check_output, run
from tempfile import TemporaryDirectory
from zipfile import ZipFile


def contents(path):
    with ZipFile(path) as archive:
        return set(archive.namelist())


def binary_jar(component):
    jars = Path(f"modules/{component}/target").glob(f"widgets-{component}-*.jar")
    jar, = [p for p in jars if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
    return jar


def classpath(consumer):
    paths = Path(f"src/test/{consumer}/target/classpath.txt").read_text().strip()
    jars = [Path(name) for name in paths.split(pathsep)]
    print(f"{consumer}: {len(jars)} JARs, {sum(j.stat().st_size for j in jars):,} bytes")
    for jar in jars:
        print(f"  {jar.name} ({jar.stat().st_size:,} bytes)")
    return jars


minimal = classpath("minimal")
full = classpath("consumer")
entries = set().union(*(contents(jar) for jar in minimal))
for prefix in ("com/kniazkov/widgets/db/", "com/kniazkov/widgets/images/ImageLoader",
               "openize/", "com/drew/", "com/twelvemonkeys/", "org/imgscalr/",
               "org/h2/", "org/sqlite/"):
    assert not any(entry.startswith(prefix) for entry in entries), prefix
assert sum(j.stat().st_size for j in minimal) < sum(j.stat().st_size for j in full)
all_classes = set()
for component in ("core", "http", "db", "images"):
    jar = binary_jar(component)
    classes = {name for name in contents(jar) if name.endswith(".class")}
    assert classes and classes.isdisjoint(all_classes), component
    all_classes.update(classes)

aggregate, = [jar for jar in full if jar.name.startswith("widgets-")]
expected = {name for name in contents(aggregate) if name.endswith(".class")
            and not name.startswith("com/kniazkov/widgets/example/")}
assert expected == all_classes, sorted(expected ^ all_classes)

# Inspect static references as well as the paths exercised by the browser.
runtime = pathsep.join(map(str, minimal))
web_modules = [jar for jar in minimal if jar.name.startswith("widgets-")]
missing = check_output(["jdeps", "--multi-release", "21", "--missing-deps",
                        "--class-path", runtime, *web_modules], text=True)
assert not missing.strip(), missing

with TemporaryDirectory() as temporary:
    directory = Path(temporary)
    run(["javac", "-cp", runtime, "-d", directory, "modules/test/BuildId.java"], check=True)

    def build_id(paths):
        cp = pathsep.join(map(str, [directory, *paths]))
        return check_output(["java", "-cp", cp, "BuildId"], text=True).strip()

    original = build_id(minimal)
    assert original == build_id(reversed(minimal))
    db = binary_jar("db")
    assert build_id([*minimal, db]) != original
    expanded = directory / "expanded"
    for jar in web_modules:
        with ZipFile(jar) as archive:
            archive.extractall(expanded)
    external = [jar for jar in minimal if jar not in web_modules]
    assert build_id([expanded, *external]) == original
