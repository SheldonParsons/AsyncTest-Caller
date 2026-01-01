package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser
import com.intellij.psi.PsiClass
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver.TypeResolver
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils.getDsNode
import io.swagger.v3.oas.models.media.Schema
class SchemaParser(
    private val context: OpenApiBuildContext
) {
    fun parse(node: AsyncTestVariableNode): Schema<*> {
        val schema = Schema<Any>()
            .type("object").description(node.statement)
        if (node.children.isEmpty()) {
            getDsNode(node)?.let {
                it.children.forEach { childNode ->
                    schema.addProperty(
                        childNode.name,
                        TypeResolver.resolveSchema(childNode, context)
                    )
                }
            }
        } else {
            node.children.forEach { node ->
                schema.addProperty(
                    node.name,
                    TypeResolver.resolveSchema(node, context)
                )
            }
        }
        return schema
    }
}
