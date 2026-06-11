package com.autodiag.data

import kotlinx.coroutines.flow.Flow

interface ObdRepository {
    val liveData: Flow<Map<String, Double>>
    val dtcs: Flow<List<String>>
    suspend fun clearDtcs()
    fun connect()
    fun disconnect()
}
