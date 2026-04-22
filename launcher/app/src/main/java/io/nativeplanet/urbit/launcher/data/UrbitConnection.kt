package io.nativeplanet.urbit.launcher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class UrbitConnection(
    private val shipUrl: String = "http://127.0.0.1:80",
    private val loopbackUrl: String = "http://127.0.0.1:12321"
) {
    private val eventId = AtomicLong(1)
    private var cookie: String? = null

    private val cookieJar = object : CookieJar {
        private val store = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store.addAll(cookies)
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = store.toList()
    }

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val isAuthenticated: Boolean get() = cookie != null
    val authCookie: String? get() = cookie
    val baseUrl: String get() = shipUrl

    /**
     * Fetch the +code from the ship via the loopback lens API (port 12321).
     * This port allows unauthenticated access from localhost — perfect for
     * on-device use where the launcher and ship share the same phone.
     */
    suspend fun fetchCode(): String? = withContext(Dispatchers.IO) {
        try {
            val json = """{"source":{"dojo":"+code"},"sink":{"stdout":null}}"""
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(loopbackUrl)
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val result = if (response.isSuccessful) {
                response.body?.string()?.trim()?.trim('"')?.replace("\\n", "")
            } else null
            response.close()
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Auto-authenticate: fetch +code via loopback, then log in via Eyre.
     * No user input needed when ship and launcher are on the same device.
     */
    suspend fun autoAuthenticate(): Boolean {
        val code = fetchCode() ?: return false
        return authenticate(code)
    }

    suspend fun authenticate(code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val body = "password=$code".toRequestBody("application/x-www-form-urlencoded".toMediaType())
            val request = Request.Builder()
                .url("$shipUrl/~/login")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            val success = response.isSuccessful || response.code == 204
            if (success) {
                cookie = response.header("set-cookie")
            }
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun scry(app: String, path: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$shipUrl/~/scry/$app$path.json")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val result = if (response.isSuccessful) response.body?.string() else null
            response.close()
            result
        } catch (e: Exception) {
            null
        }
    }

    suspend fun poke(app: String, mark: String, json: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = JSONArray().put(
                JSONObject().apply {
                    put("id", eventId.getAndIncrement())
                    put("action", "poke")
                    put("ship", getShipName() ?: return@withContext false)
                    put("app", app)
                    put("mark", mark)
                    put("json", json)
                }
            )
            val body = payload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$shipUrl/~/channel/${System.currentTimeMillis()}")
                .put(body)
                .build()
            val response = client.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getShipName(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$shipUrl/~/name")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val name = if (response.isSuccessful) response.body?.string()?.trim()?.trim('"') else null
            response.close()
            name
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getApps(): List<String> = withContext(Dispatchers.IO) {
        try {
            val result = scry("hood", "/kiln/running")
            if (result != null) {
                val json = JSONObject(result)
                json.keys().asSequence().toList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getDockets(): Map<String, DocketInfo> = withContext(Dispatchers.IO) {
        try {
            val result = scry("docket", "/charges")
            if (result != null) {
                val root = JSONObject(result)
                // Response is wrapped in {"initial": {...}}
                val charges = if (root.has("initial")) root.getJSONObject("initial") else root
                val map = mutableMapOf<String, DocketInfo>()
                charges.keys().forEach { key ->
                    val charge = charges.getJSONObject(key)
                    map[key] = DocketInfo(
                        title = charge.optString("title", key),
                        info = charge.optString("info", ""),
                        color = charge.optString("color", "0x0"),
                        version = charge.optString("version", ""),
                        image = charge.optString("image", "")
                    )
                }
                map
            } else emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}

data class DocketInfo(
    val title: String,
    val info: String,
    val color: String,
    val version: String,
    val image: String
)
