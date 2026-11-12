/**
 * Shared contracts — the only module every other module depends on, and the one
 * that depends on nothing.
 *
 * <p>The contracts themselves are not written yet. The intended package layout
 * is in place:
 * <ul>
 *   <li>{@code data} — the types that travel between modules.</li>
 *   <li>{@code services} — roles with exactly one active implementation.
 *       "Choose one."</li>
 *   <li>{@code services.spi} — the plugin surface, with many implementations
 *       loaded side by side. "Add as many as you like."</li>
 *   <li>{@code util} — {@link com.buildingassessment.common.util.ServiceLocator}.</li>
 * </ul>
 *
 * <h2>Two things to remember when filling this in</h2>
 *
 * <p><b>Export a package only once it has a class.</b> {@code exports} on an
 * empty package is a compile error, which is why only {@code util} is exported
 * below.
 *
 * <p><b>Every service type needs a {@code uses} clause here — not in the module
 * that wants the service.</b> {@code ServiceLocator} calls
 * {@code ServiceLoader.load(layer, X)} from inside <em>this</em> module, and
 * JPMS resolves the service-use declaration against the calling module. A
 * missing clause fails at runtime, with the providers sitting right there on the
 * module path:
 * <pre>
 *   ServiceConfigurationError: com.example.X: module
 *   com.buildingassessment.common does not declare `uses`
 * </pre>
 * The fix is always a {@code uses} line here, never one in {@code core} or in a
 * plugin.
 *
 * <p>So for each service type added to {@code services} or {@code services.spi}:
 * <pre>{@code
 *   exports com.buildingassessment.common.services;
 *   uses    com.buildingassessment.common.services.YourService;
 * }</pre>
 *
 * <p>Nothing with a third-party dependency belongs in this module. Every plugin
 * inherits its requires clause, so a JSON or JDBC dependency added here is
 * forced onto every sensor plugin in the system.
 */
module common {
    exports com.buildingassessment.common.data;
    exports com.buildingassessment.common.util;
    exports com.buildingassessment.common.services.spi;
}
