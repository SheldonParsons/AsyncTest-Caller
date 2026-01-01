package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver
import com.sheldon.idea.plugin.api.model.ApiNode
import io.swagger.v3.oas.models.Operation
class MethodNodeResolver {
    fun registerMethod(methodNode: ApiNode, operation: Operation) {
        operation.description(methodNode.desc).summary(methodNode.alias)
    }
}