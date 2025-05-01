package app.filemanager.utils

import io.ktor.utils.io.charsets.*

/**
 * CryptoParameters 是一个定义用于加密操作的相关参数的对象。
 * 提供了一些常量和配置信息，用于加密算法的初始化和执行。
 */
object CryptoParameters {
    /**
     * 表示用于某些加密或身份验证操作的固定密钥常量，不建议直接暴露或在生产环境中直接使用。
     *
     * 该常量可能被用于文件管理工具的特定功能模块中，作为标识或认证的核心参数。
     * 注意：出于安全性考虑，应避免在代码中明文存储重要密钥，建议改为通过环境变量
     * 或安全存储机制访问密钥。
     */
    const val key: String = "ca1a19a3f5b9c9ba"

    /**
     * 表示加密算法的常量值。
     *
     * 此变量定义了使用的加密/解密算法以及填充方式，采用 AES 算法，
     * 工作模式为 CBC（Cipher Block Chaining），并使用 PKCS5 填充。
     *
     * 使用该算法时，请确保密钥和初始化向量（IV）的长度符合要求。
     */
    const val algorithm: String = "AES/CBC/PKCS5Padding"

    /**
     * keyAlgorithm 是一个常量，指定用于加密操作的密钥算法。
     *
     * 目前的值为 "AES"，表示该常量用于对称加密中的高级加密标准算法 (Advanced Encryption Standard)。
     */
    const val keyAlgorithm: String = "AES"

    /**
     * 表示用于计算密钥摘要的算法名称。
     *
     * 此常量定义了应用程序中默认使用的摘要算法，通常用于对数据进行哈希计算。
     * 在文件操作、数据校验或加密相关功能中，可以使用该算法生成校验值以验证数据完整性。
     *
     * 目前此常量设置为 "SHA-256"，该算法是一种安全哈希算法，广泛用于密码学计算，具有较高的安全性。
     */
    const val keyDigestAlgorithm: String = "SHA-256"

    /**
     * 表示初始化向量 (Initialization Vector, IV) 的大小，通常用于加密算法。
     * 该值用于指定加密过程中所需的 IV 字节长度。
     *
     * ivSize 是一个常量，其值定义为 16，表示使用 16 字节的初始化向量。
     * 这是许多对称加密算法（如 AES）的常见要求，可以保证加密安全性。
     */
    const val ivSize: Int = 16

    /**
     * 默认的字符编码集，使用 UTF-8 编码。
     *
     * 此变量指定了用于文件读取、写入或任何字符串编码处理的默认字符集。
     * UTF-8 是一种广泛使用的字符编码方式，支持多种语言字符，具有良好的兼容性和效率。
     *
     * 推荐在需要明确指定字符编码的场景中使用该变量以保持一致性。
     */
    val charset = Charsets.UTF_8
}

/**
 * 对称加密接口，定义加密与解密的基本操作。
 *
 * 此接口提供了基于字符串和字节数组的加密与解密功能，支持灵活的对称加密操作。
 * 具体实现可能依赖不同的加密算法，如AES、DES等。
 */
interface SymmetricCryptoInterface {
    /**
     * 使用对称加密算法对给定的文本进行加密。
     *
     * @param text 要加密的字符串文本。
     * @return 加密后的字符串结果。
     */
    fun encrypt(text: String): String
    /**
     * 使用对称加密算法对加密的文本进行解密。
     *
     * @param text 要解密的字符串文本。
     * @return 解密后的字符串结果。
     */
    fun decrypt(text: String): String

    /**
     * 使用对称加密算法对输入的字节数组进行加密操作。
     *
     * @param data 要加密的字节数组。
     * @return 加密后的字节数组。
     */
    fun encrypt(data: ByteArray): ByteArray
    /**
     * 使用对称加密算法对输入的字节数组进行解密操作。
     *
     * @param data 加密的字节数组，需要解密的数据。
     * @return 解密后的字节数组。
     */
    fun decrypt(data: ByteArray): ByteArray
}

/**
 * SymmetricCrypto 是一个多平台的对称加密工具类，提供加密和解密功能。
 * 它支持对字符串及字节数组数据的加密与解密，具体算法由不同平台进行实现。
 */
expect object SymmetricCrypto : SymmetricCryptoInterface {
    /**
     * 对输入的文本进行加密处理。
     *
     * @param text 要加密的字符串文本。
     * @return 返回加密后的字符串结果。
     */
    override fun encrypt(text: String): String
    /**
     * 解密指定的字符串内容。
     *
     * @param text 需要解密的字符串。
     * @return 解密后的字符串内容。
     */
    override fun decrypt(text: String): String
    /**
     * 对输入的字节数组数据进行加密操作。
     *
     * @param data 要加密的字节数组。
     * @return 返回加密后的字节数组。
     */
    override fun encrypt(data: ByteArray): ByteArray
    /**
     * 解密输入的字节数组数据。
     *
     * @param data 需要解密的字节数组。
     * @return 解密后的字节数组。
     */
    override fun decrypt(data: ByteArray): ByteArray
}