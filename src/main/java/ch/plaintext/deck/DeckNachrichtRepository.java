/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Zugriff auf die Postfach-Einträge und Agenten-Nachrichten (Karte 2162, Soll 2 und Soll 3). */
public interface DeckNachrichtRepository extends JpaRepository<DeckNachricht, Long> {

    /** Alle Nachrichten eines Mandats, chronologisch — Grundlage der Chat-Ansicht. */
    List<DeckNachricht> findByMandatAndDeletedIsFalseOrderByIdAsc(String mandat);

    /** Postfach eines Agenten: ungelesen, chronologisch. */
    List<DeckNachricht> findByMandatAndAnAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc(String mandat, String an);

    /** Postfach eines Agenten, alles — Grundlage der Zählung in den Metriken. */
    List<DeckNachricht> findByMandatAndAnAndDeletedIsFalseOrderByIdAsc(String mandat, String an);

    /** Alle Einträge zu einer Karte, z.B. um die Vorgeschichte einer Zuweisung zu zeigen. */
    List<DeckNachricht> findByMandatAndKarteIdAndDeletedIsFalseOrderByIdAsc(String mandat, Long karteId);
}
