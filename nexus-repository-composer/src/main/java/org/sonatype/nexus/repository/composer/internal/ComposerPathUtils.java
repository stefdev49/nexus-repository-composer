package org.sonatype.nexus.repository.composer.internal;

public class ComposerPathUtils {
    // private constructor
    private ComposerPathUtils() {
    }

    /**
     * Build the path to the zip file for a given package.
     * @param packageVendor The vendor of the package
     * @param packageProject The project name of the package
     * @param packageVersion The version of the package
     * @return The path to the zip file
     */
    public static String buildZipballPath(String packageVendor, String packageProject, String packageVersion) {
        return packageVendor + "/" + packageProject + "/" + packageVersion + "/" + packageVendor + "-" + packageProject + "-" + packageVersion + ".zip";
    }
}
