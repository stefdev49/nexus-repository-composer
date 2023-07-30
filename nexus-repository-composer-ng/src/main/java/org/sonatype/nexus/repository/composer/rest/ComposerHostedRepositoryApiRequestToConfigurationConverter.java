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
package org.sonatype.nexus.repository.composer.rest;

import javax.inject.Named;

import org.sonatype.nexus.repository.config.Configuration;
import org.sonatype.nexus.repository.rest.api.HostedRepositoryApiRequestToConfigurationConverter;

/**
 * @since 3.20
 */
@Named
public class ComposerHostedRepositoryApiRequestToConfigurationConverter
    extends HostedRepositoryApiRequestToConfigurationConverter<ComposerHostedRepositoryApiRequest>
{
  @Override
  public Configuration convert(final ComposerHostedRepositoryApiRequest request) {
    Configuration configuration = super.convert(request);
    configuration.attributes("composer").set("distribution", request.getComposer().getDistribution());
    configuration.attributes("composerSigning").set("keypair", request.getComposerSigning().getKeypair());
    configuration.attributes("composerSigning").set("passphrase", request.getComposerSigning().getPassphrase());
    return configuration;
  }
}