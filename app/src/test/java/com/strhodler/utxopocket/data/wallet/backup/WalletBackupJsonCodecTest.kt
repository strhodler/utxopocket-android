package com.strhodler.utxopocket.data.wallet.backup

import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.json.JSONArray
import org.json.JSONObject
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WalletBackupJsonCodecTest {

    @Test
    fun encodeDecodeRoundtripPreservesPayload() {
        val payload = samplePayload()

        val encoded = WalletBackupJsonCodec.encode(
            payload = payload,
            passphrase = DEFAULT_PASSPHRASE.copyOf(),
            iterations = BACKUP_MIN_KDF_ITERATIONS,
            createdAtMillis = DEFAULT_CREATED_AT,
            secureRandom = seededRandom(42L)
        )

        val decoded = WalletBackupJsonCodec.decode(
            encoded = encoded,
            passphrase = DEFAULT_PASSPHRASE.copyOf()
        )

        val success = assertIs<WalletBackupDecodeResult.Success>(decoded)
        assertEquals(DEFAULT_CREATED_AT, success.createdAtMillis)
        assertEquals(payload, success.payload)
    }

    @Test
    fun decodeRejectsUnknownEnvelopeField() {
        val encoded = encodeSamplePayload(seed = 7L)
        val mutated = mutateEnvelope(encoded) { envelope ->
            envelope.put("unexpected", true)
        }

        val failure = decodeFailure(mutated, DEFAULT_PASSPHRASE.copyOf())
        assertIs<WalletBackupFailure.InvalidPayload>(failure)
    }

    @Test
    fun decodeRejectsUnsupportedSchema() {
        val encoded = encodeSamplePayload(seed = 11L)
        val mutated = mutateEnvelope(encoded) { envelope ->
            envelope.put("schema", "other.wallet.backup")
        }

        val failure = decodeFailure(mutated, DEFAULT_PASSPHRASE.copyOf())
        assertEquals(
            WalletBackupFailure.InvalidPayload("Unsupported backup schema"),
            failure
        )
    }

    @Test
    fun decodeRejectsUnsupportedVersion() {
        val encoded = encodeSamplePayload(seed = 21L)
        val mutated = mutateEnvelope(encoded) { envelope ->
            envelope.put("version", 999)
        }

        val failure = decodeFailure(mutated, DEFAULT_PASSPHRASE.copyOf())
        assertEquals(WalletBackupFailure.UnsupportedVersion(version = 999), failure)
    }

    @Test
    fun decodeRejectsUnsupportedKdfParameters() {
        val encoded = encodeSamplePayload(seed = 33L)
        val unsupportedName = mutateEnvelope(encoded) { envelope ->
            envelope.getJSONObject("kdf").put("name", "scrypt")
        }
        val unsupportedNameFailure = decodeFailure(unsupportedName, DEFAULT_PASSPHRASE.copyOf())
        assertEquals(
            WalletBackupFailure.UnsupportedKdf(name = "scrypt", iterations = BACKUP_MIN_KDF_ITERATIONS),
            unsupportedNameFailure
        )

        val unsupportedIterations = mutateEnvelope(encoded) { envelope ->
            envelope.getJSONObject("kdf").put("iterations", BACKUP_MIN_KDF_ITERATIONS - 1)
        }
        val unsupportedIterationsFailure = decodeFailure(unsupportedIterations, DEFAULT_PASSPHRASE.copyOf())
        assertEquals(
            WalletBackupFailure.UnsupportedKdf(
                name = BACKUP_KDF_NAME,
                iterations = BACKUP_MIN_KDF_ITERATIONS - 1
            ),
            unsupportedIterationsFailure
        )
    }

    @Test
    fun decodeWithWrongPassphraseFailsClosed() {
        val encoded = encodeSamplePayload(seed = 100L)

        val failure = decodeFailure(encoded, "wrong".toCharArray())

        assertEquals(WalletBackupFailure.WrongPassphraseOrCorrupt, failure)
    }

    @Test
    fun decodeRejectsTamperedCiphertextWithSafeFailure() {
        val encoded = encodeSamplePayload(seed = 202L)
        val tampered = mutateEnvelope(encoded) { envelope ->
            val ciphertext = Base64.getDecoder().decode(envelope.getString("ciphertext"))
            ciphertext[0] = (ciphertext[0].toInt() xor 0x01).toByte()
            envelope.put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
        }

        val failure = decodeFailure(tampered, DEFAULT_PASSPHRASE.copyOf())

        assertEquals(WalletBackupFailure.WrongPassphraseOrCorrupt, failure)
    }

    @Test
    fun decodeRejectsTruncatedEnvelope() {
        val encoded = encodeSamplePayload(seed = 303L)
        val truncated = encoded.copyOf(encoded.size - 8)

        val failure = decodeFailure(truncated, DEFAULT_PASSPHRASE.copyOf())

        assertIs<WalletBackupFailure.InvalidPayload>(failure)
    }

    @Test
    fun decodeRejectsOversizedFile() {
        val oversized = ByteArray(MAX_BACKUP_FILE_BYTES + 1) { 0x41 }

        val failure = decodeFailure(oversized, DEFAULT_PASSPHRASE.copyOf())

        assertEquals(WalletBackupFailure.OversizedFile(maxBytes = MAX_BACKUP_FILE_BYTES), failure)
    }

    @Test
    fun decodeRejectsForbiddenPinAndDuressFields() {
        val forbiddenKeys = listOf(
            "pin_hash",
            "pin_salt",
            "pin_kdf_iterations",
            "duress_pin_hash",
            "duress_pin_salt",
            "duress_pin_kdf_iterations"
        )

        forbiddenKeys.forEachIndexed { index, key ->
            val payload = validPayloadJson().apply {
                getJSONObject("appPreferences").put(key, "secret")
            }
            val encoded = encodeRawPayload(payload, seed = 404L + index)

            val failure = decodeFailure(encoded, DEFAULT_PASSPHRASE.copyOf())
            val forbidden = assertIs<WalletBackupFailure.ForbiddenField>(failure)
            assertEquals("payload.appPreferences.$key", forbidden.fieldPath)
        }
    }

    @Test
    fun decodeRejectsForbiddenNodeConfigurationFields() {
        val payload = validPayloadJson().apply {
            put("node_custom_list", JSONArray().put("tcp://clearnet.example.com:50001"))
        }
        val encoded = encodeRawPayload(payload, seed = 500L)

        val failure = decodeFailure(encoded, DEFAULT_PASSPHRASE.copyOf())

        val forbidden = assertIs<WalletBackupFailure.ForbiddenField>(failure)
        assertEquals("payload.node_custom_list", forbidden.fieldPath)
    }

    @Test
    fun decodeRejectsUnknownPayloadField() {
        val payload = validPayloadJson().apply {
            getJSONArray("wallets").getJSONObject(0).put("unexpected", true)
        }
        val encoded = encodeRawPayload(payload, seed = 600L)

        val failure = decodeFailure(encoded, DEFAULT_PASSPHRASE.copyOf())

        assertEquals(WalletBackupFailure.InvalidPayload("Payload validation failed"), failure)
    }

    @Test
    fun decodeRejectsIncorrectFieldTypes() {
        val payload = validPayloadJson().apply {
            getJSONObject("appPreferences").put("pinShuffleEnabled", "true")
        }
        val encoded = encodeRawPayload(payload, seed = 701L)
        val payloadFailure = decodeFailure(encoded, DEFAULT_PASSPHRASE.copyOf())
        assertEquals(WalletBackupFailure.InvalidPayload("Payload validation failed"), payloadFailure)

        val encodedEnvelope = encodeSamplePayload(seed = 702L)
        val invalidEnvelope = mutateEnvelope(encodedEnvelope) { envelope ->
            envelope.put("version", "1")
        }
        val envelopeFailure = decodeFailure(invalidEnvelope, DEFAULT_PASSPHRASE.copyOf())
        assertEquals(WalletBackupFailure.InvalidPayload("Missing envelope version"), envelopeFailure)
    }

    @Test
    fun wrongPassphraseFailureDoesNotLeakSensitiveDetails() {
        val encoded = encodeSamplePayload(seed = 808L)

        val failure = decodeFailure(encoded, "bad-passphrase".toCharArray())

        assertEquals(WalletBackupFailure.WrongPassphraseOrCorrupt, failure)
        assertTrue(failure.toString().contains("WrongPassphraseOrCorrupt"))
    }

    private fun decodeFailure(encoded: ByteArray, passphrase: CharArray): WalletBackupFailure {
        val decoded = WalletBackupJsonCodec.decode(encoded = encoded, passphrase = passphrase)
        return assertIs<WalletBackupDecodeResult.Failure>(decoded).failure
    }

    private fun encodeSamplePayload(seed: Long): ByteArray =
        WalletBackupJsonCodec.encode(
            payload = samplePayload(),
            passphrase = DEFAULT_PASSPHRASE.copyOf(),
            iterations = BACKUP_MIN_KDF_ITERATIONS,
            createdAtMillis = DEFAULT_CREATED_AT,
            secureRandom = seededRandom(seed)
        )

    private fun mutateEnvelope(
        encoded: ByteArray,
        mutate: (JSONObject) -> Unit
    ): ByteArray {
        val envelope = JSONObject(String(encoded, Charsets.UTF_8))
        mutate(envelope)
        return envelope.toString().toByteArray(Charsets.UTF_8)
    }

    private fun encodeRawPayload(payloadJson: JSONObject, seed: Long): ByteArray {
        val secureRandom = seededRandom(seed)
        val salt = ByteArray(BACKUP_SALT_BYTES).also(secureRandom::nextBytes)
        val nonce = ByteArray(BACKUP_NONCE_BYTES).also(secureRandom::nextBytes)
        val key = deriveKey(DEFAULT_PASSPHRASE.copyOf(), salt, BACKUP_MIN_KDF_ITERATIONS)
        val aad = buildAadForTest(
            iterations = BACKUP_MIN_KDF_ITERATIONS,
            salt = salt,
            nonce = nonce
        )
        val ciphertext = encryptForTest(
            plaintext = payloadJson.toString().toByteArray(Charsets.UTF_8),
            key = key,
            nonce = nonce,
            aad = aad
        )
        return JSONObject()
            .put("schema", BACKUP_SCHEMA_NAME)
            .put("version", BACKUP_SCHEMA_VERSION)
            .put("createdAt", DEFAULT_CREATED_AT)
            .put(
                "kdf",
                JSONObject()
                    .put("name", BACKUP_KDF_NAME)
                    .put("iterations", BACKUP_MIN_KDF_ITERATIONS)
                    .put("saltB64", Base64.getEncoder().encodeToString(salt))
            )
            .put(
                "encryption",
                JSONObject()
                    .put("cipher", BACKUP_CIPHER_NAME)
                    .put("nonceB64", Base64.getEncoder().encodeToString(nonce))
            )
            .put("ciphertext", Base64.getEncoder().encodeToString(ciphertext))
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, BACKUP_KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance(BACKUP_KDF_NAME).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun buildAadForTest(iterations: Int, salt: ByteArray, nonce: ByteArray): ByteArray {
        val value =
            "$BACKUP_SCHEMA_NAME|$BACKUP_SCHEMA_VERSION|$BACKUP_KDF_NAME|$iterations|${Base64.getEncoder().encodeToString(salt)}|$BACKUP_CIPHER_NAME|${Base64.getEncoder().encodeToString(nonce)}"
        return value.toByteArray(Charsets.UTF_8)
    }

    private fun encryptForTest(
        plaintext: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(BACKUP_GCM_TAG_BITS, nonce)
        )
        cipher.updateAAD(aad)
        return cipher.doFinal(plaintext)
    }

    private fun seededRandom(seed: Long): SecureRandom =
        SecureRandom.getInstance("SHA1PRNG").apply { setSeed(seed) }

    private fun samplePayload(): WalletBackupPayload =
        WalletBackupPayload(
            wallets =
                listOf(
                    WalletBackupWallet(
                        walletRef = "wallet-1",
                        meta = WalletBackupWalletMeta(
                            name = "Primary",
                            network = "TESTNET4",
                            descriptor = "wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/0/*)",
                            changeDescriptor = "wpkh([8e8074b3/84h/1h/0h]tpubDDXF6KFU6ZNATjg6RBsf3Kkex7HLKpnhuk1PodeQtFLfFFD2qLZZTTX7V7t9SBNhYEEhH2CjbcHZLSsfQfZRfid5YKuPd3kXQX84UoYQyac/1/*)",
                            sharedDescriptors = false,
                            viewOnly = true,
                            color = "orange",
                            sortOrder = 0
                        ),
                        labels = WalletBackupLabels(),
                        collections = emptyList(),
                        collectionMemberships = emptyList(),
                        canvasItems = emptyList()
                    )
                ),
            appPreferences =
                WalletBackupAppPreferences(
                    preferredNetwork = "TESTNET4",
                    themePreference = "SYSTEM",
                    themeProfile = "DEFAULT",
                    appLanguage = "en",
                    balanceUnit = "SATS",
                    balancesHidden = false,
                    hapticsEnabled = true,
                    walletBalanceRange = "All",
                    showBalanceChart = true,
                    pinShuffleEnabled = true,
                    advancedMode = false,
                    dustThresholdSats = 1_000L
                ),
            walletDetailPreferences =
                listOf(
                    WalletBackupWalletDetailPreferences(
                        walletRef = "wallet-1",
                        transactionSort = "NEWEST_FIRST",
                        showPending = true,
                        utxoSort = "LARGEST_AMOUNT",
                        transactionFilter = WalletBackupTransactionFilter(
                            showLabeled = true,
                            showUnlabeled = true,
                            showReceived = true,
                            showSent = true
                        ),
                        utxoFilter = WalletBackupUtxoFilter(
                            showLabeled = true,
                            showUnlabeled = true,
                            showSpendable = true,
                            showNotSpendable = true
                        ),
                        balanceRange = "All",
                        showBalanceChart = true
                    )
                )
        )

    private fun validPayloadJson(): JSONObject =
        JSONObject()
            .put(
                "wallets",
                JSONArray().put(
                    JSONObject()
                        .put("walletRef", "wallet-1")
                        .put(
                            "meta",
                            JSONObject()
                                .put("name", "Primary")
                                .put("network", "TESTNET4")
                                .put("descriptor", "wpkh(tpubexample/0/*)")
                                .put("changeDescriptor", "wpkh(tpubexample/1/*)")
                                .put("sharedDescriptors", false)
                                .put("viewOnly", true)
                                .put("color", "orange")
                                .put("sortOrder", 0)
                        )
                        .put(
                            "labels",
                            JSONObject()
                                .put("transactionLabels", JSONArray())
                                .put("utxoLabels", JSONArray())
                                .put("pendingBip329", JSONArray())
                        )
                        .put("collections", JSONArray())
                        .put("collectionMemberships", JSONArray())
                        .put("canvasItems", JSONArray())
                )
            )
            .put(
                "appPreferences",
                JSONObject()
                    .put("preferredNetwork", "TESTNET4")
                    .put("themePreference", "SYSTEM")
                    .put("themeProfile", "DEFAULT")
                    .put("appLanguage", "en")
                    .put("balanceUnit", "SATS")
                    .put("balancesHidden", false)
                    .put("hapticsEnabled", true)
                    .put("walletBalanceRange", "All")
                    .put("showBalanceChart", true)
                    .put("pinShuffleEnabled", true)
                    .put("advancedMode", false)
                    .put("dustThresholdSats", 1_000L)
            )
            .put(
                "walletDetailPreferences",
                JSONArray().put(
                    JSONObject()
                        .put("walletRef", "wallet-1")
                        .put("transactionSort", "NEWEST_FIRST")
                        .put("showPending", true)
                        .put("utxoSort", "LARGEST_AMOUNT")
                        .put(
                            "transactionFilter",
                            JSONObject()
                                .put("showLabeled", true)
                                .put("showUnlabeled", true)
                                .put("showReceived", true)
                                .put("showSent", true)
                        )
                        .put(
                            "utxoFilter",
                            JSONObject()
                                .put("showLabeled", true)
                                .put("showUnlabeled", true)
                                .put("showSpendable", true)
                                .put("showNotSpendable", true)
                        )
                        .put("balanceRange", "All")
                        .put("showBalanceChart", true)
                )
            )

    private companion object {
        private val DEFAULT_PASSPHRASE = "correct horse battery staple".toCharArray()
        private const val DEFAULT_CREATED_AT = 1_770_000_000_000L
    }
}
