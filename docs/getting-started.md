# Getting Started With UtxoPocket

This guide walks new users through installing the Android app, importing a testnet wallet, and exploring the main privacy features without risking real funds.

## 1. Install the App
- Download the latest APK from the GitHub Releases page.
- Allow installs from unknown sources (Settings → Security → Install unknown apps).
- Launch UtxoPocket and grant the Tor notification/foreground service permissions when prompted.

## 2. Create a Testnet Wallet
If you do not already have a watch-only wallet:
1. Pick a testnet-friendly wallet (Sparrow, Specter, Mutiny, or another BDK-compatible tool) and switch it to **Testnet3**, **Testnet4**, or **Signet**.
2. Generate a new descriptor wallet (BIP-84 or BIP-86). Make sure the wallet is watch-only or that you keep signing keys on a separate device.
3. Fund it using a faucet such as:
   - https://mempool.space/testnet/faucet (Testnet3/Testnet4)
   - https://signetfaucet.com (Signet)
4. Wait for at least one confirmation so UtxoPocket can show transactions/UTXOs immediately.

## 3. Export Descriptors
Every watch-only import needs two descriptors: **receive** (external) and **change** (internal). Examples:
```
Receive: wpkh([abcd1234/84h/1h/0h]tpub.../0/*)
Change:  wpkh([abcd1234/84h/1h/0h]tpub.../1/*)
```
- If your wallet supports BIP-389 multipath descriptors (e.g., `wpkh(.../<0;1>/*)`), you can paste that single string.
- Ensure the descriptor contains no private keys (`xprv`, `tprv`, etc.). UtxoPocket rejects them by design.

## 4. Import Into UtxoPocket
1. Tap **Add wallet** on the home screen.
2. Select the target network (Mainnet/Testnet3/Testnet4/Signet) so the Electrum presets/stop-gap match.
3. Paste the receive descriptor, then the change descriptor (or the multipath descriptor if applicable).
4. Choose a color and label; toggle “Shared descriptors” on if another app also watches this wallet to increase the address gap.
5. Confirm. Tor will bootstrap, Electrum will sync, and you will land on the wallet detail screen once data is cached.

## 5. Explore The UI
- **Wallet list**: shows balance, transaction count, last sync time, and Tor/node status per wallet.
- **Wallet detail**: scroll through transactions, UTXOs, and the balance history chart. Tap a transaction/UTXO to inspect details or labels.
- **Health analytics**: enable Transaction, UTXO, and Wallet Health from Settings → Privacy & analysis to surface scores and badges.
- **Wiki & glossary**: open the in-app knowledge base (More → Wiki) for deeper explanations of descriptors, Tor usage, and coin control.

## 6. Tips & Troubleshooting
- If Tor is slow to start, leave the app in the foreground until the progress reaches 100%; renewing the identity (Settings → Tor → Renew) can help.
- Descriptor validation errors usually mean a missing wildcard (`*`) or a private key snippet—double-check the source wallet export.
- To test panic wipe safely, use only testnet wallets; the action deletes all local data and you will need to re-import descriptors afterward.

You are now ready to monitor real watch-only wallets on mainnet. Keep descriptors safe, rotate Tor identities as needed, and use the health indicators to spot privacy or inventory issues early.
