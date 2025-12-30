package com.sheldon.idea.plugin.api.utils.build.docs

import com.intellij.psi.PsiElement

data class ExtractionContext(
    val targetElement: PsiElement, // 当前要解析的元素（类、方法、参数、字段）
    // 预留缓存：比如在解析 Method 时提取出的 @ApiImplicitParams，
    // key 为参数名，value 为提取出的文档信息
    val paramMetadata: MutableMap<String, DocInfo> = mutableMapOf()
)