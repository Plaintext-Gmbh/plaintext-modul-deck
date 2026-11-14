/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Zugriff auf Kommentare, Antworten und Nachrichten (Karte 2162). */
public interface DeckKommentarRepository extends JpaRepository<DeckKommentar, Long> {

    /** Alle Beiträge einer Karte, chronologisch (Rückfrage-Panel). */
    List<DeckKommentar> findByKarteIdAndDeletedIsFalseOrderByIdAsc(Long karteId);

    /** Ungelesene Beiträge eines Agenten über alle seine Karten — Grundlage von «etwas für mich?». */
    List<DeckKommentar> findByKarteBearbeiterAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc(String agent);

    /** Ungelesene Antworten auf eine bestimmte Karte. */
    List<DeckKommentar> findByKarteIdAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc(Long karteId);
}
