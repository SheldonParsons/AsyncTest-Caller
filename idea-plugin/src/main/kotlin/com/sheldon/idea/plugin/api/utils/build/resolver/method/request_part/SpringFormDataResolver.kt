package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode

import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.AsyncTestFormData
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.CommonUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.method.parameter.SimpleTypeResolver

class SpringFormDataResolver : RequestPartResolver {

    override fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest, module: Module): ApiRequest {
        // 获取boundary
        val boundary = CommonUtils.getBoundaryString()
        if (variable.t !== null) {
            val formDataList = buildTree(variable.t, "root")
            if (!formDataList.isNullOrEmpty()) {
                apiRequest.formData = AsyncTestFormData(boundary, formDataList)
//                ResolverHelper.addOrUpdateElement(
//                    apiRequest.headers,
//                    AsyncTestVariableNode(
//                        type = "string",
//                        name = SpringClassName.CONTENT_TYPE,
//                        defaultValue = "${CommonConstant.DEFAULT_FORM_DATA_HEADER_CONTENT_TYPE_PREFIX}${boundary}"
//                    )
//                )
                apiRequest.bodyType = AsyncTestBodyType.FORM_DATA
            }
        }
        return apiRequest
    }


    fun buildTree(
        psiType: PsiType, name: String
    ): MutableList<AsyncTestVariableNode>? {
        if (isGeneralObject(psiType)) return null
        val typeStr = mapToAsyncType(psiType)
        val formDataList = mutableListOf<AsyncTestVariableNode>()
        if (typeStr == AsyncTestType.FILES) {
            val fileNode = AsyncTestVariableNode(
                type = typeStr,
                name = name,
                contentType = CommonConstant.DEFAULT_FORM_DATA_FILE_FIELD_CONTENT_TYPE
            )
            formDataList.add(fileNode)
        } else if (typeStr == AsyncTestType.DS) {
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
                            name = name,
                            contentType = CommonConstant.DEFAULT_FORM_DATA_FIELD_CONTENT_TYPE
                        )
                        val fieldComment = ResolverHelper.getElementComment(field)
                        if (fieldComment.isNotEmpty()) {
                            textNode.statement = fieldComment
                        }
                        formDataList.add(textNode)
                    } else if (ResolverHelper.isMultipartFile(field.type)) {
                        val fileNode = AsyncTestVariableNode(
                            type = typeStr,
                            name = name,
                            contentType = CommonConstant.DEFAULT_FORM_DATA_FILE_FIELD_CONTENT_TYPE
                        )
                        val fieldComment = ResolverHelper.getElementComment(field)
                        if (fieldComment.isNotEmpty()) {
                            fileNode.statement = fieldComment
                        }
                        formDataList.add(fileNode)
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
                name = name,
                contentType = CommonConstant.DEFAULT_FORM_DATA_FIELD_CONTENT_TYPE
            )
            formDataList.add(textNode)
        } else if (typeStr == AsyncTestType.ARRAY) {
            val textNode = AsyncTestVariableNode(
                type = typeStr,
                name = name,
                childList = mutableListOf(""),
                contentType = CommonConstant.DEFAULT_FORM_DATA_FIELD_CONTENT_TYPE
            )
            formDataList.add(textNode)
        }
        return formDataList
    }
}