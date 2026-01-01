package com.sheldon.idea.plugin.api.front.dashboard.utils
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils.ExportHelper
import okhttp3.Response
import okhttp3.MediaType
/**
 * 处理 OkHttp 响应
 * @param project IDEA 项目对象
 * @param response OkHttp 返回的 Response 对象
 * @param updateUiCallback 回调函数，用于更新 UI (headerText, bodyText)
 */
fun handleResponse(
    project: Project,
    response: CachedResponse,
    updateUiCallback: (String, String) -> Unit
) {
    val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
    val headersMap = response.headers.toMultimap().mapValues { entry ->
        entry.value.joinToString(", ")
    }
    val formattedHeaders = gson.toJson(headersMap)
    val mediaType = response.mediaType
    val isText = isTextType(mediaType)
    val bodyBytes = response.bodyBytes
    if (isText) {
        val bodyString = String(bodyBytes, mediaType?.charset() ?: Charsets.UTF_8)
        val finalBody = if (isJsonType(mediaType)) {
            try { gson.toJson(JsonParser.parseString(bodyString)) } catch (_: Exception) { bodyString }
        } else {
            bodyString
        }
        ApplicationManager.getApplication().invokeLater {
            updateUiCallback(formattedHeaders, finalBody)
        }
    } else {
        ApplicationManager.getApplication().invokeLater {
            updateUiCallback(formattedHeaders, "(binary data: ${byteCountToDisplaySize(bodyBytes.size.toLong())})")
        }
        val fileName = resolveFileName(response)
        val extension = fileName.substringAfterLast(".", "")
        ApplicationManager.getApplication().invokeLater {
            ExportHelper.exportBinaryToFile(
                project = project,
                data = bodyBytes,
                defaultFileName = fileName,
                extensions = extension,
                title = "保存文件"
            )
        }
    }
}
fun resolveFileName(response: CachedResponse): String {
    val contentDisposition = response.headers["Content-Disposition"]
    if (contentDisposition != null) {
        val pattern = "filename=\"?([^;\"]+)\"?".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(contentDisposition)
        if (match != null) {
            return match.groupValues[1].trim()
        }
    }
    try {
        val path = java.net.URI(response.requestUrl).path
        val fileNameFromUrl = path.substringAfterLast("/")
        if (fileNameFromUrl.isNotEmpty() && fileNameFromUrl.contains(".")) {
            return fileNameFromUrl
        }
    } catch (e: Exception) {
    }
    val subtype = response.mediaType?.subtype ?: "dat"
    return "download_file.$subtype"
}
/** 判断是否是 JSON 类型 */
fun isJsonType(mediaType: MediaType?): Boolean {
    if (mediaType == null) return false
    return mediaType.subtype.contains("json", ignoreCase = true) ||
            mediaType.subtype.contains("javascript", ignoreCase = true)
}
/** 判断是否是可显示的文本类型 */
fun isTextType(mediaType: MediaType?): Boolean {
    if (mediaType == null) return true 
    val type = mediaType.type
    val subtype = mediaType.subtype
    return type == "text" ||
            subtype.contains("json") ||
            subtype.contains("xml") ||
            subtype.contains("html") ||
            subtype.contains("x-www-form-urlencoded")
}
/** 尝试从 Content-Disposition 或 URL 获取文件名 */
fun getFileNameFromResponse(response: Response): String {
    val contentDisposition = response.header("Content-Disposition")
    if (contentDisposition != null) {
        val pattern = "filename=\"?([^\";]+)\"?".toRegex()
        val match = pattern.find(contentDisposition)
        if (match != null) {
            return match.groupValues[1]
        }
    }
    val urlPath = response.request.url.encodedPath
    val fileNameFromUrl = urlPath.substringAfterLast("/")
    if (fileNameFromUrl.isNotEmpty() && fileNameFromUrl.contains(".")) {
        return fileNameFromUrl
    }
    val subtype = response.body?.contentType()?.subtype ?: "dat"
    return "download_file.$subtype"
}
/** 字节数转可读大小 */
fun byteCountToDisplaySize(size: Long): String {
    val unit = 1024
    if (size < unit) return "$size B"
    val exp = (Math.log(size.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", size / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}