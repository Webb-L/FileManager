package app.filemanager.service.templates

import app.filemanager.data.file.FileSimpleInfo
import kotlinx.html.*

/**
 * 索引页面模板
 */
class IndexTemplate : BaseTemplate() {
    /**
     * 生成共享文件列表页面
     */
    fun render(
        search: String? = null,
        files: List<FileSimpleInfo> = emptyList(),
        scripts: Map<String, Pair<String, String>> = emptyMap()
    ): HTML.() -> Unit = {
        lang = "zh"
        head {
            applyCommonHead("文件管理")
            style {
                unsafe {
                    +"""
                        pre {
                            white-space: pre-wrap;       /* CSS 3 */
                            word-wrap: break-word;       /* IE 5.5+ */
                            overflow-wrap: break-word;
                        }
                        
                        .script-content {
                            max-height: 300px;
                            overflow-y: auto;
                        }
                    """.trimIndent()
                }
            }
        }
        body {
            applyBodyClasses()

            div {
                classes = setOf("min-h-screen", "mx-auto", "max-w-[1440px]", "p-4", "md:p-6")

                div {
                    classes =
                        setOf("bg-surface-container-lowest", "dark:bg-dark-surface-container-low", "rounded-xl", "p-6")

                    // 头部区域
                    div {
                        classes = setOf(
                            "flex",
                            "flex-col",
                            "sm:flex-row",
                            "justify-between",
                            "items-start",
                            "sm:items-center",
                            "gap-4",
                            "mb-8"
                        )

                        div {
                            h1 {
                                classes = setOf("text-2xl", "font-medium", "text-primary", "dark:text-dark-primary")
                                +"文件管理"
                            }
                            p {
                                classes =
                                    setOf("text-sm", "text-background-on/60", "dark:text-dark-background-on/60", "mt-1")
                                +"管理您的所有文件和文件夹"
                            }
                        }

                        div {
                            classes = setOf("flex", "gap-2")
                            a {
                                href = "/"
                                classes = setOf(
                                    "inline-flex",
                                    "items-center",
                                    "px-4",
                                    "py-2",
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
                                i { classes = setOf("fas", "fa-home", "mr-2") }
                                +"返回首页"
                            }
                        }
                    }

                    // 搜索区域
                    div {
                        classes = setOf("flex", "flex-wrap", "gap-4", "mb-6")

                        div {
                            classes = setOf("flex-1", "min-w-[240px]")
                            form {
                                action = ""
                                method = FormMethod.get
                                classes = setOf("relative")

                                input {
                                    type = InputType.text
                                    name = "search"
                                    id = "searchInput"
                                    placeholder = "搜索文件名称"
                                    value = search ?: ""
                                    classes = setOf(
                                        "w-full", "pl-12", "pr-14", "py-3", "border", "border-outline/30",
                                        "dark:border-dark-outline/30", "rounded-full", "focus:outline-none",
                                        "focus:border-primary", "dark:focus:border-dark-primary", "text-sm",
                                        "bg-white", "dark:bg-dark-surface-container"
                                    )
                                }

                                i {
                                    classes = setOf(
                                        "fas",
                                        "fa-search",
                                        "absolute",
                                        "left-5",
                                        "top-1/2",
                                        "-translate-y-1/2",
                                        "text-outline",
                                        "dark:text-dark-outline",
                                        "w-4",
                                        "h-4",
                                        "flex",
                                        "justify-center",
                                        "items-center"
                                    )
                                }

                                button {
                                    type = ButtonType.submit
                                    classes = setOf(
                                        "absolute", "right-4", "top-1/2", "-translate-y-1/2",
                                        "text-primary", "dark:text-dark-primary"
                                    )
                                    i { classes = setOf("fas", "fa-arrow-right") }
                                }
                            }
                        }
                    }

                    // 文件网格
                    renderFileGrid(files)
                }
            }

            // 浮动批量下载按钮
            renderFloatingButton()

            // 批量下载弹窗
            renderBatchDownloadModal(scripts)

            // JavaScript
            renderJavaScript()
        }
    }

    private fun DIV.renderFileGrid(files: List<FileSimpleInfo>) {
        div {
            id = "fileGrid"
            classes = setOf(
                "grid", "grid-cols-1", "sm:grid-cols-2", "lg:grid-cols-3",
                "xl:grid-cols-4", "2xl:grid-cols-5", "gap-4"
            )

            if (files.isEmpty()) {
                div {
                    classes = setOf("col-span-full", "text-center", "py-8")
                    i {
                        classes = setOf(
                            "fas", "fa-info-circle", "text-4xl",
                            "text-outline/50", "dark:text-dark-outline/50", "mb-3"
                        )
                    }
                    p {
                        classes = setOf("text-background-on/60", "dark:text-dark-background-on/60")
                        +"未找到匹配的文件或文件夹"
                    }
                }
            } else {
                for (file in files) {
                    if (file.isDirectory) {
                        a {
                            href = file.path
                            classes = setOf(
                                "file-card", "flex", "justify-between", "items-center", "p-4",
                                "bg-white", "dark:bg-dark-surface-container", "border", "border-outline/10",
                                "dark:border-dark-outline/10", "rounded-lg", "hover-state-layer",
                                "active-state-layer", "ripple"
                            )

                            div {
                                classes = setOf("flex", "items-center", "overflow-hidden")
                                div {
                                    classes = setOf(
                                        "w-10", "h-10", "rounded-full", "bg-primary-light/30",
                                        "dark:bg-dark-primary-light/30", "flex", "items-center",
                                        "justify-center", "mr-4", "flex-shrink-0"
                                    )
                                    i { classes = setOf("fas", "fa-folder", "text-primary", "dark:text-dark-primary") }
                                }
                                span {
                                    classes = setOf("text-sm", "font-medium", "truncate")
                                    +file.name
                                }
                            }

                            button {
                                classes = setOf(
                                    "text-primary", "dark:text-dark-primary", "p-2", "rounded-full",
                                    "hover:bg-primary-light/20", "dark:hover:bg-dark-primary-light/20",
                                    "transition-colors", "flex-shrink-0"
                                )
                                i { classes = setOf("fas", "fa-chevron-right") }
                            }
                        }
                    } else {
                        // 文件使用下载链接
                        a {
                            href = file.path
                            attributes["download"] = file.name
                            classes = setOf(
                                "file-card", "flex", "justify-between", "items-center", "p-4",
                                "bg-white", "dark:bg-dark-surface-container", "border", "border-outline/10",
                                "dark:border-dark-outline/10", "rounded-lg", "hover-state-layer",
                                "active-state-layer", "ripple"
                            )

                            div {
                                classes = setOf("flex", "items-center", "overflow-hidden")
                                div {
                                    classes = setOf(
                                        "w-10", "h-10", "rounded-full", "bg-secondary-light/30",
                                        "dark:bg-dark-secondary-light/30", "flex", "items-center",
                                        "justify-center", "mr-4", "flex-shrink-0"
                                    )
                                    i {
                                        classes = setOf("fas", "fa-file", "text-secondary", "dark:text-dark-secondary")
                                    }
                                }
                                span {
                                    classes = setOf("text-sm", "font-medium", "truncate")
                                    +file.name
                                }
                            }

                            div {
                                classes = setOf(
                                    "text-background-on/50", "dark:text-dark-background-on/50",
                                    "flex", "items-center"
                                )
                                i { classes = setOf("fas", "fa-download", "text-secondary", "ml-2", "text-sm") }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun BODY.renderFloatingButton() {
        div {
            classes = setOf("fixed", "bottom-8", "right-8")
            button {
                id = "floatingBatchDownloadBtn"
                classes = setOf(
                    "flex", "items-center", "px-4", "py-3", "rounded-full", "bg-primary",
                    "dark:bg-dark-primary", "text-primary-on", "dark:text-dark-primary-on", "shadow-lg",
                    "hover:bg-primary-dark", "dark:hover:bg-dark-primary-light", "transition-colors"
                )
                i { classes = setOf("fas", "fa-download", "mr-2", "text-lg") }
                span {
                    classes = setOf("font-medium")
                    +"批量下载"
                }
            }
        }
    }

    private fun BODY.renderBatchDownloadModal(scripts: Map<String, Pair<String, String>>) {
        div {
            id = "batchDownloadModal"
            classes = setOf(
                "fixed", "inset-0", "bg-black", "bg-opacity-50", "flex", "items-center",
                "justify-center", "z-50", "hidden"
            )

            div {
                classes = setOf(
                    "bg-white", "dark:bg-dark-surface-container", "rounded-xl", "p-6",
                    "max-w-lg", "w-full", "mx-4", "shadow-xl"
                )

                div {
                    classes = setOf("flex", "items-center", "mb-4")
                    h3 {
                        classes = setOf("text-xl", "font-medium", "text-primary", "dark:text-dark-primary")
                        +"批量下载"
                    }
                }

                div {
                    classes = setOf("mb-6")

                    // 脚本类型切换标签
                    div {
                        classes = setOf("border-b", "border-outline/20", "dark:border-dark-outline/20", "mb-4")
                        div {
                            classes = setOf("flex")

                            var firstScript = true
                            for ((scriptType, scriptInfo) in scripts) {
                                button {
                                    classes = setOf("script-tab", "px-4", "py-2", "border-b-2") +
                                            if (firstScript)
                                                setOf(
                                                    "border-primary", "dark:border-dark-primary", "text-primary",
                                                    "dark:text-dark-primary", "font-medium"
                                                )
                                            else
                                                setOf(
                                                    "border-transparent", "text-background-on/60",
                                                    "dark:text-dark-background-on/60", "hover:text-primary",
                                                    "dark:hover:text-dark-primary"
                                                )

                                    attributes["data-tab"] = scriptType.lowercase()
                                    attributes["data-ext"] = scriptInfo.first
                                    +scriptType
                                }
                                firstScript = false
                            }
                        }
                    }

                    // 脚本内容
                    div {
                        classes = setOf("script-content")

                        var firstScript = true
                        for ((scriptType, scriptInfo) in scripts) {
                            div {
                                id = "${scriptType.lowercase()}-script"
                                classes = setOf(
                                    "bg-gray-100", "dark:bg-dark-surface-container-highest",
                                    "rounded", "p-3", "text-sm", "font-mono"
                                ) +
                                        if (firstScript) emptySet() else setOf("hidden")

                                div {
                                    classes = setOf("script-container")
                                    pre { +scriptInfo.second }
                                }
                            }
                            firstScript = false
                        }
                    }
                }

                div {
                    classes = setOf("flex", "justify-end", "gap-3")
                    button {
                        id = "closeModal"
                        classes = setOf(
                            "px-4", "py-2", "text-sm", "font-medium", "border", "border-outline/30",
                            "dark:border-dark-outline/30", "text-background-on", "dark:text-dark-background-on",
                            "rounded-full", "hover:bg-surface-container", "dark:hover:bg-dark-surface-container-high",
                            "transition-colors"
                        )
                        +"取消"
                    }
                    button {
                        id = "downloadScript"
                        classes = setOf(
                            "px-4", "py-2", "text-sm", "font-medium", "bg-primary",
                            "dark:bg-dark-primary", "text-primary-on", "dark:text-dark-primary-on",
                            "rounded-full", "hover:bg-primary-dark", "dark:hover:bg-dark-primary-light",
                            "transition-colors"
                        )
                        +"下载脚本"
                    }
                }
            }
        }
    }

    private fun BODY.renderJavaScript() {
        script {
            unsafe {
                +"""
                document.addEventListener('DOMContentLoaded', function() {
                    const floatingBtn = document.getElementById('floatingBatchDownloadBtn');
                    const modal = document.getElementById('batchDownloadModal');
                    const closeBtn = document.getElementById('closeModal');
                
                    // 获取第一个脚本标签的信息作为默认值
                    const firstTab = document.querySelector('.script-tab');
                    let currentTab = firstTab ? firstTab.getAttribute('data-tab') : '';
                    let currentExt = firstTab ? firstTab.getAttribute('data-ext') : '';
                
                    // 显示弹窗
                    floatingBtn.addEventListener('click', function() {
                        modal.classList.remove('hidden');
                    });
                
                    // 关闭弹窗
                    closeBtn.addEventListener('click', function() {
                        modal.classList.add('hidden');
                    });
                
                    // 点击背景关闭弹窗
                    modal.addEventListener('click', function(e) {
                        if (e.target === modal) {
                            modal.classList.add('hidden');
                        }
                    });
                
                    // 标签切换
                    const tabs = document.querySelectorAll('.script-tab');
                    tabs.forEach(tab => {
                        tab.addEventListener('click', function() {
                            // 获取当前标签名称和文件扩展名
                            currentTab = this.getAttribute('data-tab');
                            currentExt = this.getAttribute('data-ext');
                
                            // 移除所有标签的激活状态
                            tabs.forEach(t => {
                                t.classList.remove('border-primary', 'dark:border-dark-primary', 'text-primary', 'dark:text-dark-primary', 'font-medium');
                                t.classList.add('border-transparent', 'text-background-on/60', 'dark:text-dark-background-on/60');
                            });
                
                            // 激活当前标签
                            this.classList.remove('border-transparent', 'text-background-on/60', 'dark:text-dark-background-on/60');
                            this.classList.add('border-primary', 'dark:border-dark-primary', 'text-primary', 'dark:text-dark-primary', 'font-medium');
                
                            // 隐藏所有脚本内容
                            document.querySelectorAll('.script-content > div').forEach(content => {
                                content.classList.add('hidden');
                            });
                
                            // 显示当前脚本内容
                            document.getElementById(currentTab + '-script').classList.remove('hidden');
                        });
                    });
                
                    // 下载脚本按钮
                    document.getElementById('downloadScript').addEventListener('click', function() {
                        // 获取当前显示的脚本
                        let scriptText = '';
                        let scriptElement = document.getElementById(currentTab + '-script');
                        if (scriptElement) {
                            scriptText = scriptElement.querySelector('pre').textContent;
                        }
                
                        // 创建Blob对象
                        const blob = new Blob([scriptText], { type: 'text/plain' });
                
                        // 创建下载链接
                        const url = window.URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.style.display = 'none';
                        a.href = url;
                
                        // 设置文件名
                        a.download = 'download_script.' + currentExt;
                
                        // 触发下载
                        document.body.appendChild(a);
                        a.click();
                
                        // 清理
                        window.URL.revokeObjectURL(url);
                        document.body.removeChild(a);
                    });
                });
                """.trimIndent()
            }
        }
    }
} 