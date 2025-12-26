package com.sheldon.idea.plugin.api.utils

class ScanSession(var saveMock: Boolean = false) {
}

inline fun <T> scanContext(session: ScanSession, exec: (ScanSession) -> T): T {
    try {
        return exec(session)
    } finally {
    }
}