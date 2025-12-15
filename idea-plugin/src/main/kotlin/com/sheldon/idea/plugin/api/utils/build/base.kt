package com.sheldon.idea.plugin.api.utils.build

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiType
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.ChildNodeType
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.model.NodeType
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.PathUtils
import com.sheldon.idea.plugin.api.utils.ProjectCacheService
import com.sheldon.idea.plugin.api.utils.build.helper.ClassHelper
import com.sheldon.idea.plugin.api.utils.build.helper.MethodHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.SpringClassResolver
import com.sheldon.idea.plugin.api.utils.calculateSafeHash

abstract class TreeBuilder {

    /**
     * 算法：深度优先寻找第一个包含 .java 文件的目录
     * 这就是你要求的“根据第一个 Java 文件确定锚点”
     */
    fun findBasePackageDirectory(dir: PsiDirectory): PsiDirectory? {
        // 检查当前目录下是否有 Java 文件
        if (dir.files.any { it is PsiJavaFile }) {
            return dir
        }

        // 如果没有，继续往子目录找 (取第一个非空子目录)
        for (sub in dir.subdirectories) {
            val found = findBasePackageDirectory(sub)
            if (found != null) return found
        }
        return null
    }

    fun isController(psiClass: PsiClass): Boolean {
        // 检查类上是否有 @org.springframework.stereotype.Controller 注解
        // 如果是注解定义，直接返回
        if (psiClass.isAnnotationType) return false
        // 如果是抽象类或者是接口直接忽略
        if (psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return false
        }
        // 排除匿名内部类
        if (psiClass.name == null) return false
        return AnnotationResolver.hasAnnotations(
            psiClass, SpringClassName.SPRING_CONTROLLER_ANNOTATION, findAll = false
        )
    }

    fun isMappingMethod(method: PsiMethod): Boolean {
        // 简单判断：只要有注解且注解名包含 Mapping 就认为是接口方法
        return SpringClassName.SPRING_SINGLE_REQUEST_MAPPING_ANNOTATIONS.any { method.hasAnnotation(it) }
    }

    fun makeRootNode(module: Module): ApiNode {
        return ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.ROOT_DIR.code,
            code_type = CodeType.MODULE.code,
            name = module.name,
            treePath = "${module.name}[${CodeType.MODULE.code}]",
            alias = "",
            desc = "",
        )
    }

    fun makeDirNode(
        subDir: PsiDirectory, parentPath: String
    ): ApiNode {
        return ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.DIR.code,
            code_type = CodeType.DIR.code,
            name = subDir.name,
            treePath = "${parentPath}[${CodeType.DIR.code}]",
            alias = "",
            desc = "",
        )
    }

    fun makeClassNode(classHelper: ClassHelper, psiClass: PsiClass, parentPath: String): ApiNode {
        val (clsAlias, clsDesc) = classHelper.parseDoc(psiClass)
        val request = SpringClassResolver().resolveRequestMapping(psiClass)
        return ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.DIR.code,
            code_type = CodeType.CLASS.code,
            name = psiClass.name ?: "Unknown",
            treePath = "${parentPath}[${CodeType.CLASS.code}]",
            alias = clsAlias,
            desc = clsDesc,
            classRequest = request
        )
    }

    fun makeMethodNode(
        methodHelper: MethodHelper,
        psiMethod: PsiMethod,
        prefixPath: String,
        classNode: ApiNode,
        callback: (MethodHelper, ApiRequest) -> String
    ): ApiNode? {
        val (mAlias, mDesc) = methodHelper.parseDoc(psiMethod)
        val request: ApiRequest = methodHelper.getMethodNodeCoreInfo(classNode) ?: return null
        request.path = PathUtils.normalizeToAsyncTestPath(request.path)
        val hash = request.calculateSafeHash()
        val requestKey = callback(methodHelper, request)
        val result = ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.INTERFACE_NODE.code,
            code_type = CodeType.METHOD.code,
            name = psiMethod.name,
            treePath = "${prefixPath}.${psiMethod.name}[${CodeType.METHOD.code}]",
            alias = mAlias,
            desc = mDesc,
            request = requestKey,
            path = request.path,
            method = request.method,
            hash = hash
        )

        return result
    }

    fun makeMethodExcludeParam(
        methodHelper: MethodHelper,
        psiMethod: PsiMethod,
        prefixPath: String,
        classNode: ApiNode,
    ): ApiNode? {
        fun getRequestKey(request: ApiRequest): String? {
            if (request.method == null) return null
            return "${request.method!!.lowercase()}:${request.path}"
        }
        val (mAlias, mDesc) = methodHelper.parseDoc(psiMethod)
        val request: ApiRequest = methodHelper.getMethodNodeCoreInfo(classNode, true) ?: return null
        request.path = PathUtils.normalizeToAsyncTestPath(request.path)
        val requestKey = getRequestKey(request) ?: return null
        return ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.INTERFACE_NODE.code,
            code_type = CodeType.METHOD.code,
            name = psiMethod.name,
            treePath = "$prefixPath.${psiMethod.name}",
            alias = mAlias,
            desc = mDesc,
            request = requestKey,
            path = request.path,
            method = request.method
        )
    }
}

abstract class BaseHelper {

    object DocHelper {

        /**
         * 解析注释，返回 (Alias, Description)
         */
        fun parseDoc(element: PsiElement): Pair<String?, String?> {
            if (element !is PsiDocCommentOwner) return null to null

            val docComment: PsiDocComment = element.docComment ?: return null to null

            // 获取纯文本，去除 /** * */ 等符号
            val fullText = docComment.descriptionElements.joinToString("") { it.text }.trim()

            if (fullText.isEmpty()) return null to null

            val lines = fullText.split("\n")

            // 1. Alias: 第一行非空文本
            val alias = lines.firstOrNull { it.isNotBlank() }?.trim()

            // 2. Desc: 剩下的所有文本
            val desc = lines.filter { it.isNotBlank() && it.trim() != alias }.joinToString("\n") { it.trim() }
                .takeIf { it.isNotBlank() }

            return alias to desc
        }
    }

    fun parseDoc(element: PsiElement): Pair<String?, String?> {
        return DocHelper.parseDoc(element)
    }
}

/**
 * 单个参数的分析结果 (中间态)
 * SpringParameterResolver 会返回这个对象
 */
data class ParamAnalysisResult(
    val location: ParamLocation, // 参数位置
    val name: String,            // 参数名 (key)
    val isRequired: Boolean = true,
    val t: PsiType? = null,
    val defaultValue: String? = null,
)