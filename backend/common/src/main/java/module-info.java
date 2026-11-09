/**
 * Shared contracts — the only module every other module depends on, and the one
 * that depends on nothing.
 *
 * <p>Three kinds of agreement, kept in separate packages because they behave
 * differently:
 * <ul>
 *   <li>{@code data} — the types that travel between modules.</li>
 *   <li>{@code services} — roles with exactly one active implementation
 *       (store, publisher, message source). "Choose one."</li>
 *   <li>{@code services.spi} — the plugin surface, with many implementations
 *       loaded side by side. "Add as many as you like."</li>
 * </ul>
 *
 * <p>No `uses` directives: {@link com.buildingassessment.common.util.ServiceLocator}
 * declares service use at runtime via {@code Module#addUses}, which keeps this
 * module free of any dependency on the modules that define or implement SPIs.
 *
 * <p>Nothing with a third-party dependency belongs here. Every plugin inherits
 * this module's requires clause, so a JSON or JDBC dependency added here is
 * forced onto every sensor plugin in the system.
 */
module com.buildingassessment.common {
    exports com.buildingassessment.common.data;
    exports com.buildingassessment.common.services;
    exports com.buildingassessment.common.services.spi;
    exports com.buildingassessment.common.util;
}
