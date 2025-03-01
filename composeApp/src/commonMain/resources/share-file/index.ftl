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
</head>
<body class="bg-surface dark:bg-dark-surface text-background-on dark:text-dark-background-on">
    <div class="min-h-screen mx-auto max-w-[1440px] p-4 md:p-6">
        <div class="bg-surface-container-lowest dark:bg-dark-surface-container-low rounded-xl p-6">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
                <div>
                    <h1 class="text-2xl font-medium text-primary dark:text-dark-primary">文件管理</h1>
                    <p class="text-sm text-background-on/60 dark:text-dark-background-on/60 mt-1">管理您的所有文件和文件夹</p>
                </div>
                <a href="/" class="inline-flex items-center px-4 py-2 bg-primary dark:bg-dark-primary text-primary-on dark:text-dark-primary-on rounded-full text-sm font-medium hover:bg-primary-dark dark:hover:bg-dark-primary-light transition-colors ripple">
                    <i class="fas fa-home mr-2"></i>
                    返回首页
                </a>
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
                        <i class="fas fa-search text-4xl text-outline/50 dark:text-dark-outline/50 mb-3"></i>
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
                                    <i class="fas fa-arrow-right w-5 h-5 flex justify-center items-center"></i>
                                </button>
                            </a>
                        <#else>
                            <a href="${file.path}" class="file-card flex justify-between items-center p-4 bg-white dark:bg-dark-surface-container border border-outline/10 dark:border-dark-outline/10 rounded-lg hover-state-layer active-state-layer ripple">
                                <div class="flex items-center overflow-hidden">
                                    <div class="w-10 h-10 rounded-full bg-secondary-light/30 dark:bg-dark-secondary/30 flex items-center justify-center mr-4 flex-shrink-0">
                                        <i class="far fa-file text-secondary dark:text-dark-secondary"></i>
                                    </div>
                                    <span class="text-sm font-medium truncate">${file.name}</span>
                                </div>
                                <button class="text-primary dark:text-dark-primary p-2 rounded-full hover:bg-primary-light/20 dark:hover:bg-dark-primary-light/20 transition-colors flex-shrink-0">
                                    <i class="fas fa-download w-5 h-5 flex justify-center items-center"></i>
                                </button>
                            </a>
                        </#if>
                    </#list>
                </#if>
            </div>
        </div>
    </div>
    
    <script src="/static/index.js"></script>
</body>
</html>