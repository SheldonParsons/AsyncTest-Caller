package com.sheldon.idea.plugin.api.model

/**
 * 持久化状态容器。
 * 这里的所有字段都会被 IDEA 写入到 .idea/async_test_cache.xml 中。
 */
data class CacheState(
    var version: Int = 1,
    var globalSettings: String = "",
    var privateInfo: String = "",
    var asynctestInfo: String = "",
    var moduleSettingMap: MutableMap<String, String> = mutableMapOf(),
    var moduleTreeMap: MutableMap<String, String> = mutableMapOf(),
    var moduleRequestMap: MutableMap<String, String> = mutableMapOf(),
    var moduleRequestMockMap: MutableMap<String, String> = mutableMapOf(),
    var moduleAllDataStructurePool: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    val moduleAllRequestMethodPathPool: MutableMap<String, MutableSet<String>> = mutableMapOf(),
    var moduleDataStructureMap: MutableMap<String, String> = mutableMapOf(),
    var moduleDirAliasMap: MutableMap<String, String> = mutableMapOf()
)