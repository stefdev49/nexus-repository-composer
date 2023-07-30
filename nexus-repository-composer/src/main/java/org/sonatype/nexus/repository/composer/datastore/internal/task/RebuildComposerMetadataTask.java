/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.datastore.internal.task;

import java.io.IOException;
import java.util.Collections;
import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.RepositoryTaskSupport;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.composer.datastore.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.datastore.internal.data.ComposerKeyValueFacet;
import org.sonatype.nexus.repository.composer.datastore.internal.hosted.metadata.ComposerHostedMetadataFacet;
import org.sonatype.nexus.repository.content.fluent.FluentAsset;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.scheduling.Cancelable;
import org.sonatype.nexus.scheduling.CancelableHelper;

@Named
public class RebuildComposerMetadataTask
    extends RepositoryTaskSupport
    implements Cancelable
{
  @Override
  protected void execute(final Repository repository) {
    log.debug("Populating metadata in repository {} started", repository.getName());

    boolean isFullRebuild = getConfiguration()
        .getBoolean(RebuildComposerMetadataTaskDescriptor._COMPOSERMETADATA_FULL_REBUILD, false);

    executeRebuild(repository, isFullRebuild);
  }

  private void executeRebuild(final Repository repository, boolean isFullRebuild) {
    if (isFullRebuild) {
      // Remove all data in key-value storage
      data(repository).removeAllPackageMetadata();
    }

    // Get all assets
    Iterable<FluentAsset> assets = content(repository).getComposerPackageAssets();

    // Add metadata from each asset into key-value table
    for (FluentAsset asset : assets) {
      CancelableHelper.checkCancellation();
      metadata(repository).addPackageMetadata(asset);
    }

    // Remove Release index file
    metadata(repository).removeInReleaseIndex();

    // Rebuild index files
    try {
      metadata(repository).rebuildMetadata(Collections.emptyList());
    }
    catch (IOException e) {
      log.error("Error index rebuilding", log.isDebugEnabled() ? e : null);
    }
  }

  @Override
  protected boolean appliesTo(final Repository repository) {
    return repository.getFormat().getValue().equals(ComposerFormat.NAME) &&
        repository.getType().getValue().equals(HostedType.NAME);
  }

  @Override
  public String getMessage() {
    return "Rebuilding Composer metadata in " + getRepositoryField();
  }

  private ComposerContentFacet content(final Repository repository) {
    return repository.facet(ComposerContentFacet.class);
  }

  private ComposerKeyValueFacet data(final Repository repository) {
    return repository.facet(ComposerKeyValueFacet.class);
  }

  private ComposerHostedMetadataFacet metadata(final Repository repository) {
    return repository.facet(ComposerHostedMetadataFacet.class);
  }
}
