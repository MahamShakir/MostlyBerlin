// ============================================================================
// trades.js — TICKET-I093 + TICKET-I094 + TICKET-I098
// ============================================================================
// WHAT:    Fetches /api/v1/trades and renders the table.
// HOW:     fetch() + DOM querySelectors. NO frameworks — that's the point of Day 7.
// WHY:     Day 7 is the "feel the pain" sprint. Day 8 you replace it with React.
// OBSERVE: After this script runs, the <tbody> contains real trade rows.
// ============================================================================
// HINT: ONLY for this local dashboard, hard-code the viewer:viewer-pass creds.
//       In the real React app (Day 8+) we centralise auth in apiService.js
//       and load creds from a login form.
// ============================================================================

// TODO(TICKET-I093): set the base URL — default to localhost:8080 during dev.
const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("viewer:viewer-pass");
const PAGE_SIZE = 20;

// Module-level state — Day 8 is exactly what makes this approach painful.
let allTrades = [];
let sortKey = "tradeDate";
let sortDir = "desc";

document.addEventListener("DOMContentLoaded", () => {
    bindSortHandlers();
    loadTrades();
});

/**
 * TODO(TICKET-I093):
 *  - show loading <div>
 *  - fetch /api/v1/trades with Authorization header
 *  - on success: hide loading, save to allTrades, render
 *  - on error: hide loading, show error <div>
 *
 * HINT: use async/await for readability.
 */
let pageContent = [];

document.addEventListener("DOMContentLoaded", loadTrades);

async function loadTrades() {
    const loading  = document.getElementById("trades-loading");
    const errorDiv = document.getElementById("trades-error");
    loading.classList.remove("hidden");
    errorDiv.classList.add("hidden");

    try {
        const url = `${API_BASE}/trades?size=${PAGE_SIZE}`;
        const res = await fetch(url, { headers: { "Authorization": AUTH_HEADER } });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const page = await res.json();
        pageContent = page.content || page;
        render();
    } catch (e) {
        errorDiv.textContent = "Could not load trades: " + e.message;
        errorDiv.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

/**
 * TODO(TICKET-I094): build the click handler for each <th>.
 *  - same key clicked twice → toggle direction
 *  - render arrow ▲/▼
 */
function bindSortHandlers() {
    document.querySelectorAll("th[data-key]").forEach(th => {
        th.addEventListener("click", () => {
            const key = th.dataset.key;
            if (key === sortKey) sortDir = sortDir === "asc" ? "desc" : "asc";
            else { sortKey = key; sortDir = "asc"; }
            render();
        });
    });
}

function render() {
    const tbody = document.getElementById("trades-tbody");
    tbody.innerHTML = pageContent.map(rowHtml).join("");
}

function rowHtml(t) {
    const badgeClass = "badge-" + (t.status || "pending").toLowerCase();
    return `
        <tr>
            <td>${escapeHtml(t.tradeRef)}</td>
            <td>${t.instrumentId}</td>
            <td>${t.counterpartyId}</td>
            <td>${formatNumber(t.quantity)}</td>
            <td>${formatNumber(t.price)}</td>
            <td>${t.tradeDate}</td>
            <td><span class="badge ${badgeClass}">${t.status ?? "PENDING"}</span></td>
        </tr>
    `;
}

function formatNumber(n) {
    if (n == null) return "";
    const num = Number(n);
    return Number.isFinite(num) ? num.toLocaleString("en-GB", { maximumFractionDigits: 4 }) : String(n);
}

function escapeHtml(s) {
    return String(s ?? "").replace(/[&<>"']/g, c => ({
        "&": "&", "<": "<", ">": ">", "\"": """, "'": "'"
    }[c]));
}

function compareBy(key, dir) {
    const mul = dir === "asc" ? 1 : -1;
    return (a, b) => {
        if (a[key] < b[key]) return -1 * mul;
        if (a[key] > b[key]) return  1 * mul;
        return 0;
    };
}
