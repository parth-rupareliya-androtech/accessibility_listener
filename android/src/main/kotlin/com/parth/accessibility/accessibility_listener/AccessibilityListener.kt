package com.parth.accessibility.accessibility_listener

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityListener : AccessibilityService() {
    val rect = Rect()
    var blackList = ArrayList<String>()
    var density = 1.0f

    override fun onServiceConnected() {
        blackList.add("com.android.systemui:id/battery_level")
        blackList.add("com.android.systemui:id/status_bar_clock")
        density = resources.displayMetrics.density
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val parentNodeInfo = event.source ?: return
        val nodes: ArrayList<HashMap<String, String>> = ArrayList()
        getAllNodesWithSize(parentNodeInfo, nodes)
        sink?.success(nodes)
    }

    private fun getAllNodesWithSize(nodeInfo: AccessibilityNodeInfo?, nodes: ArrayList<HashMap<String, String>>) {
        if (nodeInfo == null) return
        nodeInfo.getBoundsInScreen(rect)
        if (rect.top > 0 && rect.left > 0 && rect.width() > 0 && rect.height() > 0)
            if (nodeInfo.text != null && nodeInfo.text.trim().isNotEmpty()) nodes.add(getBoundingPoints(rect, nodeInfo.text.trim().toString()))
        if (nodeInfo.childCount > 0)
            for (i in 0 until nodeInfo.childCount)
                getAllNodesWithSize(nodeInfo.getChild(i), nodes)
    }

    private fun getBoundingPoints(rect: Rect, text: String): HashMap<String, String> {
        val frame = HashMap<String, String>()
        frame["name"] = text
        frame["top"] = (rect.top / density).toInt().toString()
        frame["left"] = (rect.left / density).toInt().toString()
        frame["width"] = (rect.width() / density).toInt().toString()
        frame["height"] = (rect.height() / density).toInt().toString()
        return frame
    }

    override fun onInterrupt() {

    }
}