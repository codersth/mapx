package com.codersth.mapx.common

/**
 *
 * @author zhanglei1
 * @date 2021/3/16-18:12
 * @since V1.0.0
 */
enum class DisplayAreaLevel {
    Country,
    Province,
    City,
    County,
    Point;

    companion object {

        /**
         * 根据后台返回的totalLevel返回对应的级别
         */
        fun getLevel(totalLevel: Int): DisplayAreaLevel {
            val count = values().size
            if (totalLevel in 1..count) {
                return values()[count - totalLevel]
            } else {
                return Point
            }
        }
    }

    /**
     * 获取下一级别，最后一级返回null
     */
    fun nextLevel(): DisplayAreaLevel? {
        return offset(1)
    }

    /**
     * 获取上一级别，最后一级返回null
     */
    fun lastLevel(): DisplayAreaLevel? {
        return offset(-1)
    }

    /**
     * 获取偏差值
     * @param offset 负值表示当前值的往上取，正值当前值往下取
     */
    fun offset(offset: Int): DisplayAreaLevel? {
        val enumValues = enumValues<DisplayAreaLevel>()
        val position = ordinal + offset
        return if (position >= 0 && position < enumValues.size) {
            enumValues[position]
        } else {
            null
        }
    }
}