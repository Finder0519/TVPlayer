package cn.finder0519.tvplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import cn.finder0519.tvplayer.TvPlayerController
import cn.finder0519.tvplayer.util.QrBitmap

@OptIn(ExperimentalTvMaterial3Api::class)
@UnstableApi
@Composable
fun MainScreen(controller: TvPlayerController) {
    val isPlaying by controller.isPlaying
    val qrUrl by controller.qrUrl
    val statusText by controller.statusText

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RectangleShape,
    ) {
        if (isPlaying) {
            PlayerScreen(controller)
        } else {
            HomeScreen(
                qrUrl = qrUrl,
                statusText = statusText,
            )
        }
    }
}

@Composable
private fun HomeScreen(qrUrl: String?, statusText: String) {
    val context = LocalContext.current
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = "TV Player",
                style = TvMaterialTheme.typography.headlineLarge,
            )
            if (qrUrl != null) {
                Text(
                    text = "请用手机扫描二维码打开控制页",
                    style = TvMaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                val bmp = QrBitmap.generate(qrUrl, 480)
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "控制页二维码",
                    modifier = Modifier.size(420.dp),
                )
                Text(
                    text = qrUrl,
                    style = TvMaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                )
            } else {
                Text(
                    text = "无法获取局域网 IP，请检查网络连接",
                    style = TvMaterialTheme.typography.bodyLarge,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                )
            }
            if (statusText.isNotEmpty()) {
                Text(
                    text = statusText,
                    style = TvMaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun PlayerScreen(controller: TvPlayerController) {
    // 播放页中按下系统「返回」键：直接销毁播放器并回到二维码主页
    BackHandler(enabled = true) {
        controller.backToHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = controller.playerManager.exoPlayer
                    useController = true
                    // 刚开始不自动显示播放控件，仅在用户操作后出现
                    controllerAutoShow = true
                    // 首次加载及播放过程中缓冲时均自动显示转圈动画
                    setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    // 播放页保持屏幕常亮，禁止系统休眠
                    keepScreenOn = true
                }
            },
            update = { view ->
                view.player = controller.playerManager.exoPlayer
            },
        )
    }
}
