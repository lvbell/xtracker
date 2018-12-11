package com.xc.tracker.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import java.lang.Exception
import java.util.*

/**
 * Created by cjh on 2018/11/27.
 */
private var mUUID: String? = null
private val DEVICE_PARAMS = "device_params"
private val KEY_UUID = "uuid"

fun getDeviceId(context: Context): String? {
  var deviceId: String? = null
  try {
    if (ContextCompat.checkSelfPermission(context,
            Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
      // 如果有权限才获取设备id,防止崩溃
      val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
      // 执行到此处时，已有权限，忽略该警告
      deviceId = tm.deviceId
      if (deviceId == null) {
        // 如果获取不到设备号(平板电脑等没有电话服务的设备会出现该情况),则获取android id
        deviceId = android.provider.Settings.Secure.getString(context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID) + ""
      }
    }

    if (deviceId == null || deviceId == "") {
      // 此时获取UUID
      if (mUUID == null) {
        // 首先从SharedPreferences中获取
        val sp = context.applicationContext
            .getSharedPreferences(DEVICE_PARAMS, Context.MODE_PRIVATE)
        mUUID = sp.getString(KEY_UUID, null)
        if (mUUID == null) {
          // SharedPreferences中没有UUID，则生成一个UUID
          mUUID = UUID.randomUUID().toString()
          // 并保存
          sp.edit().putString(KEY_UUID, mUUID).apply()
        }
      }
      deviceId = mUUID
    }
  } catch (e: Exception) {
    e.printStackTrace()
  }
  return deviceId
}