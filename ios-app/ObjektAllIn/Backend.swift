import Foundation
import Security

struct AppError: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}

struct SessionStore {
    private static let account = "objekt-all-in-supabase-session"
    static func save(_ session: AuthSession) {
        guard let data = try? JSONEncoder().encode(session) else { return }
        delete()
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "de.objektallin.services.ios",
            kSecAttrAccount as String: account,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
            kSecValueData as String: data
        ]
        SecItemAdd(query as CFDictionary, nil)
    }
    static func read() -> AuthSession? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "de.objektallin.services.ios",
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data else { return nil }
        return try? JSONDecoder().decode(AuthSession.self, from: data)
    }
    static func delete() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: "de.objektallin.services.ios",
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
    }
}

@MainActor
final class Backend {
    private let defaults = UserDefaults.standard
    var projectURL: String {
        get { defaults.string(forKey: "supabase_url") ?? "https://pqydohrjsuoxnzdoykgs.supabase.co" }
        set { defaults.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "supabase_url") }
    }
    var publicKey: String {
        get { defaults.string(forKey: "supabase_public_key") ?? "sb_publishable_r6AKL_SLCbUI8Qw0U0GH8g_AUssip1T" }
        set { defaults.set(newValue.trimmingCharacters(in: .whitespacesAndNewlines), forKey: "supabase_public_key") }
    }
    private(set) var session: AuthSession? = SessionStore.read()
    var signedIn: Bool { session != nil }
    var userId: UUID? { session?.user?.id }
    var configured: Bool { !projectURL.isEmpty && !publicKey.isEmpty }

    func setConfig(url: String, key: String) throws {
        guard let parts = URLComponents(string: url), parts.scheme == "https",
              let host = parts.host, !host.isEmpty, parts.user == nil, parts.password == nil,
              !key.isEmpty else { throw AppError(message: "Bitte gültige Supabase-HTTPS-URL und öffentlichen Schlüssel eintragen.") }
        projectURL = url.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        publicKey = key
    }

    private func rawRequest(path: String, method: String, body: Data?, token: String?) async throws -> (Data, Int) {
        guard configured, let url = URL(string: projectURL + path) else {
            throw AppError(message: "Supabase-Projekt noch nicht verbunden.")
        }
        var req = URLRequest(url: url)
        req.httpMethod = method
        req.setValue(publicKey, forHTTPHeaderField: "apikey")
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if let token { req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        if method == "POST" || method == "PATCH" { req.setValue("return=representation", forHTTPHeaderField: "Prefer") }
        req.httpBody = body
        req.timeoutInterval = 25
        let (data, response) = try await URLSession.shared.data(for: req)
        guard let http = response as? HTTPURLResponse else { throw AppError(message: "Ungültige Serverantwort") }
        return (data, http.statusCode)
    }

    func login(email: String, password: String) async throws {
        let body = try JSONSerialization.data(withJSONObject: ["email": email, "password": password])
        let (data, status) = try await rawRequest(path: "/auth/v1/token?grant_type=password", method: "POST", body: body, token: nil)
        guard (200..<300).contains(status) else { throw AppError(message: "Anmeldung fehlgeschlagen (\(status)). E-Mail und Passwort prüfen.") }
        let auth = try JSONDecoder().decode(AuthSession.self, from: data)
        session = auth; SessionStore.save(auth)
    }

    private func renew() async throws {
        guard let old = session else { throw AppError(message: "Bitte erneut anmelden.") }
        let body = try JSONSerialization.data(withJSONObject: ["refresh_token": old.refresh_token])
        let (data, code) = try await rawRequest(path: "/auth/v1/token?grant_type=refresh_token", method: "POST", body: body, token: nil)
        guard (200..<300).contains(code) else { logout(); throw AppError(message: "Sitzung abgelaufen. Bitte neu anmelden.") }
        var refreshed = try JSONDecoder().decode(AuthSession.self, from: data)
        // Die Auth-Antwort liefert normalerweise die User-ID. Alte ID dennoch erhalten, falls ausgelassen.
        if refreshed.user == nil {
            refreshed = AuthSession(access_token: refreshed.access_token, refresh_token: refreshed.refresh_token,
                                    expires_in: refreshed.expires_in, user: old.user)
        }
        session = refreshed; SessionStore.save(refreshed)
    }

    func request(path: String, method: String = "GET", payload: [String: Any]? = nil) async throws -> Data {
        guard let session else { throw AppError(message: "Nicht angemeldet") }
        let body = try payload.map { try JSONSerialization.data(withJSONObject: $0) }
        var (data, status) = try await rawRequest(path: path, method: method, body: body, token: session.access_token)
        if status == 401 {
            try await renew()
            (data, status) = try await rawRequest(path: path, method: method, body: body, token: self.session?.access_token)
        }
        if status == 403 { throw AppError(message: "Keine Berechtigung. Das Benutzerkonto muss erst als Firmenmitglied freigeschaltet werden.") }
        guard (200..<300).contains(status) else {
            let snippet = String(decoding: data, as: UTF8.self).prefix(240)
            throw AppError(message: "Serverfehler \(status): \(snippet)")
        }
        return data
    }

    func listRecords() async throws -> [WorkRecord] {
        let data = try await request(path: "/rest/v1/records?select=*&order=updated_at.desc&limit=300")
        return try JSONDecoder().decode([WorkRecord].self, from: data)
    }
    func listMembers() async throws -> [Member] {
        let data = try await request(path: "/rest/v1/company_members?select=user_id,display_name,role,active&order=display_name.asc")
        return try JSONDecoder().decode([Member].self, from: data)
    }
    func listAudit() async throws -> [AuditEvent] {
        let data = try await request(path: "/rest/v1/audit_events?select=id,record_id,record_title,record_kind,actor_id,action,changed_at&order=changed_at.desc&limit=150")
        return try JSONDecoder().decode([AuditEvent].self, from: data)
    }
    func save(_ form: EditableRecord, id: UUID?) async throws {
        if let id {
            _ = try await request(path: "/rest/v1/records?id=eq.\(id.uuidString.lowercased())", method: "PATCH", payload: form.payload())
        } else {
            _ = try await request(path: "/rest/v1/records", method: "POST", payload: form.payload())
        }
    }
    func logout() { session = nil; SessionStore.delete() }
}
