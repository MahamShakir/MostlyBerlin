/**
 * ============================================================================
 * withAuditLog HOC — TICKET-I124D (Day 9)
 * ============================================================================
 * WHAT:    Higher-order component (Component, label?) -> AuditLoggedComponent
 *          that logs "[audit] <name> mounted" once and "[audit] <name>
 *          render" on every render. Preserves displayName for React DevTools
 *          and forwards all props unchanged.
 * HOW:     Function-in / function-out. Uses a useRef flag to log mount
 *          exactly once; the console.log at the top of the render function
 *          fires on every render.
 * WHY:     Day-8 AM taught the HOC pattern as `function: Component ->
 *          Component`; wrapping <TradeTable> with it on Day 9 PM is the
 *          only project surface where students prove the pattern works.
 *
 *          The console output is also the debugging tool that shows
 *          whether TICKET-I124A's BreakContext is correctly *not*
 *          re-rendering the trade table on a resolve (the goal of that
 *          optimisation).
 * OBSERVE: React DevTools shows the wrapped component named
 *          `withAuditLog(TradeTable)`; resolving a break logs
 *          `[audit] ResolveBreakModal render` and `[audit] Navbar render`
 *          but NOT `[audit] TradeTable render`.
 * ============================================================================
 */
import { useEffect, useRef } from 'react';

export function withAuditLog(Component, label) {
    const name = label ?? Component.displayName ?? Component.name ?? 'Component';

    function AuditLoggedComponent(props) {
        const mounted = useRef(false);
        useEffect(() => {
            if (!mounted.current) {
                console.log(`[audit] ${name} mounted`);
                mounted.current = true;
            }
        }, []);
        console.log(`[audit] ${name} render`);
        return <Component {...props} />;
    }

    AuditLoggedComponent.displayName = `withAuditLog(${name})`;
    return AuditLoggedComponent;
}