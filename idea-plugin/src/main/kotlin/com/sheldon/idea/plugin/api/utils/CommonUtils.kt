package com.sheldon.idea.plugin.api.utils

import com.sheldon.idea.plugin.api.constant.CommonConstant
import java.util.UUID

object CommonUtils {
    // eg.:----WebKitFormBoundaryF9Eah5XyhLDkgQNs
    fun getBoundaryString(prefix: String = CommonConstant.DEFAULT_BOUNDARY_PREFIX, fix: Boolean = true): String {
        if (fix) {
            return "${prefix}${CommonConstant.FIX_BOUNDARY_PREFIX}"
        }
        return prefix +
                UUID.randomUUID().toString().replace("-", "").take(16)
    }
}