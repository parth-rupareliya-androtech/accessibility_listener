package com.parth.accessibility.accessibility_listener
//
//import android.app.Activity
//import android.content.Context
//import android.content.Intent
//import android.net.Uri
//import android.os.Build
//import android.provider.Settings
//import io.flutter.embedding.engine.FlutterEngineCache
//import io.flutter.plugin.common.BasicMessageChannel.MessageHandler
//import io.flutter.plugin.common.BasicMessageChannel
//import io.flutter.plugin.common.BasicMessageChannel.Reply
//import io.flutter.plugin.common.JSONMessageCodec
//import io.flutter.plugin.common.MethodCall
//import io.flutter.plugin.common.MethodChannel.MethodCallHandler
//import io.flutter.plugin.common.MethodChannel.Result
//
//class OverlayMethodCallHandler(context: Context) : MethodCallHandler, MessageHandler<Any> {
//    private var pendingResult: Result? = null
//    private var context: Context? = null
//
//    init {
//        this.context = context
//    }
//
//    private fun checkOverlayPermission(): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            Settings.canDrawOverlays(context)
//        } else true
//    }
//
//    override fun onMethodCall(call: MethodCall, result: Result) {
//        pendingResult = result
//        if (call.method.equals("showOverlay")) {
//            if (!checkOverlayPermission()) {
//                result.error("PERMISSION", "overlay permission is not enabled", null)
//                return
//            }
//            val intent = Intent(context, OverlayService::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//            context?.startService(intent)
//            result.success(null)
//        } else if (call.method.equals("isOverlayActive")) {
//            result.success(OverlayService.isRunning)
//            return
//        } else if (call.method.equals("closeOverlay")) {
//            if (OverlayService.isRunning) {
//                val i = Intent(context, OverlayService::class.java)
//                i.putExtra(OverlayService.INTENT_EXTRA_IS_CLOSE_WINDOW, true)
//                context?.startService(i)
//                result.success(true)
//            }
//            return
//        } else {
//            result.notImplemented()
//        }
//    }
//
//    override fun onMessage(message: Any?, reply: Reply<Any>) {
//        val overlayMessageChannel: BasicMessageChannel<Any>? = FlutterEngineCache.getInstance()[OverlayConstants.CACHED_TAG]?.let {
//            BasicMessageChannel(it.dartExecutor, OverlayConstants.MESSENGER_TAG, JSONMessageCodec.INSTANCE)
//        }
//        overlayMessageChannel?.send(message, reply)
//    }
//}