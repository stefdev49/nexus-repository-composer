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
package org.sonatype.nexus.repository.composer;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.rest.api.SimpleApiRepositoryAdapter;
import org.sonatype.nexus.repository.rest.api.model.AbstractApiRepository;
import org.sonatype.nexus.repository.routing.RoutingRuleStore;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.types.ProxyType;

/**
 * Adapter to expose raw specific repository configuration for the repositories REST API.
 *
 * @since 3.41
 */
@Named(ComposerFormat.NAME)
public class ComposerRepositoryAdapter
    extends SimpleApiRepositoryAdapter
{
  private static final String RAW = "raw";

  @Inject
  public ComposerRepositoryAdapter(final RoutingRuleStore routingRuleStore) {
    super(routingRuleStore);
  }

  @Override
  public AbstractApiRepository adapt(final Repository repository) {
    switch (repository.getType().toString()) {
      case HostedType.NAME:
        return new ComposerHostedApiRepository(
            repository.getName(),
            repository.getUrl(),
            repository.getConfiguration().isOnline(),
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            getComponentAttributes(repository),
            createComposerAttributes(repository));
      case ProxyType.NAME:
        return new ComposerProxyApiRepository(
            repository.getName(),
            repository.getUrl(),
            repository.getConfiguration().isOnline(),
            getHostedStorageAttributes(repository),
            getCleanupPolicyAttributes(repository),
            getProxyAttributes(repository),
            getNegativeCacheAttributes(repository),
            getHttpClientAttributes(repository),
            getRoutingRuleName(repository),
            getReplicationAttributes(repository),
            createComposerAttributes(repository));
      default:
        return super.adapt(repository);
    }
  }

  private ComposerAttributes createComposerAttributes(final Repository repository) {
    String disposition = repository.getConfiguration().attributes(RAW).get("contentDisposition", String.class);
    return new ComposerAttributes(disposition);
  }
}
