package com.parth.accessibility.accessibility_listener

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.annotation.NonNull
import com.parth.accessibility.accessibility_listener.Utils.isAccessibilitySettingsOn
import io.flutter.FlutterInjector
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.FlutterEngineGroup
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.embedding.engine.dart.DartExecutor.DartEntrypoint
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


var sink: EventChannel.EventSink? = null

class AccessibilityListenerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    private lateinit var channel: MethodChannel
    private lateinit var overlayChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private lateinit var context: Context
    private lateinit var mActivity: Activity
    private var pendingResult: Result? = null
    val REQUEST_CODE_FOR_ACCESSIBILITY = 167
    private var handler: OverlayMethodCallHandler? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_FOR_ACCESSIBILITY) {
            when (resultCode) {
                FlutterActivity.RESULT_OK -> pendingResult?.success(true)
                FlutterActivity.RESULT_CANCELED -> pendingResult?.success(context.let { isAccessibilitySettingsOn(it) })
                else -> pendingResult?.success(false)
            }
            return true
        }
        return false
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "accessibility_listener")
        channel.setMethodCallHandler(this)

        context = flutterPluginBinding.applicationContext
        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "accessibility_listener_event")
        eventChannel.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                sink = events
            }

            override fun onCancel(arguments: Any?) {
                sink?.endOfStream()
                sink = null
            }
        })

        if (handler == null) {
            handler = OverlayMethodCallHandler(context)
        }
        MethodChannel(flutterPluginBinding.binaryMessenger, OverlayConstants.CHANNEL_TAG).setMethodCallHandler(handler)
        val messenger = BasicMessageChannel(flutterPluginBinding.binaryMessenger, OverlayConstants.MESSENGER_TAG, JSONMessageCodec.INSTANCE)
        messenger.setMessageHandler(handler)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
        binding.addActivityResultListener(this)

        // Overlay
        FlutterEngineCache.getInstance().put(
            OverlayConstants.CACHED_TAG,
            FlutterEngineGroup(context).createAndRunEngine(context, DartEntrypoint(FlutterInjector.instance().flutterLoader().findAppBundlePath(), "overlaySecondary"))
        )
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onDetachedFromActivity() {
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        pendingResult = result
        when (call.method) {
            "isAccessibilityPermissionEnabled" -> result.success(isAccessibilitySettingsOn(context))
            "requestAccessibilityPermission" -> {
                Log.e("TAG", mActivity.toString())
                mActivity.startActivityForResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_CODE_FOR_ACCESSIBILITY)
            }
            "showOverlay" -> {
                val intent = Intent(context, OverlayService::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                context.startService(intent)
                result.success(null)
            }
            "isOverlayActive" -> result.success(OverlayService.isRunning)
            "closeOverlay" -> {
                if (OverlayService.isRunning) {
                    val i = Intent(context, OverlayService::class.java)
                    i.putExtra(OverlayService.INTENT_EXTRA_IS_CLOSE_WINDOW, true)
                    context.startService(i)
                    result.success(true)
                }
            }
            else -> result.notImplemented()
        }
    }
}
