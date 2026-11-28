// Refreshes the alert table every 15 seconds. The page works without this script; it only saves
// a reload. The container has aria-live="polite", so screen readers hear new alerts.
(function () {
  const box = document.getElementById('alerts');
  if (!box || !box.dataset.fragment) {
    return;
  }
  async function refresh() {
    try {
      const response = await fetch(box.dataset.fragment, { headers: { 'X-Requested-With': 'fetch' } });
      if (response.ok && !response.redirected) {
        const html = await response.text();
        if (html !== box.innerHTML) {
          box.innerHTML = html;
        }
      }
    } catch (e) {
      // Offline or the server restarted: keep the current table and try again next time.
    }
  }
  setInterval(refresh, 15000);
})();
