package com.doanlthtvdk.doanlthtvdk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo

private fun Context.isNetworkAvailable(): Boolean {
  return runCatching {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
    networkInfo?.isConnected == true
  }.getOrDefault(false)
}

class NetworkChangeReceiver : BroadcastReceiver() {
  var onNetworkChange: ((Boolean) -> Unit)? = null

  override fun onReceive(context: Context?, intent: Intent?) {
    context ?: return
    intent ?: return
    onNetworkChange?.invoke(context.isNetworkAvailable())
  }
}