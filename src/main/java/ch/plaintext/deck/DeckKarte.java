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
 * Eine Karte auf dem Board (Karte 2162, Soll 1).
 *
 * <p>Erbt von {@link SuperModel} — nicht aus Bequemlichkeit, sondern weil die Architektur das
 * verlangt ({@code SuperModelVertragTest}): {@code id}, {@code mandat}, {@code deleted} und die
 * Auditing-Felder kommen von dort, damit mandantenbezogene Abfragen und der Modulexport auch für
 * diese Tabelle funktionieren.</p>
 *
 * <p>Der Text ist bewusst {@code text} und nicht {@code @Lob}: letzteres verlangt in PostgreSQL
 * eine {@code oid}-Spalte und ist für einen Markdown-Text die falsche Wahl.</p>
 */
@Entity
@Table(name = "deck_karte")
@Getter
@Setter
public class DeckKarte extends SuperModel {

    /** Guter Titel, eine Zeile (Soll 1: „je Karte ein guter Titel"). */
    @Column(nullable = false, length = 300)
    private String titel;

    /** Beschreibung, Markdown wie im heutigen Deck. */
    @Column(columnDefinition = "text")
    private String beschreibung;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeckStapel stapel = DeckStapel.BACKLOG;

    /** Höher = wichtiger. 0 heisst „ohne Priorität"; das ist der Normalfall, nicht ein Fehler. */
    @Column(nullable = false)
    private int prio;

    /**
     * Kommagetrennte Labels, wie im heutigen Deck gepflegt ({@code dev}, {@code Zu überprüfen} …).
     *
     * <p>Keine eigene Tabelle: Labels sind bei uns Freitext mit Konvention, keine Stammdaten.
     * Eine n:m-Tabelle wäre der richtige Weg, sobald jemand Labels auswählbar machen will —
     * dann gehört eine Karte dazu.</p>
     */
    @Column(length = 300)
    private String labels;

    /**
     * Agent, der die Karte gerade hat ({@code null} = niemand). Der Name ist der Agentenname aus
     * {@code $CIE_AGENT} bzw. der Slot-Name, keine Benutzer-Id: die Agenten sind keine Konten.
     */
    @Column(length = 100)
    private String bearbeiter;

    /**
     * {@code true}, wenn die Karte auf eine Antwort wartet („Rückfrage" im Board).
     *
     * <p>Eigenes Feld statt eines Labels, weil daran die Ribbon-Farbe und der Filter im
     * Rückfrage-Panel hängen: ein Label kann jeder versehentlich umbenennen.</p>
     */
    @Column(nullable = false)
    private boolean rueckfrage;

    /**
     * {@code true}, wenn der Mensch hier etwas beitragen muss, ohne dass es eine Rückfrage ist
     * („Interaktiv" im Board).
     */
    @Column(nullable = false)
    private boolean interaktiv;

    /** Kurzfassung fürs Board und für die MCP-Kopfzeile; wird beim Speichern aus dem Text erzeugt. */
    @Column(length = 300)
    private String kurzfassung;
}
