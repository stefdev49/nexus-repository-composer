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
package org.sonatype.nexus.repository.composer.datastore.internal.hosted;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.RecipeSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.composer.datastore.ComposerContentFacet;
import org.sonatype.nexus.repository.composer.datastore.internal.data.ComposerKeyValueFacet;
import org.sonatype.nexus.repository.composer.datastore.internal.hosted.metadata.ComposerHostedMetadataFacet;
import org.sonatype.nexus.repository.composer.internal.ComposerSecurityFacet;
import org.sonatype.nexus.repository.composer.internal.snapshot.ComposerSnapshotHandler;
import org.sonatype.nexus.repository.content.browse.BrowseFacet;
import org.sonatype.nexus.repository.content.search.SearchFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.AlwaysMatcher;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * Composer hosted repository recipe.
 *
 * @since 3.31
 */
@Named(ComposerHostedRecipe.NAME)
@Singleton
public class ComposerHostedRecipe
    extends RecipeSupport
    implements org.sonatype.nexus.repository.composer.recipes.ComposerHostedRecipe
{
  public static final String NAME = "composer-hosted";

  @Inject
  Provider<ComposerSecurityFacet> securityFacet;

  @Inject
  Provider<ConfigurableViewFacet> viewFacet;

  @Inject
  Provider<ComposerContentFacet> composerContentFacet;

  @Inject
  Provider<ComposerHostedFacet> composerHostedFacet;

  @Inject
  Provider<BrowseFacet> browseFacet;

  @Inject
  Provider<ComposerLastAssetMaintenanceFacet> maintenanceFacet;

  @Inject
  Provider<ComposerHostedSnapshotFacet> hostedSnapshotFacet;

  @Inject
  Provider<ComposerKeyValueFacet> composerKeyValueFacet;

  @Inject
  Provider<ComposerHostedMetadataFacet> composerHostedMetadataFacet;

  @Inject
  TimingHandler timingHandler;

  @Inject
  SecurityHandler securityHandler;

  @Inject
  ExceptionHandler exceptionHandler;

  @Inject
  HandlerContributor handlerContributor;

  @Inject
  ConditionalRequestHandler conditionalRequestHandler;

  @Inject
  PartialFetchHandler partialFetchHandler;

  @Inject
  ContentHeadersHandler contentHeadersHandler;

  @Inject
  LastDownloadedHandler lastDownloadedHandler;

  @Inject
  ComposerSnapshotHandler snapshotHandler;

  @Inject
  ComposerHostedHandler hostedHandler;

  @Inject
  Provider<SearchFacet> searchFacet;

  @Inject
  public ComposerHostedRecipe(
      @Named(HostedType.NAME) final Type type,
      @Named(ComposerFormat.NAME) final Format format)
  {
    super(type, format);
    log.info("hosted/ComposerHostedRecipe created");
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(composerContentFacet.get());
    repository.attach(composerHostedFacet.get());
    repository.attach(maintenanceFacet.get());
    repository.attach(browseFacet.get());
    repository.attach(searchFacet.get());
    repository.attach(hostedSnapshotFacet.get());
    repository.attach(composerKeyValueFacet.get());
    repository.attach(composerHostedMetadataFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    builder.route(new Route.Builder().matcher(new AlwaysMatcher())
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(lastDownloadedHandler)
        .handler(snapshotHandler)
        .handler(hostedHandler).create());

    builder.defaultHandlers(notFound());
    facet.configure(builder.create());
    return facet;
  }
}
