#!/bin/bash

# API服务器地址设为常量
API_SERVER="http://127.0.0.1:12040"

# User-Agent常量
USER_AGENT="FileDownloader/1.0 (Custom Shell Script)"

# 设置本地目标目录为当前目录
TARGET_DIR="."

# 如果目标目录不为空，则要求用户确认
if [ "$(ls -A "$TARGET_DIR" 2>/dev/null)" ]; then
  echo "警告: 当前目录不是空目录，下载操作可能会覆盖现有文件。"
  echo "按回车键继续，或按任意其他键取消操作..."
  read -n 1 key
  if [ "$key" != "" ]; then
    echo "操作已取消。"
    exit 1
  fi
  echo "用户已确认，将继续覆盖下载..."
fi

# 初始化全局计数器
TOTAL_FILES=0
TOTAL_DIRS=0
SUCCESS_FILES=0
FAILED_FILES=0
SUCCESS_DIRS=0
FAILED_DIRS=0

# 递归处理文件和目录的函数
process_files_and_directories() {
    local remote_path=$1
    local local_path=$2

    echo "处理路径: $remote_path -> $local_path"

    # 发送 HTTP 请求获取当前路径的内容
    response=$(curl -s -H "X-API-Request: true" -H "User-Agent: ${USER_AGENT}" "${API_SERVER}${remote_path}")

    # 检查响应是否有效
    if ! echo "$response" | jq -e . >/dev/null 2>&1; then
        echo "错误: 无法解析路径 $remote_path 的响应"
        return 1
    fi

    while read -r item; do
        # 跳过空行
        [ -z "$item" ] && continue

        name=$(echo "$item" | jq -r '.name')
        is_dir=$(echo "$item" | jq -r '.isDirectory')
        item_path=$(echo "$item" | jq -r '.path')
        local_item_path="$local_path/$name"

        if [ "$is_dir" == "true" ]; then
            # 如果是目录，创建对应的本地目录
            echo "创建目录: $local_item_path"
            ((TOTAL_DIRS++))

            if mkdir -p "$local_item_path"; then
                ((SUCCESS_DIRS++))
            else
                echo "错误: 无法创建目录 $local_item_path"
                ((FAILED_DIRS++))
                continue
            fi

            # 递归处理子目录
            process_files_and_directories "$item_path" "$local_item_path"

        else
            # 如果是文件，下载文件
            echo "下载文件: $item_path -> $local_item_path"
            ((TOTAL_FILES++))

            if curl -s -f -H "X-API-Request: true" -H "User-Agent: ${USER_AGENT}" "${API_SERVER}${item_path}" -o "$local_item_path"; then
                ((SUCCESS_FILES++))
            else
                echo "错误: 无法下载文件 $item_path"
                ((FAILED_FILES++))
            fi
        fi
    done < <(echo "$response" | jq -c '.[]')
}

# 从根目录开始处理
echo "开始下载文件和创建目录到当前文件夹..."
process_files_and_directories "/" "$TARGET_DIR"

# 打印统计结果
echo "========== 下载统计 =========="
echo "总文件夹数: $TOTAL_DIRS"
echo "成功创建的文件夹: $SUCCESS_DIRS"
echo "创建失败的文件夹: $FAILED_DIRS"
echo "总文件数: $TOTAL_FILES"
echo "成功下载的文件: $SUCCESS_FILES"
echo "下载失败的文件: $FAILED_FILES"
echo "============================="

# 计算总成功率
TOTAL_ITEMS=$((TOTAL_FILES + TOTAL_DIRS))
SUCCESS_ITEMS=$((SUCCESS_FILES + SUCCESS_DIRS))

if [ $TOTAL_ITEMS -gt 0 ]; then
    SUCCESS_RATE=$(( SUCCESS_ITEMS * 100 / TOTAL_ITEMS ))
    echo "总成功率: ${SUCCESS_RATE}%"
fi

echo "完成!"