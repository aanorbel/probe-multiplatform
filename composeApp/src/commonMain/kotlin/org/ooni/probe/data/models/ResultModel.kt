package org.ooni.probe.data.models

import kotlinx.datetime.LocalDateTime
import org.ooni.probe.shared.now

data class ResultModel(
    val id: Id? = null,
    val testGroupName: String?,
    val startTime: LocalDateTime = LocalDateTime.now(),
    val isViewed: Boolean = false,
    val isDone: Boolean = false,
    val dataUsageUp: Long = 0,
    val dataUsageDown: Long = 0,
    val failureMessage: String? = null,
    val networkId: NetworkModel.Id? = null,
    val testDescriptorId: InstalledTestDescriptorModel.Id?,
) {
    data class Id(
        val value: Long,
    )

    val idOrThrow get() = id ?: throw IllegalStateException("Id no available")
}
