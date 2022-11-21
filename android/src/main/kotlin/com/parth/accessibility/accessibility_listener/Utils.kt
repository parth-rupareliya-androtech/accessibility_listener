package com.parth.accessibility.accessibility_listener

import android.content.Context
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.text.TextUtils.SimpleStringSplitter


object Utils {
    fun isAccessibilitySettingsOn(mContext: Context): Boolean {
        var accessibilityEnabled = 0
        val service = mContext.packageName + "/" + AccessibilityListener::class.java.canonicalName
        try {
            accessibilityEnabled = Settings.Secure.getInt(mContext.applicationContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (e: SettingNotFoundException) {
        }
        val mStringColonSplitter = SimpleStringSplitter(':')
        if (accessibilityEnabled == 1) {
            val settingValue = Settings.Secure.getString(mContext.applicationContext.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue)
                while (mStringColonSplitter.hasNext()) {
                    return mStringColonSplitter.next().equals(service, ignoreCase = true)
                }
            }
        }
        return false
    }
}