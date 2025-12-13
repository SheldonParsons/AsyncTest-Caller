package com.sheldon.idea.plugin.api.utils

import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import java.math.BigInteger
import java.security.MessageDigest

/**
 * 计算安全且极速的 Hash (MD5流式 + Base36压缩)
 * 结果长度: 约 25 位
 * 碰撞概率: 忽略不计 (绝对安全)
 */
fun ApiRequest.calculateSafeHash(): String {
    // 使用 MD5 保证唯一性
    val md = MessageDigest.getInstance("MD5")

    // 辅助函数: 直接更新 bytes，避免创建 String 对象
    fun update(str: String?) {
        if (!str.isNullOrEmpty()) {
            md.update(str.toByteArray())
        }
        md.update(0) // 分隔符
    }

    fun updateBool(b: Boolean) {
        md.update(if (b) 1 else 0)
    }

    // 1. Headers (排序保证稳定性)
    this.headers.sortedBy { it.name }.forEach {
        update(it.name)
        updateBool(it.required)
        update(it.defaultValue)
    }

    // 2. Query (排序)
    this.query.sortedBy { it.name }.forEach {
        update(it.type)
        update(it.name)
        updateBool(it.required)
        update(it.defaultValue)
    }

    // 3. BodyType
    val type = this.bodyType ?: "none"
    update(type)

    // 4. Body Content
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

    // 5. 【关键优化】将 16字节的 MD5 转为 Base36 字符串
    // 1 表示正数
    return BigInteger(1, md.digest()).toString(36)
}