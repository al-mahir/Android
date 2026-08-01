package com.iti.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.iti.domain.connectivity.ConnectivityObserver
import com.iti.domain.connectivity.ConnectivityStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private fun isOnline(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override val status: Flow<ConnectivityStatus> = callbackFlow {
        trySend(if (isOnline()) ConnectivityStatus.Available else ConnectivityStatus.Unavailable)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val ok = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                trySend(if (ok) ConnectivityStatus.Available else ConnectivityStatus.Unavailable)
            }
            override fun onLost(network: Network) {
                trySend(ConnectivityStatus.Unavailable)
            }
            override fun onUnavailable() {
                trySend(ConnectivityStatus.Unavailable)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)

        awaitClose { cm.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    override fun currentStatus(): ConnectivityStatus =
        if (isOnline()) ConnectivityStatus.Available else ConnectivityStatus.Unavailable
}
