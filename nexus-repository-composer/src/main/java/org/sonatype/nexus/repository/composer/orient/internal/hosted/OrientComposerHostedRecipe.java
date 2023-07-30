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
package org.sonatype.nexus.repository.composer.orient.internal.hosted;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Format;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.Type;
import org.sonatype.nexus.repository.composer.ComposerFormat;
import org.sonatype.nexus.repository.composer.internal.ComposerRecipeSupport;
import org.sonatype.nexus.repository.composer.internal.ComposerSecurityFacet;
import org.sonatype.nexus.repository.composer.internal.snapshot.ComposerSnapshotHandler;
import org.sonatype.nexus.repository.composer.orient.ComposerRestoreFacet;
import org.sonatype.nexus.repository.composer.orient.internal.OrientComposerFacetImpl;
import org.sonatype.nexus.repository.attributes.AttributesFacet;
import org.sonatype.nexus.repository.http.PartialFetchHandler;
import org.sonatype.nexus.repository.search.ElasticSearchFacet;
import org.sonatype.nexus.repository.security.SecurityHandler;
import org.sonatype.nexus.repository.storage.StorageFacet;
import org.sonatype.nexus.repository.storage.UnitOfWorkHandler;
import org.sonatype.nexus.repository.types.HostedType;
import org.sonatype.nexus.repository.view.ConfigurableViewFacet;
import org.sonatype.nexus.repository.view.Route;
import org.sonatype.nexus.repository.view.Router;
import org.sonatype.nexus.repository.view.ViewFacet;
import org.sonatype.nexus.repository.view.handlers.ConditionalRequestHandler;
import org.sonatype.nexus.repository.view.handlers.ContentHeadersHandler;
import org.sonatype.nexus.repository.view.handlers.ExceptionHandler;
import org.sonatype.nexus.repository.view.handlers.FormatHighAvailabilitySupportHandler;
import org.sonatype.nexus.repository.view.handlers.HandlerContributor;
import org.sonatype.nexus.repository.view.handlers.LastDownloadedHandler;
import org.sonatype.nexus.repository.view.handlers.TimingHandler;
import org.sonatype.nexus.repository.view.matchers.AlwaysMatcher;

import static org.sonatype.nexus.repository.http.HttpHandlers.notFound;

/**
 * @since 3.17
 */
@Named(OrientComposerHostedRecipe.NAME)
@Singleton
public class OrientComposerHostedRecipe
    extends ComposerRecipeSupport
{

  public static final String NAME = "composer-hosted";

  @Inject
  Provider<ComposerSecurityFacet> securityFacet;

  @Inject
  FormatHighAvailabilitySupportHandler highAvailabilitySupportHandler;

  @Inject
  Provider<ConfigurableViewFacet> viewFacet;

  @Inject
  Provider<OrientComposerFacetImpl> composerFacet;

  @Inject
  Provider<ComposerRestoreFacet> composerRestoreFacet;

  @Inject
  Provider<OrientComposerHostedFacet> composerHostedFacet;

  @Inject
  Provider<OrientComposerHostedSnapshotFacet> snapshotFacet;

  @Inject
  Provider<StorageFacet> storageFacet;

  @Inject
  Provider<AttributesFacet> attributesFacet;

  @Inject
  Provider<OrientComposerHostedComponentMaintenanceFacet> componentMaintenance;

  @Inject
  Provider<ElasticSearchFacet> searchFacet;

  @Inject
  ExceptionHandler exceptionHandler;

  @Inject
  TimingHandler timingHandler;

  @Inject
  SecurityHandler securityHandler;

  @Inject
  PartialFetchHandler partialFetchHandler;

  @Inject
  UnitOfWorkHandler unitOfWorkHandler;

  @Inject
  OrientComposerHostedHandler hostedHandler;

  @Inject
  ConditionalRequestHandler conditionalRequestHandler;

  @Inject
  ContentHeadersHandler contentHeadersHandler;

  @Inject
  ComposerSnapshotHandler snapshotHandler;

  @Inject
  LastDownloadedHandler lastDownloadedHandler;

  @Inject
  HandlerContributor handlerContributor;

  @Inject
  public OrientComposerHostedRecipe(@Named(HostedType.NAME) final Type type,
                               @Named(ComposerFormat.NAME) final Format format)
  {
    super(type, format);
  }

  @Override
  public void apply(final Repository repository) throws Exception {
    repository.attach(securityFacet.get());
    repository.attach(configure(viewFacet.get()));
    repository.attach(storageFacet.get());
    repository.attach(composerFacet.get());
    repository.attach(composerRestoreFacet.get());
    repository.attach(composerHostedFacet.get());
    repository.attach(snapshotFacet.get());
    repository.attach(attributesFacet.get());
    repository.attach(componentMaintenance.get());
    repository.attach(searchFacet.get());
  }

  private ViewFacet configure(final ConfigurableViewFacet facet) {
    Router.Builder builder = new Router.Builder();

    builder.route(new Route.Builder().matcher(new AlwaysMatcher())
        .handler(timingHandler)
        .handler(securityHandler)
        .handler(highAvailabilitySupportHandler)
        .handler(exceptionHandler)
        .handler(handlerContributor)
        .handler(conditionalRequestHandler)
        .handler(partialFetchHandler)
        .handler(contentHeadersHandler)
        .handler(unitOfWorkHandler)
        .handler(lastDownloadedHandler)
        .handler(snapshotHandler)
        .handler(hostedHandler).create());

    builder.defaultHandlers(notFound());
    facet.configure(builder.create());
    return facet;
  }
}
