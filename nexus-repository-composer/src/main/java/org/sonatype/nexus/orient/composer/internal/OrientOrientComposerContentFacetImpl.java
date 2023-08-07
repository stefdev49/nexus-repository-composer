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
import org.sonatype.nexus.common.collect.AttributesMap;
import org.sonatype.nexus.common.hash.HashAlgorithm;
import org.sonatype.nexus.orient.composer.OrientComposerContentFacet;
import org.sonatype.nexus.repository.FacetSupport;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.external.ComposerFormatAttributesExtractor;
import org.sonatype.nexus.repository.composer.internal.AssetKind;
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
import org.sonatype.nexus.transaction.Transactional;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;
import static org.sonatype.nexus.common.hash.HashAlgorithm.*;
import static org.sonatype.nexus.content.composer.internal.recipe.ComposerRecipeSupport.*;
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
public class OrientOrientComposerContentFacetImpl
    extends FacetSupport
    implements OrientComposerContentFacet
{
  public static final List<HashAlgorithm> HASH_ALGORITHMS = ImmutableList.of(MD5, SHA1, SHA256);

  private final ComposerFormatAttributesExtractor composerFormatAttributesExtractor;

  private final AssetEntityAdapter assetEntityAdapter;

  @Inject
  public OrientOrientComposerContentFacetImpl(final AssetEntityAdapter assetEntityAdapter, final ComposerFormatAttributesExtractor composerFormatAttributesExtractor) {
    this.assetEntityAdapter = checkNotNull(assetEntityAdapter);
    this.composerFormatAttributesExtractor = checkNotNull(composerFormatAttributesExtractor);
  }

  // TODO: composer does not have config, this method is here only to have this bundle do Import-Package org.sonatype.nexus.repository.config
  // TODO: as FacetSupport subclass depends on it. Actually, this facet does not need any kind of configuration
  // TODO: it's here only to circumvent this OSGi/maven-bundle-plugin issue.
  @Override
  protected void doValidate(final Configuration configuration) throws Exception {
    // empty
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

    final Blob blob = tx.requireBlob(asset.requireBlobRef());
    return toContent(asset, blob);
  }

  @Override
  public Content put(final String path, final Payload content) throws IOException {

    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content, HASH_ALGORITHMS)) {
      return doPutContent(path, tempBlob, content, AssetKind.ZIPBALL, null, null, null);
    }
  }

  @Override
  public Content put(String path, Content content, AssetKind assetKind) throws IOException {
    StorageFacet storageFacet = facet(StorageFacet.class);
    try (TempBlob tempBlob = storageFacet.createTempBlob(content, HASH_ALGORITHMS)) {
      switch (assetKind) {
        case ZIPBALL:
          return doPutContent(path, tempBlob, content, assetKind, null, null, null);
        case PACKAGES:
          return doPutMetadata(path, tempBlob, content, assetKind);
        case PACKAGE:
          return doPutMetadata(path, tempBlob, content, assetKind);
        case LIST:
          return doPutMetadata(path, tempBlob, content, assetKind);
        case PROVIDER:
          return doPutMetadata(path, tempBlob, content, assetKind);
        default:
          throw new IllegalStateException("Unexpected asset kind: " + assetKind);
      }
    }
  }

  @TransactionalStoreBlob
  protected Content doPutMetadata(final String path,
                                  final TempBlob tempBlob,
                                  final Payload payload,
                                  final AssetKind assetKind)
          throws IOException
  {
    StorageTx tx = UnitOfWork.currentTx();
    Result result = getResult(path);
    Asset asset = getOrCreateAsset(path, result.vendor, result.project, result.version);
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

  @Override
  @TransactionalStoreBlob
  public Asset put(final String path, final AssetBlob assetBlob, @Nullable final AttributesMap contentAttributes) {
    StorageTx tx = UnitOfWork.currentTx();
    Result result = getResult(path);
    Asset asset = getOrCreateAsset(path, result.vendor, result.project, result.version);
    tx.attachBlob(asset, assetBlob);
    Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
    tx.saveAsset(asset);
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
    Result result = getResult(path);

    StorageTx tx = UnitOfWork.currentTx();

    Asset asset = getOrCreateAsset(path, result.vendor, result.project, result.version);

    AttributesMap contentAttributes = null;
    if (payload instanceof Content) {
      Content.applyToAsset(asset, Content.maintainLastModified(asset, contentAttributes));
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
      asset.formatAttributes().set(P_VENDOR, result.vendor);
      asset.formatAttributes().set(P_PROJECT, result.project);
      asset.formatAttributes().set(P_VERSION, result.version);
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

  private static Result getResult(String path) {
    String[] parts = path.split("/");
    String vendor = parts[0];
    String project ;
    if (parts.length > 1) {
      project = parts[1];
    }
    else {
      project = vendor;
    }
    String version;
    if (parts.length == 3) {
      version = "latest";
    }
    else {
      version = parts[2];
    }
    return new Result(vendor, project, version);
  }

  private static class Result {
    public final String vendor;
    public final String project;
    public final String version;

    public Result(String vendor, String project, String version) {
      this.vendor = vendor;
      this.project = project;
      this.version = version;
    }
  }

  @Override
  @TransactionalStoreMetadata
  public Asset getOrCreateAsset(final String path,
                                final String group,
                                final String name,
                                final String version) {
    final StorageTx tx = UnitOfWork.currentTx();
    final Bucket bucket = tx.findBucket(getRepository());

    Component component = findComponent(tx, group, name, version);
    if (component == null) {
      component = tx.createComponent(bucket, getRepository().getFormat()).group(group).name(name).version(version);
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

  @Override
  @TransactionalDeleteBlob
  public boolean delete(final String path) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();

    final Component component = findComponent(tx, path);
    if (component == null) {
      return false;
    }

    tx.deleteComponent(component);
    return true;
  }

  @Override
  @TransactionalTouchMetadata
  public void setCacheInfo(final String path, final Content content, final CacheInfo cacheInfo) throws IOException {
    StorageTx tx = UnitOfWork.currentTx();
    Bucket bucket = tx.findBucket(getRepository());

    // by EntityId
    Asset asset = Content.findAsset(tx, bucket, content);
    if (asset == null) {
      // by format coordinates
      Component component = tx.findComponentWithProperty(P_NAME, path, bucket);
      if (component != null) {
        asset = tx.firstAsset(component);
      }
    }
    if (asset == null) {
      log.debug("Attempting to set cache info for non-existent composer component {}", path);
      return;
    }

    log.debug("Updating cacheInfo of {} to {}", path, cacheInfo);
    CacheInfo.applyToAsset(asset, cacheInfo);
    tx.saveAsset(asset);
  }

  @Override
  @Transactional
  public boolean assetExists(final String name) {
    StorageTx tx = UnitOfWork.currentTx();
    return assetEntityAdapter.exists(tx.getDb(), name, tx.findBucket(getRepository()));
  }

  // findComponent function by path. split path into group, name, version
  private Component findComponent(final StorageTx tx, final String path) {
    Result result = getResult(path);
    return findComponent(tx, result.vendor, result.project, result.version);
  }

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

  private Asset findAsset(final StorageTx tx, final String path) {
    log.debug("Finding asset for path {}", path);
    return tx.findAssetWithProperty(P_NAME, path, tx.findBucket(getRepository()));
  }

  private Content toContent(final Asset asset, final Blob blob) {
    final Content content = new Content(new BlobPayload(blob, asset.requireContentType()));
    Content.extractFromAsset(asset, HASH_ALGORITHMS, content.getAttributes());
    return content;
  }

}
