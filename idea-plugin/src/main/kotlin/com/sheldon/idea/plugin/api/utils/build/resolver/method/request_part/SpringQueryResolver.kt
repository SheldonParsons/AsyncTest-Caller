package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode

import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter.SimpleTypeResolver

class SpringQueryResolver : RequestPartResolver {

    override fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest): ApiRequest {
        if (variable.t !== null) {
            val paramList = buildTree(variable.t, variable)
            if (!paramList.isNullOrEmpty()) {
                apiRequest.query = ResolverHelper.mergeHeadersOrParams(apiRequest.query, paramList, distinct = false)
                ResolverHelper.addOrUpdateElement(
                    apiRequest.headers,
                    AsyncTestVariableNode(
                        type = "string",
                        name = SpringClassName.CONTENT_TYPE,
                        defaultValue = SpringClassName.APPLICATION_JSON
                    )
                )
            }
        }
        return apiRequest
    }

    fun buildTree(
        psiType: PsiType, variable: ParamAnalysisResult
    ): MutableList<AsyncTestVariableNode>? {
        if (TypeUtils.isGeneralObject(psiType)) return null
        val typeStr = TypeUtils.mapToAsyncType(psiType)
        val paramList = mutableListOf<AsyncTestVariableNode>()
        if (typeStr == AsyncTestType.DS) {
            val resolveResult = PsiUtil.resolveGenericsClassInType(psiType)
            val psiClass = resolveResult.element
            if (psiClass != null) {
                for (field in psiClass.allFields) {
                    if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                        continue
                    }

                    // 4. Spring/内部注解过滤 (关键修复)
                    if (field.hasAnnotation(SpringClassName.SPRING_ANN_AUTOWIRED) || field.hasAnnotation(SpringClassName.JAVAX_ANN_RESOURCE) || field.hasAnnotation(
                            SpringClassName.JAKARTA_ANN_RESOURCE
                        ) || field.hasAnnotation(SpringClassName.SPRING_ANN_VALUE)
                    ) {
                        continue
                    }
                    if (SimpleTypeResolver().isSimpleType(field.type)) {
                        val textNode = AsyncTestVariableNode(
                            type = typeStr,
                            name = field.name,
                            defaultValue = variable.defaultValue ?: "",
                            required = variable.isRequired
                        )
                        val fieldComment = ResolverHelper.getElementComment(field)
                        if (fieldComment.isNotEmpty()) {
                            textNode.statement = fieldComment
                        }
                        paramList.add(textNode)
                    } else if (ResolverHelper.isMultipartFile(field.type)) {
                        val fileNode = AsyncTestVariableNode(
                            type = typeStr,
                            name = field.name,
                            defaultValue = variable.defaultValue ?: "",
                            required = variable.isRequired
                        )
                        val fieldComment = ResolverHelper.getElementComment(field)
                        if (fieldComment.isNotEmpty()) {
                            fileNode.statement = fieldComment
                        }
                        paramList.add(fileNode)
                    }
                }
            }
        } else if (typeStr in listOf(
                AsyncTestType.STRING,
                AsyncTestType.INTEGER,
                AsyncTestType.BOOLEAN,
                AsyncTestType.NUMBER,
                AsyncTestType.NULL
            )
        ) {
            val textNode = AsyncTestVariableNode(
                type = typeStr,
                name = variable.name,
                defaultValue = variable.defaultValue ?: "",
                required = variable.isRequired
            )
            paramList.add(textNode)
        } else if (typeStr == AsyncTestType.ARRAY) {
            val textNode = AsyncTestVariableNode(
                type = typeStr,
                name = variable.name,
                childList = arrayListOf(""),
                defaultValue = variable.defaultValue ?: "",
                required = variable.isRequired
            )
            paramList.add(textNode)
        }
        return paramList
    }
}