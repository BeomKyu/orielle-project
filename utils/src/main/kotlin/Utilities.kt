package com.orielle.utils

import kotlinx.serialization.Serializable
import kotlinx.coroutines.*

@Serializable
class Printer(val message: String) {
    fun printMessage() = runBlocking {
        // 실험 API(kotlin.time/ kotlinx.datetime) 대신 안정적인 JDK 시간 API 사용
        val now: java.time.Instant = java.time.Instant.now()
        launch {
            delay(1000L)
            println(now.toString())
        }
        println(message)
    }
}