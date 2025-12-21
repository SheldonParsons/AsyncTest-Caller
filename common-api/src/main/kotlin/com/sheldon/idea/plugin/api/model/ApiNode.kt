package com.sheldon.idea.plugin.api.model

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.annotations.Transient

/**
 * 对应你要求的 JSON 结构
 */
@Tag("node")
data class ApiNode(
    var type: Int = 0,
    var child_type: Int = 0,
    var code_type: Int = 0,
    var count: Int = 0,
    var method: String? = null,
    var name: String = "",
    var alias: String? = null,
    var desc: String? = null,
    var tree_path: String = "",
    @XCollection(propertyElementName = "child")
    var children: ArrayList<ApiNode> = arrayListOf(),
    var request: String? = null,
    @Transient
    var classRequest: ApiRequest? = null,
    var path: String? = null,
    var hash: String = ""
) {
    fun addChild(node: ApiNode) {
        children.add(node)
    }

    fun traverseFindRequest(callback: (String?) -> Unit) {
        // 1. 检查自己 (包括它自己)
        if (this.code_type == 3) {
            callback(this.request)
        }

        // 2. 递归检查所有子节点
        // 使用 forEach 遍历 children 列表
        this.children.forEach { child ->
            child.traverseFindRequest(callback)
        }
    }

    /**
     * 辅助数据类，用于解析路径片段
     */
    private data class PathSegment(val name: String, val codeType: Int)

    /**
     * 1. 通过字符串找到节点和父级节点
     *
     * @param pathStr 例如 "demo[0].child[1].grandchild[2]"
     * @return 返回 Pair(目标节点, 父节点)。如果是根节点，父节点为 null。如果没找到，返回 null。
     */
    fun findNodeWithParent(pathStr: String): Pair<ApiNode, ApiNode?>? {
        val segments = parsePathString(pathStr)
        if (segments.isEmpty()) return null

        // 1. 检查根节点是否匹配路径的第一段
        // 如果当前节点(this)的名字或code_type不匹配路径的起始部分，说明根本不在这个树里
        val rootSegment = segments[0]
        if (!isMatch(this, rootSegment)) {
            return null
        }

        var currentNode: ApiNode = this
        var parentNode: ApiNode? = null

        // 2. 从路径的第二段开始向下遍历
        for (i in 1 until segments.size) {
            val targetSegment = segments[i]

            // 在子节点中查找
            val foundChild = currentNode.children.find { child ->
                isMatch(child, targetSegment)
            }

            if (foundChild != null) {
                parentNode = currentNode
                currentNode = foundChild
            } else {
                // 路径中断，未找到
                return null
            }
        }

        return currentNode to parentNode
    }

    /**
     * 2. 通过字符串删除一个节点，并且传入一个新的节点插入到删除的节点的位置
     *
     * @param pathStr 目标节点的路径
     * @param newNode 新的节点
     * @return Boolean 是否替换成功
     */
    fun replaceNodeByPath(pathStr: String, newNode: ApiNode): Int {
        // 使用上面的查找函数找到目标和父级
        val result = findNodeWithParent(pathStr) ?: return 0
        val (targetNode, parentNode) = result

        // 如果 parentNode 为 null，说明要替换的是根节点自己 (this)
        if (parentNode == null) {
            return 2
        }

        // 找到目标节点在父级 children 列表中的索引
        val index = parentNode.children.indexOf(targetNode)
        if (index != -1) {
            // 替换逻辑：设置到原位置
            parentNode.children[index] = newNode
            return 1
        }

        return 0
    }

    // ==========================================
    // 私有辅助方法
    // ==========================================

    /**
     * 判断节点是否与片段匹配
     */
    private fun isMatch(node: ApiNode, segment: PathSegment): Boolean {
        return node.name == segment.name && node.code_type == segment.codeType
    }

    /**
     * 解析路径字符串
     * 输入: "A[0].B[1]"
     * 输出: [PathSegment("A", 0), PathSegment("B", 1)]
     */
    private fun parsePathString(pathStr: String): List<PathSegment> {
        if (pathStr.isBlank()) return emptyList()

        // 按点分割
        val rawSegments = pathStr.split(".")
        val result = ArrayList<PathSegment>()

        // 正则表达式匹配 name[code]
        // ^(.*) 匹配名字 (考虑到名字里可能有特殊字符，贪婪匹配到最后一个 [ 之前)
        // \[(\d+)\]$ 匹配结尾的 [数字]
        val regex = Regex("^(.*)\\[(\\d+)\\]$")

        for (segment in rawSegments) {
            val matchResult = regex.find(segment)
            if (matchResult != null && matchResult.groupValues.size >= 3) {
                val name = matchResult.groupValues[1]
                val code = matchResult.groupValues[2].toInt()
                result.add(PathSegment(name, code))
            } else {
                // 如果格式不对，可以抛出异常或忽略，这里选择打印错误并返回空以终止
                System.err.println("路径格式错误: $segment")
                return emptyList()
            }
        }
        return result
    }
}