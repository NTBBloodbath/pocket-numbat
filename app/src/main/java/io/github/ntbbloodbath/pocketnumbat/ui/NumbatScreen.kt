package io.github.ntbbloodbath.pocketnumbat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ntbbloodbath.pocketnumbat.R
import kotlinx.coroutines.launch

@Composable
fun NumbatScreen(viewModel: NumbatViewModel) {
    val state by viewModel.state.collectAsState()

    NumbatContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onSubmit = viewModel::submit,
        onClear = viewModel::clearHistory,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumbatContent(
    state: UiState,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showExamples by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val copyText: (String) -> Unit = { text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Numbat", text))
        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(state.history.size, state.isEvaluating) {
        if (state.history.isNotEmpty()) {
            listState.animateScrollToItem(state.history.lastIndex)
        }
    }

    LaunchedEffect(imeVisible) {
        if (!imeVisible && state.history.isNotEmpty()) {
            listState.requestScrollToItem(state.history.lastIndex)
        }
    }


    val loadError = state.loadError

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(onClick = { showExamples = true }) {
                        Text(stringResource(R.string.help))
                    }
                    if (state.history.isNotEmpty()) {
                        TextButton(onClick = onClear) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            when {
                state.isLoading -> LoadingState(modifier = Modifier.weight(1f))
                loadError != null -> LoadErrorState(
                    message = loadError,
                    modifier = Modifier.weight(1f),
                )

                state.history.isEmpty() -> EmptyState(modifier = Modifier.weight(1f))
                else -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.history, key = { it.id }) { entry ->
                            HistoryItem(entry = entry, onCopy = copyText)
                        }
                    }
                    if (listState.canScrollForward) {
                        SmallFloatingActionButton(
                            onClick = {
                                if (state.history.isNotEmpty()) {
                                    scope.launch {
                                        listState.animateScrollToItem(state.history.lastIndex)
                                    }
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.jump_to_latest),
                            )
                        }
                    }
                }
            }

            InputRow(
                input = state.input,
                enabled = !state.isLoading && loadError == null && !state.isEvaluating,
                onInputChange = onInputChange,
                onSubmit = onSubmit,
            )
        }
    }

    if (showExamples) {
        ExamplesSheet(
            onDismiss = { showExamples = false },
            onExampleSelected = { expression ->
                onInputChange(expression)
                showExamples = false
            },
        )
    }
}

@Composable
private fun InputRow(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.input_placeholder)) },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        )
        FilledIconButton(
            onClick = onSubmit,
            enabled = enabled && input.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.evaluate),
            )
        }
    }
}

@Composable
private fun HistoryItem(entry: HistoryEntry, onCopy: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = entry.input,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
        )

        val error = entry.error
        val output = entry.output
        when {
            error != null -> ErrorCard(error = error, onCopy = onCopy)
            output != null -> ResultCard(output = output, onCopy = onCopy)
            else -> LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultCard(output: String, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = output,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            TextButton(onClick = { onCopy(output) }) {
                Text(stringResource(R.string.copy))
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onCopy: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectionContainer(modifier = Modifier.weight(1f)) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            TextButton(onClick = { onCopy(error) }) {
                Text(
                    text = stringResource(R.string.copy),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoadErrorState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.load_error, message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_example),
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class ExampleGroup(
    val titleRes: Int,
    val examples: List<String>,
    val noteRes: Int? = null,
)

private val exampleGroups = listOf(
    ExampleGroup(
        titleRes = R.string.examples_units,
        examples = listOf(
            "2 m + 30 cm",
            "2 in to cm",
            "100 km / 2 h -> km/h",
        ),
    ),
    ExampleGroup(
        titleRes = R.string.examples_currency,
        examples = listOf(
            "1 USD -> EUR",
            "10 EUR -> USD",
        ),
        noteRes = R.string.examples_currency_note,
    ),
    ExampleGroup(
        titleRes = R.string.examples_variables,
        examples = listOf("let radius = 5 cm; circle_area(radius)"),
    ),
    ExampleGroup(
        titleRes = R.string.examples_functions,
        examples = listOf(
            "sin(30 deg)",
            "sqrt(2)",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamplesSheet(
    onDismiss: () -> Unit,
    onExampleSelected: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.examples_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.examples_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            exampleGroups.forEach { group ->
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(group.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                group.noteRes?.let { noteRes ->
                    Text(
                        text = stringResource(noteRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                group.examples.forEach { example ->
                    Text(
                        text = example,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onExampleSelected(example) }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }
    }
}
