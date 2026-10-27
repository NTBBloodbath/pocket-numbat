package io.github.ntbbloodbath.pocketnumbat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ntbbloodbath.pocketnumbat.R
import io.github.ntbbloodbath.pocketnumbat.calc.Identifier
import io.github.ntbbloodbath.pocketnumbat.calc.IdentifierKind
import io.github.ntbbloodbath.pocketnumbat.ui.theme.LocalNumbatAccent
import kotlinx.coroutines.flow.collect
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
    var showClearHistory by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val hideTopBar = imeVisible && isLandscape
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
            if (!hideTopBar) {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    actions = {
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below,
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(stringResource(R.string.examples_title))
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = { showExamples = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = stringResource(R.string.examples_title),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (state.history.isNotEmpty()) {
                            TooltipBox(
                                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Below,
                                ),
                                tooltip = {
                                    PlainTooltip {
                                        Text(stringResource(R.string.clear_history))
                                    }
                                },
                                state = rememberTooltipState(),
                            ) {
                                IconButton(onClick = { showClearHistory = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.clear_history),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                TooltipAnchorPosition.Below,
                            ),
                            tooltip = {
                                PlainTooltip {
                                    Text(stringResource(R.string.help))
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            IconButton(onClick = { uriHandler.openUri(NUMBAT_TUTORIAL_URL) }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = stringResource(R.string.help),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    },
                )
            }
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

                state.history.isEmpty() -> EmptyState(
                    modifier = Modifier.weight(1f),
                    onBrowseExamples = { showExamples = true },
                )
                else -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.history, key = { it.id }) { entry ->
                            HistoryItem(
                                entry = entry,
                                onCopy = copyText,
                                onReuse = onInputChange,
                                reuseEnabled = !state.isEvaluating,
                            )
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
                identifiers = state.identifiers,
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

    if (showClearHistory) {
        AlertDialog(
            onDismissRequest = { showClearHistory = false },
            title = { Text(stringResource(R.string.clear_history_title)) },
            text = { Text(stringResource(R.string.clear_history_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClear()
                        showClearHistory = false
                    },
                ) {
                    Text(stringResource(R.string.clear_history))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistory = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun InputRow(
    input: String,
    identifiers: List<Identifier>,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val accent = LocalNumbatAccent.current
    val textState = rememberTextFieldState(input)
    val suggestionScroll = rememberScrollState()
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(input) {
        if (textState.text.toString() != input) {
            textState.setTextAndPlaceCursorAtEnd(input)
        }
    }

    LaunchedEffect(textState) {
        snapshotFlow { textState.text.toString() }
            .collect { onInputChange(it) }
    }

    val cursor = textState.selection.max
    val token = completionToken(textState.text.toString(), cursor)
    val suggestions = if (focused && token.isNotEmpty()) {
        identifiers
            .filter {
                it.name.startsWith(token, ignoreCase = true) &&
                    !it.name.equals(token, ignoreCase = true)
            }
            .sortedWith(
                compareBy<Identifier>(
                    { it.kind != IdentifierKind.FUNCTION },
                    { it.name.length },
                    { it.name },
                ),
            )
            .take(6)
    } else {
        emptyList()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(suggestionScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    suggestions.forEach { identifier ->
                        SuggestionChip(
                            onClick = { insertIdentifier(textState, identifier) },
                            label = {
                                Text(
                                    text = identifier.name +
                                        if (identifier.kind == IdentifierKind.FUNCTION) "(" else "",
                                )
                            },
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextField(
                    state = textState,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focused = it.isFocused },
                    placeholder = { Text(stringResource(R.string.input_placeholder)) },
                    prefix = {
                        Text(
                            text = ">",
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .clearAndSetSemantics {},
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            color = accent,
                        )
                    },
                    lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 1, maxHeightInLines = 4),
                    enabled = enabled,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    trailingIcon = {
                        if (textState.text.isNotEmpty() && enabled) {
                            IconButton(onClick = { textState.clearText() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_input),
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                )
                FilledIconButton(
                    onClick = {
                        onInputChange(textState.text.toString())
                        onSubmit()
                    },
                    enabled = enabled && textState.text.isNotBlank(),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(R.string.evaluate),
                    )
                }
            }
        }
    }
}

private fun completionToken(text: String, cursor: Int): String {
    val end = cursor.coerceIn(0, text.length)
    var start = end
    while (start > 0 && text[start - 1].isIdentifierPart()) {
        start--
    }
    return text.substring(start, end)
}

private fun Char.isIdentifierPart(): Boolean {
    return isLetterOrDigit() || this == '_' || this == '°' || this == 'µ' || this == 'Ω'
}

private fun insertIdentifier(textState: TextFieldState, identifier: Identifier) {
    val end = textState.selection.max
    val text = textState.text.toString()
    var start = end
    while (start > 0 && text[start - 1].isIdentifierPart()) {
        start--
    }
    val insertion = if (identifier.kind == IdentifierKind.FUNCTION) {
        "${identifier.name}("
    } else {
        identifier.name
    }
    textState.edit {
        replace(start, end, insertion)
        selection = TextRange(start + insertion.length)
    }
}

@Composable
private fun HistoryItem(
    entry: HistoryEntry,
    onCopy: (String) -> Unit,
    onReuse: (String) -> Unit,
    reuseEnabled: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SelectionContainer {
            Text(
                text = entry.input,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
            )
        }
        val error = entry.error
        val output = entry.output
        when {
            error != null -> ErrorCard(
                error = error,
                onCopy = { onCopy(error) },
                onReuse = { onReuse(entry.input) },
                reuseEnabled = reuseEnabled,
            )

            output == null -> LinearProgressIndicator(Modifier.fillMaxWidth())

            output.isEmpty() -> DefinitionRow(
                input = entry.input,
                onReuse = { onReuse(entry.input) },
                reuseEnabled = reuseEnabled,
            )

            else -> ResultCard(
                output = output,
                onCopy = { onCopy(output) },
                onReuse = { onReuse(entry.input) },
                reuseEnabled = reuseEnabled,
            )
        }
    }
}

@Composable
private fun DefinitionRow(
    input: String,
    onReuse: () -> Unit,
    reuseEnabled: Boolean,
) {
    val label = if (input.isDefinition()) R.string.defined else R.string.done
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onReuse, enabled = reuseEnabled) {
                Text(stringResource(R.string.reuse))
            }
        }
    }
}

private fun String.isDefinition(): Boolean {
    val keyword = trimStart().takeWhile { !it.isWhitespace() && it != ';' }
    return keyword in setOf("let", "fn", "function", "unit", "dimension")
}

@Composable
private fun ResultCard(
    output: String,
    onCopy: () -> Unit,
    onReuse: () -> Unit,
    reuseEnabled: Boolean,
) {
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
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            TextButton(
                onClick = onReuse,
                enabled = reuseEnabled,
            ) {
                Text(stringResource(R.string.reuse))
            }
            TextButton(onClick = onCopy) {
                Text(stringResource(R.string.copy))
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onCopy: () -> Unit,
    onReuse: () -> Unit,
    reuseEnabled: Boolean,
) {
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
            TextButton(
                onClick = onReuse,
                enabled = reuseEnabled,
            ) {
                Text(
                    text = stringResource(R.string.reuse),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            TextButton(onClick = onCopy) {
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
private fun EmptyState(
    modifier: Modifier = Modifier,
    onBrowseExamples: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onBrowseExamples) {
            Text(stringResource(R.string.browse_examples))
        }
    }
}

private const val NUMBAT_TUTORIAL_URL = "https://numbat.dev/docs/tutorial/"
private const val NUMBAT_EXAMPLES_URL = "https://numbat.dev/docs/examples/"
private const val NUMBAT_SYNTAX_URL = "https://numbat.dev/docs/examples/example-numbat_syntax/"

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
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
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
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        group.examples.forEach { example ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { onExampleSelected(example) }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = ">",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = LocalNumbatAccent.current,
                                )
                                Text(
                                    text = example,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.more_on_numbat_dev),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    listOf(
                        R.string.numbat_examples to NUMBAT_EXAMPLES_URL,
                        R.string.numbat_syntax to NUMBAT_SYNTAX_URL,
                    ).forEach { (labelRes, url) ->
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { uriHandler.openUri(url) }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
