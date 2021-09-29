package com.brex.plugins.codeowners

class CodeOwnerRule(ow: String, res: String, line: Int, location: String) {
    private val owner: String = sanitize(ow)
    private val context: String = sanitize(res)
    val lineNumber: Int = line
    val annotationLocation : String = location

    fun getPopup(): List<String> {
       return listOf("Owner: $owner", "Context: $context")
    }

    fun getOwner() : String {
        return "Owner: $owner"
    }

    fun sanitize(javaPath : String) : String {
        if ("." in javaPath) {
            return javaPath.split(".").last()
        }
        return javaPath
    }
}