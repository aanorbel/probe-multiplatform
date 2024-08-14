package org.ooni.probe.data.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.ooni.engine.models.NetworkType
import org.ooni.probe.Database
import org.ooni.probe.data.Network
import org.ooni.probe.data.models.NetworkModel

class NetworkRepository(
    private val database: Database,
    private val backgroundDispatcher: CoroutineDispatcher,
) {
    /*
     If the model has an ID, only update.
     If the model does not have an ID, search if we already have an entry with the same values.
     If we do, return that ID, otherwise create a new entry.
     */
    suspend fun createIfNew(model: NetworkModel): NetworkModel.Id =
        withContext(backgroundDispatcher) {
            database.transactionWithResult {
                if (model.id == null) {
                    database.networkQueries.selectByValues(
                        network_name = model.networkName,
                        ip = model.ip,
                        asn = model.asn,
                        country_code = model.countryCode,
                        network_type = model.networkType?.value,
                    )
                        .executeAsOneOrNull()
                        ?.let { return@transactionWithResult NetworkModel.Id(it.id) }
                }

                database.networkQueries.insertOrReplace(
                    id = model.id?.value,
                    network_name = model.networkName,
                    ip = model.ip,
                    asn = model.asn,
                    country_code = model.countryCode,
                    network_type = model.networkType?.value,
                )

                model.id
                    ?: NetworkModel.Id(
                        database.networkQueries.selectLastInsertedRowId().executeAsOne(),
                    )
            }
        }

    fun list() =
        database.networkQueries
            .selectAll()
            .asFlow()
            .mapToList(backgroundDispatcher)
            .map { list -> list.map { it.toModel() } }
}

fun Network.toModel(): NetworkModel =
    NetworkModel(
        id = NetworkModel.Id(id),
        networkName = network_name,
        ip = ip,
        asn = asn,
        countryCode = country_code,
        networkType = network_type?.let(NetworkType::fromValue),
    )
