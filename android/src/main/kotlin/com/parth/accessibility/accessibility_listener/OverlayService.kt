package com.parth.accessibility.accessibility_listener

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import io.flutter.embedding.android.FlutterTextureView
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngineCache
import java.util.*
import kotlin.math.abs

object OverlayConstants {
    const val CACHED_TAG = "CachedEngine"
    const val CHANNEL_TAG = "slayer/overlay_channel"
    const val MESSENGER_TAG = "slayer/overlay_messenger"
    const val CHANNEL_ID = "Overlay Channel"
    const val NOTIFICATION_ID = 4579
}

class OverlayService : Service(), View.OnTouchListener {
    private var windowManager: WindowManager? = null
    private var flutterView: FlutterView? = null
    private val mAnimationHandler = Handler()
    private var lastX = 0f
    private var lastY: Float = 0f
    private var lastYPosition = 0
    private var dragging = false
    private val MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER = 0.8f
    private val szWindow = Point()
    private var mTrayAnimationTimer: Timer? = null
    private var mTrayTimerTask: TrayAnimationTimerTask? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    override fun onDestroy() {
        Log.d("OverLay", "Destroying the overlay window service")
        isRunning = false
        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(OverlayConstants.NOTIFICATION_ID)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val isCloseWindow = intent.getBooleanExtra(INTENT_EXTRA_IS_CLOSE_WINDOW, false)
        if (isCloseWindow) {
            if (windowManager != null) {
                windowManager!!.removeView(flutterView)
                windowManager = null
                stopSelf()
            }
            isRunning = false
            return START_STICKY
        }
        if (windowManager != null) {
            windowManager!!.removeView(flutterView)
            windowManager = null
            stopSelf()
        }
        isRunning = true
        Log.d("onStartCommand", "Service started")
        val engine = FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]
        engine!!.lifecycleChannel.appIsResumed()
        flutterView = FlutterView(applicationContext, FlutterTextureView(applicationContext))
        flutterView!!.attachToFlutterEngine(FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]!!)
        flutterView!!.fitsSystemWindows = true
        flutterView!!.isFocusable = true
        flutterView!!.isFocusableInTouchMode = true
        flutterView!!.setBackgroundColor(Color.WHITE)
//        overlayMessageChannel.setMessageHandler { message: Any?, reply: BasicMessageChannel.Reply<Any?>? -> WindowSetup.messenger.send(message) }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val w = windowManager!!.defaultDisplay.width
        val h = windowManager!!.defaultDisplay.height
        szWindow[w] = h
        val params = WindowManager.LayoutParams(
            150,
            150,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.alpha = MAXIMUM_OPACITY_ALLOWED_FOR_S_AND_HIGHER
        }
        params.gravity = Gravity.CENTER or Gravity.LEFT
        flutterView!!.setOnTouchListener(this)
        windowManager!!.addView(flutterView, params)
        return START_STICKY
    }

    override fun onCreate() {
        createNotificationChannel()
        val notificationIntent = Intent(this, AccessibilityListenerPlugin::class.java)
        val pendingFlags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0, notificationIntent, pendingFlags
        )
        val notifyIcon = applicationContext.resources.getIdentifier(String.format("ic_%s", "launcher"), "mipmap", applicationContext.packageName)
        val notification: Notification = NotificationCompat.Builder(this, OverlayConstants.CHANNEL_ID)
            .setContentTitle("overlayTitle")
            .setContentText("overlayContent")
            .setSmallIcon(notifyIcon)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
        startForeground(OverlayConstants.NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                OverlayConstants.CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)!!
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        if (windowManager != null) {
            val params = flutterView!!.layoutParams as WindowManager.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragging = false
                    lastX = event.rawX
                    lastY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    Log.e("TAG", event.x.toString())
                    Log.e("TAG", event.y.toString())
                    val dx = event.rawX - lastX
                    val dy: Float = event.rawY - lastY
                    if (!dragging && dx * dx + dy * dy < 25) {
                        return false
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                    val xx = params.x + dx.toInt()
                    val yy = params.y + dy.toInt()
                    params.x = xx
                    params.y = yy
                    windowManager!!.updateViewLayout(flutterView, params)
                    dragging = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    lastYPosition = params.y
                    windowManager!!.updateViewLayout(flutterView, params)
                    mTrayTimerTask = TrayAnimationTimerTask()
                    mTrayAnimationTimer = Timer()
                    mTrayAnimationTimer!!.schedule(mTrayTimerTask, 0, 25)
                    return dragging
                }
                else -> return false
            }
            return false
        }
        return false
    }

    private inner class TrayAnimationTimerTask : TimerTask() {
        var mDestX = 0
        var mDestY: Int = lastYPosition
        var params = flutterView?.layoutParams as WindowManager.LayoutParams
        override fun run() {
            mAnimationHandler.post {
                params.x = 2 * (params.x - mDestX) / 3 + mDestX
                params.y = 2 * (params.y - mDestY) / 3 + mDestY
                windowManager?.updateViewLayout(flutterView, params)
                if (abs(params.x - mDestX) < 2 && abs(params.y - mDestY) < 2) {
                    cancel()
                    mTrayAnimationTimer?.cancel()
                }
            }
        }

        init {
            mDestX = if (params.x + flutterView!!.width / 2 <= szWindow.x / 2) 0 else szWindow.x - flutterView!!.width
        }
    }

    companion object {
        const val INTENT_EXTRA_IS_CLOSE_WINDOW = "IsCloseWindow"
        var isRunning = false
    }
}