package com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part

import com.intellij.openapi.module.Module
import com.intellij.psi.PsiType
import com.sheldon.idea.plugin.api.constant.CommonConstant
import com.sheldon.idea.plugin.api.method.AsyncTestType
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode

import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.ResolverHelper

class SpringHeadersResolver : RequestPartResolver {

    override fun push(variable: ParamAnalysisResult, apiRequest: ApiRequest, module: Module): ApiRequest {
        if (variable.t !== null) {
            val headerList = buildTree(variable.t, "root", variable)
            if (!headerList.isNullOrEmpty()) {
                apiRequest.headers =
                    ResolverHelper.mergeHeadersOrParams(apiRequest.headers, headerList) { it.name.lowercase() }
            }
        }
        return apiRequest
    }

    fun buildTree(
        psiType: PsiType, name: String, variable: ParamAnalysisResult
    ): MutableList<AsyncTestVariableNode>? {
        if (isGeneralObject(psiType)) return null
        val typeStr = mapToAsyncType(psiType)
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
                name = name,
                defaultValue = variable.defaultValue ?: "",
                required = variable.isRequired
            )
            headerList.add(textNode)
        }
        return headerList
    }
}