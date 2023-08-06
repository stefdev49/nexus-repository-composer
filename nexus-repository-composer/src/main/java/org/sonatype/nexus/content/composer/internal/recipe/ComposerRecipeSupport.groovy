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

import org.sonatype.nexus.repository.composer.internal.AssetKind
import org.sonatype.nexus.repository.view.Context
import org.sonatype.nexus.repository.view.Route
import org.sonatype.nexus.repository.view.matchers.ActionMatcher
import org.sonatype.nexus.repository.view.matchers.LiteralMatcher
import org.sonatype.nexus.repository.view.matchers.logic.LogicMatchers
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher

import javax.inject.Inject
import javax.inject.Provider

import org.sonatype.nexus.content.composer.ComposerContentFacet
import org.sonatype.nexus.repository.Format
import org.sonatype.nexus.repository.RecipeSupport
import org.sonatype.nexus.repository.Type
import org.sonatype.nexus.repository.content.browse.BrowseFacet
import org.sonatype.nexus.repository.content.maintenance.SingleAssetMaintenanceFacet
import org.sonatype.nexus.repository.content.search.SearchFacet
import org.sonatype.nexus.repository.http.PartialFetchHandler

import org.sonatype.nexus.repository.composer.internal.ComposerIndexHtmlForwardHandler
import org.sonatype.nexus.repository.composer.internal.ComposerSecurityFacet
import org.sonatype.nexus.repository.security.SecurityHandler
import org.sonatype.nexus.repository.view.ConfigurableViewFacet
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler
import org.sonatype.nexus.repository.view.handlers.HandlerContributor
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler
import org.sonatype.nexus.repository.view.handlers.TimingHandler

import static org.sonatype.nexus.repository.http.HttpMethods.GET
import static org.sonatype.nexus.repository.http.HttpMethods.HEAD
import static org.sonatype.nexus.repository.http.HttpMethods.PUT

/**
 * @since 3.24
 */
abstract class ComposerRecipeSupport
    extends RecipeSupport
{
  public static final String VENDOR_TOKEN = 'vendor'

  public static final String PROJECT_TOKEN = 'project'

  public static final String VERSION_TOKEN = 'version'

  public static final String NAME_TOKEN = 'name'

  public static final String PACKAGE_FIELD_NAME = "package";

  public static final String SOURCE_TYPE_FIELD_NAME = 'src-type';

  public static final String SOURCE_URL_FIELD_NAME = 'src-url';

  public static final String SOURCE_REFERENCE_FIELD_NAME = 'src-ref';

  @Inject
  Provider<ComposerContentFacet> contentFacet

  @Inject
  Provider<ComposerSecurityFacet> securityFacet

  @Inject
  Provider<ConfigurableViewFacet> viewFacet

  @Inject
  Provider<SingleAssetMaintenanceFacet> maintenanceFacet

  @Inject
  Provider<SearchFacet> searchFacet

  @Inject
  Provider<BrowseFacet> browseFacet

  @Inject
  Provider<ComposerReplicationFacet> replicationFacet

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

  protected ComposerRecipeSupport(final Type type, final Format format)
  {
    super(type, format)
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