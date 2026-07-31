package com.javaide.mobile.vcs

import android.content.Context
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.HashAlgorithmTags
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.PGPSignatureGenerator
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder
import org.eclipse.jgit.lib.GpgConfig
import org.eclipse.jgit.lib.GpgSignature
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.Signer
import org.eclipse.jgit.transport.CredentialsProvider
import java.io.ByteArrayOutputStream

/**
 * A JGit [Signer] backed by our own app-generated OpenPGP key (see [PgpKeyManager]), instead of
 * JGit's built-in BouncyCastleGpgSigner, which expects a real local GPG keybox file (~/.gnupg) --
 * something that doesn't exist on Android. Wiring this in via CommitCommand.setSigner(...)
 * bypasses that lookup entirely; JGit's own key-locator/keybox code is never invoked.
 */
class GitCommitSigner(private val context: Context) : Signer {

    override fun sign(
        repository: Repository,
        config: GpgConfig,
        data: ByteArray,
        committer: PersonIdent,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): GpgSignature {
        val secretKey = PgpKeyManager.secretKey(context)
        val privateKey = secretKey.extractPrivateKey(
            JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(PgpKeyManager.passphrase())
        )
        val signatureGenerator = PGPSignatureGenerator(
            JcaPGPContentSignerBuilder(secretKey.publicKey.algorithm, HashAlgorithmTags.SHA256).setProvider("BC")
        )
        signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey)
        signatureGenerator.update(data)
        val signature = signatureGenerator.generate()

        val armored = ByteArrayOutputStream().also { out ->
            ArmoredOutputStream(out).use { armor -> signature.encode(armor) }
        }.toByteArray()
        return GpgSignature(armored)
    }

    override fun canLocateSigningKey(
        repository: Repository,
        config: GpgConfig,
        committer: PersonIdent,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): Boolean = true // a key is always generated on first use if one doesn't exist yet
}
