package app.filemanager.service.templates

/**
 * 模板工厂类，用于统一管理所有模板
 */
object TemplateFactory {
    // 将属性改为公开，这样外部可以直接访问
    val indexTemplate by lazy { IndexTemplate() }
    val passwordTemplate by lazy { PasswordTemplate() }
    val waitingTemplate by lazy { WaitingTemplate() }
}