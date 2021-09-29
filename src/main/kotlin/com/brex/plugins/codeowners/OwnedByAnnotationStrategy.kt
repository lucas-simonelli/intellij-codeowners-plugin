package com.brex.plugins.codeowners

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class OwnedByAnnotationStrategy(private val project: Project) {
    private val proj = project
    private val packageInfo = "package-info.java"
    private val annotationRegex = "@OwnedBy\\((?:value ?=)? ?([^,]*)(?:, ?boundedContext ?= ?(.*))?\\)".toRegex()
    private val analyzableFiles = setOf("java")

    /** Given a file, find the owner
     * 1. If not java, return empty
     * 2. If java & it's annotated: show owner
     * 3. If java and not annotated: go up and check package-info.java
     */
    fun getCodeOwner(file: VirtualFile): CodeOwnerRule? {
        if (file.extension !in analyzableFiles) {
            return null
        }
        return getOwner(file.path)
    }

    fun getOwner(path: String): CodeOwnerRule? {
        val file = File(path)
        if (!file.exists()) {
            val dir = File(file.parent).parent
            if (dir == proj.basePath) {
                return null
            }
            return getOwner(dir + File.separator + packageInfo)
        }
        val lines = file.readLines()
        var i = 1
        for (line in lines) {
            val match = annotationRegex.find(line)
            if (match != null && match.groups.size > 1) {
                val owner = match.groups[1]?.value
                val group = match.groups[2]?.value
                return CodeOwnerRule(owner ?: "--", group ?: "--", i, file.path)
            }
            if (line.contains("class")) {
                break
            }
            i++
        }

        val dir = file.parent
        if (dir == proj.basePath) {
            return null
        }
        return getOwner(dir + File.separator + packageInfo)
    }
}
