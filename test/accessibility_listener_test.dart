import 'package:flutter_test/flutter_test.dart';
import 'package:accessibility_listener/accessibility_listener.dart';
import 'package:accessibility_listener/accessibility_listener_platform_interface.dart';
import 'package:accessibility_listener/accessibility_listener_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockAccessibilityListenerPlatform
    with MockPlatformInterfaceMixin
    implements AccessibilityListenerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final AccessibilityListenerPlatform initialPlatform = AccessibilityListenerPlatform.instance;

  test('$MethodChannelAccessibilityListener is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelAccessibilityListener>());
  });

  test('getPlatformVersion', () async {
    AccessibilityListener accessibilityListenerPlugin = AccessibilityListener();
    MockAccessibilityListenerPlatform fakePlatform = MockAccessibilityListenerPlatform();
    AccessibilityListenerPlatform.instance = fakePlatform;

    expect(await accessibilityListenerPlugin.getPlatformVersion(), '42');
  });
}
