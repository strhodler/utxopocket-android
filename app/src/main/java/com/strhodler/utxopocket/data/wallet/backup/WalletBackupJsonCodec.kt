package com.strhodler.utxopocket.data.wallet.backup

import com.strhodler.utxopocket.domain.model.WalletBackupFailure
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

internal const val BACKUP_SCHEMA_NAME: String = "utxopocket.watch-only-backup"
internal const val BACKUP_SCHEMA_VERSION: Int = 1
internal const val BACKUP_KDF_NAME: String = "PBKDF2WithHmacSHA256"
internal const val BACKUP_CIPHER_NAME: String = "AES-256-GCM"
internal const val BACKUP_KEY_BYTES: Int = 32
internal const val BACKUP_SALT_BYTES: Int = 16
internal const val BACKUP_NONCE_BYTES: Int = 12
internal const val BACKUP_GCM_TAG_BITS: Int = 128
internal const val BACKUP_MIN_KDF_ITERATIONS: Int = 150_000
internal const val BACKUP_MAX_KDF_ITERATIONS: Int = 1_200_000
internal const val MAX_BACKUP_FILE_BYTES: Int = 8 * 1024 * 1024
internal const val MAX_DECRYPTED_PAYLOAD_BYTES: Int = 16 * 1024 * 1024

internal data class WalletBackupPayload(
    val wallets: List<WalletBackupWallet>,
    val appPreferences: WalletBackupAppPreferences?,
    val walletDetailPreferences: List<WalletBackupWalletDetailPreferences>
)

internal data class WalletBackupWallet(
    val walletRef: String,
    val meta: WalletBackupWalletMeta,
    val labels: WalletBackupLabels = WalletBackupLabels(),
    val collections: List<WalletBackupCollection> = emptyList(),
    val collectionMemberships: List<WalletBackupCollectionMembership> = emptyList(),
    val canvasItems: List<WalletBackupCanvasItem> = emptyList()
)

internal data class WalletBackupWalletMeta(
    val name: String,
    val network: String,
    val descriptor: String,
    val changeDescriptor: String?,
    val sharedDescriptors: Boolean,
    val viewOnly: Boolean,
    val color: String,
    val sortOrder: Int
)

internal data class WalletBackupLabels(
    val transactionLabels: List<WalletBackupTransactionLabel> = emptyList(),
    val utxoLabels: List<WalletBackupUtxoLabel> = emptyList(),
    val pendingBip329: List<WalletBackupPendingLabel> = emptyList()
)

internal data class WalletBackupTransactionLabel(
    val txid: String,
    val label: String
)

internal data class WalletBackupUtxoLabel(
    val txid: String,
    val vout: Int,
    val label: String?,
    val spendable: Boolean?
)

internal data class WalletBackupPendingLabel(
    val type: String,
    val ref: String,
    val keyPath: String?,
    val label: String?,
    val spendable: Boolean?,
    val hasSpendable: Boolean,
    val overwriteExisting: Boolean
)

internal data class WalletBackupCollection(
    val name: String,
    val colorKey: String
)

internal data class WalletBackupCollectionMembership(
    val txid: String,
    val vout: Int,
    val collectionName: String
)

internal data class WalletBackupCanvasItem(
    val itemType: String,
    val refId: String,
    val positionIndex: Int
)

internal data class WalletBackupAppPreferences(
    val preferredNetwork: String,
    val themePreference: String,
    val themeProfile: String,
    val appLanguage: String,
    val balanceUnit: String,
    val balancesHidden: Boolean,
    val hapticsEnabled: Boolean,
    val walletBalanceRange: String,
    val showBalanceChart: Boolean,
    val pinShuffleEnabled: Boolean,
    val advancedMode: Boolean,
    val dustThresholdSats: Long
)

internal data class WalletBackupWalletDetailPreferences(
    val walletRef: String,
    val transactionSort: String,
    val showPending: Boolean,
    val utxoSort: String,
    val transactionFilter: WalletBackupTransactionFilter,
    val utxoFilter: WalletBackupUtxoFilter,
    val balanceRange: String,
    val showBalanceChart: Boolean
)

internal data class WalletBackupTransactionFilter(
    val showLabeled: Boolean,
    val showUnlabeled: Boolean,
    val showReceived: Boolean,
    val showSent: Boolean
)

internal data class WalletBackupUtxoFilter(
    val showLabeled: Boolean,
    val showUnlabeled: Boolean,
    val showSpendable: Boolean,
    val showNotSpendable: Boolean
)

internal sealed class WalletBackupDecodeResult {
    data class Success(
        val createdAtMillis: Long,
        val payload: WalletBackupPayload
    ) : WalletBackupDecodeResult()

    data class Failure(val failure: WalletBackupFailure) : WalletBackupDecodeResult()
}

internal object WalletBackupJsonCodec {

    private val envelopeKeys = setOf("schema", "version", "createdAt", "kdf", "encryption", "ciphertext")
    private val kdfKeys = setOf("name", "iterations", "saltB64")
    private val encryptionKeys = setOf("cipher", "nonceB64")
    private val payloadKeys = setOf("wallets", "appPreferences", "walletDetailPreferences")
    private val walletKeys =
        setOf("walletRef", "meta", "labels", "collections", "collectionMemberships", "canvasItems")
    private val walletMetaKeys =
        setOf("name", "network", "descriptor", "changeDescriptor", "sharedDescriptors", "viewOnly", "color", "sortOrder")
    private val labelsKeys = setOf("transactionLabels", "utxoLabels", "pendingBip329")
    private val txLabelKeys = setOf("txid", "label")
    private val utxoLabelKeys = setOf("txid", "vout", "label", "spendable")
    private val pendingLabelKeys =
        setOf("type", "ref", "keyPath", "label", "spendable", "hasSpendable", "overwriteExisting")
    private val collectionKeys = setOf("name", "colorKey")
    private val membershipKeys = setOf("txid", "vout", "collectionName")
    private val canvasItemKeys = setOf("itemType", "refId", "positionIndex")
    private val appPreferenceKeys =
        setOf(
            "preferredNetwork",
            "themePreference",
            "themeProfile",
            "appLanguage",
            "balanceUnit",
            "balancesHidden",
            "hapticsEnabled",
            "walletBalanceRange",
            "showBalanceChart",
            "pinShuffleEnabled",
            "advancedMode",
            "dustThresholdSats"
        )
    private val walletDetailPreferenceKeys =
        setOf(
            "walletRef",
            "transactionSort",
            "showPending",
            "utxoSort",
            "transactionFilter",
            "utxoFilter",
            "balanceRange",
            "showBalanceChart"
        )
    private val txFilterKeys = setOf("showLabeled", "showUnlabeled", "showReceived", "showSent")
    private val utxoFilterKeys = setOf("showLabeled", "showUnlabeled", "showSpendable", "showNotSpendable")

    private val forbiddenKeys =
        setOf(
            "pin_hash",
            "pin_salt",
            "pin_kdf_iterations",
            "duress_pin_hash",
            "duress_pin_salt",
            "duress_pin_kdf_iterations",
            "node_connection_option",
            "node_selected_public_id",
            "node_custom_list",
            "network_logs_enabled",
            "network_logs_info_seen",
            "xprv",
            "tprv",
            "seed",
            "mnemonic",
            "privateKey",
            "wif"
        )

    fun encode(
        payload: WalletBackupPayload,
        passphrase: CharArray,
        iterations: Int,
        createdAtMillis: Long,
        secureRandom: SecureRandom = SecureRandom()
    ): ByteArray {
        require(iterations in BACKUP_MIN_KDF_ITERATIONS..BACKUP_MAX_KDF_ITERATIONS) {
            "KDF iterations are out of allowed bounds"
        }
        val payloadBytes = payloadToJson(payload).toString().toByteArray(Charsets.UTF_8)
        require(payloadBytes.size <= MAX_DECRYPTED_PAYLOAD_BYTES) {
            "Backup payload exceeds maximum decrypted payload size"
        }

        val salt = ByteArray(BACKUP_SALT_BYTES).also(secureRandom::nextBytes)
        val nonce = ByteArray(BACKUP_NONCE_BYTES).also(secureRandom::nextBytes)
        val key = deriveKey(passphrase, salt, iterations)
        val aad = buildAad(
            schema = BACKUP_SCHEMA_NAME,
            version = BACKUP_SCHEMA_VERSION,
            kdfName = BACKUP_KDF_NAME,
            iterations = iterations,
            salt = salt,
            cipher = BACKUP_CIPHER_NAME,
            nonce = nonce
        )
        val ciphertext = encrypt(payloadBytes, key, nonce, aad)

        val envelope = JSONObject().apply {
            put("schema", BACKUP_SCHEMA_NAME)
            put("version", BACKUP_SCHEMA_VERSION)
            put("createdAt", createdAtMillis)
            put(
                "kdf",
                JSONObject()
                    .put("name", BACKUP_KDF_NAME)
                    .put("iterations", iterations)
                    .put("saltB64", base64Encode(salt))
            )
            put(
                "encryption",
                JSONObject()
                    .put("cipher", BACKUP_CIPHER_NAME)
                    .put("nonceB64", base64Encode(nonce))
            )
            put("ciphertext", base64Encode(ciphertext))
        }
        val encoded = envelope.toString().toByteArray(Charsets.UTF_8)
        require(encoded.size <= MAX_BACKUP_FILE_BYTES) {
            "Backup file exceeds maximum file size"
        }
        return encoded
    }

    fun decode(encoded: ByteArray, passphrase: CharArray): WalletBackupDecodeResult {
        if (encoded.size > MAX_BACKUP_FILE_BYTES) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.OversizedFile(maxBytes = MAX_BACKUP_FILE_BYTES)
            )
        }

        val envelopeJson = runCatching { decodeUtf8Strict(encoded) }
            .getOrElse {
                return WalletBackupDecodeResult.Failure(
                    WalletBackupFailure.InvalidPayload("Backup file is not valid UTF-8")
                )
            }

        val envelope = runCatching { JSONObject(envelopeJson) }
            .getOrElse {
                return WalletBackupDecodeResult.Failure(
                    WalletBackupFailure.InvalidPayload("Backup envelope is not valid JSON")
                )
            }

        validateKeys(envelope, envelopeKeys, "envelope")?.let {
            return WalletBackupDecodeResult.Failure(it)
        }

        val schema = envelope.requiredString("schema")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Missing envelope schema")
            )
        if (schema != BACKUP_SCHEMA_NAME) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Unsupported backup schema")
            )
        }
        val version = envelope.requiredInt("version")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Missing envelope version")
            )
        if (version != BACKUP_SCHEMA_VERSION) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.UnsupportedVersion(version = version)
            )
        }

        val createdAtMillis = envelope.requiredLong("createdAt")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Missing envelope timestamp")
            )
        if (createdAtMillis < 0L) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid envelope timestamp")
            )
        }

        val kdf = envelope.optJSONObject("kdf")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Missing KDF block")
            )
        validateKeys(kdf, kdfKeys, "envelope.kdf")?.let {
            return WalletBackupDecodeResult.Failure(it)
        }
        val kdfName = kdf.requiredString("name")
        val iterations = kdf.requiredInt("iterations")
        if (kdfName != BACKUP_KDF_NAME ||
            iterations == null ||
            iterations !in BACKUP_MIN_KDF_ITERATIONS..BACKUP_MAX_KDF_ITERATIONS
        ) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.UnsupportedKdf(
                    name = kdfName?.takeIf { it.isNotBlank() },
                    iterations = iterations
                )
            )
        }
        val saltB64 = kdf.requiredString("saltB64")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid KDF salt encoding")
            )
        val salt = decodeBase64(saltB64)
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid KDF salt encoding")
            )
        if (salt.size != BACKUP_SALT_BYTES) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid KDF salt length")
            )
        }

        val encryption = envelope.optJSONObject("encryption")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Missing encryption block")
            )
        validateKeys(encryption, encryptionKeys, "envelope.encryption")?.let {
            return WalletBackupDecodeResult.Failure(it)
        }
        val cipherName = encryption.requiredString("cipher")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Unsupported encryption cipher")
            )
        if (cipherName != BACKUP_CIPHER_NAME) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Unsupported encryption cipher")
            )
        }
        val nonceB64 = encryption.requiredString("nonceB64")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid nonce encoding")
            )
        val nonce = decodeBase64(nonceB64)
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid nonce encoding")
            )
        if (nonce.size != BACKUP_NONCE_BYTES) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid nonce length")
            )
        }

        val ciphertextB64 = envelope.requiredString("ciphertext")
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid ciphertext encoding")
            )
        val ciphertext = decodeBase64(ciphertextB64)
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Invalid ciphertext encoding")
            )

        val key = deriveKey(passphrase, salt, iterations)
        val aad = buildAad(
            schema = BACKUP_SCHEMA_NAME,
            version = BACKUP_SCHEMA_VERSION,
            kdfName = BACKUP_KDF_NAME,
            iterations = iterations,
            salt = salt,
            cipher = BACKUP_CIPHER_NAME,
            nonce = nonce
        )
        val plaintext = try {
            decrypt(ciphertext, key, nonce, aad)
        } catch (_: AEADBadTagException) {
            return WalletBackupDecodeResult.Failure(WalletBackupFailure.WrongPassphraseOrCorrupt)
        } catch (_: GeneralSecurityException) {
            return WalletBackupDecodeResult.Failure(WalletBackupFailure.WrongPassphraseOrCorrupt)
        }

        if (plaintext.size > MAX_DECRYPTED_PAYLOAD_BYTES) {
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.OversizedFile(maxBytes = MAX_DECRYPTED_PAYLOAD_BYTES)
            )
        }

        val payloadJson = runCatching { decodeUtf8Strict(plaintext) }
            .getOrElse {
                return WalletBackupDecodeResult.Failure(
                    WalletBackupFailure.InvalidPayload("Decrypted payload is not valid UTF-8")
                )
            }

        val payloadRoot = runCatching { JSONObject(payloadJson) }
            .getOrElse {
                return WalletBackupDecodeResult.Failure(
                    WalletBackupFailure.InvalidPayload("Decrypted payload is not valid JSON")
                )
            }

        findForbiddenField(payloadRoot, "payload")?.let { fieldPath ->
            return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.ForbiddenField(fieldPath)
            )
        }

        val payload = parsePayload(payloadRoot)
            ?: return WalletBackupDecodeResult.Failure(
                WalletBackupFailure.InvalidPayload("Payload validation failed")
            )

        return WalletBackupDecodeResult.Success(
            createdAtMillis = createdAtMillis,
            payload = payload
        )
    }

    private fun payloadToJson(payload: WalletBackupPayload): JSONObject {
        val wallets = JSONArray().apply {
            payload.wallets.forEach { wallet ->
                put(
                    JSONObject()
                        .put("walletRef", wallet.walletRef)
                        .put(
                            "meta",
                            JSONObject()
                                .put("name", wallet.meta.name)
                                .put("network", wallet.meta.network)
                                .put("descriptor", wallet.meta.descriptor)
                                .put("changeDescriptor", wallet.meta.changeDescriptor)
                                .put("sharedDescriptors", wallet.meta.sharedDescriptors)
                                .put("viewOnly", wallet.meta.viewOnly)
                                .put("color", wallet.meta.color)
                                .put("sortOrder", wallet.meta.sortOrder)
                        )
                        .put(
                            "labels",
                            JSONObject()
                                .put(
                                    "transactionLabels",
                                    JSONArray().apply {
                                        wallet.labels.transactionLabels.forEach { entry ->
                                            put(
                                                JSONObject()
                                                    .put("txid", entry.txid)
                                                    .put("label", entry.label)
                                            )
                                        }
                                    }
                                )
                                .put(
                                    "utxoLabels",
                                    JSONArray().apply {
                                        wallet.labels.utxoLabels.forEach { entry ->
                                            put(
                                                JSONObject()
                                                    .put("txid", entry.txid)
                                                    .put("vout", entry.vout)
                                                    .put("label", entry.label)
                                                    .put("spendable", entry.spendable)
                                            )
                                        }
                                    }
                                )
                                .put(
                                    "pendingBip329",
                                    JSONArray().apply {
                                        wallet.labels.pendingBip329.forEach { entry ->
                                            put(
                                                JSONObject()
                                                    .put("type", entry.type)
                                                    .put("ref", entry.ref)
                                                    .put("keyPath", entry.keyPath)
                                                    .put("label", entry.label)
                                                    .put("spendable", entry.spendable)
                                                    .put("hasSpendable", entry.hasSpendable)
                                                    .put("overwriteExisting", entry.overwriteExisting)
                                            )
                                        }
                                    }
                                )
                        )
                        .put(
                            "collections",
                            JSONArray().apply {
                                wallet.collections.forEach { collection ->
                                    put(
                                        JSONObject()
                                            .put("name", collection.name)
                                            .put("colorKey", collection.colorKey)
                                    )
                                }
                            }
                        )
                        .put(
                            "collectionMemberships",
                            JSONArray().apply {
                                wallet.collectionMemberships.forEach { membership ->
                                    put(
                                        JSONObject()
                                            .put("txid", membership.txid)
                                            .put("vout", membership.vout)
                                            .put("collectionName", membership.collectionName)
                                    )
                                }
                            }
                        )
                        .put(
                            "canvasItems",
                            JSONArray().apply {
                                wallet.canvasItems.forEach { item ->
                                    put(
                                        JSONObject()
                                            .put("itemType", item.itemType)
                                            .put("refId", item.refId)
                                            .put("positionIndex", item.positionIndex)
                                    )
                                }
                            }
                        )
                )
            }
        }

        val root = JSONObject().put("wallets", wallets)
        payload.appPreferences?.let { app ->
            root.put(
                "appPreferences",
                JSONObject()
                    .put("preferredNetwork", app.preferredNetwork)
                    .put("themePreference", app.themePreference)
                    .put("themeProfile", app.themeProfile)
                    .put("appLanguage", app.appLanguage)
                    .put("balanceUnit", app.balanceUnit)
                    .put("balancesHidden", app.balancesHidden)
                    .put("hapticsEnabled", app.hapticsEnabled)
                    .put("walletBalanceRange", app.walletBalanceRange)
                    .put("showBalanceChart", app.showBalanceChart)
                    .put("pinShuffleEnabled", app.pinShuffleEnabled)
                    .put("advancedMode", app.advancedMode)
                    .put("dustThresholdSats", app.dustThresholdSats)
            )
        }
        root.put(
            "walletDetailPreferences",
            JSONArray().apply {
                payload.walletDetailPreferences.forEach { pref ->
                    put(
                        JSONObject()
                            .put("walletRef", pref.walletRef)
                            .put("transactionSort", pref.transactionSort)
                            .put("showPending", pref.showPending)
                            .put("utxoSort", pref.utxoSort)
                            .put(
                                "transactionFilter",
                                JSONObject()
                                    .put("showLabeled", pref.transactionFilter.showLabeled)
                                    .put("showUnlabeled", pref.transactionFilter.showUnlabeled)
                                    .put("showReceived", pref.transactionFilter.showReceived)
                                    .put("showSent", pref.transactionFilter.showSent)
                            )
                            .put(
                                "utxoFilter",
                                JSONObject()
                                    .put("showLabeled", pref.utxoFilter.showLabeled)
                                    .put("showUnlabeled", pref.utxoFilter.showUnlabeled)
                                    .put("showSpendable", pref.utxoFilter.showSpendable)
                                    .put("showNotSpendable", pref.utxoFilter.showNotSpendable)
                            )
                            .put("balanceRange", pref.balanceRange)
                            .put("showBalanceChart", pref.showBalanceChart)
                    )
                }
            }
        )
        return root
    }

    private fun parsePayload(payloadRoot: JSONObject): WalletBackupPayload? {
        validateKeys(payloadRoot, payloadKeys, "payload")?.let { return null }
        val walletsArray = payloadRoot.optJSONArray("wallets") ?: return null
        val wallets = mutableListOf<WalletBackupWallet>()
        for (index in 0 until walletsArray.length()) {
            val walletJson = walletsArray.optJSONObject(index) ?: return null
            validateKeys(walletJson, walletKeys, "payload.wallets[$index]")?.let { return null }
            val walletRef = walletJson.requiredString("walletRef")?.trim().orEmpty()
            if (walletRef.isBlank()) return null

            val metaJson = walletJson.optJSONObject("meta") ?: return null
            validateKeys(metaJson, walletMetaKeys, "payload.wallets[$index].meta")?.let { return null }
            val name = metaJson.requiredString("name")?.trim().orEmpty()
            val network = metaJson.requiredString("network")?.trim().orEmpty()
            val descriptor = metaJson.requiredString("descriptor")?.trim().orEmpty()
            if (name.isBlank() || network.isBlank() || descriptor.isBlank()) return null
            val sortOrder = metaJson.requiredInt("sortOrder") ?: return null

            val labelsJson = walletJson.optJSONObject("labels") ?: return null
            validateKeys(labelsJson, labelsKeys, "payload.wallets[$index].labels")?.let { return null }

            val txLabels = labelsJson.optJSONArray("transactionLabels") ?: return null
            val parsedTxLabels = mutableListOf<WalletBackupTransactionLabel>()
            for (txIndex in 0 until txLabels.length()) {
                val txJson = txLabels.optJSONObject(txIndex) ?: return null
                validateKeys(
                    txJson,
                    txLabelKeys,
                    "payload.wallets[$index].labels.transactionLabels[$txIndex]"
                )?.let { return null }
                val txid = txJson.requiredString("txid")?.trim().orEmpty()
                val label = txJson.requiredString("label").orEmpty()
                if (txid.isBlank() || label.isBlank()) return null
                parsedTxLabels += WalletBackupTransactionLabel(txid = txid, label = label)
            }

            val utxoLabels = labelsJson.optJSONArray("utxoLabels") ?: return null
            val parsedUtxoLabels = mutableListOf<WalletBackupUtxoLabel>()
            for (utxoIndex in 0 until utxoLabels.length()) {
                val utxoJson = utxoLabels.optJSONObject(utxoIndex) ?: return null
                validateKeys(
                    utxoJson,
                    utxoLabelKeys,
                    "payload.wallets[$index].labels.utxoLabels[$utxoIndex]"
                )?.let { return null }
                val txid = utxoJson.requiredString("txid")?.trim().orEmpty()
                if (txid.isBlank()) return null
                val vout = utxoJson.requiredInt("vout") ?: return null
                if (vout < 0) return null
                val label = if (utxoJson.has("label") && !utxoJson.isNull("label")) {
                    utxoJson.requiredString("label") ?: return null
                } else {
                    null
                }
                val spendable = if (utxoJson.has("spendable") && !utxoJson.isNull("spendable")) {
                    utxoJson.requiredBoolean("spendable") ?: return null
                } else {
                    null
                }
                parsedUtxoLabels += WalletBackupUtxoLabel(
                    txid = txid,
                    vout = vout,
                    label = label,
                    spendable = spendable
                )
            }

            val pendingLabels = labelsJson.optJSONArray("pendingBip329") ?: return null
            val parsedPending = mutableListOf<WalletBackupPendingLabel>()
            for (pendingIndex in 0 until pendingLabels.length()) {
                val pendingJson = pendingLabels.optJSONObject(pendingIndex) ?: return null
                validateKeys(
                    pendingJson,
                    pendingLabelKeys,
                    "payload.wallets[$index].labels.pendingBip329[$pendingIndex]"
                )?.let { return null }
                val type = pendingJson.requiredString("type")?.trim().orEmpty()
                val ref = pendingJson.requiredString("ref")?.trim().orEmpty()
                if (type.isBlank() || ref.isBlank()) return null
                val keyPath = if (pendingJson.has("keyPath") && !pendingJson.isNull("keyPath")) {
                    pendingJson.requiredString("keyPath") ?: return null
                } else {
                    null
                }
                val label = if (pendingJson.has("label") && !pendingJson.isNull("label")) {
                    pendingJson.requiredString("label") ?: return null
                } else {
                    null
                }
                val spendable = if (pendingJson.has("spendable") && !pendingJson.isNull("spendable")) {
                    pendingJson.requiredBoolean("spendable") ?: return null
                } else {
                    null
                }
                val hasSpendable = pendingJson.requiredBoolean("hasSpendable") ?: return null
                val overwriteExisting = pendingJson.requiredBoolean("overwriteExisting") ?: return null
                parsedPending += WalletBackupPendingLabel(
                    type = type,
                    ref = ref,
                    keyPath = keyPath,
                    label = label,
                    spendable = spendable,
                    hasSpendable = hasSpendable,
                    overwriteExisting = overwriteExisting
                )
            }

            val collectionsArray = walletJson.optJSONArray("collections") ?: return null
            val collections = mutableListOf<WalletBackupCollection>()
            for (collectionIndex in 0 until collectionsArray.length()) {
                val collectionJson = collectionsArray.optJSONObject(collectionIndex) ?: return null
                validateKeys(
                    collectionJson,
                    collectionKeys,
                    "payload.wallets[$index].collections[$collectionIndex]"
                )?.let { return null }
                val collectionName = collectionJson.requiredString("name")?.trim().orEmpty()
                val colorKey = collectionJson.requiredString("colorKey")?.trim().orEmpty()
                if (collectionName.isBlank() || colorKey.isBlank()) return null
                collections += WalletBackupCollection(name = collectionName, colorKey = colorKey)
            }

            val membershipsArray = walletJson.optJSONArray("collectionMemberships") ?: return null
            val memberships = mutableListOf<WalletBackupCollectionMembership>()
            for (membershipIndex in 0 until membershipsArray.length()) {
                val membershipJson = membershipsArray.optJSONObject(membershipIndex) ?: return null
                validateKeys(
                    membershipJson,
                    membershipKeys,
                    "payload.wallets[$index].collectionMemberships[$membershipIndex]"
                )?.let { return null }
                val txid = membershipJson.requiredString("txid")?.trim().orEmpty()
                val vout = membershipJson.requiredInt("vout") ?: return null
                val collectionName = membershipJson.requiredString("collectionName")?.trim().orEmpty()
                if (txid.isBlank() || vout < 0 || collectionName.isBlank()) return null
                memberships += WalletBackupCollectionMembership(
                    txid = txid,
                    vout = vout,
                    collectionName = collectionName
                )
            }

            val canvasArray = walletJson.optJSONArray("canvasItems") ?: return null
            val canvasItems = mutableListOf<WalletBackupCanvasItem>()
            for (canvasIndex in 0 until canvasArray.length()) {
                val canvasJson = canvasArray.optJSONObject(canvasIndex) ?: return null
                validateKeys(
                    canvasJson,
                    canvasItemKeys,
                    "payload.wallets[$index].canvasItems[$canvasIndex]"
                )?.let { return null }
                val itemType = canvasJson.requiredString("itemType")?.trim().orEmpty()
                val refId = canvasJson.requiredString("refId")?.trim().orEmpty()
                val positionIndex = canvasJson.requiredInt("positionIndex") ?: return null
                if (itemType.isBlank() || refId.isBlank() || positionIndex < 0) return null
                canvasItems += WalletBackupCanvasItem(
                    itemType = itemType,
                    refId = refId,
                    positionIndex = positionIndex
                )
            }

            wallets += WalletBackupWallet(
                walletRef = walletRef,
                meta = WalletBackupWalletMeta(
                    name = name,
                    network = network,
                    descriptor = descriptor,
                    changeDescriptor = if (metaJson.has("changeDescriptor") && !metaJson.isNull("changeDescriptor")) {
                        metaJson.requiredString("changeDescriptor") ?: return null
                    } else {
                        null
                    },
                    sharedDescriptors = metaJson.requiredBoolean("sharedDescriptors") ?: return null,
                    viewOnly = metaJson.requiredBoolean("viewOnly") ?: return null,
                    color = metaJson.requiredString("color") ?: return null,
                    sortOrder = sortOrder
                ),
                labels = WalletBackupLabels(
                    transactionLabels = parsedTxLabels,
                    utxoLabels = parsedUtxoLabels,
                    pendingBip329 = parsedPending
                ),
                collections = collections,
                collectionMemberships = memberships,
                canvasItems = canvasItems
            )
        }

        val appPreferences = if (payloadRoot.has("appPreferences")) {
            val appJson = payloadRoot.optJSONObject("appPreferences") ?: return null
            validateKeys(appJson, appPreferenceKeys, "payload.appPreferences")?.let { return null }
            val preferredNetwork = appJson.requiredString("preferredNetwork")?.trim() ?: return null
            val themePreference = appJson.requiredString("themePreference")?.trim() ?: return null
            val themeProfile = appJson.requiredString("themeProfile")?.trim() ?: return null
            val appLanguage = appJson.requiredString("appLanguage")?.trim() ?: return null
            val balanceUnit = appJson.requiredString("balanceUnit")?.trim() ?: return null
            val balancesHidden = appJson.requiredBoolean("balancesHidden") ?: return null
            val hapticsEnabled = appJson.requiredBoolean("hapticsEnabled") ?: return null
            val walletBalanceRange = appJson.requiredString("walletBalanceRange")?.trim() ?: return null
            val showBalanceChart = appJson.requiredBoolean("showBalanceChart") ?: return null
            val pinShuffleEnabled = appJson.requiredBoolean("pinShuffleEnabled") ?: return null
            val advancedMode = appJson.requiredBoolean("advancedMode") ?: return null
            val dustThresholdSats = appJson.requiredLong("dustThresholdSats") ?: return null
            WalletBackupAppPreferences(
                preferredNetwork = preferredNetwork,
                themePreference = themePreference,
                themeProfile = themeProfile,
                appLanguage = appLanguage,
                balanceUnit = balanceUnit,
                balancesHidden = balancesHidden,
                hapticsEnabled = hapticsEnabled,
                walletBalanceRange = walletBalanceRange,
                showBalanceChart = showBalanceChart,
                pinShuffleEnabled = pinShuffleEnabled,
                advancedMode = advancedMode,
                dustThresholdSats = dustThresholdSats
            ).takeIf {
                it.preferredNetwork.isNotBlank() &&
                    it.themePreference.isNotBlank() &&
                    it.themeProfile.isNotBlank() &&
                    it.appLanguage.isNotBlank() &&
                    it.balanceUnit.isNotBlank() &&
                    it.walletBalanceRange.isNotBlank() &&
                    it.dustThresholdSats != Long.MIN_VALUE
            } ?: return null
        } else {
            null
        }

        val walletDetailsJson = payloadRoot.optJSONArray("walletDetailPreferences") ?: JSONArray()
        val walletDetails = mutableListOf<WalletBackupWalletDetailPreferences>()
        for (index in 0 until walletDetailsJson.length()) {
            val detailJson = walletDetailsJson.optJSONObject(index) ?: return null
            validateKeys(detailJson, walletDetailPreferenceKeys, "payload.walletDetailPreferences[$index]")
                ?.let { return null }
            val txFilterJson = detailJson.optJSONObject("transactionFilter") ?: return null
            validateKeys(txFilterJson, txFilterKeys, "payload.walletDetailPreferences[$index].transactionFilter")
                ?.let { return null }
            val utxoFilterJson = detailJson.optJSONObject("utxoFilter") ?: return null
            validateKeys(utxoFilterJson, utxoFilterKeys, "payload.walletDetailPreferences[$index].utxoFilter")
                ?.let { return null }
            val walletRef = detailJson.requiredString("walletRef")?.trim().orEmpty()
            val transactionSort = detailJson.requiredString("transactionSort")?.trim().orEmpty()
            val showPending = detailJson.requiredBoolean("showPending") ?: return null
            val utxoSort = detailJson.requiredString("utxoSort")?.trim().orEmpty()
            val showBalanceChart = detailJson.requiredBoolean("showBalanceChart") ?: return null
            val balanceRange = detailJson.requiredString("balanceRange")?.trim().orEmpty()
            val txShowLabeled = txFilterJson.requiredBoolean("showLabeled") ?: return null
            val txShowUnlabeled = txFilterJson.requiredBoolean("showUnlabeled") ?: return null
            val txShowReceived = txFilterJson.requiredBoolean("showReceived") ?: return null
            val txShowSent = txFilterJson.requiredBoolean("showSent") ?: return null
            val utxoShowLabeled = utxoFilterJson.requiredBoolean("showLabeled") ?: return null
            val utxoShowUnlabeled = utxoFilterJson.requiredBoolean("showUnlabeled") ?: return null
            val utxoShowSpendable = utxoFilterJson.requiredBoolean("showSpendable") ?: return null
            val utxoShowNotSpendable = utxoFilterJson.requiredBoolean("showNotSpendable") ?: return null
            walletDetails += WalletBackupWalletDetailPreferences(
                walletRef = walletRef,
                transactionSort = transactionSort,
                showPending = showPending,
                utxoSort = utxoSort,
                transactionFilter = WalletBackupTransactionFilter(
                    showLabeled = txShowLabeled,
                    showUnlabeled = txShowUnlabeled,
                    showReceived = txShowReceived,
                    showSent = txShowSent
                ),
                utxoFilter = WalletBackupUtxoFilter(
                    showLabeled = utxoShowLabeled,
                    showUnlabeled = utxoShowUnlabeled,
                    showSpendable = utxoShowSpendable,
                    showNotSpendable = utxoShowNotSpendable
                ),
                balanceRange = balanceRange,
                showBalanceChart = showBalanceChart
            )
            if (walletDetails.last().walletRef.isBlank() ||
                walletDetails.last().transactionSort.isBlank() ||
                walletDetails.last().utxoSort.isBlank() ||
                walletDetails.last().balanceRange.isBlank()
            ) {
                return null
            }
        }

        return WalletBackupPayload(
            wallets = wallets,
            appPreferences = appPreferences,
            walletDetailPreferences = walletDetails
        )
    }

    private fun validateKeys(
        objectJson: JSONObject,
        allowedKeys: Set<String>,
        path: String
    ): WalletBackupFailure? {
        val iterator = objectJson.keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            val fieldPath = "$path.$key"
            if (forbiddenKeys.contains(key)) {
                return WalletBackupFailure.ForbiddenField(fieldPath)
            }
            if (!allowedKeys.contains(key)) {
                return WalletBackupFailure.InvalidPayload("Unknown field: $fieldPath")
            }
        }
        return null
    }

    private fun findForbiddenField(value: Any?, path: String): String? {
        return when (value) {
            is JSONObject -> {
                val iterator = value.keys()
                while (iterator.hasNext()) {
                    val key = iterator.next()
                    val childPath = "$path.$key"
                    if (forbiddenKeys.contains(key)) {
                        return childPath
                    }
                    val child = value.opt(key)
                    val nested = findForbiddenField(child, childPath)
                    if (nested != null) {
                        return nested
                    }
                }
                null
            }

            is JSONArray -> {
                for (index in 0 until value.length()) {
                    val child = value.opt(index)
                    val nested = findForbiddenField(child, "$path[$index]")
                    if (nested != null) {
                        return nested
                    }
                }
                null
            }

            else -> null
        }
    }

    private fun JSONObject.requiredString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return opt(key) as? String
    }

    private fun JSONObject.requiredBoolean(key: String): Boolean? {
        if (!has(key) || isNull(key)) return null
        return opt(key) as? Boolean
    }

    private fun JSONObject.requiredInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key)
        return when (value) {
            is Int -> value
            is Long -> value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
            else -> null
        }
    }

    private fun JSONObject.requiredLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val value = opt(key)
        return when (value) {
            is Int -> value.toLong()
            is Long -> value
            else -> null
        }
    }

    private fun decodeUtf8Strict(bytes: ByteArray): String {
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (error: CharacterCodingException) {
            throw IllegalArgumentException("Invalid UTF-8 payload", error)
        }
    }

    private fun buildAad(
        schema: String,
        version: Int,
        kdfName: String,
        iterations: Int,
        salt: ByteArray,
        cipher: String,
        nonce: ByteArray
    ): ByteArray {
        val aad =
            "$schema|$version|$kdfName|$iterations|${base64Encode(salt)}|$cipher|${base64Encode(nonce)}"
        return aad.toByteArray(Charsets.UTF_8)
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, BACKUP_KEY_BYTES * 8)
        return try {
            SecretKeyFactory.getInstance(BACKUP_KDF_NAME).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encrypt(
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

    private fun decrypt(
        ciphertext: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(BACKUP_GCM_TAG_BITS, nonce)
        )
        cipher.updateAAD(aad)
        return cipher.doFinal(ciphertext)
    }

    private fun base64Encode(value: ByteArray): String =
        Base64.getEncoder().encodeToString(value)

    private fun decodeBase64(value: String): ByteArray? =
        runCatching { Base64.getDecoder().decode(value) }.getOrNull()
}
