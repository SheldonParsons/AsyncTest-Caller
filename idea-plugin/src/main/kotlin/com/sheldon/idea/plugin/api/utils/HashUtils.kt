package com.sheldon.idea.plugin.api.utils
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.DataStructure
import java.math.BigInteger
import java.security.MessageDigest

fun ApiRequest.calculateSafeHash(): String {
    val md = MessageDigest.getInstance("MD5")
    fun update(str: String?) {
        if (!str.isNullOrEmpty()) {
            md.update(str.toByteArray())
        }
        md.update(0) 
    }
    fun updateBool(b: Boolean) {
        md.update(if (b) 1 else 0)
    }
    update(this.name)
    update(this.alias)
    update(this.desc)
    update(this.method)
    update(this.path)
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
                update(node.defaultValue)
                md.update(2) 
                node.children.forEach { child ->
                    processJsonNode(child)
                }
                md.update(3) 
            }
            this.json.forEach { processJsonNode(it) }
        }
    }
    return BigInteger(1, md.digest()).toString(36)
}
fun DataStructure.calculateSafeHash(): String {
    if (this.data.isEmpty()) {
        return "empty" 
    }
    val md = MessageDigest.getInstance("MD5")
    fun update(str: String?) {
        if (!str.isNullOrEmpty()) {
            md.update(str.toByteArray())
        }
        md.update(0) 
    }
    fun processJsonNode(node: AsyncTestVariableNode) {
        update(node.type)
        update(node.name)
        update(node.dsTarget)
        update(node.statement)
        update(node.contentType)
        update(if (node.required) "1" else "0")
        md.update(2) 
        node.children.forEach { child ->
            processJsonNode(child)
        }
        md.update(3) 
    }
    processJsonNode(this.data[0])
    return BigInteger(1, md.digest()).toString(36)
}