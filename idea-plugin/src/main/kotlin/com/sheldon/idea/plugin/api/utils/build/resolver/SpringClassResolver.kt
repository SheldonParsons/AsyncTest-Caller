package com.sheldon.idea.plugin.api.utils.build.resolver

import com.intellij.psi.PsiClass
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.resolver.method.SpringMethodAnnotationResolver

class SpringClassResolver {
    fun resolveRequestMapping(psiClass: PsiClass): ApiRequest? {
        val annotation =
            AnnotationResolver.findAnnotationInHierarchy(psiClass, SpringClassName.REQUEST_MAPPING_ANNOTATION)
                ?: return null
        return SpringMethodAnnotationResolver.parseSingleAnnotation(annotation, true)
    }
}