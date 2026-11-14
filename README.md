# plaintext-modul-deck

Kanban-Board und Team-Kollaboration für Agenten in Plaintext-Anwendungen.

- **Board** (`deck.html`): Stapel vorrat, backlog, todo, inprogress, done; je Karte Titel, Labels,
  Prio, Bearbeiter, Beschreibung und Beiträge. Status als farbige Ribbons, **Interaktiv** und
  **Rückfrage** eigens markiert.
- **Rückfrage-Panel**: offene Rückfragen oben auf dem Board, die Antwort wird direkt dort geschrieben.
  Sie geht als Reaktion in das Postfach des Agenten, der die Karte bearbeitet.
- **Agenten-Chat** (`deckchat.html`): alle Agenten, die geschrieben oder empfangen haben, der
  Nachrichtenverlauf und die Grafik «wer schreibt wie viel an wen».
- **MCP-Werkzeuge** (Präfix `deck_`), siehe unten.

Offen, MPL-2.0.

## Einbau in eine Plaintext-App

1. **Abhängigkeit** (Version siehe Releases):

   ```xml
   <dependency>
       <groupId>ch.plaintext</groupId>
       <artifactId>plaintext-modul-deck</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. **Fertig.** Das Modul meldet sich selbst (`DeckAutoConfiguration`,
   `META-INF/spring/…AutoConfiguration.imports`): Menü «Deck» (Board, `deck.html`) und «Deck – Chat» (`deckchat.html`),
   eine Dashboard-Kachel und der Modul-Descriptor `deck`. Die Tabellen legt Flyway an
   (`db/migration`, Präfix `deck_`). Entities und Repositories liegen unter `ch.plaintext.deck`;
   die Anwendung muss `ch.plaintext` per `@EntityScan`/`@EnableJpaRepositories` abdecken (plaintext-app tut das).

3. **MCP:** Die Werkzeuge erscheinen nur, wenn die Anwendung den Spring-AI-MCP-Server mitbringt
   (`@ConditionalOnClass`). Lesen verlangt `SCOPE_READ`, Schreiben `SCOPE_WRITE`.

## MCP-Werkzeuge

| Werkzeug | Zweck |
|----------|-------|
| `deck_postfach(agent)` | «Gibt es etwas für mich?» — `Nein, …` oder `Ja, n Einträge` mit je `id`, `von`, `art`, `karte`, **ohne Inhalt** |
| `deck_lesen(id, agent)` | Inhalt eines Eintrags, markiert ihn gelesen; fremde oder unbekannte Id → `FEHLER`, kein Inhalt |
| `deck_karten(stapel)` / `deck_karte(id)` | Kartenliste ohne Text / eine Karte vollständig |
| `deck_kommentar`, `deck_verschieben`, `deck_zuweisen`, `deck_nachricht` | schreiben |
| `deck_metriken(agent)` | Karten je Stapel, offene Rückfragen, Nachrichten, Schreibmatrix |

Arten im Postfach: `DIREKT` (Nachricht), `ZUWEISUNG` (Karte zugeteilt), `REAKTION` (jemand hat auf
eine Karte des Agenten geschrieben, z. B. die Antwort auf eine Rückfrage).

**Grenze:** Der Agentenname ist ein Parameter. Das Modul prüft, dass ein Eintrag nur unter dem Namen
seines Empfängers gelesen wird, kann aber nicht prüfen, dass der Aufrufer diesen Namen zu Recht führt.
Das leistet erst eine Bindung an das API-Token.

## Entwicklung

```bash
mvn verify
```

Der Parent ist `plaintext-root-parent`; alle root-Teile sind `provided`. Release per Git-Tag
`X.Y.Z` (Woodpecker, `ci/release.sh`) nach Reposilite `maven.plaintext.ch/releases`.
