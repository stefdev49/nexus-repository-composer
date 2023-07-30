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
package org.sonatype.nexus.blobstore.restore.composer.internal.orient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.orient.OrientBaseRestoreBlobStrategy;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.orient.ComposerRestoreFacet;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.aether.util.StringUtils.isEmpty;
import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA256;

/**
 * @since 3.17
 */
@Named("composer")
@Singleton
public class OrientComposerRestoreBlobStrategy
    extends OrientBaseRestoreBlobStrategy<ComposerRestoreBlobData>
{
  @Inject
  public OrientComposerRestoreBlobStrategy(final NodeAccess nodeAccess,
                                      final RepositoryManager repositoryManager,
                                      final BlobStoreManager blobStoreManager,
                                      final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
  }

  @Override
  public ComposerRestoreBlobData createRestoreData(final RestoreBlobData blobData) {
    checkState(!isEmpty(blobData.getBlobName()), "Blob name cannot be empty");
    return new ComposerRestoreBlobData(blobData);
  }

  @Override
  protected boolean canAttemptRestore(@Nonnull final ComposerRestoreBlobData data) {
    Repository repository = data.getBlobData().getRepository();
    Optional<ComposerRestoreFacet> composerRestoreFacetFacet = repository.optionalFacet(ComposerRestoreFacet.class);

    if (!composerRestoreFacetFacet.isPresent()) {
      log.warn("Skipping as Composer Restore Facet not found on repository: {}", repository.getName());
      return false;
    }
    return true;
  }

  @Override
  public String getAssetPath(@Nonnull final ComposerRestoreBlobData data) {
    return data.getBlobData().getBlobName();
  }

  @Override
  protected boolean assetExists(@Nonnull final ComposerRestoreBlobData data) {
    ComposerRestoreFacet facet = data.getBlobData().getRepository().facet(ComposerRestoreFacet.class);
    return facet.assetExists(data.getBlobData().getBlobName());
  }

  @Override
  protected void createAssetFromBlob(@Nonnull final AssetBlob assetBlob, @Nonnull final ComposerRestoreBlobData data)
      throws IOException
  {
    final Repository repository = data.getBlobData().getRepository();
    ComposerRestoreFacet facet = repository.facet(ComposerRestoreFacet.class);
    final String path = data.getBlobData().getBlobName();

    facet.restore(assetBlob, path);
  }

  @Override
  public boolean componentRequired(final ComposerRestoreBlobData data) {
    final Repository repository = data.getBlobData().getRepository();
    final ComposerRestoreFacet facet = repository.facet(ComposerRestoreFacet.class);
    return facet.componentRequired(data.getBlobData().getBlobName());
  }

  @Override
  protected Repository getRepository(@Nonnull final ComposerRestoreBlobData data) {
    return data.getBlobData().getRepository();
  }

  @Override
  protected Query getComponentQuery(final ComposerRestoreBlobData data) throws IOException {
    final Repository repository = data.getBlobData().getRepository();
    final ComposerRestoreFacet facet = repository.facet(ComposerRestoreFacet.class);
    return facet.getComponentQuery(data.getBlobData().getBlob());
  }

  @Nonnull
  @Override
  public List<HashAlgorithm> getHashAlgorithms() {
    return newArrayList(MD5, SHA1, SHA256);
  }
}
