/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.boot.menu.MenuAnnotation;
import org.springframework.stereotype.Component;

/**
 * Menüpunkt «Deck – Chat» (Karte 2162, Soll 2).
 *
 * <p>Eigene Seite statt Abschnitt unter dem Brett: der Chat und die Schreibgrafik sind eine eigene
 * Frage («wer redet mit wem»), und das Brett ist mit fünf Stapeln und dem Rückfrage-Panel schon
 * voll. Der Menüpunkt hängt als Unterpunkt unter «Deck».</p>
 *
 * <p>{@code link} ist auch hier Pflicht — ohne ihn sperrt der {@code PageAccessGuard} im Modus
 * {@code STRICT} die Seite für alle (siehe {@link DeckMenu}).</p>
 */
@Component
@MenuAnnotation(
        title = "Deck – Chat",
        link = "deckchat.html",
        parent = "Deck",
        order = 1,
        icon = "pi pi-comments",
        roles = {"USER", "ADMIN", "ROOT"})
public class DeckChatMenu {
}
