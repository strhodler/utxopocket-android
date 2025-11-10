package com.strhodler.utxopocket.presentation.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.SetHiddenTopBar
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding

@Composable
fun GlossaryRoute(
    onBack: () -> Unit,
    onEntrySelected: (String) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: GlossaryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    GlossaryScreen(
        state = state,
        onBack = onBack,
        onEntrySelected = onEntrySelected,
        onOpenSearch = onOpenSearch
    )
}

@Composable
fun GlossaryScreen(
    state: GlossaryUiState,
    onBack: () -> Unit,
    onEntrySelected: (String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    SetSecondaryTopBar(
        title = stringResource(id = R.string.glossary_title),
        onBackClick = onBack
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(id = R.string.glossary_search_icon_description)
                )
            }
        },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        GlossaryBrowseList(
            entries = state.entries,
            onEntrySelected = onEntrySelected,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@Composable
private fun GlossaryBrowseList(
    entries: List<GlossaryEntry>,
    onEntrySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(entries, key = { it.id }) { entry ->
            GlossaryEntryCard(
                entry = entry,
                onClick = { onEntrySelected(entry.id) }
            )
        }
    }
}

@Composable
fun GlossarySearchRoute(
    onBack: () -> Unit,
    onEntrySelected: (String) -> Unit,
    viewModel: GlossaryViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SetHiddenTopBar()
    GlossarySearchScreen(
        state = state,
        onBack = onBack,
        onEntrySelected = onEntrySelected,
        onQueryChange = viewModel::onSearchQueryChange,
        onSearch = viewModel::onSearchSubmit,
        onClearQuery = viewModel::clearSearch
    )
}

@Composable
private fun GlossarySearchScreen(
    state: GlossaryUiState,
    onBack: () -> Unit,
    onEntrySelected: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        GlossarySearchField(
            query = state.query,
            focusRequester = focusRequester,
            onQueryChange = onQueryChange,
            onBack = onBack,
            onClear = onClearQuery,
            onSearch = {
                onSearch(it)
                focusManager.clearFocus()
            }
        )

        if (state.query.isBlank() && state.suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.glossary_search_suggestions_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                GlossarySuggestionRow(
                    suggestions = state.suggestions,
                    onSuggestionSelected = onQueryChange
                )
            }
        }

        val resultCount = state.filteredEntries.size
        GlossaryResultsList(
            entries = state.filteredEntries,
            query = state.query,
            resultCount = resultCount,
            onEntrySelected = onEntrySelected,
            onClearQuery = onClearQuery,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        }
    }
}

@Composable
private fun GlossarySearchField(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onSearch: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        placeholder = { Text(text = stringResource(id = R.string.glossary_search_placeholder)) },
        leadingIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(id = R.string.navigation_back_action)
                )
            }
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.glossary_search_clear_content_description)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}

@Composable
private fun GlossarySuggestionRow(
    suggestions: List<String>,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            AssistChip(
                onClick = { onSuggestionSelected(suggestion) },
                label = { Text(text = suggestion) }
            )
        }
    }
}

@Composable
private fun GlossaryResultsList(
    entries: List<GlossaryEntry>,
    query: String,
    resultCount: Int,
    onEntrySelected: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (query.isNotBlank()) {
            item {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.glossary_search_results,
                        count = resultCount,
                        resultCount,
                        query
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
        if (entries.isEmpty()) {
            if (query.isNotBlank()) {
                item {
                    GlossaryEmptyState(
                        query = query,
                        onReset = onClearQuery
                    )
                }
            }
        } else {
            items(entries, key = { it.id }) { entry ->
                GlossaryEntryCard(
                    entry = entry,
                    onClick = { onEntrySelected(entry.id) }
                )
            }
        }
    }
}

@Composable
private fun GlossaryEmptyState(
    query: String,
    onReset: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(GlossaryCardCornerRadius),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(id = R.string.glossary_empty_title, query),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.glossary_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onReset) {
                Text(text = stringResource(id = R.string.glossary_reset_search))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlossaryEntryCard(
    entry: GlossaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(GlossaryCardCornerRadius),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = entry.term,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = entry.shortDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}

private val GlossaryCardCornerRadius = 12.dp
