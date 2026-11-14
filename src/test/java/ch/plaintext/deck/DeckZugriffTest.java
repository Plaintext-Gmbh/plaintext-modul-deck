/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.PlaintextSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Karte 1533 AR1 über Karte 2162: die Sichtbarkeitsregel beim Einzelzugriff.
 *
 * <p>Der Test fährt die drei Fälle, die eine Zugriffsklasse überhaupt rechtfertigen: fremdes Mandat
 * wird nicht gesehen, gelöscht wird nicht gesehen, und ein Aufruf ohne Anmeldung (MCP) landet im
 * Mandanten {@code default}, statt alles zu sehen.</p>
 */
@DisplayName("Karte 1533 AR1: Einzelzugriff der Deck-Daten")
class DeckZugriffTest {

    private DeckKarteRepository karten;
    private DeckNachrichtRepository nachrichten;
    private PlaintextSecurity security;

    @BeforeEach
    void aufbau() {
        karten = mock(DeckKarteRepository.class);
        nachrichten = mock(DeckNachrichtRepository.class);
        security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("test");
    }

    private static DeckKarte karte(long id, String mandat, boolean deleted) {
        DeckKarte k = new DeckKarte();
        k.setId(id);
        k.setMandat(mandat);
        k.setDeleted(deleted);
        return k;
    }

    private static DeckNachricht nachricht(long id, String mandat, boolean deleted) {
        DeckNachricht n = new DeckNachricht();
        n.setId(id);
        n.setMandat(mandat);
        n.setDeleted(deleted);
        return n;
    }

    @Test
    @DisplayName("eine Karte im eigenen Mandanten kommt zurück")
    void karteImMandanten() {
        when(karten.findById(1L)).thenReturn(Optional.of(karte(1L, "test", false)));
        assertEquals(1L, new DeckKarteZugriff(karten, security).find(1L).orElseThrow().getId());
    }

    @Test
    @DisplayName("eine fremde Karten-Id ist unsichtbar, nicht nur unauffällig")
    void fremdesMandatKarte() {
        when(karten.findById(2L)).thenReturn(Optional.of(karte(2L, "anderer", false)));
        assertTrue(new DeckKarteZugriff(karten, security).find(2L).isEmpty(),
                "die Id allein darf kein Schlüssel sein");
    }

    @Test
    @DisplayName("eine gelöschte Karte kommt auch im eigenen Mandanten nicht zurück")
    void geloeschteKarte() {
        when(karten.findById(3L)).thenReturn(Optional.of(karte(3L, "test", true)));
        assertTrue(new DeckKarteZugriff(karten, security).find(3L).isEmpty());
    }

    @Test
    @DisplayName("ohne Id wird gar nicht erst gesucht")
    void ohneIdKarte() {
        assertTrue(new DeckKarteZugriff(karten, security).find(null).isEmpty());
    }

    @Test
    @DisplayName("ein Eintrag ohne Anmeldung landet im Mandanten default, nicht mandantenfrei")
    void ohneAnmeldungDefault() {
        when(security.getMandat()).thenReturn(" ");
        when(nachrichten.findById(4L)).thenReturn(Optional.of(nachricht(4L, "default", false)));
        assertEquals(4L, new DeckNachrichtZugriff(nachrichten, security).find(4L).orElseThrow().getId());
    }

    @Test
    @DisplayName("ein fremder Eintrag bleibt unsichtbar, auch ohne Anmeldung")
    void fremdesMandatNachricht() {
        when(security.getMandat()).thenReturn(null);
        when(nachrichten.findById(5L)).thenReturn(Optional.of(nachricht(5L, "anderer", false)));
        assertTrue(new DeckNachrichtZugriff(nachrichten, security).find(5L).isEmpty());
    }

    @Test
    @DisplayName("ein gelöschter Eintrag ist weg")
    void geloeschteNachricht() {
        when(nachrichten.findById(6L)).thenReturn(Optional.of(nachricht(6L, "test", true)));
        assertTrue(new DeckNachrichtZugriff(nachrichten, security).find(6L).isEmpty());
    }
}
