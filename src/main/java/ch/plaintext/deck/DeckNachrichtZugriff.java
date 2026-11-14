/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.PlaintextSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Der eine erlaubte Weg, einen Deck-Eintrag per Id zu holen (Karte 1533, AR1): nicht gelöscht und
 * im eigenen Mandanten — dieselbe Regel wie {@link DeckKarteZugriff}.
 *
 * <p>Die Adressprüfung («gehört der Eintrag diesem Agenten?») bleibt bewusst bei
 * {@link DeckService#lesen}: sie ist keine Sichtbarkeitsregel, sondern die Frage, ob der Aufrufer
 * der Empfänger ist. Zwei verschiedene Dinge in einer Klasse zu vermischen macht beide unklar.</p>
 */
@Component
@RequiredArgsConstructor
public class DeckNachrichtZugriff {

    private final DeckNachrichtRepository repo;
    private final PlaintextSecurity security;

    /** Einzelzugriff mit Mandanten- und Löschprüfung. */
    public Optional<DeckNachricht> find(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repo.findById(id)
                .filter(n -> !Boolean.TRUE.equals(n.getDeleted()))
                .filter(n -> n.getMandat() != null && n.getMandat().equalsIgnoreCase(mandat()));
    }

    /** Rückfall wie {@link DeckService#mandat()}: ein MCP-Aufruf kommt ohne Mandant. */
    private String mandat() {
        String m = security.getMandat();
        return m == null || m.isBlank() ? "default" : m;
    }
}
