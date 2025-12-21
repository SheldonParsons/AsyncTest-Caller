package com.sheldon.idea.plugin.api.model

// 1. 全局设置
data class GlobalSettings(
    var publicServerUrl: String = "https://www.asynctest.com",
    var usingPublic: Boolean = true,
    var customerServerUrl: String = "https://www.asynctest.com"
)

data class PrivateInfo(
    var username: String = "",
    var token: String = "",
    var expireTimestamp: Long = 0
)

// 3. 远端平台缓存信息
data class AsyncTestInfo(
    var projectList: ArrayList<RemoteProject> = ArrayList()
)

data class RemoteProject(var id: String = "", var name: String = "") {
    override fun toString(): String {
        return name
    }
}

// 4. 模块级别设置
data class ModuleSetting(
    var moduleInfo: String = "",
    var bindProject: RemoteProject = RemoteProject(),
    var bindVersion: String = ""
)

// request 映射 <Method+Path, ApiRequest>
data class ModuleRequestMapping(var mapping: java.util.HashMap<String, ApiRequest> = java.util.HashMap())

data class ModuleRequestMockMapping(var mapping: java.util.HashMap<String, ApiMockRequest> = java.util.HashMap())

// 数据模型 映射 <package.className, DataStructure>
data class DataStructureMapping(var mapping: java.util.HashMap<String, DataStructure> = java.util.HashMap())

data class DirAliasMapping(var mapping: java.util.HashMap<String, Alias> = java.util.HashMap())


data class Alias(var alias: String, var desc: String)