package com.sheldon.idea.plugin.api.front.dashboard.utils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.constant.AsyncTestConstant
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.model.ApiMockRequest
import com.sheldon.idea.plugin.api.model.FormDataField
import com.sheldon.idea.plugin.api.utils.HttpExecutor
import com.sheldon.idea.plugin.api.utils.context
import com.sheldon.idea.plugin.api.utils.runBackgroundReadUI
import okhttp3.Response
import okhttp3.Headers
import okhttp3.MediaType
data class CachedResponse(
    val code: Int,
    val headers: Headers,
    val bodyBytes: ByteArray,
    val mediaType: MediaType?,
    val requestUrl: String
)
object MockRequestHelper {
    fun send(
        project: Project,
        request: ApiMockRequest,
        url: String,
        bodyType: String?,
        callback: (CachedResponse) -> Unit
    ) {
        project.context().runBackgroundReadUI(
            lockKey = CommonConstant.AST_CALLER_GLOBAL_HTTP_ACTION,
            requiresReadAction = false,
            backgroundTask = { p ->
                val httpExecutor = HttpExecutor()
                httpExecutor.setMethod(request.method?.uppercase() ?: "GET")
                httpExecutor.setUrl(url)
                setHeaders(request.headers ?: "{}", httpExecutor)
                setParams(request.query ?: "{}", httpExecutor)
                if (bodyType != null) {
                    if (bodyType.contains("application/json")) {
                        setJsonBody(request.body, httpExecutor)
                    } else if (bodyType.contains("multipart/form-data")) {
                        setFormDataBody(request.formData, httpExecutor)
                    }
                }
                // 1. 获取原始响应 (流)
                val rawResponse = httpExecutor.sendAndGetResponse()
                // 2. ★★★ 关键步骤：在后台线程立即读取数据 ★★★
                try {
                    // 读取 body bytes (这一步会消耗流)
                    val bytes = rawResponse.body?.bytes() ?: ByteArray(0)
                    // 3. 构建脱水后的 CachedResponse
                    return@runBackgroundReadUI CachedResponse(
                        code = rawResponse.code,
                        headers = rawResponse.headers, // Headers 对象是安全的
                        bodyBytes = bytes,
                        mediaType = rawResponse.body?.contentType(),
                        requestUrl = url
                    )
                } finally {
                    // 4. 务必关闭原始响应，释放连接资源
                    rawResponse.close()
                }
            },
            uiUpdate = { result, p ->
                callback(result)
            }
        )
    }
    private fun setJsonBody(jsonString: String, httpExecutor: HttpExecutor) {
        httpExecutor.setBody(jsonString)
    }
    private fun setFormDataBody(jsonString: String, httpExecutor: HttpExecutor) {
        val gson = Gson()
        val type = object : TypeToken<Map<String, FormDataField>>() {}.type
        val map: Map<String, FormDataField> = gson.fromJson(jsonString, type)
        httpExecutor.setBody(map)
    }
    private fun setHeaders(jsonString: String, httpExecutor: HttpExecutor) {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = Gson().fromJson(jsonString, type)
        map.forEach { (key, value) ->
            httpExecutor.setHeader(key, value.toString())
        }
    }
    private fun setParams(jsonString: String, httpExecutor: HttpExecutor) {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val map: Map<String, Any> = Gson().fromJson(jsonString, type)
        map.forEach { (key, value) ->
            httpExecutor.setParam(key, value.toString())
        }
    }
}