package com.sheldon.idea.plugin.api.utils


class ScanSession(var saveMock: Boolean = false) {

}

inline fun <R> scanContext(session: ScanSession, exec: (ScanSession) -> R): R {
    try {
        return exec(session)
    } finally {
    }
}