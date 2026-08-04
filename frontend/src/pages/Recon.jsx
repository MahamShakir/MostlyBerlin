/**
 * ============================================================================
 * Recon.jsx — TICKET-I107
 * ============================================================================
 * WHAT:    Recon-breaks page.
 * WHY:     Where Ops users actually resolve breaks.
 * ============================================================================
 */

 import { useState } from 'react';
 import StatusBadge from '../components/StatusBadge.jsx';
 import { useReconResults } from '../hooks/useReconResults.js';
 import { resolveBreak } from '../services/apiService.js';
 import { ResolveBreakModal } from '../components/ResolveBreakModal.jsx';

 export default function Recon() {
    const [filter, setFilter] = useState('OPEN');

    const {
        results,
        loading,
        error,
        refetch
    } = useReconResults(filter);
 
    const [modalBreak, setModalBreak] = useState(null);
 
    const handleConfirm = async (note) => {
        if (!modalBreak) throw new Error('no break selected');
        await resolveBreak(modalBreak.id, { note });
        await refetch();
    };
 
    return (
        <>
            <h1>Reconciliation Breaks</h1>

            <div className="filters">
                {['OPEN', 'RESOLVED', 'SUPPRESSED'].map(s => (
                    <button
                        key={s}
                        className={filter === s ? 'active' : ''}
                        onClick={() => setFilter(s)}
                    >
                        {s}
                    </button>
                ))}
            </div>

            {loading && <div className="loading">Loading…</div>}

            {error && <div className="error">{error.message}</div>}

            <table className="data-table">
                <thead>
                    <tr>
                        <th>Trade Ref</th>
                        <th>Discrepancy</th>
                        <th>Status</th>
                        <th>Detected</th>
                        <th>Action</th>
                    </tr>
                </thead>

                <tbody>
                    {results.map(r => {
                        const status = r.status;

                        const detected = r.detectedAt
                            ? new Date(r.detectedAt).toLocaleString('en-GB')
                            : '—';

                        return (
                            <tr key={r.id}>
                                <td>{r.tradeRef ?? r.tradeId ?? '—'}</td>
                                <td>{r.discrepancyType ?? '—'}</td>
                                <td>
                                    <StatusBadge status={status} />
                                </td>
                                <td>{detected}</td>
                                <td>
                                    {status === 'OPEN' && (
                                        <button onClick={() => setModalBreak(r)}>
                                            Resolve
                                        </button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>

            <ResolveBreakModal
                open={!!modalBreak}
                breakId={modalBreak?.id}
                onClose={() => setModalBreak(null)}
                onConfirm={handleConfirm}
            />
        </>
    );
 }
