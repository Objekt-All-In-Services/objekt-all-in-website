# Objekt All-In Services – eigenständige Website

Komplett statische Website ohne STRATO-Editor, Baukasten, externe JavaScript-Pakete oder Abo-Software.

## Inhalt
- `index.html` Startseite, Leistungen, Über uns, Ablauf
- `kontakt.html` Kontaktseite mit Kontaktformular (öffnet das E-Mail-Programm, **kein** serverseitiger Versand)
- `impressum.html` Impressum
- `datenschutz.html` vor Veröffentlichung rechtlich prüfen (Cloudflare, Cookies, optionale Fremddienste)
- `styles.css` Farben, Schriften und responsives Design
- `script.js` Handy-Menü, Jahreszahl und Formular
- `favicon.svg` eigenes Seitensymbol
- `_headers` optionale Cloudflare Pages Sicherheits-Header

## Veröffentlichen über Cloudflare Pages
1. In deinem eigenen GitHub-Konto ein neues Repository erstellen, etwa `objekt-all-in-website`, und die Dateien ins Stammverzeichnis hochladen.
2. In Cloudflare auf **Workers & Pages**, danach **Create / Pages / Connect to Git** gehen, eigenes Repository auswählen.
3. Als Framework **None** auswählen. Build-Befehl leer lassen. Als Ausgabeordner `.` (Punkt) angeben.
4. Erst nach erfolgreichem Test die Domain `objekt-services.com` unter **Custom domains** verknüpfen. Bestehende E-Mail-DNS-Einträge (MX, SPF, DKIM, DMARC) nicht versehentlich löschen.
5. Danach genügt eine Änderung in GitHub: Cloudflare veröffentlicht sie bei aktivierter Git-Integration automatisch.

## Wichtig
- Dies ist ein eigenständiger erster Entwurf, keine pixelgenaue Kopie der Kollegenseite. Deren veröffentlichte URL war beim Erstellen nicht abrufbar.
- Das Kontaktformular öffnet eine vorbereitete E-Mail; es versendet noch keine Nachricht im Hintergrund. Für echtes Formular ohne E-Mail-Programm später Cloudflare Worker, Turnstile und E-Mail-Dienst integrieren.
- Impressum und Datenschutzerklärung bitte für die endgültige Hosting-/Formular-Konfiguration fachlich prüfen.
- Keine Mitarbeiter-/Bewerbungsseite eingebaut.