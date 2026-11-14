/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Karte 2162: die Seiten-Bean hält keinen eigenen Zustand, den die Seite nicht braucht — und sie
 * verschluckt keine Fehler, sonst steht Daniel vor einem Brett, das nichts tut.
 */
@DisplayName("Karte 2162: DeckBoardBackingBean")
class DeckBoardBackingBeanTest {

    private DeckService service;
    private DeckBoardBackingBean bean;

    @BeforeEach
    void aufbau() {
        service = mock(DeckService.class);
        when(service.beitraege(anyLong())).thenReturn(List.of());
        bean = new DeckBoardBackingBean(service);
    }

    @Test
    @DisplayName("laden füllt Brett und offene Rückfragen")
    void laden() {
        when(service.board()).thenReturn(java.util.Map.of(DeckStapel.VORRAT, List.of()));
        when(service.offeneRueckfragen()).thenReturn(List.of());

        bean.laden();

        assertNotNull(bean.getBoard());
        assertNotNull(bean.getOffeneRueckfragen());
        assertNull(bean.getFehler());
    }

    @Test
    @DisplayName("getBoard lädt nach, wenn die Bean noch nichts geholt hat")
    void getBoardLaedtNach() {
        when(service.board()).thenReturn(java.util.Map.of(DeckStapel.VORRAT, List.of()));
        when(service.offeneRueckfragen()).thenReturn(List.of());

        assertNotNull(bean.getBoard(), "eine Sitzungs-Bean darf beim ersten Zugriff selbst laden");
        verify(service).board();
    }

    @Test
    @DisplayName("anlegen ohne Titel tut nichts")
    void anlegenOhneTitel() {
        bean.setNeuerTitel("   ");
        bean.anlegen();
        verify(service, never()).anlegen(any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("anlegen legt im Vorrat an und leert das Eingabefeld")
    void anlegen() {
        bean.setNeuerTitel("  Neue Karte  ");
        bean.anlegen();

        verify(service).anlegen(eq("Neue Karte"), eq(null), eq(DeckStapel.VORRAT), eq(0), eq(null));
        assertNull(bean.getNeuerTitel(), "der Titel bleibt nicht im Feld stehen");
    }

    @Test
    @DisplayName("ein Fehler beim Anlegen steht an der Bean und wird nicht verschluckt")
    void anlegenFehler() {
        when(service.anlegen(any(), any(), any(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("Titel zu lang"));
        bean.setNeuerTitel("X");

        bean.anlegen();

        assertEquals("Titel zu lang", bean.getFehler());
        assertEquals("X", bean.getNeuerTitel(), "die Eingabe bleibt erhalten, damit sie nicht neu getippt werden muss");
    }

    @Test
    @DisplayName("öffnen holt die Beiträge der Karte")
    void oeffnen() {
        bean.oeffnen(12L);
        verify(service).beitraege(12L);
        assertEquals(12L, bean.getOffeneKarte());
    }

    @Test
    @DisplayName("öffnen ohne Karte liefert eine leere Liste, keine Ausnahme")
    void oeffnenOhneKarte() {
        bean.oeffnen(null);
        assertTrue(bean.getBeitraege().isEmpty());
        verify(service, never()).beitraege(anyLong());
    }

    @Test
    @DisplayName("antworten ohne offene Karte oder ohne Text tut nichts")
    void antwortenOhneInhalt() {
        bean.antworten();
        bean.oeffnen(1L);
        bean.setAntwort("  ");
        bean.antworten();
        verify(service, never()).beitrag(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("antworten schreibt eine Antwort unter dem angemeldeten Benutzer")
    void antworten() {
        when(service.benutzer()).thenReturn("daniel");
        bean.oeffnen(4L);
        bean.setAntwort("  Ja, so machen.  ");

        bean.antworten();

        verify(service).beitrag(4L, "daniel", "Ja, so machen.", DeckKommentar.Art.ANTWORT);
        assertNull(bean.getAntwort());
    }

    @Test
    @DisplayName("Labels fürs Board ohne Interaktiv und Rückfrage, die schon als Marke stehen")
    void labelsOhneMarken() {
        DeckKarte k = new DeckKarte();
        k.setLabels("dev, Interaktiv, Rückfrage, Blockiert");
        assertEquals(java.util.List.of("dev", "Blockiert"),
                new DeckBoardBackingBean(org.mockito.Mockito.mock(DeckService.class)).labelsOhneMarken(k));
    }
}
