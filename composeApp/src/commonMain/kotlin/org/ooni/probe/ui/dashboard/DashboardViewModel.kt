package org.ooni.probe.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ooni.engine.models.TaskOrigin
import org.ooni.probe.data.models.Descriptor
import org.ooni.probe.data.models.RunSpecification
import org.ooni.probe.data.models.TestState

class DashboardViewModel(
    getTestDescriptors: () -> Flow<List<Descriptor>>,
    runDescriptors: suspend (RunSpecification) -> Unit,
    getCurrentTestState: () -> Flow<TestState>,
) : ViewModel() {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 1)

    private val _state = MutableStateFlow(State())
    val state = _state.asStateFlow()

    init {
        getTestDescriptors()
            .onEach { tests ->
                _state.update { it.copy(tests = tests.groupByType()) }
            }
            .launchIn(viewModelScope)

        getCurrentTestState()
            .onEach { testState ->
                _state.update { it.copy(testState = testState) }
            }
            .launchIn(viewModelScope)

        events
            .onEach { event ->
                when (event) {
                    Event.StartClick -> {
                        coroutineScope {
                            launch {
                                runDescriptors(buildRunSpecification())
                            }
                        }
                    }
                }
            }.launchIn(viewModelScope)
    }

    fun onEvent(event: Event) {
        events.tryEmit(event)
    }

    private fun List<Descriptor>.groupByType() =
        mapOf(
            DescriptorType.Default to filter { it.source is Descriptor.Source.Default },
            DescriptorType.Installed to filter { it.source is Descriptor.Source.Installed },
        )

    private fun buildRunSpecification(): RunSpecification {
        val allTests = state.value.tests.values.flatten()
        return RunSpecification(
            tests =
                allTests.map { test ->
                    RunSpecification.Test(
                        source =
                            when (test.source) {
                                is Descriptor.Source.Default ->
                                    RunSpecification.Test.Source.Default(test.name)

                                is Descriptor.Source.Installed ->
                                    RunSpecification.Test.Source.Installed(test.source.value.id)
                            },
                        netTests = test.netTests,
                    )
                },
            taskOrigin = TaskOrigin.OoniRun,
            isRerun = false,
        )
    }

    enum class DescriptorType {
        Default,
        Installed,
    }

    data class State(
        val tests: Map<DescriptorType, List<Descriptor>> = emptyMap(),
        val testState: TestState = TestState.Idle,
    )

    sealed interface Event {
        data object StartClick : Event
    }
}
