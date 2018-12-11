package com.xc.tracker.utils

import android.app.Activity
import android.support.v4.app.Fragment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.xc.tracker.Tracker
import com.xc.tracker.data.TrackerEvent
import com.xc.tracker.data.TrackerMode
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.xc.tracker.data.ELEMENT_CONTENT
import com.xc.tracker.data.ELEMENT_TYPE
import com.xc.tracker.service.TrackerService

val PRETTY_GSON: Gson by lazy {
  GsonBuilder().setPrettyPrinting().create()
}
val GSON: Gson by lazy {
  Gson()
}

const val TAG = "AndroidTracker"

internal fun Activity?.getTrackTitle(): String = this?.title?.toString() ?: ""

internal fun Fragment.getTrackTitle(): String = activity?.getTrackTitle() ?: ""

@Suppress("UNUSED_PARAMETER")
internal fun View.getTrackProperties(ev: MotionEvent?): Map<String, Any> {
  // 首先获取元素本身的属性
  val properties = HashMap<String, Any>()
  properties[ELEMENT_TYPE] = this.javaClass.name
  if (this is TextView) {
    properties[ELEMENT_CONTENT] = this.text?.toString() ?: ""
  }

  // 然后获取开发者附加的属性
  val additionalProperties = Tracker.elementsProperties[this]
  additionalProperties?.filter { it.value != null }?.forEach {
    properties[it.key] = it.value!!
  }
  Tracker.elementsProperties.remove(this)
  return properties
}

/**
 * 对事件进行统计
 *
 * @param event 要统计的事件
 * @param background 当前事件是否为切换到后台
 * @param foreground 当前事件是否为切换到前台
 */
internal fun trackEvent(event: TrackerEvent, background: Boolean = false,
    foreground: Boolean = false) {
  // 此处尝试对数据进行上报
  // 具体的上报策略由TrackerService掌控
  TrackerService.report(event, background, foreground)
  // 打印日志
  log(event)
}

internal fun log(event: TrackerEvent) {
  if (Tracker.mode == TrackerMode.DEBUG_ONLY) {
    log(event.toPrettyJson())
  } else if (Tracker.mode == TrackerMode.DEBUG_TRACK) {
    log(event.toPrettyJson())
  }
}

private fun log(s: String) {
  Log.d(TAG, s)
}
