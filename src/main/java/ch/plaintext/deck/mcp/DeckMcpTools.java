/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck.mcp;

import ch.plaintext.deck.DeckKarte;
import ch.plaintext.deck.DeckKommentar;
import ch.plaintext.deck.DeckNachricht;
import ch.plaintext.deck.DeckService;
import ch.plaintext.deck.DeckStapel;
import ch.plaintext.deck.PostfachKopf;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MCP-Werkzeuge des Boards für Agenten (Karte 2162, Soll 3).
 *
 * <p><b>Der teure Weg ist der Standard.</b> Ein Agent, der alle 30 Sekunden fragt «gibt es etwas für
 * mich?», darf dafür nicht den Inhalt seines Postfachs bezahlen. {@code deck_postfach} antwortet
 * darum mit einer Zeile — Zahl und Kopfzeilen ohne Text — und der Agent holt den Inhalt erst mit
 * {@code deck_lesen} zu der Id, die ihn interessiert.</p>
 *
 * <p>Alle Werkzeuge rufen {@link DeckService} auf, nicht die Repositories: die Regeln (eine Rückfrage
 * markiert die Karte, eine Antwort nimmt die Markierung zurück, eine Reaktion landet im Postfach)
 * gelten für Agenten genauso wie für die Seite.</p>
 *
 * <p>{@code @ConditionalOnClass} ist nötig, weil die Bibliothek nur in der Webapp liegt
 * ({@code provided}); ohne sie lässt sich das Modul allein bauen und testen.</p>
 */
@Component
@ConditionalOnClass(name = "org.springframework.ai.mcp.annotation.McpTool")
public class DeckMcpTools {

    private final DeckService service;

    public DeckMcpTools(DeckService service) {
        this.service = service;
    }

    // ── Soll 3: Postfach ──────────────────────────────────────────────────────

    @McpTool(name = "deck_postfach",
            description = "Gibt es etwas für mich? Antwortet mit einer Zeile: Anzahl ungelesener Einträge und "
                    + "je Eintrag Id, Absender, Art (DIREKT, ZUWEISUNG, REAKTION) und Kartennummer — ohne Inhalt. "
                    + "Den Inhalt holt deck_lesen mit der Id.")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    public String postfach(
            @McpToolParam(description = "Agentenname, z.B. dee1", required = true) String agent) {
        List<PostfachKopf> kopf = service.postfachKopf(agent);
        if (kopf.isEmpty()) {
            return "Nein, nichts für " + agent + ".";
        }
        StringBuilder sb = new StringBuilder("Ja, ").append(kopf.size())
                .append(kopf.size() == 1 ? " Eintrag" : " Einträge").append(" für ").append(agent).append(':');
        for (PostfachKopf k : kopf) {
            sb.append("\n- id=").append(k.id())
                    .append(" von=").append(k.von())
                    .append(" art=").append(k.art());
            if (k.karteId() != null) {
                sb.append(" karte=").append(k.karteId());
            }
        }
        return sb.toString();
    }

    @McpTool(name = "deck_lesen",
            description = "Liest einen Postfach-Eintrag samt Inhalt und markiert ihn als gelesen. Die Id stammt "
                    + "aus deck_postfach. Bei Art ZUWEISUNG oder REAKTION steht zusätzlich die Karte mit ihren offenen "
                    + "Beiträgen dabei — das ist der Inhalt, für den der Aufruf gedacht ist.")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    public String lesen(
            @McpToolParam(description = "Id des Eintrags aus deck_postfach", required = true) Long id,
            @McpToolParam(description = "Agentenname, z.B. dee1", required = true) String agent) {
        try {
            DeckNachricht n = service.lesen(id, agent);
            StringBuilder sb = new StringBuilder("Eintrag ").append(n.getId())
                    .append(" von ").append(n.getVon())
                    .append(" (").append(n.getArt()).append("):\n").append(n.getText());
            if (n.getKarteId() != null) {
                karteAnhaengen(sb, n.getKarteId());
            }
            return sb.toString();
        } catch (IllegalArgumentException e) {
            return "FEHLER: " + e.getMessage();
        }
    }

    // ── Karten ────────────────────────────────────────────────────────────────

    @McpTool(name = "deck_karten",
            description = "Listet die Karten des Boards einer Stapelspalte: Id, Titel, Labels, Bearbeiter und die "
                    + "Marken Interaktiv/Rückfrage. Ohne Stapel alle. Gibt bewusst keine Beschreibung und keine "
                    + "Kommentare aus — die holt deck_karte mit der Id.")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    public String karten(
            @McpToolParam(description = "vorrat, backlog, todo, inprogress oder done; leer = alle", required = false) String stapel) {
        try {
            List<DeckKarte> karten = stapel == null || stapel.isBlank()
                    ? service.alleKarten()
                    : service.kartenImStapel(DeckStapel.von(stapel));
            if (karten.isEmpty()) {
                return "Keine Karten" + (stapel == null || stapel.isBlank() ? "." : " im Stapel " + stapel + ".");
            }
            StringBuilder sb = new StringBuilder(karten.size() + (karten.size() == 1 ? " Karte" : " Karten") + ":");
            for (DeckKarte k : karten) {
                sb.append("\n- id=").append(k.getId())
                        .append(" [").append(k.getStapel().code()).append(']')
                        .append(' ').append(k.getTitel());
                if (k.getLabels() != null && !k.getLabels().isBlank()) {
                    sb.append(" (").append(k.getLabels()).append(')');
                }
                if (k.getBearbeiter() != null && !k.getBearbeiter().isBlank()) {
                    sb.append(" @").append(k.getBearbeiter());
                }
                if (k.isInteraktiv()) {
                    sb.append(" [Interaktiv]");
                }
                if (k.isRueckfrage()) {
                    sb.append(" [Rückfrage]");
                }
            }
            return sb.toString();
        } catch (IllegalArgumentException e) {
            return "FEHLER: " + e.getMessage();
        }
    }

    @McpTool(name = "deck_karte",
            description = "Zeigt eine Karte vollständig: Stapel, Beschreibung, Labels, Priorität, Bearbeiter und alle "
                    + "Beiträge chronologisch. Die Id stammt aus deck_karten oder aus dem Postfach.")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    public String karte(
            @McpToolParam(description = "Id der Karte", required = true) Long id) {
        Optional<DeckKarte> gefunden = service.karte(id);
        if (gefunden.isEmpty()) {
            return "FEHLER: Karte " + id + " gibt es nicht.";
        }
        DeckKarte k = gefunden.get();
        StringBuilder sb = new StringBuilder();
        sb.append(k.getTitel()).append("\nid=").append(k.getId())
                .append(" stapel=").append(k.getStapel().code())
                .append(" prio=").append(k.getPrio());
        if (k.getLabels() != null && !k.getLabels().isBlank()) {
            sb.append(" labels=").append(k.getLabels());
        }
        if (k.getBearbeiter() != null && !k.getBearbeiter().isBlank()) {
            sb.append(" bearbeiter=").append(k.getBearbeiter());
        }
        if (k.isInteraktiv()) {
            sb.append(" interaktiv=ja");
        }
        if (k.isRueckfrage()) {
            sb.append(" rueckfrage=offen");
        }
        if (k.getBeschreibung() != null && !k.getBeschreibung().isBlank()) {
            sb.append("\n\n").append(k.getBeschreibung());
        }
        List<DeckKommentar> beitraege = service.beitraege(k.getId());
        if (beitraege.isEmpty()) {
            sb.append("\n\nKeine Beiträge.");
        } else {
            sb.append("\n\nBeiträge (").append(beitraege.size()).append("):");
            for (DeckKommentar b : beitraege) {
                sb.append("\n- [").append(b.getArt()).append("] ").append(b.getAutor()).append(": ").append(b.getText());
            }
        }
        return sb.toString();
    }

    // ── Schreiben ─────────────────────────────────────────────────────────────

    @McpTool(name = "deck_kommentar",
            description = "Schreibt einen Beitrag zu einer Karte. Art KOMMENTAR (Vorgabe) hängt einen Fortschritt oder "
                    + "Beleg an, RUECKFRAGE stellt eine Frage an den Menschen und markiert die Karte im Rückfrage-Panel, "
                    + "ANTWORT beantwortet eine Rückfrage und nimmt die Markierung zurück. Auf eine Karte, die ein "
                    + "anderer Agent hat, geht zusätzlich eine Reaktion in dessen Postfach.")
    @PreAuthorize("hasAuthority('SCOPE_WRITE')")
    public String kommentar(
            @McpToolParam(description = "Id der Karte", required = true) Long karteId,
            @McpToolParam(description = "Agentenname, z.B. dee1", required = true) String autor,
            @McpToolParam(description = "Text des Beitrags", required = true) String text,
            @McpToolParam(description = "KOMMENTAR, RUECKFRAGE oder ANTWORT; leer = KOMMENTAR", required = false) String art) {
        try {
            DeckKommentar.Art a = art == null || art.isBlank()
                    ? DeckKommentar.Art.KOMMENTAR
                    : DeckKommentar.Art.valueOf(art.trim().toUpperCase());
            DeckKommentar c = service.beitrag(karteId, autor, text, a);
            return "OK: Beitrag " + c.getId() + " (" + c.getArt() + ") zu Karte " + karteId + " gespeichert.";
        } catch (IllegalArgumentException e) {
            return "FEHLER: " + e.getMessage();
        }
    }

    @McpTool(name = "deck_verschieben",
            description = "Verschiebt eine Karte in eine andere Stapelspalte und setzt den Bearbeiter. Gültige Stapel: "
                    + "vorrat, backlog, todo, inprogress, done.")
    @PreAuthorize("hasAuthority('SCOPE_WRITE')")
    public String verschieben(
            @McpToolParam(description = "Id der Karte", required = true) Long karteId,
            @McpToolParam(description = "Zielstapel: vorrat, backlog, todo, inprogress oder done", required = true) String stapel,
            @McpToolParam(description = "Agentenname, der die Karte übernimmt; leer = bisheriger bleibt", required = false) String bearbeiter) {
        try {
            DeckKarte k = service.verschieben(karteId, DeckStapel.von(stapel), bearbeiter);
            return "OK: Karte " + k.getId() + " ist jetzt in " + k.getStapel().code()
                    + (k.getBearbeiter() == null ? "." : " bei " + k.getBearbeiter() + ".");
        } catch (IllegalArgumentException e) {
            return "FEHLER: " + e.getMessage();
        }
    }

    @McpTool(name = "deck_zuweisen",
            description = "Weist einem Agenten eine Karte zu und legt die Zuweisung in dessen Postfach (Art ZUWEISUNG). "
                    + "Der Agent erfährt davon über deck_postfach, ohne dass jemand ihn anrufen muss.")
    @PreAuthorize("hasAuthority('SCOPE_WRITE')")
    public String zuweisen(
            @McpToolParam(description = "Id der Karte", required = true) Long karteId,
            @McpToolParam(description = "Agentenname, der die Karte bekommt", required = true) String agent,
            @McpToolParam(description = "Agentenname, der zuweist", required = true) String von) {
        try {
            DeckKarte k = service.zuweisen(karteId, agent, von);
            return "OK: Karte " + k.getId() + " gehört jetzt " + k.getBearbeiter() + ".";
        } catch (IllegalArgumentException e) {
            return "FEHLER: " + e.getMessage();
        }
    }

    @McpTool(name = "deck_nachricht",
            description = "Schickt einem anderen Agenten eine direkte Nachricht (Art DIREKT). Der Empfänger sieht sie "
                    + "beim nächsten deck_postfach.")
    @PreAuthorize("hasAuthority('SCOPE_WRITE')")
    public String nachricht(
            @McpToolParam(description = "Agentenname des Absenders", required = true) String von,
            @McpToolParam(description = "Agentenname des Empfängers", required = true) String an,
            @McpToolParam(description = "Text der Nachricht", required = true) String text) {
        try {
            DeckNachricht n = service.nachricht(von, an, text);
            return "OK: Nachricht " + n.getId() + " an " + an + " gespeichert.";
        } catch (IllegalArgumentException e) {
            return "FEHLER: " + e.getMessage();
        }
    }

    // ── Metriken (Soll 3: für einen Methodik-Agenten abfragbar) ───────────────

    @McpTool(name = "deck_metriken",
            description = "Kennzahlen des Boards für die Auswertung: Karten je Stapel, offene Rückfragen, Einträge je "
                    + "Agent und die Matrix «wer schreibt wie viel an wen». Ohne Parameter das ganze Board.")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    public String metriken(
            @McpToolParam(description = "Agent, auf den eingeschränkt wird; leer = alle", required = false) String agent) {
        StringBuilder sb = new StringBuilder("Board:");
        for (DeckStapel s : DeckStapel.values()) {
            sb.append("\n- ").append(s.code()).append(": ").append(service.kartenImStapel(s).size());
        }
        sb.append("\noffene-rueckfragen: ").append(service.offeneRueckfragen().size());

        List<DeckNachricht> nachrichten = service.nachrichten();
        sb.append("\nnachrichten: ").append(nachrichten.size());
        if (agent != null && !agent.isBlank()) {
            sb.append("\nungelesen-fuer-").append(agent).append(": ").append(service.postfachAnzahl(agent));
        }

        var matrix = service.schreibMatrix();
        if (matrix.isEmpty()) {
            sb.append("\nSchreibmatrix: leer.");
        } else {
            sb.append("\nSchreibmatrix (von -> an: Anzahl):");
            for (var von : matrix.entrySet()) {
                for (var an : von.getValue().entrySet()) {
                    if (agent == null || agent.isBlank()
                            || agent.equalsIgnoreCase(von.getKey()) || agent.equalsIgnoreCase(an.getKey())) {
                        sb.append("\n- ").append(von.getKey()).append(" -> ").append(an.getKey())
                                .append(": ").append(an.getValue());
                    }
                }
            }
        }
        return sb.toString();
    }

    // ── Hilfen ────────────────────────────────────────────────────────────────

    /** Hängt Karte und Beiträge an eine Postfach-Antwort — nur bei Zuweisung und Reaktion. */
    private void karteAnhaengen(StringBuilder sb, Long karteId) {
        Optional<DeckKarte> gefunden = service.karte(karteId);
        if (gefunden.isEmpty()) {
            sb.append("\n(Karte ").append(karteId).append(" gibt es nicht mehr.)");
            return;
        }
        DeckKarte k = gefunden.get();
        sb.append("\n\nKarte ").append(k.getId()).append(" [").append(k.getStapel().code()).append("] ").append(k.getTitel());
        if (k.isRueckfrage()) {
            sb.append(" (Rückfrage offen)");
        }
        List<DeckKommentar> beitraege = service.beitraege(k.getId());
        if (!beitraege.isEmpty()) {
            sb.append("\nBeiträge:");
            for (DeckKommentar b : beitraege) {
                sb.append("\n- [").append(b.getArt()).append("] ").append(b.getAutor()).append(": ").append(b.getText());
            }
        }
    }
}
