package com.dettle.app.data.workspace

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks file modification timestamps (mtime) across agent read and edit turns.
 * Inspired by Claude Code's readFileState mechanism:
 * Prevents the agent from editing files based on stale or unread state.
 */
@Singleton
class FileStateTracker @Inject constructor() {
    private val readMtimes = ConcurrentHashMap<String, Long>()

    /** Record that file at [path] was read when its lastModified was [mtime] */
    fun recordRead(path: String, mtime: Long) {
        readMtimes[path] = mtime
    }

    /** Record that file at [path] was modified or written */
    fun recordModified(path: String, mtime: Long) {
        readMtimes[path] = mtime
    }

    /** Returns true if file has been read at least once */
    fun hasBeenRead(path: String): Boolean = readMtimes.containsKey(path)

    /** Returns true if current file mtime is newer than when it was last read */
    fun isStale(path: String, currentMtime: Long): Boolean {
        val lastRead = readMtimes[path] ?: return false
        return currentMtime > lastRead
    }

    /** Reset state (e.g. at start of new task/conversation) */
    fun clear() {
        readMtimes.clear()
    }
}
