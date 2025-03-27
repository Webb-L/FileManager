package app.filemanager.utils.serializer

import app.filemanager.data.main.DeviceType
import app.filemanager.service.data.ConnectType
import app.filemanager.service.data.SocketDevice
import app.filemanager.utils.SymmetricCrypto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class SocketDeviceSerializer : KSerializer<SocketDevice> {
    private val mapSerializer = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SocketDeviceCrypto") {
        element<String>("encryptedData")
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override fun serialize(encoder: Encoder, value: SocketDevice) {
        val map = mapOf(
            "id" to value.id,
            "name" to value.name,
            "host" to value.host,
            "port" to value.port.toString(),
            "type" to value.type.toString(),
            "connectType" to value.connectType.toString()
        )

        // 使用 Protobuf 序列化为字节数组
        val protoBytes = ProtoBuf.encodeToByteArray(mapSerializer, map)

        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, Base64.encode(SymmetricCrypto.encrypt(protoBytes)))
        }
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): SocketDevice {
        var encryptedString = ""

        decoder.decodeStructure(descriptor) {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> encryptedString = decodeStringElement(descriptor, 0)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
        }

        val map = ProtoBuf.decodeFromByteArray(mapSerializer, SymmetricCrypto.decrypt(Base64.decode(encryptedString)))
        return SocketDevice(
            id = map["id"].orEmpty(),
            name = map["name"].orEmpty(),
            host = map["host"].orEmpty(),
            port = map["port"]?.toIntOrNull() ?: 0,
            type = map["type"]?.let { enumValueOf<DeviceType>(it) } ?: DeviceType.JVM,
            connectType = map["connectType"]?.let { enumValueOf<ConnectType>(it) } ?: ConnectType.New
        )
    }
}