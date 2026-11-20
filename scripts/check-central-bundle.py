"""Verify the Central release bundle produced by mvn deploy -DskipUpload=true."""
import hashlib
import io
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile


def check_signature(bundle, path, workspace):
    data = workspace / "artifact"
    signature = workspace / "artifact.asc"
    data.write_bytes(bundle.read(path))
    signature.write_bytes(bundle.read(path + ".asc"))
    subprocess.run(["gpg", "--batch", "--verify", signature, data], check=True)


def check_classifiers(bundle, base):
    with ZipFile(io.BytesIO(bundle.read(base + ".jar"))) as binary:
        classes = {name[:-6] for name in binary.namelist()
                   if name.endswith(".class") and "$" not in name}
    with ZipFile(io.BytesIO(bundle.read(base + "-sources.jar"))) as sources:
        java = {name[:-5] for name in sources.namelist() if name.endswith(".java")}
    assert classes and java == classes, (base, sorted(java ^ classes))
    with ZipFile(io.BytesIO(bundle.read(base + "-javadoc.jar"))) as docs:
        assert "index.html" in docs.namelist(), base
        pages = {name[:-5] for name in docs.namelist() if name.endswith(".html")}
        assert pages & classes, f"No API pages for {base}"
        for page in pages:
            if page.startswith("com/kniazkov/widgets/") and "/class-use/" not in page:
                enclosing = page.split(".")[0]
                if not page.endswith(("/package-summary", "/package-use", "/package-tree")):
                    assert enclosing in classes, (base, page)


def main():
    ns = "{http://maven.apache.org/POM/4.0.0}"
    version = ET.parse("modules/pom.xml").getroot().findtext(ns + "version")
    components = {"widgets-modules", "widgets", "widgets-core", "widgets-http",
                  "widgets-db", "widgets-images"}
    path = Path("modules/target/central-publishing/central-bundle.zip")
    with ZipFile(path) as bundle, tempfile.TemporaryDirectory() as tmp:
        files = {name for name in bundle.namelist() if not name.endswith("/")}
        primaries = {name for name in files if name.endswith((".pom", ".jar"))}
        expected = set()
        for artifact in sorted(components):
            base = f"com/kniazkov/{artifact}/{version}/{artifact}-{version}"
            expected.add(base + ".pom")
            if artifact != "widgets-modules":
                expected.update(base + suffix for suffix in (".jar", "-sources.jar", "-javadoc.jar"))
                check_classifiers(bundle, base)
        assert expected == primaries, sorted(expected ^ primaries)
        for name in sorted(expected):
            data = bundle.read(name)
            check_signature(bundle, name, Path(tmp))
            for algorithm in ("md5", "sha1", "sha256", "sha512"):
                digest = hashlib.new(algorithm, data).hexdigest()
                assert bundle.read(name + "." + algorithm).decode().strip() == digest, name
    print(f"Verified {len(components)} components and all {len(expected)} signed POM/JAR files.")


if __name__ == "__main__":
    main()
