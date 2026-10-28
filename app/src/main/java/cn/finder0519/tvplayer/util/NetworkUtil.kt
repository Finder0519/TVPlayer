package cn.finder0519.tvplayer.util

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtil {

    /**
     * 获取用于局域网访问的 IPv4 地址（非回环、非链路本地）。
     */
    fun getLanIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address &&
                        !addr.isLoopbackAddress &&
                        !addr.isLinkLocalAddress &&
                        !addr.isAnyLocalAddress
                    ) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }
}
