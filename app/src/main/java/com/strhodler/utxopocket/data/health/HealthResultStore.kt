package com.strhodler.utxopocket.data.health

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Reusable store that wires DAO streams, entity mappers, and filter logic for health data.
 */
class HealthResultStore<Entity, Domain, Filter>(
    private val observeQuery: (Long) -> Flow<List<Entity>>,
    private val entityToDomain: (Entity) -> Domain,
    private val domainToEntity: (Domain, Long) -> Entity,
    private val replaceAction: suspend (Long, List<Entity>) -> Unit,
    private val clearAction: suspend (Long) -> Unit,
    private val filterResults: (List<Domain>, Filter) -> List<Domain>,
    private val dispatcher: CoroutineDispatcher
) {

    fun stream(walletId: Long, filter: Filter): Flow<List<Domain>> =
        observeQuery(walletId)
            .map { entities -> entities.map(entityToDomain) }
            .map { results -> filterResults(results, filter) }

    suspend fun replace(walletId: Long, results: Collection<Domain>) {
        withContext(dispatcher) {
            val entities = results.map { domain -> domainToEntity(domain, walletId) }
            replaceAction(walletId, entities)
        }
    }

    suspend fun clear(walletId: Long) {
        withContext(dispatcher) {
            clearAction(walletId)
        }
    }
}
