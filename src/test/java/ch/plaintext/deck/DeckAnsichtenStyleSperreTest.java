/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package ch.plaintext.deck;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Karte 2269: Die Deck-Ansichten tragen keine {@code style="…"}-Attribute und keine
 * {@code @media}-Blöcke in einem eingebetteten {@code <style>}. Das ist die lokale Fassung der
 * Klinke «Massnahme 13» (InlineStyleSperrklinkeVertragTest) aus der App, die hier nicht greift,
 * weil sie App-Verträge auf den App-Bestand prüft.
 *
 * <p><b>Warum ein Parser und keine Textsuche.</b> {@code deckchat.xhtml} erklärt in einem
 * XML-Kommentar, warum die Balkenbreite als CSS-Klasse kommt — und schreibt dabei
 * {@code style="width:..%"} hin. Eine Suche nach der Zeichenkette findet diese Begründung und
 * meldet einen Verstoss, den es nicht gibt. Der App-Zähler täte genau das
 * ({@code inhalt.indexOf(" style=\"")}). Ein Kommentar ist kein Attribut; das entscheidet der
 * Parser, nicht eine Zeichenkette.</p>
 *
 * <p><b>Warum eine Mindestmenge geprüft wird.</b> Ein Lauf über null Dateien ist grün und sagt
 * nichts. Der Archtests-JAR löst das für mehrmodulige Repos mit festgehaltenen Zahlen; hier steht
 * die kleinere Variante: der Scan muss die bekannten Ansichten wirklich gefunden haben.</p>
 */
@DisplayName("Karte 2269: Deck-Ansichten ohne Inline-Styles")
class DeckAnsichtenStyleSperreTest {

    /** Die Ansichten, die es heute gibt. Wächst das Modul, wächst die Liste — sie ist der Beleg,
     *  dass der Scan nicht ins Leere greift. */
    private static final List<String> BEKANNTE_ANSICHTEN = List.of("deck.xhtml", "deckchat.xhtml");

    private static Path ansichtsWurzel;

    @BeforeAll
    static void wurzelFinden() {
        ansichtsWurzel = findeAnsichtsWurzel();
    }

    @Test
    @DisplayName("der Scan findet die Ansichten, statt über null Dateien grün zu sein")
    void scanFindetDieAnsichten() {
        List<Path> gefunden = ansichten();
        List<String> namen = gefunden.stream().map(p -> p.getFileName().toString()).toList();
        for (String bekannt : BEKANNTE_ANSICHTEN) {
            assertTrue(namen.contains(bekannt),
                    "Ansicht " + bekannt + " nicht im Scan (" + ansichtsWurzel + "). Gefunden: " + namen
                            + " — wurde sie verschoben? Dann hier den Pfad nachziehen. Ein Scan ohne sie"
                            + " ist grün und prüft nichts.");
        }
    }

    @Test
    @DisplayName("keine Ansicht trägt ein style-Attribut")
    void keineAnsichtTraegtEinStyleAttribut() {
        List<String> verstoesse = new ArrayList<>();
        for (Path ansicht : ansichten()) {
            List<String> treffer = styleAttribute(lies(ansicht));
            if (!treffer.isEmpty()) {
                verstoesse.add(ansicht.getFileName() + ": style= an " + treffer);
            }
        }
        assertTrue(verstoesse.isEmpty(),
                "Inline-Styles in Deck-Ansichten — bitte eine Klasse in deck/deck.css definieren und"
                        + " styleClass benutzen (Massnahme 13):\n  " + String.join("\n  ", verstoesse));
    }

    @Test
    @DisplayName("kein eingebettetes <style> mit @media")
    void keinEingebettetesMediaQuery() {
        List<String> verstoesse = new ArrayList<>();
        for (Path ansicht : ansichten()) {
            if (hatMediaQuery(lies(ansicht))) {
                verstoesse.add(ansicht.getFileName().toString());
            }
        }
        assertTrue(verstoesse.isEmpty(),
                "Eingebetteter @media-Block in einer Deck-Ansicht — das Stapeln macht das Grid,"
                        + " ein Breakpoint gehört in deck/deck.css und nicht in die Seite:\n  "
                        + String.join("\n  ", verstoesse));
    }

    // --- Kontrollen in beide Richtungen -----------------------------------------------------

    @Test
    @DisplayName("Negativkontrolle: der Zähler sieht ein Attribut, aber keinen Kommentar")
    void zaehlerGreiftNurAufAttribute() {
        assertEquals(List.of("div"),
                styleAttribute("<div><div style=\"width:8%\"/></div>"),
                "ein echtes style-Attribut muss gefunden werden");
        assertEquals(List.of(),
                styleAttribute("<div><!-- nicht als style=\"width:8%\" --></div>"),
                "ein style= in einem XML-Kommentar ist kein Attribut; wer ihn zählt, meldet eine"
                        + " Begründung als Verstoss (Karte 2258: eine Erkennung ohne Grenze)");
        assertEquals(List.of("h:outputLabel"),
                styleAttribute("<h:outputLabel xmlns:h=\"http://java.sun.com/jsf/html\""
                        + " style=\"color:red\"/>"),
                "auch ein JSF-Element mit Praefix ist ein Element");
        assertEquals(List.of(),
                styleAttribute("<div styleClass=\"deck-leer\"/>"),
                "styleClass ist der erlaubte Weg und darf nicht als style durchgehen");
    }

    @Test
    @DisplayName("Negativkontrolle: @media wird im eingebetteten <style> gesehen, im Kommentar nicht")
    void mediaQueryZaehlerGreift() {
        assertTrue(hatMediaQuery("<div><style>@media (max-width:600px){.a{color:red}}</style></div>"),
                "ein eingebetteter @media-Block muss gefunden werden");
        assertFalse(hatMediaQuery("<div><!-- kein @media hier --></div>"),
                "ein @media in einem Kommentar ist kein Block");
        assertFalse(hatMediaQuery("<div class=\"deck-leer\"/>"),
                "ohne style-Element gibt es nichts zu finden");
    }

    // ---------------------------------------------------------------------------------------

    /** Alle {@code .xhtml} unter {@code META-INF/resources}, sortiert, damit Meldungen stabil sind. */
    private static List<Path> ansichten() {
        try (Stream<Path> s = Files.walk(ansichtsWurzel)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".xhtml"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Ansichten unter " + ansichtsWurzel + " nicht lesbar", e);
        }
    }

    /** Elementnamen, die ein {@code style}-Attribut tragen. Leer heisst: keins. */
    static List<String> styleAttribute(String inhalt) {
        List<String> treffer = new ArrayList<>();
        var dokument = parse(inhalt);
        if (dokument == null) {
            return treffer;
        }
        var knoten = dokument.getElementsByTagName("*");
        for (int i = 0; i < knoten.getLength(); i++) {
            var element = (org.w3c.dom.Element) knoten.item(i);
            var attribute = element.getAttributes();
            for (int a = 0; a < attribute.getLength(); a++) {
                var attr = (org.w3c.dom.Attr) attribute.item(a);
                // localName traegt keinen Praefix; styleClass heisst anders und faellt nicht durch.
                if ("style".equals(attr.getLocalName())) {
                    treffer.add(element.getTagName());
                    break;
                }
            }
        }
        return treffer;
    }

    /** Wahr, wenn ein {@code <style>}-Element einen {@code @media}-Block enthält. */
    static boolean hatMediaQuery(String inhalt) {
        var dokument = parse(inhalt);
        if (dokument == null) {
            return false;
        }
        var stilelemente = dokument.getElementsByTagName("style");
        for (int i = 0; i < stilelemente.getLength(); i++) {
            String text = stilelemente.item(i).getTextContent();
            if (text != null && text.contains("@media")) {
                return true;
            }
        }
        return false;
    }

    private static org.w3c.dom.Document parse(String inhalt) {
        try {
            DocumentBuilderFactory fabrik = DocumentBuilderFactory.newInstance();
            fabrik.setNamespaceAware(true);
            // Kein Netz, keine externen Entities: die Eingabe ist der eigene Quelltext, aber ein
            // Parser, der DOCTYPEs aufloest, ist eine Tuer, die hier niemand braucht.
            fabrik.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrik.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrik.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder bauer = fabrik.newDocumentBuilder();
            return bauer.parse(new ByteArrayInputStream(inhalt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Testeingabe ist kein wohlgeformtes XML: " + e.getMessage(), e);
        }
    }

    private static String lies(Path datei) {
        try {
            return Files.readString(datei, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Datei " + datei + " nicht lesbar", e);
        }
    }

    private static Path findeAnsichtsWurzel() {
        Path kandidat = Paths.get("").toAbsolutePath();
        while (kandidat != null) {
            Path ansichten = kandidat.resolve("src/main/resources/META-INF/resources");
            if (Files.isDirectory(ansichten) && Files.isRegularFile(kandidat.resolve("pom.xml"))) {
                return ansichten;
            }
            kandidat = kandidat.getParent();
        }
        throw new IllegalStateException(
                "Kein src/main/resources/META-INF/resources über " + Paths.get("").toAbsolutePath()
                        + " gefunden. Der Test muss im Modul-Verzeichnis laufen (mvn test).");
    }
}
