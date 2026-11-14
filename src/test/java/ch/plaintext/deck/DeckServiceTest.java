/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.PlaintextSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Karte 2162: das Verhalten des Boards, gemessen am {@link DeckService}.
 *
 * <p>Die Repositories sind Mocks — geprüft wird die Fachlogik, nicht JPA. Was hier festgehalten
 * wird, sind die drei Regeln, die ein Agent sonst erst im Betrieb merkt: eine Rückfrage markiert
 * die Karte, eine Antwort nimmt die Markierung zurück, und das Brett zeigt jeden Stapel auch dann,
 * wenn er leer ist.</p>
 */
@DisplayName("Karte 2162: DeckService")
class DeckServiceTest {

    private DeckKarteRepository karten;
    private DeckKommentarRepository kommentare;
    private DeckNachrichtRepository nachrichten;
    private PlaintextSecurity security;
    private DeckService service;

    private final List<DeckKarte> gespeicherteKarten = new ArrayList<>();
    private final List<DeckNachricht> gespeicherteNachrichten = new ArrayList<>();

    @BeforeEach
    void aufbau() {
        karten = mock(DeckKarteRepository.class);
        kommentare = mock(DeckKommentarRepository.class);
        nachrichten = mock(DeckNachrichtRepository.class);
        security = mock(PlaintextSecurity.class);
        when(security.getMandat()).thenReturn("test");
        when(security.getUser()).thenReturn("daniel");

        when(karten.save(any(DeckKarte.class))).thenAnswer(inv -> {
            DeckKarte k = inv.getArgument(0);
            if (!gespeicherteKarten.contains(k)) {
                gespeicherteKarten.add(k);
            }
            return k;
        });
        when(nachrichten.save(any(DeckNachricht.class))).thenAnswer(inv -> {
            DeckNachricht n = inv.getArgument(0);
            gespeicherteNachrichten.add(n);
            return n;
        });
        when(kommentare.save(any(DeckKommentar.class))).thenAnswer(inv -> inv.getArgument(0));

        service = new DeckService(karten, kommentare, nachrichten,
                new DeckKarteZugriff(karten, security), new DeckNachrichtZugriff(nachrichten, security), security);
    }

    @Test
    @DisplayName("das Brett zeigt alle fünf Stapel, auch die leeren, und ordnet die Karten zu")
    void brettZeigtAlleStapel() {
        DeckKarte imBacklog = karte();
        imBacklog.setTitel("B");
        imBacklog.setStapel(DeckStapel.BACKLOG);
        DeckKarte inArbeit = karte();
        inArbeit.setTitel("A");
        inArbeit.setStapel(DeckStapel.INPROGRESS);
        when(karten.findByMandatAndDeletedIsFalseOrderByPrioDescIdAsc("test"))
                .thenReturn(List.of(imBacklog, inArbeit));

        Map<DeckStapel, List<DeckKarte>> brett = service.board();

        assertEquals(5, brett.size(), "jeder Stapel steht da, auch der leere");
        assertEquals(List.of(imBacklog), brett.get(DeckStapel.BACKLOG));
        assertEquals(List.of(inArbeit), brett.get(DeckStapel.INPROGRESS));
        assertTrue(brett.get(DeckStapel.DONE).isEmpty());
        assertEquals(DeckStapel.VORRAT, brett.keySet().iterator().next(),
                "die Anzeigereihenfolge beginnt beim Vorrat");
    }

    @Test
    @DisplayName("eine neue Karte bekommt Mandat, Kurzfassung und den Vorgabestapel")
    void neueKarte() {
        DeckKarte k = service.anlegen("Titel", "Erste Zeile\nzweite", null, 3, "dev");

        assertEquals("Titel", k.getTitel());
        assertEquals(DeckStapel.BACKLOG, k.getStapel(), "ohne Angabe landet die Karte im Backlog");
        assertEquals(3, k.getPrio());
        assertEquals("test", k.getMandat());
        assertEquals("Erste Zeile", k.getKurzfassung());
        verify(karten).save(k);
    }

    @Test
    @DisplayName("Mandat und Benutzer fallen lesbar zurück, wenn die Sitzung nichts liefert")
    void mandatUndBenutzerOhneSitzung() {
        when(security.getMandat()).thenReturn(null);
        when(security.getUser()).thenReturn("  ");
        assertEquals("default", service.mandat());
        assertEquals("unbekannt", service.benutzer());
    }

    @Test
    @DisplayName("Verschieben setzt den Stapel und den Bearbeiter")
    void verschieben() {
        DeckKarte k = karte();
        k.setStapel(DeckStapel.TODO);
        when(karten.findById(7L)).thenReturn(Optional.of(k));

        service.verschieben(7L, DeckStapel.INPROGRESS, "dee1");

        assertEquals(DeckStapel.INPROGRESS, k.getStapel());
        assertEquals("dee1", k.getBearbeiter());
    }

    @Test
    @DisplayName("Verschieben ohne Bearbeiter lässt den bisherigen stehen")
    void verschiebenOhneBearbeiter() {
        DeckKarte k = karte();
        k.setBearbeiter("dee1");
        when(karten.findById(7L)).thenReturn(Optional.of(k));

        service.verschieben(7L, DeckStapel.DONE, " ");

        assertEquals("dee1", k.getBearbeiter(), "ein leerer Name heisst nicht 'niemand'");
    }

    @Test
    @DisplayName("eine unbekannte Karte wird beim Verschieben gemeldet, nicht still verschluckt")
    void verschiebenUnbekannt() {
        when(karten.findById(99L)).thenReturn(Optional.empty());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.verschieben(99L, DeckStapel.DONE, "dee1"));
        assertTrue(e.getMessage().contains("99"));
    }

    @Test
    @DisplayName("eine gelöschte Karte zählt nicht als vorhanden")
    void geloeschteKarte() {
        DeckKarte k = karte();
        k.setDeleted(true);
        when(karten.findById(1L)).thenReturn(Optional.of(k));
        assertTrue(service.karte(1L).isEmpty());
    }

    @Test
    @DisplayName("Rückfrage markiert die Karte, Antwort nimmt die Markierung zurück")
    void rueckfrageUndAntwort() {
        DeckKarte k = karte();
        k.setTitel("K");
        when(karten.findById(5L)).thenReturn(Optional.of(k));

        DeckKommentar frage = service.beitrag(5L, "dee1", "Soll ich?", DeckKommentar.Art.RUECKFRAGE);
        assertTrue(k.isRueckfrage(), "eine Rückfrage setzt die Markierung am Brett");
        assertEquals(DeckKommentar.Art.RUECKFRAGE, frage.getArt());
        assertEquals("test", frage.getMandat());

        DeckKommentar antwort = service.beitrag(5L, "daniel", "Ja.", DeckKommentar.Art.ANTWORT);
        assertFalse(k.isRueckfrage(), "die Antwort nimmt die Markierung zurück");
        assertEquals(DeckKommentar.Art.ANTWORT, antwort.getArt());
    }

    @Test
    @DisplayName("ein normaler Kommentar ändert die Rückfrage-Markierung nicht")
    void kommentarLaesstMarkierung() {
        DeckKarte k = karte();
        k.setRueckfrage(true);
        when(karten.findById(5L)).thenReturn(Optional.of(k));

        service.beitrag(5L, "dee1", "Fortschritt", null);

        assertTrue(k.isRueckfrage());
        verify(karten, never()).save(k);
    }

    @Test
    @DisplayName("ein Beitrag ohne Art ist ein Kommentar")
    void beitragOhneArt() {
        DeckKarte k = karte();
        when(karten.findById(5L)).thenReturn(Optional.of(k));
        DeckKommentar c = service.beitrag(5L, "dee1", "Text", null);
        assertEquals(DeckKommentar.Art.KOMMENTAR, c.getArt());
    }

    @Test
    @DisplayName("Label und Bearbeiter lassen sich setzen und wieder entfernen")
    void labelUndBearbeiter() {
        DeckKarte k = karte();
        when(karten.findById(3L)).thenReturn(Optional.of(k));

        service.label(3L, "dev", true);
        assertEquals("dev", k.getLabels());
        service.label(3L, "Zu überprüfen", true);
        assertEquals("dev, Zu überprüfen", k.getLabels());
        service.label(3L, "dev", false);
        assertEquals("Zu überprüfen", k.getLabels());

        service.bearbeiterSetzen(3L, "dee1");
        assertEquals("dee1", k.getBearbeiter());
        service.bearbeiterSetzen(3L, "");
        assertNull(k.getBearbeiter(), "leer heisst hier wirklich niemand");
    }

    @Test
    @DisplayName("die Schreibmatrix zählt je Paar und die Agentenliste ist alphabetisch")
    void schreibMatrix() {
        when(nachrichten.findByMandatAndDeletedIsFalseOrderByIdAsc("test"))
                .thenReturn(List.of(nachricht("dee1", "chef"), nachricht("dee1", "chef"),
                        nachricht("chef", "dee1"), nachricht("dee3", "chef")));

        Map<String, Map<String, Long>> matrix = service.schreibMatrix();

        assertEquals(2L, matrix.get("dee1").get("chef"));
        assertEquals(1L, matrix.get("chef").get("dee1"));
        assertEquals(1L, matrix.get("dee3").get("chef"));
        assertEquals(List.of("chef", "dee1", "dee3"), service.agenten());
    }

    @Test
    @DisplayName("eine Nachricht bekommt Absender, Empfänger und Mandat")
    void nachricht() {
        DeckNachricht n = service.nachricht("dee1", "chef", "Karte 2162: Gerüst steht.");
        assertEquals("dee1", n.getVon());
        assertEquals("chef", n.getAn());
        assertEquals("test", n.getMandat());
        assertNotNull(n.getText());
    }

    @Test
    @DisplayName("offene Rückfragen und Beiträge gehen gefiltert ans Repository")
    void abfragen() {
        when(karten.findByMandatAndRueckfrageIsTrueAndDeletedIsFalseOrderByIdAsc("test"))
                .thenReturn(List.of(karte()));
        assertEquals(1, service.offeneRueckfragen().size());

        when(kommentare.findByKarteIdAndDeletedIsFalseOrderByIdAsc(4L)).thenReturn(List.of());
        assertTrue(service.beitraege(4L).isEmpty());

        when(karten.findByMandatAndStapelAndDeletedIsFalseOrderByPrioDescIdAsc("test", DeckStapel.DONE))
                .thenReturn(List.of(karte()));
        assertEquals(1, service.kartenImStapel(DeckStapel.DONE).size());

        when(nachrichten.findByMandatAndDeletedIsFalseOrderByIdAsc("test")).thenReturn(List.of());
        assertTrue(service.nachrichten().isEmpty());
        assertTrue(service.schreibMatrix().isEmpty());
        assertTrue(service.agenten().isEmpty());
    }

    @Test
    @DisplayName("ein Beitrag zu einer unbekannten Karte wird gemeldet")
    void beitragUnbekannt() {
        when(karten.findById(404L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.beitrag(404L, "dee1", "x", DeckKommentar.Art.KOMMENTAR));
    }

    @Test
    @DisplayName("alleKarten geht über das Mandat der Sitzung")
    void alleKarten() {
        when(karten.findByMandatAndDeletedIsFalseOrderByPrioDescIdAsc("test")).thenReturn(List.of());
        assertTrue(service.alleKarten().isEmpty());
        verify(karten).findByMandatAndDeletedIsFalseOrderByPrioDescIdAsc(anyString());
    }

    // ── Soll 3: Postfach ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Zuweisen setzt den Bearbeiter und legt die Zuweisung ins Postfach")
    void zuweisen() {
        DeckKarte k = karte();
        k.setId(5L);
        k.setTitel("Deck bauen");
        when(karten.findById(5L)).thenReturn(Optional.of(k));

        service.zuweisen(5L, "  dee2  ", "chef");

        assertEquals("dee2", k.getBearbeiter(), "der Name wird getrimmt gespeichert");
        DeckNachricht n = gespeicherteNachrichten.get(0);
        assertEquals("chef", n.getVon());
        assertEquals("dee2", n.getAn());
        assertEquals(DeckNachricht.Art.ZUWEISUNG, n.getArt());
        assertEquals(5L, n.getKarteId());
        assertTrue(n.getText().contains("Deck bauen"), "die Anzeigezeile nennt die Karte beim Namen");
    }

    @Test
    @DisplayName("Zuweisen ohne Agentennamen wird gemeldet, nicht still verschluckt")
    void zuweisenOhneAgent() {
        assertThrows(IllegalArgumentException.class, () -> service.zuweisen(5L, "   ", "chef"));
        assertTrue(gespeicherteNachrichten.isEmpty(), "ohne Ziel entsteht kein Postfach-Eintrag");
    }

    @Test
    @DisplayName("Zuweisen einer unbekannten Karte wird gemeldet")
    void zuweisenUnbekannt() {
        when(karten.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.zuweisen(99L, "dee2", "chef"));
    }

    @Test
    @DisplayName("das Postfach liefert nur ungelesene Einträge des Agenten")
    void postfach() {
        when(nachrichten.findByMandatAndAnAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc("test", "dee1"))
                .thenReturn(List.of(nachricht("chef", "dee1"), nachricht("devops", "dee1")));

        assertEquals(2, service.postfachAnzahl("dee1"));
        assertEquals(2, service.postfach("dee1").size());
    }

    @Test
    @DisplayName("die Kopfzeile trägt Id, Absender, Art und Karte — aber keinen Text")
    void postfachKopfOhneText() {
        DeckNachricht n = nachricht("chef", "dee1");
        n.setId(7L);
        n.setArt(DeckNachricht.Art.ZUWEISUNG);
        n.setKarteId(2162L);
        when(nachrichten.findByMandatAndAnAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc("test", "dee1"))
                .thenReturn(List.of(n));

        List<PostfachKopf> kopf = service.postfachKopf("dee1");

        assertEquals(1, kopf.size());
        assertEquals(7L, kopf.get(0).id());
        assertEquals("chef", kopf.get(0).von());
        assertEquals("ZUWEISUNG", kopf.get(0).art());
        assertEquals(2162L, kopf.get(0).karteId());
    }

    @Test
    @DisplayName("eine Zuweisung kommt als ZUWEISUNG an die Kopfzeile, auch wenn das Feld null wäre")
    void postfachKopfOhneArt() {
        DeckNachricht n = nachricht("chef", "dee1");
        n.setId(8L);
        n.setArt(null);
        when(nachrichten.findByMandatAndAnAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc("test", "dee1"))
                .thenReturn(List.of(n));

        assertEquals("DIREKT", service.postfachKopf("dee1").get(0).art());
    }

    @Test
    @DisplayName("lesen markiert den Eintrag als gelesen")
    void lesen() {
        DeckNachricht n = nachricht("chef", "dee1");
        n.setId(7L);
        when(nachrichten.findById(7L)).thenReturn(Optional.of(n));

        DeckNachricht gelesen = service.lesen(7L, "dee1");

        assertTrue(gelesen.isGelesen());
        verify(nachrichten).save(n);
    }

    @Test
    @DisplayName("ein fremder Eintrag lässt sich nicht lesen — sonst wäre die Id ein Schlüssel zum fremden Post")
    void lesenFremd() {
        DeckNachricht n = nachricht("chef", "dee2");
        n.setId(7L);
        when(nachrichten.findById(7L)).thenReturn(Optional.of(n));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.lesen(7L, "dee1"));
        assertTrue(e.getMessage().contains("dee1"));
        assertFalse(n.isGelesen(), "ein abgewiesener Zugriff ändert nichts");
    }

    @Test
    @DisplayName("ein unbekannter oder gelöschter Eintrag wird beim Lesen gemeldet")
    void lesenUnbekannt() {
        when(nachrichten.findById(404L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.lesen(404L, "dee1"));

        DeckNachricht weg = nachricht("chef", "dee1");
        weg.setDeleted(true);
        when(nachrichten.findById(405L)).thenReturn(Optional.of(weg));
        assertThrows(IllegalArgumentException.class, () -> service.lesen(405L, "dee1"));
    }

    @Test
    @DisplayName("ein Beitrag auf einer fremden Karte legt eine Reaktion ins Postfach des Bearbeiters")
    void reaktion() {
        DeckKarte k = karte();
        k.setId(5L);
        k.setTitel("Deck");
        k.setBearbeiter("dee2");
        when(karten.findById(5L)).thenReturn(Optional.of(k));

        service.beitrag(5L, "dee1", "Bitte prüfen", DeckKommentar.Art.KOMMENTAR);

        assertEquals(1, gespeicherteNachrichten.size());
        DeckNachricht n = gespeicherteNachrichten.get(0);
        assertEquals(DeckNachricht.Art.REAKTION, n.getArt());
        assertEquals("dee2", n.getAn());
        assertEquals(5L, n.getKarteId());
    }

    @Test
    @DisplayName("auf die eigene Karte gibt es keine Reaktion — niemand schreibt sich selbst ins Postfach")
    void keineReaktionAufEigeneKarte() {
        DeckKarte k = karte();
        k.setId(5L);
        k.setBearbeiter("dee1");
        when(karten.findById(5L)).thenReturn(Optional.of(k));

        service.beitrag(5L, "dee1", "Fortschritt", DeckKommentar.Art.KOMMENTAR);

        assertTrue(gespeicherteNachrichten.isEmpty());
    }

    @Test
    @DisplayName("ohne Bearbeiter gibt es keine Reaktion")
    void keineReaktionOhneBearbeiter() {
        DeckKarte k = karte();
        k.setId(5L);
        when(karten.findById(5L)).thenReturn(Optional.of(k));

        service.beitrag(5L, "dee1", "x", DeckKommentar.Art.KOMMENTAR);

        assertTrue(gespeicherteNachrichten.isEmpty());
    }

    /** Karte im Test-Mandanten — ohne Mandat greift die Zugriffsklasse, wie sie soll. */
    private static DeckKarte karte() {
        DeckKarte k = new DeckKarte();
        k.setMandat("test");
        return k;
    }

    private static DeckNachricht nachricht(String von, String an) {
        DeckNachricht n = new DeckNachricht();
        n.setVon(von);
        n.setAn(an);
        n.setText("x");
        n.setMandat("test");
        return n;
    }
}
