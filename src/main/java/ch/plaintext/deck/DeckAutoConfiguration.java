/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.deck;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Einbau ohne eigenen Code (Karte 2162): das Jar auf den Klassenpfad, fertig. Spring Boot lädt diese Klasse
 * über {@code META-INF/spring/...AutoConfiguration.imports}; sie meldet Dienst, Seiten-Beans und MCP-Werkzeuge
 * an. Entities und Repositories liegen unter {@code ch.plaintext}, das die Anwendung per {@code @EntityScan}
 * und {@code @EnableJpaRepositories} abdeckt; die Migrationen liegen in {@code db/migration}.
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = DeckAutoConfiguration.class)
public class DeckAutoConfiguration {
}
