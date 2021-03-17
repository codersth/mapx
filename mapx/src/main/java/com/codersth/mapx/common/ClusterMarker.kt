package com.codersth.mapx.common

/**
 * 聚合点结构
 * @see [ClusterOverlay]
 * @author zhanglei1
 * @date 2021/3/16-18:04
 * @since V1.0.0
 */
data class ClusterMarker<T>(
    var children: List<ClusterMarker<T>>?,
    var level: DisplayAreaLevel,
    var lat: Double,
    var lng: Double,
    var text: String,
    val number: Int
) {
    /**
     * 具体地图上的标记对象
     */
    var marker: T? = null
}
