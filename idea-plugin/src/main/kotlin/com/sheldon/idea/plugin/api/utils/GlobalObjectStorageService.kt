package com.sheldon.idea.plugin.api.utils
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
@Service(Service.Level.APP)
class GlobalObjectStorageService : Disposable {
    private val storage = ConcurrentHashMap<String, Any>()
    private val locks = ConcurrentHashMap.newKeySet<String>()
    fun <T : Any> save(key: String, node: T) {
        storage[key] = node
    }
    fun getRaw(key: String): Any? {
        return storage[key]
    }
    inline fun <reified T> get(key: String): T? {
        return getRaw(key) as? T
    }
    fun <T> appendToList(key: String, item: T) {
        storage.compute(key) { _, existingValue ->
            @Suppress("UNCHECKED_CAST")
            val list = (existingValue as? MutableList<T>) ?: CopyOnWriteArrayList()
            list.add(item)
            list
        }
    }
    fun contains(key: String): Boolean = storage.containsKey(key)
    fun clear() {
        storage.clear()
        locks.clear()
    }
    fun acquireLock(lockKey: String): Boolean {
        return locks.add(lockKey)
    }
    fun releaseLock(lockKey: String) {
        locks.remove(lockKey)
    }
    override fun dispose() {
        storage.clear()
        locks.clear()
    }
}
