<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文件管理</title>
    <link href="/static/all.min.css" rel="stylesheet">
    <link href="/static/shared-styles.css" rel="stylesheet">
    <script src="/static/tailwindcss.js"></script>
    <script src="/static/theme-config.js"></script>
    <style>
        pre {
            white-space: pre-wrap;       /* CSS 3 */
            word-wrap: break-word;       /* IE 5.5+ */
            overflow-wrap: break-word;
        }

        .script-content {
            max-height: 300px;
            overflow-y: auto;
        }
    </style>
</head>
<body class="bg-surface dark:bg-dark-surface text-background-on dark:text-dark-background-on">
<div class="min-h-screen mx-auto max-w-[1440px] p-4 md:p-6">
    <div class="bg-surface-container-lowest dark:bg-dark-surface-container-low rounded-xl p-6">
        <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
            <div>
                <h1 class="text-2xl font-medium text-primary dark:text-dark-primary">文件管理</h1>
                <p class="text-sm text-background-on/60 dark:text-dark-background-on/60 mt-1">管理您的所有文件和文件夹</p>
            </div>
            <div class="flex gap-2">
                <a href="/" class="inline-flex items-center px-4 py-2 bg-primary dark:bg-dark-primary text-primary-on dark:text-dark-primary-on rounded-full text-sm font-medium hover:bg-primary-dark dark:hover:bg-dark-primary-light transition-colors ripple">
                    <i class="fas fa-home mr-2"></i>
                    返回首页
                </a>
            </div>
        </div>
        <div class="flex flex-wrap gap-4 mb-6">
            <div class="flex-1 min-w-[240px]">
                <form action="" method="get" class="relative">
                    <input type="text" name="search" id="searchInput" placeholder="搜索文件名称" value="${search!''}"
                           class="w-full pl-12 pr-14 py-3 border border-outline/30 dark:border-dark-outline/30 rounded-full focus:outline-none focus:border-primary dark:focus:border-dark-primary text-sm bg-white dark:bg-dark-surface-container">
                    <i class="fas fa-search absolute left-5 top-1/2 -translate-y-1/2 text-outline dark:text-dark-outline w-4 h-4 flex justify-center items-center"></i>
                    <button type="submit" class="absolute right-4 top-1/2 -translate-y-1/2 text-primary dark:text-dark-primary">
                        <i class="fas fa-arrow-right"></i>
                    </button>
                </form>
            </div>
        </div>

        <div id="fileGrid" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-5 gap-4">
            <#if files?size == 0>
                <div class="col-span-full text-center py-8">
                    <i class="fas fa-info-circle text-4xl text-outline/50 dark:text-dark-outline/50 mb-3"></i>
                    <p class="text-background-on/60 dark:text-dark-background-on/60">未找到匹配的文件或文件夹</p>
                </div>
            <#else>
                <#list files as file>
                    <#if file.directory>
                        <a href="${file.path}" class="file-card flex justify-between items-center p-4 bg-white dark:bg-dark-surface-container border border-outline/10 dark:border-dark-outline/10 rounded-lg hover-state-layer active-state-layer ripple">
                            <div class="flex items-center overflow-hidden">
                                <div class="w-10 h-10 rounded-full bg-primary-light/30 dark:bg-dark-primary-light/30 flex items-center justify-center mr-4 flex-shrink-0">
                                    <i class="fas fa-folder text-primary dark:text-dark-primary"></i>
                                </div>
                                <span class="text-sm font-medium truncate">${file.name}</span>
                            </div>
                            <button class="text-primary dark:text-dark-primary p-2 rounded-full hover:bg-primary-light/20 dark:hover:bg-dark-primary-light/20 transition-colors flex-shrink-0">
                                <i class="fas fa-chevron-right"></i>
                            </button>
                        </a>
                    <#else>
                        <!-- 文件使用下载链接 -->
                        <a href="${file.path}" download="${file.name}" class="file-card flex justify-between items-center p-4 bg-white dark:bg-dark-surface-container border border-outline/10 dark:border-dark-outline/10 rounded-lg hover-state-layer active-state-layer ripple">
                            <div class="flex items-center overflow-hidden">
                                <div class="w-10 h-10 rounded-full bg-secondary-light/30 dark:bg-dark-secondary-light/30 flex items-center justify-center mr-4 flex-shrink-0">
                                    <i class="fas fa-file text-secondary dark:text-dark-secondary"></i>
                                </div>
                                <span class="text-sm font-medium truncate">${file.name}</span>
                            </div>
                            <div class="text-background-on/50 dark:text-dark-background-on/50 flex items-center">
                                <i class="fas fa-download text-secondary ml-2 text-sm"></i>
                            </div>
                        </a>
                    </#if>
                </#list>
            </#if>
        </div>
    </div>
</div>

<!-- 浮动批量下载按钮 -->
<div class="fixed bottom-8 right-8">
    <button id="floatingBatchDownloadBtn" class="flex items-center px-4 py-3 rounded-full bg-primary dark:bg-dark-primary text-primary-on dark:text-dark-primary-on shadow-lg hover:bg-primary-dark dark:hover:bg-dark-primary-light transition-colors">
        <i class="fas fa-download mr-2 text-lg"></i>
        <span class="font-medium">批量下载</span>
    </button>
</div>

<!-- 批量下载弹窗 -->
<div id="batchDownloadModal" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 hidden">
    <div class="bg-white dark:bg-dark-surface-container rounded-xl p-6 max-w-lg w-full mx-4 shadow-xl">
        <div class="flex items-center mb-4">
            <h3 class="text-xl font-medium text-primary dark:text-dark-primary">批量下载</h3>
        </div>
        <div class="mb-6">
            <!-- 脚本类型切换标签 -->
            <div class="border-b border-outline/20 dark:border-dark-outline/20 mb-4">
                <div class="flex">
                    <#assign firstScript = true>
                    <#list scripts?keys as scriptType>
                        <button class="script-tab px-4 py-2 border-b-2 ${firstScript?then('border-primary dark:border-dark-primary text-primary dark:text-dark-primary font-medium', 'border-transparent text-background-on/60 dark:text-dark-background-on/60 hover:text-primary dark:hover:text-dark-primary')}"
                                data-tab="${scriptType?lower_case}"
                                data-ext="${scripts[scriptType].first}">
                            ${scriptType}
                        </button>
                        <#assign firstScript = false>
                    </#list>
                </div>
            </div>

            <!-- 脚本内容 -->
            <div class="script-content">
                <#assign firstScript = true>
                <#list scripts?keys as scriptType>
                    <div id="${scriptType?lower_case}-script" class="bg-gray-100 dark:bg-dark-surface-container-highest rounded p-3 text-sm font-mono${firstScript?then('', ' hidden')}">
                        <div class="script-container">
                            <pre>${scripts[scriptType].second?html}</pre>
                        </div>
                    </div>
                    <#assign firstScript = false>
                </#list>
            </div>
        </div>
        <div class="flex justify-end gap-3">
            <button id="closeModal" class="px-4 py-2 text-sm font-medium border border-outline/30 dark:border-dark-outline/30 text-background-on dark:text-dark-background-on rounded-full hover:bg-surface-container dark:hover:bg-dark-surface-container-high transition-colors">
                取消
            </button>
            <button id="downloadScript" class="px-4 py-2 text-sm font-medium bg-primary dark:bg-dark-primary text-primary-on dark:text-dark-primary-on rounded-full hover:bg-primary-dark dark:hover:bg-dark-primary-light transition-colors">
                下载脚本
            </button>
        </div>
    </div>
</div>

<script>
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
</script>
</body>
</html>