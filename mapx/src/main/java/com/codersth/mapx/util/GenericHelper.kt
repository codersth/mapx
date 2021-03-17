package com.codersth.mapx.util

/**
 *
 * @author zhanglei1
 * @date 2021/3/17-10:24
 * @since V1.0.0
 */
object GenericHelper {
    inline fun <reified T> List<*>.asListOfType(): List<T>? =
        if (all { it is T })
            @Suppress("UNCHECKED_CAST")
            this as List<T> else
            null
}