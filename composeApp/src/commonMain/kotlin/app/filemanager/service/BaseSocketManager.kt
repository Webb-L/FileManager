package app.filemanager.service

import app.filemanager.exception.TimeoutException
import app.filemanager.extensions.chunked
import app.filemanager.service.socket.Socket
import app.filemanager.service.socket.SocketHeader
import app.filemanager.service.socket.SocketMessage
import io.ktor.utils.io.core.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

/**
 * 抽象类 BaseSocketManager，提供基础的 Socket 通信管理功能。
 * 用于处理客户端与服务器之间的多分片消息发送与接收，实现 socket 的数据操作的一系列通用逻辑。
 *
 * @param tag 标识该 SocketManager 的标志字符串，用于调试或日志记录。
 */
abstract class BaseSocketManager(private val tag: String) {
    /**
     * 抽象属性，用于表示具体的套接字实现。
     * 子类需要对该属性进行初始化，以便实现具体的网络通信功能。
     */
    protected abstract val socket: Socket

    internal val cancelKeys = mutableSetOf<Long>()

    /**
     * 一个内部使用的可变映射，存储与消息回复相关的数据结构。
     *
     * - 键：`Long` 类型，表示消息的唯一标识符（一般为 `replyKey`）。
     * - 值：一个 `Pair` 类型，其中：
     *   - 第一个元素为 `Int`，表示附加信息，-2 = 完成，其他数字都是等待。
     *   - 第二个元素为 `Any` 类型，表示与消息相关的任意对象（如具体的回复内容或状态数据）。
     *
     * 该变量主要用于管理与 Socket 通信相关的回复消息，支持在并发环境中动态存储和获取对应的回复信息。
     */
    internal val replyMessage = mutableMapOf<Long, Pair<Int, Any>>()

    /**
     * 用于临时存储与网络通信相关的数据结构。
     *
     * 该变量是一个嵌套的可变映射，其结构如下：
     * - 外层的键为Long类型，通常表示某种唯一标识符，例如客户端或请求的ID。
     * - 内层的键为Long类型，可能表示子请求的ID或相关联的额外信息。
     * - 内层的值为ByteArray类型，用于存储与某些操作或通信过程相关的二进制数据，例如序列化后的对象、文件数据或通信消息。
     *
     * 这是一个私有的变量，主要用于封装与网络通信的具体细节，避免与外部类直接交互。通常适用于以下场景：
     * - 缓存未完成网络请求的临时数据。
     * - 储存需要后续处理的接收消息。
     * - 提高通信过程的效率和数据组织的灵活性。
     */
    private val tempDataMap = mutableMapOf<Long, MutableMap<Long, ByteArray>>()

    /**
     * mainScope 是一个以生命周期为范围限制的主协程作用域实例。
     * 提供在 BaseSocketManager 中进行协程管理的能力，确保在协程执行的生命周期内
     * 可以统一处理协程任务的启动和取消。
     */
    private val mainScope = MainScope()

    /**
     * 伴生对象，用于定义和管理与类相关的全局常量。
     */
    companion object {
        // 10 minutes in seconds
        const val CONNECT_TIMEOUT = 600

        const val PORT = 1204

        val SEND_IDENTIFIER = "--FileManager--bytearray".toByteArray()

        const val SEND_LENGTH = 1024 * 8

        /**
         * 表示最大分片长度的常量，用于定义在数据传输过程中每个分片的最大长度。
         * 此值用于确保分片在网络传输时的稳定性和效率，避免超出可接受的大小限制。
         * 常量默认为 1024 * 6。
         */
        const val MAX_LENGTH = 1024 * 6 // 最大分片长度

        /**
         * 定义用于管理请求超时的常量。
         *
         * 该常量表示超时时间，单位为毫秒，典型场景是用于方法中循环等待某些条件完成的时间限制。
         * 利用此超时时间，避免因条件未满足而导致程序无法继续执行的问题。
         *
         * 使用范围：
         * - 在等待异步操作结果时，用于计算当前时间与超时时间之间的差值。
         * - 配合检查机制，例如 `waitFinish` 方法，处理客户端或服务端的响应超时问题。
         *
         * 常量值定义为 10,000 毫秒（即 10 秒），具体数值可根据实际需求调整。
         */
        const val TIMEOUT = 10_000L // 超时时间
    }

    /**
     * 向指定客户端发送消息的方法，该方法支持序列化对象并分块发送大文件。
     *
     * @param clientId 客户端的标识符，默认为空字符串。
     * @param header 消息的头部信息，包括命令和设备列表。
     * @param params 消息的参数，默认是一个空的键值对映射。
     * @param value 要发送的消息内容，可以是任意可序列化类型的数据或字节数组。
     */
    @OptIn(ExperimentalSerializationApi::class)
    internal suspend inline fun <reified T> send(
        clientId: String = "",
        header: SocketHeader,
        params: Map<String, String> = mapOf(),
        value: T
    ) {
        if (cancelKeys.contains(params["replyKey"] ?: "0".toLong())) {
            return
        }

        val byteArray = if (value is ByteArray) value else ProtoBuf.encodeToByteArray(value)

        if (byteArray.size <= MAX_LENGTH) {
//            println("【${tag}】 header = $header params = $params it=${byteArray.size}")
            socket.send(
                clientId,
                ProtoBuf.encodeToByteArray(
                    SocketMessage(
                        header = header,
                        params = params,
                        body = byteArray
                    )
                )
            )
            return
        }

        var index = 1
        val chunked = byteArray.chunked(MAX_LENGTH)
        chunked.forEach {
            val socketMessage = SocketMessage(
                header = header,
                params = params + mapOf("index" to index.toString(), "count" to chunked.size.toString()),
                body = it
            )
//            println("【${tag}】 header = ${socketMessage.header} params = ${socketMessage.params} it=${it.size}")

            socket.send(
                clientId,
                ProtoBuf.encodeToByteArray(
                    socketMessage
                )
            )
            index++
        }
    }

    /**
     * 等待任务完成的辅助方法。通过检查指定的 `replyKey` 是否存在于 `replyMessage` 中并执行回调函数 `callback` 来决定是否结束等待。
     *
     * @param replyKey 用于标记消息的键值，用以判断任务的完成状态。
     * @param callback 回调函数，当满足条件时执行，返回布尔值以决定是否退出等待循环。
     *                 回调函数的参数为布尔值，表示是否结束超时等待。
     */
    internal suspend fun <T> waitFinish(replyKey: Long, callback: (Result<T>) -> Unit) {
        var startTime = Clock.System.now().toEpochMilliseconds()
        var endTime = startTime + TIMEOUT
        var lastStatus = -1

        while (true) {
            val isMessagePending = replyMessage.contains(replyKey)
            // 判断是有超时 有超时就结束
            if (Clock.System.now().toEpochMilliseconds() >= endTime) {
                cancelKeys.add(replyKey)
                callback(Result.failure(TimeoutException()))
                send(
                    header = SocketHeader(command = "cancelKey"),
                    params = mapOf("replyKey" to replyKey.toString()),
                    value = byteArrayOf()
                )
                break
            }

            // 所有数据存在但是没有完成
            if (isMessagePending) {
                // 完成
                if (replyMessage[replyKey]!!.first == -2) {
                    @Suppress("UNCHECKED_CAST")
                    callback(Result.success(replyMessage[replyKey]!!.second as T))
                    break
                }

                // 未完成还有数据
                if (lastStatus != replyMessage[replyKey]!!.first) {
                    startTime = Clock.System.now().toEpochMilliseconds()
                    endTime = startTime + TIMEOUT
                    lastStatus = replyMessage[replyKey]!!.first
                }
            }
            delay(100)
        }

        replyMessage.remove(replyKey)
    }

    /**
     * 处理接收到的 Socket 消息。
     *
     * @param message 接收到的 {@link SocketMessage} 对象，包含消息的头部、参数及其内容。
     * `message.params` 包含消息参数，如 `replyKey`、`index` 和 `count`：
     * - `replyKey`：表示这条消息的唯一标识，用于区分不同的消息。
     * - `index`：当前数据片的索引。
     * - `count`：总的数据片数。
     * `message.body` 为消息的主要内容，以字节数组形式表示。
     */
    internal fun receive(message: SocketMessage) {
        MainScope().launch {
            val replyKey = (message.params["replyKey"] ?: "0").toLong()
            val index = (message.params["index"] ?: "0").toLong()
            val count = (message.params["count"] ?: "0").toLong()

            // 数据没有分片
            if (index == 0L && count == 0L) {
                replyMessage[replyKey] = Pair(-2, message.body)
                return@launch
            }

            // 数据分片
            if (!tempDataMap.containsKey(replyKey)) {
                tempDataMap[replyKey] = mutableMapOf(index to message.body)
                return@launch
            }

            val replyMap: MutableMap<Long, ByteArray> = tempDataMap[replyKey]!!
            replyMap[index] = message.body
            if (index == count) {
                val byteArray = replyMap.keys.sorted().fold(ByteArray(0)) { acc, key ->
                    acc + (replyMap[key] ?: ByteArray(0))
                }

                replyMessage[replyKey] = Pair(-2, byteArray)
                tempDataMap.remove(replyKey)
            }
        }
    }
}