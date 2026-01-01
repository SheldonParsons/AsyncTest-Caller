package com.sheldon.idea.plugin.api.utils.build.docs.extractor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.docs.DocExtractor
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.ExtractionContext
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiImplicitParamParser
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiModelInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiModelParser
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiModelPropertyInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiModelPropertyParser
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiOperationParser
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiParamInfo
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiParamParser
import com.sheldon.idea.plugin.api.utils.build.docs.utils.ApiParser
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.findAnnotationInHierarchy
class SwaggerExtractor : DocExtractor {
    override fun getOrder() = 20
    override fun extract(context: ExtractionContext, currentDoc: DocInfo, codeType: CodeType, hasDocs: Boolean) {
        val element = context.targetElement
        val paramMetadata = context.paramMetadata
        var swaggerTitle: String? = null
        var swaggerDesc: String? = null
        // 1. 处理类 (@Api)
        if (isClass(codeType, element)) {
            swaggerTitle = handleApi(element as PsiClass, currentDoc, hasDocs)
            // 1. 处理接口方法
        } else if (isMethod(codeType, element)) {
            val (title, desc) = handleApiOperation(element as PsiMethod, currentDoc, hasDocs, paramMetadata)
            swaggerTitle = title
            swaggerDesc = desc
        } else if (isPojoClass(codeType, element)) {
            val (title, desc) = handleApiModel(element as PsiClass, currentDoc, hasDocs)
            swaggerTitle = title
            swaggerDesc = desc
        } else if (isPojoField(codeType, element)) {
            val (title, _) = handleApiModelProperty(element as PsiField, currentDoc, hasDocs)
            swaggerTitle = title
        } else if (isParam(codeType, element)) {
            val (title, _) = handleApiParam(element as PsiParameter, currentDoc, hasDocs)
            swaggerTitle = title
        }
        currentDoc.merge(swaggerTitle, swaggerDesc)
    }
    private fun handleApi(psiClass: PsiClass, currentDoc: DocInfo, hasDocs: Boolean): String {
        val annotation: PsiAnnotation = getAnnotation(psiClass, SpringClassName.SWAGGER_API) ?: return ""
        val apiInfo = ApiParser.parse(annotation)
        if (apiInfo.value.isEmpty()) {
            apiInfo.value = psiClass.qualifiedName.toString()
        }
        if (hasDocs) {
            apiInfo.qualifiedName = psiClass.qualifiedName ?: ""
            currentDoc.apiInfo = apiInfo
        }
        return apiInfo.tags.first()
    }
    private fun handleApiOperation(
        psiMethod: PsiMethod, currentDoc: DocInfo, hasDocs: Boolean, paramMetadata: MutableMap<String, DocInfo>
    ): Pair<String, String> {
        val annotation: PsiAnnotation =
            getAnnotation(psiMethod, SpringClassName.SWAGGER_API_OPERATION) ?: return Pair("", "")
        val apiOperationInfo = ApiOperationParser.parse(annotation)
        val paramAnnotation = getAnnotation(psiMethod, SpringClassName.SWAGGER_API_IMPLICIT_PARAM)
        val paramDataList = ApiImplicitParamParser.parse(paramAnnotation)
        val paramAnnotations = getAnnotation(psiMethod, SpringClassName.SWAGGER_API_IMPLICIT_PARAMS)
        val paramsDataList = ApiImplicitParamParser.parse(paramAnnotations)
        paramDataList.forEach { paramData ->
            paramMetadata[paramData.name] = DocInfo(apiImplicitParamInfo = paramData)
        }
        paramsDataList.forEach { paramData ->
            paramMetadata[paramData.name] = DocInfo(apiImplicitParamInfo = paramData)
        }
        if (hasDocs) {
            currentDoc.apiOperationInfo = apiOperationInfo
        }
        return Pair(apiOperationInfo.title, apiOperationInfo.desc)
    }
    private fun handleApiModel(
        psiMethod: PsiClass, currentDoc: DocInfo, hasDocs: Boolean
    ): Pair<String, String> {
        val annotation: PsiAnnotation =
            getAnnotation(psiMethod, SpringClassName.SWAGGER_API_MODEL) ?: return Pair("", "")
        val apiModelInfo: ApiModelInfo = ApiModelParser.parse(annotation)
        if (hasDocs) {
            currentDoc.apiModelInfo = apiModelInfo
        }
        return Pair(apiModelInfo.name, apiModelInfo.description)
    }
    private fun handleApiModelProperty(
        psiMethod: PsiField, currentDoc: DocInfo, hasDocs: Boolean
    ): Pair<String, String> {
        val annotation: PsiAnnotation =
            getAnnotation(psiMethod, SpringClassName.SWAGGER_API_MODEL_PROPERTY) ?: return Pair("", "")
        val apiModelPropertyInfo: ApiModelPropertyInfo = ApiModelPropertyParser.parse(annotation)
        if (hasDocs) {
            currentDoc.apiApiModelPropertyInfo = apiModelPropertyInfo
        }
        return Pair(
            apiModelPropertyInfo.title, ""
        )
    }
    private fun handleApiParam(
        psiMethod: PsiParameter, currentDoc: DocInfo, hasDocs: Boolean
    ): Pair<String, String> {
        val annotation: PsiAnnotation =
            getAnnotation(psiMethod, SpringClassName.SWAGGER_API_PARAM) ?: return Pair("", "")
        val apiParamInfo: ApiParamInfo = ApiParamParser.parse(annotation)
        if (hasDocs) {
            currentDoc.apiParamInfo = apiParamInfo
        }
        return Pair(
            apiParamInfo.name, ""
        )
    }
    // 辅助方法：你需要实现基于 PSI 获取注解值的逻辑
    private fun getAnnotation(
        element: PsiElement, fqn: String
    ): PsiAnnotation? {
        if (element is PsiClass || element is PsiMethod) {
            return AnnotationResolver.findAnnotationInHierarchy(element, fqn)
        } else if (element is PsiField) {
            return element.getAnnotation(fqn)
        } else if (element is PsiParameter) {
            return element.findAnnotationInHierarchy(fqn)
        }
        return null
    }
    private fun isClass(codeType: CodeType, element: PsiElement): Boolean {
        return codeType == CodeType.CLASS && element is PsiClass
    }
    private fun isMethod(codeType: CodeType, element: PsiElement): Boolean {
        return codeType == CodeType.METHOD && element is PsiMethod
    }
    private fun isParam(codeType: CodeType, element: PsiElement): Boolean {
        return codeType == CodeType.PARAM && element is PsiParameter
    }
    private fun isPojoClass(codeType: CodeType, element: PsiElement): Boolean {
        return codeType == CodeType.POJO_CLASS && element is PsiClass
    }
    private fun isPojoField(codeType: CodeType, element: PsiElement): Boolean {
        return codeType == CodeType.POJO_FIELD && element is PsiField
    }
}