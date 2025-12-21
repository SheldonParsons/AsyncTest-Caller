package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.google.gson.GsonBuilder
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
import com.sheldon.idea.plugin.api.model.DataStructure
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.lifecycle.AfterNode
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
import com.sheldon.idea.plugin.api.utils.calculateSafeHash

class SpringBodyResolver(val module: Module) :
    RequestPartResolver {

    override fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest): ApiRequest {
        /**
         * 1、获取持久化缓存信息
         * 2、将创建的dto对象更新进去ProjectCacheService.saveOrUpdateSingleRequest
         * 3、将dto加入到ProjectCacheService.addReferToDsPool
         * */
//        println("类型----------------------：${variable.t}")
        // 优先解析的权重更高，如果已经解析过了，直接返回
        if (apiRequest.json.isNotEmpty()) {
            return apiRequest
        }
        if (variable.t !== null) {
            val currentSessionIds = mutableSetOf<String>()
            val rootNode = buildTree(variable.t, "root", currentSessionIds, isRoot = true)
            if (rootNode != null && rootNode.type in listOf(
                    AsyncTestType.ARRAY, AsyncTestType.DS, AsyncTestType.OBJECT
                )
            ) {
                apiRequest.json = mutableListOf(rootNode)

                // =======================================================
                // 验证打印逻辑 START
                // =======================================================
//                val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()
//
//                val cacheService = ProjectCacheService.getInstance(module.project)

//                println("\n========== [1] API 请求体结构 (Root) ==========")
//                // 预期：Root 是实心的，但其子 DS 节点 (如 data) 是空心的 (children=[])
//                println(gson.toJson(rootNode))
//
//                println("\n========== [2] ProjectCache 存储的定义 (Definitions) ==========")
//                // 预期：这里包含了所有涉及到的 DS 的完整定义 (Root 自身也会在这里存一份)
//                val allDefs = cacheService.getDataStructureMapping(module.name)?.mapping
//                println("allDefs.size:${allDefs?.size}")
//                allDefs?.forEach { (id, node) ->
//                    println(">>> 定义 ID: ${id},module: ${module.name}")
//                    println(">>> alias: ${node.alias}")
//                    println(gson.toJson(node.data))
//                    println("--------------------------------------------------")
//                }
//                println("所有ds:${cacheService.getReferDsPool(module.name)}")
//
//                println("======================================================\n")
                // =======================================================
                // 验证打印逻辑 END
                // =======================================================
                // 处理headers，添加content-type
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
        originElement: PsiElement? = null
    ): AsyncTestVariableNode? {
        // 1. 获取对应的 AsyncTestType
        if (TypeUtils.isGeneralObject(psiType)) return null
        val typeStr = TypeUtils.mapToAsyncType(psiType)
        if (typeStr == AsyncTestType.FILES) return null
        val node = AsyncTestVariableNode(type = typeStr, name = name)
        if (originElement != null) {
            val fieldComment = ResolverHelper.getElementComment(originElement)
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
            // 提取类的注释
            if (node.statement.isEmpty()) {
                val classComment = ResolverHelper.getElementComment(psiClass)
                if (classComment.isNotEmpty()) {
                    node.statement = classComment
                }
            }
            if (isRoot) {
                // === 情况 A: 我是根节点 (Root) ===
                // 需求：显示所有字段。
                node.name = psiType.presentableText
                // 1. 标记为已处理 (Root 自己也需要防止被子孙节点引用导致死循环)
                sessionIds.add(dsTargetId)
                // 2. 直接在当前 node 上填充 children (变实心)
                parsePojoFields(psiClass, resolveResult.substitutor, node, sessionIds)
                // 存入持久层
                val ds = DataStructure(
                    alias = psiType.presentableText,
                    data = mutableListOf(node)
                )

                ds.hash = ds.calculateSafeHash()
                AfterNode.execute(module, dsTargetId, ds)
                // 4. 返回这个填满数据的 node
                return node

            } else {
                // === 情况 B: 我是子节点 (非 Root) ===
                // 需求：不显示内容，只显示引用。

                // 1. 保持 node.children 为空 (变空心)

                // 2. 检查是否需要去构建它的完整定义
                if (!sessionIds.contains(dsTargetId)) {
                    sessionIds.add(dsTargetId)

                    // 3. 触发构建：去生成一个全新的、实心的定义节点存入缓存
                    // 注意：这里生成的 definitionNode 跟当前的 node 没有对象引用的关系，是独立的
                    buildAndSaveDefinition(psiClass, dsTargetId, resolveResult.substitutor, psiType, sessionIds)
                }

                // 4. 返回这个空心的 node (仅作为引用)
                return node
            }
        }

        // 3. 深度限制防止栈溢出
        if (depth > 20) return node

        // 4. 根据类型处理子节点
        when (typeStr) {
            AsyncTestType.ARRAY -> {
                // 如果是数组/集合，提取泛型元素类型，生成一个子节点
                // 例如 List<User> -> children 中放入一个 User 类型的节点
                val componentType = extractArrayComponentType(psiType)
                if (componentType != null) {
                    // 数组的子项通常没有具体名字，或者叫 "item" / "[0]"
                    val newNode = buildTree(componentType, "item", sessionIds, depth + 1)
                    if (newNode != null) {
                        node.children.add(newNode)
                    }
                }
            }

            AsyncTestType.OBJECT -> {
                // 如果是对象（且不是 Map/Object基类），递归解析字段
                // 这里需要排除 java.util.Map (视作空对象) 和 java.lang.Object
                if (TypeUtils.isMapType(psiType)) {
                    // === 情况 A: 是 Map 类型 ===
                    // 提取 Value 的类型 (比如 SheldonMappingValue)
                    val valueType = extractMapValueType(psiType)

                    // 如果提取到了 Value 类型，就创建一个名为 "key" 的子节点
                    // 这样前端展示时就知道：Map 里面装着这种结构的值
                    if (valueType != null) {
                        // 子节点名字叫 "key" (或者 "<key>", "{key}" 随你喜欢)
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
        // 1. 创建一个新的定义节点
        val definitionNode = AsyncTestVariableNode(
            type = AsyncTestType.DS,
            name = psiType.presentableText,
            dsTarget = dsTargetId
        )
        val classComment = ResolverHelper.getElementComment(psiClass)
        if (classComment.isNotEmpty()) {
            definitionNode.statement = classComment
        }

        // 2. 填充它的 children (这一步让它变实心)
        parsePojoFields(psiClass, substitutor, definitionNode, sessionIds)

        // 存入持久化
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
            // static, transient
            if (field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                continue
            }

            // 类型黑名单过滤 (日志、Spring上下文等)
            val fieldTypeText = field.type.canonicalText
            if (fieldTypeText.contains("org.slf4j.Logger") ||
                fieldTypeText.contains("java.util.logging.Logger") ||
                fieldTypeText.contains("org.springframework.context.ApplicationContext")
            ) {
                continue
            }

            // 注解黑名单过滤 (@JsonIgnore)
            if (field.hasAnnotation("com.fasterxml.jackson.annotation.JsonIgnore")) {
                continue
            }

            // Spring/内部注解过滤 (关键修复)
            if (field.hasAnnotation(SpringClassName.SPRING_ANN_AUTOWIRED) || field.hasAnnotation(SpringClassName.JAVAX_ANN_RESOURCE) || field.hasAnnotation(
                    SpringClassName.JAKARTA_ANN_RESOURCE
                ) || field.hasAnnotation(SpringClassName.SPRING_ANN_VALUE)
            ) {
                continue
            }
            // 获取泛型替换后的真实类型
            val fieldRealType = substitutor.substitute(field.type) ?: continue

            // 如果字段是 DS，返回引用；如果字段是 String，返回实体。
            val childNode = buildTree(fieldRealType, field.name, sessionIds, 0, isRoot = false, field)
            if (childNode != null) {
                parentNode.children.add(childNode)
            }
        }
    }

}

