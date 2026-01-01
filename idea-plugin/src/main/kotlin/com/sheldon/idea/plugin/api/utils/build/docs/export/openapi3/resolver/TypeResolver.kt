package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser.SchemaParser
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils.buildSchemaByAsyncType
import io.swagger.v3.oas.models.media.*
object TypeResolver {
    fun resolveSchema(node: AsyncTestVariableNode, context: OpenApiBuildContext): Schema<*> {
        val schemaType = buildSchemaByAsyncType(node.type)
        if (node.type !in listOf(AsyncTestType.DS, AsyncTestType.NULL, AsyncTestType.ARRAY, AsyncTestType.OBJECT)) {
            schemaType.description(node.statement)
            return schemaType
        } else if (node.type == AsyncTestType.DS) {
            return resolveObjectSchema(node, context)
        } else if (node.type == AsyncTestType.OBJECT) {
            return ObjectSchema().items(resolveSchema(node.children.first(), context))
        } else if (node.type == AsyncTestType.ARRAY) {
            return ArraySchema().items(resolveSchema(node.children.first(), context))
        } else {
            return StringSchema()
        }
    }
    private fun resolveObjectSchema(node: AsyncTestVariableNode, context: OpenApiBuildContext): Schema<*> {
        if (!context.registeredSchemas.contains(node.dsTarget)) {
            val schema = SchemaParser(context).parse(node)
            context.registerSchema(node.dsTarget ?: "", schema)
        }
        return Schema<Any>().`$ref`("#/components/schemas/${node.dsTarget ?: ""}")
    }
}
