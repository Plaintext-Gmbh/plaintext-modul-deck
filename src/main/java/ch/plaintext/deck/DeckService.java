/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

import ch.plaintext.PlaintextSecurity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fachlogik des Boards (Karte 2162, Soll 1) — Karten anlegen, verschieben, kommentieren,
 * Rückfragen stellen und beantworten.
 *
 * <p>Die Klasse ist die einzige Stelle, die eine Karte verändert: Seiten-Bean und MCP-Werkzeuge
 * rufen sie beide auf. Andernfalls hätte das Board Regeln, die für Agenten nicht gelten — genau
 * die Doppelung, die im heutigen Deck dazu geführt hat, dass ein Label nur in der Oberfläche
 * gesetzt wurde.</p>
 */
@Service
@Transactional
public class DeckService {

    private final DeckKarteRepository karten;
    private final DeckKommentarRepository kommentare;
    private final DeckNachrichtRepository nachrichten;
    private final DeckKarteZugriff kartenZugriff;
    private final DeckNachrichtZugriff nachrichtenZugriff;
    private final PlaintextSecurity security;

    public DeckService(DeckKarteRepository karten, DeckKommentarRepository kommentare,
                       DeckNachrichtRepository nachrichten, DeckKarteZugriff kartenZugriff,
                       DeckNachrichtZugriff nachrichtenZugriff, PlaintextSecurity security) {
        this.karten = karten;
        this.kommentare = kommentare;
        this.nachrichten = nachrichten;
        this.kartenZugriff = kartenZugriff;
        this.nachrichtenZugriff = nachrichtenZugriff;
        this.security = security;
    }

    /** Mandat der laufenden Sitzung. */
    public String mandat() {
        String m = security.getMandat();
        return m == null || m.isBlank() ? "default" : m;
    }

    /** Angemeldeter Benutzer; {@code null} (z.B. im MCP-Aufruf) wird zu {@code unbekannt}. */
    public String benutzer() {
        String u = security.getUser();
        return u == null || u.isBlank() ? "unbekannt" : u;
    }

    // ── Karten ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeckKarte> alleKarten() {
        return karten.findByMandatAndDeletedIsFalseOrderByPrioDescIdAsc(mandat());
    }

    @Transactional(readOnly = true)
    public List<DeckKarte> kartenImStapel(DeckStapel stapel) {
        return karten.findByMandatAndStapelAndDeletedIsFalseOrderByPrioDescIdAsc(mandat(), stapel);
    }

    /**
     * Board als Stapel → Karten. Die Reihenfolge der {@link DeckStapel} ist die Anzeigereihenfolge;
     * ein leerer Stapel bleibt als leerer Eintrag stehen, damit das Brett nicht springt.
     */
    @Transactional(readOnly = true)
    public Map<DeckStapel, List<DeckKarte>> board() {
        Map<DeckStapel, List<DeckKarte>> brett = new LinkedHashMap<>();
        List<DeckKarte> alle = alleKarten();
        for (DeckStapel stapel : DeckStapel.values()) {
            List<DeckKarte> gefiltert = new ArrayList<>();
            for (DeckKarte k : alle) {
                if (k.getStapel() == stapel) {
                    gefiltert.add(k);
                }
            }
            brett.put(stapel, gefiltert);
        }
        return brett;
    }

    @Transactional(readOnly = true)
    public Optional<DeckKarte> karte(long id) {
        return kartenZugriff.find(id);
    }

    public DeckKarte anlegen(String titel, String beschreibung, DeckStapel stapel, int prio, String labels) {
        DeckKarte k = new DeckKarte();
        k.setTitel(titel);
        k.setBeschreibung(beschreibung);
        k.setStapel(stapel == null ? DeckStapel.BACKLOG : stapel);
        k.setPrio(prio);
        k.setLabels(labels);
        k.setKurzfassung(kurzfassung(beschreibung));
        k.setMandat(mandat());
        return karten.save(k);
    }

    /** Verschiebt eine Karte in einen anderen Stapel und setzt den Bearbeiter. */
    public DeckKarte verschieben(long id, DeckStapel ziel, String bearbeiter) {
        DeckKarte k = karte(id).orElseThrow(() -> new IllegalArgumentException("Karte " + id + " gibt es nicht."));
        k.setStapel(ziel);
        if (bearbeiter != null && !bearbeiter.isBlank()) {
            k.setBearbeiter(bearbeiter.trim());
        }
        return karten.save(k);
    }

    /** Setzt oder entfernt den Bearbeiter; leer = niemand. */
    public DeckKarte bearbeiterSetzen(long id, String bearbeiter) {
        DeckKarte k = karte(id).orElseThrow(() -> new IllegalArgumentException("Karte " + id + " gibt es nicht."));
        k.setBearbeiter(bearbeiter == null || bearbeiter.isBlank() ? null : bearbeiter.trim());
        return karten.save(k);
    }

    /**
     * Setzt oder entfernt ein Label. Kommagetrennt, getrimmt, ohne Doppelungen — dieselbe Form, die
     * das heutige Deck in seiner Label-Zeile führt.
     */
    public DeckKarte label(long id, String label, boolean setzen) {
        DeckKarte k = karte(id).orElseThrow(() -> new IllegalArgumentException("Karte " + id + " gibt es nicht."));
        k.setLabels(labelAendern(k.getLabels(), label, setzen));
        return karten.save(k);
    }

    static String labelAendern(String vorhanden, String label, boolean setzen) {
        String neu = label == null ? "" : label.trim();
        List<String> liste = new ArrayList<>();
        boolean schonDa = false;
        if (vorhanden != null && !vorhanden.isBlank()) {
            for (String teil : vorhanden.split(",")) {
                String t = teil.trim();
                if (t.isEmpty()) {
                    continue;
                }
                if (t.equalsIgnoreCase(neu)) {
                    // Vorhandene Schreibweise bleibt stehen: Wer "Dev" gepflegt hat und "dev"
                    // nochmals setzt, meint dasselbe Label und will es nicht umbenannt bekommen.
                    schonDa = true;
                    if (setzen) {
                        liste.add(t);
                    }
                } else {
                    liste.add(t);
                }
            }
        }
        if (setzen && !schonDa && !neu.isEmpty()) {
            liste.add(neu);
        }
        return liste.isEmpty() ? null : String.join(", ", liste);
    }

    // ── Beiträge ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DeckKommentar> beitraege(long karteId) {
        return kommentare.findByKarteIdAndDeletedIsFalseOrderByIdAsc(karteId);
    }

    /** Kommentar, Rückfrage oder Antwort zu einer Karte. Eine Rückfrage setzt die Markierung. */
    public DeckKommentar beitrag(long karteId, String autor, String text, DeckKommentar.Art art) {
        DeckKarte k = karte(karteId).orElseThrow(() -> new IllegalArgumentException("Karte " + karteId + " gibt es nicht."));
        DeckKommentar kommentar = new DeckKommentar();
        kommentar.setKarte(k);
        kommentar.setAutor(autor);
        kommentar.setText(text);
        kommentar.setArt(art == null ? DeckKommentar.Art.KOMMENTAR : art);
        kommentar.setMandat(mandat());
        if (kommentar.getArt() == DeckKommentar.Art.RUECKFRAGE) {
            k.setRueckfrage(true);
            karten.save(k);
        } else if (kommentar.getArt() == DeckKommentar.Art.ANTWORT) {
            k.setRueckfrage(false);
            karten.save(k);
        }
        reaktion(k, autor);
        return kommentare.save(kommentar);
    }

    /** Karten, die auf eine Antwort warten (Rückfrage-Panel). */
    @Transactional(readOnly = true)
    public List<DeckKarte> offeneRueckfragen() {
        return karten.findByMandatAndRueckfrageIsTrueAndDeletedIsFalseOrderByIdAsc(mandat());
    }

    // ── Nachrichten ───────────────────────────────────────────────────────────

    public DeckNachricht nachricht(String von, String an, String text) {
        return nachricht(von, an, text, DeckNachricht.Art.DIREKT, null);
    }

    /** Postfach-Eintrag beliebiger Art; {@code karteId} bleibt bei einer direkten Nachricht leer. */
    public DeckNachricht nachricht(String von, String an, String text, DeckNachricht.Art art, Long karteId) {
        DeckNachricht n = new DeckNachricht();
        n.setVon(von);
        n.setAn(an);
        n.setText(text);
        n.setArt(art == null ? DeckNachricht.Art.DIREKT : art);
        n.setKarteId(karteId);
        n.setMandat(mandat());
        return nachrichten.save(n);
    }

    @Transactional(readOnly = true)
    public List<DeckNachricht> nachrichten() {
        return nachrichten.findByMandatAndDeletedIsFalseOrderByIdAsc(mandat());
    }

    /**
     * Weist einem Agenten eine Karte zu und legt die Zuweisung ins Postfach (Soll 3, Art ZUWEISUNG).
     *
     * <p>Die Nachricht trägt nur den Titel als Anzeigezeile, nicht die Beschreibung: der Agent soll
     * selbst entscheiden, wann er die Karte liest.</p>
     */
    public DeckKarte zuweisen(long karteId, String agent, String von) {
        if (agent == null || agent.isBlank()) {
            throw new IllegalArgumentException("Ohne Agentennamen lässt sich keine Karte zuweisen.");
        }
        DeckKarte k = karte(karteId).orElseThrow(() -> new IllegalArgumentException("Karte " + karteId + " gibt es nicht."));
        String ziel = agent.trim();
        k.setBearbeiter(ziel);
        karten.save(k);
        nachricht(von, ziel, "Karte " + k.getId() + ": " + k.getTitel(), DeckNachricht.Art.ZUWEISUNG, k.getId());
        return k;
    }

    /**
     * Postfach eines Agenten: ungelesene Einträge, chronologisch.
     *
     * <p>Der Aufruf ist die Grundlage von «gibt es etwas für mich?» und darum billig gehalten — er
     * liest eine indizierte Abfrage und gibt die Zeilen mit Text zurück. Für die kopfzeilen-only
     * Variante siehe {@link #postfachKopf(String)}.</p>
     */
    @Transactional(readOnly = true)
    public List<DeckNachricht> postfach(String agent) {
        return nachrichten.findByMandatAndAnAndGelesenIsFalseAndDeletedIsFalseOrderByIdAsc(mandat(), agent);
    }

    /** Nur die Anzahl — die Antwort auf «gibt es etwas für mich?» ohne eine einzige Textzeile. */
    @Transactional(readOnly = true)
    public int postfachAnzahl(String agent) {
        return postfach(agent).size();
    }

    /**
     * Kopfzeilen des Postfachs: Id, Absender, Art, Karte — <b>kein Text</b>.
     *
     * <p>Soll 3 verlangt genau das: der Agent soll sehen, dass es etwas gibt und von wem, aber nicht
     * den Inhalt mitbezahlen. Er holt ihn erst mit {@link #lesen(long, String)}. Das Rückgaberecord
     * {@link PostfachKopf} hat kein Textfeld — der Inhalt kann also nicht versehentlich mitreisen.</p>
     */
    @Transactional(readOnly = true)
    public List<PostfachKopf> postfachKopf(String agent) {
        List<PostfachKopf> kopf = new ArrayList<>();
        for (DeckNachricht n : postfach(agent)) {
            kopf.add(new PostfachKopf(n.getId(), n.getVon(),
                    n.getArt() == null ? DeckNachricht.Art.DIREKT.name() : n.getArt().name(),
                    n.getKarteId()));
        }
        return kopf;
    }

    /**
     * Liest einen Eintrag und markiert ihn als gelesen. Der Agent entscheidet damit selbst, wann er
     * den Inhalt bezahlt.
     *
     * <p>Ein fremder Eintrag wird abgewiesen, nicht still zurückgegeben: sonst könnte ein Agent die
     * Id eines anderen raten und dessen Post lesen.</p>
     */
    public DeckNachricht lesen(long id, String agent) {
        DeckNachricht n = nachrichtenZugriff.find(id)
                .orElseThrow(() -> new IllegalArgumentException("Eintrag " + id + " gibt es nicht."));
        if (agent == null || !agent.trim().equals(n.getAn())) {
            throw new IllegalArgumentException("Eintrag " + id + " gehört nicht zu " + agent + ".");
        }
        n.setGelesen(true);
        return nachrichten.save(n);
    }

    /**
     * Reaktion auf eine Karte (Soll 3, Art REAKTION): jemand hat auf eine Karte geschrieben, die ein
     * Agent hat. Der Schreiber selbst bekommt keine Reaktion auf sein eigenes Wort.
     */
    private void reaktion(DeckKarte k, String autor) {
        String bearbeiter = k.getBearbeiter();
        if (bearbeiter == null || bearbeiter.isBlank() || bearbeiter.equalsIgnoreCase(autor)) {
            return;
        }
        nachricht(autor, bearbeiter, "Karte " + k.getId() + ": " + k.getTitel(),
                DeckNachricht.Art.REAKTION, k.getId());
    }

    /**
     * «Wer schreibt wie viel an wen» (Soll 2, Grafik): Matrix Absender → Empfänger → Anzahl.
     * Gezählt werden die gespeicherten Nachrichten, nicht die Zeichen: die Frage der Karte ist
     * Häufigkeit, nicht Umfang.
     */
    @Transactional(readOnly = true)
    public Map<String, Map<String, Long>> schreibMatrix() {
        Map<String, Map<String, Long>> matrix = new LinkedHashMap<>();
        for (DeckNachricht n : nachrichten()) {
            matrix.computeIfAbsent(n.getVon(), k -> new LinkedHashMap<>())
                    .merge(n.getAn(), 1L, Long::sum);
        }
        return matrix;
    }

    /** Alle Agenten, die je geschrieben oder empfangen haben, alphabetisch. */
    @Transactional(readOnly = true)
    public List<String> agenten() {
        List<String> namen = new ArrayList<>();
        for (DeckNachricht n : nachrichten()) {
            if (!namen.contains(n.getVon())) {
                namen.add(n.getVon());
            }
            if (!namen.contains(n.getAn())) {
                namen.add(n.getAn());
            }
        }
        namen.sort(String::compareToIgnoreCase);
        return namen;
    }

    // ── Hilfen ────────────────────────────────────────────────────────────────

    /** Erste Zeile der Beschreibung, gekürzt — was in der Kopfzeile eines MCP-Aufrufs steht. */
    static String kurzfassung(String beschreibung) {
        if (beschreibung == null || beschreibung.isBlank()) {
            return null;
        }
        String erste = beschreibung.strip().split("\\R", 2)[0].strip();
        return erste.length() <= 200 ? erste : erste.substring(0, 197) + "…";
    }

    /** Zeitstempel für Anzeigen; die Entity führt die Auditing-Felder von {@code SuperModel}. */
    static String zeit(Instant zeitpunkt) {
        return zeitpunkt == null ? "" : zeitpunkt.toString();
    }
}
