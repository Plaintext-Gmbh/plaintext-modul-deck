# Soll 4: Polling oder Push (Karte 2162)

Frage: Wie oft soll ein Agent nach dem Bootstrap `deck_postfach` fragen? Ist 1 Minute zu knapp?
Oder soll das Modul über herdr aktiv wecken?

## Messung 1: was ein Poll-Turn kostet

Quelle: `usage`-Felder der eigenen Claude-Code-Session von sre-claude (Opus), 71 Modell-Turns am
14.11.2026, Datei `~/.claude/projects/-home-mad-code/<session>.jsonl`. Nur die Zahlen gelesen, keine Inhalte.

| Feld | Minimum | Median | Maximum |
|---|---:|---:|---:|
| `cache_read_input_tokens` (Kontext, der je Turn neu gelesen wird) | 27 525 | 116 219 | 175 534 |
| `cache_creation_input_tokens` (neu in den Cache geschrieben) | 140 | 1 150 | 65 967 |
| `output_tokens` | 75 | 349 | 2 149 |
| `input_tokens` (ungecacht) | 2 | 2 | 4 |

- **Frisch gestarteter Agent, erster Turn:** 27 525 gelesen und 27 989 geschrieben, 200 Ausgabe.
- **Der kleinste Turn** (ein Werkzeugaufruf mit kurzer Antwort): 140 geschrieben, 75 687 gelesen, 121 Ausgabe.
- **Der grösste Schreibposten, 65 967,** fiel nach einer Pause von gut vier Stunden an: Der Cache war
  abgelaufen, und der ganze Kontext wurde neu geschrieben.

**Die Antwort des Werkzeugs ist nicht der Kostenpunkt.** `Nein, nichts für sre-claude.` sind 28
Zeichen (Tokenzahl nicht gemessen, grob zehn). **Bezahlt wird der Turn:** Jede Frage eines Modell-Agenten liest
seinen ganzen Kontext neu, bei einem frischen Agenten rund 28 000 Tokens und bei einem, der eine
Weile gearbeitet hat, mehr als 100 000. Ein kurzes Ja/Nein macht den Poll darum nicht billig. Es
verhindert nur, dass er noch teurer wird.

## Rechnung

Gewichte nach Anthropic-Preisliste, relativ zum normalen Input-Token: Cache lesen 0,1×, Cache
schreiben 1,25× (5-Minuten-Cache) bzw. 2× (1-Stunden-Cache), Ausgabe 5× (Opus). Einheit:
Input-Token-Äquivalente (ITÄ). Pro leerem Poll angesetzt: 140 geschrieben, 121 Ausgabe
(kleinster gemessener Turn) und der Kontext C als Lesung.

| Kontext C | je Poll | 1-Minuten-Takt, je Stunde | 8 Stunden |
|---:|---:|---:|---:|
| 27 525 (frisch) | 2 753 + 175 + 605 ≈ **3 500 ITÄ** | ≈ 212 000 | ≈ 1,7 Mio. |
| 116 219 (Median) | 11 622 + 175 + 605 ≈ **12 400 ITÄ** | ≈ 744 000 | ≈ 6,0 Mio. |
| 175 534 (Maximum) | 17 553 + 175 + 605 ≈ **18 300 ITÄ** | ≈ 1,1 Mio. | ≈ 8,8 Mio. |

**Bei zehn Agenten im 1-Minuten-Takt** sind das 17 bis 88 Mio. ITÄ pro Arbeitstag, allein für Nein.

**Ein langsamerer Takt hilft weniger, als er verspricht.** Liegt der Abstand über der Lebensdauer
des Caches (5 Minuten bzw. 1 Stunde), wird bei jedem Poll der ganze Kontext neu geschrieben. Das
kostet 1,25 C bzw. 2 C statt 0,1 C, also das 12,5- bis 20-Fache je Poll. Bei C = 116 219 und
5-Minuten-Cache kostet ein 10-Minuten-Takt rund 146 000 ITÄ je Poll und 875 000 je Stunde. Das ist
**mehr** als der 1-Minuten-Takt (744 000). Die gemessenen 65 967 nach der langen Pause zeigen genau diesen Fall.

## Messung 2: was ein Poll ohne Modell kostet

Fragt nicht das Modell, sondern ein Skript neben dem Agenten (curl gegen `/mcp`, `deck_postfach`),
kostet ein Nein **null Modell-Tokens**: eine HTTP-Anfrage und eine indizierte Abfrage
(`mandat, an, gelesen=false`). Das Modell wird nur geweckt, wenn die Antwort Ja ist. Dann kostet
es genau einen Turn, so wie heute eine Meldung über `bin/melden`.

## Empfehlung

1. **Kein Modell-Polling im Minutentakt.** Es ist nicht «zu knapp», es ist falsch bemessen: Die
   Kosten hängen am Kontext des Agenten, nicht an der Antwort, und sie fallen auch bei Nein an.
2. **Push über herdr, ausgelöst von einem billigen Poll ausserhalb des Modells.** Ein kleiner
   Wächter je Agent (oder einer für alle) fragt `deck_postfach` alle 30–60 Sekunden ab. Bei Ja
   schickt er dem Agenten **eine** Zeile mit Kopf (Id, Absender, Art). Der Agent entscheidet
   selbst, wann er `deck_lesen` aufruft. Damit kommt die Nachricht spätestens nach 60 Sekunden an,
   und ein Nein kostet 0 Tokens.
3. **Eine dauerhafte Verbindung des Moduls zu herdr** (das Modul weckt selbst) wäre die
   Ausbaustufe. Sie spart den Poll des Wächters, verlangt aber, dass die Anwendung auf
   apps.plaintext.ch die herdr-Instanz auf dem Arbeitsrechner erreicht. Das ist eine Netz- und
   Sicherheitsfrage, die das Modul nicht lösen kann. Darum zuerst Schritt 2.
4. **Regelkonflikt, der vor Schritt 2 entschieden werden muss:** Laut `agents/meldungen.md` schreibt
   kein Agent direkt in ein fremdes herdr-Fenster, weil eine Eingabe ohne Kopfzeile aussähe, als
   käme sie von Daniel. Der Wächter muss darum die Kopfzeile von `bin/melden` mitschicken
   (`[melden: deck] (Timer/Skript, kein Mensch)`), oder Daniel lässt ihn als Ausnahme zu.
