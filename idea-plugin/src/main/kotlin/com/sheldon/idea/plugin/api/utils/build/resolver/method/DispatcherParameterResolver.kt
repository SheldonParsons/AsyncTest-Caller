package com.sheldon.idea.plugin.api.utils.build.resolver.method

import com.intellij.openapi.module.Module
import com.sheldon.idea.plugin.api.method.ParamLocation
import com.sheldon.idea.plugin.api.model.ApiRequest
import com.sheldon.idea.plugin.api.model.AsyncTestFormData
import com.sheldon.idea.plugin.api.utils.build.ParamAnalysisResult
import com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part.SpringBodyResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part.SpringFormDataResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part.SpringHeadersResolver
import com.sheldon.idea.plugin.api.utils.build.resolver.method.request_part.SpringQueryResolver

class DispatcherParameterResolver {
    /**
     * @param apiRequest @RequestMapping 上解析出来的静态信息 (headers, params)
     * @param parsedParams 方法参数解析出来的动态信息 (List<ParamAnalysisResult>)
     */
    fun analyze(
        apiRequest: ApiRequest,
        parsedParams: List<ParamAnalysisResult>,
        module: Module
    ): ApiRequest {
        var hasJsonBody = false
        for (result in parsedParams) {
            val location = result.location
            when (location) {
                ParamLocation.QUERY -> {
                    SpringQueryResolver().push(result, apiRequest)
                }

                ParamLocation.BODY -> {
                    SpringBodyResolver(module).push(result, apiRequest)
                    apiRequest.formData = AsyncTestFormData()
                    hasJsonBody = true
                }

                ParamLocation.FORM_DATA -> {
                    if (hasJsonBody) {
                        continue
                    }
                    SpringFormDataResolver().push(result, apiRequest)
                }

                ParamLocation.HEADER -> {
                    SpringHeadersResolver().push(result, apiRequest)
                }
            }
        }
        return apiRequest
    }
}
