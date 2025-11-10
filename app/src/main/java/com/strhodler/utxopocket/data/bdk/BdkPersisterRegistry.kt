package com.strhodler.utxopocket.data.bdk

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.bitcoindevkit.Persister
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BdkPersisterRegistry @Inject constructor() {

    private val persisters = ConcurrentHashMap<String, Entry>()

    fun acquire(path: String): ManagedPersister {
        val entry = synchronized(persisters) {
            persisters[path]?.also { it.refCount++ } ?: run {
                val persister = Persister.newSqlite(path)
                Entry(persister = persister, refCount = 1).also { persisters[path] = it }
            }
        }
        return ManagedPersister(entry.persister) {
            release(path)
        }
    }

    fun evict(path: String) {
        val entry = synchronized(persisters) {
            persisters.remove(path)
        } ?: return
        runCatching { entry.persister.destroy() }
    }

    private fun release(path: String) {
        val toDestroy = synchronized(persisters) {
            val entry = persisters[path] ?: return
            entry.refCount--
            if (entry.refCount <= 0) {
                persisters.remove(path)
                entry
            } else {
                null
            }
        }
        toDestroy?.let { runCatching { it.persister.destroy() } }
    }

    private data class Entry(
        val persister: Persister,
        var refCount: Int
    )

    class ManagedPersister internal constructor(
        val persister: Persister,
        private val releaseAction: () -> Unit
    ) : Closeable {
        private val closed = AtomicBoolean(false)

        override fun close() {
            if (closed.compareAndSet(false, true)) {
                releaseAction()
            }
        }
    }
}
