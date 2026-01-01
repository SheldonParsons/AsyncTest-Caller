package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.resolver
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import io.swagger.v3.oas.models.tags.Tag
class ClassNodeResolver {
    fun registerController(classNode: ApiNode, context: OpenApiBuildContext): String {
        var tagName = classNode.name
        var tagDesc = classNode.desc
        val docInfo: DocInfo = classNode.docs as DocInfo
        if (docInfo.title.isNotEmpty()) {
            tagName = docInfo.title
        }
        if (docInfo.description.isNotEmpty()) {
            tagDesc = docInfo.description
        }
        docInfo.apiInfo?.let { apiInfo ->
            if (apiInfo.qualifiedName.isNotEmpty()) {
                tagName = apiInfo.qualifiedName
            }
            if (apiInfo.tags.isNotEmpty()) {
                tagName = apiInfo.tags.first()
            }
            if (apiInfo.description.isNotEmpty()) {
                tagDesc = apiInfo.description
            }
        }
        val existedName = context.openAPI.tags
            ?.firstOrNull { it.name == tagName }
            ?.name
        if (existedName != null) return existedName
        val tag = Tag()
            .name("${tagName}(${classNode.path})")
            .description(tagDesc)
        context.openAPI.addTagsItem(tag)
        return tagName
    }
}