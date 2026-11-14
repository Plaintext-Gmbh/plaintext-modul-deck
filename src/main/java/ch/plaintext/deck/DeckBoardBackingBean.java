/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Seiten-Bean des Boards (Karte 2162, Soll 1).
 *
 * <p>Sie hält nur, was die Seite zum Zeichnen braucht: das Brett, die offenen Rückfragen und die
 * Auswahl für das Detailfenster. Jede fachliche Regel steckt in {@link DeckService} — die
 * MCP-Werkzeuge rufen dieselbe Klasse auf und dürfen nicht an einer zweiten Wahrheit hängen.</p>
 */
@Component("deckBoardBean")
@Scope("session")
@Getter
public class DeckBoardBackingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Nicht {@code final}: {@code PlaintextSessionBeanSerialisierbarTest} weist ein
     * {@code final transient}-Dienstfeld zurück, weil es nach dem Deserialisieren dauerhaft
     * {@code null} bliebe und sich nicht mehr nachsetzen liesse.
     */
    private transient DeckService service;

    /** Stapel → Karten, in Anzeigereihenfolge. */
    private Map<DeckStapel, List<DeckKarte>> board;

    /** Karten, die auf eine Antwort warten. */
    private List<DeckKarte> offeneRueckfragen;

    /** Beiträge der gerade geöffneten Karte. */
    private List<DeckKommentar> beitraege;

    /** Gerade geöffnete Karte, {@code null} = kein Detail offen. */
    private Long offeneKarte;

    /** Titel der neuen Karte im Anlegefeld, damit ein Fehlschlag die Eingabe nicht verschluckt. */
    @Setter
    private String neuerTitel;

    /** Text der Antwort im Rückfrage-Panel. */
    @Setter
    private String antwort;

    /** Fehlermeldung des letzten Zugriffs, {@code null} = alles gut. */
    @Setter
    private String fehler;

    public DeckBoardBackingBean(DeckService service) {
        this.service = service;
    }

    /** Feldinjektion für Spring, damit das Feld nach dem Deserialisieren wieder gesetzt werden kann. */
    @Autowired
    void setService(DeckService service) {
        this.service = service;
    }

    /**
     * {@code f:viewAction} beim Seitenaufbau — bewusst nicht {@code @PostConstruct}: die Bean lebt
     * in der Sitzung, ein PostConstruct liefe einmal beim Anlegen und zeigte danach den ersten Stand
     * für immer.
     */
    public void laden() {
        board = service.board();
        offeneRueckfragen = service.offeneRueckfragen();
        if (offeneKarte != null) {
            beitraege = service.beitraege(offeneKarte);
        }
        // {@code fehler} bleibt bewusst stehen: anlegen() und antworten() rufen laden() auf, um das
        // neue Brett zu holen. Würde laden() die Meldung löschen, sähe Daniel von einem Fehlschlag
        // nie etwas — die Seite zeigte still den alten Stand.
    }

    /** Wechselt den dargestellten Stapel — die Seite braucht dafür keinen Zustand, das Brett hat alle. */
    public Map<DeckStapel, List<DeckKarte>> getBoard() {
        if (board == null) {
            laden();
        }
        return board;
    }

    public List<DeckKarte> getOffeneRueckfragen() {
        if (offeneRueckfragen == null) {
            laden();
        }
        return offeneRueckfragen;
    }

    /**
     * Labels der Karte für die Anzeige, ohne «Interaktiv» und «Rückfrage»: die beiden stehen schon
     * als eigene Marke in Signalfarbe, ein zweites graues Schild wäre Rauschen.
     */
    public List<String> labelsOhneMarken(DeckKarte k) {
        List<String> liste = new java.util.ArrayList<>();
        if (k == null || k.getLabels() == null) {
            return liste;
        }
        for (String teil : k.getLabels().split(",")) {
            String t = teil.trim();
            if (!t.isEmpty() && !t.equalsIgnoreCase(DeckService.LABEL_INTERAKTIV) && !t.equalsIgnoreCase("Rückfrage")) {
                liste.add(t);
            }
        }
        return liste;
    }

    /** Öffnet das Detail einer Karte. */
    public void oeffnen(Long karteId) {
        offeneKarte = karteId;
        beitraege = karteId == null ? List.of() : service.beitraege(karteId);
    }

    /** Legt eine Karte im Stapel {@code vorrat} an — der erste Stapel, wie im heutigen Deck. */
    public void anlegen() {
        if (neuerTitel == null || neuerTitel.isBlank()) {
            return;
        }
        try {
            service.anlegen(neuerTitel.strip(), null, DeckStapel.VORRAT, 0, null);
            neuerTitel = null;
            fehler = null;
        } catch (RuntimeException e) {
            fehler = e.getMessage();
        }
        laden();
    }

    /** Beantwortet die Rückfrage der geöffneten Karte. */
    public void antworten() {
        if (offeneKarte == null || antwort == null || antwort.isBlank()) {
            return;
        }
        try {
            service.beitrag(offeneKarte, mandantenName(), antwort.strip(), DeckKommentar.Art.ANTWORT);
            antwort = null;
            fehler = null;
        } catch (RuntimeException e) {
            fehler = e.getMessage();
        }
        laden();
    }

    /** Der Mensch, der hier antwortet — der angemeldete Benutzer. */
    private String mandantenName() {
        return service.benutzer();
    }
}
