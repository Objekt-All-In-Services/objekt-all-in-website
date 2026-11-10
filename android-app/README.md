# Objekt All-In Services · Android

Eine **native Android-App in Java**, keine Web-App und keine WebView. Entwurf 1.0 für die interne Auftragsorganisation der Objekt All-In Services GbR.

## Funktionen

- Übersicht mit aktiven Aufträgen, offenen Aufgaben, Rechnungsständen und Einnahmen des Monats
- Aufträge: Kunde, Adresse, Telefonnummer, Termin, Betrag, Status, Zuständiger und Auftragsfotos
- Kundenstamm und Ansprechpartner
- Aufgaben und Termine
- Angebote, Rechnungen, Teilzahlungen und Einnahmenübersicht
- Fuhrpark und Geräte/Maschinen mit Wartungsterminen
- Zwei lokale Profilnamen: Silvan und Lion (keine Benutzer-Authentifizierung)
- Vollständige Offline-Datenspeicherung in SQLite
- Manuelle ZIP-Datensicherung und Wiederherstellung mit Fotos
- Original-Firmenlogo und blau-goldene Gestaltung

## Grenzen der ersten Version

Die Datenspeicherung ist **lokal auf dem jeweiligen Android-Gerät**. Eine automatische Synchronisierung zwischen den Smartphones von Silvan und Lion ist **noch nicht implementiert**; die Profilwahl ist kein Login. Für zentrale, rollenbasierte Zusammenarbeit ist eine sichere Backend-Anbindung nötig. Finanzbereiche sind organisatorische Übersichten, keine rechtsverbindliche Rechnungsstellung oder Buchhaltung. Datensicherung im ZIP-Format vor Gerätetausch durchführen.

## Mit Android Studio bauen

1. Android Studio öffnen (aktueller Android Gradle Plugin 8.7.x kompatibler Stand) und dieses Verzeichnis importieren.
2. Android SDK Platform 35 installieren; Java 17 einstellen.
3. Gradle sync und `app > Tasks > build > assembleDebug` ausführen.
4. Debug-APK in `app/build/outputs/apk/debug/app-debug.apk` auf Android ab Version 8 installieren.

Alternativ erstellt die GitHub Action `.github/workflows/android-apk.yml` eine Debug-APK und hinterlegt sie unter **Actions → Android APK erstellen → Artifacts**.

Für eine spätere Veröffentlichung im Play Store oder reguläre Verteilung wird ein eigenes Release-Signing und die Prüfung aktueller Play-Store-Anforderungen benötigt. Das automatische APK-Artefakt ist eine Debug-Version zum privaten Testen.

## Technik und Datenschutz

- Java, Android SDK, `SQLiteOpenHelper`, `ACTION_OPEN_DOCUMENT`, `ACTION_CREATE_DOCUMENT`
- Keine Zusatz-Abhängigkeiten, kein Online-Tracking, keine Internetberechtigung
- Bilder werden im app-internen Speicher komprimiert gespeichert und in ZIP-Sicherungen aufgenommen
- Keine ungefragte Netzwerkübertragung; weder Nutzerkonto noch Server notwendig
- Zugriff auf die App ist nicht zusätzlich per PIN geschützt; Android-Gerätesperre verwenden

© Objekt All-In Services GbR. Native Android-Projektversion 1.0.
