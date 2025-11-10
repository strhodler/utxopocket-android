package com.strhodler.utxopocket.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.ui.graphics.vector.ImageVector
import com.strhodler.utxopocket.R

sealed class MainDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val labelRes: Int
) {
    data object Wallets : MainDestination(
        route = "wallets",
        icon = Icons.Rounded.Wallet,
        labelRes = R.string.nav_wallets
    )

    data object Settings : MainDestination(
        route = "settings",
        icon = Icons.Rounded.Settings,
        labelRes = R.string.nav_settings
    )

    data object Wiki : MainDestination(
        route = "wiki",
        icon = Icons.Rounded.Book,
        labelRes = R.string.nav_wiki
    )

    data object More : MainDestination(
        route = "more",
        icon = Icons.Rounded.MoreHoriz,
        labelRes = R.string.nav_more
    )

    companion object {
        val BottomBarItems: List<MainDestination>
            get() = listOf(Wallets, Settings, More)
    }
}
