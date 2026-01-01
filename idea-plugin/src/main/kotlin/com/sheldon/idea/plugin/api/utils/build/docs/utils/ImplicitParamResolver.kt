package com.sheldon.idea.plugin.api.utils.build.docs.utils
import com.intellij.psi.PsiElement
import com.sheldon.idea.plugin.api.utils.build.docs.DocInfo
object ImplicitParamResolver {
    fun resolve(methodElement: PsiElement): MutableMap<String, DocInfo> {
        val result = mutableMapOf<String, DocInfo>()
        return result
    }
}