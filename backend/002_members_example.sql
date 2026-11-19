-- BEISPIEL. ECHTE Nutzer-UUIDs aus Supabase > Authentication > Users einsetzen.
-- Zunaechst zwei Benutzer per Dashboard-Einladung erstellen. Sign-ups deaktivieren.
-- Nicht in einer App mit einem Admin-API-Schluessel ausfuehren.

-- insert into public.company_members(user_id,display_name,role)
-- values
-- ('SILVAN_UUID_HIER', 'Silvan', 'admin'),
-- ('LION_UUID_HIER', 'Lion', 'manager');

-- select user_id, display_name, role, active from public.company_members;
