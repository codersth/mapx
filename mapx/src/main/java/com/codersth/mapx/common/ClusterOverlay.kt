package com.codersth.mapx.common

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.codersth.mapx.util.GenericHelper.asListOfType
import java.io.Serializable

/**
 *
 * 聚合处理抽象逻辑层，针对具体地图处理请继承此类。
 * @author zhanglei1
 * @date 2021/3/16-17:25
 * @since V1.0.0
 */
abstract class ClusterOverlay<T>(
    private val marker: ClusterMarker<T>,
    defaultLevel: DisplayAreaLevel
) {

    companion object {
        private const val TAG = "ClusterOverlay"
        private const val THREAD_NAME_CALCULATE = "calculate"
        private const val THREAD_NAME_DRAW = "draw"
        private const val CALCULATE_CLUSTER = 0x101
        private const val ADD_MARKER_LIST = 0x101


        /**
         * 默认缓存的聚合标记图片数量
         */
        private const val DEFAULT_MARKER_CLUSTER_CACHE_SiZE = 100

    }

    /**
     * 处理计算任务的线程。
     */
    private val mCalculateHandlerThread = HandlerThread(THREAD_NAME_CALCULATE)

    /**
     * 处理绘制任务的线程。
     */
    private val mDrawHandlerThread = HandlerThread(THREAD_NAME_DRAW)

    private var mCalculateHandler: Handler? = null

    private var mDrawHandler: DrawHandler? = null

    /**
     * 标识是否应该进行地图视角更新，比如手势绽放时无需进行。
     * 默认为true。
     */
    private var mNeedUpdateMapCamera = true

    /**
     * 记录当前点击的标注，转换地图视角时用
     */
    private var mClickedMarker: ClusterMarker<T>? = null

    var markerClickListener: OnMapMarkerClickListener<T>? = null
    //    private var mClusterRender: MapClusterRender? = null
    private var mDisplayAreaLevel: DisplayAreaLevel? = null
        set(value) {
            if (field != value) {
                assignClusters(value!!)
                field = value
            }
        }

    init {
        initHandlers()
        setDisplayAreaLevel(defaultLevel)
    }

    private fun initHandlers() {
        mCalculateHandlerThread.start()
        mDrawHandlerThread.start()
        mCalculateHandler = CalculateHandler(
            mCalculateHandlerThread.looper
        )
        mDrawHandler = DrawHandler(mDrawHandlerThread.looper)
    }

    private fun assignClusters(displayAreaLevel: DisplayAreaLevel) {
        // 如果显示级别为空，则不处理
        mCalculateHandler?.removeMessages(CALCULATE_CLUSTER)
        // 构造处理参数
        val message = Message.obtain()
        message.what = CALCULATE_CLUSTER
        message.obj = CalculateParam(this.marker, displayAreaLevel)
        mCalculateHandler?.sendMessage(message)
    }

    /**
     * 调整地图视角。
     * @param list 地图上显示的点列表
     */
    abstract fun updateMapCamera(list: List<ClusterMarker<T>>)

    /**
     * 将聚合点显示在地图对应的经纬度上，子类可以此方法中绑定[ClusterMarker]与各地图marker的关系，然后在标记点击时根据地图标记
     * 传入聚合数据。
     * @see [onClusterMarkerClick]
     */
    abstract fun addMarkerOnMap(clusterMarker: ClusterMarker<T>, lat: Double, lng: Double): T

    /**
     * 从地图上移除标记
     * @param complete 移除成功后的回调。
     */
    abstract fun removeMarkerFromMap(markers: List<T>, complete: ((Boolean) -> Unit))

    /**
     * 点击地图标记时调用。
     */
    protected fun onClusterMarkerClick(marker: ClusterMarker<T>) {
        // 降低显示级别
        // 如果当前是聚合点，则作聚合处理
        if (DisplayAreaLevel.Point != marker.level) {
            mClickedMarker = marker
            marker.children?.also {children ->
                mNeedUpdateMapCamera = true
                if (children.isNotEmpty()) {
                    setDisplayAreaLevel(children.first().level)
                }
            }
        } else {
            // 回调，处理点标记点击
            markerClickListener?.onMapMarkerClick(marker)
        }
    }

    protected fun setDisplayAreaLevel(displayAreaLevel: DisplayAreaLevel) {
        Log.d(
            TAG,
            "setDisplayAreaLevel displayAreaLevel = $displayAreaLevel mDisplayAreaLevelLiveData.value=${displayAreaLevel}"
        )
        mDisplayAreaLevel = displayAreaLevel
    }
    /**
     * 销毁，释放必要属性
     */
    fun destroy() {
//        mDisplayAreaLevelLiveData.removeObserver(mDisplayAreaLevelObserver)
        mDrawHandlerThread.quit()
        mDrawHandler?.removeCallbacksAndMessages(null)
        mCalculateHandlerThread.quit()
        mCalculateHandler?.removeCallbacksAndMessages(null)
    }

    /**
     * 耗时任务处理类
     */
    inner class CalculateHandler(looper: Looper) :
        Handler(
            looper
        ) {

        /**
         * 不同显示等级下的站点缓存
         */
        private val mMarkerCache = HashMap<DisplayAreaLevel, List<ClusterMarker<T>>>()

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                CALCULATE_CLUSTER -> {
                    (msg.obj as? CalculateParam<T>)?.also {
                        calculateClusters(it.marker, it.displayAreaLevel)
                    }
                }
            }
        }

        /**
         * 计算出聚合后的点
         */
        private fun calculateClusters(
            clusterMarker: ClusterMarker<T>,
            displayAreaLevel: DisplayAreaLevel
        ) {
            // 复制一份数据，规避同步
            var markerItemList = ArrayList(getMarkerInLevel(clusterMarker, displayAreaLevel))
            if (mNeedUpdateMapCamera) {
                // 如果当前点击了聚合点，则地图视角以子标注为基准；否则以全地图分散后的标注点为基准。
                if (mClickedMarker != null) {
                    mClickedMarker?.children?.also { childrenMarkers ->
                        updateMapCamera(childrenMarkers)
                        markerItemList = ArrayList(childrenMarkers)
                    }
                } else {
                    updateMapCamera(markerItemList)
                }
            }
            // 重置标识
            mNeedUpdateMapCamera = false
            // 发送任务到地图绘制处理线程
            val message = Message.obtain()
            message.what = ADD_MARKER_LIST
            message.obj = markerItemList
            mDrawHandler?.sendMessage(message)
        }

        private fun getMarkerInLevel(
            ClusterMarker: ClusterMarker<T>,
            displayAreaLevel: DisplayAreaLevel
        ): List<ClusterMarker<T>> {
            // 先从缓存中获取，为空则重新计算
            var childrenList = mMarkerCache[displayAreaLevel]
            if (childrenList != null) {
                return childrenList
            }
            childrenList = ArrayList<ClusterMarker<T>>()
            // TODO 可考虑其他数据结构
            val queue = ArrayList<ClusterMarker<T>>()
            queue.add(ClusterMarker)
            // 循环将对应显示级别的标记添加到列表
            while (queue.isNotEmpty()) {
                val currentItem = queue.removeAt(0)
                if (currentItem.level == displayAreaLevel) {
                    childrenList.add(currentItem)
                } else {
                    currentItem.children?.forEach {
                        queue.add(it)
                    }
                }
            }
            mMarkerCache.put(displayAreaLevel, childrenList)
            return childrenList
        }
    }

    /**
     * 绘制任务处理类
     */
    inner class DrawHandler(
        looper: Looper
    ) : Handler(looper) {

        /**
         * 已添加到地图上显示的标记
         */
        private var mAddedMapMakerList = ArrayList<T>()

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                ADD_MARKER_LIST -> {
                    val displayMarkerList: List<ClusterMarker<T>>? =
                        (msg.obj as? List<*>)?.asListOfType()
                    displayMarkerList?.also { list ->
                        addClusterToMap(list)
                    }
                }
            }
        }

        /**
         * 将聚合元素添加至地图上
         */
        private fun addClusterToMap(markerList: List<ClusterMarker<T>>) {
            /**
             * 移除目前已经在地图上显示的点
             */
            val removingMarkerList = ArrayList(mAddedMapMakerList)
            if (removingMarkerList.isEmpty()) {
                markerList.forEach {
                    val marker = addMarkerOnMap(it, it.lat, it.lng)
                    mAddedMapMakerList.add(marker)
                }
            } else {
                removeMarkerFromMap(removingMarkerList) { isComplete ->
                    if (isComplete) {
                        mAddedMapMakerList.clear()
                        addClusterToMap(markerList)
                    }
                }
            }
        }
    }

    internal class CalculateParam<T>(
        val marker: ClusterMarker<T>,
        val displayAreaLevel: DisplayAreaLevel
    ) : Serializable
}