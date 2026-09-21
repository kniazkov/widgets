/*
 * Copyright (c) 2026 Ivan Kniazkov
 */
package com.kniazkov.widgets.base;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests each source boundary independently of HTTP path validation.
 */
public class StaticSourceTest {
    /**
     * Generating a cache URL uses the same confinement rules as serving the resource.
     */
    @Test
    public void versionedUrlsCannotReadOutsideSource() throws Exception {
        final Path root = this.folder.newFolder().toPath();
        final StaticSource source = StaticSource.directory("/images", root);
        for (final String path : new String[]{"../secret.svg", "/secret.svg",
                "a.svg?x=1", "a.svg#fragment", "%2e%2e/secret.svg"}) {
            assertThrows(SecurityException.class, () -> source.versionedUrl(path));
        }
        assertThrows(NoSuchFileException.class, () -> source.versionedUrl("missing.svg"));
    }

    /**
     * Versioned URLs cannot follow a symlink outside the mounted root.
     */
    @Test
    public void versionedUrlsRejectExternalSymlinks() throws Exception {
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path root = this.folder.newFolder().toPath();
        final Path secret = this.folder.newFile().toPath();
        Files.createSymbolicLink(root.resolve("escape.svg"), secret);
        final StaticSource source = StaticSource.directory("/images", root);
        assertThrows(SecurityException.class, () -> source.versionedUrl("escape.svg"));
    }

    /**
     * Isolated public and private trees.
     */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * Binary files and Unicode names survive unchanged; mount matching uses segments.
     */
    @Test
    public void readsOnlyFilesInsideMount() throws Exception {
        final Path root = this.folder.newFolder().toPath();
        final byte[] bytes = {0, 1, -1, 127};
        Files.write(root.resolve("фото 1.png"), bytes);
        final StaticSource source = StaticSource.directory("/media", root);
        assertEquals("/media/", source.getPrefix());
        assertTrue(source.matches("/media"));
        assertFalse(source.matches("/media-other/a"));
        assertArrayEquals(bytes, source.read("/media/фото 1.png"));
        for (final String name : new String[]{"/media", "/media/", "/media/missing",
                "/media-other/фото 1.png"}) {
            assertThrows(name, NoSuchFileException.class, () -> source.read(name));
        }
        Files.createDirectory(root.resolve("directory"));
        assertThrows(NoSuchFileException.class, () -> source.read("/media/directory"));
    }

    /**
     * Traversal, second decoding, URI syntax and platform-specific paths are rejected.
     */
    @Test
    public void rejectsAmbiguousRelativePaths() throws Exception {
        final StaticSource source = StaticSource.directory("/public",
            this.folder.getRoot().toPath());
        for (final String path : new String[]{"../secret", "a/../../secret", "./secret",
                "a//b", "/etc/passwd", "a/", "a\\b", "C:secret", "%2e%2e/secret",
                "a?b", "a#b", "a!b", "a\u0000b", "a\nb"}) {
            assertThrows(path, SecurityException.class, () -> source.read("/public/" + path));
        }
    }

    /**
     * Real paths prevent file, directory and common-prefix sibling symlink escapes.
     */
    @Test
    public void confinesSymlinksToRealRoot() throws Exception {
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path root = this.folder.newFolder("public").toPath();
        final Path sibling = this.folder.newFolder("public-private").toPath();
        Files.writeString(sibling.resolve("secret"), "secret");
        Files.writeString(root.resolve("ok"), "public");
        Files.createSymbolicLink(root.resolve("file"), sibling.resolve("secret"));
        Files.createSymbolicLink(root.resolve("dir"), sibling);
        Files.createSymbolicLink(root.resolve("inside"), root.resolve("ok"));
        final Path alias = this.folder.getRoot().toPath().resolve("alias");
        Files.createSymbolicLink(alias, root);
        final StaticSource source = StaticSource.directory("/files", alias);
        assertThrows(SecurityException.class, () -> source.read("/files/file"));
        assertThrows(SecurityException.class, () -> source.read("/files/dir/secret"));
        assertArrayEquals("public".getBytes(StandardCharsets.UTF_8), source.read("/files/inside"));
    }

    /**
     * Invalid mounts fail during configuration, and options retain immutable snapshots.
     */
    @Test
    public void validatesConfiguration() {
        final Path root = this.folder.getRoot().toPath();
        for (final String prefix : new String[]{"", "/", "relative", "//", "/a//b",
                "/a/..", "/a%20b", "/scripts", "/fonts/sub", "/index.html", "/style.css"}) {
            assertThrows(prefix, IllegalArgumentException.class,
                () -> StaticSource.directory(prefix, root));
        }
        assertThrows(IllegalArgumentException.class,
            () -> StaticSource.classpath("/a", getClass(), "/"));
        assertThrows(NullPointerException.class, () -> StaticSource.directory("/a", null));
        assertThrows(NullPointerException.class, () -> StaticSource.classpath("/a", null, "/b"));
        final StaticSource source = StaticSource.directory("/a", root);
        final Options.Builder builder = new Options.Builder().addStaticSource(source);
        final Options options = builder.build();
        assertThrows(IllegalArgumentException.class,
            () -> builder.addStaticSource(StaticSource.directory("/a/", root)));
        assertThrows(NullPointerException.class, () -> builder.addStaticSource(null));
        builder.addStaticSource(StaticSource.directory("/b", root));
        assertEquals(1, options.getStaticSources().size());
        assertThrows(UnsupportedOperationException.class, () -> options.getStaticSources().clear());
    }

    /**
     * Exploded resources expose only their explicitly mounted public subtree.
     */
    @Test
    public void readsExplodedClasspathWithoutExposingPrivateEntries() throws Exception {
        final Path root = this.folder.newFolder().toPath();
        final Path publicRoot = Files.createDirectory(root.resolve("public"));
        Files.writeString(publicRoot.resolve("ok.txt"), "public");
        final Path classFile = root.resolve("com/kniazkov/widgets/base/StaticAnchor.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, anchorBytes());
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            final StaticSource source = StaticSource.classpath("/assets",
                loader.loadClass(StaticAnchor.class.getName()), "/public");
            assertArrayEquals("public".getBytes(StandardCharsets.UTF_8),
                source.read("/assets/ok.txt"));
            assertThrows(NoSuchFileException.class,
                () -> source.read("/assets/com/kniazkov/widgets/base/StaticAnchor.class"));
        }
    }

    /**
     * Exploded classpath roots reject symlinks to external files.
     */
    @Test
    public void explodedClasspathRejectsExternalSymlink() throws Exception {
        SymlinkTestSupport.assumeSupported(this.folder.getRoot().toPath());
        final Path root = this.folder.newFolder().toPath();
        final Path publicRoot = Files.createDirectory(root.resolve("public"));
        final Path secret = this.folder.newFile().toPath();
        Files.createSymbolicLink(publicRoot.resolve("escape.txt"), secret);
        final Path classFile = root.resolve("com/kniazkov/widgets/base/StaticAnchor.class");
        Files.createDirectories(classFile.getParent());
        Files.write(classFile, anchorBytes());
        try (URLClassLoader loader = new URLClassLoader(new URL[]{root.toUri().toURL()}, null)) {
            final StaticSource source = StaticSource.classpath("/assets",
                loader.loadClass(StaticAnchor.class.getName()), "/public");
            assertThrows(SecurityException.class, () -> source.read("/assets/escape.txt"));
        }
    }

    /**
     * A real JAR without directory entries serves only its explicitly mounted subtree.
     */
    @Test
    public void readsPackagedJarWithoutExposingPrivateEntries() throws Exception {
        final Path archive = this.folder.getRoot().toPath().resolve("resources with spaces.jar");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(archive))) {
            jar.putNextEntry(new JarEntry("com/kniazkov/widgets/base/StaticAnchor.class"));
            jar.write(anchorBytes());
            jar.closeEntry();
            for (final String name : new String[]{"public/фото 1.txt", "secret.txt"}) {
                jar.putNextEntry(new JarEntry(name));
                jar.write(name.getBytes(StandardCharsets.UTF_8));
                jar.closeEntry();
            }
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[]{archive.toUri().toURL()}, null)) {
            final StaticSource source = StaticSource.classpath("/assets",
                loader.loadClass(StaticAnchor.class.getName()), "/public");
            assertArrayEquals("public/фото 1.txt".getBytes(StandardCharsets.UTF_8),
                source.read("/assets/фото 1.txt"));
            assertThrows(NoSuchFileException.class, () -> source.read("/assets/secret.txt"));
            assertThrows(SecurityException.class, () -> source.read("/assets/../secret.txt"));
        }
    }

    /**
     * Gets a dependency-free class for isolated classloader fixtures.
     */
    private static byte[] anchorBytes() throws Exception {
        try (InputStream input = StaticAnchor.class.getResourceAsStream("StaticAnchor.class")) {
            return input.readAllBytes();
        }
    }

    /**
     * Deployment can register a data directory before it exists.
     */
    @Test
    public void permitsLateDirectoryCreation() throws Exception {
        final Path root = this.folder.getRoot().toPath().resolve("later");
        final StaticSource source = StaticSource.directory("/data", root);
        assertThrows(NoSuchFileException.class, () -> source.read("/data/file.txt"));
        Files.createDirectory(root);
        Files.writeString(root.resolve("file.txt"), "created");
        assertArrayEquals("created".getBytes(StandardCharsets.UTF_8),
            source.read("/data/file.txt"));
    }
}
