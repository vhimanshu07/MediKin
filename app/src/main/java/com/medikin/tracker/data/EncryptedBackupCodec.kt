package com.medikin.tracker.data

import com.medikin.tracker.domain.TrackerSnapshot
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupEnvelope(
    val version: Int,
    val exportedAt: String,
    val snapshot: TrackerSnapshot,
)

class EncryptedBackupCodec(
    private val random: SecureRandom = SecureRandom(),
) {
    fun encode(snapshot: TrackerSnapshot, password: CharArray): ByteArray {
        require(password.size >= MIN_PASSWORD_LENGTH) {
            "Use at least $MIN_PASSWORD_LENGTH characters for the backup password"
        }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val plaintext = trackerGson().toJson(
            BackupEnvelope(
                version = FORMAT_VERSION,
                exportedAt = Instant.now().toString(),
                snapshot = snapshot,
            ),
        ).toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(MAGIC)
        }
        val ciphertext = cipher.doFinal(plaintext)
        plaintext.fill(0)
        return ByteBuffer.allocate(MAGIC.size + salt.size + iv.size + ciphertext.size)
            .put(MAGIC)
            .put(salt)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun decode(bytes: ByteArray, password: CharArray): TrackerSnapshot {
        require(password.isNotEmpty()) { "Backup password is required" }
        require(bytes.size > MAGIC.size + SALT_BYTES + IV_BYTES + TAG_BITS / 8) {
            "This is not a valid MediKin backup"
        }
        val buffer = ByteBuffer.wrap(bytes)
        val magic = ByteArray(MAGIC.size).also(buffer::get)
        require(magic.contentEquals(MAGIC)) { "This is not a valid MediKin backup" }
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val iv = ByteArray(IV_BYTES).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(MAGIC)
        }
        val plaintext = cipher.doFinal(ciphertext)
        return try {
            val envelope = trackerGson().fromJson(
                plaintext.toString(StandardCharsets.UTF_8),
                BackupEnvelope::class.java,
            )
            require(envelope.version == FORMAT_VERSION) { "Unsupported MediKin backup version" }
            envelope.snapshot
        } finally {
            plaintext.fill(0)
        }
    }

    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try {
                SecretKeySpec(bytes, "AES")
            } finally {
                bytes.fill(0)
            }
        } finally {
            spec.clearPassword()
        }
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private const val FORMAT_VERSION = 1
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
        private const val KEY_BITS = 256
        private const val PBKDF2_ITERATIONS = 210_000
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private val MAGIC = "MEDIKIN1".toByteArray(StandardCharsets.US_ASCII)
    }
}
