package com.sheldon.idea.plugin.api.utils

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class HttpExecutor {

    companion object {
        // 单例 Client，复用连接池
        private val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    // 内部状态
    private var url: String = ""
    private var method: String = "GET"
    private val queryParams = mutableMapOf<String, String>()
    private val headers = mutableMapOf<String, String>()
    private var body: Any? = null // 支持 String(JSON) 或 Map(Form/File)

    // ==========================================
    // 1. 你要求的 API 函数 (Strict Setters)
    // ==========================================

    fun setUrl(url: String) {
        this.url = url
    }

    fun setMethod(method: String) {
        // 统一转大写
        this.method = method.uppercase()
    }

    fun setParam(key: String, value: String) {
        this.queryParams[key] = value
    }

    fun setHeader(key: String, value: String) {
        this.headers[key] = value
    }

    /**
     * 设置请求体
     * - 传入 String: 视为 JSON
     * - 传入 Map: 视为表单。如果 Map 值包含 File，自动视为 Multipart 上传
     */
    fun setBody(body: Any) {
        this.body = body
    }

    // ==========================================
    // 2. 核心发送函数
    // ==========================================

    fun send(): String {
        println("in send")
        println(this.url)
        try {
            // A. 构建 URL (拼接 Query Params)
            val httpUrlBuilder = this.url
                .toHttpUrlOrNull()
                ?.newBuilder()
                ?: throw IllegalArgumentException("无效的 URL: ${this.url}")

            queryParams.forEach { (k, v) -> httpUrlBuilder.addQueryParameter(k, v) }
            val finalUrl = httpUrlBuilder.build()

            // B. 构建 RequestBody
            val requestBody = createRequestBody()

            // C. 构建 Request
            // method() 函数直接接受字符串
            val request = Request.Builder()
                .url(finalUrl)
                .apply {
                    headers.forEach { (k, v) -> addHeader(k, v) }
                }
                .method(this.method, requestBody)
                .build()

            // D. 执行请求
            client.newCall(request).execute().use { response ->
                println(response)
                if (!response.isSuccessful) {
                    // 你可以在这里决定是抛异常还是返回错误信息，这里简单返回 body
                    // throw IOException("Unexpected code $response")
                    println(response)
                }
                println("------")
                return response.body?.string() ?: ""
            }

        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("请求发送失败: ${e.message}", e)
        }
    }

    // ==========================================
    // 3. 私有辅助逻辑
    // ==========================================

    private fun createRequestBody(): RequestBody? {
        // GET/HEAD 等不需要 body 的情况，如果用户没设 body，返回 null
        if (!HttpMethod.requiresRequestBody(method) && body == null) {
            return null
        }
        // POST/PUT 等强制需要 body，如果用户没设，给个空的
        if (HttpMethod.requiresRequestBody(method) && body == null) {
            return ByteArray(0).toRequestBody(null, 0, 0)
        }

        return when (val currentBody = body) {
            is String -> {
                currentBody.toRequestBody()
            }

            is Map<*, *> -> {
                // 2. 处理 Map
                if (isMultipart(currentBody)) {
                    buildMultipartBody(currentBody)
                } else {
                    buildFormBody(currentBody)
                }
            }

            else -> null
        }
    }

    // 检查是否包含文件
    private fun isMultipart(map: Map<*, *>): Boolean {
        return map.values.any { it is File }
    }

    // 构建普通表单 (application/x-www-form-urlencoded)
    private fun buildFormBody(map: Map<*, *>): RequestBody {
        val builder = FormBody.Builder()
        map.forEach { (k, v) ->
            builder.add(k.toString(), v.toString())
        }
        return builder.build()
    }

    // 构建文件上传 (multipart/form-data)
    private fun buildMultipartBody(map: Map<*, *>): RequestBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)

        map.forEach { (k, v) ->
            val keyStr = k.toString()
            when (v) {
                is File -> {
                    // 自动探测 MIME 类型
                    val mimeType = try {
                        Files.probeContentType(v.toPath())
                    } catch (e: Exception) {
                        null
                    } ?: "application/octet-stream"

                    val fileBody = v.asRequestBody(mimeType.toMediaTypeOrNull())
                    builder.addFormDataPart(keyStr, v.name, fileBody)
                }

                else -> {
                    builder.addFormDataPart(keyStr, v.toString())
                }
            }
        }
        return builder.build()
    }
}

// 简单的辅助对象，用于判断 Method 是否需要 Body (OkHttp 内部逻辑的简化版)
object HttpMethod {
    fun requiresRequestBody(method: String): Boolean {
        return method == "POST" || method == "PUT" || method == "PATCH" || method == "PROPPATCH" || method == "REPORT"
    }
}