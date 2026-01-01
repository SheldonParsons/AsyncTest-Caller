package com.sheldon.idea.plugin.api.utils.build.docs.export.openapi3.utils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileChooser.FileSaverDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.IOException
object ExportHelper {
    /**
     * 保存文本内容 (原有逻辑，保持不变)
     */
    fun exportJsonToFile(
        project: Project,
        jsonContent: String,
        defaultFileName: String = "openapi.json",
        extensions: String = "json",
        title: String = "保存 JSON 文档",
    ): Boolean {
        return saveFile(project, defaultFileName, extensions, title) { file ->
            file.writeText(jsonContent) // 写入文本
        }
    }
    /**
     * ★★★ 新增：保存二进制内容 (用于文件下载) ★★★
     */
    fun exportBinaryToFile(
        project: Project,
        data: ByteArray,
        defaultFileName: String,
        extensions: String, // 例如 "png", "pdf", "zip"，如果不确定可以传 ""
        title: String = "保存文件",
    ): Boolean {
        return saveFile(project, defaultFileName, extensions, title) { file ->
            file.writeBytes(data) // 写入二进制字节
        }
    }
    /**
     * 提取公共的保存对话框逻辑
     */
    private fun saveFile(
        project: Project,
        defaultFileName: String,
        extensions: String,
        title: String,
        writeAction: (File) -> Unit
    ): Boolean {
        // 如果有扩展名限制，则添加过滤器；否则允许所有文件
        val descriptor = if (extensions.isNotEmpty()) {
            FileSaverDescriptor(title, "选择保存位置", extensions)
        } else {
            FileSaverDescriptor(title, "选择保存位置")
        }
        val saveFileDialog: FileSaverDialog =
            FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        // 显式强转 null 避免编译器歧义
        val fileWrapper = saveFileDialog.save(null as VirtualFile?, defaultFileName)
            ?: return false
        var file = fileWrapper.file
        // 自动补全扩展名逻辑
        if (extensions.isNotEmpty() && !file.name.endsWith(".$extensions", ignoreCase = true)) {
            file = File(file.parentFile, file.name + ".$extensions")
        }
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                writeAction(file)
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return true
    }
}