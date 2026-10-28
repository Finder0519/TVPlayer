package cn.finder0519.tvplayer.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cn.finder0519.tvplayer.model.PlayRequest
import cn.finder0519.tvplayer.model.ProxyConfig
import cn.finder0519.tvplayer.model.ProxyType
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * 负责创建 ExoPlayer 并支持通过 http / socks5 代理播放。
 */
class PlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null

    private var currentUrl: String? = null
    private var currentProxy: ProxyConfig = ProxyConfig.NONE

    /** 播放加载失败时的回调（只会回调一次）。 */
    var onPlayerError: (() -> Unit)? = null

    private var errorNotified = false

    private val listener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            if (!errorNotified) {
                errorNotified = true
                onPlayerError?.invoke()
            }
        }
    }

    val exoPlayer: ExoPlayer?
        get() = player

    /**
     * 是否存在当前配置变更（URL 或代理）。
     */
    fun needsReset(req: PlayRequest): Boolean {
        return currentUrl != req.url || currentProxy != req.proxy
    }

    /**
     * 使用指定配置播放。若配置与当前一致则仅播放（恢复暂停）。\n     */
    fun play(req: PlayRequest) {
        val current = player
        if (current == null || needsReset(req)) {
            // 配置变化，重新构建播放器
            errorNotified = false
            val old = current
            if (old != null) {
                old.removeListener(listener)
                old.release()
            }
            currentUrl = req.url
            currentProxy = req.proxy
            val newPlayer = buildPlayer(req.url, req.proxy)
            newPlayer.addListener(listener)
            player = newPlayer
            newPlayer.setMediaItem(MediaItem.fromUri(req.url))
            newPlayer.prepare()
            newPlayer.playWhenReady = true
        } else {
            current.playWhenReady = true
            current.play()
        }
    }

    /**
     * 停止播放并释放资源。
     */
    fun stop() {
        player?.let {
            it.removeListener(listener)
            it.stop()
            it.release()
        }
        player = null
        currentUrl = null
        currentProxy = ProxyConfig.NONE
    }

    private fun buildPlayer(url: String, proxy: ProxyConfig): ExoPlayer {
        val client = when (proxy.type) {
            ProxyType.NONE -> OkHttpClient.Builder().build()
            else -> OkHttpClient.Builder()
                .proxy(
                    when (proxy.type) {
                        ProxyType.HTTP -> Proxy(Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port))
                        ProxyType.SOCKS5 -> Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxy.host, proxy.port))
                        ProxyType.NONE -> Proxy.NO_PROXY
                    }
                )
                .build()
        }
        val dataSourceFactory = OkHttpDataSource.Factory(client)
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSourceFactory))
            .build()
    }
}
