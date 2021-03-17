package com.codersth.mapxsample

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.amap.api.maps.model.animation.AlphaAnimation
import com.amap.api.maps.model.animation.Animation
import com.codersth.mapx.common.ClusterMarker
import com.codersth.mapx.common.ClusterOverlay
import com.codersth.mapx.common.DisplayAreaLevel

/**
 *
 * @author zhanglei1
 * @date 2021/3/17-11:50
 * @since V1.0.0
 */
class AMapClusterOverlay(
    private val context: Context, private val map: AMap, private val marker: ClusterMarker<Marker>, private val defaultLevel: DisplayAreaLevel
): ClusterOverlay<Marker>(marker, defaultLevel), AMap.OnCameraChangeListener,
    AMap.OnMarkerClickListener {

    companion object {
        private const val TAG = "AMapClusterOverlay"

        /**
         * 默认缓存的点标记图片数量。当前所有标记点图片一样
         */
        private const val DEFAULT_MARKER_POINT_CACHE_SiZE = 5

        /**
         * 默认缓存的聚合标记图片数量
         */
        private const val DEFAULT_MARKER_CLUSTER_CACHE_SiZE = 100
        /**
         * 以标注点作为视角边界时，地图上的点与屏幕边距
         */
        private const val MARKER_PADDING_TO_SCREEN = 320
        /**
         * 行政区域与地图缩放比例对应关系
         */
        private val AREA_ZOOM_RELATIONS = mapOf(
            DisplayAreaLevel.Country to Pair(3F, 4F),
            DisplayAreaLevel.Province to Pair(4.1F, 5.9F),
            DisplayAreaLevel.City to Pair(6F, 7.5F),
            DisplayAreaLevel.County to Pair(7.6F, 10.9F),
            DisplayAreaLevel.Point to Pair(11F, 19F)
        )
    }

    init {
        map.setOnMarkerClickListener(this)
        map.setOnCameraChangeListener(this)
    }
    /**
     * 标记点图标（聚合情况）图像缓存，K为聚合标记上的文字和数量。
     * 默认最多会缓存80张图片作为聚合显示元素图片
     */
    private var mMarkerClusterBitmapLruCache: LruCache<Pair<String, Int>, BitmapDescriptor>? =
        object :
            LruCache<Pair<String, Int>, BitmapDescriptor>(DEFAULT_MARKER_CLUSTER_CACHE_SiZE) {
            override fun entryRemoved(
                evicted: Boolean,
                key: Pair<String, Int>,
                oldValue: BitmapDescriptor?,
                newValue: BitmapDescriptor?
            ) {
                // 释放图像内存
                oldValue?.bitmap?.also {
                    if (it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }

    /**
     * 标记点图标（点情况）图像缓存。
     * 默认最多会缓存5张图片作为聚合显示元素图片
     */
    private var mMarkerPointBitmapLruCache: LruCache<DisplayAreaLevel, BitmapDescriptor>? =
        object :
            LruCache<DisplayAreaLevel, BitmapDescriptor>(DEFAULT_MARKER_POINT_CACHE_SiZE) {
            override fun entryRemoved(
                evicted: Boolean,
                key: DisplayAreaLevel,
                oldValue: BitmapDescriptor?,
                newValue: BitmapDescriptor?
            ) {
                // 释放图像内存
                oldValue?.bitmap?.also {
                    if (it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }

    /**
     * 根据标记对象显示正确的图标
     */
    private fun getMarkerBitmap(markerItem: ClusterMarker<Marker>): BitmapDescriptor? {
        if (DisplayAreaLevel.Point == markerItem.level) {
            mMarkerPointBitmapLruCache?.get(DisplayAreaLevel.Point)?.also {
                return it
            }
            BitmapDescriptorFactory.fromView(getMarkerPointView(markerItem))?.also {
                mMarkerPointBitmapLruCache?.put(DisplayAreaLevel.Point, it)
                return it
            }
        } else {
            if (markerItem.children != null) {
                mMarkerClusterBitmapLruCache?.get(Pair(markerItem.text, markerItem.number))
                    ?.also {
                        return it
                    }
            }
            BitmapDescriptorFactory.fromView(getMarkerClusterView(markerItem)).also {
                if (markerItem.children != null) {
                    mMarkerClusterBitmapLruCache?.put(
                        Pair(
                            markerItem.text,
                            markerItem.number
                        ), it
                    )
                }
                return it
            }
        }
        return null
    }

    /**
     * 创建聚合标记视图
     */
    @SuppressLint("InflateParams")
    private fun getMarkerClusterView(markerItem: ClusterMarker<Marker>): View {
//                Logger.d(TAG, "getMarkerClusterView ${markerItem.number}")
        val markerView = LayoutInflater.from(context).inflate(
            R.layout.station_map_marker_cluster,
            null
        )
        markerView.findViewById<TextView>(R.id.text_tv).text = markerItem.text
        markerView.findViewById<TextView>(R.id.num_tv).text = "${markerItem.number}"
        return markerView
    }

    /**
     * 创建点标记视图
     */
    @SuppressLint("InflateParams")
    private fun getMarkerPointView(marker: ClusterMarker<Marker>): View {
        val markerView = LayoutInflater.from(context).inflate(
            R.layout.station_map_marker_point,
            null
        )
        return markerView
    }

    override fun updateMapCamera(list: List<ClusterMarker<Marker>>) {
        if (list.isEmpty()) {
            return
        }
        updateCameraWithBounds(list)
    }

    /**
     * 以标注点边界切换地图视角
     */
    private fun updateCameraWithBounds(list: List<ClusterMarker<Marker>>) {
        val first = list.first()
        AREA_ZOOM_RELATIONS[first.level]?.also { zoomRange ->
            Log.d(TAG, "zoomRange.lower ${zoomRange.first}")
            val boundsBuild = LatLngBounds.Builder()
            for (item in list) {
                boundsBuild.include(LatLng(item.lat, item.lng))
            }
            map.minZoomLevel = zoomRange.first
            map.maxZoomLevel = zoomRange.second
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    boundsBuild.build(),
                    MARKER_PADDING_TO_SCREEN
                )
            )
        }
    }

    override fun removeMarkerFromMap(markers: List<Marker>, complete: (Boolean) -> Unit) {
        val alphaAnimation = AlphaAnimation(1F, 0F)
        markers.forEach {
            alphaAnimation.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart() {

                }

                override fun onAnimationEnd() {
                    map.clear()
                    complete(true)
                }
            })
            it.setAnimation(alphaAnimation)
            it.startAnimation()
        }
    }

    override fun addMarkerOnMap(clusterMarker: ClusterMarker<Marker>, lat: Double, lng: Double): Marker {
        val markerOptions = MarkerOptions()
        markerOptions.anchor(0.5f, 0.5f)
            .icon(getMarkerBitmap(clusterMarker)).position(LatLng(clusterMarker.lat, clusterMarker.lng))
        val newMarker: Marker = map.addMarker(markerOptions)
        newMarker.setAnimation(AlphaAnimation(0f, 1f))
        newMarker.setObject(clusterMarker)
        newMarker.startAnimation()
        return newMarker
    }

    override fun onCameraChange(p0: CameraPosition?) {

    }

    override fun onCameraChangeFinish(p0: CameraPosition) {
        Log.d(TAG, "onCameraChangeFinish zoom = ${p0.zoom}")
        // 地图手势缩放后也会引起聚合
        adjustDisplayAreaLevel(p0.zoom)?.also {
            setDisplayAreaLevel(it)
        }
        map.minZoomLevel = 3F
        map.maxZoomLevel = 19F
    }

    /**
     * 根据地图缩放来确定显示的行政区域级别
     * @param zoom 地图绽放级别
     */
    private fun adjustDisplayAreaLevel(zoom: Float): DisplayAreaLevel? {
        AREA_ZOOM_RELATIONS.entries.forEach {
            if (zoom >= it.value.first && zoom <= it.value.second) {
                return it.key
            }
        }
        return null
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        (p0?.`object` as? ClusterMarker<Marker>)?.also {
            onClusterMarkerClick(it)
        }
        return true
    }

}