package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiType
import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver.TypeResolver
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.RequestBody
class RequestBodyParser(
    private val context: OpenApiBuildContext
) {
    fun parse(methodNode: ApiNode, request: ApiRequest): RequestBody? {
        if (request.bodyType == AsyncTestBodyType.FORM_DATA) {
            val nodeList = request.formData.data
            val content = Content()
            nodeList.forEach {
                val schema = TypeResolver.resolveSchema(it, context)
                content.addMediaType(
                    "multipart/form-data",
                    MediaType().schema(schema)
                )
            }
            return RequestBody().content(content)
        } else if (request.bodyType == AsyncTestBodyType.JSON && request.json.isNotEmpty()) {
            val schema = TypeResolver.resolveSchema(request.json.first(), context)
            return RequestBody()
                .content(
                    Content().addMediaType(
                        "application/json",
                        MediaType().schema(schema)
                    )
                )
        }
        return null
    }
}
