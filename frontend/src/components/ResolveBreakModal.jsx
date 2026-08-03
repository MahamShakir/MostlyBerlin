/**
 * ============================================================================
 * ResolveBreakModal — TICKET-I124C (Day 9)
 * ============================================================================
 * WHAT:    Accessible resolve-break dialog that replaces window.confirm.
 *          Native <div role="dialog">, focus trap, Esc-to-close,
 *          backdrop-click-to-close, resolution-note textarea with a 5-char
 *          minimum, and optimistic-update + rollback via BreakContext.
 * HOW:     dispatch({type:"RESOLVE"}) fires *before* the PUT request. If
 *          onConfirm rejects, dispatch({type:"REOPEN"}) rolls the badge
 *          back and the inline error toast surfaces the failure without
 *          closing the dialog.
 * WHY:     window.confirm blocks the entire JS event loop — incompatible
 *          with the Kafka-driven realtime feed, because incoming SSE
 *          messages queue behind the modal and arrive in a burst when the
 *          user finally clicks OK.
 *
 *          Optimistic-update + rollback is the pattern Day 10 demos: the
 *          badge decrements instantly, then rolls back if the backend
 *          returns 4xx — useReducer makes the rollback a single dispatch.
 * OBSERVE: Click Resolve -> dialog opens, focus jumps to the textarea, Esc
 *          closes it, backdrop click closes it. Stub the API to return 500
 *          -> badge decrements then increments back within 1s and the
 *          modal shows the inline error.
 * ============================================================================
 */
import { useEffect, useRef, useState } from 'react';
import { useBreaks } from '../context/BreakContext';

export function ResolveBreakModal({ open, breakId, onClose, onConfirm }) {
    const { dispatch } = useBreaks();
    const [reason, setReason] = useState('');
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState(null);
    const dialogRef = useRef(null);

    // Focus trap + Esc-to-close. Cleaned up on close/unmount.
    useEffect(() => {
        if (!open) return;
        const root = dialogRef.current;
        const focusables = root.querySelectorAll(
            'button, [href], input, textarea, [tabindex]:not([tabindex="-1"])'
        );
        const first = focusables[0];
        const last = focusables[focusables.length - 1];
        first?.focus();

        function onKey(e) {
            if (e.key === 'Escape') {
                onClose();
                return;
            }
            if (e.key === 'Tab') {
                if (e.shiftKey && document.activeElement === first) {
                    e.preventDefault();
                    last.focus();
                } else if (!e.shiftKey && document.activeElement === last) {
                    e.preventDefault();
                    first.focus();
                }
            }
        }
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [open, onClose]);

    if (!open) return null;

    async function submit() {
        setBusy(true);
        setError(null);
        dispatch({ type: 'RESOLVE' });  // optimistic decrement
        try {
            await onConfirm(reason);
            onClose();
        } catch (err) {
            dispatch({ type: 'REOPEN' });  // rollback on API failure
            setError(err.message ?? 'Resolve failed');
        } finally {
            setBusy(false);
        }
    }

    return (
        <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="rbm-title"
            className="modal-backdrop"
            ref={dialogRef}
            onClick={(e) => {
                if (e.target === e.currentTarget) onClose();
            }}
        >
            <div className="modal">
                <h2 id="rbm-title">Resolve break {breakId}</h2>
                <label>
                    Resolution reason
                    <textarea
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        rows={4}
                    />
                </label>
                {error && <p role="alert" className="modal-error">{error}</p>}
                <div className="modal-actions">
                    <button onClick={onClose} disabled={busy}>Cancel</button>
                    <button
                        onClick={submit}
                        disabled={busy || reason.trim().length < 5}
                    >
                        {busy ? 'Resolving…' : 'Confirm resolve'}
                    </button>
                </div>
            </div>
        </div>
    );
}