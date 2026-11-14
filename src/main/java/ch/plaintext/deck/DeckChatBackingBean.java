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
import java.util.ArrayList;
import java.util.List;

/**
 * Seiten-Bean des Chats (Karte 2162, Soll 2).
 *
 * <p>Soll 2 verlangt zwei Dinge: <b>alle aktiven Agenten sichtbar</b> und <b>wer schreibt wie viel
 * an wen</b>. Beides liest dieselbe Tabelle — die Nachrichtenliste ist der Verlauf, die Matrix die
 * Summe darüber. Die Matrix wird beim Aufbau einmal gezogen und nicht je Zeile neu berechnet: sonst
 * liefe für jeden Balken eine Abfrage über den ganzen Mandanten.</p>
 */
@Component("deckChatBean")
@Scope("session")
@Getter
public class DeckChatBackingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Nicht {@code final}: {@code PlaintextSessionBeanSerialisierbarTest} weist ein
     * {@code final transient}-Dienstfeld zurück, weil es nach dem Deserialisieren dauerhaft
     * {@code null} bliebe und sich nicht mehr nachsetzen liesse.
     */
    private transient DeckService service;

    /** Alle Agenten, alphabetisch — die Antwort auf «wer ist aktiv». */
    private List<String> agenten;

    /** Der Verlauf, chronologisch. */
    private List<DeckNachricht> verlauf;

    /** Die Schreibgrafik, eine Zeile je Paar. */
    private List<SchreibZeile> grafik;

    /** Empfänger und Text für eine neue Nachricht aus der Oberfläche. */
    @Setter
    private String empfaenger;
    @Setter
    private String text;

    /** Fehlermeldung des letzten Zugriffs, {@code null} = alles gut. */
    @Setter
    private String fehler;

    public DeckChatBackingBean(DeckService service) {
        this.service = service;
    }

    /** Feldinjektion für Spring, damit das Feld nach dem Deserialisieren wieder gesetzt werden kann. */
    @Autowired
    void setService(DeckService service) {
        this.service = service;
    }

    /** {@code f:viewAction} beim Seitenaufbau — wie beim Brett bewusst nicht {@code @PostConstruct}. */
    public void laden() {
        agenten = service.agenten();
        verlauf = service.nachrichten();
        List<SchreibZeile> zeilen = SchreibZeile.aus(service.schreibMatrix());
        // Fleissigste Paare zuerst: die Frage ist "wer schreibt wie viel", und die stillen Paare
        // stehen als lange Nullenliste sonst im Weg. Bei Gleichstand bleibt die Reihenfolge der
        // Matrix, also die Reihenfolge des ersten Auftretens.
        zeilen.sort((a, b) -> Long.compare(b.anzahl(), a.anzahl()));
        grafik = zeilen;
    }

    public List<String> getAgenten() {
        if (agenten == null) {
            laden();
        }
        return agenten;
    }

    public List<DeckNachricht> getVerlauf() {
        if (verlauf == null) {
            laden();
        }
        return verlauf;
    }

    public List<SchreibZeile> getGrafik() {
        if (grafik == null) {
            laden();
        }
        return grafik;
    }

    /** Schreibt eine Nachricht an einen anderen Agenten. */
    public void senden() {
        if (empfaenger == null || empfaenger.isBlank() || text == null || text.isBlank()) {
            return;
        }
        try {
            service.nachricht(service.benutzer(), empfaenger.strip(), text.strip());
            text = null;
            fehler = null;
        } catch (RuntimeException e) {
            fehler = e.getMessage();
        }
        laden();
    }

    /** Die Auswahlliste des Empfängers; fällt lesbar zurück, solange noch niemand geschrieben hat. */
    public List<String> getAgentenAuswahl() {
        List<String> auswahl = new ArrayList<>(getAgenten());
        if (auswahl.isEmpty()) {
            auswahl.add(service.benutzer());
        }
        return auswahl;
    }
}
