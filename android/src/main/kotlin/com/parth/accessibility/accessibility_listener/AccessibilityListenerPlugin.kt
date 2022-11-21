package com.parth.accessibility.accessibility_listener

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.NonNull
import com.parth.accessibility.accessibility_listener.Utils.isAccessibilitySettingsOn
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry


var sink: EventChannel.EventSink? = null

class AccessibilityListenerPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private var mActivity: Activity? = null
    private val pendingResult: Result? = null
    val REQUEST_CODE_FOR_ACCESSIBILITY = 167

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
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
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "isAccessibilityPermissionEnabled" -> result.success(context?.let { isAccessibilitySettingsOn(it) })
            "requestAccessibilityPermission" -> mActivity?.startActivityForResult(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), REQUEST_CODE_FOR_ACCESSIBILITY)
//            "showOverlay" -> {
//                val intent = Intent(context, OverlayService::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                context.startService(intent)
//                result.success(null)
//            }
//            "isOverlayActive" -> result.success(OverlayService.isRunning)
//            "closeOverlay" -> {
//                if (OverlayService.isRunning) {
//                    val i = Intent(context, OverlayService::class.java)
//                    i.putExtra(OverlayService.INTENT_EXTRA_IS_CLOSE_WINDOW, true)
//                    context.startService(i)
//                    result.success(true)
//                }
//            }
            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE_FOR_ACCESSIBILITY) {
            when (resultCode) {
                FlutterActivity.RESULT_OK -> pendingResult?.success(true)
                FlutterActivity.RESULT_CANCELED -> pendingResult?.success(context?.let { isAccessibilitySettingsOn(it) })
                else -> pendingResult?.success(false)
            }
            return true
        }
        return false
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        mActivity = null
    }

    override fun onDetachedFromActivity() {
        mActivity = null
    }
}