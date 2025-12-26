package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.psi.PsiType
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.TypeUtils
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class SpringHeadersResolver : RequestPartResolver {
    override fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest): ApiRequest {
        if (variable.t !== null) {
            val headerList = buildTree(variable.t, variable)
            if (!headerList.isNullOrEmpty()) {
                apiRequest.headers =
                    ResolverHelper.mergeHeadersOrParams(apiRequest.headers, headerList) { it.name.lowercase() }
            }
        }
        return apiRequest
    }

    fun buildTree(
        psiType: PsiType, variable: ParamAnalysisResult
    ): MutableList<AsyncTestVariableNode>? {
        if (TypeUtils.isGeneralObject(psiType)) return null
        val typeStr = TypeUtils.mapToAsyncType(psiType)
        val headerList = mutableListOf<AsyncTestVariableNode>()
        if (typeStr in listOf(
                AsyncTestType.STRING,
                AsyncTestType.INTEGER,
                AsyncTestType.BOOLEAN,
                AsyncTestType.NUMBER,
                AsyncTestType.NULL
            )
        ) {
            val textNode = AsyncTestVariableNode(
                type = AsyncTestType.STRING,
                name = variable.name,
                defaultValue = variable.defaultValue ?: "",
                required = variable.isRequired
            )
            headerList.add(textNode)
        }
        return headerList
    }
}