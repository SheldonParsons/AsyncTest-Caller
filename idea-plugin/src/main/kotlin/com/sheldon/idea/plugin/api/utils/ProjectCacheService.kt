package com.sheldon.idea.plugin.api.utils

import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.XmlSerializerUtil
import com.sheldon.idea.plugin.api.model.*
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@Service(Service.Level.PROJECT)
@State(
    name = "AsyncTestCallerCache",
    storages = [Storage("async_test_force_dump.xml", roamingType = RoamingType.DISABLED)]
)
class ProjectCacheService(val project: Project) : PersistentStateComponent<CacheState> {
    private var state = CacheState()
    private val CURRENT_VERSION = 1
    override fun loadState(loadedState: CacheState) {
        if (loadedState.version < CURRENT_VERSION) {
            this.state = CacheState().apply { version = CURRENT_VERSION }
        } else {
            XmlSerializerUtil.copyBean(loadedState, this.state)
        }
    }

    override fun getState(): CacheState {
        return state
    }

    fun getGlobalSettings(): GlobalSettings {
        return state.globalSettings
    }

    fun saveGlobalSettings(settings: GlobalSettings) {
        state.globalSettings = settings
    }

    fun getPrivateInfo(): PrivateInfo {
        return state.privateInfo
    }

    fun savePrivateInfo(info: PrivateInfo) {
        state.privateInfo = info
    }

    fun getAsyncTestInfo(): AsyncTestInfo {
        return state.asynctestInfo
    }

    fun saveAsyncTestInfo(info: AsyncTestInfo) {
        state.asynctestInfo = info
    }

    fun getModuleSetting(projectName: String): ArrayList<ModuleSetting> {
        return state.moduleSettingMap.getOrPut(projectName) { arrayListOf() }
    }

    fun saveModuleSetting(projectName: String, setting: ModuleSetting) {
        state.moduleSettingMap
            .getOrPut(projectName) { arrayListOf() }
            .add(setting)
    }

    fun cleanModuleSetting(projectName: String) {
        state.moduleSettingMap.getOrPut(projectName) { arrayListOf() }.clear()
    }

    fun getModuleTree(moduleName: String): ApiNode? {
        return state.moduleTreeMap[moduleName]
    }

    fun getTreeMap(): MutableMap<String, ApiNode> {
        return state.moduleTreeMap
    }

    fun saveModuleTree(moduleName: String, tree: ApiNode) {
        state.moduleTreeMap[moduleName] = tree
    }

    fun getModuleDirAlias(moduleName: String): DirAliasMapping? {
        return state.moduleDirAliasMap.get(moduleName)
    }

    fun getSingleDirAlias(moduleName: String, dirName: String): Alias? {
        return getModuleDirAlias(moduleName)?.mapping?.get(dirName)
    }

    fun saveOrUpdateDirAlias(moduleName: String, dirName: String, alias: Alias) {
        val mappingObj = getModuleDirAlias(moduleName) ?: DirAliasMapping()
        mappingObj.mapping[dirName] = alias
        state.moduleDirAliasMap[moduleName] = mappingObj
    }

    fun getModuleRequests(moduleName: String): ModuleRequestMapping? {
        return state.moduleRequestMap[moduleName]
    }

    fun saveModuleRequests(moduleName: String, mapping: ModuleRequestMapping) {
        state.moduleRequestMap[moduleName] = mapping
    }

    fun getRequest(moduleName: String, key: String): ApiRequest? {
        val requestMapping = getModuleRequests(moduleName) ?: return null
        return requestMapping.mapping.get(key)
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

    fun getModuleRequestMocks(moduleName: String): ModuleRequestMockMapping? {
        return state.moduleRequestMockMap[moduleName]
    }

    fun saveModuleRequestMocks(moduleName: String, mapping: ModuleRequestMockMapping) {
        state.moduleRequestMockMap[moduleName] = mapping
    }

    fun getRequestMock(moduleName: String, key: String): ApiMockRequest? {
        val requestMockMapping = getModuleRequestMocks(moduleName) ?: return null
        return requestMockMapping.mapping.get(key)
    }

    fun saveOrUpdateSingleRequestMock(moduleName: String, key: String, mock: ApiMockRequest): String {
        val mappingObj = getModuleRequestMocks(moduleName) ?: ModuleRequestMockMapping()
        mappingObj.mapping[key] = mock
        saveModuleRequestMocks(moduleName, mappingObj)
        return key
    }

    fun getDataStructureMapping(moduleName: String): DataStructureMapping? {
        return state.moduleDataStructureMap[moduleName]
    }

    fun getDataStructure(moduleName: String, key: String): DataStructure? {
        val structureMapping = getDataStructureMapping(moduleName) ?: return null
        return structureMapping.mapping.get(key)
    }

    fun saveDataStructureMapping(moduleName: String, mapping: DataStructureMapping) {
        state.moduleDataStructureMap[moduleName] = mapping
    }

    fun saveOrUpdateSingleDataStructure(moduleName: String, key: String, body: DataStructure) {
        val mappingObj = getDataStructureMapping(moduleName) ?: DataStructureMapping()
        mappingObj.mapping[key] = body
        saveDataStructureMapping(moduleName, mappingObj)
    }

    fun addReferToDsPool(moduleName: String, refer: String) {
        state.moduleAllDataStructurePool.getOrPut(moduleName) { HashSet() }.add(refer)
    }

    fun getReferDsPool(moduleName: String): MutableSet<String> {
        return state.moduleAllDataStructurePool.getOrPut(moduleName) { HashSet() }
    }

    fun cleanModuleDirAlias(moduleName: String, dirName: String) {
        val mappingObj = getModuleDirAlias(moduleName) ?: return
        mappingObj.mapping.remove(dirName)
    }

    fun cleanModuleDirAlias(moduleName: String) {
        val mappingObj = getModuleDirAlias(moduleName) ?: return
        mappingObj.mapping.clear()
    }

    fun cleanModuleTree(moduleName: String) {
        state.moduleTreeMap.remove(moduleName)
    }

    fun cleanModuleTree() {
        state.moduleTreeMap.clear()
    }

    fun cleanModuleDsPool(moduleName: String) {
        state.moduleAllDataStructurePool[moduleName]?.clear()
    }

    fun cleanModuleDsPool() {
        state.moduleAllDataStructurePool.clear()
    }

    fun addReferToMethodPathPool(moduleName: String, refer: String) {
        state.moduleAllRequestMethodPathPool.getOrPut(moduleName) { HashSet() }.add(refer)
    }

    fun cleanModuleMethodPathPool(moduleName: String) {
        state.moduleAllRequestMethodPathPool[moduleName]?.clear()
    }

    fun cleanModuleMethodPathPool() {
        state.moduleAllRequestMethodPathPool.clear()
    }

    fun cleanModuleRequests(moduleName: String) {
        state.moduleRequestMap.remove(moduleName)
    }

    fun cleanModuleRequests() {
        state.moduleRequestMap.clear()
    }

    fun cleanRequest(moduleName: String, key: String) {
        state.moduleRequestMap[moduleName]?.mapping?.remove(key)
    }

    fun cleanModuleRequestMocks(moduleName: String) {
        state.moduleRequestMockMap.remove(moduleName)
    }

    fun cleanModuleRequestMocks() {
        state.moduleRequestMockMap.clear()
    }

    fun cleanModuleDs(moduleName: String) {
        state.moduleDataStructureMap.remove(moduleName)
    }

    fun cleanModuleDs() {
        state.moduleDataStructureMap.clear()
    }


    companion object {
        fun getInstance(project: Project): ProjectCacheService {
            return project.getService(ProjectCacheService::class.java)
        }
    }
}