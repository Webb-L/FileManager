package app.filemanager.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf

object CryptoProtoBuf {
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> encode(value: T): ByteArray {
        val bytes = ProtoBuf.encodeToByteArray(value)
        return SymmetricCrypto.encrypt(bytes)
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> decode(data: ByteArray): T {
        val decrypted = SymmetricCrypto.decrypt(data)
        return ProtoBuf.decodeFromByteArray(decrypted)
    }
}

