package com.sheldon.idea.plugin.api.utils.build

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocCommentOwner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiAnnotation
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

    fun findBasePackageDirectory(dir: PsiDirectory): PsiDirectory? {
        if (dir.files.any { it is PsiJavaFile }) {
            return dir
        }
        for (sub in dir.subdirectories) {
            val found = findBasePackageDirectory(sub)
            if (found != null) return found
        }
        return null
    }

    fun isController(psiClass: PsiClass): Boolean {
        if (psiClass.isAnnotationType) return false
        if (psiClass.isInterface || psiClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return false
        }
        if (psiClass.name == null) return false
        return AnnotationResolver.hasAnnotations(
            psiClass, SpringClassName.SPRING_CONTROLLER_ANNOTATION, findAll = false
        )
    }

    fun isMappingMethod(method: PsiMethod): Boolean {
        if (hasRequestMappingAnnotation(method)) return true

        for (superMethod in method.findSuperMethods(true)) {
            if (hasRequestMappingAnnotation(superMethod)) return true
        }

        return false
    }

    private fun hasRequestMappingAnnotation(method: PsiMethod): Boolean {
        val annotations: Array<PsiAnnotation> = method.modifierList.annotations
        return annotations.any { SpringClassName.SPRING_SINGLE_REQUEST_MAPPING_ANNOTATIONS.contains(it.qualifiedName) }
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
            name = psiClass.name ?: "",
            tree_path = parentPath,
            alias = clsAlias ?: "",
            desc = clsDesc ?: "",
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
        request.name = psiMethod.name
        request.alias = mAlias ?: ""
        request.desc = mDesc ?: ""
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
            val rawText = docComment.descriptionElements.joinToString("") { it.text }
            val cleanText = normalizeJavadoc(rawText)
            val validLines = cleanText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val alias = validLines.firstOrNull()
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
            result = result.replace(Regex("(?m)^[\\s]*\\*+"), "")
                .replace(Regex("^/\\*+"), "")
                .replace(Regex("\\*+/$"), "")
            result = result.replace(Regex("(?i)</?(p|br|div|li|ul|h\\d)[^>]*>"), "\n")
            result = result.replace(Regex("<[^>]+>"), "")
            result = result.replace(Regex("\\{@\\w+\\s+(.*?)\\}"), "$1")
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


data class ParamAnalysisResult(
    val location: ParamLocation,
    val name: String,
    val isRequired: Boolean = true,
    val t: PsiType? = null,
    val defaultValue: String? = null,
)