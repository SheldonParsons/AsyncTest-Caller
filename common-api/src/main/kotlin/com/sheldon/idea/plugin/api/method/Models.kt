package com.sheldon.idea.plugin.api.method

import com.google.gson.annotations.SerializedName
import java.util.concurrent.ThreadLocalRandom

/**
 * 参数位置枚举：决定了这个参数最终去 ApiRequest 的哪个列表
 */
enum class ParamLocation {
    HEADER,
    QUERY,
    BODY,
    FORM_DATA,
}


/**
 * AsyncTest 平台专用的 Schema 节点
 */
data class AsyncTestVariableNode(
    // 类型: "ds" (结构体), "string", "integer", "number", "boolean", "array", "null", "files", "object"
    @SerializedName("t")
    val type: String,

    // 字段名 (如果是根节点通常是类名，如果是字段则是字段名)
    @SerializedName("name")
    var name: String = "",

    // 随机 7 位数字 ID
    @SerializedName("id")
    val id: Int = generateRandomId(),

    // 默认值 / 引用 Key
    // 对于简单类型: 默认值 (如 "0", "")
    // 对于 "ds": 引用 Key (如 "com.example.UserDto")
    @SerializedName("default")
    var defaultValue: String = "",

    // 描述 (目前留空)
    @SerializedName("statement")
    var statement: String = "",

    // 是否必填
    @SerializedName("required")
    var required: Boolean = false,

    // content-type
    @SerializedName("content_type")
    var contentType: String = "",

    // 子节点 (用于 ds 的字段 或 array 的元素)
    @SerializedName("children")
    var children: MutableList<AsyncTestVariableNode> = mutableListOf(),

    @SerializedName("ds_target")
    var dsTarget: String? = null,

    @SerializedName("child_list")
    val childList: MutableList<String> = mutableListOf(),

    @SerializedName("file_list")
    var fileList: MutableList<String> = mutableListOf(),
) {
    companion object {
        fun generateRandomId(): Int {
            return ThreadLocalRandom.current().nextInt(1000000, 9999999)
        }
    }
}


object AsyncTestType {
    const val STRING = "string"
    const val INTEGER = "integer"
    const val BOOLEAN = "boolean"
    const val NUMBER = "number"
    const val ARRAY = "array"
    const val NULL = "null"
    const val FILES = "files"
    const val OBJECT = "object"
    const val DS = "ds"
}

object AsyncTestBodyType {
    const val NONE = "none"
    const val FORM_DATA = "form-data"
    const val X_WWW_FORM_URLENCODED = "x-www-form-urlencoded"
    const val JSON = "json"
    const val RAW = "raw"
}