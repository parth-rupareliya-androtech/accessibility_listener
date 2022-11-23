import 'dart:developer';
import 'dart:io';

import 'package:flutter/services.dart';

class AccessibilityListener {
  final MethodChannel _methodeChannel = const MethodChannel("accessibility_listener");
  final EventChannel _eventChannel = const EventChannel("accessibility_listener_event");
  Stream? _stream;

  Stream get accessStream {
    if (Platform.isAndroid) {
      _stream ??= _eventChannel.receiveBroadcastStream();
      return _stream!;
    }
    throw Exception("Accessibility API exclusively available on Android!");
  }

  Future<bool> requestAccessibilityPermission() async {
    try {
      return await _methodeChannel.invokeMethod('requestAccessibilityPermission');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  Future<bool> isAccessibilityPermissionEnabled() async {
    try {
      return await _methodeChannel.invokeMethod('isAccessibilityPermissionEnabled');
    } on PlatformException catch (error) {
      log("$error");
      return false;
    }
  }

  final MethodChannel _overlayMethodeChannel = const MethodChannel("slayer/overlay_channel");

  Future<void> showOverlay() async {
    try {
      await _overlayMethodeChannel.invokeMethod("showOverlay");
    } on PlatformException catch (error) {
      print("$error");
    }
  }

  Future<bool> closeOverlay() async {
    try {
      return await _overlayMethodeChannel.invokeMethod("closeOverlay");
    } on PlatformException catch (error) {
      print("$error");
    }
    return false;
  }

  Future<bool> isActive() async {
    try {
      return await _overlayMethodeChannel.invokeMethod("isOverlayActive");
    } on PlatformException catch (error) {
      print("$error");
    }
    return false;
  }
}
