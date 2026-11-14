/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.deck.mcp.DeckMcpTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Karte 2162, Soll 3: die MCP-Werkzeuge aus Sicht eines Agenten.
 *
 * <p>Der Punkt dieser Tests ist nicht die Formatierung, sondern die Zusage der Karte: <b>«gibt es
 * etwas für mich?» kostet eine Zeile und keinen Inhalt.</b> Ein Test, der nur die Ausgabe
 * vergleicht, würde nicht merken, wenn der Text wieder mitkäme — darum prüfen die Tests hier
 * ausdrücklich, dass der Inhalt <em>nicht</em> in der Antwort steht.</p>
 */
@DisplayName("Karte 2162: MCP-Werkzeuge")
class DeckMcpToolsTest {

    private DeckService service;
    private DeckMcpTools tools;

    @BeforeEach
    void aufbau() {
        service = mock(DeckService.class);
        tools = new DeckMcpTools(service);
    }

    @Test
    @DisplayName("Postfach leer: eine Nein-Zeile, kein Listenkopf")
    void postfachLeer() {
        when(service.postfachKopf("dee1")).thenReturn(List.of());

        String antwort = tools.postfach("dee1");

        assertTrue(antwort.startsWith("Nein"), "ohne Einträge sagt die Antwort klar nein: " + antwort);
        assertTrue(antwort.contains("dee1"));
    }

    @Test
    @DisplayName("Postfach mit Einträgen: Id, Absender und Art, aber kein Inhalt")
    void postfachOhneInhalt() {
        when(service.postfachKopf("dee1")).thenReturn(List.of(
                new PostfachKopf(7L, "chef", "ZUWEISUNG", 2162L),
                new PostfachKopf(8L, "devops", "DIREKT", null)));

        String antwort = tools.postfach("dee1");

        assertTrue(antwort.contains("Ja, 2 Einträge"));
        assertTrue(antwort.contains("id=7"));
        assertTrue(antwort.contains("von=chef"));
        assertTrue(antwort.contains("art=ZUWEISUNG"));
        assertTrue(antwort.contains("karte=2162"), "bei einer Zuweisung nennt die Kopfzeile die Karte");
        assertTrue(antwort.contains("id=8"));
        assertFalse(antwort.contains("karte=") && antwort.contains("id=8 karte="),
                "eine direkte Nachricht hat keine Karte");
    }

    @Test
    @DisplayName("Postfach kostet keine Inhalte: der Dienst liefert Kopfzeilen, nicht die Zeilen")
    void postfachFragtKopfzeilen() {
        when(service.postfachKopf("dee1")).thenReturn(List.of());

        tools.postfach("dee1");

        verify(service).postfachKopf("dee1");
        verify(service, never()).postfach(anyString());
        verify(service, never()).nachrichten();
    }

    @Test
    @DisplayName("lesen gibt den Inhalt und hängt die Karte an")
    void lesen() {
        DeckNachricht n = new DeckNachricht();
        n.setId(7L);
        n.setVon("chef");
        n.setAn("dee1");
        n.setArt(DeckNachricht.Art.ZUWEISUNG);
        n.setKarteId(5L);
        n.setText("Karte 5: Deck-Modul bauen");
        when(service.lesen(7L, "dee1")).thenReturn(n);

        DeckKarte k = new DeckKarte();
        k.setId(5L);
        k.setTitel("Deck-Modul bauen");
        k.setStapel(DeckStapel.TODO);
        when(service.karte(5L)).thenReturn(Optional.of(k));
        when(service.beitraege(5L)).thenReturn(List.of());

        String antwort = tools.lesen(7L, "dee1");

        assertTrue(antwort.contains("Karte 5: Deck-Modul bauen"));
        assertTrue(antwort.contains("ZUWEISUNG"));
        assertTrue(antwort.contains("Deck-Modul bauen"), "die Karte wird mitgegeben");
    }

    @Test
    @DisplayName("lesen einer fremden Id meldet den Fehler, statt still zu liefern")
    void lesenFremd() {
        when(service.lesen(7L, "dee1")).thenThrow(new IllegalArgumentException("Eintrag 7 gehört nicht zu dee1."));

        String antwort = tools.lesen(7L, "dee1");

        assertTrue(antwort.startsWith("FEHLER:"));
    }

    @Test
    @DisplayName("Kartenliste nach Stapel, mit Marken, ohne Beschreibung")
    void karten() {
        DeckKarte k = new DeckKarte();
        k.setId(9L);
        k.setTitel("Gerüst");
        k.setStapel(DeckStapel.INPROGRESS);
        k.setBearbeiter("dee1");
        k.setLabels("dev, Zu überprüfen");
        k.setInteraktiv(true);
        k.setRueckfrage(true);
        when(service.kartenImStapel(DeckStapel.INPROGRESS)).thenReturn(List.of(k));

        String antwort = tools.karten("inprogress");

        assertTrue(antwort.contains("1 Karte"));
        assertTrue(antwort.contains("id=9 [inprogress] Gerüst"));
        assertTrue(antwort.contains("dev, Zu überprüfen"));
        assertTrue(antwort.contains("@dee1"));
        assertTrue(antwort.contains("[Interaktiv]"));
        assertTrue(antwort.contains("[Rückfrage]"));
    }

    @Test
    @DisplayName("Kartenliste ohne Stapel zeigt alle, mit Tippfehler nennt sie die gültigen")
    void kartenOhneStapel() {
        when(service.alleKarten()).thenReturn(List.of());
        assertTrue(tools.karten(null).startsWith("Keine Karten"));

        when(service.kartenImStapel(any())).thenThrow(new IllegalArgumentException("Unbekannter Stapel"));
        assertTrue(tools.karten("in-progress").startsWith("FEHLER:"));
    }

    @Test
    @DisplayName("eine Karte vollständig: Beschreibung und Beiträge chronologisch")
    void karte() {
        DeckKarte k = new DeckKarte();
        k.setId(3L);
        k.setTitel("Board");
        k.setBeschreibung("Zeile eins");
        k.setStapel(DeckStapel.TODO);
        k.setPrio(2);
        when(service.karte(3L)).thenReturn(Optional.of(k));

        DeckKommentar b = new DeckKommentar();
        b.setArt(DeckKommentar.Art.RUECKFRAGE);
        b.setAutor("dee1");
        b.setText("Welche Farbe?");
        when(service.beitraege(3L)).thenReturn(List.of(b));

        String antwort = tools.karte(3L);

        assertTrue(antwort.contains("Board"));
        assertTrue(antwort.contains("stapel=todo"));
        assertTrue(antwort.contains("Zeile eins"));
        assertTrue(antwort.contains("[RUECKFRAGE] dee1: Welche Farbe?"));
    }

    @Test
    @DisplayName("eine unbekannte Karte wird gemeldet")
    void karteUnbekannt() {
        when(service.karte(404L)).thenReturn(Optional.empty());
        assertTrue(tools.karte(404L).startsWith("FEHLER:"));
    }

    @Test
    @DisplayName("Kommentar ohne Art ist ein Kommentar, mit Art wird sie übernommen")
    void kommentar() {
        DeckKommentar c = new DeckKommentar();
        c.setId(11L);
        c.setArt(DeckKommentar.Art.KOMMENTAR);
        when(service.beitrag(eq(3L), eq("dee1"), anyString(), any())).thenReturn(c);

        assertTrue(tools.kommentar(3L, "dee1", "Text", null).startsWith("OK: Beitrag 11"));
        verify(service).beitrag(3L, "dee1", "Text", DeckKommentar.Art.KOMMENTAR);

        tools.kommentar(3L, "dee1", "Frage?", "rueckfrage");
        verify(service).beitrag(3L, "dee1", "Frage?", DeckKommentar.Art.RUECKFRAGE);

        assertTrue(tools.kommentar(3L, "dee1", "x", "unsinn").startsWith("FEHLER:"),
                "eine unbekannte Art wird gemeldet, nicht still zum Kommentar gemacht");
    }

    @Test
    @DisplayName("Verschieben nennt den neuen Stapel")
    void verschieben() {
        DeckKarte k = new DeckKarte();
        k.setId(3L);
        k.setStapel(DeckStapel.DONE);
        k.setBearbeiter("dee1");
        when(service.verschieben(3L, DeckStapel.DONE, "dee1")).thenReturn(k);

        String antwort = tools.verschieben(3L, "done", "dee1");

        assertTrue(antwort.startsWith("OK: Karte 3 ist jetzt in done"));
        assertTrue(antwort.contains("bei dee1"));
    }

    @Test
    @DisplayName("Zuweisen landet beim Agenten, ein leerer Name wird gemeldet")
    void zuweisen() {
        DeckKarte k = new DeckKarte();
        k.setId(3L);
        k.setBearbeiter("dee2");
        when(service.zuweisen(3L, "dee2", "chef")).thenReturn(k);
        when(service.zuweisen(3L, "", "chef"))
                .thenThrow(new IllegalArgumentException("Ohne Agentennamen lässt sich keine Karte zuweisen."));

        assertTrue(tools.zuweisen(3L, "dee2", "chef").startsWith("OK: Karte 3 gehört jetzt dee2"));
        assertTrue(tools.zuweisen(3L, "", "chef").startsWith("FEHLER:"));
    }

    @Test
    @DisplayName("Nachricht an einen anderen Agenten")
    void nachricht() {
        DeckNachricht n = new DeckNachricht();
        n.setId(5L);
        when(service.nachricht("dee1", "chef", "Hallo")).thenReturn(n);

        assertTrue(tools.nachricht("dee1", "chef", "Hallo").startsWith("OK: Nachricht 5 an chef"));
    }

    @Test
    @DisplayName("Metriken nennen die Stapel, die Rückfragen und die Schreibmatrix")
    void metriken() {
        when(service.kartenImStapel(any())).thenReturn(List.of());
        when(service.offeneRueckfragen()).thenReturn(List.of());
        when(service.nachrichten()).thenReturn(List.of());
        when(service.schreibMatrix()).thenReturn(Map.of());

        String antwort = tools.metriken(null);

        assertTrue(antwort.contains("vorrat: 0"));
        assertTrue(antwort.contains("done: 0"));
        assertTrue(antwort.contains("offene-rueckfragen: 0"));
        assertTrue(antwort.contains("Schreibmatrix: leer"));
    }

    @Test
    @DisplayName("Metriken mit Agent filtern die Matrix auf seine Zeilen und nennen sein Postfach")
    void metrikenMitAgent() {
        when(service.kartenImStapel(any())).thenReturn(List.of());
        when(service.offeneRueckfragen()).thenReturn(List.of());
        when(service.nachrichten()).thenReturn(List.of());
        when(service.postfachAnzahl("dee1")).thenReturn(3);
        when(service.schreibMatrix()).thenReturn(Map.of(
                "dee1", Map.of("chef", 4L),
                "dee2", Map.of("chef", 1L)));

        String antwort = tools.metriken("dee1");

        assertTrue(antwort.contains("ungelesen-fuer-dee1: 3"));
        assertTrue(antwort.contains("dee1 -> chef: 4"));
        assertFalse(antwort.contains("dee2 -> chef"), "fremde Zeilen bleiben weg");
    }

    @Test
    @DisplayName("eine Reaktion auf eine gelöschte Karte sagt das, statt zu brechen")
    void lesenMitGeloeschterKarte() {
        DeckNachricht n = new DeckNachricht();
        n.setId(7L);
        n.setVon("chef");
        n.setArt(DeckNachricht.Art.REAKTION);
        n.setKarteId(99L);
        n.setText("Karte 99: alt");
        when(service.lesen(7L, "dee1")).thenReturn(n);
        when(service.karte(99L)).thenReturn(Optional.empty());

        String antwort = tools.lesen(7L, "dee1");

        assertTrue(antwort.contains("gibt es nicht mehr"));
        verify(service, never()).beitraege(anyLong());
    }
}
