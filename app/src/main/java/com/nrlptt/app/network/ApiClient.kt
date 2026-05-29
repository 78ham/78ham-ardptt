package com.nrlptt.app.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class UserInfo(
    val id: Int = 0, val username: String = "", val callsign: String = "",
    val dmrId: Int = 0, val server: String? = null,
    val serverPort: Int? = null, val serverUdpPort: Int? = null
)

data class RoomInfo(val id: Int = 0, val name: String = "", val memberCount: Int = 0)
data class GroupInfo(val id: Int = 0, val name: String = "", val onlineCount: Int = 0)
data class DeviceData(
    val id: Int = 0, val callsign: String = "", val ssid: Int = 0,
    val groupId: Int = 0, val dmrId: Int = 0, val isOnline: Boolean = false,
    val devModel: Int = 100
)

data class PlatformServer(val name: String = "", val host: String = "", val port: String = "60050", val online: Int = 0, val total: Int = 0)

class ApiSession {
    companion object {
        private const val TAG = "ApiSession"
        private val gson = Gson()
        private val mapType = object : TypeToken<Map<String, Any>>() {}.type
    }

    var token: String = ""

    suspend fun login(host: String, user: String, pass: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = post("${baseUrl(host)}/user/login", mapOf("username" to user, "password" to pass))
                val map = gson.fromJson<Map<String, Any>>(resp, mapType)
                val code = code(map)
                if (code != 20000 && code != 60204) throw Exception(msg(map))
                val data = map["data"] as? Map<*, *> ?: throw Exception("empty")
                token = data["token"] as? String ?: throw Exception("no token")
                getUserInfo(host).getOrThrow()
            }
        }

    suspend fun getUserInfo(host: String): Result<UserInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val map = postParsed("${baseUrl(host)}/user/info", emptyMap<String, Any>())
            val d = dataMap(map) ?: throw Exception("empty")
            UserInfo(
                id = int(d, "id"), username = str(d, "username"), callsign = str(d, "callsign"),
                dmrId = int(d, "dmr_id"), server = d["server"] as? String,
                serverPort = num(d, "server_port"), serverUdpPort = num(d, "server_udp_port")
            )
        }
    }

    suspend fun getRoomList(host: String): Result<List<RoomInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val map = postParsed("${baseUrl(host)}/group/list/mini", emptyMap<String, Any>())
            @Suppress("UNCHECKED_CAST")
            val list = map["data"] as? List<Map<String, Any>> ?: emptyList()
            list.map { RoomInfo(int(it, "id"), str(it, "name"), int(it, "member_count")) }
        }
    }

    suspend fun getDevice(host: String, callsign: String, ssid: Int): Result<DeviceData> =
        withContext(Dispatchers.IO) {
            runCatching {
                val map = postParsed("${baseUrl(host)}/device/get", mapOf("callsign" to callsign, "ssid" to ssid))
                val d = dataMap(map) ?: throw Exception("empty")
                DeviceData(
                    id = int(d, "id"), callsign = str(d, "callsign"), ssid = int(d, "ssid"),
                    groupId = int(d, "group_id"), dmrId = int(d, "dmr_id"),
                    isOnline = d["is_online"] as? Boolean ?: false, devModel = int(d, "dev_model", 100)
                )
            }
        }

    suspend fun updateDevice(host: String, device: DeviceData, newGroup: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = mapOf(
                    "id" to device.id, "callsign" to device.callsign, "ssid" to device.ssid,
                    "dmr_id" to device.dmrId, "group_id" to newGroup, "is_online" to device.isOnline,
                    "dev_model" to device.devModel
                )
                val map = postParsed("${baseUrl(host)}/device/update", body)
                if (code(map) != 20000) throw Exception(msg(map))
                true
            }
        }

    suspend fun getGroup(host: String, groupId: Int): Result<GroupInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val map = postParsed("${baseUrl(host)}/group/get", mapOf("group_id" to groupId))
            val d = dataMap(map) ?: throw Exception("empty")
            @Suppress("UNCHECKED_CAST")
            val devmap = d["devmap"] as? List<Map<String, Any>> ?: emptyList()
            val online = devmap.count { it["is_online"] == true }
            GroupInfo(id = int(d, "id"), name = str(d, "name"), onlineCount = online)
        }
    }

    private fun postParsed(url: String, body: Any): Map<String, Any> = gson.fromJson(post(url, body), mapType)

    private fun dataMap(map: Map<String, Any>): Map<String, Any>? {
        if (code(map) != 20000) throw Exception(msg(map))
        @Suppress("UNCHECKED_CAST")
        return map["data"] as? Map<String, Any>
    }

    private fun post(url: String, body: Any): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"; connectTimeout = 15000; readTimeout = 15000; doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "NrlPtt/1.0")
            setRequestProperty("Connection", "keep-alive")
            if (token.isNotEmpty()) setRequestProperty("x-token", token)
            outputStream.use { it.write(gson.toJson(body).toByteArray(StandardCharsets.UTF_8)) }
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Exception("HTTP $code")
        return text
    }

    private fun baseUrl(host: String) = if (host.startsWith("http")) host else "https://$host"
    private fun code(m: Map<String, Any>) = (m["code"] as? Number)?.toInt() ?: 0
    private fun msg(m: Map<String, Any>) = m["message"] as? String ?: "error"
    private fun int(m: Map<*, *>, key: String, default: Int = 0) = (m[key] as? Number)?.toInt() ?: default
    private fun num(m: Map<*, *>, key: String) = (m[key] as? Number)?.toInt()
    private fun str(m: Map<*, *>, key: String, default: String = "") = m[key] as? String ?: default
}

object ApiClient {
    suspend fun getPlatformList(): Result<List<PlatformServer>> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://m.nrlptt.com/platform/list").openConnection() as HttpURLConnection
            conn.apply {
                requestMethod = "GET"; connectTimeout = 10000; readTimeout = 10000
                setRequestProperty("User-Agent", "NrlPtt/1.0")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val gson = Gson()
            val map = gson.fromJson<Map<String, Any>>(text, object : TypeToken<Map<String, Any>>() {}.type)
            @Suppress("UNCHECKED_CAST")
            val data = map["data"] as? Map<String, Any>
            val items = data?.get("items") as? List<*> ?: emptyList<Any>()
            items.mapNotNull { item ->
                (item as? Map<*, *>)?.let {
                    PlatformServer(
                        name = it["name"] as? String ?: "",
                        host = it["host"] as? String ?: "",
                        port = (it["port"] as? Number)?.toString() ?: "60050",
                        online = (it["online"] as? Number)?.toInt() ?: 0,
                        total = (it["total"] as? Number)?.toInt() ?: 0
                    )
                }
            }
        }
    }
}
