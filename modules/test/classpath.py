"""Compare the runtime dependencies Maven actually resolved for both consumers."""
from os import pathsep
from pathlib import Path
from zipfile import ZipFile


def contents(path):
    with ZipFile(path) as archive:
        return set(archive.namelist())


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
    jar, = Path(f"modules/{component}/target").glob("widgets-*.jar")
    classes = {name for name in contents(jar) if name.endswith(".class")}
    assert classes and classes.isdisjoint(all_classes), component
    all_classes.update(classes)

aggregate, = [jar for jar in full if jar.name.startswith("widgets-")]
expected = {name for name in contents(aggregate) if name.endswith(".class")
            and not name.startswith("com/kniazkov/widgets/example/")}
assert expected == all_classes, sorted(expected ^ all_classes)
