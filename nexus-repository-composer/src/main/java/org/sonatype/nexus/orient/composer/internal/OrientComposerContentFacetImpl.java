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
package org.sonatype.nexus.orient.composer.internal;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.blobstore.api.Blob;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.external.ComposerFormatAttributesExtractor;
import org.sonatype.nexus.repository.composer.internal.AssetKind;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.ComposerWritePolicySelector;
import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.storage.*;
import org.sonatype.nexus.repository.transaction.TransactionalDeleteBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreBlob;
import org.sonatype.nexus.repository.transaction.TransactionalStoreMetadata;
import org.sonatype.nexus.repository.transaction.TransactionalTouchBlob;
import org.sonatype.nexus.repository.transaction.TransactionalTouchMetadata;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;
import org.sonatype.nexus.repository.view.payloads.BlobPayload;
import org.sonatype.nexus.repository.view.payloads.TempBlob;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.*;
import static org.sonatype.nexus.orient.composer.internal.OrientComposerRecipeSupport.*;
import static org.sonatype.nexus.repository.composer.external.ComposerAttributes.P_PROJECT;
import static org.sonatype.nexus.repository.composer.external.ComposerAttributes.P_VENDOR;
import static org.sonatype.nexus.repository.storage.AssetEntityAdapter.P_ASSET_KIND;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_GROUP;
import static org.sonatype.nexus.repository.storage.ComponentEntityAdapter.P_VERSION;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_NAME;

/**
 * A {@link OrientComposerContentFacet} that persists to a {@link StorageFacet}.
 *
 * @since 3.0
 */
@Named
public class OrientComposerContentFacetImpl
    extends FacetSupport
    implements OrientComposerContentFacet
{
  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(MD5, SHA1, SHA256);

  private final Format format;
  
  private final ComposerFormatAttributesExtractor composerFormatAttributesExtractor;

  @Inject
  public OrientComposerContentFacetImpl(
          @Named(ComposerFormat.NAME) final Format format,
          final ComposerFormatAttributesExtractor composerFormatAttributesExtractor) {
    this.format = checkNotNull(format);
    this.composerFormatAttributesExtractor = checkNotNull(composerFormatAttributesExtractor);
  }

  @Override
  protected void doInit(final Configuration configuration) throws Exception {
    super.doInit(configuration);
    log.info("XXX STEF XXX Initializing orient composer content facet for repository {}", getRepository().getName());
    getRepository().facet(StorageFacet.class).registerWritePolicySelector(new ComposerWritePolicySelector());
  }

  @Nullable
  @Override
  @TransactionalTouchBlob
  public Content get(final String path) {
    StorageTx tx = UnitOfWork.currentTx();

    final Asset asset = findAsset(tx, path);
    if (asset == null) {
      return null;
    }
    if (asset.markAsDownloaded()) {
      tx.saveAsset(asset);
    }

    final Blob blob = tx.requireBlob(asset.requireBlobRef());
    return toContent(asset, blob);
  }

  @Override
  public Content put(String path, Payload payload, AssetKind assetKind) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HASH_ALGORITHMS)) {
      switch (assetKind) {
        case ZIPBALL:
          return doPutContent(path, tempBlob, payload, assetKind, null, null, null);
        case PACKAGES:
        case PACKAGE:
        case LIST:
        case PROVIDER:
          return doPutMetadata(path, tempBlob, payload, assetKind);
        default:
          throw new IllegalStateException("Unexpected asset kind: " + assetKind);
      }
    }
  }

  @Override
  public Content put(final String path, final Payload payload, final String sourceType, final String sourceUrl, final String sourceReference) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(payload, HASH_ALGORITHMS)) {
      return doPutContent(path, tempBlob, payload, AssetKind.ZIPBALL, sourceType, sourceUrl, sourceReference);
    }
  }

  @Override
  @TransactionalTouchMetadata
  public void setCacheInfo(final String path, final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    Asset asset = Content.findAsset(tx, bucket, content);
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent composer component {}", path);
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", path, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @TransactionalStoreBlob
  protected Content doPutMetadata(final String path,
                                  final TempBlob tempBlob,
                                  final Payload payload,
                                  final AssetKind assetKind)
          throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    log.info("XXX STEF XXX Putting metadata at {}", path);
    Asset asset = getOrCreateAsset(path);
    asset.formatAttributes().set(P_ASSET_KIND, assetKind.toString());

    if (payload instanceof Content) {
      Content.applyToAsset(asset, Content.maintainLastModified(asset, ((Content) payload).getAttributes()));
    }

    AssetBlob assetBlob = tx.setBlob(
            asset,
            path,
            tempBlob,
            null,
            payload.getContentType(),
            false
    );

    tx.saveAsset(asset);

    return toContent(asset, assetBlob.getBlob());
  }

  @TransactionalStoreMetadata
  public Asset getOrCreateAsset(final String path) {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());

    Asset asset = findAsset(tx, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, format);
      asset.name(path);
    }

    asset.markAsDownloaded(AssetManager.DEFAULT_LAST_DOWNLOADED_INTERVAL);

    return asset;
  }

  @TransactionalStoreBlob
  protected Content doPutContent(final String path,
                                 final TempBlob tempBlob,
                                 final Payload payload,
                                 final AssetKind assetKind,
                                 final String sourceType,
                                 final String sourceUrl,
                                 final String sourceReference)
      throws IOException
  {
    log.info("XXX STEF XXX Putting content at {}", path);
    String[] parts = path.split("/");
    String vendor = parts[0];
    String project = parts[1];
    String version = parts[2];


    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = getOrCreateAsset(path, vendor, project, version);

    if (payload instanceof Content) {
      Content.applyToAsset(asset, Content.maintainLastModified(asset, ((Content) payload).getAttributes()));
    }
    AssetBlob assetBlob = tx.setBlob(
        asset,
        path,
        tempBlob,
        null,
        payload.getContentType(),
        false
    );

    try {
      asset.formatAttributes().clear();
      asset.formatAttributes().set(P_ASSET_KIND, assetKind.toString());
      asset.formatAttributes().set(P_VENDOR, vendor);
      asset.formatAttributes().set(P_PROJECT, project);
      asset.formatAttributes().set(P_VERSION, version);
      asset.formatAttributes().set(SOURCE_TYPE_FIELD_NAME, sourceType);
      asset.formatAttributes().set(SOURCE_URL_FIELD_NAME, sourceUrl);
      asset.formatAttributes().set(SOURCE_REFERENCE_FIELD_NAME, sourceReference);
      composerFormatAttributesExtractor.extractFromZip(tempBlob, asset.formatAttributes());
    }
    catch (Exception e) {
      log.error("Error extracting format attributes for {}, skipping", path, e);
    }

    tx.saveAsset(asset);

    return toContent(asset, assetBlob.getBlob());
  }

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    final Asset asset = findAsset(tx, path);
    if (asset == null) {
      return false;
    }

    tx.deleteAsset(asset);
    return true;
  }


  @TransactionalStoreMetadata
  public Asset getOrCreateAsset(final String path,
                                final String group,
                                final String name,
                                final String version)
  {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());

    Component component = findComponent(tx, group, name, version);
    if (component == null) {
      component = tx.createComponent(bucket, format).group(group).name(name).version(version);
      tx.saveComponent(component);
    }

    Asset asset = findAsset(tx, path);
    if (asset == null) {
      asset = tx.createAsset(bucket, component);
      asset.name(path);
    }

    asset.markAsDownloaded();

    return asset;
  }

  @Nullable
  private Asset findAsset(final StorageTx tx, final String path) {
    log.info("XXX STEF XXX Finding component for path {}", path);
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
  }

  @Nullable
  private Component findComponent(final StorageTx tx, final String group, final String name, final String version) {
    Iterable<Component> components = tx.findComponents(Query.builder()
                    .where(P_GROUP).eq(group)
                    .and(P_NAME).eq(name)
                    .and(P_VERSION).eq(version)
                    .build(),
            singletonList(getRepository()));
    if (components.iterator().hasNext()) {
      return components.iterator().next();
    }
    return null;
  }

  private Content toContent(final Asset asset, final Blob blob) {
    final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }
}
