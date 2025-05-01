package app.filemanager.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * SymmetricCrypto 的 Android 平台实现，提供对称加密功能。
 * 基于 AES/CBC/PKCS5Padding 加密算法。
 */
actual object SymmetricCrypto : SymmetricCryptoInterface {

    /**
     * 根据输入的密钥生成 SecretKeySpec。
     *
     * @param key 密钥字符串
     * @return 生成的密钥规格对象
     */
    private fun getKeySpec(key: String): SecretKeySpec {
        val sha = MessageDigest.getInstance(CryptoParameters.keyDigestAlgorithm)
        val keyBytes = sha.digest(key.toByteArray(CryptoParameters.charset)).copyOf(CryptoParameters.ivSize)
        return SecretKeySpec(keyBytes, CryptoParameters.keyAlgorithm)
    }

    /**
     * 加密字节数组数据。
     * 生成随机IV并将其与加密后的数据拼接返回。
     *
     * @param data 要加密的字节数组
     * @return IV和加密数据的组合字节数组
     */
    private fun encryptData(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CryptoParameters.algorithm)
        val secretKey = getKeySpec(CryptoParameters.key)

        val ivBytes = ByteArray(CryptoParameters.ivSize).also {
            SecureRandom().nextBytes(it)
        }
        val ivSpec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        return ivBytes + cipher.doFinal(data)
    }

    /**
     * 解密字节数组数据。
     * 从数据中提取IV并用于解密剩余部分。
     *
     * @param data 包含IV和加密数据的字节数组
     * @return 解密后的原始数据
     */
    private fun decryptData(data: ByteArray): ByteArray {
        val ivBytes = data.copyOfRange(0, CryptoParameters.ivSize)
        val cipherText = data.copyOfRange(CryptoParameters.ivSize, data.size)

        val cipher = Cipher.getInstance(CryptoParameters.algorithm)
        val secretKey = getKeySpec(CryptoParameters.key)
        val ivSpec = IvParameterSpec(ivBytes)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
        return cipher.doFinal(cipherText)
    }

    /**
     * 加密字符串，返回Base64编码的结果
     */
    actual override fun encrypt(text: String): String =
        Base64.encodeToString(encryptData(text.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)

    /**
     * 解密Base64编码的加密字符串
     */
    actual override fun decrypt(text: String): String =
        decryptData(Base64.decode(text, Base64.DEFAULT)).toString(Charsets.UTF_8)

    /**
     * 加密字节数组数据
     */
    actual override fun encrypt(data: ByteArray): ByteArray = encryptData(data)

    /**
     * 解密字节数组数据
     */
    actual override fun decrypt(data: ByteArray): ByteArray = decryptData(data)
}