package app.filemanager.service.templates

import io.ktor.server.html.*
import kotlinx.html.*

/**
 * 基础HTML模板，定义共用的HTML结构和样式
 */
abstract class BaseTemplate {
    /**
     * 应用通用的头部元素
     */
    protected fun HEAD.applyCommonHead(title: String) {
        meta { charset = "UTF-8" }
        meta { name = "viewport"; content = "width=device-width, initial-scale=1.0" }
        title { +title }
        link { rel = "stylesheet"; href = "/static/all.min.css" }
        link { rel = "stylesheet"; href = "/static/shared-styles.css" }
        script { src = "/static/tailwindcss.js" }
        script { src = "/static/theme-config.js" }
    }
    
    /**
     * 应用通用的body类
     */
    protected fun BODY.applyBodyClasses() {
        classes = setOf("bg-surface", "dark:bg-dark-surface", "text-background-on", "dark:text-dark-background-on")
    }
} 