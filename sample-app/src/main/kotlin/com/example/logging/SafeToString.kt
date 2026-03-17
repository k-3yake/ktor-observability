package com.example.logging

import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

fun Any.toSafeString(): String {
    val kClass = this::class
    val className = kClass.simpleName ?: "Unknown"
    val props = kClass.memberProperties.joinToString(", ") { prop ->
        val value = prop.getter.call(this)
        val displayValue = if (prop.findAnnotation<Sensitive>() != null) {
            mask(value)
        } else {
            value.toString()
        }
        "${prop.name}=$displayValue"
    }
    return "$className($props)"
}

private fun mask(value: Any?): String {
    if (value == null) return "***"
    val str = value.toString()
    if (str.length <= 4) return "***"
    val visiblePrefix = str.substring(0, 3)
    val visibleSuffix = str.substring(str.length - 3)
    return "$visiblePrefix***$visibleSuffix"
}
