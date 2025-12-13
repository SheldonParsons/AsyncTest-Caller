package com.sheldon.idea.plugin.api.utils

import com.google.gson.GsonBuilder
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.sheldon.idea.plugin.api.method.AsyncTestVariableNode
import com.sheldon.idea.plugin.api.model.*

/**
 * 项目级缓存服务
 * 负责数据的序列化、反序列化、默认值处理
 */
@Service(Service.Level.PROJECT)
@State(
    name = "AsyncTestCallerCache",
    storages = [Storage("async_test_caller_cache.xml", roamingType = RoamingType.DISABLED)]
)
class ProjectCacheService : PersistentStateComponent<CacheState> {

    private var state = CacheState()

    private val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

    private val CURRENT_VERSION = 1

    override fun loadState(loadedState: CacheState) {
        if (loadedState.version < CURRENT_VERSION) {
            // 版本升级，丢弃旧数据，使用新的默认值
            this.state = CacheState().apply { version = CURRENT_VERSION }
        } else {
            XmlSerializerUtil.copyBean(loadedState, this.state)
        }
    }

    override fun getState(): CacheState {
        return state
    }

    private inline fun <reified T> safeFromJson(json: String, defaultProvider: () -> T): T {
        if (json.isBlank() || json == "{}") {
            return defaultProvider()
        }
        return try {
            gson.fromJson(json, T::class.java) ?: defaultProvider()
        } catch (e: Exception) {
            e.printStackTrace() // 记录错误日志
            defaultProvider() // 解析失败也返回默认值
        }
    }

    fun getGlobalSettings(): GlobalSettings {
        return safeFromJson(state.globalSettings) { GlobalSettings() }
    }

    fun saveGlobalSettings(settings: GlobalSettings) {
        state.globalSettings = gson.toJson(settings)
    }

    fun getPrivateInfo(): PrivateInfo {
        return safeFromJson(state.privateInfo) { PrivateInfo() }
    }

    fun savePrivateInfo(info: PrivateInfo) {
        state.privateInfo = gson.toJson(info)
    }

    fun getAsyncTestInfo(): AsyncTestInfo {
        return safeFromJson(state.asynctestInfo) { AsyncTestInfo() }
    }

    fun saveAsyncTestInfo(info: AsyncTestInfo) {
        state.asynctestInfo = gson.toJson(info)
    }

    fun getModuleSetting(moduleName: String): ModuleSetting? {
        val json = state.moduleSettingMap[moduleName] ?: return null
        return safeFromJson(json) { null }
    }

    fun saveModuleSetting(moduleName: String, setting: ModuleSetting) {
        state.moduleSettingMap[moduleName] = gson.toJson(setting)
    }

    fun getModuleTree(moduleName: String): ModuleTree? {
        val json = state.moduleTreeMap[moduleName] ?: return null
        return safeFromJson(json) { null }
    }

    fun saveModuleTree(moduleName: String, tree: ModuleTree) {
        state.moduleTreeMap[moduleName] = gson.toJson(tree)
    }

    fun cleanModuleTree(moduleName: String) {
        state.moduleTreeMap.remove(moduleName)
    }

    fun cleanModuleTree() {
        state.moduleTreeMap.clear()
    }

    fun getModuleDirAlias(moduleName: String): DirAliasMapping? {
        val aliasMapping = state.moduleDirAliasMap[moduleName] ?: return null
        return safeFromJson(aliasMapping) { null }
    }

    fun getSingleDirAlias(moduleName: String, dirName: String): Alias? {
        return getModuleDirAlias(moduleName)?.mapping?.get(dirName)
    }

    fun saveOrUpdateDirAlias(moduleName: String, dirName: String, alias: Alias) {
        val mappingObj = getModuleDirAlias(moduleName) ?: DirAliasMapping()
        mappingObj.mapping[dirName] = alias
        state.moduleDirAliasMap[moduleName] = gson.toJson(mappingObj)
    }

    fun cleanModuleDirAlias(moduleName: String, dirName: String) {
        val mappingObj = getModuleDirAlias(moduleName) ?: return
        mappingObj.mapping.remove(dirName)
    }

    fun cleanModuleDirAlias(moduleName: String) {
        val mappingObj = getModuleDirAlias(moduleName) ?: return
        mappingObj.mapping.clear()
    }


    fun getModuleRequests(moduleName: String): ModuleRequestMapping? {
        val json = state.moduleRequestMap[moduleName] ?: return null
        return safeFromJson(json) { null }
    }

    fun saveModuleRequests(moduleName: String, mapping: ModuleRequestMapping) {
        state.moduleRequestMap[moduleName] = gson.toJson(mapping)
    }

    fun cleanModuleRequests(moduleName: String) {
        state.moduleRequestMap.remove(moduleName)
    }

    fun cleanModuleRequests() {
        state.moduleRequestMap.clear()
    }

    fun getRequest(moduleName: String, key: String): ApiRequest? {
        val requestMapping = getModuleRequests(moduleName) ?: return null
        return requestMapping.mapping[key]
    }

    private fun getRequestKey(request: ApiRequest): String? {
        if (request.method == null) return null
        return "${request.method!!.lowercase()}:${request.path}"
    }

    fun saveOrUpdateSingleRequest(moduleName: String, request: ApiRequest): String? {
        val key = getRequestKey(request) ?: return null
        val mappingObj = getModuleRequests(moduleName) ?: ModuleRequestMapping()
        mappingObj.mapping[key] = request
        saveModuleRequests(moduleName, mappingObj)
        return key
    }

    fun getDataStructureMapping(moduleName: String): DataStructureMapping? {
        val structure = state.moduleDataStructureMap[moduleName] ?: return null
        return safeFromJson(structure) { null }
    }

    fun getDataStructure(moduleName: String, key: String): DataStructure? {
        val structureMapping = getDataStructureMapping(moduleName) ?: return null
        return structureMapping.mapping[key]
    }

    fun saveDataStructureMapping(moduleName: String, mapping: DataStructureMapping) {
        state.moduleDataStructureMap[moduleName] = gson.toJson(mapping)
    }

    fun saveOrUpdateSingleDataStructure(moduleName: String, key: String, body: DataStructure) {
        val mappingObj = getDataStructureMapping(moduleName) ?: DataStructureMapping()
        mappingObj.mapping[key] = body
        saveDataStructureMapping(moduleName, mappingObj)
    }

    fun addReferToDsPool(moduleName: String, refer: String) {
        state.moduleAllDataStructurePool
            .getOrPut(moduleName) { mutableSetOf() }
            .add(refer)
    }

    fun cleanModuleDsPool(moduleName: String) {
        state.moduleAllDataStructurePool[moduleName]?.clear()
    }

    fun cleanModuleDsPool() {
        state.moduleAllDataStructurePool.clear()
    }

    fun addReferToMethodPathPool(moduleName: String, refer: String) {
        state.moduleAllRequestMethodPathPool.getOrPut(moduleName) { mutableSetOf() }
            .add(refer)
    }

    fun cleanModuleMethodPathPool(moduleName: String) {
        state.moduleAllRequestMethodPathPool[moduleName]?.clear()
    }

    fun cleanModuleMethodPathPool() {
        state.moduleAllRequestMethodPathPool.clear()
    }

    companion object {
        fun getInstance(project: Project): ProjectCacheService = project.service()
    }
}