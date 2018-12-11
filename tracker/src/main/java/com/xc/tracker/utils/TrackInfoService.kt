package com.xc.tracker.utils

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET

/**
 * Created by cjh on 2018/11/27.
 */
interface TrackInfoService {
  @GET("appinterface/debris/getDebrisInfo")
  fun getInfo(): Observable<ResponseBody>
}