import SwiftUI

private let navy = Color(red: 0.04, green: 0.13, blue: 0.24)
private let gold = Color(red: 0.82, green: 0.63, blue: 0.32)

struct RootView: View {
    @EnvironmentObject private var store: Workspace
    @Environment(\.scenePhase) private var phase
    var body: some View {
        Group {
            if store.isLoggedIn { MainTabs() }
            else { LoginView() }
        }
        .task { if store.isLoggedIn { await store.reload() } }
        .onChange(of: phase) { _, value in
            if value == .active && store.isLoggedIn { Task { await store.reload() } }
        }
    }
}

struct LoginView: View {
    @EnvironmentObject var store: Workspace
    @State private var projectURL = ""
    @State private var apiKey = ""
    @State private var email = ""
    @State private var password = ""
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 19) {
                    Image("BrandLogo")
                        .resizable().scaledToFit().frame(width: 172, height: 172)
                        .font(.system(size: 72)).foregroundStyle(gold).padding(.top, 25)
                    Text("OBJEKT ALL-IN SERVICES").font(.title2.weight(.heavy)).foregroundStyle(navy)
                    Text("Gemeinsame Auftragsverwaltung für Silvan und Lion")
                        .font(.subheadline).multilineTextAlignment(.center)
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Firmenzugang").font(.headline)
                        TextField("E-Mail", text: $email).keyboardType(.emailAddress)
                            .textContentType(.username).textInputAutocapitalization(.never)
                        SecureField("Persönliches Passwort", text: $password)
                            .textContentType(.password)
                        DisclosureGroup("Serververbindung (einmalig)") {
                            TextField("https://projekt.supabase.co", text: $projectURL)
                                .textInputAutocapitalization(.never).keyboardType(.URL)
                            TextField("Öffentlicher Supabase Publishable Key", text: $apiKey)
                                .textInputAutocapitalization(.never).autocorrectionDisabled()
                            Text("Nur den öffentlichen Publishable Key verwenden. Keine Admin- oder Service-Schlüssel.")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }.textFieldStyle(.roundedBorder).padding(20).background(.white, in: RoundedRectangle(cornerRadius: 16))
                    Button {
                        Task { await store.login(email: email, password: password, url: projectURL, key: apiKey) }
                    } label: {
                        HStack { if store.isBusy { ProgressView() }; Text("Sicher anmelden") }
                            .frame(maxWidth: .infinity).padding(12)
                    }
                    .buttonStyle(.borderedProminent).tint(navy)
                    .disabled(store.isBusy || email.isEmpty || password.isEmpty || projectURL.isEmpty || apiKey.isEmpty)
                    if let error = store.errorMessage {
                        Text(error).foregroundStyle(.red).font(.caption).multilineTextAlignment(.center)
                    }
                    Text("Nur für freigeschaltete Teammitglieder. Änderungen werden dokumentiert.")
                        .font(.caption).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }.padding(22)
            }.background(Color(.systemGroupedBackground))
                .onAppear { projectURL=store.api.projectURL; apiKey=store.api.publicKey }
        }
    }
}

struct MainTabs: View {
    var body: some View {
        TabView {
            NavigationStack { DashboardView() }
                .tabItem { Label("Übersicht", systemImage: "square.grid.2x2.fill") }
            NavigationStack { RecordListView() }
                .tabItem { Label("Aufträge", systemImage: "shippingbox.fill") }
            NavigationStack { ActivityView() }
                .tabItem { Label("Änderungen", systemImage: "clock.arrow.circlepath") }
            NavigationStack { SettingsView() }
                .tabItem { Label("Mehr", systemImage: "gearshape.fill") }
        }
    }
}

struct DashboardView: View {
    @EnvironmentObject var store: Workspace
    @State private var newRecord = false
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 19) {
                VStack(alignment: .leading, spacing: 9) {
                    Text("OBJEKT ALL-IN SERVICES").font(.caption.weight(.heavy)).foregroundStyle(gold)
                    Text("Alles im Blick.").font(.largeTitle.bold()).foregroundStyle(.white)
                    Text("Angemeldet als \(store.currentName)").foregroundStyle(.white.opacity(0.8))
                    Button("+ Neuen Auftrag eintragen") { newRecord = true }
                        .buttonStyle(.borderedProminent).tint(gold).foregroundStyle(navy)
                        .padding(.top, 7)
                }.frame(maxWidth: .infinity, alignment: .leading)
                    .padding(23).background(navy, in: RoundedRectangle(cornerRadius: 18))
                HStack(spacing: 12) {
                    MetricCard(name: "Aktive Aufträge", value: "\(store.openJobs())", icon: "shippingbox")
                    MetricCard(name: "Offene Aufgaben", value: "\(store.openTasks())", icon: "checkmark.circle")
                }
                MetricCard(name: "Offene Rechnungsbeträge", value: store.invoicesOpen().euros, icon: "eurosign.circle")
                HStack {
                    Text("Aktuelle Aufträge").font(.title3.bold())
                    Spacer()
                    Button("Aktualisieren") { Task { await store.reload() } }
                }
                ForEach(store.records.filter { $0.kind == "Auftrag" }.prefix(5)) { record in
                    NavigationLink { RecordDetailView(recordID: record.id) } label: {
                        RecordRow(record: record, assignee: store.memberName(record.assigned_to))
                    }.buttonStyle(.plain)
                }
                if store.records.filter({ $0.kind == "Auftrag" }).isEmpty {
                    ContentUnavailableView("Noch keine Aufträge", systemImage: "tray", description: Text("Lege den ersten Auftrag an."))
                }
            }.padding()
        }.background(Color(.systemGroupedBackground)).navigationTitle("Übersicht")
            .sheet(isPresented: $newRecord) { NavigationStack { RecordEditor(kind: "Auftrag") } }
            .refreshable { await store.reload() }
    }
}

struct MetricCard: View {
    let name: String
    let value: String
    let icon: String
    var body: some View {
        VStack(alignment: .leading, spacing: 9) {
            Image(systemName: icon).foregroundStyle(gold).font(.title3)
            Text(value).font(.title2.bold()).foregroundStyle(navy).minimumScaleFactor(0.7).lineLimit(1)
            Text(name).font(.caption).foregroundStyle(.secondary)
        }.frame(maxWidth: .infinity, alignment: .leading).padding(16)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 15))
    }
}

struct RecordListView: View {
    @EnvironmentObject var store: Workspace
    @State private var selectedKind = "Auftrag"
    @State private var showingNew = false
    @State private var search = ""
    var body: some View {
        VStack(spacing: 0) {
            Picker("Bereich", selection: $selectedKind) {
                ForEach(recordKinds, id: \.self) { Text($0).tag($0) }
            }.pickerStyle(.menu).padding(.horizontal)
            List {
                ForEach(store.records.filter { record in
                    record.kind == selectedKind && (search.isEmpty || record.title.localizedCaseInsensitiveContains(search) || record.customer.localizedCaseInsensitiveContains(search))
                }) { record in
                    NavigationLink { RecordDetailView(recordID: record.id) } label: {
                        RecordRow(record: record, assignee: store.memberName(record.assigned_to))
                    }
                }
            }.listStyle(.insetGrouped)
                .refreshable { await store.reload() }
                .searchable(text: $search, prompt: "Auftrag oder Kunde suchen")
        }.navigationTitle(selectedKind == "Auftrag" ? "Aufträge" : selectedKind)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showingNew = true } label: { Image(systemName: "plus.circle.fill") }
                }
                ToolbarItem(placement: .topBarLeading) {
                    Button { Task { await store.reload() } } label: { Image(systemName: "arrow.clockwise") }
                }
            }
            .sheet(isPresented: $showingNew) { NavigationStack { RecordEditor(kind: selectedKind) } }
    }
}

struct RecordRow: View {
    let record: WorkRecord
    let assignee: String
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: record.kind == "Auftrag" ? "shippingbox.fill" : "doc.text")
                .foregroundStyle(gold).frame(width: 36)
            VStack(alignment: .leading, spacing: 3) {
                Text(record.title).font(.headline).foregroundStyle(navy)
                if !record.customer.isEmpty { Text(record.customer).font(.subheadline).foregroundStyle(.secondary) }
                Text("\(record.status) · \(assignee)").font(.caption).foregroundStyle(.secondary)
            }
            Spacer(minLength: 3)
            if record.amount_cents != 0 { Text(record.amount_cents.euros).font(.caption.weight(.bold)).foregroundStyle(navy) }
        }.padding(.vertical, 5)
    }
}

struct RecordDetailView: View {
    @EnvironmentObject var store: Workspace
    let recordID: UUID
    @State private var editing = false
    private var record: WorkRecord? { store.records.first(where: { $0.id == recordID }) }
    var body: some View {
        Group {
            if let record {
                List {
                    Section("Auftrag") {
                        LabeledContent("Status", value: record.status)
                        LabeledContent("Kunde", value: record.customer.isEmpty ? "–" : record.customer)
                        LabeledContent("Zuständig", value: store.memberName(record.assigned_to))
                        LabeledContent("Termin", value: record.due_date ?? "–")
                        LabeledContent("Anschrift", value: record.address.isEmpty ? "–" : record.address)
                        LabeledContent("Telefon", value: record.phone.isEmpty ? "–" : record.phone)
                    }
                    Section("Beschreibung") { Text(record.details.isEmpty ? "Keine Angaben" : record.details) }
                    Section("Beträge") {
                        LabeledContent("Betrag", value: record.amount_cents.euros)
                        LabeledContent("Bezahlt", value: record.paid_cents.euros)
                    }
                    Section("Nachvollziehbarkeit") {
                        LabeledContent("Erstellt von", value: store.memberName(record.created_by))
                        LabeledContent("Zuletzt bearbeitet", value: store.memberName(record.updated_by))
                        LabeledContent("Geändert am", value: store.formattedDate(record.updated_at))
                    }
                }.navigationTitle(record.title)
                    .toolbar { Button("Bearbeiten") { editing = true } }
                    .sheet(isPresented: $editing) { NavigationStack { RecordEditor(original: record) } }
            } else { ContentUnavailableView("Eintrag nicht gefunden", systemImage: "tray") }
        }
    }
}

struct RecordEditor: View {
    @EnvironmentObject var store: Workspace
    @Environment(\.dismiss) private var dismiss
    let original: WorkRecord?
    @State private var form: EditableRecord
    @State private var saving = false
    init(kind: String = "Auftrag", original: WorkRecord? = nil) {
        self.original = original
        _form = State(initialValue: EditableRecord(kind: kind, original: original))
    }
    var body: some View {
        Form {
            Section("Allgemein") {
                Picker("Bereich", selection: $form.kind) {
                    ForEach(recordKinds, id: \.self) { Text($0).tag($0) }
                }
                TextField("Titel *", text: $form.title)
                TextField("Kunde", text: $form.customer)
                Picker("Status", selection: $form.status) {
                    ForEach(["Offen", "Geplant", "In Arbeit", "Erledigt", "Abgeschlossen", "Bezahlt", "Storniert"], id: \.self) { Text($0).tag($0) }
                }
                Picker("Zuständig", selection: $form.assignee) {
                    Text("Nicht zugewiesen").tag("")
                    ForEach(store.members.filter(\.active)) { person in
                        Text(person.display_name).tag(person.user_id.uuidString.lowercased())
                    }
                }
            }
            Section("Einsatz & Kontakt") {
                TextField("Adresse", text: $form.address)
                TextField("Telefon", text: $form.phone).keyboardType(.phonePad)
                TextField("Termin (JJJJ-MM-TT)", text: $form.dueDate)
                    .keyboardType(.numbersAndPunctuation)
            }
            Section("Details") {
                TextEditor(text: $form.details).frame(minHeight: 110)
            }
            Section("Finanzen") {
                TextField("Betrag in €", text: $form.amount).keyboardType(.decimalPad)
                TextField("Bereits bezahlt in €", text: $form.paid).keyboardType(.decimalPad)
            }
            if let error = store.errorMessage { Section { Text(error).foregroundStyle(.red) } }
        }
        .navigationTitle(original == nil ? "Eintrag erstellen" : "Eintrag bearbeiten")
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button("Abbrechen") { dismiss() } }
            ToolbarItem(placement: .confirmationAction) {
                Button("Speichern") {
                    saving = true
                    Task {
                        let success = await store.save(form, id: original?.id)
                        saving = false
                        if success { dismiss() }
                    }
                }.disabled(form.title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || saving)
            }
        }
    }
}

struct ActivityView: View {
    @EnvironmentObject var store: Workspace
    var body: some View {
        List(store.events) { event in
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: event.action == "created" ? "plus.circle.fill" : event.action == "deleted" ? "trash.fill" : "pencil.circle.fill")
                    .foregroundStyle(event.action == "deleted" ? .red : gold)
                    .font(.title3)
                VStack(alignment: .leading, spacing: 4) {
                    Text(event.record_title).font(.headline)
                    Text("\(store.memberName(event.actor_id)) · \(event.action == "created" ? "erstellt" : event.action == "deleted" ? "gelöscht" : "geändert")")
                    Text("\(event.record_kind) · \(event.changed_at.replacingOccurrences(of: "T", with: " ").prefix(16))")
                        .font(.caption).foregroundStyle(.secondary)
                }
            }.padding(.vertical, 5)
        }.navigationTitle("Änderungsprotokoll")
            .refreshable { await store.reload() }
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button { Task { await store.reload() } } label: { Image(systemName: "arrow.clockwise") } } }
    }
}

struct SettingsView: View {
    @EnvironmentObject var store: Workspace
    var body: some View {
        List {
            Section("Team") {
                LabeledContent("Angemeldet als", value: store.currentName)
                LabeledContent("Rolle", value: store.currentRole == "admin" ? "Administrator" : "Mitverwalter")
                ForEach(store.members.filter(\.active)) { member in
                    HStack { Text(member.display_name); Spacer(); Text(member.role == "admin" ? "Admin" : "Mitverwalter").foregroundStyle(.secondary) }
                }
            }
            Section("Synchronisierung") {
                LabeledContent("Letzter Abgleich", value: store.lastSync?.formatted(date: .abbreviated, time: .shortened) ?? "Noch nicht")
                Button("Jetzt aktualisieren") { Task { await store.reload() } }
                Text("Online-Datenbank notwendig. Änderungen werden beim Speichern übertragen, andere Geräte laden neue Daten beim Aktualisieren.")
                    .font(.caption).foregroundStyle(.secondary)
            }
            Section("Sicherheit") {
                Text("Eigene Konten für Silvan und Lion. Jeder Datensatz erhält seine Bearbeiterkennung. Änderungen lassen sich im Protokoll verfolgen.")
                    .font(.caption)
                Button("Abmelden", role: .destructive) { store.logout() }
            }
        }.navigationTitle("Einstellungen")
    }
}
