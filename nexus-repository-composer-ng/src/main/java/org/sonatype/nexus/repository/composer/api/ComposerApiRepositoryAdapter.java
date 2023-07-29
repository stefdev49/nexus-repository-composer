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

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.text.Strings2;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

/**
 * Adapter to expose Composer specific properties for the repositories REST API.
 *
 * @since 3.20
 */
@Named(ComposerFormat.NAME)
public class ComposerApiRepositoryAdapter
    extends SimpleApiRepositoryAdapter
{
  @Inject
  public ComposerApiRepositoryAdapter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public AbstractApiRepository adapt(final Repository repository) {
    boolean online = repository.getConfiguration().isOnline();
    String name = repository.getName();
    String url = repository.getUrl();

    switch (repository.getType().toString()) {
      case HostedType.NAME:
        return new ComposerHostedApiRepository(
            name,
            url,
            online,
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            createComposerHostedRepositoriesAttributes(repository),
            createComposerSigningRepositoriesAttributes(repository),
            getComponentAttributes(repository));
      case ProxyType.NAME:
        return new ComposerProxyApiRepository(name, url, online,
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            createComposerProxyRepositoriesAttributes(repository),
            getProxyAttributes(repository),
            getNegativeCacheAttributes(repository),
            getHttpClientAttributes(repository),
            getRoutingRuleName(repository),
            getReplicationAttributes(repository));
    }
    return null;
  }

  private ComposerHostedRepositoriesAttributes createComposerHostedRepositoriesAttributes(final Repository repository) {
    String distribution = repository.getConfiguration().attributes(ComposerFormat.NAME).get("distribution", String.class);
    return new ComposerHostedRepositoriesAttributes(distribution);
  }

  private ComposerSigningRepositoriesAttributes createComposerSigningRepositoriesAttributes(final Repository repository) {
    NestedAttributesMap composerAttributes = repository.getConfiguration().attributes("composerSigning");
    String keypair = composerAttributes.get("keypair", String.class);
    String passphrase = composerAttributes.get("passphrase", String.class);
    if (!Strings2.isBlank(passphrase)) {
      return new ComposerSigningRepositoriesAttributes(keypair, null);
    }
    return null;
  }

  private ComposerProxyRepositoriesAttributes createComposerProxyRepositoriesAttributes(final Repository repository) {
    NestedAttributesMap composerAttributes = repository.getConfiguration().attributes(ComposerFormat.NAME);
    String distribution = composerAttributes.get("distribution", String.class);
    Boolean flat = composerAttributes.get("flat", Boolean.class);
    return new ComposerProxyRepositoriesAttributes(distribution, flat);
  }
}
