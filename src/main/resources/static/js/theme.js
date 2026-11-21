/* Applies the saved appearance (accent, dark mode, currency symbol) on every page.
   Loaded in <head> so the theme is set before first paint (no flash). */
(function () {
    var settings = {};
    try { settings = JSON.parse(localStorage.getItem('finance-theme') || '{}') || {}; } catch (e) { settings = {}; }
    var root = document.documentElement;
    if (settings.accent) root.style.setProperty('--accent', settings.accent);
    if (settings.dark) root.setAttribute('data-theme', 'dark');
    window.FMTheme = {
        settings: settings,
        apply: function (s) {
            if (s.accent) root.style.setProperty('--accent', s.accent);
            if (s.dark) root.setAttribute('data-theme', 'dark'); else root.removeAttribute('data-theme');
            document.querySelectorAll('.cur').forEach(function (el) { el.textContent = s.currency || '₹'; });
        },
        save: function (s) { try { localStorage.setItem('finance-theme', JSON.stringify(s)); } catch (e) {} }
    };
    document.addEventListener('DOMContentLoaded', function () {
        var quickDialog = document.getElementById('quick-add-dialog');
        var quickButton = document.querySelector('.quick-add-fab');
        var quickLoaded = false;
        async function openQuickLog() {
            if (!quickDialog) return;
            quickDialog.showModal();
            var form = document.getElementById('quick-expense-form');
            var date = form && form.querySelector('[name="date"]');
            if (date && !date.value) date.value = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
            if (quickLoaded) { var existingAmount = form && form.querySelector('[name="amount"]'); if (existingAmount) existingAmount.focus(); return; }
            var category = document.getElementById('quick-category');
            var account = document.getElementById('quick-account');
            try {
                var response = await fetch('/api/quick-log/options', { headers: { Accept: 'application/json' } });
                if (!response.ok) throw new Error('Could not load quick-log options');
                var data = await response.json();
                data.categories.forEach(function (item) {
                    var option = document.createElement('option'); option.value = item.id; option.textContent = item.name; category.appendChild(option);
                });
                data.accounts.forEach(function (item) {
                    var option = document.createElement('option'); option.value = item.id; option.textContent = item.name; account.appendChild(option);
                });
                var suggestions = document.getElementById('merchant-suggestions');
                data.merchants.forEach(function (merchant) {
                    var option = document.createElement('option'); option.value = merchant; suggestions.appendChild(option);
                });
                quickLoaded = true;
                var amount = form.querySelector('[name="amount"]');
                if (amount) amount.focus();
            } catch (error) {
                var note = document.getElementById('quick-log-error');
                if (note) note.textContent = 'Could not load your categories. Please try again.';
            }
        }
        if (quickButton) quickButton.addEventListener('click', openQuickLog);
        var closeQuick = quickDialog && quickDialog.querySelector('.dialog-close');
        if (closeQuick) closeQuick.addEventListener('click', function () { quickDialog.close(); });
        document.addEventListener('keydown', function (event) {
            if (event.key.toLowerCase() !== 'n' || event.altKey || event.ctrlKey || event.metaKey || event.repeat) return;
            var target = event.target;
            if (target && (target.isContentEditable || /INPUT|TEXTAREA|SELECT/.test(target.tagName))) return;
            if (quickDialog && !quickDialog.open) { event.preventDefault(); openQuickLog(); }
        });
        document.querySelectorAll('.sidebar-theme-toggle').forEach(function (button) {
            var current = false;
            try { current = JSON.parse(localStorage.getItem('finance-theme') || '{}').dark === true; } catch (e) {}
            function sync() {
                button.setAttribute('aria-pressed', String(current));
                button.querySelector('.theme-toggle-icon').textContent = current ? '☾' : '☼';
            }
            sync();
            button.addEventListener('click', function () {
                current = !current;
                var next = Object.assign({}, window.FMTheme.settings, { dark: current });
                window.FMTheme.settings = next;
                window.FMTheme.apply(next);
                window.FMTheme.save(next);
                sync();
            });
        });
        if (settings.currency) {
            document.querySelectorAll('.cur').forEach(function (el) { el.textContent = settings.currency; });
        }
    });
})();
