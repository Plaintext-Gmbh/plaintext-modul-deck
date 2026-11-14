/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Zugriff auf die Karten (Karte 2162). */
public interface DeckKarteRepository extends JpaRepository<DeckKarte, Long> {

    /** Alle nicht gelöschten Karten eines Mandats, Board-Reihenfolge. */
    List<DeckKarte> findByMandatAndDeletedIsFalseOrderByPrioDescIdAsc(String mandat);

    /** Karten eines Stapels, oberste Priorität zuerst. */
    List<DeckKarte> findByMandatAndStapelAndDeletedIsFalseOrderByPrioDescIdAsc(String mandat, DeckStapel stapel);

    /** Karten, die auf eine Antwort warten — Grundlage des Rückfrage-Panels. */
    List<DeckKarte> findByMandatAndRueckfrageIsTrueAndDeletedIsFalseOrderByIdAsc(String mandat);
}
