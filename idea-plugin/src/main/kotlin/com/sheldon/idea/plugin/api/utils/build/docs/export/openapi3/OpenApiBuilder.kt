package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser.ControllerParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.core.util.Json
class OpenApiBuilder {
    private val context = OpenApiBuildContext()
    fun build(controllers: List<ApiNode>): String {
        context.openAPI.info(
            Info()
                .title("OpenApi 3.1 文档")
                .version("1.0.0")
                .description("AsyncTest Caller OpenAPI 3 Documentation")
        )
        val controllerParser = ControllerParser(context)
        controllers.forEach {
            controllerParser.parse(it)
        }
        val openAPI = context.openAPI
        return Json.pretty(openAPI)
    }
}
