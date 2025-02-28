<!DOCTYPE html>
<html lang="zh">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="5">
    <title>文件准备中</title>
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
    </style>
</head>
<body class="text-gray-800">
<div class="min-h-screen mx-auto max-w-[1440px] flex items-center justify-center">
    <div class="bg-white rounded-lg p-8 text-center w-full max-w-md">
        <div class="mb-6">
            <i class="fas fa-clock text-primary text-4xl"></i>
        </div>
        <h1 class="text-2xl font-medium mb-4">文件准备中</h1>
        <p class="text-secondary text-sm mb-6">对方正在准备文件，请稍候片刻...</p>
        <div class="flex justify-center">
            <div class="animate-spin rounded-full h-8 w-8 border-4 border-primary border-t-transparent"></div>
        </div>
    </div>
</div>
</body>
</html>