<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>访问验证</title>
    <link href="/static/all.min.css" rel="stylesheet">
    <link href="/static/shared-styles.css" rel="stylesheet">
    <script src="/static/tailwindcss.js"></script>
    <script src="/static/theme-config.js"></script>
</head>
<body class="bg-surface dark:bg-dark-surface text-background-on dark:text-dark-background-on">
    <div class="min-h-screen mx-auto max-w-[1440px] p-4 md:p-6 flex items-center justify-center">
        <div class="bg-surface-container-lowest dark:bg-dark-surface-container-low rounded-xl p-6 w-full max-w-md">
            <div class="text-center mb-8">
                <h1 class="text-2xl font-medium text-primary dark:text-dark-primary">访问验证</h1>
                <p class="text-sm text-background-on/60 dark:text-dark-background-on/60 mt-1">此内容需要密码才能访问</p>
            </div>
            
            <form action="" method="get" class="space-y-6">
                <div class="relative">
                    <input type="password" name="pwd" id="passwordInput" placeholder="请输入访问密码"
                           class="w-full pl-12 pr-14 py-3 border border-outline/30 dark:border-dark-outline/30 rounded-full focus:outline-none focus:border-primary dark:focus:border-dark-primary text-sm bg-white dark:bg-dark-surface-container">
                    <i class="fas fa-lock absolute left-5 top-1/2 -translate-y-1/2 text-outline dark:text-dark-outline w-4 h-4 flex justify-center items-center"></i>
                </div>
                
                <#if error??>
                    <div class="text-error dark:text-dark-error text-sm text-center">
                        <i class="fas fa-exclamation-circle mr-2"></i>
                        ${error}
                    </div>
                </#if>
                
                <button type="submit" 
                        class="w-full py-3 bg-primary dark:bg-dark-primary text-primary-on dark:text-dark-primary-on rounded-full text-sm font-medium hover:bg-primary-dark dark:hover:bg-dark-primary-light transition-colors ripple">
                    <i class="fas fa-arrow-right mr-2"></i>
                    验证访问
                </button>
            </form>
        </div>
    </div>
    
    <script src="/static/index.js"></script>
</body>
</html> 