package com.sheldon.idea.plugin.api.model

// 1. 全局设置
data class GlobalSettings(
    var publicServerUrl: String = "https://www.asynctest.com",
    var usingPublic: Boolean = true,
    var customerServerUrl: String = ""
)

data class PrivateInfo(
    var username: String = "",
    var token: String = "",
    var expireTimestamp: Long = 0
)

// 3. 远端平台缓存信息
data class AsyncTestInfo(
    var projectList: MutableList<RemoteProject> = mutableListOf()
)

data class RemoteProject(var id: String = "", var name: String = "")

// 4. 模块级别设置
data class ModuleSetting(var moduleInfo: String = "", var bindProject: RemoteProject)

// request 映射 <Method+Path, ApiRequest>
data class ModuleRequestMapping(var mapping: MutableMap<String, ApiRequest> = mutableMapOf())

data class ModuleRequestMockMapping(var mapping: MutableMap<String, ApiMockRequest> = mutableMapOf())

// 数据模型 映射 <package.className, DataStructure>
data class DataStructureMapping(var mapping: MutableMap<String, DataStructure> = mutableMapOf())

data class DirAliasMapping(var mapping: MutableMap<String, Alias> = mutableMapOf())


data class Alias(var alias: String, var desc: String)