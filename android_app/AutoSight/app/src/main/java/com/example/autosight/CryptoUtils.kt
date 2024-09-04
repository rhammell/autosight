package com.example.autosight

import org.bouncycastle.asn1.ASN1Integer
import org.bouncycastle.asn1.DERSequence
import org.bouncycastle.asn1.x9.X9ECParameters
import org.bouncycastle.crypto.ec.CustomNamedCurves
import org.bouncycastle.crypto.params.ECDomainParameters
import org.bouncycastle.crypto.params.ECPrivateKeyParameters
import org.bouncycastle.crypto.params.ECPublicKeyParameters
import org.bouncycastle.crypto.signers.ECDSASigner
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Security
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom

object CryptoUtils {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    private const val KEY_ALIAS = "WalletPrivateKeyEncryptionKey"
    private const val PRIVATE_KEY_PREF = "WalletPrivateKey"
    private const val IV_PREF = "WalletPrivateKeyIV"

    fun generateOrRetrievePrivateKey(context: Context): String {
        val sharedPref = context.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
        val encryptedKey = sharedPref.getString(PRIVATE_KEY_PREF, null)
        val iv = sharedPref.getString(IV_PREF, null)

        return if (encryptedKey != null && iv != null) {
            // Key exists, decrypt and return
            decryptPrivateKey(encryptedKey, iv)
        } else {
            // Generate new key, encrypt and store
            val newKey = generatePrivateKey()
            val (encryptedNewKey, newIv) = encryptPrivateKey(newKey)
            with(sharedPref.edit()) {
                putString(PRIVATE_KEY_PREF, encryptedNewKey)
                putString(IV_PREF, newIv)
                apply()
            }
            newKey
        }
    }

    private fun generatePrivateKey(): String {
        val secureRandom = SecureRandom()
        val privateKeyBytes = ByteArray(32) // 32 bytes for 256-bit key
        secureRandom.nextBytes(privateKeyBytes)
        return privateKeyBytes.toHex()
    }

    private fun encryptPrivateKey(privateKey: String): Pair<String, String> {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val encryptedBytes = cipher.doFinal(privateKey.toByteArray(Charsets.UTF_8))
        val encodedEncryptedBytes = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        val encodedIv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)

        return Pair(encodedEncryptedBytes, encodedIv)
    }

    private fun decryptPrivateKey(encryptedKey: String, iv: String): String {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)

        val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val ivBytes = Base64.decode(iv, Base64.DEFAULT)
        val spec = GCMParameterSpec(128, ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val encryptedBytes = Base64.decode(encryptedKey, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun serializeMessage(encoded: String): String {
        return encoded.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
    }

    fun calculateSHA256(input: String): String {
        val serializedBytes = input.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return MessageDigest.getInstance("SHA-256").run {
            val digest = digest(serializedBytes)
            digest.joinToString("") { "%02x".format(it) }
        }
    }

    fun calculateSHA512(input: String): String {
        return MessageDigest.getInstance("SHA-512").run {
            val digest = digest(input.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        }
    }

    fun derivePublicKey(privateKeyHex: String): String {
        val ecParams: X9ECParameters = CustomNamedCurves.getByName("secp256k1")
        val ecDomainParams = ECDomainParameters(ecParams.curve, ecParams.g, ecParams.n, ecParams.h)
        val privateKeyBigInt = BigInteger(privateKeyHex, 16)
        val q = ecDomainParams.g.multiply(privateKeyBigInt)
        val publicKeyParams = ECPublicKeyParameters(q, ecDomainParams)
        val publicKeyX = publicKeyParams.q.affineXCoord.toBigInteger()
        val publicKeyY = publicKeyParams.q.affineYCoord.toBigInteger()
        return publicKeyX.toString(16).padStart(64, '0') + publicKeyY.toString(16).padStart(64, '0')
    }

    fun createSignature(sha512Hash: String, privateKey: String): String {
        val ecParams: X9ECParameters = CustomNamedCurves.getByName("secp256k1")
        val ecDomainParams = ECDomainParameters(ecParams.curve, ecParams.g, ecParams.n, ecParams.h)
        val privateKeyBigInt = BigInteger(privateKey, 16)
        val privateKeyParams = ECPrivateKeyParameters(privateKeyBigInt, ecDomainParams)
        val signer = ECDSASigner()
        signer.init(true, privateKeyParams)
        val hashBytes = sha512Hash.hexStringToByteArray()
        val signature = signer.generateSignature(hashBytes)
        val r = ASN1Integer(signature[0])
        val s = ASN1Integer(signature[1])
        val derSequence = DERSequence(arrayOf(r, s))
        val derSignature = derSequence.encoded
        return derSignature.toHex()
    }

    // Extension functions
    private fun String.hexStringToByteArray(): ByteArray {
        return ByteArray(this.length / 2) { this.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }
}