# DragoBank

DragoBank ist ein eigenständiges Wirtschaftssystem für Minecraft Paper 26.3, entwickelt als Bewerbungsprojekt.

## Funktionen

- Permanente Speicherung mittels SQLite
- Automatische Kontoerstellung
- UUID-basierte Spielerkonten
- Überweisungen zwischen Spielern
- Moneydrop mit gleichmäßiger Geldverteilung und Restbetragberechnung
- Administratorbefehle
- Berechtigungssystem
- Tab-Completion
- Konfigurierbare Währung und Startguthaben
- Eingabevalidierung und Schutz vor Zahlenüberläufen
- Datenbanktransaktionen mit Rollback

## Spielerbefehle

| Befehl | Beschreibung |
|---|---|
| `/bank` | Eigenen Kontostand anzeigen |
| `/balance` | Eigenen Kontostand anzeigen |
| `/bal` | Kurzform von /balance |
| `/bank balance` | Eigenen Kontostand anzeigen |
| `/pay <Spieler> <Betrag>` | Geld überweisen |
| `/moneydrop <Betrag>` | Geld gleichmäßig verteilen |
| `/bank help` | Befehlsübersicht anzeigen |

Überweisungen und Moneydrops können alternativ über `/bank pay` und `/bank moneydrop` ausgeführt werden.

## Administratorbefehle

| Befehl | Beschreibung |
|---|---|
| `/bank balance <Spieler>` | Fremden Kontostand anzeigen |
| `/bank add <Spieler> <Betrag>` | Geld hinzufügen |
| `/bank remove <Spieler> <Betrag>` | Geld entfernen |
| `/bank set <Spieler> <Betrag>` | Kontostand festlegen |
| `/bank reload` | Konfiguration neu laden |

## Technologien

- Java 25
- Paper API
- SQLite
- JDBC
- Gradle
- JUnit 5

## Installation

1. Plugin kompilieren (`./gradlew build`).
2. Die erstellte JAR aus `build/libs` in den Plugins-Ordner des Servers kopieren.
3. Paper-Server starten.
4. Bei Bedarf die generierte config.yml anpassen.

Es werden keine zusätzlichen Minecraft-Plugins benötigt.

## Tests

Das Plugin wurde auf einem lokalen Paper-26.3-Server getestet. Zusätzlich enthält das Projekt automatisierte Tests für die Geldlogik und Datenbankoperationen.

Die automatisierten Tests können über `./gradlew test` ausgeführt werden.

## Projektstatus

Version 1.0.0

Das Projekt dient der Demonstration grundlegender Kenntnisse in der Java- und Minecraft-Plugin-Entwicklung. Die Datenbankzugriffe erfolgen synchron und sind für den überschaubaren Umfang dieses Projekts ausgelegt.