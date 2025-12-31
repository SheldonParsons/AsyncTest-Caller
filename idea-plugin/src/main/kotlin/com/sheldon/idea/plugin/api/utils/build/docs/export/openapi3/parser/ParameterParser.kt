package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser

import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils.buildSchemaByAsyncType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.Operation

class ParameterParser(
    private val context: OpenApiBuildContext
) {

    fun parseQuery(methodNode: ApiNode, query: MutableList<AsyncTestVariableNode>, operation: Operation) {
        query.forEach {
            if (it.type !in listOf(AsyncTestType.DS, AsyncTestType.NULL)) {
                val schemaType = buildSchemaByAsyncType(it.type)
                val parameter = Parameter()
                    .name(it.name)
                    .`in`("query")
                    .description(it.statement)
                    .schema(schemaType)
                operation.addParametersItem(parameter)
            }
        }
    }

    fun parseHeader(methodNode: ApiNode, headers: MutableList<AsyncTestVariableNode>, operation: Operation) {
        println("headers:${headers}")
        headers.forEach {
            if (it.type !in listOf(AsyncTestType.DS, AsyncTestType.NULL, AsyncTestType.ARRAY)) {
                val schemaType = buildSchemaByAsyncType(it.type)
                val parameter = Parameter()
                    .name(it.name)
                    .`in`("headers")
                    .description(it.statement)
                    .schema(schemaType)
                operation.addParametersItem(parameter)
            }
        }
    }
}
