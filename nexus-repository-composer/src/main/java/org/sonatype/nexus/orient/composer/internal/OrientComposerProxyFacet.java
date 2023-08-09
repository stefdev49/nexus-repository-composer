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
package org.sonatype.nexus.orient.composer.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.sonatype.nexus.repository.cache.CacheInfo;
import org.sonatype.nexus.repository.composer.external.ComposerJsonProcessor;
import org.sonatype.nexus.repository.composer.internal.AssetKind;
import org.sonatype.nexus.repository.proxy.ProxyFacetSupport;
import org.sonatype.nexus.repository.view.*;
import org.sonatype.nexus.repository.view.matchers.token.TokenMatcher;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.orient.composer.internal.OrientComposerRecipeSupport.*;
import static org.sonatype.nexus.repository.composer.internal.ComposerPathUtils.*;
import static org.sonatype.nexus.repository.http.HttpMethods.GET;

/**
 * @since 3.0
 */
@Named
public class OrientComposerProxyFacet
    extends ProxyFacetSupport
{
  private static final String PACKAGES_JSON = "packages.json";

  private static final String LIST_JSON = "packages/list.json";

  private final ComposerJsonProcessor composerJsonProcessor;

  @Inject
  public OrientComposerProxyFacet(ComposerJsonProcessor composerJsonProcessor) {
    this.composerJsonProcessor = composerJsonProcessor;
  }

  @Override
  protected Content getCachedContent(final Context context) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        return content().get(PACKAGES_JSON);
      case LIST:
        return content().get(LIST_JSON);
      case PROVIDER:
        return content().get(buildProviderPath(context));
      case PACKAGE:
        return content().get(buildPackagePath(context));
      case ZIPBALL:
        return content().get(buildZipballPath(context));
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected void indicateVerified(final Context context, final Content content, final CacheInfo cacheInfo) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        content().setCacheInfo(PACKAGES_JSON, content, cacheInfo);
        break;
      case LIST:
        content().setCacheInfo(LIST_JSON, content, cacheInfo);
        break;
      case PROVIDER:
        content().setCacheInfo(buildProviderPath(context), content, cacheInfo);
        break;
      case PACKAGE:
        content().setCacheInfo(buildPackagePath(context), content, cacheInfo);
        break;
      case ZIPBALL:
        content().setCacheInfo(buildZipballPath(context), content, cacheInfo);
        break;
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected Content store(final Context context, final Content content) throws IOException {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case PACKAGES:
        return content().put(PACKAGES_JSON, generatePackagesJson(context), assetKind);
      case LIST:
        return content().put(LIST_JSON, content, assetKind);
      case PROVIDER:
        return content().put(buildProviderPath(context), content, assetKind);
      case PACKAGE:
        return content().put(buildPackagePath(context), content, assetKind);
      case ZIPBALL:
        return content().put(buildZipballPath(context), content, assetKind);
      default:
        throw new IllegalStateException();
    }
  }

  @Override
  protected String getUrl(@Nonnull final Context context) {
    AssetKind assetKind = context.getAttributes().require(AssetKind.class);
    switch (assetKind) {
      case ZIPBALL:
        return getZipballUrl(context);
      default:
        return context.getRequest().getPath().substring(1);
    }
  }

  private String getZipballUrl(final Context context) {
    try {
      TokenMatcher.State state = context.getAttributes().require(TokenMatcher.State.class);
      Map<String, String> tokens = state.getTokens();
      String vendor = tokens.get(VENDOR_TOKEN);
      String project = tokens.get(PROJECT_TOKEN);
      String version = tokens.get(VERSION_TOKEN);

      // try v2 package
      try {
        String path = "/" + buildPackagePath(vendor, project);
        Payload payload = getPackagePayload(context, path);
        if (payload != null) {
          return composerJsonProcessor.getDistUrlFromPackage(vendor, project, version, payload);
        }
      }
      catch (Exception e) {
        // ignored because we have a fallback
      }

      // try v2 package (dev versions)
      try {
        String path = "/" + buildPackagePathForDevVersions(vendor, project);
        Payload payload = getPackagePayload(context, path);
        String url = composerJsonProcessor.getDistUrlFromPackage(vendor, project, version, payload);
        if (payload != null) {
          return composerJsonProcessor.getDistUrlFromPackage(vendor, project, version, payload);
        }
      }
      catch (Exception e) {
        // ignored because we have a fallback
      }

      // try v1 provider
      String path = "/" + buildProviderPath(vendor, project);
      Payload payload = getProviderPayload(context, path);
      if (payload == null) {
        throw new NonResolvableProviderJsonException(
                String.format("No provider found for vendor %s, project %s, version %s", vendor, project, version));
      } else {
        return composerJsonProcessor.getDistUrl(vendor, project, version, payload);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }

  private OrientComposerContentFacet content() {
    return getRepository().facet(OrientComposerContentFacet.class);
  }

  private Payload getPackagePayload(final Context context, String path) throws Exception {
    Request request = new Request.Builder().action(GET).path(path)
            .attribute(OrientComposerContentHandler.DO_NOT_REWRITE, "true").build();
    Response response = getRepository().facet(ViewFacet.class).dispatch(request, context);
    return response.getPayload();
  }

  private Payload getProviderPayload(final Context context, String path) throws Exception {
    return getPackagePayload(context, path);
  }

  @VisibleForTesting
  static class NonResolvableProviderJsonException
          extends RuntimeException
  {
    public NonResolvableProviderJsonException(final String message) {
      super(message);
    }
  }

  private Content generatePackagesJson(final Context context) throws IOException {
    try {
      // TODO: Better logging and error checking on failure/non-200 scenarios
      Request request = new Request.Builder().action(GET).path("/" + LIST_JSON).build();
      Response response = getRepository().facet(ViewFacet.class).dispatch(request, context);
      Payload payload = checkNotNull(response.getPayload());
      return composerJsonProcessor.generatePackagesFromList(getRepository(), payload);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    catch (Exception e) {
      Throwables.throwIfUnchecked(e);
      throw new RuntimeException(e);
    }
  }
}
