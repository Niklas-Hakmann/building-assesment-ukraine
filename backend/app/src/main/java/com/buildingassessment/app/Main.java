package com.buildingassessment.app;

import com.buildingassessment.common.data.SensorDescriptor;
import com.buildingassessment.common.data.SensorType;
import com.buildingassessment.common.services.MessageSource;
import com.buildingassessment.common.services.MessageSourceException;
import com.buildingassessment.common.services.ReadingStore;
import com.buildingassessment.common.services.SensorCatalog;
import com.buildingassessment.common.services.spi.SensorDecoder;
import com.buildingassessment.common.services.spi.SensorPlugin;

import java.util.List;
import java.util.Map;

/**
 * Composition root: resolves the modules, lets the sensor plugins register
 * themselves, and starts the message source.
 *
 * <p>Depends on {@code common} alone. It names no sensor type and no
 * implementation, which is what lets a sensor plugin be added or removed
 * without this module being recompiled.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        boolean demo = List.of(args).contains("--demo");
        ModuleConfig config = new ModuleConfig();

        SensorCatalog catalog = config.sensorCatalog();
        ReadingStore store = config.readingStore();
        Map<SensorType, SensorDecoder> decoders = config.decoders();

        List<SensorPlugin> plugins = config.plugins();
        plugins.forEach(plugin -> plugin.register(catalog));

        System.out.printf("[app] %d sensor plugin(s), decoders for %s, %d sensor(s) registered%n",
                plugins.size(), decoders.keySet(), catalog.all().size());
        catalog.all().stream()
                .sorted((a, b) -> a.id().value().compareTo(b.id().value()))
                .forEach(s -> System.out.printf("[app]   %s  %-12s %s%s%n",
                        s.id(), s.type(), s.displayName(), s.simulated() ? " (simulated)" : ""));

        SensorPipeline pipeline = new SensorPipeline(decoders, store, config.readingPublisher());

        try (MessageSource source = demo
                ? new DemoMessageSource()
                : config.messageSource().orElseThrow(() -> new MessageSourceException(
                        "no MessageSource provider found; run with --demo to exercise the pipeline"))) {
            source.start(pipeline::accept);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> plugins.forEach(SensorPlugin::shutdown)));

        System.out.printf("[app] stored readings: %d%n", store.latestAll().size());
        for (SensorDescriptor sensor : catalog.all()) {
            store.latest(sensor.id()).ifPresent(reading ->
                    System.out.printf("[app]   %s -> %s%n", sensor.id(), reading.measurements()));
        }
    }
}
