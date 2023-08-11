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
package org.sonatype.nexus.orient.composer.internal

import org.sonatype.nexus.content.composer.internal.recipe.ComposerContentHandler
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.attributes.AttributesFacet
import org.sonatype.nexus.repository.composer.internal.AssetKind
import org.sonatype.nexus.repository.composer.internal.ComposerFormat
import org.sonatype.nexus.repository.composer.internal.ComposerIndexHtmlForwardHandler
import org.sonatype.nexus.repository.composer.internal.ComposerSecurityFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler
import org.sonatype.nexus.repository.search.ElasticSearchFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.storage.SingleAssetComponentMaintenance
import org.sonatype.nexus.repository.storage.StorageFacet
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import javax.annotation.Nonnull
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.PUT

/**
 * Composer hosted repository recipe.
 *
 * @since 3.0
 */
@Named(OrientComposerHostedRecipe.NAME)
@Priority(Integer.MAX_VALUE)
@Singleton
class OrientComposerHostedRecipe
    extends RecipeSupport {
  public static final String NAME = 'composer-hosted'

  @Inject
  Provider<ComposerSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<ElasticSearchFacet> searchFacet

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  Provider<AttributesFacet> attributesFacet

  @Inject
  Provider<SingleAssetComponentMaintenance> componentMaintenance

  @Inject
  Provider<OrientComposerContentFacetImpl> composerContentFacet

  @Inject
  UnitOfWorkHandler unitOfWorkHandler

  @Inject
  Provider<OrientComposerHostedFacet> hostedFacet

  @Inject
  Provider<OrientComposerHostedMetadataFacet> hostedMetadataFacet

  @Inject
  OrientComposerHostedDownloadHandler downloadHandler

  @Inject
  OrientComposerHostedUploadHandler uploadHandler

  @Inject
  ExceptionHandler exceptionHandler

  @Inject
  TimingHandler timingHandler

  @Inject
  ComposerIndexHtmlForwardHandler indexHtmlForwardHandler

  @Inject
  SecurityHandler securityHandler

  @Inject
  PartialFetchHandler partialFetchHandler

  @Inject
  ComposerContentHandler contentHandler

  @Inject
  ConditionalRequestHandler conditionalRequestHandler

  @Inject
  ContentHeadersHandler contentHeadersHandler

  @Inject
  LastDownloadedHandler lastDownloadedHandler

  @Inject
  HandlerContributor handlerContributor

  @Inject
  OrientComposerHostedRecipe(@Named(HostedType.NAME) final Type type,
                             @Named(ComposerFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(composerContentFacet.get())
    repository.attach(storageFacet.get())
    repository.attach(attributesFacet.get())
    repository.attach(componentMaintenance.get())
    repository.attach(hostedFacet.get())
    repository.attach(hostedMetadataFacet.get())
    repository.attach(searchFacet.get())
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
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(unitOfWorkHandler)
            .handler(downloadHandler)
            .create())

    builder.route(providerMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.PROVIDER))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(unitOfWorkHandler)
            .handler(downloadHandler)
            .create())

    builder.route(packageMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.PACKAGE))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(unitOfWorkHandler)
            .handler(downloadHandler)
            .create())

    builder.route(zipballMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.ZIPBALL))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(unitOfWorkHandler)
            .handler(downloadHandler)
            .create())

    builder.route(uploadMatcher()
            .handler(timingHandler)
            .handler(assetKindHandler.rcurry(AssetKind.ZIPBALL))
            .handler(securityHandler)
            .handler(exceptionHandler)
            .handler(handlerContributor)
            .handler(conditionalRequestHandler)
            .handler(partialFetchHandler)
            .handler(contentHeadersHandler)
            .handler(unitOfWorkHandler)
            .handler(uploadHandler)
            .create())

    builder.defaultHandlers(HttpHandlers.badRequest())

    facet.configure(builder.create())

    return facet
  }

  Closure assetKindHandler = { Context context, AssetKind value ->
    context.attributes.set(AssetKind, value)
    return context.proceed()
  }

  static Route.Builder packagesMatcher() {
    new Route.Builder().matcher(
            LogicMatchers.and(
                    new ActionMatcher(GET, HEAD),
                    new LiteralMatcher('/packages.json')
            ))
  }

  static Route.Builder listMatcher() {
    new Route.Builder().matcher(
            LogicMatchers.and(
                    new ActionMatcher(GET, HEAD),
                    new LiteralMatcher('/packages/list.json')
            ))
  }

  static Route.Builder providerMatcher() {
    new Route.Builder().matcher(
            LogicMatchers.and(
                    new ActionMatcher(GET, HEAD),
                    new TokenMatcher('/p/{vendor:.+}/{project:.+}.json')
            ))
  }

  static Route.Builder packageMatcher() {
    new Route.Builder().matcher(
            LogicMatchers.and(
                    new ActionMatcher(GET, HEAD),
                    new TokenMatcher('/p2/{vendor:.+}/{project:.+}.json')
            ))
  }

  static Route.Builder zipballMatcher() {
    new Route.Builder().matcher(
            LogicMatchers.and(
                    new ActionMatcher(GET, HEAD),
                    new TokenMatcher('/{vendor:.+}/{project:.+}/{version:.+}/{name:.+}.zip')
            ))
  }

  static Route.Builder uploadMatcher() {
    new Route.Builder().matcher(
            LogicMatchers.and(
                    new ActionMatcher(PUT),
                    new TokenMatcher('/packages/upload/{vendor:.+}/{project:.+}/{version:.+}')
            ))
  }
}
