import Foundation

struct Member: Codable, Identifiable, Hashable {
    let user_id: UUID
    let display_name: String
    let role: String
    let active: Bool
    var id: UUID { user_id }
}

struct WorkRecord: Codable, Identifiable, Hashable {
    let id: UUID
    var kind: String
    var title: String
    var details: String
    var customer: String
    var address: String
    var phone: String
    var status: String
    var amount_cents: Int64
    var paid_cents: Int64
    var due_date: String?
    var assigned_to: UUID?
    var created_by: UUID
    var updated_by: UUID
    var created_at: String
    var updated_at: String

    var amount: Double { Double(amount_cents) / 100 }
    var paid: Double { Double(paid_cents) / 100 }
}

struct AuditEvent: Codable, Identifiable {
    let id: Int64
    let record_id: UUID
    let record_title: String
    let record_kind: String
    let actor_id: UUID
    let action: String
    let changed_at: String
}

struct AuthSession: Codable {
    let access_token: String
    let refresh_token: String
    let expires_in: Int?
    let user: AuthUser?
}

struct AuthUser: Codable { let id: UUID; let email: String? }

struct EditableRecord {
    var kind = "Auftrag"
    var title = ""
    var details = ""
    var customer = ""
    var address = ""
    var phone = ""
    var status = "Offen"
    var amount = "0"
    var paid = "0"
    var dueDate = ""
    var assignee = ""

    init(kind: String = "Auftrag", original: WorkRecord? = nil) {
        self.kind = original?.kind ?? kind
        if let r = original {
            title = r.title; details = r.details; customer = r.customer
            address = r.address; phone = r.phone; status = r.status
            amount = String(format: "%.2f", r.amount)
            paid = String(format: "%.2f", r.paid)
            dueDate = r.due_date ?? ""
            assignee = r.assigned_to?.uuidString.lowercased() ?? ""
        }
    }

    func payload() -> [String: Any] {
        func cents(_ s: String) -> Int64 {
            let clean = s.replacingOccurrences(of: ",", with: ".")
            return Int64(((Double(clean) ?? 0) * 100).rounded())
        }
        var result: [String: Any] = [
            "kind": kind, "title": title.trimmingCharacters(in: .whitespacesAndNewlines),
            "details": details, "customer": customer, "address": address,
            "phone": phone, "status": status,
            "amount_cents": cents(amount), "paid_cents": cents(paid)
        ]
        result["due_date"] = dueDate.isEmpty ? NSNull() : dueDate
        result["assigned_to"] = assignee.isEmpty ? NSNull() : assignee
        return result
    }
}

let recordKinds = ["Auftrag", "Aufgabe", "Termin", "Kunde", "Angebot", "Rechnung", "Einnahme", "Fahrzeug", "Gerät"]

extension Int64 {
    var euros: String {
        (Double(self) / 100).formatted(.currency(code: "EUR").locale(Locale(identifier: "de_DE")))
    }
}

extension String {
    var shortDate: String {
        String(prefix(10))
    }
}
