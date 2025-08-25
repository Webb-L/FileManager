package app.filemanager.service.plugins

import app.filemanager.ui.state.device.DeviceCertificateState
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import org.koin.java.KoinJavaComponent.inject

class PermissionCheckConfiguration {
    var resource: String = ""
    var action: String = ""
}

val PermissionCheck = createRouteScopedPlugin("PermissionCheck", createConfiguration = ::PermissionCheckConfiguration) {
    val deviceCertificateState: DeviceCertificateState by inject(DeviceCertificateState::class.java)
    val resource = pluginConfig.resource
    val action = pluginConfig.action
    
    on(CallSetup) { call ->
        // 从请求头或查询参数中获取 token
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ") 
            ?: call.request.queryParameters["token"]
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, "缺少认证令牌")
                return@on
            }
        
        // 检查权限 - checkPermission 返回 true 表示没有权限
        if (deviceCertificateState.checkPermission(token, resource, action)) {
            call.respond(HttpStatusCode.Forbidden, "对方没有为你设置权限")
            return@on
        }
    }
}