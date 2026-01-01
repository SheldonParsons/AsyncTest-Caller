package com.sheldon.idea.plugin.api.utils
import com.sheldon.idea.plugin.api.model.FormDataField
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.util.concurrent.TimeUnit
class HttpExecutor {
    companion object {
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    private var url: String = ""
    private var method: String = "GET"
    private val queryParams = mutableMapOf<String, String>()
    private val headers = mutableMapOf<String, String>()
    private var body: Any? = null
    fun setUrl(url: String) {
        this.url = url
    }
    fun setMethod(method: String) {
        this.method = method.uppercase()
    }
    fun setParam(key: String, value: String) {
        this.queryParams[key] = value
    }
    fun setHeader(key: String, value: String) {
        this.headers[key] = value
    }
    fun setBody(body: Any) {
        this.body = body
    }
    fun send(): String {
        try {
            val httpUrlBuilder = this.url
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?: throw IllegalArgumentException("无效的 URL: ${this.url}")
            queryParams.forEach { (k, v) -> httpUrlBuilder.addQueryParameter(k, v) }
            val finalUrl = httpUrlBuilder.build()
            val requestBody = createRequestBody()
            val request = Request.Builder()
                .url(finalUrl)
                .apply {
                    headers.forEach { (k, v) -> addHeader(k, v) }
                }
                .method(this.method, requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                return response.body?.string() ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("请求发送失败: ${e.message}", e)
        }
    }
    fun sendAndGetResponse(): Response {
        try {
            val httpUrlBuilder = this.url
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?: throw IllegalArgumentException("无效的 URL: ${this.url}")
            queryParams.forEach { (k, v) -> httpUrlBuilder.addQueryParameter(k, v) }
            val finalUrl = httpUrlBuilder.build()
            val requestBody = createRequestBody()
            val request = Request.Builder()
                .url(finalUrl)
                .apply {
                    headers.forEach { (k, v) -> addHeader(k, v) }
                }
                .method(this.method, requestBody)
                .build()
            return client.newCall(request).execute()
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("请求发送失败: ${e.message}", e)
        }
    }
    private fun createRequestBody(): RequestBody? {
        if (!HttpMethod.requiresRequestBody(method) && body == null) {
            return null
        }
        if (HttpMethod.requiresRequestBody(method) && body == null) {
            return ByteArray(0).toRequestBody(null, 0, 0)
        }
        return when (val currentBody = body) {
            is String -> {
                currentBody.toRequestBody()
            }
            is Map<*, *> -> {
                val typedMap = currentBody.entries
                    .mapNotNull { (k, v) ->
                        val key = k as? String ?: return@mapNotNull null
                        val value = v as? FormDataField ?: return@mapNotNull null
                        key to value
                    }
                    .toMap()
                if (typedMap.isNotEmpty()) {
                    buildMultipartBody(typedMap)
                } else {
                    null
                }
            }
            else -> null
        }
    }
    private fun isMultipart(map: Map<*, *>): Boolean {
        return map.values.any { it is File }
    }
    private fun buildFormBody(map: Map<*, *>): RequestBody {
        val builder = FormBody.Builder()
        map.forEach { (k, v) ->
            builder.add(k.toString(), v.toString())
        }
        return builder.build()
    }
    fun buildMultipartBody(formData: Map<String, FormDataField>): MultipartBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        formData.forEach { (key, data) ->
            if (data.type == "text") {
                // 1. 处理普通文本
                builder.addFormDataPart(key, data.value)
            } else {
                // 2. 处理文件 (关键点在这里)
                // 不需要 Map key 重复，而是遍历 data.files 列表
                // 针对同一个 key，循环调用 addFormDataPart
                if (data.fileList.isNotEmpty()) {
                    for (filePath in data.fileList) {
                        val file = File(filePath)
                        if (file.exists()) {
                            // 自动识别 MimeType，识别不出来就用 octet-stream
                            val mimeType = data.contentType.toMediaTypeOrNull()
                            val fileBody = file.asRequestBody(mimeType)
                            // ★★★ 关键：这里用的是同一个 key (例如 "file") ★★★
                            builder.addFormDataPart(key, file.name, fileBody)
                        }
                    }
                }
            }
        }
        return builder.build()
    }
}
object HttpMethod {
    fun requiresRequestBody(method: String): Boolean {
        return method == "POST" || method == "PUT" || method == "PATCH" || method == "PROPPATCH" || method == "REPORT"
    }
}