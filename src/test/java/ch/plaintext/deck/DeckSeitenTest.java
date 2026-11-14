/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.deck;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Karte 2162: {@code rendered} auf einem nackten HTML-Element wertet Facelets nicht aus. Es schreibt
 * das Attribut ungeprüft in die Seite, und das Element erscheint immer. So trug im ersten Stand jede
 * Karte die Marken «Interaktiv» und «Rückfrage», auch ohne Grund. Der Test hält fest, dass
 * {@code rendered} nur an JSF-Komponenten steht ({@code ui:}, {@code h:}, {@code p:}, {@code f:}).
 */
@DisplayName("Karte 2162: rendered nur an JSF-Komponenten")
class DeckSeitenTest {

    private static final Pattern NACKT_MIT_RENDERED =
            Pattern.compile("<([a-z][a-z0-9]*)(\\s[^>]*)?\\srendered=", Pattern.CASE_INSENSITIVE);

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"META-INF/resources/deck.xhtml", "META-INF/resources/deckchat.xhtml"})
    void keinRenderedAufHtml(String pfad) throws IOException {
        String seite;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(pfad)) {
            assertNotNull(in, pfad + " fehlt im Klassenpfad");
            seite = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher m = NACKT_MIT_RENDERED.matcher(seite);
        boolean gefunden = m.find();
        assertFalse(gefunden, () -> pfad + ": rendered an <" + m.group(1) + ">, das Facelets nicht auswertet");
    }
}
