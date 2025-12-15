package com.sheldon.idea.plugin.api.utils

import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.DataStructure
import java.math.BigInteger
import java.security.MessageDigest

/**
 * 计算安全且极速的 Hash (MD5流式 + Base36压缩)
 * 结果长度: 约 25 位
 * 碰撞概率: 忽略不计
 */
fun ApiRequest.calculateSafeHash(): String {

    val md = MessageDigest.getInstance("MD5")

    fun update(str: String?) {
        if (!str.isNullOrEmpty()) {
            md.update(str.toByteArray())
        }
        md.update(0) // 分隔符
    }

    fun updateBool(b: Boolean) {
        md.update(if (b) 1 else 0)
    }

    this.headers.sortedBy { it.name }.forEach {
        update(it.name)
        updateBool(it.required)
        update(it.defaultValue)
    }

    this.query.sortedBy { it.name }.forEach {
        update(it.type)
        update(it.name)
        updateBool(it.required)
        update(it.defaultValue)
    }

    val type = this.bodyType ?: "none"
    update(type)

    when (type) {
        "form-data" -> {
            this.formData.data.forEach {
                update(it.type)
                update(it.name)
                update(it.contentType)
            }
        }

        "json" -> {
            fun processJsonNode(node: AsyncTestVariableNode) {
                update(node.type)
                update(node.name)
                update(node.dsTarget)
                update(node.statement)

                md.update(2) // 子节点开始符
                node.children.forEach { child ->
                    processJsonNode(child)
                }
                md.update(3) // 子节点结束符
            }
            this.json.forEach { processJsonNode(it) }
        }
    }


    return BigInteger(1, md.digest()).toString(36)
}

fun DataStructure.calculateSafeHash(): String {
    if (this.data.isEmpty()) {
        return "empty" // 或者 return ""，视你的业务而定
    }
    val md = MessageDigest.getInstance("MD5")
    fun update(str: String?) {
        if (!str.isNullOrEmpty()) {
            md.update(str.toByteArray())
        }
        md.update(0) // 分隔符
    }

    fun processJsonNode(node: AsyncTestVariableNode) {
        update(node.type)
        update(node.name)
        update(node.dsTarget)
        update(node.statement)
        update(if (node.required) "1" else "0")

        md.update(2) // 子节点开始符
        node.children.forEach { child ->
            processJsonNode(child)
        }
        md.update(3) // 子节点结束符
    }
    processJsonNode(this.data[0])
    return BigInteger(1, md.digest()).toString(36)
}