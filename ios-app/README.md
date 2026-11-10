# Objekt All-In Services · Native iOS-App für Lion

Diese App ist eine **SwiftUI-iPhone-App**, kein WebView. Enthalten: Login, Übersicht, Aufträge, Kunden, Termine, Aufgaben, Angebote, Rechnungen, Einnahmen, Fuhrpark/Geräte, Tätigkeitsprotokoll. Die Daten liegen in eurem eigenen Supabase-Projekt und werden für Silvan und Lion gemeinsam bereitgestellt.

## Stand

Die nativen Swift-Dateien und der XcodeGen-Projektplan sind fertig, die App ist **noch nicht signiert und nicht auf einem iPhone installiert**. Ein iPhone lässt keine Android-APK zu. Für TestFlight braucht ihr Mitgliedschaft im Apple Developer Program, eine Signierungsidentität und eine App in App Store Connect. Auch ein Mac mit Xcode oder ein entsprechend konfigurierter Build-Dienst ist erforderlich.

## Bauen

1. Xcode 16+ und XcodeGen auf macOS installieren (`brew install xcodegen`).
2. Im Ordner `ios-app`: `xcodegen generate`.
3. `ObjektAllIn.xcodeproj` öffnen, bei *Signing & Capabilities* das eigene Developer-Team auswählen und einen eindeutigen Bundle Identifier festlegen.
4. Zunächst Simulator testen. Für das iPhone von Lion einen signierten Build über TestFlight verteilen. Nur Lion einladen.
5. Supabase-Setup laut `../backend/README.md` durchführen und in der App Project URL sowie **Publishable Key** eintragen.

## Was noch fehlt

- Die vorhandene Android-v1.0-App hat nur lokale Daten. Für einen gemeinsamen Stand benötigt Silvan eine aktualisierte Android-Version mit Cloud-Zugang (im Android-Projekt ist die Anbindung vorbereitet).
- Bestandsaufträge/Fotos der Offline-Android-App werden nicht automatisch übertragen.
- Kein ungetesteter produktiver Einsatz mit echten Kundendaten. Vorher DSGVO/AVV, Aufbewahrung und Sicherheitsprüfung vornehmen.
- Kein Foto-Upload in der iOS-Cloud-Version. Fotoverwaltung kann in einer nächsten Ausbaustufe über geschützten Supabase Storage erfolgen.

## Datenschutz

Der öffentliche Supabase-Key darf im Client stehen; `service_role` und persönliche Zugangsdaten dürfen niemals in Quellcode oder öffentliche GitHub-Repositories. Auth-Session wird im iOS Keychain gespeichert. Datenbankzugriffe sind ausschließlich für freigeschaltete Mitglieder erlaubt.
