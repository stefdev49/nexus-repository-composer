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

import javax.annotation.Nonnull;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.blobstore.api.BlobStoreManager;
import org.sonatype.nexus.blobstore.restore.RestoreBlobData;
import org.sonatype.nexus.blobstore.restore.orient.OrientBaseRestoreBlobStrategy;
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.common.log.DryRunPrefix;
import org.sonatype.nexus.common.node.NodeAccess;
import org.sonatype.nexus.orient.composer.OrientComposerContentFacet;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.manager.RepositoryManager;
import org.sonatype.nexus.repository.storage.AssetBlob;
import org.sonatype.nexus.repository.storage.Query;

import com.google.common.collect.ImmutableList;

import static org.sonatype.nexus.common.hash.HashAlgorithm.MD5;
import static org.sonatype.nexus.common.hash.HashAlgorithm.SHA1;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * @since 3.15
 */
@Named("composer")
@Priority(Integer.MAX_VALUE)
@Singleton
public class OrientComposerRestoreBlobStrategy
    extends OrientBaseRestoreBlobStrategy<ComposerRestoreBlobData>
{
  private final AttributesMap NO_CONTENT_ATTRIBUTES = null;

  @Inject
  public OrientComposerRestoreBlobStrategy(
      final NodeAccess nodeAccess,
      final RepositoryManager repositoryManager,
      final BlobStoreManager blobStoreManager,
      final DryRunPrefix dryRunPrefix)
  {
    super(nodeAccess, repositoryManager, blobStoreManager, dryRunPrefix);
  }

  @Override
  protected List<HashAlgorithm> getHashAlgorithms() {
    return ImmutableList.of(SHA1, MD5);
  }

  @Override
  protected ComposerRestoreBlobData createRestoreData(final RestoreBlobData blobData) {
    return new ComposerRestoreBlobData(blobData);
  }

  @Override
  protected boolean canAttemptRestore(final ComposerRestoreBlobData data) {
    Repository repository = data.getBlobData().getRepository();

    if (!repository.optionalFacet(OrientComposerContentFacet.class).isPresent()) {
      log.warn("Skipping as Composer Facet not found on repository: {}", repository.getName());
      return false;
    }

    return true;
  }

  @Override
  protected String getAssetPath(final ComposerRestoreBlobData data) {
    return data.getBlobData().getBlobName();
  }

  @Override
  protected boolean assetExists(final ComposerRestoreBlobData data) {
    return getComposerContentFacet(data).assetExists(data.getBlobData().getBlobName());
  }

  @Override
  protected void createAssetFromBlob(final AssetBlob assetBlob, final ComposerRestoreBlobData data) {
    getComposerContentFacet(data).put(getAssetPath(data), assetBlob, NO_CONTENT_ATTRIBUTES);
  }

  private OrientComposerContentFacet getComposerContentFacet(final ComposerRestoreBlobData data) {
    return data.getBlobData().getRepository().facet(OrientComposerContentFacet.class);
  }

  @Override
  protected boolean componentRequired(@Nonnull final ComposerRestoreBlobData data) throws IOException {
    return true;
  }

  @Override
  protected Query getComponentQuery(@Nonnull final ComposerRestoreBlobData data) {
    return Query.builder().where(P_NAME).eq(data.getBlobData().getBlobName()).build();
  }

  @Override
  protected Repository getRepository(@Nonnull final ComposerRestoreBlobData data) {
    return data.getBlobData().getRepository();
  }
}
