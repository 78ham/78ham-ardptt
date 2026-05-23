package com.nrlptt.app.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object ApiClient {
    private const val TAG = "ApiClient"
    private val gson = Gson()
    var token: String = ""

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

    suspend fun login(host: String, user: String, pass: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            try {
                val resp = post("${baseUrl(host)}/user/login", mapOf("username" to user, "password" to pass))
                val map = parse(resp)
                val code = code(map)
                if (code == 20000 || code == 60204) {
                    val data = map["data"] as? Map<*, *> ?: return@withContext Result.failure(Exception("empty"))
                    token = data["token"] as? String ?: ""
                    if (token.isEmpty()) return@withContext Result.failure(Exception("no token"))
                    getUserInfo(host)
                } else Result.failure(Exception(msg(map)))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getUserInfo(host: String): Result<UserInfo> = withContext(Dispatchers.IO) {
        try {
            val resp = post("${baseUrl(host)}/user/info", emptyMap<String, Any>())
            val map = parse(resp)
            if (code(map) == 20000) {
                val d = map["data"] as? Map<*, *> ?: return@withContext Result.failure(Exception("empty"))
                Result.success(UserInfo(
                    id = (d["id"] as? Number)?.toInt() ?: 0,
                    username = d["username"] as? String ?: "",
                    callsign = d["callsign"] as? String ?: "",
                    dmrId = (d["dmr_id"] as? Number)?.toInt() ?: 0,
                    server = d["server"] as? String,
                    serverPort = (d["server_port"] as? Number)?.toInt(),
                    serverUdpPort = (d["server_udp_port"] as? Number)?.toInt()
                ))
            } else Result.failure(Exception(msg(map)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRoomList(host: String): Result<List<RoomInfo>> = withContext(Dispatchers.IO) {
        try {
            val resp = post("${baseUrl(host)}/group/list/mini", emptyMap<String, Any>())
            val map = parse(resp)
            if (code(map) == 20000) {
                val list = (map["data"] as? List<*>)?.mapNotNull { item ->
                    (item as? Map<*, *>)?.let {
                        RoomInfo(
                            id = (it["id"] as? Number)?.toInt() ?: 0,
                            name = it["name"] as? String ?: "",
                            memberCount = (it["member_count"] as? Number)?.toInt() ?: 0
                        )
                    }
                } ?: emptyList()
                Result.success(list)
            } else Result.failure(Exception(msg(map)))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getDevice(host: String, callsign: String, ssid: Int): Result<DeviceData> =
        withContext(Dispatchers.IO) {
            try {
                val resp = post("${baseUrl(host)}/device/get", mapOf("callsign" to callsign, "ssid" to ssid))
                val map = parse(resp)
                if (code(map) == 20000) {
                    val d = map["data"] as? Map<*, *> ?: return@withContext Result.failure(Exception("empty"))
                    Result.success(DeviceData(
                        id = (d["id"] as? Number)?.toInt() ?: 0,
                        callsign = d["callsign"] as? String ?: "",
                        ssid = (d["ssid"] as? Number)?.toInt() ?: 0,
                        groupId = (d["group_id"] as? Number)?.toInt() ?: 0,
                        dmrId = (d["dmr_id"] as? Number)?.toInt() ?: 0,
                        isOnline = d["is_online"] as? Boolean ?: false,
                        devModel = (d["dev_model"] as? Number)?.toInt() ?: 100
                    ))
                } else Result.failure(Exception(msg(map)))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun updateDevice(host: String, device: DeviceData, newGroup: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "id" to device.id, "callsign" to device.callsign, "ssid" to device.ssid,
                    "dmr_id" to device.dmrId, "group_id" to newGroup, "is_online" to device.isOnline,
                    "dev_model" to device.devModel
                )
                val resp = post("${baseUrl(host)}/device/update", body)
                val map = parse(resp)
                if (code(map) == 20000) Result.success(true)
                else Result.failure(Exception(msg(map)))
            } catch (e: Exception) { Result.failure(e) }
        }

    suspend fun getGroup(host: String, groupId: Int): Result<GroupInfo> = withContext(Dispatchers.IO) {
        try {
            val resp = post("${baseUrl(host)}/group/get", mapOf("group_id" to groupId))
            val map = parse(resp)
            if (code(map) == 20000) {
                val d = map["data"] as? Map<*, *> ?: return@withContext Result.failure(Exception("empty"))
                val devmapList = d["devmap"] as? List<*> ?: emptyList<Any>()
                val online = devmapList.count { (it as? Map<*, *>)?.get("is_online") == true }
                Result.success(GroupInfo(
                    id = (d["id"] as? Number)?.toInt() ?: 0,
                    name = d["name"] as? String ?: "",
                    onlineCount = online
                ))
            } else Result.failure(Exception(msg(map)))
        } catch (e: Exception) { Result.failure(e) }
    }

    // === helpers ===
    private fun baseUrl(host: String) = if (host.startsWith("http")) host else "https://$host"

    private fun post(url: String, body: Any): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"
            connectTimeout = 15000; readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("User-Agent", "NrlPtt/1.0")
            if (token.isNotEmpty()) setRequestProperty("x-token", token)
            outputStream.use { it.write(gson.toJson(body).toByteArray(StandardCharsets.UTF_8)) }
        }
        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use { it.readText() } ?: ""
        if (code !in 200..299) throw Exception("HTTP $code")
        return text
    }

    private fun parse(json: String) = gson.fromJson(json, Map::class.java) as Map<*, *>
    private fun code(m: Map<*, *>) = (m["code"] as? Number)?.toInt() ?: 0
    private fun msg(m: Map<*, *>) = m["message"] as? String ?: "error"
}
