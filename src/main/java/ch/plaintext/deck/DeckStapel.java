/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

/**
 * Die fünf Stapel des Boards (Karte 2162, Soll 1). Reihenfolge = Anzeigereihenfolge.
 *
 * <p>Die Namen sind absichtlich die des heutigen Nextcloud-Decks: solange beide Werkzeuge
 * nebeneinander laufen (Grenze der Karte), muss ein Agent nicht zwischen zwei Vokabularen
 * übersetzen.</p>
 */
public enum DeckStapel {

    VORRAT("vorrat"),
    BACKLOG("backlog"),
    TODO("todo"),
    INPROGRESS("inprogress"),
    DONE("done");

    private final String code;

    DeckStapel(String code) {
        this.code = code;
    }

    /** Der Name, der in MCP-Aufrufen und in der Datenbank steht. */
    public String code() {
        return code;
    }

    /**
     * Sucht einen Stapel über seinen Code. Wirft {@link IllegalArgumentException} mit den gültigen
     * Namen: ein Agent, der sich vertippt, soll lesen können, was erlaubt ist, statt ein {@code null}
     * weiterzureichen.
     */
    public static DeckStapel von(String code) {
        for (DeckStapel s : values()) {
            if (s.code.equalsIgnoreCase(code == null ? "" : code.trim())) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unbekannter Stapel '" + code + "'. Gültig: " + alleCodes());
    }

    public static String alleCodes() {
        StringBuilder b = new StringBuilder();
        for (DeckStapel s : values()) {
            if (!b.isEmpty()) {
                b.append(", ");
            }
            b.append(s.code);
        }
        return b.toString();
    }
}
