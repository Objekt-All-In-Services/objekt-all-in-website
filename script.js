(function () {
  const menuButton = document.querySelector('.menu-toggle');
  const menu = document.querySelector('.nav-links');
  if (menuButton && menu) {
    menuButton.addEventListener('click', () => {
      const isOpen = menu.classList.toggle('open');
      menuButton.setAttribute('aria-expanded', String(isOpen));
    });
    menu.querySelectorAll('a').forEach(a => a.addEventListener('click', () => {
      menu.classList.remove('open');
      menuButton.setAttribute('aria-expanded','false');
    }));
  }
  const year = document.querySelectorAll('[data-year]');
  year.forEach(el => el.textContent = new Date().getFullYear());
  const form = document.querySelector('#contact-form');
  if (form) {
    form.addEventListener('submit', event => {
      event.preventDefault();
      if (!form.reportValidity()) return;
      const values = new FormData(form);
      const name = (values.get('name') || '').toString().trim();
      const email = (values.get('email') || '').toString().trim();
      const phone = (values.get('phone') || '').toString().trim();
      const message = (values.get('message') || '').toString().trim();
      const subject = encodeURIComponent('Neue Anfrage über die Webseite von ' + name);
      const body = encodeURIComponent('Name: ' + name + '\nE-Mail: ' + email + '\nTelefon: ' + (phone || 'nicht angegeben') + '\n\nAnfrage:\n' + message);
      window.location.href = 'mailto:info@objekt-services.com?subject=' + subject + '&body=' + body;
      const note = document.querySelector('#form-note');
      if (note) note.textContent = 'Dein E-Mail-Programm sollte sich jetzt öffnen. Bitte die vorbereitete E-Mail dort noch absenden. Falls nichts passiert, schreibe direkt an info@objekt-services.com.';
    });
  }
})();