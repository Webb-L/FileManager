function downloadLargeFile() {
    // 创建一个Blob对象，内容为10GB的空白数据
    const sizeInGB = 10;
    const sizeInBytes = sizeInGB * 1024 * 1024 * 1024; // 10GB in bytes

    // 创建一个Uint8Array，填充数据
    const chunkSize = 1024 * 1024; // 1MB
    const chunks = Math.ceil(sizeInBytes / chunkSize);
    const blobParts = [];

    for (let i = 0; i < chunks; i++) {
        // 每次生成1MB的数据
        const part = new Uint8Array(chunkSize);
        blobParts.push(part);
    }

    // 创建Blob对象
    const blob = new Blob(blobParts, { type: 'application/octet-stream' });

    // 创建下载链接
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'largefile.bin'; // 文件名
    document.body.appendChild(a);
    a.click();

    // 清理
    setTimeout(() => {
        URL.revokeObjectURL(url);
        document.body.removeChild(a);
    }, 100);
}

// 调用函数以下载文件
// downloadLargeFile();


async function downloadFileInChunks(url, chunkSize) {
    const response = await fetch(url);
    const totalSize = parseInt(response.headers.get('Content-Length'), 10);
    const totalChunks = Math.ceil(totalSize / chunkSize);

    const fileChunks = [];

    for (let i = 0; i < totalChunks; i++) {
        const start = i * chunkSize;
        const end = Math.min(start + chunkSize - 1, totalSize - 1);

        const chunkResponse = await fetch(url, {
            headers: {
                'Range': `bytes=${start}-${end}`
            }
        });

        if (!chunkResponse.ok) {
            throw new Error(`Failed to fetch chunk ${i}: ${chunkResponse.statusText}`);
        }

        const chunkBlob = await chunkResponse.blob();
        fileChunks.push(chunkBlob);
    }

    // 合并所有的Blob
    const finalBlob = new Blob(fileChunks);

    // 创建下载链接
    const downloadUrl = URL.createObjectURL(finalBlob);
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = 'downloaded_file.bin'; // 文件名
    document.body.appendChild(a);
    a.click();

    // 清理
    setTimeout(() => {
        URL.revokeObjectURL(downloadUrl);
        document.body.removeChild(a);
    }, 100);
}

// 使用示例
const fileUrl = 'http://127.0.0.1:12040/%E4%B8%8B%E8%BD%BD/1Gfile'; // 替换为实际文件URL
const chunkSize = 1024 * 1024; // 1MB
downloadFileInChunks(fileUrl, chunkSize);