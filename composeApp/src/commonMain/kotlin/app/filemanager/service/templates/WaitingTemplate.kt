package app.filemanager.service.templates

import kotlinx.html.*

/**
 * 等待页面模板
 */
class WaitingTemplate : BaseTemplate() {
    /**
     * 生成等待页面
     */
    fun render(): HTML.() -> Unit = {
        lang = "zh"
        head {
            applyCommonHead("文件准备中")
            meta { httpEquiv = "refresh"; content = "5" }
        }
        body {
            applyBodyClasses()

            div {
                classes = setOf(
                    "min-h-screen",
                    "mx-auto",
                    "max-w-[1440px]",
                    "p-4",
                    "md:p-6",
                    "flex",
                    "items-center",
                    "justify-center"
                )

                div {
                    classes = setOf(
                        "bg-surface-container-lowest",
                        "dark:bg-dark-surface-container-low",
                        "rounded-xl",
                        "p-8",
                        "text-center",
                        "w-full",
                        "max-w-md"
                    )

                    div {
                        classes = setOf("mb-6")
                        div {
                            classes = setOf(
                                "w-16",
                                "h-16",
                                "rounded-full",
                                "bg-primary-light/30",
                                "dark:bg-dark-primary-light/30",
                                "flex",
                                "items-center",
                                "justify-center",
                                "mx-auto"
                            )
                            i {
                                classes = setOf("fas", "fa-clock", "text-primary", "dark:text-dark-primary", "text-3xl")
                            }
                        }
                    }

                    h1 {
                        classes = setOf("text-2xl", "font-medium", "mb-4", "text-primary", "dark:text-dark-primary")
                        +"文件准备中"
                    }

                    p {
                        classes = setOf("text-background-on/60", "dark:text-dark-background-on/60", "text-sm", "mb-6")
                        +"对方正在准备文件，请稍候片刻..."
                    }

                    div {
                        classes = setOf("flex", "justify-center")
                        div {
                            classes = setOf(
                                "animate-spin",
                                "rounded-full",
                                "h-8",
                                "w-8",
                                "border-4",
                                "border-primary",
                                "dark:border-dark-primary",
                                "border-t-transparent"
                            )
                        }
                    }
                }
            }

            script { src = "/static/index.js" }
        }
    }
} 