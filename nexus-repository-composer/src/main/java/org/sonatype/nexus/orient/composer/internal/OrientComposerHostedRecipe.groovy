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

import org.sonatype.nexus.content.composer.internal.recipe.ComposerRecipeSupport
import org.sonatype.nexus.orient.composer.OrientComposerContentFacet
import org.sonatype.nexus.repository.composer.internal.AssetKind
import org.sonatype.nexus.repository.composer.internal.ComposerGroupPackageJsonHandler
import org.sonatype.nexus.repository.composer.internal.ComposerGroupPackagesJsonHandler
import org.sonatype.nexus.repository.composer.internal.ComposerGroupProviderJsonHandler
import org.sonatype.nexus.repository.content.browse.BrowseFacet

import org.sonatype.nexus.content.composer.internal.recipe.ComposerRecipeSupport
import org.sonatype.nexus.repository.composer.internal.ComposerFormat
import org.sonatype.nexus.repository.storage.StorageFacet

import javax.annotation.Nonnull
import javax.annotation.Priority
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.http.HttpHandlers
import org.sonatype.nexus.repository.http.HttpMethods
import org.sonatype.nexus.repository.types.HostedType
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.Router
import org.sonatype.nexus.repository.view.ViewFacet
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.SuffixMatcher
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import static org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers.and

/**
 * Composer hosted repository recipe.
 *
 * @since 3.0
 */
@Named(OrientComposerHostedRecipe.NAME)
@Priority(Integer.MAX_VALUE)
@Singleton
class OrientComposerHostedRecipe
    extends OrientComposerRecipeSupport {
  public static final String NAME = 'composer-hosted'

  @Inject
  Provider<StorageFacet> storageFacet

  @Inject
  OrientComposerHostedRecipe(@Named(HostedType.NAME) final Type type,
                             @Named(ComposerFormat.NAME) final Format format) {
    super(type, format)
  }

  @Override
  void apply(@Nonnull final Repository repository) throws Exception {
    repository.attach(securityFacet.get())
    repository.attach(configure(viewFacet.get()))
    repository.attach(storageFacet.get())
    repository.attach(maintenanceFacet.get())
    repository.attach(searchFacet.get())
    repository.attach(browseFacet.get())
    repository.attach(replicationFacet.get())
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
        .handler(contentHandler)
        .handler(packagesJsonHandler)
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
        .handler(contentHandler)
        .handler(providerJsonHandler)
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
            .handler(contentHandler)
            .handler(packageJsonHandler)
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
            .handler(lastDownloadedHandler)
            .handler(contentHandler)
            .create())

    builder.defaultHandlers(HttpHandlers.badRequest())

    facet.configure(builder.create())

    return facet
  }
}
