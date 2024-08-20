package org.ooni.probe.data.repositories

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.di.Dependencies
import org.ooni.testing.createTestDatabaseDriver
import org.ooni.testing.factories.ResultModelFactory
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ResultRepositoryTest {
    private lateinit var subject: ResultRepository

    @BeforeTest
    fun before() {
        subject =
            ResultRepository(
                database = Dependencies.buildDatabase(::createTestDatabaseDriver),
                backgroundDispatcher = Dispatchers.Default,
            )
    }

    @Test
    fun createWithIdAndGet() =
        runTest {
            val model = ResultModelFactory.build(id = ResultModel.Id(Random.nextLong().absoluteValue))

            val modelId = subject.createOrUpdate(model)
            val result = subject.list().first().first()

            assertEquals(model, result)
            assertEquals(model.id, modelId)
        }

    @Test
    fun createWithoutId() =
        runTest {
            val model = ResultModelFactory.build(id = null)

            val modelId = subject.createOrUpdate(model)

            assertNotNull(modelId)
        }
}
