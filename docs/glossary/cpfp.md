---
id: cpfp
title: Child-Pays-for-Parent (CPFP)
summary: A technique where a high-fee child transaction pulls a low-fee parent into a block.
related: [rbf-cpfp-strategies]
keywords: [fees, package]
---

CPFP spends an unconfirmed output (often your own change) with a higher feerate so miners evaluate the package (parent+child) together. Use CPFP when RBF isn’t available and ensure the child keeps funds within the same descriptor bucket.

