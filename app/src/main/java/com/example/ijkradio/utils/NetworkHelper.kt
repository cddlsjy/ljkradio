package com.example.ijkradio.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * 网络状态监听器
 * 用于检测网络连接状态变化
 */
class NetworkHelper(private val context: Context) {

    /**
     * 网络状态监听接口
     */
    interface NetworkStateListener {
        fun onNetworkAvailable()
        fun onNetworkLost()
    }

    private var networkStateListener: NetworkStateListener? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * 设置网络状态监听器
     */
    fun setNetworkStateListener(listener: NetworkStateListener) {
        networkStateListener = listener
    }

    /**
     * 开始监听网络状态
     */
    fun startListening() {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                networkStateListener?.onNetworkAvailable()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                networkStateListener?.onNetworkLost()
            }

            override fun onUnavailable() {
                super.onUnavailable()
                networkStateListener?.onNetworkLost()
            }

            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                // 网络能力变化检测
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet) {
                    networkStateListener?.onNetworkAvailable()
                } else {
                    networkStateListener?.onNetworkLost()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            networkCallback?.let {
                connectivityManager?.registerNetworkCallback(networkRequest, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 停止监听网络状态
     */
    fun stopListening() {
        networkCallback?.let {
            try {
                connectivityManager?.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        networkCallback = null
        networkStateListener = null
    }

    /**
     * 检查是否有网络连接
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * 检查是否通过移动网络连接
     */
    fun isMobileNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    /**
     * 检查是否通过WiFi连接
     */
    fun isWifiNetwork(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * 获取当前网络类型描述
     */
    fun getNetworkTypeDescription(): String {
        return when {
            isWifiNetwork() -> "WiFi"
            isMobileNetwork() -> "移动网络"
            isNetworkAvailable() -> "其他网络"
            else -> "无网络"
        }
    }
}
