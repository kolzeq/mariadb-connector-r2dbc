/*
 * Copyright 2020 MariaDB Ab.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mariadb.r2dbc.integration.authentication;

import io.r2dbc.spi.R2dbcNonTransientResourceException;
import java.io.File;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mariadb.r2dbc.*;
import org.mariadb.r2dbc.api.MariadbConnection;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class Sha256PluginTest extends BaseConnectionTest {

  private static String rsaPublicKey;
  private static String cachingRsaPublicKey;
  private static boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

  private static boolean validPath(String path) {
    if (path == null) return false;
    try {
      File f = new File(path);
      return f.exists();
    } catch (Exception e) {
      // eat
    }
    return false;
  }

  @BeforeAll
  public static void init() throws Exception {
    Assumptions.assumeTrue(!isMariaDBServer() && minVersion(5, 7, 0));

    rsaPublicKey = System.getProperty("rsaPublicKey");
    if (!validPath(rsaPublicKey) && minVersion(8, 0, 0)) {
      rsaPublicKey =
          sharedConn
              .createStatement("SELECT @@caching_sha2_password_public_key_path")
              .execute()
              .flatMap(r -> r.map((row, metadata) -> row.get(0, String.class)))
              .blockLast();
      if (!validPath(rsaPublicKey)) {
        rsaPublicKey = System.getenv("TEST_DB_SERVER_PUBLIC_KEY_PATH");
        if (!validPath(rsaPublicKey)) {
          File sslDir = new File(System.getProperty("user.dir") + "/ssl");
          if (sslDir.exists() && sslDir.isDirectory()) {
            rsaPublicKey = System.getProperty("user.dir") + "/ssl/public.key";
          } else rsaPublicKey = null;
        }
      }
    }

    cachingRsaPublicKey = System.getProperty("cachingRsaPublicKey");
    if (!validPath(cachingRsaPublicKey)) {
      cachingRsaPublicKey =
          sharedConn
              .createStatement("SELECT @@sha256_password_public_key_path")
              .execute()
              .flatMap(r -> r.map((row, metadata) -> row.get(0, String.class)))
              .blockLast();
      if (!validPath(cachingRsaPublicKey)) {
        cachingRsaPublicKey = System.getenv("TEST_DB_SERVER_PUBLIC_KEY_PATH");
        if (!validPath(cachingRsaPublicKey)) {
          File sslDir = new File(System.getProperty("user.dir") + "/ssl");
          if (sslDir.exists() && sslDir.isDirectory()) {
            cachingRsaPublicKey = System.getProperty("user.dir") + "/ssl/public.key";
          } else cachingRsaPublicKey = null;
        }
      }
    }

    sharedConn
        .createStatement("DROP USER IF EXISTS 'sha256User'@'%'")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();
    sharedConn
        .createStatement("DROP USER IF EXISTS 'cachingSha256User'@'%'")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();
    sharedConn
        .createStatement("DROP USER IF EXISTS 'cachingSha256User2'@'%'")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();

    String sqlCreateUser;
    String sqlGrant;
    if (minVersion(8, 0, 0)) {
      sqlCreateUser = "CREATE USER 'sha256User'@'%' IDENTIFIED WITH sha256_password BY 'password'";
      sqlGrant = "GRANT ALL PRIVILEGES ON *.* TO 'sha256User'@'%'";
    } else {
      sqlCreateUser = "CREATE USER 'sha256User'@'%'";
      sqlGrant =
          "GRANT ALL PRIVILEGES ON *.* TO 'sha256User'@'%' IDENTIFIED WITH "
              + "sha256_password BY 'password'";
    }
    sharedConn.createStatement(sqlCreateUser).execute().blockLast();
    sharedConn.createStatement(sqlGrant).execute().blockLast();
    if (minVersion(8, 0, 0)) {
      sharedConn
          .createStatement(
              "CREATE USER 'cachingSha256User'@'%'  IDENTIFIED WITH caching_sha2_password BY 'password'")
          .execute()
          .blockLast();
      sharedConn
          .createStatement("GRANT ALL PRIVILEGES ON *.* TO 'cachingSha256User'@'%'")
          .execute()
          .blockLast();
      sharedConn
          .createStatement(
              "CREATE USER 'cachingSha256User2'@'%'  IDENTIFIED WITH caching_sha2_password BY 'password'")
          .execute()
          .blockLast();
      sharedConn
          .createStatement("GRANT ALL PRIVILEGES ON *.* TO 'cachingSha256User2'@'%'")
          .execute()
          .blockLast();
      sharedConn
          .createStatement(
              "CREATE USER 'cachingSha256User3'@'%'  IDENTIFIED WITH caching_sha2_password BY 'password'")
          .execute()
          .blockLast();
      sharedConn
          .createStatement("GRANT ALL PRIVILEGES ON *.* TO 'cachingSha256User3'@'%'")
          .execute()
          .blockLast();
    }
  }

  @AfterAll
  public static void after2() {
    Assumptions.assumeTrue(!isMariaDBServer() && minVersion(5, 7, 0));
    sharedConn
        .createStatement("DROP USER sha256User")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();
    sharedConn
        .createStatement("DROP USER cachingSha256User")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();
    sharedConn
        .createStatement("DROP USER cachingSha256User2")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();
    sharedConn
        .createStatement("DROP USER cachingSha256User3")
        .execute()
        .map(res -> res.getRowsUpdated())
        .onErrorReturn(Mono.empty())
        .blockLast();
  }

  @Test
  public void sha256PluginTestWithServerRsaKey() throws Exception {
    Assumptions.assumeTrue(
        !isWindows && !isMariaDBServer() && rsaPublicKey != null && minVersion(8, 0, 0));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("sha256User")
            .password("password")
            .rsaPublicKey(rsaPublicKey)
            .build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection.close().block();
  }

  @Test
  public void sha256PluginTestWithoutServerRsaKey() throws Exception {
    Assumptions.assumeTrue(!isWindows && !isMariaDBServer() && minVersion(8, 0, 0));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("sha256User")
            .password("password")
            .allowPublicKeyRetrieval(true)
            .build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection.close().block();
  }

  @Test
  public void sha256PluginTestException() throws Exception {
    Assumptions.assumeTrue(!isMariaDBServer() && minVersion(8, 0, 0));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("sha256User")
            .password("password")
            .build();
    new MariadbConnectionFactory(conf)
        .create()
        .as(StepVerifier::create)
        .expectErrorMatches(
            throwable ->
                throwable instanceof R2dbcNonTransientResourceException
                    && throwable
                        .getMessage()
                        .contains("RSA public key is not available client side"))
        .verify();
  }

  @Test
  public void sha256PluginTestSsl() throws Exception {
    Assumptions.assumeTrue(haveSsl(sharedConn));
    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("sha256User")
            .password("password")
            .allowPublicKeyRetrieval(true)
            .sslMode(SslMode.ENABLE_TRUST)
            .build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection.close().block();
  }

  @Test
  public void cachingSha256PluginTestWithServerRsaKey() throws Exception {
    Assumptions.assumeTrue(
        !isWindows && !isMariaDBServer() && cachingRsaPublicKey != null && minVersion(8, 0, 0));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("cachingSha256User")
            .password("password")
            .cachingRsaPublicKey(cachingRsaPublicKey)
            .build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection.close().block();
  }

  @Test
  public void cachingSha256PluginTestWithoutServerRsaKey() throws Exception {
    Assumptions.assumeTrue(!isWindows && rsaPublicKey != null && minVersion(8, 0, 0));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("cachingSha256User2")
            .password("password")
            .allowPublicKeyRetrieval(true)
            .build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection.close().block();

    MariadbConnectionConfiguration conf2 =
        TestConfiguration.defaultBuilder
            .clone()
            .username("cachingSha256User")
            .password("password")
            .build();
    MariadbConnection connection2 = new MariadbConnectionFactory(conf2).create().block();
    connection2.close().block();
  }

  @Test
  public void cachingSha256PluginTestException() throws Exception {
    Assumptions.assumeTrue(!isMariaDBServer() && minVersion(8, 0, 0));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("cachingSha256User3")
            .password("password")
            .build();
    new MariadbConnectionFactory(conf)
        .create()
        .as(StepVerifier::create)
        .expectErrorMatches(
            throwable -> {
              throwable.printStackTrace();
              return throwable instanceof R2dbcNonTransientResourceException
                  && throwable.getMessage().contains("RSA public key is not available client side");
            })
        .verify();
  }

  @Test
  public void cachingSha256PluginTestSsl() throws Exception {
    Assumptions.assumeTrue(!isMariaDBServer() && minVersion(8, 0, 0));
    Assumptions.assumeTrue(haveSsl(sharedConn));

    MariadbConnectionConfiguration conf =
        TestConfiguration.defaultBuilder
            .clone()
            .username("cachingSha256User")
            .password("password")
            .sslMode(SslMode.ENABLE_TRUST)
            .build();
    MariadbConnection connection = new MariadbConnectionFactory(conf).create().block();
    connection.close().block();
    MariadbConnection connection3 = new MariadbConnectionFactory(conf).create().block();
    connection3.close().block();
  }
}
