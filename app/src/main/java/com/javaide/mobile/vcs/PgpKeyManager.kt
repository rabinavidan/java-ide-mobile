package com.javaide.mobile.vcs

import android.content.Context
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.bcpg.PublicKeyPacket
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.bcpg.sig.KeyFlags
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openpgp.PGPKeyRingGenerator
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.Security
import java.util.Date

/**
 * Generates (once) and stores an OpenPGP keypair used to sign commits. Android has no
 * ~/.gnupg / gpg-agent for JGit's built-in signer to rely on, so this is a self-contained
 * replacement: a plain RSA OpenPGP keyring generated with BouncyCastle and kept in app-private
 * storage. Unlike the APK debug-signing key (DebugSigningKey.kt), this is *not* hardware-backed --
 * OpenPGP isn't a key format AndroidKeyStore can produce -- so its protection is exactly the
 * normal per-app storage sandbox, same trust level as the project source files themselves.
 */
object PgpKeyManager {

    private const val KEY_FILE = "vcs_signing_key.pgp"
    private const val PASSPHRASE = "javaide-vcs-key"
    private const val DEFAULT_USER_ID = "JavaIDE User <javaide@localhost>"

    init {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private fun keyFile(context: Context) = File(context.filesDir, KEY_FILE)

    fun hasKey(context: Context): Boolean = keyFile(context).isFile

    fun getOrCreateKeyRing(context: Context, userId: String = DEFAULT_USER_ID): PGPSecretKeyRing {
        val file = keyFile(context)
        if (file.isFile) {
            return PGPSecretKeyRing(file.readBytes(), JcaKeyFingerprintCalculator())
        }
        val keyRing = generateKeyRing(userId)
        file.writeBytes(keyRing.encoded)
        return keyRing
    }

    fun secretKey(context: Context): PGPSecretKey = getOrCreateKeyRing(context).secretKey

    fun passphrase(): CharArray = PASSPHRASE.toCharArray()

    /** Armored (ASCII) public key text, for the user to copy into their GitHub GPG keys settings. */
    fun armoredPublicKey(context: Context): String {
        val keyRing = getOrCreateKeyRing(context)
        val out = ByteArrayOutputStream()
        ArmoredOutputStream(out).use { armor -> keyRing.publicKey.encode(armor) }
        return out.toString(StandardCharsets.US_ASCII.name())
    }

    private fun generateKeyRing(userId: String): PGPSecretKeyRing {
        val kpg = KeyPairGenerator.getInstance("RSA", "BC")
        kpg.initialize(2048)
        val pgpKeyPair = JcaPGPKeyPair(
            PublicKeyPacket.VERSION_4,
            PublicKeyAlgorithmTags.RSA_GENERAL,
            kpg.generateKeyPair(),
            Date()
        )

        val subpacketGenerator = PGPSignatureSubpacketGenerator()
        subpacketGenerator.setKeyFlags(false, KeyFlags.SIGN_DATA or KeyFlags.CERTIFY_OTHER)

        val digestCalculator = JcaPGPDigestCalculatorProviderBuilder().setProvider("BC").build()
            .get(HashAlgorithmTags.SHA1)
        val contentSignerBuilder = JcaPGPContentSignerBuilder(pgpKeyPair.publicKey.algorithm, HashAlgorithmTags.SHA256)
            .setProvider("BC")
        val encryptor = JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256)
            .setProvider("BC")
            .build(PASSPHRASE.toCharArray())

        val keyRingGenerator = PGPKeyRingGenerator(
            PGPSignature.POSITIVE_CERTIFICATION,
            pgpKeyPair,
            userId,
            digestCalculator,
            subpacketGenerator.generate(),
            null,
            contentSignerBuilder,
            encryptor
        )
        return keyRingGenerator.generateSecretKeyRing()
    }
}
