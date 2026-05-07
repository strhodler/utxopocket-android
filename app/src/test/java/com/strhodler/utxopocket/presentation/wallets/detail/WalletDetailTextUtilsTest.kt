package com.strhodler.utxopocket.presentation.wallets.detail

import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.WalletTransactionSort
import com.strhodler.utxopocket.domain.model.WalletUtxoSort
import kotlin.test.Test
import kotlin.test.assertEquals

class WalletDetailTextUtilsTest {

    @Test
    fun ellipsizeMiddle_keepsShortValueUntouched() {
        assertEquals("abc", ellipsizeMiddle("abc"))
    }

    @Test
    fun ellipsizeMiddle_shortensLongValueWithHeadAndTail() {
        val value = "1234567890abcdef"

        assertEquals("1234...def", ellipsizeMiddle(value, head = 4, tail = 3))
    }

    @Test
    fun walletTransactionSort_labelRes_mapsAllValues() {
        assertEquals(
            R.string.wallet_detail_transactions_sort_newest_first,
            WalletTransactionSort.NEWEST_FIRST.labelRes()
        )
        assertEquals(
            R.string.wallet_detail_transactions_sort_oldest_first,
            WalletTransactionSort.OLDEST_FIRST.labelRes()
        )
        assertEquals(
            R.string.wallet_detail_transactions_sort_highest_amount,
            WalletTransactionSort.HIGHEST_AMOUNT.labelRes()
        )
        assertEquals(
            R.string.wallet_detail_transactions_sort_lowest_amount,
            WalletTransactionSort.LOWEST_AMOUNT.labelRes()
        )
    }

    @Test
    fun walletUtxoSort_labelRes_mapsAllValues() {
        assertEquals(
            R.string.wallet_detail_transactions_sort_highest_amount,
            WalletUtxoSort.LARGEST_AMOUNT.labelRes()
        )
        assertEquals(
            R.string.wallet_detail_transactions_sort_lowest_amount,
            WalletUtxoSort.SMALLEST_AMOUNT.labelRes()
        )
        assertEquals(
            R.string.wallet_detail_transactions_sort_newest_first,
            WalletUtxoSort.NEWEST_FIRST.labelRes()
        )
        assertEquals(
            R.string.wallet_detail_transactions_sort_oldest_first,
            WalletUtxoSort.OLDEST_FIRST.labelRes()
        )
    }
}
