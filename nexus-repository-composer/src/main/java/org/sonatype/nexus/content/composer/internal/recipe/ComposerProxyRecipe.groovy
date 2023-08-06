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
package org.sonatype.nexus.content.composer.internal.recipe

import org.sonatype.nexus.content.composer.internal.ComposerProviderHandler
import org.sonatype.nexus.repository.composer.internal.AssetKind
import org.sonatype.nexus.repository.http.HttpHandlers

import javax.annotation.Nonnull
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.cache.NegativeCacheFacet
import org.sonatype.nexus.repository.cache.NegativeCacheHandler
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.httpclient.HttpClientFacet
import org.sonatype.nexus.repository.proxy.ProxyHandler
import org.sonatype.nexus.repository.purge.PurgeUnusedFacet
import org.sonatype.nexus.repository.composer.internal.ComposerFormat
import org.sonatype.nexus.repository.routing.RoutingRuleHandler
import org.sonatype.nexus.repository.types.ProxyType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound
import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and

/**
 * Composer proxy repository recipe.
 *
 * @since 3.24
 */
@Named(ComposerProxyRecipe.NAME)
@Priority(Integer.MAX_VALUE)
@Singleton
class ComposerProxyRecipe
    extends ComposerRecipeSupport
{
  public static final String NAME = 'composer-proxy'

  @Inject
  Provider<HttpClientFacet> httpClientFacet

  @Inject
  Provider<NegativeCacheFacet> negativeCacheFacet

  @Inject
  Provider<ComposerProxyFacet> proxyFacet

  @Inject
  Provider<PurgeUnusedFacet> purgeUnusedFacet

  @Inject
  NegativeCacheHandler negativeCacheHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ProxyHandler proxyHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  RoutingRuleHandler routingRuleHandler

  @Inject
  ComposerContentHandler composerContentHandler

  @Inject
  ComposerProviderHandler composerProviderHandler

  @Inject
  ComposerProxyRecipe(@Named(ProxyType.NAME) final Type type,
                 @Named(ComposerFormat.NAME) final Format format
  )
  {
    super(type, format)
  }

  @Override
  void apply(final @Nonnull Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(httpClientFacet.get())
    repository.attach(negativeCacheFacet.get())
    repository.attach(proxyFacet.get())
    repository.attach(contentFacet.get())
    repository.attach(maintenanceFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(purgeUnusedFacet.get())
  }

  /**
   * Configure {@link ViewFacet}.
   */
  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder()

    builder.route(packagesMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.PACKAGES))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(negativeCacheHandler)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(proxyHandler)
            .create())

    builder.route(listMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.LIST))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(negativeCacheHandler)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(proxyHandler)
            .create())

    builder.route(providerMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.PROVIDER))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(negativeCacheHandler)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(composerProviderHandler)
            .handler(composerContentHandler)
            .handler(proxyHandler)
            .create())

    builder.route(packageMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.PACKAGE))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(negativeCacheHandler)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(composerContentHandler)
            .handler(proxyHandler)
            .create())

    builder.route(zipballMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.ZIPBALL))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(negativeCacheHandler)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(proxyHandler)
            .create())

    addBrowseUnsupportedRoute(builder)

    builder.defaultHandlers(HttpHandlers.notFound())

    facet.configure(builder.create())

    return facet
  }
}
