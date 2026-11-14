/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.boot.dashboard.DashboardTile;
import org.springframework.stereotype.Component;

/**
 * Startseiten-Kachel «Deck» (Karte 2162). {@code menuTitle} muss den Menüpunkt aus {@link DeckMenu}
 * treffen — sonst ist die Kachel für jeden Mandanten sichtbar, auch wenn das Menü ausgeblendet ist.
 */
@Component
@DashboardTile(
        id = "deck",
        title = "Deck",
        icon = "pi pi-objects-column",
        link = "deck.html",
        menuTitle = "Deck",
        order = 56,
        roles = {"USER", "ADMIN", "ROOT"})
public class DeckDashboardTile {
}
