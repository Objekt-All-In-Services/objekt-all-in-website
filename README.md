# Objekt All-In Services · Native Firmen-Apps

Eigenständiges Projekt für die GbR, mit getrennter Anmeldung für **Silvan** und **Lion**, gemeinsamer Datenbank und unveränderbarem Änderungsprotokoll.

| Bereich | Status |
|---|---|
| Android | Die bisherige lokale App bleibt unverändert; die neue Team-App besitzt eine eigene App-Kennung und einen nativen Cloud-Arbeitsraum. Eine neue APK wird gebaut. |
| iOS | Native SwiftUI-App mit Login, Übersicht, Aufträgen, Aufgaben, Kunden, Terminen, Finanzübersichten und Audit-Ansicht. Quellcode vorhanden; noch kein signierter iPhone-Build. |
| Synchronisation | Supabase SQL-Datenbank und Zugriffsschutz implementiert; **noch nicht eingerichtet oder produktiv verbunden**. |
| Änderungsprotokoll | PostgreSQL-Trigger protokolliert Login-Benutzer, Zeit, Vorher-/Nachher-Daten. App zeigt zuletzt geänderte Einträge. |
| Benutzerrollen | Silvan: `admin`, Lion: `manager`; beim Backend-Setup mit echten UUIDs freizuschalten. |

## Start

1. `backend/README.md` für die Einrichtung der zentralen Datenbank lesen.
2. `ios-app/README.md` für iPhone/TestFlight lesen.
3. Android-Projekt `android-app` mit Android Studio oder dem Workflow `Android APK erstellen` bauen.
4. Mit zwei getrennten Testkonten anmelden. Einen Test-Auftrag auf Android erstellen, auf iPhone aktualisieren, verändern, und unter **Änderungen** den jeweiligen Bearbeiter prüfen.

## Nicht als produktionsbereit ausgeben

Ohne Supabase-Projekt, Benutzerfreigaben und iOS-Code-Signierung existiert **noch keine** funktionierende Zwei-Handy-Synchronisation. Die Android-v1.0-APK bleibt zunächst offline und kann nicht mit dem iPhone synchronisieren. Die Android-v1.1-Cloud-Ansicht verwaltet eigene Online-Einträge; bestehende Offline-Daten werden nicht automatisch migriert.

## Sicherheit

Unveröffentlichte Kundendaten nur in einer geschützten Datenbank speichern. Keine Passwörter/Secret Keys in den Quellcode. Vor produktivem Einsatz DSGVO/AVV und Sicherungskonzept prüfen; erst auf zwei realen Geräten testen.
