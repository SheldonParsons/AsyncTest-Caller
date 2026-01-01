package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser
import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver.MethodNodeResolver
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils.getRequest
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
class OperationParser(
    private val context: OpenApiBuildContext
) {
    private val parameterParser = ParameterParser(context)
    private val requestBodyParser = RequestBodyParser(context)
    private val responseParser = ResponseParser(context)
    fun attachOperation(
        pathItem: PathItem,
        httpMethod: PathItem.HttpMethod,
        methodNode: ApiNode,
        tagName: String
    ) {
        val operation = Operation()
        operation.addTagsItem(tagName)
        MethodNodeResolver().registerMethod(methodNode, operation)
        val request = getRequest(methodNode)
        if (request != null) {
            parameterParser.parseQuery(methodNode, request.query, operation)
            parameterParser.parseHeader(methodNode, request.headers, operation)
            requestBodyParser.parse(methodNode, request)?.let {
                operation.requestBody(it)
            }
        }
        operation.responses(
            responseParser.parse(methodNode)
        )
        when (httpMethod) {
            PathItem.HttpMethod.GET -> pathItem.get(operation)
            PathItem.HttpMethod.POST -> pathItem.post(operation)
            PathItem.HttpMethod.PUT -> pathItem.put(operation)
            PathItem.HttpMethod.DELETE -> pathItem.delete(operation)
            PathItem.HttpMethod.PATCH -> pathItem.patch(operation)
            else -> {}
        }
    }
}
