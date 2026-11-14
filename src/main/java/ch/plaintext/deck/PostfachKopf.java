/*
 * Copyright (C) plaintext.ch, 2026.
 */
package ch.plaintext.deck;

/**
 * Kopfzeile eines Postfach-Eintrags (Karte 2162, Soll 3) — <b>bewusst ohne Text</b>.
 *
 * <p>Soll 3 verlangt: «wenn ja, Kopfzeile mit Absender und Art und einer eindeutigen Id, aber kein
 * Inhalt; der Agent entscheidet selbst, wann er liest». Dieses Record ist diese Vorgabe in Code:
 * es hat kein Feld, in das der Inhalt passen würde. Ein DTO, das den Text mitführt und ihn nur
 * nicht anzeigt, würde beim nächsten umbauenden Handgriff doch wieder mitgeliefert — und der Agent
 * zahlt ihn.</p>
 *
 * @param id      eindeutige Nummer des Eintrags, mit der {@code DeckService#lesen} ihn holt
 * @param von     Absender
 * @param art     {@code DIREKT}, {@code ZUWEISUNG} oder {@code REAKTION}
 * @param karteId Karte, um die es geht; {@code null} bei einer direkten Nachricht
 */
public record PostfachKopf(long id, String von, String art, Long karteId) {
}
