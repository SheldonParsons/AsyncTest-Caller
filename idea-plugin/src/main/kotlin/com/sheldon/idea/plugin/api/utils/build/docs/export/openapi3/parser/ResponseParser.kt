package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
class ResponseParser(
    private val context: OpenApiBuildContext
) {
    fun parse(methodNode: ApiNode): ApiResponses {
        val responses = ApiResponses()
        // 200 OK（通用成功返回）
        responses.addApiResponse(
            "200", ApiResponse().description("OK").content(
                Content().addMediaType(
                    "application/json", MediaType().schema(ObjectSchema())
                )
            )
        )
        return responses
    }
}
