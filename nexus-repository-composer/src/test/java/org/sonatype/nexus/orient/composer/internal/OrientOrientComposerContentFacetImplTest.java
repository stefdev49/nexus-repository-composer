package org.sonatype.nexus.orient.composer.internal;

import junit.framework.TestCase;

public class OrientOrientComposerContentFacetImplTest extends TestCase {

    public void testGetResultPackageJson() {
        // given
        String path = "packages.json";

        // when
        OrientOrientComposerContentFacetImpl.Result result = OrientOrientComposerContentFacetImpl.getResult(path);

        // then
        assertEquals("packages.json", result.vendor);
        assertEquals("packages.json", result.project);
        assertEquals("latest", result.version);
    }

    public void testGetResultPackageListJson() {
        // given
        String path = "packages/list.json";

        // when
        OrientOrientComposerContentFacetImpl.Result result = OrientOrientComposerContentFacetImpl.getResult(path);

        // then
        assertEquals("packages", result.vendor);
        assertEquals("list.json", result.project);
        assertEquals("latest", result.version);
    }

    public void testGetResultRealPackage() {
        // given
        String path = "vendor/project/1.2.3";

        // when
        OrientOrientComposerContentFacetImpl.Result result = OrientOrientComposerContentFacetImpl.getResult(path);

        // then
        assertEquals("vendor", result.vendor);
        assertEquals("project", result.project);
        assertEquals("1.2.3", result.version);
    }
}