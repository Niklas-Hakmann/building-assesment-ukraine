// No `exports` yet: the package currently only has a package-info.java,
// and javac does not allow exporting a package with no actual type in it.
// Add the export once a real public type is added.
module com.buildingassessment.readingstore {
    requires com.buildingassessment.core;
}
