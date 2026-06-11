package com.autodiag.data

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import org.json.JSONObject

class WebSocketRepository(private val host: String = "127.0.0.1", private val port: Int = 8000) : ObdRepository {

    private val client = OkHttpClient()
    private var ws: WebSocket? = null

    override val liveData: Flow<Map<String, Double>> = callbackFlow {
        val request = Request.Builder().url("ws://$host:$port/ws/live").build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                val map = mutableMapOf<String, Double>()
                json.keys().forEach { key -> map[key] = json.getDouble(key) }
                trySend(map)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        })
        awaitClose { ws?.cancel() }
    }

    override val dtcs: Flow<List<String>> = callbackFlow {
        val request = Request.Builder().url("http://$host:$port/dtc").build()
        client.newCall(request).execute().use { resp ->
            val json = JSONObject(resp.body!!.string())
            val arr = json.getJSONArray("dtcs")
            trySend((0 until arr.length()).map { arr.getString(it) })
        }
        awaitClose {}
    }

    override suspend fun clearDtcs() {
        val request = Request.Builder().url("http://$host:$port/dtc").delete().build()
        client.newCall(request).execute().close()
    }

    override fun connect() {}
    override fun disconnect() { ws?.cancel() }
}
