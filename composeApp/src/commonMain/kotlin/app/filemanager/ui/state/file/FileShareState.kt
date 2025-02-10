package app.filemanager.ui.state.file

import androidx.compose.runtime.mutableStateMapOf

class FileShareState {
    val sendFile = mutableStateMapOf<String,FileShareStatus>()
}


/**
 * 表示文件共享的状态。
 */
enum class FileShareStatus {
    /**
     * 表示文件正在传输中的状态。
     */
    SENDING,
    /**
     * 表示文件共享状态为已被拒绝。
     * 用于标识文件共享请求被拒绝的情况。
     */
    REJECTED,
    /**
     * 表示文件共享过程中的错误状态。
     * 该状态指示文件共享未成功完成，并出现错误或异常情况。
     */
    ERROR,
    /**
     * 表示文件共享已完成的状态。
     * 通常在文件成功发送并被接收方接收后使用。
     */
    COMPLETED,
    /**
     * 表示文件共享正在等待的状态。
     * 此状态通常用于标识文件共享请求已发送，但尚未收到接收方的响应。
     */
    WAITING
}

/**
 * 表示文件共享的状态类型。
 * 用于定义文件共享操作的当前状态。
 */
enum class FileShareLikeCategory {
    /**
     * 表示文件共享过程中的等待状态。
     * 此状态下操作可能尚未开始或正在排队中。
     */
    WAITING,
    /**
     * 表示文件分享操作正在进行的状态。
     */
    RUNNING,
    /**
     * 表示文件共享中被拒绝的状态。
     *
     * 此状态表明共享操作已经被明确拒绝，无法继续进行。
     */
    REJECTED
}