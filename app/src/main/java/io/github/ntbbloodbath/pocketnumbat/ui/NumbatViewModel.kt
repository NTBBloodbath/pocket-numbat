package io.github.ntbbloodbath.pocketnumbat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ntbbloodbath.pocketnumbat.calc.NumbatEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HistoryEntry(
    val id: Long,
    val input: String,
    val output: String? = null,
    val error: String? = null,
)

data class UiState(
    val history: List<HistoryEntry> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isEvaluating: Boolean = false,
)

class NumbatViewModel : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val engineDispatcher = Dispatchers.IO.limitedParallelism(1)
    private var engine: NumbatEngine? = null
    private var nextId = 0L

    init {
        viewModelScope.launch {
            val created = try {
                withContext(engineDispatcher) { NumbatEngine.create() }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadError = error.message ?: error.javaClass.simpleName,
                    )
                }
                return@launch
            }

            if (!isActive) {
                withContext(NonCancellable + engineDispatcher) { created.close() }
                return@launch
            }

            engine = created
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(input = value) }
    }

    fun submit() {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.isEvaluating) return

        val input = snapshot.input.trim()
        if (input.isEmpty()) return

        val currentEngine = engine
        val entryId = nextId++

        _state.update {
            it.copy(
                input = "",
                isEvaluating = true,
                history = it.history + HistoryEntry(id = entryId, input = input),
            )
        }

        viewModelScope.launch {
            val result = withContext(engineDispatcher) {
                if (currentEngine == null) {
                    Result.failure<String>(IllegalStateException("Numbat engine is not ready"))
                } else {
                    runCatching { currentEngine.eval(input) }
                }
            }

            result.onSuccess { output ->
                updateEntry(entryId) { it.copy(output = output) }
            }.onFailure { error ->
                updateEntry(entryId) {
                    it.copy(error = error.message ?: error.javaClass.simpleName)
                }
            }

            _state.update { it.copy(isEvaluating = false) }
        }
    }

    fun clearHistory() {
        _state.update { it.copy(history = emptyList()) }
    }

    override fun onCleared() {
        val currentEngine = engine
        engine = null
        if (currentEngine != null) {
            CoroutineScope(SupervisorJob() + engineDispatcher).launch {
                currentEngine.close()
            }
        }
        super.onCleared()
    }

    private fun updateEntry(id: Long, transform: (HistoryEntry) -> HistoryEntry) {
        _state.update { state ->
            state.copy(
                history = state.history.map { entry ->
                    if (entry.id == id) transform(entry) else entry
                },
            )
        }
    }
}
