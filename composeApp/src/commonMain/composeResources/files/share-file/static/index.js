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


// async function downloadFileInChunks(url, chunkSize) {
//     const response = await fetch(url);
//     console.log(await response.body);
//     console.log(response.headers.get('Content-Length'));
//     const totalSize = parseInt(response.headers.get('Content-Length'), 10);
//     const totalChunks = Math.ceil(totalSize / chunkSize);
//     console.log(totalChunks, totalSize);
//
//     const fileChunks = [];
//
//     for (let i = 0; i < totalChunks; i++) {
//         const start = i * chunkSize;
//         const end = Math.min(start + chunkSize - 1, totalSize - 1);
//
//         const chunkResponse = await fetch(url, {
//             headers: {
//                 'Range': `bytes=${start}-${end}`
//             }
//         });
//
//         if (!chunkResponse.ok) {
//             throw new Error(`Failed to fetch chunk ${i}: ${chunkResponse.statusText}`);
//         }
//
//         const chunkBlob = await chunkResponse.blob();
//         fileChunks.push(chunkBlob);
//     }
//
//     // 合并所有的Blob
//     const finalBlob = new Blob(fileChunks);
//
//     // 创建下载链接
//     const downloadUrl = URL.createObjectURL(finalBlob);
//     const a = document.createElement('a');
//     a.href = downloadUrl;
//     a.download = 'downloaded_file.bin'; // 文件名
//     document.body.appendChild(a);
//     a.click();
//
//     // 清理
//     setTimeout(() => {
//         URL.revokeObjectURL(downloadUrl);
//         document.body.removeChild(a);
//     }, 100);
// }

// 使用示例
const fileUrl = 'http://127.0.0.1:12040/1Gfile'; // 替换为实际文件URL
// const chunkSize = 8192; // 1MB
// downloadFileInChunks(fileUrl, chunkSize);

async function fetchFileWithRange(url, options = {}) {
    const {
        onProgress, // 进度回调函数
        onData,     // 数据回调函数
        abortController = new AbortController(), // 用于取消请求
    } = options;

    try {
        // 首先尝试获取文件信息
        const fileInfoResponse = await fetch(url);
        if (!fileInfoResponse.ok) {
            if (fileInfoResponse.status === 404) {
                throw new Error('文件未找到');
            }
            throw new Error(`请求失败: ${fileInfoResponse.status}`);
        }

        // 获取文件大小
        const contentLength = fileInfoResponse.headers.get('content-length');
        console.log("contentLength", contentLength);
        const totalSize = parseInt(contentLength || '0');

        if (totalSize === 0) {
            // 如果文件大小为0或无法获取，直接下载整个文件
            const blob = await fileInfoResponse.blob();
            return blob;
        }

        // 使用 Range 请求分块下载
        let downloadedSize = 0;
        const chunkSize = 1024 * 1024; // 1MB 分块
        const chunks = [];

        while (downloadedSize < totalSize) {
            const start = downloadedSize;
            const end = Math.min(downloadedSize + chunkSize - 1, totalSize - 1);

            const response = await fetch(url, {
                headers: {
                    'Range': `bytes=${start}-${end}`
                },
                signal: abortController.signal
            });

            if (!response.ok && response.status !== 206) {
                throw new Error(`分块下载失败: ${response.status}`);
            }

            const chunk = await response.blob();
            chunks.push(chunk);
            downloadedSize += chunk.size;

            // 计算下载进度
            const progress = (downloadedSize / totalSize) * 100;

            // 调用进度回调
            if (onProgress) {
                onProgress({
                    loaded: downloadedSize,
                    total: totalSize,
                    progress: progress
                });
            }

            // 调用数据回调
            if (onData) {
                onData(chunk);
            }
        }

        // 合并所有分块
        const finalBlob = new Blob(chunks);
        return finalBlob;
    } catch (error) {
        if (error.name === 'AbortError') {
            console.log('下载已取消');
        }
        throw error;
    }
}

// 使用示例：
// async function downloadFile(url, filename) {
//     try {
//         // 创建 AbortController 用于取消下载
//         const abortController = new AbortController();
//
//         // 开始下载
//         const blob = await fetchFileWithRange(url, {
//             onProgress: (progressData) => {
//                 console.log(`下载进度: ${progressData.progress.toFixed(2)}%`);
//             },
//             abortController: abortController
//         });
//
//         // 创建下载链接
//         const downloadUrl = URL.createObjectURL(blob);
//         const link = document.createElement('a');
//         link.href = downloadUrl;
//         link.download = filename;
//
//         // 触发下载
//         document.body.appendChild(link);
//         link.click();
//         document.body.removeChild(link);
//
//         // 清理
//         URL.revokeObjectURL(downloadUrl);
//
//         console.log('下载完成');
//     } catch (error) {
//         console.error('下载失败:', error);
//     }
// }

// 使用方法
// downloadFile(fileUrl, 'example.pdf');

// 如果需要取消下载，可以这样使用：
// const abortController = new AbortController();
// fetchFileWithRange(fileUrl, {
//     abortController,
//     onProgress: (progressData) => {
//         console.log(`下载进度: ${progressData.progress.toFixed(2)}%`);
//     }
// }).then(blob => {
//     // 处理下载完成的文件
// }).catch(error => {
//     console.error('下载出错:', error);
// });

// 需要取消时调用：
// abortController.abort();