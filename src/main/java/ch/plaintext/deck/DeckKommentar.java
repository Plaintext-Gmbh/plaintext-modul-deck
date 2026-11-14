/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.framework.SuperModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Ein Beitrag zu einer Karte (Karte 2162, Soll 1: Kommentare; Soll 1 Rückfrage-Panel).
 *
 * <p>Kommentar und Antwort stehen in derselben Tabelle, unterschieden durch {@link Art}. Warum
 * nicht zwei Tabellen: im Panel stehen sie chronologisch gemischt, und „wer hat zuletzt
 * geschrieben" ist eine Frage über beide. Der Unterschied ist die Richtung, nicht der Ort.</p>
 *
 * <p>{@code gelesen} ist die Markierung für das MCP-Polling: der Agent bekommt beim Abholen nur
 * ungelesene Beiträge als Kopfzeile, und das Lesen setzt die Markierung. Ohne dieses Feld müsste
 * der Agent den Zeitstempel der letzten Abholung mitschleppen — das geht schief, sobald er auf
 * zwei Rechnern läuft.</p>
 */
@Entity
@Table(name = "deck_kommentar")
@Getter
@Setter
public class DeckKommentar extends SuperModel {

    /** Wer den Beitrag geschrieben hat: Agentenname oder {@code daniel}. Keine Benutzer-Id. */
    @Column(nullable = false, length = 100)
    private String autor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Art art = Art.KOMMENTAR;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    /** Der Agent hat den Beitrag per {@code deck_lesen} geholt. */
    @Column(nullable = false)
    private boolean gelesen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "karte_id", nullable = false)
    private DeckKarte karte;

    /** Richtung des Beitrags. */
    public enum Art {
        /** Frage eines Agenten an den Menschen; beantwortet im Rückfrage-Panel. */
        RUECKFRAGE,
        /** Antwort des Menschen auf eine Rückfrage. */
        ANTWORT,
        /** Normaler Kommentar (Fortschritt, Beleg, Notiz). */
        KOMMENTAR
    }
}
