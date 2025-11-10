# How to Test UtxoPocket

This guide explains how to prepare your environment and start testing **UtxoPocket**, a *watch-only* Bitcoin wallet that lets you **monitor** balances, transactions, and UTXOs from a wallet descriptor.

---

## What is UtxoPocket?

UtxoPocket is a **watch-only wallet**:

* **It does not contain private keys.**
* **It cannot sign transactions.**
* **It only monitors**: addresses, balances, UTXOs, and transactions derived from BDK-compatible *descriptors* (these strings embed xpubs, fingerprints, and derivation paths).

> Its purpose is to observe and analyze wallets safely, without ever risking real funds.

---

## Environment Setup

Before using UtxoPocket, you need a Bitcoin wallet on **testnet**, since UtxoPocket can only monitor an existing wallet.

### 1. Install Sparrow Wallet

* Official repository: [https://github.com/sparrowwallet/sparrow](https://github.com/sparrowwallet/sparrow)
* Download the release for your OS and install it.

### 2. Create a Testnet Wallet

* Follow this step-by-step video:
  🎥 [How to create a testnet wallet with Sparrow](https://www.youtube.com/watch?v=7JJkLW4SHKQ)
* Make sure to use **testnet3** or **testnet4** — not mainnet.

### 3. Get Testnet Bitcoin (tBTC)

You can:

* Use faucets:
    - Testnet 3: https://coinfaucet.eu/en/btc-testnet/
    - Testnet 3: https://bitcoinfaucet.uo1.net/
    - Testnet 4: https://mempool.space/testnet4/faucet
    - Testnet 4: https://coinfaucet.eu/en/btc-testnet4/
* Or ask for testnet sats in the Telegram channel:
  - https://t.me/+vwfXIeMvuGxlNGQ0

> Testnet Bitcoin has **no real value**. It’s just for testing.

---

🧩 Quick Start with a Shared Test Descriptor
--------------------------------------------

To simplify testing, you can start by importing this **shared test descriptor** directly into UtxoPocket:

wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/<0;1>/*)#0hkam622

> ⚠️ This descriptor is for **testnet only** and contains no private information. It allows you to explore how UtxoPocket works right away, without creating your own wallet.

Steps:

1.  Copy the descriptor above.
2.  Open UtxoPocket → **Import Wallet → Testnet**.
3.  Paste the descriptor and confirm.
4.  You should see existing testnet transactions and balances appear.

> Once you’re familiar with it, you can replace this descriptor with your own (see next section).

---

## Security Rules

* **Never share your seed phrase or private keys.**
* Sharing *descriptors* or *xpubs* from **testnet** is fine for testing.
* On **mainnet**, **never share anything** related to your wallet — not descriptors, xpubs, screenshots, or addresses.
* If any app or website asks for your recovery phrase or private key, **stop immediately**.

> Remember: UtxoPocket is watch-only. It cannot spend coins. It only reads public data.

---

## Exporting a Descriptor from Sparrow (and importing it here)

You only need public data. These steps keep private keys offline.

1. **Open the wallet in Sparrow** (testnet or mainnet).  
2. Click the **Settings** tab and locate the **Descriptor** section.  
3. Press the **Edit** button to reveal the descriptor. Modern Sparrow builds surface a **single BIP‑389 multipath descriptor** that already includes both receive and change branches (e.g., `wpkh([8e8074b3/84h/1h/0h]tpub.../<0;1>/*)#0hkam622`). Copy that entire line.  
   - If you are on an older Sparrow release that still exposes separate Receive/Change fields, copy both strings.  
   - Regardless of version, make sure the descriptor ends with a checksum (`#abcd1234`) and contains a wildcard (`*`).  
4. **Move the descriptor to your test device securely** (QR, USB, or paste if both apps run on the same device). Never email or message mainnet descriptors.  
5. In UtxoPocket:  
   - Tap **Add wallet** → pick the correct network.  
   - Paste the receive + change descriptors (or the multipath descriptor).  
   - Toggle **Shared descriptors** on if another app also watches this wallet to keep the gap limit wide.  
6. Confirm and wait for Tor + Electrum sync.  
7. Compare balances/UTXO counts with Sparrow. If they differ, double-check that you pasted the correct network descriptors and that shared mode matches your other watchers.

> Tip: when testing on the same device, use Sparrows’s **QR** button to display the descriptor and scan it directly from UtxoPocket’s add-wallet screen.

---

## Reporting Bugs or Feedback

When you find a bug or unexpected behavior, please, post the report in the project’s Telegram channel or open a GitHub issue.

---

## Pre-Test Checklist

* [ ] Sparrow installed and testnet wallet created
* [ ] Descriptor ready
* [ ] Testnet Bitcoin (tBTC) received
* [ ] UtxoPocket installed
* [ ] Descriptor imported successfully
* [ ] Connected to Bitcoin testnet
* [ ] Everything running in testnet (never mainnet)

---

Stay safe, test thoroughly, and report clearly. Precision helps us improve UtxoPocket faster.
