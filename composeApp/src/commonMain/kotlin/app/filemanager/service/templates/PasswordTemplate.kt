package app.filemanager.service.templates

import kotlinx.html.*

/**
 * 密码验证页面模板
 */
class PasswordTemplate : BaseTemplate() {
    /**
     * 生成密码验证页面
     */
    fun render(error: String? = null): HTML.() -> Unit = {
        lang = "zh"
        head {
            applyCommonHead("访问验证")
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
                        "p-6",
                        "w-full",
                        "max-w-md"
                    )

                    div {
                        classes = setOf("text-center", "mb-8")
                        h1 {
                            classes = setOf("text-2xl", "font-medium", "text-primary", "dark:text-dark-primary")
                            +"访问验证"
                        }
                        p {
                            classes =
                                setOf("text-sm", "text-background-on/60", "dark:text-dark-background-on/60", "mt-1")
                            +"此内容需要密码才能访问"
                        }
                    }

                    form {
                        action = ""
                        method = FormMethod.get
                        classes = setOf("space-y-6")

                        div {
                            classes = setOf("relative")
                            input {
                                type = InputType.password
                                name = "pwd"
                                id = "passwordInput"
                                placeholder = "请输入访问密码"
                                classes = setOf(
                                    "w-full", "pl-12", "pr-14", "py-3", "border", "border-outline/30",
                                    "dark:border-dark-outline/30", "rounded-full", "focus:outline-none",
                                    "focus:border-primary", "dark:focus:border-dark-primary", "text-sm",
                                    "bg-white", "dark:bg-dark-surface-container"
                                )
                            }
                            i {
                                classes = setOf(
                                    "fas", "fa-lock", "absolute", "left-5", "top-1/2", "-translate-y-1/2",
                                    "text-outline", "dark:text-dark-outline", "w-4", "h-4", "flex",
                                    "justify-center", "items-center"
                                )
                            }
                        }

                        if (error != null) {
                            div {
                                classes = setOf("text-error", "dark:text-dark-error", "text-sm", "text-center")
                                i { classes = setOf("fas", "fa-exclamation-circle", "mr-2") }
                                +error
                            }
                        }

                        button {
                            type = ButtonType.submit
                            classes = setOf(
                                "w-full",
                                "py-3",
                                "bg-primary",
                                "dark:bg-dark-primary",
                                "text-primary-on",
                                "dark:text-dark-primary-on",
                                "rounded-full",
                                "text-sm",
                                "font-medium",
                                "hover:bg-primary-dark",
                                "dark:hover:bg-dark-primary-light",
                                "transition-colors",
                                "ripple"
                            )
                            i { classes = setOf("fas", "fa-arrow-right", "mr-2") }
                            +"验证访问"
                        }
                    }
                }
            }

            script { src = "/static/index.js" }
        }
    }
} 