# Benennungen und Namenssysteme

1. Wie stehen die Begriffe Entität, Zugriffspunkt, Adresse, Namen und Bezeichner in
   Beziehung zueinander?
- Entität ist etwas an dem arbeiten kann (Ressourcen wie Hosts, Drucker, Festplatten, Dateien). Eine Entität kann mehrere Zugriffspunkte haben.
- Zugriffspunkt wird benötigt um auf eine Entität zuzugreifen. Ein Zugriffspunkt ist eine besondere Art von Entität.
- Adresse ist der Name des Zugriffspunkts.
- Name der Entität, der unabhängig von Adresse der Entität ist. Das nennt man ortsunabhängig.
- Bezeichner ist ein eindeutiger Name für eine Entität. Bezeichner und Adresse sind zwei Arten von Benennungen.
- Jeder Bezeichner verweist auf höchstens eine Entität.
- Auf jede Entität verweist hochstens ein Bezeichner.
- Ein Bezeichner verweist immer auf die gleiche Entität.
2. Welche einfache Lösungen gibt es zum Auffinden von Entitäten bei linearer Benennung?
- Broadcasts und Multicasts: Eine Nachricht mit Bezeichner der Entität als Inhalt an die Broadcast Adresse schicken. Es ist jedoch effizienter diese Nachricht an eine Multicast Adresse zu schicken, also eine Gruppe von Rechner.
- Zeiger zur Weiterleitung: Wenn sich Entität von A nach B bewegt, wird im A einen Hinweis auf ihren neuen Ort hintergelassen. Wenn Entität lokaliert wurde, kann der Client einfach die Adresse nachschlagen indem er die Kette der weiterleitenden Zeiger nachgeht.
3. Wie funktionieren heimatgestutzte Ansätze?
- Unterstützt mobile Entitäten durch ein Heimatstandort, der den aktuellen Standort einer Entität keinnt. Der Heimatstandort ist meistens der Ort, wo die Entität entstand.
- Ein Client schickt dann eine Anfrage an den Heimatstandsort, um die derzeitige Position der Entität herauszufinden.
4. Was ist ein Namensraum?
- Ein Namensraum strukturiert Namen. Ein Namensraum ist meist streng hierarchisch, d.h. der Namensgraph hat die Struktur eines Baums.
- Ein Namensraum hat meistens nur einen Wurzelknoten. Ein Blattknoten verkörpert eine benannte Entität. Ein Verzeichnisknoten hat eine Anzahl an ausgehnder Kanten, die wiederum zu einem Blattknoten oder Verzeichnisknoten zeigen können.
- Namensräume bieten einen praktischen Mechanismus zum Speichern und Laden von Informationen über Entitäten nach dem Namen.
5. Wie funktioniert die hierarchische Namensauflösung?
- Der Vorgang, einen Namen nachzuschlagen, wird Namensauflösung genannt.
- Iterative Namensauflösung: Client hat einen Resolver der Anfragen an unterschiedliche Stationen mit dem Pfadnamen schickt. Jede Station löst den Pfadnamen soweit er kann und schickt dann dem Client die nächste Ansprechstation sowie der Rest des Pfadnamens.
- Rekursive Namensauflösung: Resolver bekommt nicht jedes Mal das Zwischenergebnis zurückgeschickt, sondern jeder Namensserver schickt das Ergebnis seiner Auflösung rekursiv an den nächsten Namensserver. Jeder Namensserver antwortet dann, bis der Resolver seine Auflösung bekommt.
