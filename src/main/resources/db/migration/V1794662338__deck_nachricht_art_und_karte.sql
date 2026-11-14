-- Karte 2162, Soll 3: das Postfach des Agenten braucht die Art und die Karte.
--
-- Gefragt wird "gibt es etwas fuer mich?" - und die Antwort muss sagen, WAS es ist: eine direkte
-- Nachricht, eine zugewiesene Karte oder eine Reaktion auf eine eigene Karte. Ohne die Art muesste
-- der Agent erst lesen, um zu wissen, ob es ihn ueberhaupt betrifft; genau das soll er nicht.
--
-- karte_id ist bewusst KEIN Fremdschluessel: eine Nachricht ueber eine Karte soll lesbar bleiben,
-- auch wenn die Karte spaeter geloescht wird. Ein Fremdschluessel mit ON DELETE SET NULL waere die
-- Alternative - er zwingt aber jede Loeschung der Karte durch dieselbe Tabellenkette, und die
-- Nachricht ist ein Protokoll, keine Unterordnung.
--
-- Bestandszeilen sind direkte Nachrichten; der Default traegt sie mit.

ALTER TABLE deck_nachricht ADD COLUMN art VARCHAR(20) NOT NULL DEFAULT 'DIREKT';
ALTER TABLE deck_nachricht ADD COLUMN karte_id BIGINT;

-- Das Postfach liest je Mandat und Empfaenger, unerledigt zuerst. Der vorhandene Index
-- idx_deck_nachricht_empfaenger deckt (mandat, an, gelesen) bereits; die Art kommt als
-- Zusatzspalte dazu, damit die Kopfzeile ohne Tabellenzugriff entsteht.
CREATE INDEX idx_deck_nachricht_art ON deck_nachricht (mandat, an, art);
