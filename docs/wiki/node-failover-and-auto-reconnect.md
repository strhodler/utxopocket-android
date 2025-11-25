# Node failover and auto-reconnect

UtxoPocket rotates Electrum endpoints when a connection fails so you stay online without leaking usage or degrading privacy guarantees.

## Policies

- `Prefer private, then public` (default): try private nodes first; if none are healthy, try public presets.
- `Prefer public, then private`: inverse preference.
- `Private only`: rotate only among your private nodes.
- `Public only`: rotate only among the bundled presets.

Unavailable options are disabled per network (e.g., no private nodes means private policies are off until you add one).

## Auto-reconnect and health tracking

- Auto-reconnect is off by default. When you enable it, the app records the last 21 connection outcomes per node (timestamp, success/failure, latency, Tor usage) on-device only. Nothing is sent externally.
- Settings are per network (mainnet/testnets): policy and auto-reconnect can differ between networks and are persisted separately.
- Jittered backoff per node on consecutive failures: 1s → 2s → 4s → 8s → 16s → 30s, then capped; after 8 failures the node enters a 5-minute cooldown. Backoff is skipped if auto-reconnect is disabled.
- If every candidate is cooling down, the wallet shows an offline/backoff message instead of looping retries.
- Clearing history wipes these per-node events and resets backoff state.

## Node selection flow (by policy)

1) Build the candidate list from the active network:
   - Public presets (Tor-only) and custom nodes scoped to that network.
   - The currently selected node is tried first, then the rest in order.
2) Apply policy filters (`only` vs `prefer`), falling back to the other class if the preferred class is empty.
3) Skip nodes in backoff (unless auto-reconnect is disabled, in which case backoff is ignored).
4) Attempt connection; on success, record latency; on failure, record the error, increment the failure streak, and advance backoff. Public rotation is allowed only when auto-reconnect is on.

## UI surface

- Node screen: toggle auto-reconnect per network; when on, a compact selector shows the current policy. Switch disables public toggles in “Private only” and private toggles in “Public only”.
- Node detail screen: shows last success/failure, streak, backoff timer, recent events (up to 21), and lets you activate/test/edit/delete (private only) plus clear that node’s history.
- Settings → Clear connection history: wipes stored node events for the active network.

## Privacy and security posture

- All health data stays local; hostnames are already user-visible in the UI and are not transmitted elsewhere.
- Tor-only transport remains enforced for presets and onion customs; policies never downgrade to clearnet.
- If no nodes match the policy, the app surfaces “no nodes” instead of silently switching to a weaker option.

## Ops/QA notes

- Test chains: mainnet prefers private-first; testnets can be set to public-first if no private nodes exist.
- Regression checks: chained failures (public→public, private→private, private→public), backoff respect, history clear, and Tor-only enforcement.
