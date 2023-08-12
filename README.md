<!--

    Sonatype Nexus (TM) Open Source Version
    Copyright (c) 2018-present Sonatype, Inc.
    All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.

    This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
    which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.

    Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
    of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
    Eclipse Foundation. All other trademarks are the property of their respective owners.

-->
# Nexus Repository Composer Format

This repository originated in a fork of the [Sonatype Nexus Repository Composer Format](https://github.com/sonatype-nexus-community/nexus-repository-composer) plugin.
But due to lack of features and its experimental status, it has been rebooted using the nexus-repository-raw and composer tidbits from the original repository.

It is a work in progress, currently it supports only orientdb as storage backend.
The following features are implemented:
- proxy repositories
- group repositories
- rest api to configure composer repositories
- support for upload package GUI
- search composer packages

## BUGS

- hosted not usable :
```log
Using HTTP basic authentication with username "admin"
Downloading http://localhost:8081/repository/composer-group/packages.json if modified
[304] http://localhost:8081/repository/composer-group/packages.json
Downloading http://localhost:8081/repository/composer-proxy/p2/stefdev49/demo.json
[404] http://localhost:8081/repository/composer-proxy/p2/stefdev49/demo.json
Downloading http://localhost:8081/repository/composer-proxy/p2/stefdev49/demo~dev.json
[404] http://localhost:8081/repository/composer-proxy/p2/stefdev49/demo~dev.json
```

Hosted repositroy has to be moved first :
```log
Downloading http://localhost:8081/repository/composer-group/packages.json if modified
[200] http://localhost:8081/repository/composer-group/packages.json
Writing /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/packages.json into cache
Downloading http://localhost:8081/repository/composer-hosted/p2/stefdev49/demo.json
[200] http://localhost:8081/repository/composer-hosted/p2/stefdev49/demo.json
Writing /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/provider-stefdev49~demo.json into cache
Reading /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/provider-stefdev49~demo.json from cache
Reading /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/provider-stefdev49~demo.json from cache
Downloading http://localhost:8081/repository/composer-hosted/p2/stefdev49/demo~dev.json
[200] http://localhost:8081/repository/composer-hosted/p2/stefdev49/demo~dev.json
Writing /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/provider-stefdev49~demo~dev.json into cache
Reading /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/provider-stefdev49~demo.json from cache
Reading /home/stef/.cache/composer/repo/http---localhost-8081-repository-composer-group/provider-stefdev49~demo~dev.json from cache
```

### Upload compsoer packages to hosted repositories

```bash
curl -v --user 'user:pass' --upload-file example.zip http://localhost:8081/repository/composer-hosted/packages/upload/vendor/project/version
```