# Gemeinsame, sichere Datenbank

Die native iPhone-App und die optionale Android-Cloud-Ansicht verwenden dieselben `records`-Datensätze. Identitäten werden **nicht** über frei wählbare Namen zugewiesen. Zwei individuelle Auth-Konten sind vorgesehen: **Silvan (Admin)** und **Lion (Mitverwalter)**.

## Einmalige Einrichtung

1. Eigenes Supabase-Projekt erstellen und EU-Region auswählen; Unternehmensdaten/Datenschutz prüfen.
2. Im Projekt SQL Editor `001_shared_workspace.sql` ausführen.
3. Unter **Authentication → Providers / Settings** öffentliche Registrierungen abschalten. **Authentication → Users** zwei Benutzer anlegen/einladen, jedem ein eigenes Passwort geben. Keine Zugangsdaten miteinander teilen.
4. Benutzer-UUIDs aus der Auth-Benutzerübersicht kopieren und `002_members_example.sql` mit den echten UUIDs anpassen und ausführen.
5. In beiden Apps die **Project URL** und den **öffentlichen Publishable Key** (oder Legacy-`anon` Key) eintragen. Niemals `service_role`, Secret Key oder Datenbankpasswort in eine App eintragen.
6. Auf iPhone und Android mit separaten Benutzer-E-Mails einloggen. Nach Änderungen in der jeweils anderen App **Aktualisieren** nutzen; iOS aktualisiert zusätzlich beim Öffnen.

## Sicherheitsprinzip

- `public.company_members` enthält die **einzigen** erlaubten Nutzer-IDs. Ein beliebiges Supabase-Konto hat allein dadurch **keinen** Zugriff.
- RLS schützt Kundendaten serverseitig. Beide Mitglieder dürfen lesen/bearbeiten; nur Admin darf löschen.
- PostgreSQL-Trigger schreibt jeden neu angelegten, bearbeiteten oder gelöschten Datensatz in `audit_events` mit `actor_id` aus der **verifizierten** Sitzung und vorherigem/nachherigem Inhalt.
- Mitglieder können eigene Rollen und die Audit-Tabelle **nicht** über die App ändern.
- Vor produktivem Einsatz Test mit zwei Accounts: Einloggen, gemeinsamen Eintrag sehen, Änderung protokollieren, fremdes unberechtigtes Konto aussperren.
- Der aktuelle Android-v1.0-Offlinebestand wird **nicht** automatisch übertragen. Kein ungeprüfter Export privater Kundendaten; künftig ist ein expliziter Import mit Konfliktprüfung sinnvoll.
- Datenbank-Backups, Aufbewahrungsfristen, DSGVO, Berechtigung zum Zugriff auf Kundeninformationen und Datenverarbeitungsvertrag mit dem Provider vor produktivem Einsatz klären.

## Abgleich

Die App synchronisiert nur bei Netzverbindung. Ohne Backend und Einladungen ist **keine gemeinsame Nutzung aktiv**. Die native iOS-App ist ein Xcode-Projekt, keine installierbare iPhone-Datei. TestFlight benötigt Apple Developer Program und Signierung.
