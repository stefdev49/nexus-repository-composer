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
package org.sonatype.nexus.repository.composer.api;

import javax.validation.constraints.NotNull;

import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.rest.api.model.CleanupPolicyAttributes;
import org.sonatype.nexus.repository.rest.api.model.HttpClientAttributes;
import org.sonatype.nexus.repository.rest.api.model.NegativeCacheAttributes;
import org.sonatype.nexus.repository.rest.api.model.ProxyAttributes;
import org.sonatype.nexus.repository.rest.api.model.ReplicationAttributes;
import org.sonatype.nexus.repository.rest.api.model.SimpleApiProxyRepository;
import org.sonatype.nexus.repository.rest.api.model.StorageAttributes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * REST API model representing an Composer proxy repository.
 *
 * @since 3.20
 */
@JsonIgnoreProperties(value = {"format", "type", "url"}, allowGetters = true)
public class ComposerProxyApiRepository
    extends SimpleApiProxyRepository
{
  @NotNull
  protected final ComposerProxyRepositoriesAttributes composer;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public ComposerProxyApiRepository(
      @JsonProperty("name") final String name,
      @JsonProperty("url") final String url,
      @JsonProperty("online") final Boolean online,
      @JsonProperty("storage") final StorageAttributes storage,
      @JsonProperty("cleanup") final CleanupPolicyAttributes cleanup,
      @JsonProperty("composer") final ComposerProxyRepositoriesAttributes composer,
      @JsonProperty("proxy") final ProxyAttributes proxy,
      @JsonProperty("negativeCache") final NegativeCacheAttributes negativeCache,
      @JsonProperty("httpClient") final HttpClientAttributes httpClient,
      @JsonProperty("routingRuleName") final String routingRuleName,
      @JsonProperty("replication") @JsonInclude(value= Include.NON_EMPTY, content=Include.NON_NULL)
      final ReplicationAttributes replication)
  {
    super(name, ComposerFormat.NAME, url, online, storage, cleanup, proxy, negativeCache, httpClient, routingRuleName,
        replication);
    this.composer = composer;
  }

  public ComposerProxyRepositoriesAttributes getComposer() {
    return composer;
  }
}
