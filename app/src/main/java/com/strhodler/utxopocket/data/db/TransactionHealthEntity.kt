package com.strhodler.utxopocket.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.strhodler.utxopocket.domain.model.TransactionHealthBadge
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicator
import com.strhodler.utxopocket.domain.model.TransactionHealthIndicatorType
import com.strhodler.utxopocket.domain.model.TransactionHealthPillar
import com.strhodler.utxopocket.domain.model.TransactionHealthResult
import com.strhodler.utxopocket.domain.model.TransactionHealthSeverity
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "transaction_health",
    primaryKeys = ["wallet_id", "txid"],
    indices = [Index("wallet_id")]
)
data class TransactionHealthEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "txid") val txid: String,
    @ColumnInfo(name = "final_score") val finalScore: Int,
    @ColumnInfo(name = "indicators_json") val indicatorsJson: String,
    @ColumnInfo(name = "badges_json") val badgesJson: String,
    @ColumnInfo(name = "pillar_scores_json") val pillarScoresJson: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

fun TransactionHealthResult.toEntity(
    walletId: Long,
    updatedAt: Long = System.currentTimeMillis()
): TransactionHealthEntity =
    TransactionHealthEntity(
        walletId = walletId,
        txid = transactionId,
        finalScore = finalScore,
        indicatorsJson = indicators.toIndicatorJsonString(),
        badgesJson = badges.toBadgeJsonString(),
        pillarScoresJson = pillarScores.toPillarJsonString(),
        updatedAt = updatedAt
    )

fun TransactionHealthEntity.toDomain(): TransactionHealthResult =
    TransactionHealthResult(
        transactionId = txid,
        finalScore = finalScore,
        indicators = indicatorsJson.parseIndicators(),
        badges = badgesJson.parseBadges(),
        pillarScores = pillarScoresJson.parsePillarScores()
    )

private fun List<TransactionHealthIndicator>.toIndicatorJsonString(): String {
    val array = JSONArray()
    forEach { indicator ->
        array.put(
            JSONObject().apply {
                put("type", indicator.type.name)
                put("delta", indicator.delta)
                put("severity", indicator.severity.name)
                put("pillar", indicator.pillar.name)
                put("evidence", JSONObject().apply {
                    indicator.evidence.forEach { (key, value) ->
                        put(key, value)
                    }
                })
            }
        )
    }
    return array.toString()
}

private fun List<TransactionHealthBadge>.toBadgeJsonString(): String {
    val array = JSONArray()
    forEach { badge ->
        array.put(
            JSONObject().apply {
                put("id", badge.id)
                put("label", badge.label)
            }
        )
    }
    return array.toString()
}

private fun Map<TransactionHealthPillar, Int>.toPillarJsonString(): String {
    val obj = JSONObject()
    forEach { (pillar, score) ->
        obj.put(pillar.name, score)
    }
    return obj.toString()
}

private fun String.parseIndicators(): List<TransactionHealthIndicator> =
    runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val type = obj.optString("type").takeIf { it.isNotBlank() }
                    ?.let { name -> TransactionHealthIndicatorType.valueOf(name.uppercase(Locale.US)) }
                    ?: continue
                val severity = obj.optString("severity").takeIf { it.isNotBlank() }
                    ?.let { name -> TransactionHealthSeverity.valueOf(name.uppercase(Locale.US)) }
                    ?: TransactionHealthSeverity.LOW
                val delta = obj.optInt("delta")
                val pillar = obj.optString("pillar").takeIf { it.isNotBlank() }
                    ?.let { name -> TransactionHealthPillar.valueOf(name.uppercase(Locale.US)) }
                    ?: TransactionHealthPillar.PRIVACY
                val evidenceObj = obj.optJSONObject("evidence") ?: JSONObject()
                val evidence = mutableMapOf<String, String>()
                val keys = evidenceObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    evidence[key] = evidenceObj.optString(key)
                }
                add(
                    TransactionHealthIndicator(
                        type = type,
                        delta = delta,
                        severity = severity,
                        pillar = pillar,
                        evidence = evidence
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

private fun String.parseBadges(): List<TransactionHealthBadge> =
    runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                val label = obj.optString("label").takeIf { it.isNotBlank() } ?: id
                add(TransactionHealthBadge(id = id, label = label))
            }
        }
    }.getOrElse { emptyList() }

private fun String.parsePillarScores(): Map<TransactionHealthPillar, Int> =
    runCatching {
        val obj = JSONObject(this)
        val scores = mutableMapOf<TransactionHealthPillar, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            TransactionHealthPillar.values()
                .firstOrNull { it.name.equals(key, ignoreCase = true) }
                ?.let { pillar ->
                    scores[pillar] = obj.optInt(key, 0).coerceIn(0, BASE_SCORE)
                }
        }
        if (scores.isEmpty()) {
            TransactionHealthPillar.values().associateWithTo(scores) { BASE_SCORE }
        } else {
            TransactionHealthPillar.values()
                .filterNot(scores::containsKey)
                .forEach { pillar -> scores[pillar] = BASE_SCORE }
        }
        scores.toMap()
    }.getOrElse {
        TransactionHealthPillar.values().associateWith { BASE_SCORE }
    }

private const val BASE_SCORE = 100
