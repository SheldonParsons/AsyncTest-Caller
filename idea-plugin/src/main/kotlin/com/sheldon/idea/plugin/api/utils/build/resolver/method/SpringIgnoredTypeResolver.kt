package com.sheldon.idea.plugin.api.utils.build.resolver.method
import com.intellij.psi.PsiParameter
import com.sheldon.idea.plugin.api.service.SpringClassName
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper
object SpringIgnoredTypeResolver {
    /**
     * 判断参数是否应该被忽略
     * @return true 表示是上下文参数，不需要展示在 API 文档中
     */
    fun isIgnored(parameter: PsiParameter): Boolean {
        val type = parameter.type
        if (ResolverHelper.isInheritor(type, "java.util.Map") || ResolverHelper.isInheritor(
                type,
                "org.springframework.ui.ModelMap"
            )
        ) {
            if (parameter.annotations.isNotEmpty()) {
                return false
            }
            return true
        }
        for (ignoreClass in SpringClassName.IGNORED_PARAM_TYPES) {
            if (ResolverHelper.isInheritor(type, ignoreClass)) {
                return true
            }
        }
        return false
    }
}