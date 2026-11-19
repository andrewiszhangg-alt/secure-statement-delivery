(function () {
  'use strict';

  var root = document.documentElement;

  // Theme toggle; the saved choice is applied before first paint by an inline script in <head>.
  var themeButton = document.getElementById('theme-toggle');
  if (themeButton) {
    themeButton.addEventListener('click', function () {
      var next = root.dataset.theme === 'dark' ? 'light' : 'dark';
      root.dataset.theme = next;
      try { localStorage.setItem('theme', next); } catch (e) { /* storage unavailable */ }
    });
  }
  try {
    if (!localStorage.getItem('theme')) {
      matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (event) {
        if (!localStorage.getItem('theme')) root.dataset.theme = event.matches ? 'dark' : 'light';
      });
    }
  } catch (e) { /* ignore */ }

  // Toasts
  var toasts = document.getElementById('toasts');
  function toast(message, kind) {
    if (!toasts || !message) return;
    var el = document.createElement('div');
    el.className = 'toast' + (kind ? ' toast--' + kind : '');
    el.textContent = message;
    toasts.appendChild(el);
    setTimeout(function () { el.remove(); }, 4500);
  }

  var flash = document.getElementById('flash');
  if (flash) {
    toast(flash.dataset.message, 'ok');
    toast(flash.dataset.error, 'error');
  }

  // Timestamps are rendered as ISO text on the server and shown in the viewer's locale here.
  document.querySelectorAll('time[datetime]').forEach(function (el) {
    var date = new Date(el.getAttribute('datetime'));
    if (!isNaN(date)) {
      el.textContent = date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    }
  });

  // Account picker (dropdown on desktop, bottom sheet on phones)
  var picker = document.getElementById('picker');
  if (picker) {
    var pickerButton = picker.querySelector('.picker__btn');
    var menu = picker.querySelector('.menu');
    var scrim = picker.querySelector('.scrim');
    var setPicker = function (open) {
      menu.hidden = !open;
      scrim.hidden = !open;
      pickerButton.setAttribute('aria-expanded', String(open));
    };
    pickerButton.addEventListener('click', function () {
      setPicker(menu.hidden);
    });
    scrim.addEventListener('click', function () { setPicker(false); });
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') setPicker(false);
    });
  }

  // Account dialog: creates an account; the customer only picks a type
  var dialog = document.getElementById('account-dialog');
  if (dialog) {
    var form = dialog.querySelector('form');
    var typeSelect = form.elements.accountTypeId;
    var fromInput = form.elements.from;

    var openDialog = function () {
      typeSelect.value = '';
      fromInput.value = document.body.dataset.page === 'statements' ? 'statements' : 'accounts';
      if (picker) {
        picker.querySelector('.menu').hidden = true;
        picker.querySelector('.scrim').hidden = true;
      }
      dialog.showModal();
      typeSelect.focus();
    };
    document.addEventListener('click', function (event) {
      if (event.target.closest('[data-account-dialog]')) openDialog();
    });
    dialog.querySelector('[data-close]').addEventListener('click', function () { dialog.close(); });
    dialog.addEventListener('click', function (event) {
      if (event.target === dialog) dialog.close();
    });
  }

  // Statement links: View is a plain link that redirects server-side; Copy puts a fresh presigned URL on the clipboard.
  async function fetchLink(url) {
    var response = await fetch(url, { headers: { Accept: 'application/json' } });
    var body = await response.json().catch(function () { return {}; });
    if (!response.ok) throw new Error(body.detail || 'Could not generate link');
    return body.presignedUrl;
  }

  async function copyLink(url) {
    if (window.ClipboardItem && navigator.clipboard && navigator.clipboard.write) {
      var blob = fetchLink(url).then(function (link) { return new Blob([link], { type: 'text/plain' }); });
      await navigator.clipboard.write([new ClipboardItem({ 'text/plain': blob })]);
    } else {
      await navigator.clipboard.writeText(await fetchLink(url));
    }
  }

  async function onCopy(button) {
    var label = button.textContent;
    button.classList.add('is-loading');
    try {
      await copyLink(button.dataset.url);
      var minutes = button.dataset.minutes;
      toast(minutes ? 'Link copied. It stays valid for ' + minutes + ' minutes.' : 'Link copied.', 'ok');
      button.textContent = 'Copied';
      setTimeout(function () { button.textContent = label; }, 1500);
    } catch (error) {
      toast(error.message || 'Request failed', 'error');
    } finally {
      button.classList.remove('is-loading');
    }
  }

  function copyText(value) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
      return navigator.clipboard.writeText(value);
    }
    var field = document.createElement('textarea');
    field.value = value;
    field.setAttribute('readonly', '');
    field.style.position = 'fixed';
    field.style.left = '-9999px';
    document.body.appendChild(field);
    field.select();
    var ok = document.execCommand('copy');
    field.remove();
    return ok ? Promise.resolve() : Promise.reject(new Error('Could not copy'));
  }

  function onCopyId(button) {
    if (button.dataset.copying === '1') return;
    var value = button.getAttribute('data-copy');
    if (!value) return;
    button.dataset.copying = '1';
    copyText(value).then(function () {
      toast('Copied', 'ok');
    }).catch(function () {
      toast('Could not copy the id', 'error');
    }).finally(function () {
      button.dataset.copying = '0';
    });
  }

  document.addEventListener('click', function (event) {
    var idButton = event.target.closest('.id-cell[data-copy]');
    if (idButton) {
      onCopyId(idButton);
      return;
    }
    var button = event.target.closest('button[data-action="copy"]');
    if (button) onCopy(button);
  });
})();
