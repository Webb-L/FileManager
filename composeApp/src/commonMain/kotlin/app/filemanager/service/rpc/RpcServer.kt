package app.filemanager.service.rpc


expect suspend fun startRpcServer()

expect fun getAllIPAddresses(type: SocketClientIPEnum): List<String>

expect suspend fun startHttpShareFileServer()
