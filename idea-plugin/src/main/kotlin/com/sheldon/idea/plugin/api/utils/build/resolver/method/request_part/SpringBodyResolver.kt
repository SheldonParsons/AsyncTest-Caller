package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.util.PsiUtil
import com.sheldon.idea.plugin.api.method.AsyncTestBodyType
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.model.DataStructure
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
import com.sheldon.idea.plugin.api.utils.build.lifecycle.AfterNode
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
import com.sheldon.idea.plugin.api.utils.calculateSafeHash

class SpringBodyResolver(val module: Module) :
    RequestPartResolver {
    private var privateHasDoc: Boolean = false
    private var paramDoc: DocInfo? = null
    override fun push(
        variable: ParamAnalysisResult,
        apiRequest: ApiRequest,
        implicitParams: MutableMap<String, DocInfo>,
        hasDocs: Boolean
    ): ApiRequest {
        /**
         * 1、获取持久化缓存信息
         * 2、将创建的dto对象更新进去ProjectCacheService.saveOrUpdateSingleRequest
         * 3、将dto加入到ProjectCacheService.addReferToDsPool
         * */
        privateHasDoc = hasDocs
        if (apiRequest.json.isNotEmpty()) {
            return apiRequest
        }
        if (variable.t !== null) {
            val currentSessionIds = mutableSetOf<String>()
            paramDoc = variable.docInfo
            val rootNode =
                buildTree(variable.t, "root", currentSessionIds, isRoot = true, implicitParams = implicitParams)
            if (rootNode != null && rootNode.type in listOf(
                    AsyncTestType.ARRAY, AsyncTestType.DS, AsyncTestType.OBJECT
                )
            ) {
                apiRequest.json = mutableListOf(rootNode)
                ResolverHelper.addOrUpdateElement(
                    apiRequest.headers,
                    AsyncTestVariableNode(
                        type = "string",
                        name = SpringClassName.CONTENT_TYPE,
                        defaultValue = SpringClassName.APPLICATION_JSON
                    )
                )
                apiRequest.bodyType = AsyncTestBodyType.JSON
            }
        }
        return apiRequest
    }

    fun buildTree(
        psiType: PsiType,
        name: String,
        sessionIds: MutableSet<String>,
        depth: Int = 0,
        isRoot: Boolean = false,
        originElement: PsiElement? = null,
        implicitParams: MutableMap<String, DocInfo> = mutableMapOf()
    ): AsyncTestVariableNode? {
        if (TypeUtils.isGeneralObject(psiType)) return null
        val typeStr = TypeUtils.mapToAsyncType(psiType)
        if (typeStr == AsyncTestType.FILES) return null
        val node = AsyncTestVariableNode(type = typeStr, name = name)
        if (originElement != null) {
            val (docInfo, _) = DocResolver().resolve(
                originElement,
                mutableMapOf(),
                CodeType.POJO_FIELD,
                hasDocs = privateHasDoc
            )
            val fieldComment = docInfo.title
            if (fieldComment.isNotEmpty()) {
                node.statement = fieldComment
            }
        }
        if (typeStr == AsyncTestType.DS) {
            val resolveResult = PsiUtil.resolveGenericsClassInType(psiType)
            val psiClass = resolveResult.element ?: return node
            node.dsTarget = psiType.canonicalText
            node.contentType = psiType.presentableText
            val dsTargetId = node.dsTarget ?: ""
            if (node.statement.isEmpty()) {
                val (docInfo, _) = DocResolver().resolve(
                    psiClass,
                    mutableMapOf(),
                    CodeType.POJO_CLASS,
                    hasDocs = privateHasDoc
                )
                val classComment = docInfo.title
                if (classComment.isNotEmpty()) {
                    node.statement = classComment
                }
            }
            if (isRoot) {
                node.name = psiType.presentableText
                val paramDesc = getImplicitParamDesc(implicitParams, name)
                if (paramDesc != null) {
                    node.statement = paramDesc
                }
                paramDoc?.let {
                    if (it.title.isNotEmpty()) {
                        node.statement = it.title
                    }
                }
                sessionIds.add(dsTargetId)
                parsePojoFields(psiClass, resolveResult.substitutor, node, sessionIds)
                val ds = DataStructure(
                    alias = psiType.presentableText,
                    data = mutableListOf(node)
                )
                ds.hash = ds.calculateSafeHash()
                AfterNode.execute(module, dsTargetId, ds)
                return node
            } else {
                if (!sessionIds.contains(dsTargetId)) {
                    sessionIds.add(dsTargetId)
                    buildAndSaveDefinition(psiClass, dsTargetId, resolveResult.substitutor, psiType, sessionIds)
                }
                return node
            }
        }
        if (depth > 20) return node
        when (typeStr) {
            AsyncTestType.ARRAY -> {
                val componentType = extractArrayComponentType(psiType)
                if (componentType != null) {
                    val newNode = buildTree(componentType, "item", sessionIds, depth + 1)
                    if (newNode != null) {
                        node.children.add(newNode)
                    }
                }
            }

            AsyncTestType.OBJECT -> {
                if (TypeUtils.isMapType(psiType)) {
                    val valueType = extractMapValueType(psiType)
                    if (valueType != null) {
                        val newNode = buildTree(valueType, "key", sessionIds, depth + 1)
                        if (newNode != null) {
                            node.children.add(newNode)
                        }
                    }
                }
            }
        }
        return node
    }

    /**
     * 专门用于构建并保存 DS 定义 (用于缓存)
     * 这相当于让这个 DS 自己做了一次“根节点”的处理
     */
    private fun buildAndSaveDefinition(
        psiClass: PsiClass,
        dsTargetId: String,
        substitutor: PsiSubstitutor,
        psiType: PsiType,
        sessionIds: MutableSet<String>
    ) {
        val definitionNode = AsyncTestVariableNode(
            type = AsyncTestType.DS,
            name = psiType.presentableText,
            dsTarget = dsTargetId
        )
        val (docInfo, _) = DocResolver().resolve(
            psiClass,
            mutableMapOf(),
            CodeType.POJO_CLASS,
            hasDocs = privateHasDoc
        )
        val classComment = docInfo.title
        if (classComment.isNotEmpty()) {
            definitionNode.statement = classComment
        }
        parsePojoFields(psiClass, substitutor, definitionNode, sessionIds)
        val ds = DataStructure(
            alias = psiType.presentableText,
            data = mutableListOf(definitionNode)
        )
        ds.hash = ds.calculateSafeHash()
        AfterNode.execute(module, dsTargetId, ds)
    }

    /**
     * 辅助：处理 POJO 字段
     */
    private fun parsePojoFields(
        psiClass: PsiClass,
        substitutor: PsiSubstitutor,
        parentNode: AsyncTestVariableNode,
        sessionIds: MutableSet<String>
    ) {
        val qName = psiClass.qualifiedName
        if (qName?.startsWith(SpringClassName.JAVA_PREFIX) == true && qName == SpringClassName.JAVA_BASE_OBJECT) {
            return
        }
        for (field in psiClass.allFields) {
            if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                continue
            }
            val fieldTypeText = field.type.canonicalText
            if (fieldTypeText.contains("org.slf4j.Logger") ||
                fieldTypeText.contains("java.util.logging.Logger") ||
                fieldTypeText.contains("org.springframework.context.ApplicationContext")
            ) {
                continue
            }
            if (field.hasAnnotation("com.fasterxml.jackson.annotation.JsonIgnore")) {
                continue
            }
            if (field.hasAnnotation(SpringClassName.SPRING_ANN_AUTOWIRED) || field.hasAnnotation(SpringClassName.JAVAX_ANN_RESOURCE) || field.hasAnnotation(
                    SpringClassName.JAKARTA_ANN_RESOURCE
                ) || field.hasAnnotation(SpringClassName.SPRING_ANN_VALUE)
            ) {
                continue
            }
            val fieldRealType = substitutor.substitute(field.type) ?: continue
            val childNode = buildTree(fieldRealType, field.name, sessionIds, 0, isRoot = false, field)
            if (childNode != null) {
                parentNode.children.add(childNode)
            }
        }
    }
}
