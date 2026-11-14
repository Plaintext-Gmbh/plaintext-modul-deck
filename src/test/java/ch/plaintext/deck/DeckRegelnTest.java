/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Karte 2162: die kleinen Regeln des Boards, die still kaputtgehen können.
 *
 * <p>Kein Getter-Test — geprüft wird, was ein Agent oder ein Klick am Brett tatsächlich sieht:
 * dass ein Label nur einmal dasteht, dass «ohne Beschreibung» keine leere Kurzfassung erzeugt und
 * dass ein Tippfehler im Stapelnamen eine lesbare Liste der gültigen Namen zurückgibt, statt still
 * auf einen Vorgabewert zu fallen.</p>
 */
@DisplayName("Karte 2162: Board-Regeln")
class DeckRegelnTest {

    @Test
    @DisplayName("Ein Label wird nur einmal gesetzt und lässt sich einzeln wieder entfernen")
    void labelOhneDoppelung() {
        String einmal = DeckService.labelAendern(null, "dev", true);
        assertEquals("dev", einmal);

        String zweimal = DeckService.labelAendern(einmal, "dev", true);
        assertEquals("dev", zweimal, "dasselbe Label zweimal gesetzt bleibt eines");

        String dazu = DeckService.labelAendern(zweimal, "Zu überprüfen", true);
        assertEquals("dev, Zu überprüfen", dazu);

        String weg = DeckService.labelAendern(dazu, "dev", false);
        assertEquals("Zu überprüfen", weg, "das andere Label bleibt stehen");

        String leer = DeckService.labelAendern("dev", "dev", false);
        assertNull(leer, "letztes Label entfernt = keine Label-Zeile, nicht die leere Zeichenkette");
    }

    @Test
    @DisplayName("Labels werden unabhängig von Gross-/Kleinschreibung verglichen")
    void labelGrossKlein() {
        assertEquals("Dev", DeckService.labelAendern("Dev", "dev", true),
                "ein zweites 'dev' in anderer Schreibweise ist dasselbe Label");
        assertEquals("Dev, rot", DeckService.labelAendern("Dev", "rot", true),
                "ein anderes Label kommt dazu, ohne das erste umzubenennen");
        assertNull(DeckService.labelAendern("dev", "DEV", false),
                "entfernt wird dasselbe Label, auch wenn es anders geschrieben kommt");
    }

    @Test
    @DisplayName("Kurzfassung ist die erste Zeile, gekürzt, ohne Zeilenumbruch")
    void kurzfassung() {
        assertNull(DeckService.kurzfassung(null));
        assertNull(DeckService.kurzfassung("   "));
        assertEquals("Erste Zeile", DeckService.kurzfassung("Erste Zeile\nzweite Zeile"));
        assertEquals("Nur eine", DeckService.kurzfassung("  Nur eine  "));

        String lang = "x".repeat(400);
        String kurz = DeckService.kurzfassung(lang);
        assertEquals(198, kurz.length(), "200 Zeichen minus Platz für das Auslassungszeichen");
        assertTrue(kurz.endsWith("…"), "eine gekürzte Kurzfassung ist als solche erkennbar");
    }

    @Test
    @DisplayName("Ein unbekannter Stapel nennt die gültigen Namen")
    void stapelTippfehler() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> DeckStapel.von("in-progress"));
        assertTrue(e.getMessage().contains("inprogress"), "die Meldung nennt den richtigen Namen");
        assertTrue(e.getMessage().contains("vorrat"), "und alle anderen auch");

        assertEquals(DeckStapel.INPROGRESS, DeckStapel.von("INPROGRESS"),
                "die Suche ist unabhängig von der Schreibweise");
        assertEquals(DeckStapel.VORRAT, DeckStapel.von("  vorrat  "));
    }

    @Test
    @DisplayName("Die fünf Stapel stehen in Anzeigereihenfolge")
    void stapelReihenfolge() {
        assertEquals("vorrat, backlog, todo, inprogress, done", DeckStapel.alleCodes());
    }
}
