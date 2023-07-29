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
package org.sonatype.nexus.repository.composer.datastore.internal.data;

import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.entity.Continuations;
import org.sonatype.nexus.repository.Facet.Exposed;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.composer.datastore.data.ComposerKeyValueDAO;
import org.sonatype.nexus.repository.composer.datastore.data.ComposerKeyValueStore;
import org.sonatype.nexus.repository.content.kv.KeyValue;
import org.sonatype.nexus.repository.content.kv.KeyValueFacetSupport;

import org.apache.commons.lang3.StringUtils;

import static com.google.common.base.Preconditions.checkArgument;

@Named(ComposerFormat.NAME)
@Exposed
public class ComposerKeyValueFacet
    extends KeyValueFacetSupport<ComposerKeyValueDAO, ComposerKeyValueStore>
{
  private final int limit;

  private final static String CATEGORY = StringUtils.EMPTY;

  @Inject
  public ComposerKeyValueFacet(
      @Named("${nexus.composer.paging.size:-100}") final int limit
  )
  {
    super(ComposerFormat.NAME, ComposerKeyValueDAO.class);
    checkArgument(limit > 0);
    this.limit = limit;
  }

  /**
   * Store ComposerDeb metadata
   *
   * @param assetId     the assetId
   * @param componentId the componentId
   * @param metadata    the json of an ComposerDeb metadata
   */
  public void addPackageMetadata(final int componentId, final int assetId, final String metadata) {
    set(CATEGORY, composerKey(componentId, assetId), metadata);
  }

  /**
   * Remove the ComposerDeb metadata.
   *
   * @param assetId     the assetId
   * @param componentId the componentId
   */
  public void removePackageMetadata(final int componentId, final int assetId) {
    remove(CATEGORY, composerKey(componentId, assetId));
  }

  /**
   * Remove all ComposerDeb metadata.
   */
  public void removeAllPackageMetadata() {
    removeAll(CATEGORY);
  }

  /**
   * Brows ComposerDeb metadata backed by key-value object
   *
   * @return a stream of value objects representing ComposerDeb metadata as String
   */
  public Stream<String> browsePackagesMetadata() {
    return Continuations
        .streamOf((browseLimit, continuationToken) -> browseValues(CATEGORY, browseLimit, continuationToken), limit)
        .map(KeyValue::getValue);
  }

  /*
   * Creates a key for componentId. This should only be used for storing ComposerDeb JSON.
   * Other use cases should avoid overlapping this key structure.
   */
  private String composerKey(final int componentId, final int assetId) {
    return "composer-" + componentId + '-' + assetId;
  }
}
