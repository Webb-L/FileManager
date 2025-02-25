<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>文件管理系统</title>
    <link href="/static/all.min.css" rel="stylesheet">
    <script src="/static/tailwindcss.js"></script>
    <script>
        tailwind.config = {
            theme: {
                extend: {
                    colors: {
                        primary: '#6750A4',
                        secondary: '#958DA5'
                    },
                    borderRadius: {
                        'none': '0px',
                        'sm': '2px',
                        DEFAULT: '4px',
                        'md': '8px',
                        'lg': '12px',
                        'xl': '16px',
                        '2xl': '20px',
                        '3xl': '24px',
                        'full': '9999px',
                        'button': '4px'
                    }
                }
            }
        }
    </script>
    <style>
        body {
            font-family: 'Roboto', sans-serif;
        }

        input[type="number"]::-webkit-inner-spin-button,
        input[type="number"]::-webkit-outer-spin-button {
            -webkit-appearance: none;
            margin: 0;
        }

        .table-header {
            background-color: #F4F0F9;
        }

        .file-row:hover {
            background-color: #F8F5FF;
        }
    </style>
</head>
<body class="text-gray-800">
<div class="min-h-screen mx-auto max-w-[1440px]">
    <div class="bg-white rounded-lg shadow-sm p-6">
        <div class="flex justify-between items-center mb-8">
            <h1 class="text-2xl font-medium">文件管理</h1>
        </div>
        <div class="flex flex-wrap gap-4 mb-6">
            <div class="flex-1 min-w-[240px]">
                <div class="relative">
                    <input type="text" placeholder="搜索文件名称"
                           class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-md focus:outline-none focus:border-primary text-sm">
                    <i class="fas fa-search absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 w-4 h-4 flex justify-center items-center"></i>
                </div>
            </div>
        </div>
        <div class="space-y-2">
            <#list files as file>
                <#if file.directory>
                    <a href="${file.path}" class="flex justify-between items-center p-4 bg-white border border-gray-100 rounded-lg hover:bg-gray-50">
                        <div class="flex items-center">
                            <i class="fas fa-folder text-gray-400 mr-3 w-5 h-5 flex justify-center items-center"></i>
                            <span class="text-sm">${file.name}</span>
                        </div>
                        <button class="text-gray-500 hover:text-opacity-80 p-2 !rounded-button whitespace-nowrap">
                            <i class="fas fa-arrow-right w-4 h-4 flex justify-center items-center"></i>
                        </button>
                    </a>
                <#else>
                    <a href="${file.path}" class="flex justify-between items-center p-4 bg-white border border-gray-100 rounded-lg hover:bg-gray-50">
                        <div class="flex items-center">
                            <i class="far fa-solid fa-file text-gray-400 mr-3 w-5 h-5 flex justify-center items-center"></i>
                            <span class="text-sm">${file.name}</span>
                        </div>
                        <button class="text-primary hover:text-opacity-80 p-2 !rounded-button whitespace-nowrap">
                            <i class="fas fa-download w-4 h-4 flex justify-center items-center"></i>
                        </button>
                    </a>
                </#if>
            </#list>
        </div>
    </div>
</div>
<script src="/static/index.js"></script>
</body>
</html>