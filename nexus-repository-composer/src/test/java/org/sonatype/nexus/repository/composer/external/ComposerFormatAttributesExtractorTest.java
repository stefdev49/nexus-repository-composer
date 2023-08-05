package org.sonatype.nexus.repository.composer.external;

import junit.framework.TestCase;
import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.repository.storage.TempBlob;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ComposerFormatAttributesExtractorTest extends TestCase {
    // test extractFromZip method from ComposerFormatAttributesExtractor class
    public void testExtractFromZip() throws IOException {
        // given
        String zipFilePath = "src/test/resources/composer.zip";
        Blob blob = mock(Blob.class);
        when(blob.getInputStream()).thenReturn(new FileInputStream(zipFilePath));
        TempBlob tempBlobMock = mock(TempBlob.class);
        when(tempBlobMock.getBlob()).thenReturn(blob);
        ComposerFormatAttributesExtractor composerFormatAttributesExtractor = new ComposerFormatAttributesExtractor(new ComposerJsonExtractor());

        // when
        NestedAttributesMap result = new NestedAttributesMap();
        composerFormatAttributesExtractor.extractFromZip(tempBlobMock, result);

        // then
        assertEquals("rjkip/ftp-php", result.get("name"));
    }
}