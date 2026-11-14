/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.PlaintextSecurity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Der eine erlaubte Weg, eine Karte per Id zu holen (Karte 1533, AR1): nicht gelöscht und im
 * eigenen Mandanten. Service, Seiten-Bean und MCP-Werkzeuge gehen hier durch statt direkt über
 * {@code Repository.findById} — sonst steht die Sichtbarkeitsregel nur in der Listenabfrage, und
 * eine Karte liesse sich per geratener Id aus einem fremden Mandanten lesen.
 *
 * <p>Die Löschprüfung ist mehr als Kosmetik: eine gelöschte Karte darf auch dann nicht
 * zurückkommen, wenn ihre Id noch im Browser-Tab eines Benutzers steht.</p>
 */
@Component
@RequiredArgsConstructor
public class DeckKarteZugriff {

    private final DeckKarteRepository repo;
    private final PlaintextSecurity security;

    /** Einzelzugriff mit Mandanten- und Löschprüfung. */
    public Optional<DeckKarte> find(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repo.findById(id)
                .filter(k -> !Boolean.TRUE.equals(k.getDeleted()))
                .filter(k -> k.getMandat() != null && k.getMandat().equalsIgnoreCase(mandat()));
    }

    /**
     * Wie {@link DeckService#mandat()} ohne Anmeldung auf {@code default} fällt: ein MCP-Aufruf
     * trägt keinen Mandanten, und ohne diesen Rückfall fände ein Agent seine eigenen Karten nicht.
     */
    private String mandat() {
        String m = security.getMandat();
        return m == null || m.isBlank() ? "default" : m;
    }
}
