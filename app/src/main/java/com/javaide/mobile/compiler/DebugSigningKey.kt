package com.javaide.mobile.compiler

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Calendar
import javax.security.auth.x500.X500Principal

/**
 * A persistent, self-signed debug signing key/certificate stored in AndroidKeyStore, used
 * to sign the APKs this app builds. Not for distribution — same role as a local debug.keystore.
 */
object DebugSigningKey {

    private const val ALIAS = "javaide-debug-signing-key"
    private const val KEYSTORE_TYPE = "AndroidKeyStore"

    fun getOrCreate(): Pair<PrivateKey, X509Certificate> {
        val keyStore = KeyStore.getInstance(KEYSTORE_TYPE).apply { load(null) }
        if (!keyStore.containsAlias(ALIAS)) {
            generate()
        }
        val privateKey = keyStore.getKey(ALIAS, null) as PrivateKey
        val certificate = keyStore.getCertificate(ALIAS) as X509Certificate
        return privateKey to certificate
    }

    private fun generate() {
        val notBefore = Calendar.getInstance()
        val notAfter = Calendar.getInstance().apply { add(Calendar.YEAR, 30) }

        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setKeySize(2048)
            .setCertificateSubject(X500Principal("CN=Java IDE Debug"))
            .setCertificateSerialNumber(BigInteger.ONE)
            .setCertificateNotBefore(notBefore.time)
            .setCertificateNotAfter(notAfter.time)
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_TYPE)
            .apply { initialize(spec) }
            .generateKeyPair()
    }
}
