/**
 * Stores the latest reading per sensor. The implementation is not exported —
 * consumers reach it only through {@code ReadingStore} in {@code common}, which
 * is what keeps the realtime gateway from depending on this module.
 */
module com.buildingassessment.readingstore {
    requires com.buildingassessment.common;

    provides com.buildingassessment.common.services.ReadingStore
            with com.buildingassessment.readingstore.internal.InMemoryReadingStore;
}
