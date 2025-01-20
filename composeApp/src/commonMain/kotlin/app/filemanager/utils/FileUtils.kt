package app.filemanager.utils

import app.filemanager.data.file.FileSimpleInfo

/**
 * 文件工具类，用于处理文件操作的多平台实现。
 */
internal expect object FileUtils {
    /**
     * 根据指定的文件路径获取文件的基本信息。
     *
     * @param path 文件的完整路径。
     * @return 包含文件基本信息的 FileSimpleInfo 对象。
     */
    fun getFile(path: String): FileSimpleInfo

    /**
     * 根据指定路径和文件名获取文件的基本信息。
     *
     * @param path 文件所在的路径。
     * @param fileName 文件的名称。
     * @return 获取的文件基本信息，包含文件名、描述、是否为目录、是否隐藏、路径等信息。
     */
    fun getFile(path: String, fileName: String): FileSimpleInfo

    /**
     * 打开指定路径的文件。
     *
     * @param file 指定需要打开的文件路径，以字符串形式传入。
     */
    fun openFile(file: String)

    /**
     * 将指定路径的文件复制到目标路径。
     *
     * @param src 源文件路径。
     * @param dest 目标文件路径。
     * @return 如果复制成功则返回 true，否则返回 false。
     */
    fun copyFile(src: String, dest: String): Boolean

    /**
     * 移动文件方法，将指定路径的文件移动到目标路径。
     *
     * @param src 源文件的路径。
     * @param dest 目标文件的路径。
     * @return 如果文件移动成功，返回true；否则返回false。
     */
    fun moveFile(src: String, dest: String): Boolean

    /**
     * 删除指定路径的文件。
     *
     * @param path 文件的绝对路径。
     * @return 如果删除成功，返回Result封装的布尔值`true`；如果删除失败，返回Result封装的布尔值`false`以及相应的错误信息。
     */
    fun deleteFile(path: String): Result<Boolean>

    /**
     * 计算指定路径的总空间大小。
     *
     * @param path 要计算总空间的文件路径。
     * @return 指定路径的总空间大小，以字节为单位。
     */
    fun totalSpace(path: String): Long

    /**
     * 获取指定路径下的剩余存储空间大小。
     *
     * @param path 要查询的文件路径，表示需要获取剩余存储空间的目录或文件所在的路径。
     * @return 剩余存储空间的字节数，如果查询失败可能返回负值。
     */
    fun freeSpace(path: String): Long

    /**
     * 创建一个新的文件夹。
     *
     * @param path 指定新文件夹的路径。
     * @return 返回一个包含操作结果的Result对象。如果文件夹创建成功，返回Result.success(true)，
     * 如果创建失败，返回Result.failure或Result.success(false)。
     */
    fun createFolder(path: String): Result<Boolean>

    /**
     * 重命名指定路径下的文件或文件夹。
     *
     * @param path 要操作的目标路径。
     * @param oldName 需要重命名的文件或文件夹的当前名称。
     * @param newName 重命名后的新名称。
     * @return 包含操作结果的 [Result]，如果重命名成功返回 true，否则返回 false。
     */
    fun rename(path: String, oldName: String, newName: String): Result<Boolean>

    /**
     * 读取指定路径的文件内容。
     *
     * @param path 文件的路径。
     * @return 返回包含文件内容的结果，如果发生错误，则返回失败结果。
     */
    fun readFile(path: String): Result<ByteArray>

    /**
     * 从文件的指定范围读取数据。
     *
     * @param path 文件的路径。
     * @param start 要读取的起始位置（字节偏移量）。
     * @param end 要读取的结束位置（字节偏移量）。
     * @return 如果成功，返回包含所读取字节数据的结果；否则，返回错误信息的结果。
     */
    fun readFileRange(path: String, start: Long, end: Long): Result<ByteArray>


    /**
     * 按指定的块大小分块读取文件，并在每次成功读取块时调用回调函数。
     *
     * @param path 要读取的文件路径。
     * @param chunkSize 每个块的大小（以字节为单位）。
     * @param onChunkRead 每次读取一个块时触发的回调函数，接收字节数组作为参数。
     * @return 返回操作结果的封装，包含成功或失败的信息。
     */
    fun readFileChunks(path: String, chunkSize: Long, onChunkRead: (Result<Pair<Long, ByteArray>>) -> Unit)

    /**
     * 将字节数据写入到指定路径的文件中。
     *
     * @param path 文件存储路径。
     * @param fileSize 文件的大小，用于验证存储是否合法。
     * @param data 要写入的字节数组。
     * @param offset 数据写入的偏移量。
     * @return 如果写入成功，返回包含 true 的 Result；否则返回包含 false 或错误信息的 Result。
     */
    fun writeBytes(path: String, fileSize: Long, data: ByteArray, offset: Long): Result<Boolean>
}
