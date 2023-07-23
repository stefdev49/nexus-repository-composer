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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.sonatype.goodies.httpfixture.server.fluent.Behaviours;
import org.sonatype.goodies.httpfixture.server.fluent.Server;
import org.sonatype.nexus.pax.exam.NexusPaxExamSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.http.HttpStatus;
import org.sonatype.nexus.testsuite.testsupport.NexusITSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.bytes;
import static org.sonatype.nexus.testsuite.testsupport.FormatClientSupport.status;
import static org.testcontainers.shaded.org.hamcrest.text.MatchesPattern.matchesPattern;

public class ComposerHostedIT
    extends ComposerITSupport
{
  private static final String NAME_VENDOR = "rjkip";

  private static final String NAME_PROJECT = "ftp-php";

  private static final String NAME_VERSION = "v1.1.0";

  private static final String NAME_PACKAGES = "packages";

  private static final String NAME_LIST = "list";

  private static final String EXTENSION_JSON = ".json";

  private static final String EXTENSION_ZIP = ".zip";

  private static final String FILE_PROVIDER = NAME_PROJECT + EXTENSION_JSON;

  private static final String FILE_PACKAGES = NAME_PACKAGES + EXTENSION_JSON;

  private static final String FILE_LIST = NAME_LIST + EXTENSION_JSON;

  private static final String FILE_ZIPBALL = NAME_VENDOR + "-" + NAME_PROJECT + "-" + NAME_VERSION + EXTENSION_ZIP;

  private static final String PACKAGE_BASE_PATH = "p/" + NAME_VENDOR + "/";

  private static final String LIST_BASE_PATH = "packages/";

  private static final String BAD_PATH = "/this/path/is/not/valid";

  private static final String VALID_PROVIDER_URL = PACKAGE_BASE_PATH + FILE_PROVIDER;

  private static final String VALID_LIST_URL = LIST_BASE_PATH + FILE_LIST;

  private static final String VALID_ZIPBALL_BASE_URL = NAME_VENDOR + "/" + NAME_PROJECT + "/" + NAME_VERSION;
  private static final String VALID_ZIPBALL_URL = VALID_ZIPBALL_BASE_URL + "/" + FILE_ZIPBALL;

  private static final String ZIPBALL_FILE_NAME = "rjkip-ftp-php-v1.1.0.zip";
  public static final String COMPOSER_TEST_HOSTED = "composer-test-hosted";

  private ComposerClient hostedClient;

  private Server server;

  @Configuration
  public static Option[] configureNexus() {
    return NexusPaxExamSupport.options(
        NexusITSupport.configureNexusBase(),
        nexusFeature("org.sonatype.nexus.plugins", "nexus-repository-composer")
    );
  }

  @Before
  public void setup() throws Exception {
    server = Server.withPort(0)
        .serve("/" + FILE_PACKAGES)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_PACKAGES)))
        .serve("/" + VALID_LIST_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_LIST)))
        .serve("/" + VALID_PROVIDER_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(FILE_PROVIDER)))
        .serve("/" + VALID_ZIPBALL_URL)
        .withBehaviours(Behaviours.file(testData.resolveFile(ZIPBALL_FILE_NAME)))
        .start();

    Repository hostedRepo = repos.createComposerHosted(COMPOSER_TEST_HOSTED);
    hostedClient = composerClient(hostedRepo);

  }

  @Test
  public void nonExistingPackageProduces404() throws Exception {
    assertThat(status(hostedClient.get(BAD_PATH)), is(HttpStatus.NOT_FOUND));
  }

  @Test
  public void putAndGetOk() throws Exception {
    // given
    assertThat(status(hostedClient.get(VALID_ZIPBALL_URL)), is(HttpStatus.NOT_FOUND));

    // when
    assertThat(hostedClient.put(NAME_PACKAGES + "/upload/" + VALID_ZIPBALL_BASE_URL, testData.resolveFile(ZIPBALL_FILE_NAME)), is(200));

    // then
    assertThat(status(hostedClient.get(VALID_ZIPBALL_URL)), is(HttpStatus.OK));
  }

  @Test
  public void checkRestAPI() throws Exception {
    assertThat(status(hostedClient.get("/service/rest/v1/repositories")), is(HttpStatus.OK));
  }

  @Test
  public void badPutHostedConfigurationByAPI() throws Exception {
    assertThat(hostedClient.put("/service/rest/v1/repositories/composer/proxy/" + COMPOSER_TEST_HOSTED, "bad request"), is(HttpStatus.BAD_REQUEST));
  }

  @Test
  public void getHostedConfigurationByAPI() throws Exception {
    CloseableHttpResponse response = hostedClient.get("/service/rest/v1/repositories/composer/hosted/" + COMPOSER_TEST_HOSTED);
    assertThat(status(response), is(HttpStatus.OK));
    String config = new String(bytes(response));
    // convert to json and check some values
    JsonObject jsonConfig = (JsonObject) new JsonParser().parse(config);
    JsonObject expected = new JsonParser().parse("{\"name\":\"composer-test-hosted\",\"format\":\"composer\",\"online\":true,\"storage\":{\"blobStoreName\":\"default\",\"strictContentTypeValidation\":true,\"writePolicy\":\"ALLOW\"},\"cleanup\":null,\"component\":{\"proprietaryComponents\":false},\"type\":\"hosted\"}").getAsJsonObject();
    // check url field matches http://localhost:[0-9]*/repository/composer-test-hosted
    assertThat(matchesPattern("http://localhost:[0-9]*/repository/" + COMPOSER_TEST_HOSTED).matches(jsonConfig.get("url").getAsString()), is(true));
    // compare ignoring url field
    jsonConfig.remove("url");
    assertThat(jsonConfig, is(expected));

    // set online to false, then update repository
    jsonConfig.remove("online");
    jsonConfig.addProperty("online", false);
    int code = hostedClient.put("/service/rest/v1/repositories/composer/hosted/" + COMPOSER_TEST_HOSTED, jsonConfig.toString());
    assertThat(code, is(HttpStatus.NO_CONTENT));
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }
}
