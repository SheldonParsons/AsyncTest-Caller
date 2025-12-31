package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException

object ExportHelper {

    fun exportJsonToFile(project: Project, jsonContent: String, defaultFileName: String = "openapi.json"): Boolean {
        // 1. 定义保存对话框的描述信息 (标题, 描述, 后缀名过滤器)
        val descriptor = FileSaverDescriptor(
            "保存 OpenAPI 文档",    // 对话框标题
            "选择保存位置",        // 描述
            "json"               // 强制扩展名为 .json
        )

        // 2. 创建保存对话框工厂
        val saveFileDialog: FileSaverDialog =
            FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)

        val fileWrapper = saveFileDialog.save(null as VirtualFile?, defaultFileName)
            ?: return false

        // 4. 判断用户是否点击了保存 (如果点击取消，wrapper 为 null)
        var file = fileWrapper.file
        if (!file.name.endsWith(".json")) {
            file = File(file.parentFile, file.name + ".json")
        }

        try {
            // 5. 写入文件 (Kotlin 的扩展方法，非常方便)
            file.writeText(jsonContent)

            // 6. 可选：刷新 VFS (如果你保存到了项目目录下，这步能让 IDEA 立即看到新文件)
            val virtualFile = VfsUtil.findFileByIoFile(file, true)
            virtualFile?.let {
                VfsUtil.markDirtyAndRefresh(true, false, false, it)
            }

            return true

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
}