package com.sheldon.idea.plugin.api.utils.build
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.application.ApplicationManager
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
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.ApiNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.ChildNodeType
import com.sheldon.idea.plugin.api.model.CodeType
import com.sheldon.idea.plugin.api.model.NodeType
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.GlobalObjectStorageService
import com.sheldon.idea.plugin.api.utils.PathUtils
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
import com.sheldon.idea.plugin.api.utils.build.docs.DocResolver
import com.sheldon.idea.plugin.api.utils.build.helper.ClassHelper
import com.sheldon.idea.plugin.api.utils.build.helper.MethodHelper
import com.sheldon.idea.plugin.api.utils.build.resolver.AnnotationResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.SpringClassResolver
import com.sheldon.idea.plugin.api.utils.calculateSafeHash
abstract class TreeBuilder {
    fun findBasePackageDirectory(dir: PsiDirectory): PsiDirectory? {
        if (dir.files.any { it ->
                if (it is PsiJavaFile) {
                    for (psiClass in it.classes) {
                        if (isSpringBootApplicationClass(psiClass)) {
                            return@any true
                        }
                    }
                    return@any false
                } else {
                    return@any false
                }
            }) {
            return dir
        }
        for (sub in dir.subdirectories) {
            val found = findBasePackageDirectory(sub)
            if (found != null) return found
        }
        return null
    }
    fun isSpringBootApplicationClass(psiClass: PsiClass): Boolean {
        // 1️⃣ 直接注解
        if (AnnotationUtil.isAnnotated(
                psiClass,
                SpringClassName.SPRING_BOOT_APP,
                AnnotationUtil.CHECK_HIERARCHY
            )
        ) {
            return true
        }
        // 2️⃣ 元注解（组合注解）
        for (annotation in psiClass.modifierList?.annotations ?: emptyArray()) {
            val resolved = annotation.nameReferenceElement?.resolve()
            if (resolved is PsiClass) {
                if (AnnotationUtil.isAnnotated(
                        resolved,
                        SpringClassName.SPRING_BOOT_APP,
                        AnnotationUtil.CHECK_HIERARCHY
                    )
                ) {
                    return true
                }
            }
        }
        return false
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
    fun makeClassNode(
        classHelper: ClassHelper,
        psiClass: PsiClass,
        parentPath: String,
        hasDocs: Boolean = false
    ): ApiNode {
        val (docInfo: DocInfo, _) = DocResolver().resolve(psiClass, mutableMapOf(), CodeType.CLASS, hasDocs)
        val request = SpringClassResolver().resolveRequestMapping(psiClass)
        val result = ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.DIR.code,
            code_type = CodeType.CLASS.code,
            name = psiClass.name ?: "",
            tree_path = parentPath,
            alias = docInfo.title,
            desc = docInfo.description,
            path = request?.path,
            classRequest = request
        )
        if (hasDocs) {
            result.docs = docInfo
            val cacheService = ApplicationManager.getApplication().getService(GlobalObjectStorageService::class.java)
            cacheService.appendToList(CommonConstant.DOCS_OBJECT_CLASS_NODE_LIST, result)
        }
        return result
    }
    fun makeMethodNode(
        methodHelper: MethodHelper,
        psiMethod: PsiMethod,
        prefixPath: String,
        classNode: ApiNode,
        hasDocs: Boolean = false,
        callback: (MethodHelper, ApiRequest) -> String
    ): ApiNode? {
        val (docInfo: DocInfo, implicitParams) = DocResolver().resolve(
            psiMethod,
            mutableMapOf(),
            CodeType.METHOD,
            hasDocs
        )
        val request: ApiRequest =
            methodHelper.getMethodNodeCoreInfo(classNode, implicitParams = implicitParams, hasDocs = hasDocs)
                ?: return null
        request.path = PathUtils.normalizeToAsyncTestPath(request.path)
        request.name = psiMethod.name
        request.alias = docInfo.title
        request.desc = docInfo.description
        val hash = request.calculateSafeHash()
        request.hash = hash
        val requestKey = callback(methodHelper, request)
        val result = ApiNode(
            type = NodeType.INTERFACE.code,
            child_type = ChildNodeType.INTERFACE_NODE.code,
            code_type = CodeType.METHOD.code,
            name = psiMethod.name,
            tree_path = "${prefixPath}.${psiMethod.name}[${CodeType.METHOD.code}]",
            alias = docInfo.title,
            desc = docInfo.description,
            request = requestKey,
            path = request.path,
            method = request.method,
            hash = hash
        )
        if (hasDocs) {
            result.docs = docInfo
        }
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
    val docInfo: DocInfo? = null,
)