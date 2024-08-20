package org.ooni.probe.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import org.ooni.engine.Engine.MkException
import org.ooni.engine.models.Result
import org.ooni.engine.models.TaskOrigin
import org.ooni.engine.models.TestType
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.NetTest
import org.ooni.probe.data.models.ResultModel
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.TestRunError
import org.ooni.probe.data.models.TestRunState
import org.ooni.probe.data.models.UrlModel
import kotlin.time.Duration.Companion.seconds

class RunDescriptors(
    private val getTestDescriptorsBySpec: suspend (RunSpecification) -> List<Descriptor>,
    private val downloadUrls: suspend (TaskOrigin) -> Result<List<UrlModel>, MkException>,
    private val storeResult: suspend (ResultModel) -> ResultModel.Id,
    private val getCurrentTestRunState: Flow<TestRunState>,
    private val setCurrentTestState: ((TestRunState) -> TestRunState) -> Unit,
    private val observeCancelTestRun: Flow<Unit>,
    private val reportTestRunError: (TestRunError) -> Unit,
    private val runNetTest: suspend (RunNetTest.Specification) -> Unit,
) {
    suspend operator fun invoke(spec: RunSpecification) {
        val descriptors = getTestDescriptorsBySpec(spec)
        val descriptorsWithFinalInputs = descriptors.prepareInputs(spec.taskOrigin)

        if (getCurrentTestRunState.first() is TestRunState.Running) {
            Logger.i("Tests are already running, so we won't run other tests")
            return
        }
        setCurrentTestState {
            TestRunState.Running(estimatedRuntime = descriptorsWithFinalInputs.estimatedRuntime)
        }

        runDescriptorsCancellable(descriptorsWithFinalInputs, spec)

        setCurrentTestState { TestRunState.Idle }
    }

    private suspend fun runDescriptorsCancellable(
        descriptors: List<Descriptor>,
        spec: RunSpecification,
    ) {
        coroutineScope {
            val runJob = async {
                // Actually running the descriptors
                descriptors.forEach { descriptor ->
                    runDescriptor(descriptor, spec.taskOrigin, spec.isRerun)
                }
            }
            // Observe if a cancel request has been made
            val cancelJob = async {
                observeCancelTestRun
                    .take(1)
                    .collect {
                        setCurrentTestState { TestRunState.Stopping }
                        runJob.cancel()
                    }
            }

            try {
                runJob.await()
            } catch (e: Exception) {
                // Exceptions were logged in the Engine
            }
            if (cancelJob.isActive) {
                cancelJob.cancel()
            }
        }
    }

    private suspend fun List<Descriptor>.prepareInputs(taskOrigin: TaskOrigin) =
        map { descriptor ->
            descriptor.copy(
                netTests = descriptor.netTests.downloadUrlsIfNeeded(taskOrigin),
                longRunningTests = descriptor.longRunningTests.downloadUrlsIfNeeded(taskOrigin),
            )
        }

    private suspend fun List<NetTest>.downloadUrlsIfNeeded(taskOrigin: TaskOrigin): List<NetTest> =
        map { test -> test.copy(inputs = test.inputsOrDownloadUrls(taskOrigin)) }

    private suspend fun NetTest.inputsOrDownloadUrls(taskOrigin: TaskOrigin): List<String>? {
        if (!inputs.isNullOrEmpty() || test !is TestType.WebConnectivity) return inputs

        val urls = downloadUrls(taskOrigin)
            .onFailure { Logger.w("Could not download urls", it) }
            .get()
            ?.map { it.url }
            ?: emptyList()

        if (urls.isEmpty()) {
            reportTestRunError(TestRunError.DownloadUrlsFailed)
        }

        return urls
    }

    private val List<Descriptor>.estimatedRuntime
        get() = map { descriptor ->
            descriptor.netTests.sumOf {
                it.test.runtime(it.inputs).inWholeSeconds
            }.seconds
        }

    private suspend fun runDescriptor(
        descriptor: Descriptor,
        taskOrigin: TaskOrigin,
        isRerun: Boolean,
    ) {
        val newResult = ResultModel(
            testGroupName = descriptor.name,
            testDescriptorId = (descriptor.source as? Descriptor.Source.Installed)?.value?.id,
        )
        val resultWithId = newResult.copy(id = storeResult(newResult))

        (descriptor.netTests + descriptor.longRunningTests).forEachIndexed { index, netTest ->
            runNetTest(
                RunNetTest.Specification(
                    descriptor = descriptor,
                    netTest = netTest,
                    taskOrigin = taskOrigin,
                    isRerun = isRerun,
                    // TODO: fetch max runtime from preferences
                    maxRuntime = null,
                    initialResult = resultWithId,
                    testIndex = index,
                ),
            )
        }
    }
}
