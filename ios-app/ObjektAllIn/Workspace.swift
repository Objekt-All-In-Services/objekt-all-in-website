import Foundation
import SwiftUI

@MainActor
final class Workspace: ObservableObject {
    @Published var records: [WorkRecord] = []
    @Published var members: [Member] = []
    @Published var events: [AuditEvent] = []
    @Published var isLoggedIn: Bool
    @Published var isBusy = false
    @Published var errorMessage: String?
    @Published var lastSync: Date?

    let api = Backend()
    init() { isLoggedIn = api.signedIn }

    var currentName: String {
        members.first(where: { $0.user_id == api.userId })?.display_name ?? "Teammitglied"
    }
    var currentRole: String {
        members.first(where: { $0.user_id == api.userId })?.role ?? "manager"
    }
    func memberName(_ id: UUID?) -> String {
        guard let id else { return "Nicht zugewiesen" }
        return members.first(where: { $0.user_id == id })?.display_name ?? "Teammitglied"
    }
    func openJobs() -> Int {
        records.filter { $0.kind == "Auftrag" && !["Erledigt", "Abgeschlossen", "Storniert"].contains($0.status) }.count
    }
    func openTasks() -> Int { records.filter { $0.kind == "Aufgabe" && $0.status != "Erledigt" }.count }
    func invoicesOpen() -> Int64 {
        records.filter { $0.kind == "Rechnung" }.reduce(Int64(0)) { $0 + max(0, $1.amount_cents - $1.paid_cents) }
    }
    func formattedDate(_ s: String) -> String { String(s.prefix(10)) }

    func login(email: String, password: String, url: String, key: String) async {
        isBusy = true; defer { isBusy = false }
        do {
            try api.setConfig(url: url, key: key)
            try await api.login(email: email, password: password)
            try await loadAll()
            guard members.contains(where: { $0.user_id == api.userId && $0.active }) else {
                api.logout()
                throw AppError(message: "Dein Konto wurde noch nicht für die Firma freigeschaltet.")
            }
            isLoggedIn = true; errorMessage = nil
        } catch { errorMessage = error.localizedDescription }
    }
    func logout() {
        api.logout(); records=[]; events=[]; members=[]; isLoggedIn=false; errorMessage=nil
    }
    func reload() async {
        guard api.signedIn else { return }
        isBusy = true; defer { isBusy = false }
        do { try await loadAll(); errorMessage=nil }
        catch { errorMessage=error.localizedDescription }
    }
    private func loadAll() async throws {
        // Sequential statt unkontrollierter paralleler Refresh-Token-Rotationen.
        members=try await api.listMembers()
        guard members.contains(where: { $0.user_id == api.userId && $0.active }) else {
            throw AppError(message: "Kein aktiver Zugang für dieses Firmenkonto.")
        }
        records=try await api.listRecords()
        events=try await api.listAudit()
        lastSync=Date()
    }
    func save(_ form: EditableRecord, id: UUID?) async -> Bool {
        isBusy=true; defer { isBusy=false }
        do {
            try await api.save(form,id:id)
            try await loadAll()
            errorMessage=nil; return true
        } catch { errorMessage=error.localizedDescription; return false }
    }
}
