package com.codersth.mapx.common

/**
 *
 * @author zhanglei1
 * @date 2021/3/17-14:32
 * @since V1.0.0
 */
interface OnMapMarkerClickListener<T> {

    fun onMapMarkerClick(marker: ClusterMarker<T>)
}