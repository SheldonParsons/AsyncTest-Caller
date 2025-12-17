package com.sheldon.idea.plugin.api.method

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
    // ✅ 1. 全部改成 var
    // ✅ 2. 使用 @Attribute 把它变成 <node t="string"> 这样更紧凑，或者用 @Tag 变成子标签
    @Attribute("t")
    var type: String = "",

    @Attribute("name")
    var name: String = "",

    // 建议默认值给 0 或 -1，避免序列化干扰。创建新对象时再赋值。
    @Attribute("id")
    var id: Int = 0,

    @Attribute("default")
    var defaultValue: String = "",

    @Attribute("statement")
    var statement: String = "",

    @Attribute("required")
    var required: Boolean = false,

    @Attribute("content_type")
    var contentType: String = "",

    // ✅ 3. 集合字段
    // 使用 @Tag 和 @XCollection 让 XML 结构更好看
    @Tag("children")
    @XCollection(style = XCollection.Style.v2) // v2 风格通常更简洁
    var children: ArrayList<AsyncTestVariableNode> = ArrayList(),

    @Attribute("ds_target")
    var dsTarget: String? = "",

    @Tag("child_list")
    @XCollection(style = XCollection.Style.v2)
    var childList: ArrayList<String> = ArrayList(),

    @Tag("file_list")
    @XCollection(style = XCollection.Style.v2)
    var fileList: ArrayList<String> = ArrayList(),
) {
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