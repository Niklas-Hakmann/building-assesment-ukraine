package com.buildingassessment.app;

/**
 * Application entry point and composition root. Wiring of the MQTT ingestion,
 * decoding, registry, reading-store, and realtime-gateway modules into a
 * running service is not implemented yet.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.out.println("Building Assessment Ukraine - sensor backend skeleton starting.");
    }
}
