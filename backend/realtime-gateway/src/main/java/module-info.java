/**
 * Pushes readings towards the dashboard.
 *
 * <p>Requires only {@code common}: it reads through the {@code ReadingStore}
 * contract and has no compile-time knowledge of the module that implements it.
 */
module com.buildingassessment.realtimegateway {
    requires com.buildingassessment.common;

    provides com.buildingassessment.common.services.ReadingPublisher
            with com.buildingassessment.realtimegateway.internal.ConsoleReadingPublisher;
}
