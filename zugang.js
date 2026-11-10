"use strict";
(() => {
  const project = "https://pqydohrjsuoxnzdoykgs.supabase.co";
  const redirect = new URL("/zugang.html", window.location.origin).href;
  const notice = document.getElementById("notice");
  const passForm = document.getElementById("password-form");
  const resetSection = document.getElementById("reset-section");
  const successSection = document.getElementById("success-section");
  const passButton = document.getElementById("password-submit");
  const resetButton = document.getElementById("reset-submit");
  const publicKey = "sb_publishable_r6AKL_SLCbUI8Qw0U0GH8g_AUssip1T";
  const fragment = new URLSearchParams(window.location.hash.slice(1));
  const token = fragment.get("access_token");
  const tokenType = fragment.get("token_type");
  const kind = fragment.get("type");
  // The invite's bearer token must never remain visible in the address bar.
  if (window.location.hash) window.history.replaceState(null, "", window.location.pathname);
  function message(text, state) {
    notice.textContent = text;
    notice.className = "notice" + (state ? " " + state : "");
  }
  function showReset(text) {
    resetSection.hidden = false;
    passForm.hidden = true;
    message(text || "Für diese Adresse liegt noch kein gültiger Einladungslink vor.");
  }
  if (token && tokenType === "bearer" && ["invite","recovery","signup","magiclink"].includes(kind || "")) {
    passForm.hidden = false;
    message("Einladung erkannt. Lege jetzt dein persönliches Passwort fest.", "good");
  } else {
    showReset("Du hast den Einladungslink bereits geöffnet oder seine Sitzung ist abgelaufen? Fordere hier einen neuen Passwort-Link an.");
  }
  passForm.addEventListener("submit", async event => {
    event.preventDefault();
    const password = document.getElementById("password").value;
    const confirm = document.getElementById("confirm").value;
    if (password.length < 12) return message("Bitte verwende mindestens 12 Zeichen.", "error");
    if (password !== confirm) return message("Die Passwörter stimmen nicht überein.", "error");
    passButton.disabled = true;
    message("Passwort wird sicher gespeichert …");
    try {
      const response = await fetch(project + "/auth/v1/user", {
        method: "PUT", headers: {
          "apikey": publicKey, "Authorization": "Bearer " + token,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ password }), cache: "no-store", referrerPolicy: "no-referrer"
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(body.msg || body.message || body.error_description || "Link ungültig oder abgelaufen.");
      document.getElementById("password").value = "";
      document.getElementById("confirm").value = "";
      passForm.hidden = true;
      successSection.hidden = false;
      message("Fertig! Dein Zugang ist eingerichtet.", "good");
    } catch (error) {
      showReset("Das Speichern hat nicht geklappt: " + (error.message || "Unbekannter Fehler") + " Fordere unten einen neuen Link an.");
      notice.className = "notice error";
    } finally { passButton.disabled = false; }
  });
  document.getElementById("reset-form").addEventListener("submit", async event => {
    event.preventDefault();
    const email = document.getElementById("email").value.trim().toLowerCase();
    if (!email) return;
    resetButton.disabled = true;
    message("Passwort-Link wird angefordert …");
    try {
      const r = await fetch(project + "/auth/v1/recover?redirect_to=" + encodeURIComponent(redirect), {
        method: "POST",
        headers: { "apikey": publicKey, "Content-Type": "application/json" },
        body: JSON.stringify({ email }), cache: "no-store", referrerPolicy: "no-referrer"
      });
      if (!r.ok) {
        const data = await r.json().catch(() => ({}));
        if (r.status === 429) throw new Error("Das E-Mail-Limit wurde erreicht. Versuche es später erneut.");
        throw new Error(data.msg || data.message || "Der Link konnte nicht angefordert werden.");
      }
      message("Falls diese Adresse im System registriert ist, erhältst du eine E-Mail mit einem neuen Passwort-Link. Bitte auch den Spam-Ordner prüfen.", "good");
    } catch (error) {
      message(error.message || "Bitte später erneut versuchen.", "error");
    } finally { resetButton.disabled = false; }
  });
})();
