package app.filemanager.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import app.filemanager.AppActivity
import app.filemanager.service.rpc.startRpcServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BackgroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val notificationId = 1
    private val channelId = "background_service_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundServiceCompat()

        serviceScope.launch {
            startRpcServer()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.coroutineContext[Job]?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "文件管理器后台服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持文件管理器在后台运行"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceCompat() {
        val notification = createNotification()

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+ (API 34+) - 需要指定前台服务类型
                startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10+ (API 29+) - 支持前台服务类型
                startForeground(notificationId, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            }

            else -> {
                // Android 9 及以下 - 标准前台服务
                startForeground(notificationId, notification)
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, AppActivity::class.java)

        val pendingIntentFlags = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }

            else -> {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ (API 26+) - 需要通知渠道
            NotificationCompat.Builder(this, channelId)
                .setContentTitle("文件管理器")
                .setContentText("正在后台运行")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()
        } else {
            // Android 7.1 及以下 - 旧版通知
            NotificationCompat.Builder(this)
                .setContentTitle("文件管理器")
                .setContentText("正在后台运行")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }
    }

    companion object {
        private const val FOREGROUND_SERVICE_TYPE_DATA_SYNC = 1

        fun start(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    // Android 8.0+ (API 26+) - 必须使用 startForegroundService
                    try {
                        context.startForegroundService(intent)
                    } catch (e: Exception) {
                        // 如果启动前台服务失败，尝试启动普通服务
                        try {
                            context.startService(intent)
                        } catch (e2: Exception) {
                            // 忽略启动失败
                        }
                    }
                }

                else -> {
                    // Android 7.1 及以下 - 使用普通服务启动
                    try {
                        context.startService(intent)
                    } catch (e: Exception) {
                        // 忽略启动失败
                    }
                }
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackgroundService::class.java)
            context.stopService(intent)
        }
    }
}