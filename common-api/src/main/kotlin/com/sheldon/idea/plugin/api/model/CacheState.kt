package com.sheldon.idea.plugin.api.model

/**
 * 持久化状态容器。
 * 这里的所有字段都会被 IDEA 写入到 .idea/async_test_cache.xml 中。
 */
data class CacheState(
    var version: Int = 1,
    var globalSettings: GlobalSettings = GlobalSettings(),
    var privateInfo: PrivateInfo = PrivateInfo(),
    var asynctestInfo: AsyncTestInfo = AsyncTestInfo(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleSettingMap")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleSettingMap: java.util.HashMap<String, ArrayList<ModuleSetting>> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleTreeMap")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleTreeMap: java.util.HashMap<String, ApiNode> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleRequestMap")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleRequestMap: java.util.HashMap<String, ModuleRequestMapping> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleRequestMockMap")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleRequestMockMap: java.util.HashMap<String, ModuleRequestMockMapping> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleDataStructureMap")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleDataStructureMap: java.util.HashMap<String, DataStructureMapping> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleAllDataStructurePool")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleAllDataStructurePool: java.util.HashMap<String, HashSet<String>> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleAllRequestMethodPathPool")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleAllRequestMethodPathPool: java.util.HashMap<String, HashSet<String>> = java.util.HashMap(),
    @get:com.intellij.util.xmlb.annotations.Tag("moduleDirAliasMap")
    @get:com.intellij.util.xmlb.annotations.MapAnnotation(
        surroundWithTag = true,
        entryTagName = "entry",
        keyAttributeName = "key"
    )
    var moduleDirAliasMap: java.util.HashMap<String, DirAliasMapping> = java.util.HashMap()
)


