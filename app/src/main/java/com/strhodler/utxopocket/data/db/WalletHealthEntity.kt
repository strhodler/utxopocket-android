package com.strhodler.utxopocket.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.strhodler.utxopocket.domain.model.WalletHealthBadge
import com.strhodler.utxopocket.domain.model.WalletHealthIndicator
import com.strhodler.utxopocket.domain.model.WalletHealthPillar
import com.strhodler.utxopocket.domain.model.WalletHealthResult
import com.strhodler.utxopocket.domain.model.WalletHealthSeverity
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

@Entity(
    tableName = "wallet_health",
    primaryKeys = ["wallet_id"]
)
data class WalletHealthEntity(
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "final_score") val finalScore: Int,
    @ColumnInfo(name = "pillar_scores_json") val pillarScoresJson: String,
    @ColumnInfo(name = "badges_json") val badgesJson: String,
    @ColumnInfo(name = "indicators_json") val indicatorsJson: String,
    @ColumnInfo(name = "computed_at") val computedAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

fun WalletHealthResult.toEntity(
    updatedAt: Long = System.currentTimeMillis()
): WalletHealthEntity =
    WalletHealthEntity(
        walletId = walletId,
        finalScore = finalScore,
        pillarScoresJson = pillarScores.toPillarJsonString(),
        badgesJson = badges.toBadgeJsonString(),
        indicatorsJson = indicators.toIndicatorJsonString(),
        computedAt = computedAt,
        updatedAt = updatedAt
    )

fun WalletHealthEntity.toDomain(): WalletHealthResult =
    WalletHealthResult(
        walletId = walletId,
        finalScore = finalScore,
        pillarScores = pillarScoresJson.parsePillarScores(),
        badges = badgesJson.parseBadges(),
        indicators = indicatorsJson.parseIndicators(),
        computedAt = computedAt
    )

private fun Map<WalletHealthPillar, Int>.toPillarJsonString(): String {
    val obj = JSONObject()
    forEach { (pillar, score) ->
        obj.put(pillar.name, score)
    }
    return obj.toString()
}

private fun String.parsePillarScores(): Map<WalletHealthPillar, Int> =
    runCatching {
        val obj = JSONObject(this)
        val scores = mutableMapOf<WalletHealthPillar, Int>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            WalletHealthPillar.values()
                .firstOrNull { it.name.equals(key, ignoreCase = true) }
                ?.let { pillar ->
                    scores[pillar] = obj.optInt(key, BASE_SCORE).coerceIn(0, BASE_SCORE)
                }
        }
        if (scores.isEmpty()) {
            WalletHealthPillar.values().associateWithTo(scores) { BASE_SCORE }
        } else {
            WalletHealthPillar.values()
                .filterNot(scores::containsKey)
                .forEach { pillar -> scores[pillar] = BASE_SCORE }
        }
        scores.toMap()
    }.getOrElse {
        WalletHealthPillar.values().associateWith { BASE_SCORE }
    }

private fun List<WalletHealthBadge>.toBadgeJsonString(): String {
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

private fun String.parseBadges(): List<WalletHealthBadge> =
    runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                val label = obj.optString("label").takeIf { it.isNotBlank() } ?: id
                add(WalletHealthBadge(id = id, label = label))
            }
        }
    }.getOrElse { emptyList() }

private fun List<WalletHealthIndicator>.toIndicatorJsonString(): String {
    val array = JSONArray()
    forEach { indicator ->
        array.put(
            JSONObject().apply {
                put("id", indicator.id)
                put("label", indicator.label)
                put("pillar", indicator.pillar.name)
                put("severity", indicator.severity.name)
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

private fun String.parseIndicators(): List<WalletHealthIndicator> =
    runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
                val label = obj.optString("label").takeIf { it.isNotBlank() } ?: id
                val pillar = obj.optString("pillar").takeIf { it.isNotBlank() }
                    ?.let { name -> WalletHealthPillar.valueOf(name.uppercase(Locale.US)) }
                    ?: WalletHealthPillar.PRIVACY
                val severity = obj.optString("severity").takeIf { it.isNotBlank() }
                    ?.let { name -> WalletHealthSeverity.valueOf(name.uppercase(Locale.US)) }
                    ?: WalletHealthSeverity.LOW
                val evidenceObj = obj.optJSONObject("evidence") ?: JSONObject()
                val evidence = mutableMapOf<String, String>()
                val keys = evidenceObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    evidence[key] = evidenceObj.optString(key)
                }
                add(
                    WalletHealthIndicator(
                        id = id,
                        label = label,
                        pillar = pillar,
                        severity = severity,
                        evidence = evidence
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

private const val BASE_SCORE = 100
