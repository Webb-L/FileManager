package app.filemanager.service.rpc

import app.filemanager.service.socket.SocketClientIPEnum

expect suspend fun startRpcServer()

expect fun getAllIPAddresses(type: SocketClientIPEnum): List<String>
