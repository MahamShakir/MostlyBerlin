// ============================================================================
// addTrade.js — TICKET-I095 + TICKET-I096
// ============================================================================
// WHAT:    Handles the Add-Trade form: client validation + POST.
// WHY:     Showcases why ad-hoc DOM manipulation gets ugly fast — every
//          field has its own error <span> we must show/hide manually.
//          React Hook Form (Day 9) replaces all of this.
// ============================================================================

const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("trader:trader-pw");

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("trade-form").addEventListener("submit", onSubmit);
});

/**
 * TODO(TICKET-I095): client-side validation:
 *  - all fields required
 *  - quantity > 0, price > 0
 *  - tradeDate not in the future
 *  - show inline error spans, return false if invalid
 *
 * TODO(TICKET-I096): on valid, POST /api/v1/trades, show toast on success.
 */
function onSubmit(evt) {
    evt.preventDefault();
    const form = evt.target;
    const data = Object.fromEntries(new FormData(form).entries());
    if (!validate(data)) return;
    // I096 adds the POST here.
    console.log("Validated form data:", data);
}

function validate(data) {
    clearErrors();
    let ok = true;

    if (!data.tradeRef || !data.tradeRef.trim()) {
        setError("tradeRef", "required"); ok = false;
    }
    if (!data.instrumentId)   { setError("instrumentId",   "required"); ok = false; }
    if (!data.counterpartyId) { setError("counterpartyId", "required"); ok = false; }

    if (!data.quantity || Number(data.quantity) <= 0) {
        setError("quantity", "must be > 0"); ok = false;
    }
    if (!data.price || Number(data.price) <= 0) {
        setError("price", "must be > 0"); ok = false;
    }
    if (!data.tradeDate) {
        setError("tradeDate", "required"); ok = false;
    } else if (new Date(data.tradeDate) > new Date()) {
        setError("tradeDate", "must not be in the future"); ok = false;
    }

    return ok;
}

function clearErrors() {
    document.querySelectorAll(".field-error").forEach(s => s.textContent = "");
}

function setError(field, msg) {
    const el = document.querySelector(`.field-error[data-for="${field}"]`);
    if (el) el.textContent = msg;
}

function showToast(msg, isError = false) {
    const t = document.getElementById("form-feedback");
    t.textContent = msg;
    t.style.borderLeftColor = isError ? "#c62828" : "#003366";
    t.classList.remove("hidden");
}
