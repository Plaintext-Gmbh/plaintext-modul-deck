/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.framework.SuperModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Ein Postfach-Eintrag für einen Agenten (Karte 2162, Soll 2 und Soll 3).
 *
 * <p>{@code von} und {@code an} sind Agentennamen, keine Benutzer-Ids — es gibt keine Konten für
 * Agenten, und der Name ist ohnehin das, was in {@code $CIE_AGENT} steht.</p>
 *
 * <p><b>Warum alle drei Arten in einer Tabelle stehen.</b> Soll 3 nennt drei Arten, die ein Agent
 * bekommen kann: eine direkte Nachricht, eine Kartenzuweisung und eine Reaktion auf eine seiner
 * Karten. Sie in drei Tabellen zu legen hiesse, «gibt es etwas für mich?» dreimal zu fragen und
 * dreimal zu zählen — und der Aufruf soll billig sein. Mit {@link Art} und {@code karteId} ist das
 * Postfach <em>eine</em> Abfrage über <em>eine</em> Tabelle.</p>
 *
 * <p>Der Chat aus Soll 2 ist dieselbe Tabelle ohne Filter. Die Grafik «wer schreibt wie viel an
 * wen» zählt die Zeilen je Paar — Zuweisungen eingeschlossen, denn auch die sind etwas, das ein
 * Agent einem anderen zuwendet.</p>
 *
 * <p>Der Text ist bei direkten Nachrichten der Inhalt. Bei Zuweisung und Reaktion ist er eine
 * kurze Zeile für die Anzeige; die vollständige Information steht dann in Beilage, Karte und
 * Kommentar und wird erst über {@code deck_lesen} geholt.</p>
 */
@Entity
@Table(name = "deck_nachricht")
@Getter
@Setter
public class DeckNachricht extends SuperModel {

    @Column(nullable = false, length = 100)
    private String von;

    @Column(nullable = false, length = 100)
    private String an;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    /** Was für einer Art der Eintrag ist — entscheidet, was der Agent beim Lesen bekommt. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Art art = Art.DIREKT;

    /** Karte, um die es geht; {@code null} bei einer direkten Nachricht. Kein Fremdschlüssel, siehe unten. */
    private Long karteId;

    /** Beim Abholen durch den Empfänger gesetzt (siehe {@link DeckKommentar#isGelesen()}). */
    @Column(nullable = false)
    private boolean gelesen;

    /**
     * Die drei Arten aus Soll 3.
     *
     * <p>Die Reihenfolge ist die Anzeigereihenfolge im Postfach: was den Agenten direkt angeht,
     * zuerst.</p>
     */
    public enum Art {
        /** Eine direkte Nachricht von einem anderen Agenten oder vom Menschen. */
        DIREKT,
        /** Jemand hat dem Agenten eine Karte zugewiesen. */
        ZUWEISUNG,
        /** Jemand hat auf eine Karte geschrieben, die der Agent hat. */
        REAKTION
    }
}
