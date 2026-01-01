package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver.ClassNodeResolver
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils.resolveHttpMethod
import io.swagger.v3.oas.models.PathItem
class ControllerParser(
    private val context: OpenApiBuildContext
) {
    private val operationParser = OperationParser(context)
    fun parse(classNode: ApiNode) {
        val tagName = ClassNodeResolver().registerController(classNode, context)
        classNode.children.forEach { method ->
            parseMethod(method, tagName)
        }
    }
    private fun parseMethod(methodNode: ApiNode, tagName: String) {
        val fullPath = methodNode.path?.replace("//", "/")
        val pathItem = context.openAPI.paths
            .getOrDefault(fullPath, PathItem())
        operationParser.attachOperation(
            pathItem,
            resolveHttpMethod(methodNode.method) ?: PathItem.HttpMethod.GET,
            methodNode,
            tagName
        )
        context.openAPI.paths.addPathItem(fullPath, pathItem)
    }
}
