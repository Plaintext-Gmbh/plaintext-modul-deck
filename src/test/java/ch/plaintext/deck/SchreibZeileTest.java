/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Karte 2162, Soll 2: die Schreibgrafik — Breiten relativ zum Maximum aller Zellen.
 *
 * <p>Der Punkt der Tests ist die Skala: die Grafik soll «wer schreibt <em>wie viel</em>» zeigen, und
 * das tut sie nur, wenn alle Balken an derselben Zahl hängen. Eine Skala je Absender wäre die
 * hübschere, aber falsche Antwort — dann sähen 2 und 200 Nachrichten gleich breit aus.</p>
 */
@DisplayName("Karte 2162: Schreibgrafik")
class SchreibZeileTest {

    @Test
    @DisplayName("die Breite hängt am Maximum aller Zellen, nicht am Maximum je Absender")
    void breiteRelativZumGesamtmaximum() {
        // Map.of liefert keine feste Reihenfolge — für einen Test, der auf die Zeilenfolge schaut,
        // wird die Reihenfolge ausdrücklich gesetzt.
        Map<String, Long> dee1 = new LinkedHashMap<>();
        dee1.put("chef", 10L);
        dee1.put("dee2", 5L);
        Map<String, Map<String, Long>> matrix = new LinkedHashMap<>();
        matrix.put("dee1", dee1);
        matrix.put("chef", Map.of("dee1", 2L));

        List<SchreibZeile> zeilen = SchreibZeile.aus(matrix);

        assertEquals(3, zeilen.size());
        assertEquals(new SchreibZeile("dee1", "chef", 10L, 100), zeilen.get(0),
                "der Spitzenreiter füllt den Balken ganz");
        assertEquals(50, zeilen.get(1).breite(), "die Hälfte der Nachrichten, die Hälfte des Balkens");
        // 2 von 10 sind 20 Prozent — über der Untergrenze von 4, also der echte Wert.
        assertEquals(20, zeilen.get(2).breite(), "die Skala ist für alle dieselbe");
    }

    @Test
    @DisplayName("wenige Nachrichten bekommen einen Mindestbalken, damit „gezählt“ nicht wie „nichts“ aussieht")
    void mindestbreite() {
        Map<String, Map<String, Long>> matrix = new LinkedHashMap<>();
        matrix.put("chef", new LinkedHashMap<>(Map.of("dee1", 500L)));
        matrix.put("dee9", new LinkedHashMap<>(Map.of("chef", 1L)));

        List<SchreibZeile> zeilen = SchreibZeile.aus(matrix);

        assertEquals(4, zeilen.get(1).breite(),
                "1 von 500 wären 0 Prozent — der Balken bliebe unsichtbar");
        assertEquals(1L, zeilen.get(1).anzahl(), "die Zahl selbst bleibt unverfälscht");
    }

    @Test
    @DisplayName("eine leere Matrix ergibt keine Zeilen, keine Ausnahme")
    void leereMatrix() {
        assertTrue(SchreibZeile.aus(Map.of()).isEmpty());
    }

    @Test
    @DisplayName("lauter Nullen ergeben Breite 0 statt einer Division durch null")
    void nurNullen() {
        Map<String, Map<String, Long>> matrix = Map.of("dee1", Map.of("chef", 0L));

        List<SchreibZeile> zeilen = SchreibZeile.aus(matrix);

        assertEquals(1, zeilen.size());
        assertEquals(0, zeilen.get(0).breite(),
                "ohne Spitzenreiter gibt es keinen Massstab — 0 ist ehrlicher als 100");
    }

    @Test
    @DisplayName("ein fehlender Zähler zählt als 0, nicht als Ausnahme")
    void nullWert() {
        Map<String, Long> spalte = new LinkedHashMap<>();
        spalte.put("chef", null);

        List<SchreibZeile> zeilen = SchreibZeile.aus(Map.of("dee1", spalte));

        assertEquals(0L, zeilen.get(0).anzahl());
        assertEquals(0, zeilen.get(0).breite());
    }

    @Test
    @DisplayName("die Stufe rundet auf Fünferschritte für die CSS-Klasse")
    void stufe() {
        assertEquals(100, new SchreibZeile("a", "b", 1L, 100).stufe());
        assertEquals(20, new SchreibZeile("a", "b", 1L, 22).stufe());
        assertEquals(5, new SchreibZeile("a", "b", 1L, 4).stufe(), "der Mindestbalken bleibt sichtbar");
        assertEquals(0, new SchreibZeile("a", "b", 0L, 0).stufe());
    }
}
