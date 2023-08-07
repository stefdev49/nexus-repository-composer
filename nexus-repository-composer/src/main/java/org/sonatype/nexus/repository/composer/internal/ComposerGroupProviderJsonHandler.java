/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2018-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.composer.internal;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.composer.external.ComposerJsonProcessor;
import org.sonatype.nexus.repository.view.Content;
import org.sonatype.nexus.repository.view.Payload;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handler for merging provider JSON files together.
 */
@Named
@Singleton
public class ComposerGroupProviderJsonHandler
    extends ComposerGroupMergingHandler
{
  private final ComposerJsonProcessor composerJsonProcessor;

  @Inject
  public ComposerGroupProviderJsonHandler(final ComposerJsonProcessor composerJsonProcessor) {
    this.composerJsonProcessor = checkNotNull(composerJsonProcessor);
  }

  @Override
  protected Content merge(final Repository repository, final List<Payload> payloads) throws IOException {
    return composerJsonProcessor.mergeProviderJson(repository, payloads, DateTime.now());
  }
}
