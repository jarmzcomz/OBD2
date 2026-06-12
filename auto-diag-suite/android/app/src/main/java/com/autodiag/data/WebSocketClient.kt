package com.autodiag.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Transporte WiFi: se conecta al backend FastAPI (auto-diag-suite/app/main.py).
 * Acepta direcciones "host" o "host:puerto". Habilita el panel avanzado
 * (UDS / CAN / VAG) porque el backend expone esos endpoints con Zero-Brick.
 */
class WebSocketRepository(address: String) : ObdRepository {

    private val host: String
    private val port: Int

    init {
        val trimmed = address.trim().removePrefix("http://").removePrefix("ws://")
        val parts = trimmed.split(":")
        host = parts[0].ifBlank { "127.0.0.1" }
        port = parts.getOrNull(1)?.toIntOrNull() ?: 8000
    }

    private val client = OkHttpClient.Builder()
        .callTimeout(8, TimeUnit.SECONDS)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // 0 = sin límite para el WebSocket
        .build()

    private var ws: WebSocket? = null

    val baseHttp: String get() = "http://$host:$port"

    override val liveData: Flow<Map<String, Double>> = callbackFlow {
        val request = Request.Builder().url("ws://$host:$port/ws/live").build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    val map = mutableMapOf<String, Double>()
                    json.keys().forEach { key ->
                        val v = json.opt(key)
                        if (v is Number) map[key] = v.toDouble()
                    }
                    trySend(map)
                }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        })
        awaitClose { ws?.cancel() }
    }

    override val dtcs: Flow<List<String>> = flow {
        val codes = withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$baseHttp/dtc").build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: return@use emptyList<String>()
                val arr = JSONObject(body).optJSONArray("dtcs") ?: return@use emptyList<String>()
                (0 until arr.length()).map { arr.getString(it) }
            }
        }
        emit(codes)
    }.flowOn(Dispatchers.IO)

    override suspend fun clearDtcs() {
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url("$baseHttp/dtc").delete().build()
            runCatching { client.newCall(request).execute().close() }
        }
    }

    override fun connect() {}
    override fun disconnect() { ws?.cancel() }

    override val capabilities = ObdCapabilities(backendAdvanced = true)

    /** GET genérico al backend; devuelve el cuerpo crudo (JSON). Para el panel avanzado. */
    suspend fun httpGet(path: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$baseHttp/${path.trimStart('/')}").build()
        runCatching {
            client.newCall(req).execute().use { it.body?.string() ?: "" }
        }.getOrElse { "ERROR: ${it.message}" }
    }

    /** POST JSON genérico al backend; devuelve el cuerpo crudo (JSON). */
    suspend fun httpPost(path: String, json: String): String = withContext(Dispatchers.IO) {
        val body = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder().url("$baseHttp/${path.trimStart('/')}").post(body).build()
        runCatching {
            client.newCall(req).execute().use { it.body?.string() ?: "" }
        }.getOrElse { "ERROR: ${it.message}" }
    }
}
