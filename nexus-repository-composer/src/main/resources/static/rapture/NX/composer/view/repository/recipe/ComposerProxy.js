/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * Repository "Settings" form for a Maven Proxy repository
 *
 * @since 3.0
 */
Ext.define('NX.composer.view.repository.recipe.ComposerProxy', {
  extend: 'NX.composer.view.repository.RepositorySettingsForm',
  alias: 'widget.nx-composer-repository-composer-proxy',
  requires: [
    'NX.composer.view.repository.facet.ReplicationFacet',
    'NX.composer.view.repository.facet.ComposerFacet',
    'NX.composer.view.repository.facet.ProxyFacet',
    'NX.composer.view.repository.facet.StorageFacet',
    'NX.composer.view.repository.facet.RoutingRuleFacet',
    'NX.composer.view.repository.facet.HttpClientFacet',
    'NX.composer.view.repository.facet.NegativeCacheFacet',
    'NX.composer.view.repository.facet.CleanupPolicyFacet'
  ],

  /**
   * @override
   */
  initComponent: function () {
    var me = this;

    me.items = [
      {xtype: 'nx-composer-repository-replication-facet'},
      {xtype: 'nx-composer-repository-composer-facet'},
      {xtype: 'nx-composer-repository-proxy-facet'},
      {xtype: 'nx-composer-repository-storage-facet'},
      {xtype: 'nx-composer-repository-routing-rule-facet'},
      {xtype: 'nx-composer-repository-negativecache-facet'},
      {xtype: 'nx-composer-repository-cleanup-policy-facet'},
      {xtype: 'nx-composer-repository-httpclient-facet'}
    ];

    me.callParent();
  }
});
