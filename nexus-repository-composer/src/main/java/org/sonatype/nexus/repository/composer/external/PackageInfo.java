package org.sonatype.nexus.repository.composer.external;

import org.sonatype.nexus.common.collect.NestedAttributesMap;

import javax.annotation.Nullable;

import static org.sonatype.nexus.repository.composer.external.ComposerAttributes.P_NAME;
import static org.sonatype.nexus.repository.composer.external.ComposerAttributes.P_VERSION;

/**
 * Class with vendor, project, version and filename string attributes
 */
public class PackageInfo {
    public String vendor;
    public String project;
    public String version;
    public String filename;
    public String name;

    public PackageInfo(String vendor, String project, String version) {
        this.vendor = vendor;
        this.project = project;
        this.version = version;
        this.name = this.vendor + "/" + this.project;
    }

    // constructor using NestedAttributesMap
    public PackageInfo(NestedAttributesMap attributes) {
        this.name = attributes.get(P_NAME, String.class);
        // split name into vendor and project
        if ((this.name != null) && (this.name.contains("/"))) {
            String[] nameParts = this.name.split("/");
            this.vendor = nameParts[0];
            this.project = nameParts[1];
        }
        this.version = attributes.get(P_VERSION, String.class);
    }

    // generate setters and getters
    public String getVendor() {
        return vendor;
    }
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getProject() {
        return project;
    }
    public void setProject(String project) {
        this.project = project;
    }

    public String getVersion() {
        return version;
    }
    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

}
