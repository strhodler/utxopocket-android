package com.strhodler.utxopocket.presentation.settings.logs

import com.strhodler.utxopocket.domain.model.NetworkEndpointType
import com.strhodler.utxopocket.domain.model.NetworkNodeSource
import com.strhodler.utxopocket.domain.model.NetworkTransport

fun NetworkEndpointType.displayName(): String =
    when (this) {
        NetworkEndpointType.Onion -> "onion"
        NetworkEndpointType.Clearnet -> "clearnet"
        NetworkEndpointType.Local -> "local"
        NetworkEndpointType.Preset -> "preset"
        NetworkEndpointType.Unknown -> "unknown"
    }

fun NetworkTransport.displayName(): String =
    when (this) {
        NetworkTransport.SSL -> "ssl"
        NetworkTransport.TCP -> "tcp"
        NetworkTransport.Unknown -> "unknown"
    }

fun NetworkNodeSource.displayName(): String =
    when (this) {
        NetworkNodeSource.Public -> "public"
        NetworkNodeSource.Custom -> "custom"
        NetworkNodeSource.None -> "none"
        NetworkNodeSource.Unknown -> "unknown"
    }
