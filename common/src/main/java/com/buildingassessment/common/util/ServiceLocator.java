package com.buildingassessment.common.util;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves service implementations from two places at once: the boot layer
 * (modules on {@code --module-path}) and a child layer built at startup from the
 * JARs in {@code plugins/}.
 *
 * <p>The child layer is what makes a sensor plugin a drop-in. A JAR added to
 * {@code plugins/} between runs is picked up with no build and no restart of
 * anything but the process; the host is never recompiled because it never names
 * the plugin.
 *
 * <p>An absent or empty {@code plugins/} directory is not an error — the system
 * then runs with whatever is in the boot layer alone.
 */
public enum ServiceLocator {
    INSTANCE;

    private final ModuleLayer pluginLayer;
    private final List<String> pluginModules;
    private final Map<Class<?>, List<?>> cache = new ConcurrentHashMap<>();

    /**
     * Resolved relative to the working directory, which `mvn -N exec:exec` sets
     * to the repo root. A method rather than a static field because an enum
     * constructor may not read its own type's static fields.
     */
    private static Path pluginDir() {
        return Path.of("plugins");
    }

    ServiceLocator() {
        Path pluginDir = pluginDir();
        ModuleFinder finder = Files.isDirectory(pluginDir)
                ? ModuleFinder.of(pluginDir)
                : ModuleFinder.of();

        pluginModules = finder.findAll().stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .sorted()
                .toList();

        if (pluginModules.isEmpty()) {
            pluginLayer = ModuleLayer.boot();
        } else {
            Configuration configuration = ModuleLayer.boot()
                    .configuration()
                    .resolve(finder, ModuleFinder.of(), pluginModules);

            // Many loaders, not one: each plugin gets its own class loader, so two
            // plugins can bundle different versions of the same library without
            // colliding. The system loader is the parent, which is how a plugin
            // reads `common` from the boot layer.
            pluginLayer = ModuleLayer.boot()
                    .defineModulesWithManyLoaders(configuration, ClassLoader.getSystemClassLoader());
        }
    }

    /** Module names discovered in {@code plugins/}, for startup logging. */
    public List<String> pluginModuleNames() {
        return pluginModules;
    }

    /**
     * All implementations of {@code service} visible from the plugin layer,
     * which by inheritance includes those in the boot layer.
     *
     * <p>Requires a matching {@code uses} directive in this module's
     * {@code module-info.java}: {@link ServiceLoader#load(ModuleLayer, Class)}
     * checks the declaration against the calling module, and the caller here is
     * always {@code com.buildingassessment.common}.
     *
     * <p>A provider that throws during construction is logged and skipped rather
     * than taking the whole startup down — one broken plugin JAR should not
     * stop the other sensors from reporting.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> locateAll(Class<T> service) {
        return (List<T>) cache.computeIfAbsent(service, this::load);
    }

    private <T> List<T> load(Class<T> service) {
        List<T> found = new ArrayList<>();
        var iterator = ServiceLoader.load(pluginLayer, service).iterator();
        while (true) {
            try {
                if (!iterator.hasNext()) {
                    break;
                }
                found.add(iterator.next());
            } catch (ServiceConfigurationError e) {
                System.err.printf("[locator] skipping a %s provider: %s%n",
                        service.getSimpleName(), e.getMessage());
            }
        }
        return List.copyOf(found);
    }

    /**
     * The single implementation of a "choose one" service role.
     *
     * @throws IllegalStateException if more than one provider is present — an
     *         ambiguous store or publisher is a configuration error, not
     *         something to resolve by picking arbitrarily
     */
    public <T> java.util.Optional<T> locateOne(Class<T> service) {
        List<T> all = locateAll(service);
        if (all.size() > 1) {
            throw new IllegalStateException(
                    "expected at most one %s provider but found %d".formatted(
                            service.getSimpleName(), all.size()));
        }
        return all.stream().findFirst();
    }
}
