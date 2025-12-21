package com.sheldon.idea.plugin.api.method

import com.google.gson.annotations.SerializedName
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

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
@Tag("node") // 对应 XML 中的 <node> 标签
data class AsyncTestVariableNode(
    @SerializedName("t")
    @Attribute("t")
    var type: String = "",

    @SerializedName("name")
    @Attribute("name")
    var name: String = "",

    @SerializedName("id")
    @Attribute("id")
    var id: Int = 0,

    @SerializedName("default")
    @Attribute("default")
    var defaultValue: String = "",

    @SerializedName("statement")
    @Attribute("statement")
    var statement: String = "",

    @SerializedName("required")
    @Attribute("required")
    var required: Boolean = false,

    @SerializedName("content_type")
    @Attribute("content_type")
    var contentType: String = "",

    @SerializedName("children")
    @Tag("children")
    @XCollection(style = XCollection.Style.v2)
    var children: ArrayList<AsyncTestVariableNode> = arrayListOf(),

    @SerializedName("ds_target")
    @Attribute("ds_target")
    var dsTarget: String? = "",

    @SerializedName("child_list")
    @Tag("child_list")
    @XCollection(style = XCollection.Style.v2)
    var childList: ArrayList<String> = arrayListOf(),

    @SerializedName("file_list")
    @Tag("file_list")
    @XCollection(style = XCollection.Style.v2)
    var fileList: ArrayList<String> = arrayListOf(),
) {

    init {
        if (id == 0) {
            id = ThreadLocalRandom.current().nextInt(1000000, 9999999)
        }
    }

    // 这是一个辅助构造函数或方法，用于业务中创建带 ID 的新节点
    companion object {
        fun createNew(): AsyncTestVariableNode {
            return AsyncTestVariableNode().apply {
                id = ThreadLocalRandom.current().nextInt(1000000, 9999999)
            }
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


object ValidType {
    const val NO_MATCH_MODULE_PROJECT = "no-match-module-project"
    const val NO_SET_URL = "no-set-url"
    const val NO_TOKEN = "no-token"
    const val TO_JSON_FAILED = "to-json-failed"
    const val SUCCESS = "success"
}