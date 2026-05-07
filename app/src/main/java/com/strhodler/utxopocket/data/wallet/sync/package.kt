/**
 * Ownership boundaries for wallet sync orchestration.
 *
 * - [WalletSyncOrchestrator] handles queueing, retry policy, and lifecycle coordination.
 * - [NodeSyncRunner] coordinates a single network refresh attempt end-to-end.
 * - [ElectrumSessionCoordinator] owns endpoint policy checks and Tor session envelopes.
 * - [WalletSyncEngine], [WalletSnapshotPersister], and [WalletChainSnapshotMapper] own
 *   per-wallet execution, persistence strategy, and BDK snapshot mapping.
 * - [NodeStatusPublisher] and [NetworkFailureRecorder] centralize status and sanitized
 *   network failure side effects.
 */
package com.strhodler.utxopocket.data.wallet.sync
