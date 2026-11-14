/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.boot.menu.MenuAnnotation;
import org.springframework.stereotype.Component;

/**
 * Hauptmenüpunkt «Deck» (Karte 2162).
 *
 * <p>Die Rollen sind dieselben wie beim Board selbst — eine Kachel oder ein Menüpunkt, der weiter
 * reicht als seine Seite, führt nur zu einem Zugriffsfehler beim Klick
 * ({@code StartseitenKachelRechteVertragTest} prüft genau das).</p>
 *
 * <p><b>{@code link} ist Pflicht, nicht Schmuck:</b> der {@code PageAccessGuard} läuft in
 * {@code mode=STRICT} und lässt nur Seiten durch, auf die ein Menüpunkt, ein Alias oder die
 * Allowlist zeigt. Ohne {@code link} wird {@code deck.xhtml} für alle gesperrt — und zwar nicht
 * mit einer Fehlermeldung beim Bauen, sondern erst beim Klick (gemessen 14.11.2026: 302 auf
 * {@code access-denied.html}).</p>
 */
@Component
@MenuAnnotation(
        title = "Deck",
        link = "deck.html",
        order = 56,
        icon = "pi pi-objects-column",
        roles = {"USER", "ADMIN", "ROOT"})
public class DeckMenu {
}
