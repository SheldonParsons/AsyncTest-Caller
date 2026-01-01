package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.Schema
class OpenApiBuildContext {
    val openAPI: OpenAPI = OpenAPI()
        .components(Components())
        .paths(Paths())
    /** 已注册的 schema，防止重复 */
    val registeredSchemas: MutableSet<String> = mutableSetOf()
    fun registerSchema(name: String, schema: Schema<*>) {
        if (registeredSchemas.add(name)) {
            openAPI.components.addSchemas(name, schema)
        }
    }
}
