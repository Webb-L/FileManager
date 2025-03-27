package app.filemanager.utils

import java.security.MessageDigest
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

actual class SymmetricCryptoImpl : SymmetricCryptoInterface {

    private fun getKeySpec(key: String): SecretKeySpec {
        val sha = MessageDigest.getInstance(CryptoParameters.keyDigestAlgorithm)
        val keyBytes = sha.digest(key.toByteArray(CryptoParameters.charset)).copyOf(CryptoParameters.ivSize)
        return SecretKeySpec(keyBytes, CryptoParameters.keyAlgorithm)
    }

    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CryptoParameters.algorithm)
        val secretKey = getKeySpec(CryptoParameters.key)

        val ivBytes = ByteArray(CryptoParameters.ivSize).apply(Random::nextBytes)
        val ivSpec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        return ivBytes + cipher.doFinal(data)
    }

    private fun decryptData(data: ByteArray): ByteArray {
        val ivBytes = data.copyOfRange(0, CryptoParameters.ivSize)
        val cipherText = data.copyOfRange(CryptoParameters.ivSize, data.size)

        val cipher = Cipher.getInstance(CryptoParameters.algorithm)
        val secretKey = getKeySpec(CryptoParameters.key)
        val ivSpec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(cipherText)
    }

    actual override fun encrypt(text: String): String =
        Base64.getEncoder().encodeToString(encryptData(text.toByteArray(Charsets.UTF_8)))

    actual override fun decrypt(text: String): String =
        decryptData(Base64.getDecoder().decode(text)).toString(Charsets.UTF_8)

    actual override fun encrypt(data: ByteArray): ByteArray = encryptData(data)

    actual override fun decrypt(data: ByteArray): ByteArray = decryptData(data)
}