package com.sheldon.idea.plugin.api.utils.build

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
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
            tree_path = "${module.name}[${CodeType.MODULE.code}]",
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
            tree_path = "${parentPath}[${CodeType.DIR.code}]",
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
            tree_path = parentPath,
            alias = "$clsAlias",
            desc = "$clsDesc",
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
        request.hash = hash
        val requestKey = callback(methodHelper, request)
        val result = ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.INTERFACE_NODE.code,
            code_type = CodeType.METHOD.code,
            name = psiMethod.name,
            tree_path = "${prefixPath}.${psiMethod.name}[${CodeType.METHOD.code}]",
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

        val request: ApiRequest = methodHelper.getMethodNodeCoreInfo(classNode, true) ?: return null
        request.path = PathUtils.normalizeToAsyncTestPath(request.path)
        val requestKey = getRequestKey(request) ?: return null
        return ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.INTERFACE_NODE.code,
            code_type = CodeType.METHOD.code,
            name = psiMethod.name,
            tree_path = "$prefixPath.${psiMethod.name}",
            request = requestKey,
            path = request.path,
            method = request.method
        )
    }
}

abstract class BaseHelper {

    object DocHelper {

        fun parseDoc(element: PsiElement): Pair<String?, String?> {
            if (element !is PsiDocCommentOwner) return null to null
            val docComment = element.docComment ?: return null to null

            // 1. 获取原始文档内容 (包含 HTML 标签和格式符号)
            val rawText = docComment.descriptionElements.joinToString("") { it.text }

            // 2. 【核心步骤】全局标准化清洗
            val cleanText = normalizeJavadoc(rawText)

            // 3. 按空行/换行符拆分，提取 Alias 和 Desc
            // 过滤掉因为替换标签产生的多余空行
            val validLines = cleanText.lines().map { it.trim() }.filter { it.isNotEmpty() }

            val alias = validLines.firstOrNull()

            // 剩下的作为描述
            val desc = if (validLines.size > 1) {
                validLines.drop(1).joinToString("\n")
            } else {
                null
            }

            return alias to desc
        }

        /**
         * 强力清洗函数
         */
        private fun normalizeJavadoc(text: String): String {
            var result = text

            // A. 去除 Javadoc 的装饰符号 (行首的 *, /**, */)
            // (?m) 开启多行模式，^[\s]*\* 匹配每行开头的空白和星号
            result = result.replace(Regex("(?m)^[\\s]*\\*+"), "")
                .replace(Regex("^/\\*+"), "") // 去除开头的 /**
                .replace(Regex("\\*+/$"), "") // 去除结尾的 */

            // B. 处理块级标签 -> 替换为换行符 (让它们自然隔开文本)
            // 比如 <p>首页</p> 变成 \n首页\n
            result = result.replace(Regex("(?i)</?(p|br|div|li|ul|h\\d)[^>]*>"), "\n")

            // C. 移除剩下的内联 HTML 标签 (如 <span>, <a>, <b>)
            result = result.replace(Regex("<[^>]+>"), "")

            // D. 处理 Javadoc 标签 {@link Xxx} -> Xxx
            result = result.replace(Regex("\\{@\\w+\\s+(.*?)\\}"), "$1")

            // E. 处理 HTML 转义 (常见的一些)
            result = result.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")

            return result
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