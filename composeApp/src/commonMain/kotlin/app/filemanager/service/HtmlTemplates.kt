package app.filemanager.service

import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.service.templates.TemplateFactory
import kotlinx.html.HTML

/**
 * HTML DSL模板生成器，用于替代FreeMarker模板
 * 现在使用拆分后的模板结构
 */
object HtmlTemplates {

    /**
     * 生成共享文件列表页面
     */
    fun indexPage(
        search: String? = null,
        files: List<FileSimpleInfo> = emptyList(),
        scripts: Map<String, Pair<String, String>> = emptyMap()
    ): HTML.() -> Unit = TemplateFactory.indexTemplate.render(search, files, scripts)

    /**
     * 生成密码验证页面
     */
    fun passwordPage(error: String? = null): HTML.() -> Unit =
        TemplateFactory.passwordTemplate.render(error)

    /**
     * 生成等待页面
     */
    fun waitingPage(): HTML.() -> Unit = TemplateFactory.waitingTemplate.render()
} 