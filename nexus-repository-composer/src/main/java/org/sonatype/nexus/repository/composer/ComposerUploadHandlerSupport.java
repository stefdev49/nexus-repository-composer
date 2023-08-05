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

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.internal.ComposerFormat;
import org.sonatype.nexus.repository.importtask.ImportFileConfiguration;
import org.sonatype.nexus.repository.rest.UploadDefinitionExtension;
import org.sonatype.nexus.repository.security.ContentPermissionChecker;
import org.sonatype.nexus.repository.security.VariableResolverAdapter;
import org.sonatype.nexus.repository.upload.*;
import org.sonatype.nexus.repository.upload.UploadFieldDefinition.Type;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.PartPayload;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;

/**
 * Common base for composer upload handlers
 * The package name consists of a vendor name and a project name joined by a /
 */
public abstract class ComposerUploadHandlerSupport
    extends UploadHandlerSupport
{
  protected static final String FILENAME = "filename";

  protected static final String VENDOR = "vendor";

  protected static final String VENDOR_HELP_TEXT = "Component Vendor";

  protected static final String PROJECT = "project";

  protected static final String NAME_HELP_TEXT = "Component Project";

  protected static final String VERSION = "version";

  protected static final String VERSION_HELP_TEXT = "Component Version";

  protected static final String FIELD_GROUP_NAME = "Component attributes";

  protected final ContentPermissionChecker contentPermissionChecker;

  protected final VariableResolverAdapter variableResolverAdapter;

  protected final boolean datastoreEnabled;

  protected UploadDefinition definition;

  protected ComposerUploadHandlerSupport(
      final ContentPermissionChecker contentPermissionChecker,
      final VariableResolverAdapter variableResolverAdapter,
      final Set<UploadDefinitionExtension> uploadDefinitionExtensions,
      final boolean datastoreEnabled)
  {
    super(uploadDefinitionExtensions);
    this.contentPermissionChecker = contentPermissionChecker;
    this.variableResolverAdapter = variableResolverAdapter;
    this.datastoreEnabled = datastoreEnabled;
  }

  @Override
  public UploadResponse handle(final Repository repository, final ComponentUpload upload) throws IOException {
    log.info("Handling component upload for repository {}", repository.getName());

    String vendor = upload.getFields().get(VENDOR).trim();
    String project = upload.getFields().get(PROJECT).trim();
    String version = upload.getFields().get(VERSION).trim();

    //Data holders for populating the UploadResponse
    Map<String,PartPayload> pathToPayload = new LinkedHashMap<>();

    for (AssetUpload asset : upload.getAssetUploads()) {
      String path = normalizePath(vendor + '/' + project + '/' + version + '/' + vendor + '-' + project + '-' + version + ".zip");

      String pathWithPrefix = datastoreEnabled ? prependIfMissing(path, "/") : path;
      ensurePermitted(repository.getName(), ComposerFormat.NAME, pathWithPrefix, emptyMap());

      pathToPayload.put(path, asset.getPayload());
    }

    List<Content> responseContents = getResponseContents(repository, pathToPayload);

    return new UploadResponse(responseContents, new ArrayList<>(pathToPayload.keySet()));
  }

  protected abstract List<Content> getResponseContents(final Repository repository, final Map<String, PartPayload> pathToPayload)
      throws IOException;

  @Override
  public Content handle(
      final Repository repository,
      final File content,
      final String path)
      throws IOException
  {
    // TODO: Remove this handler once all formats have been converted to work with ImportFileConfiguration
    return handle(new ImportFileConfiguration(repository, content, path));
  }

  @Override
  public Content handle(final ImportFileConfiguration configuration) throws IOException {

    ensurePermitted(configuration.getRepository().getName(), ComposerFormat.NAME, configuration.getAssetName(), emptyMap());

    return doPut(configuration);
  }

  protected abstract Content doPut(final ImportFileConfiguration configuration) throws IOException;

  protected String normalizePath(final String path) {
    String result = path.replaceAll("/+", "/");

    if (result.startsWith("/")) {
      result = result.substring(1);
    }

    if (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }

    return result;
  }

  @Override
  public UploadDefinition getDefinition() {
    if (definition == null) {
      List<UploadFieldDefinition> componentFields = new ArrayList<>();
      componentFields.add(new UploadFieldDefinition(VENDOR, VENDOR_HELP_TEXT, false, Type.STRING, FIELD_GROUP_NAME));
      componentFields.add(new UploadFieldDefinition(PROJECT, NAME_HELP_TEXT, false, Type.STRING, FIELD_GROUP_NAME));
      componentFields.add(new UploadFieldDefinition(VERSION, VERSION_HELP_TEXT, false, Type.STRING, FIELD_GROUP_NAME));
          definition = getDefinition(ComposerFormat.NAME, false, componentFields,
          singletonList(new UploadFieldDefinition(FILENAME,false, Type.STRING)),
          new UploadRegexMap("(.*)", FILENAME));
    }
    return definition;
  }

  @Override
  public VariableResolverAdapter getVariableResolverAdapter() {
    return variableResolverAdapter;
  }

  @Override
  public ContentPermissionChecker contentPermissionChecker() {
    return contentPermissionChecker;
  }

  @Override
  public boolean supportsExportImport() {
    return true;
  }
}
