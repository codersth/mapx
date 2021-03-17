package com.codersth.mapxsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.amap.api.maps.AMap
import com.amap.api.maps.MapView
import com.amap.api.maps.model.Marker
import com.codersth.mapx.common.ClusterMarker
import com.codersth.mapx.common.DisplayAreaLevel
import com.codersth.mapx.common.OnMapMarkerClickListener

class MainActivity : AppCompatActivity(), AMap.OnMapLoadedListener,
    OnMapMarkerClickListener<Marker> {

    companion object {
        private const val TAG = "MainActivity"
    }
    /**
     * 地图对象
     */
    private var mMap: AMap? = null
    private var mMapView: MapView? = null
    /**
     * 地图标记渲染逻辑层
     */
    private var mMapOverlay: AMapClusterOverlay? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMap()
        mMapView?.onCreate(savedInstanceState)
    }

    /**
     * 初始化地图对象
     */
    private fun initMap() {
        if (mMap == null) {
            mMapView = findViewById<MapView>(R.id.map_view)
            mMap = mMapView?.map
            mMap?.setOnMapLoadedListener(this)
        }
    }

    override fun onMapLoaded() {
        if (mMap == null) {
            Log.e(TAG, "Map has not been loaded.")
            return
        }
        mMapOverlay = AMapClusterOverlay(
            this,
            mMap!!,
            createTestData(),
            DisplayAreaLevel.Country
            /*DisplayAreaLevel.getLevel(data.record.totalLevel)*/
        )
        mMapOverlay?.markerClickListener = this
    }

    private fun createTestData(): ClusterMarker<Marker> {
        val marker1 = ClusterMarker<Marker>(null, DisplayAreaLevel.Country, 39.955634, 116.381511, "中国", 2)
        val marker1_1 = ClusterMarker<Marker>(null, DisplayAreaLevel.Province, 31.149899,121.476764 , "上海市", 2)
        marker1.children = arrayListOf(marker1_1)
        val marker1_1_1 = ClusterMarker<Marker>(null, DisplayAreaLevel.City, 31.149899, 121.476764, "市辖区", 2)
        marker1_1.children = arrayListOf(marker1_1_1)
        val marker1_1_1_1 = ClusterMarker<Marker>(null, DisplayAreaLevel.County, 31.221564, 121.5456, "浦东新区", 2)
        marker1_1_1.children = arrayListOf(marker1_1_1_1)
        val marker1_1_1_1_1 = ClusterMarker<Marker>(null, DisplayAreaLevel.Point, 31.210355, 121.57314, "新国际博览中心", 1)
        val marker1_1_1_1_2 = ClusterMarker<Marker>(null, DisplayAreaLevel.Point, 31.024008, 121.645607, "新场古镇", 1)
        marker1_1_1_1.children = arrayListOf(marker1_1_1_1_1, marker1_1_1_1_2)
        return marker1;
    }

    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
    }

    override fun onMapMarkerClick(marker: ClusterMarker<Marker>) {
        Log.d(TAG, "onMapMarkerClick marker = $marker")
    }
}