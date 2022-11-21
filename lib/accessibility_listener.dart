import 'dart:developer';
import 'dart:io';

import 'package:flutter/services.dart';

class AccessibilityListener {
  AccessibilityListener._();

  static const MethodChannel _methodeChannel = MethodChannel("accessibility_listener");
  static const EventChannel _eventChannel = EventChannel("accessibility_listener_event");
  static Stream? _stream;

  static Stream get accessStream {
    if (Platform.isAndroid) {
      _stream ??= _eventChannel.receiveBroadcastStream();
      return _stream!;
    }
    throw Exception("Accessibility API exclusively available on Android!");
  }

  static Future<bool> requestAccessibilityPermission() async {
    try {
      return await _methodeChannel.invokeMethod('requestAccessibilityPermission');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  static Future<bool> isAccessibilityPermissionEnabled() async {
    try {
      return await _methodeChannel.invokeMethod('isAccessibilityPermissionEnabled');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }
}
