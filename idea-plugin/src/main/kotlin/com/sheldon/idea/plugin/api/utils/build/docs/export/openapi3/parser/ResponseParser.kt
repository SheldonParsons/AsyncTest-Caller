package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.parser

import com.intellij.psi.PsiMethod
import com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.OpenApiBuildContext
import io.swagger.v3.oas.models.responses.ApiResponses

class ResponseParser(
    private val context: OpenApiBuildContext
) {

    fun parse(method: PsiMethod): ApiResponses {
        return ApiResponses()
    }
}
