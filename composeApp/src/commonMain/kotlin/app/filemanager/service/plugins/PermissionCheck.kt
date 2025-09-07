package app.filemanager.service.plugins

import app.filemanager.ui.state.device.DeviceCertificateState
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.response.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class PermissionCheckConfiguration {
    var resource: String = ""
    var action: String = ""
}

val PermissionCheck = createRouteScopedPlugin(
    name = "PermissionCheck",
    createConfiguration = ::PermissionCheckConfiguration
) {
    // Resolve dependencies via Koin in a multiplatform-friendly way
    val koin = object : KoinComponent {}
    val deviceCertificateState: DeviceCertificateState = koin.get()
    val resource = pluginConfig.resource
    val action = pluginConfig.action

    on(CallSetup) { call ->
        val token = call.request.headers["Authorization"]?.removePrefix("Bearer ")
            ?: call.request.queryParameters["token"]
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, "缺少认证令牌")
                return@on
            }

        // checkPermission returns true when no permission
        if (deviceCertificateState.checkPermission(token, resource, action)) {
            call.respond(HttpStatusCode.Forbidden, "对方没有为你设置权限")
            return@on
        }
    }
}

