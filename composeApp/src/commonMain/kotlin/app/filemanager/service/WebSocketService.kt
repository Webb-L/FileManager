package app.filemanager.service

import app.filemanager.data.file.FileProtocol
import app.filemanager.data.file.FileSimpleInfo
import app.filemanager.data.main.DrawerBookmark
import app.filemanager.extensions.getFileAndFolder
import app.filemanager.ui.state.main.DeviceState
import app.filemanager.utils.FileUtils
import app.filemanager.utils.PathUtils
import com.russhwolf.settings.Settings
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.random.Random

expect class WebSocketService() {
    fun getNetworkIp(): List<String>
    suspend fun scanService(): List<String>
    fun startService()
    fun stopService()
}

const val SEND_SPLIT = "\n\n"

class WebSocketConnectService() : KoinComponent {
    private val replyMessage = mutableMapOf<Long, Any>()

    private val deviceState by inject<DeviceState>()
    private var deviceId: String = ""
    private var deviceName: String = ""


    private val client = HttpClient {
        install(WebSockets)
    }
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(host: String) {
        val settings by inject<Settings>()
        deviceId = settings.getString("deviceId", "")
        deviceName = settings.getString("deviceName", "")

        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = 12040,
            path = "/?id=$deviceId&name=$deviceName",
        ) {
            session = this
            launch {
                delay(10)
                send("/devices", value = "")
            }
            launch {
                try {
                    for (message in incoming) {
                        message as? Frame.Text ?: continue
                        parseMessage(message.readText())
                    }
                } catch (e: Exception) {
                    println(e)
                    close()
                }
            }.join()
        }
    }

    suspend fun sendReplyList(id: String, replyKey: Long, directory: String) {
        val sendFileSimpleInfos = mutableMapOf<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>().apply {
            directory.getFileAndFolder().forEach { fileSimpleInfo ->
                val key = if (fileSimpleInfo.protocol == FileProtocol.Local)
                    Pair(FileProtocol.Device, fileSimpleInfo.protocolId)
                else
                    Pair(fileSimpleInfo.protocol, fileSimpleInfo.protocolId)

                if (!containsKey(key)) {
                    put(key, mutableListOf(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    }))
                } else {
                    get(key)?.add(fileSimpleInfo.apply {
                        this.path = this.path.replace(directory, "")
                        this.protocol = FileProtocol.Local
                        this.protocolId = ""
                    })
                }
            }
        }

        send(
            "/reply_list",
            "$id $replyKey",
            "false",
            sendFileSimpleInfos
        )
    }

    suspend fun sendReplyBookmark(id: String, replyKey: Long) {
        val bookmarks = PathUtils.getBookmarks()
        send(command = "/reply_bookmark", header = "$id $replyKey", value = bookmarks)
    }

    suspend fun sendReplyRootPaths(id: String, replyKey: Long) {
        val rootPaths = PathUtils.getRootPaths()
        send(command = "/reply_root_paths", header = "$id $replyKey", value = rootPaths)
    }


    suspend fun sendFile(id: String, fileName: String) {
//        send("/upload 12132 $id$SEND_SPLIT".toByteArray().plus(FileUtils.getData(fileName, 0, 300)))
    }

    /**
     * 从远程设备获取指定路径下的文件和文件夹列表。
     *
     * @param path 要获取列表的路径。
     * @param remoteId 远程设备的ID。
     */
    suspend fun getList(path: String, remoteId: String, replyCallback: (List<FileSimpleInfo>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        send(command = "/list", header = "$remoteId $replyKey", params = path, value = "")

        for (i in 0..100) {
            delay(100)
            if (replyMessage.contains(replyKey)) {
                break
            }
        }

        val fileSimpleInfos: MutableList<FileSimpleInfo> = mutableListOf()
        if (replyMessage[replyKey] != null) {
            val tempMap = replyMessage[replyKey] as Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>
            tempMap.forEach {
                it.value.forEach { fileSimpleInfo ->
                    fileSimpleInfos.add(fileSimpleInfo.apply {
                        protocol = it.key.first
                        protocolId = it.key.second
                        this.path = path + this.path
                    })
                }
            }
        }

        replyCallback(fileSimpleInfos)
        replyMessage.remove(replyKey)
    }

    suspend fun getBookmark(remoteId: String, replyCallback: (List<DrawerBookmark>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        send(command = "/bookmark", header = "$remoteId $replyKey", value = "")

        for (i in 0..100) {
            delay(100)
            if (replyMessage.contains(replyKey)) {
                break
            }
        }

        val bookmarks: MutableList<DrawerBookmark> = mutableListOf()
        if (replyMessage[replyKey] != null) {
            bookmarks.addAll(replyMessage[replyKey] as List<DrawerBookmark>)
        }

        replyCallback(bookmarks)
        replyMessage.remove(replyKey)
    }

    suspend fun getRootPaths(remoteId: String, replyCallback: (List<String>) -> Unit) {
        val replyKey = Clock.System.now().toEpochMilliseconds() + Random.nextInt()
        send(command = "/root_paths", header = "$remoteId $replyKey", value = "")

        for (i in 0..100) {
            delay(100)
            if (replyMessage.contains(replyKey)) {
                break
            }
        }

        val paths: MutableList<String> = mutableListOf()
        if (replyMessage[replyKey] != null) {
            paths.addAll(replyMessage[replyKey] as List<String>)
        }

        replyCallback(paths)
        replyMessage.remove(replyKey)
    }

    suspend fun rename(remoteId: String, path: String, oldName: String, newName: String) {
        send(command = "/rename", header = "$remoteId ", params = "$path $oldName $newName", value = "")
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun parseMessage(msg: String) {
        val messages = msg.split(SEND_SPLIT)
        val header = messages[0].split(" ")
        println("header = $header")
        val headerCommand = header[0]
        val headerDevices = mutableListOf<String>()
        if (header.size > 1) {
            headerDevices.addAll(header[1].split(","))
        }
        val headerKey = if (header.size > 2) header[2].toLong() else -1

        val params = messages[1].split(" ")
        println("params = $params")

        val content = messages[2]
        when (headerCommand) {
            "/reply_devices" -> {
                for (message in content.split("\n")) {
                    deviceState.addDevices(
                        message,
                        this,
                    )
                }
            }

            // 远程设备需要我本地文件
            // TODO 检查权限
            "/list" -> sendReplyList(
                "",
                headerKey,
                params[0]
            )

            // 收到对方返回的文件文件夹信息
            "/reply_list" -> {
                replyMessage[headerKey] =
                    ProtoBuf.decodeFromHexString<Map<Pair<FileProtocol, String>, MutableList<FileSimpleInfo>>>(content)
            }

            // 远程设备需要我本地的书签
            // TODO 检查权限
            "/bookmark" -> sendReplyBookmark(
                header[1],
                headerKey,
            )

            // 收到对方返回的文件文件夹信息
            "/reply_bookmark" -> replyMessage[headerKey] =
                ProtoBuf.decodeFromHexString<List<DrawerBookmark>>(content)

            // 远程设备需要我本地的书签
            // TODO 检查权限
            "/root_paths" -> sendReplyRootPaths(
                header[1],
                headerKey,
            )

            // 收到对方返回的文件文件夹信息
            "/reply_root_paths" -> replyMessage[headerKey] = ProtoBuf.decodeFromHexString<List<String>>(content)

            // 重命名文件和文件夹
            // TODO 检查权限
            "/rename" -> {
                val renameArgs = content.split(" ")
                if (renameArgs.size > 2)
                    FileUtils.rename(renameArgs[0], renameArgs[1], renameArgs[2])
            }

            else -> {
                println("匹配不到指令：\n${header[0]}")
            }
        }
    }


    /**
     * 发送消息
     * 使用：send(指令, 设备1,设备2,... 消息标识, 参数, 发送的消息)
     *
     * @param command 消息指令
     * @param header 消息头 设备, 消息标识
     * @param params 消息参数 /home/user1/1.txt /home/user2/2.txt
     * @param value 内容
     */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend inline fun <reified T> send(
        command: String,
        header: String = " ",
        params: String = "",
        value: T
    ) {
        val content = if (value == null) "" else ProtoBuf.encodeToHexString(value)
        session?.send("$command $header${SEND_SPLIT}$params${SEND_SPLIT}${content}".toByteArray())
    }

    fun close() = client.close()
}