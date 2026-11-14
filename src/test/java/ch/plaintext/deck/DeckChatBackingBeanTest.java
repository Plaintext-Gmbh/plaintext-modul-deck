/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Karte 2162, Soll 2: die Seiten-Bean des Chats.
 *
 * <p>Sie hat zwei Aufgaben, die leicht schiefgehen: die Grafik muss nach Häufigkeit sortiert sein
 * (sonst steht das wichtigste Paar irgendwo in der Mitte), und ein Klick auf «Senden» ohne
 * Empfänger darf keine Nachricht an {@code null} schreiben.</p>
 */
@DisplayName("Karte 2162: DeckChatBackingBean")
class DeckChatBackingBeanTest {

    private DeckService service;
    private DeckChatBackingBean bean;

    @BeforeEach
    void aufbau() {
        service = mock(DeckService.class);
        when(service.agenten()).thenReturn(List.of());
        when(service.nachrichten()).thenReturn(List.of());
        when(service.schreibMatrix()).thenReturn(Map.of());
        bean = new DeckChatBackingBean(service);
    }

    @Test
    @DisplayName("laden füllt Agenten, Verlauf und Grafik")
    void laden() {
        when(service.agenten()).thenReturn(List.of("chef", "dee1"));
        DeckNachricht n = new DeckNachricht();
        n.setVon("dee1");
        n.setAn("chef");
        n.setText("Hallo");
        when(service.nachrichten()).thenReturn(List.of(n));
        when(service.schreibMatrix()).thenReturn(Map.of("dee1", Map.of("chef", 1L)));

        bean.laden();

        assertEquals(List.of("chef", "dee1"), bean.getAgenten());
        assertEquals(1, bean.getVerlauf().size());
        assertEquals(1, bean.getGrafik().size());
        assertNull(bean.getFehler());
    }

    @Test
    @DisplayName("die Grafik steht mit dem fleissigsten Paar oben")
    void grafikSortiert() {
        // LinkedHashMap statt Map.of: die Reihenfolge der Gleichstände ist Teil der Erwartung.
        Map<String, Map<String, Long>> matrix = new LinkedHashMap<>();
        matrix.put("chef", new LinkedHashMap<>(Map.of("dee1", 2L)));
        matrix.put("dee1", new LinkedHashMap<>(Map.of("chef", 9L)));
        matrix.put("dee3", new LinkedHashMap<>(Map.of("chef", 5L)));
        when(service.schreibMatrix()).thenReturn(matrix);

        bean.laden();

        List<SchreibZeile> grafik = bean.getGrafik();
        assertEquals("dee1", grafik.get(0).von(), "9 ist das Maximum und gehört nach oben");
        assertEquals(100, grafik.get(0).breite());
        assertEquals("dee3", grafik.get(1).von());
        assertEquals("chef", grafik.get(2).von());
    }

    @Test
    @DisplayName("der erste Zugriff lädt nach, eine Sitzungs-Bean startet leer")
    void ersterZugriffLaedt() {
        assertTrue(bean.getAgenten().isEmpty());
        verify(service).agenten();
    }

    @Test
    @DisplayName("senden ohne Empfänger oder ohne Text schreibt nichts")
    void sendenOhneInhalt() {
        bean.setEmpfaenger("  ");
        bean.setText("Text");
        bean.senden();

        bean.setEmpfaenger("chef");
        bean.setText("  ");
        bean.senden();

        verify(service, never()).nachricht(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("senden schreibt unter dem angemeldeten Benutzer und leert das Feld")
    void senden() {
        when(service.benutzer()).thenReturn("daniel");
        bean.setEmpfaenger("  dee1  ");
        bean.setText("  Bitte prüfen.  ");

        bean.senden();

        verify(service).nachricht("daniel", "dee1", "Bitte prüfen.");
        assertNull(bean.getText());
    }

    @Test
    @DisplayName("ein Fehler beim Senden steht an der Bean")
    void sendenFehler() {
        when(service.benutzer()).thenReturn("daniel");
        when(service.nachricht(any(), any(), any())).thenThrow(new IllegalArgumentException("Empfänger fehlt"));
        bean.setEmpfaenger("dee1");
        bean.setText("Text");

        bean.senden();

        assertEquals("Empfänger fehlt", bean.getFehler());
        assertEquals("Text", bean.getText(), "der Text bleibt stehen, damit er nicht neu getippt werden muss");
    }

    @Test
    @DisplayName("die Auswahlliste zeigt den Benutzer selbst, solange niemand geschrieben hat")
    void auswahlOhneAgenten() {
        when(service.benutzer()).thenReturn("daniel");

        assertEquals(List.of("daniel"), bean.getAgentenAuswahl());
    }

    @Test
    @DisplayName("die Auswahlliste ist die Agentenliste, sobald es Agenten gibt")
    void auswahlMitAgenten() {
        when(service.agenten()).thenReturn(List.of("chef", "dee1"));

        assertEquals(List.of("chef", "dee1"), bean.getAgentenAuswahl());
    }
}
