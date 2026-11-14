/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import java.util.List;
import java.util.Map;

/**
 * Die Schreibmatrix als Zeilen, die eine Seite zeichnen kann (Karte 2162, Soll 2: «Grafik, wer
 * schreibt wie viel an wen»).
 *
 * <p>Die Grafik ist bewusst reines HTML und CSS, kein Chart-Framework: die Zahlen sind
 * Ganzzahlen von 0 bis vielleicht 50, und ein Balken in einer Tabellenzelle sagt dasselbe wie eine
 * Bibliothek — mit einem Bruchteil an Abhängigkeiten und ohne Fremd-Host in der
 * Content-Security-Policy.</p>
 *
 * @param von    Absender
 * @param an     Empfänger
 * @param anzahl geschriebene Einträge
 * @param breite Balkenbreite in Prozent, bezogen auf den fleissigsten Absender
 */
public record SchreibZeile(String von, String an, long anzahl, int breite) {

    /**
     * Die Breite in Fünferschritten — der Name der CSS-Klasse, die die Seite setzt.
     *
     * <p>Die Seite schreibt die Breite <b>nicht</b> als {@code style="width: …%"}: Inline-Styles
     * sind im Projekt die Ausnahme, die begründet werden muss (Massnahme 13). Einundzwanzig
     * Klassen in Fünferschritten zeichnen denselben Balken und bleiben im Stylesheet. Fünf Prozent
     * Auflösung ist bei einem Diagramm, das Verhältnisse zeigt, genug — der genaue Wert steht als
     * Zahl daneben.</p>
     */
    public int stufe() {
        return (int) (Math.round(breite / 5.0) * 5);
    }

    /**
     * Baut die Zeilen aus der Matrix des Dienstes.
     *
     * <p>Die Breite ist relativ zum Maximum <b>aller</b> Zellen, nicht je Absender: sonst sähen
     * ein Agent mit zwei Nachrichten und einer mit zweihundert gleich breit aus, und die Grafik
     * beantwortete die Frage der Karte nicht mehr.</p>
     */
    static List<SchreibZeile> aus(Map<String, Map<String, Long>> matrix) {
        long maximum = 0;
        for (Map<String, Long> zeile : matrix.values()) {
            for (Long anzahl : zeile.values()) {
                maximum = Math.max(maximum, anzahl == null ? 0 : anzahl);
            }
        }
        List<SchreibZeile> zeilen = new java.util.ArrayList<>();
        for (Map.Entry<String, Map<String, Long>> von : matrix.entrySet()) {
            for (Map.Entry<String, Long> an : von.getValue().entrySet()) {
                long anzahl = an.getValue() == null ? 0 : an.getValue();
                // Mindestens 4 Prozent: ein Balken von 0 Pixeln sieht aus wie "nichts", obwohl
                // gezählt wurde. Wer 20-mal weniger schreibt als der Spitzenreiter, soll trotzdem
                // sehen, dass er geschrieben hat.
                int breite = maximum == 0 ? 0 : (int) Math.max(4, Math.round(anzahl * 100.0 / maximum));
                zeilen.add(new SchreibZeile(von.getKey(), an.getKey(), anzahl, breite));
            }
        }
        return zeilen;
    }
}
