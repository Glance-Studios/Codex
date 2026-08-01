package com.glance.codex.bootstrap;

import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class CodexLibLoader implements PluginLoader {

    private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2";

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        PluginLibraries libs = load();

        libs.dependencies().forEach(d ->
                resolver.addDependency(new Dependency(new DefaultArtifact(d), null)));

        libs.repositories().forEach((id, url) ->
                resolver.addRepository(
                        new RemoteRepository.Builder(id, "default", mirror(url)).build()));

        classpathBuilder.addLibrary(resolver);
    }

    /**
     * Gradle writes Maven Central into paper-libraries.json, but resolving against it directly is
     * against Maven Central's terms of service, so Paper logs a RuntimeException stack trace on
     * every boot for it. Redirect to the mirror Paper asks plugins to use.
     */
    private static String mirror(String url) {
        return url.startsWith(MAVEN_CENTRAL)
                ? MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
                : url;
    }

    private PluginLibraries load() {
        try (var in = getClass().getResourceAsStream("/paper-libraries.json")) {
            if (in == null) {
                throw new IllegalStateException("paper-libraries.json missing");
            }
            return new Gson().fromJson(
                new InputStreamReader(in, StandardCharsets.UTF_8),
                PluginLibraries.class
            );
        } catch (Exception e) {
            throw new LibraryLoadingException("Failed to load Paper Libraries", e);
        }
    }

    private record PluginLibraries(
        Map<String, String> repositories,
        List<String> dependencies
    ) {}

}
