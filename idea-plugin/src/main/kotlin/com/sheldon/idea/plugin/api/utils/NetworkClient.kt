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
//                println(response)
//                if (!response.isSuccessful) {
//                    println(response)
//                }
//                println("------")
                return response.body?.string() ?: ""
            }
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
                if (isMultipart(currentBody)) {
                    buildMultipartBody(currentBody)
                } else {
                    buildFormBody(currentBody)
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

    private fun buildMultipartBody(map: Map<*, *>): RequestBody {
        val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
        map.forEach { (k, v) ->
            val keyStr = k.toString()
            when (v) {
                is File -> {
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

object HttpMethod {
    fun requiresRequestBody(method: String): Boolean {
        return method == "POST" || method == "PUT" || method == "PATCH" || method == "PROPPATCH" || method == "REPORT"
    }
}