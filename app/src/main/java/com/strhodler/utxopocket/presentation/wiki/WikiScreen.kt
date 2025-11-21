package com.strhodler.utxopocket.presentation.wiki

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding
import com.strhodler.utxopocket.presentation.navigation.HideMainBottomBar
import com.strhodler.utxopocket.presentation.navigation.SetHiddenTopBar
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar

@Composable
fun WikiRoute(
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: WikiViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WikiScreen(
        state = state,
        onBack = onBack,
        onTopicSelected = onTopicSelected,
        onOpenSearch = onOpenSearch
    )
}

@Composable
fun WikiScreen(
    state: WikiUiState,
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    HideMainBottomBar()
    SetSecondaryTopBar(
        title = stringResource(id = R.string.nav_wiki),
        onBackClick = onBack
    )
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenSearch) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(id = R.string.wiki_search_icon_description)
                )
            }
        },
        contentWindowInsets = ScreenScaffoldInsets
    ) { innerPadding ->
        WikiBrowseList(
            categories = state.categories,
            onTopicSelected = onTopicSelected,
            modifier = Modifier
                .fillMaxSize()
                .applyScreenPadding(innerPadding)
        )
    }
}

@Composable
fun WikiSearchRoute(
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
    viewModel: WikiViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HideMainBottomBar()
    SetHiddenTopBar()
    WikiSearchScreen(
        state = state,
        onBack = onBack,
        onTopicSelected = onTopicSelected,
        onQueryChange = viewModel::onSearchQueryChange,
        onSearch = viewModel::onSearchSubmit,
        onClearQuery = viewModel::clearSearch
    )
}

@Composable
private fun WikiSearchScreen(
    state: WikiUiState,
    onBack: () -> Unit,
    onTopicSelected: (String) -> Unit,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SearchInputField(
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

        val showSuggestions = state.query.isBlank() && state.suggestions.isNotEmpty()
        if (showSuggestions) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.wiki_search_suggestions_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WikiSuggestionRow(
                    suggestions = state.suggestions,
                    onSuggestionSelected = onQueryChange
                )
            }
        }

        val resultCount = state.filteredCategories.sumOf { it.topics.size }
        WikiResultsList(
            categories = state.filteredCategories,
            query = state.query,
            resultCount = resultCount,
            onTopicSelected = onTopicSelected,
            onClearQuery = onClearQuery,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        )
    }
}

@Composable
private fun SearchInputField(
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
        placeholder = { Text(text = stringResource(id = R.string.wiki_search_placeholder)) },
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
                        contentDescription = stringResource(id = R.string.wiki_search_clear_content_description)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch(query) }
        ),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}


@Composable
private fun WikiBrowseList(
    categories: List<WikiCategory>,
    onTopicSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(categories, key = { it.id }) { category ->
            WikiCategorySection(
                category = category,
                onTopicSelected = onTopicSelected
            )
        }
    }
}

@Composable
private fun WikiResultsList(
    categories: List<WikiCategory>,
    query: String,
    resultCount: Int,
    onTopicSelected: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (query.isNotBlank()) {
            item {
                WikiSearchResultsHeader(
                    query = query,
                    resultCount = resultCount
                )
            }
        }
        if (categories.isEmpty()) {
            if (query.isNotBlank()) {
                item {
                    WikiSearchEmptyState(
                        query = query,
                        onReset = onClearQuery
                    )
                }
            }
        } else {
            items(categories, key = { it.id }) { category ->
                WikiCategorySection(
                    category = category,
                    onTopicSelected = onTopicSelected
                )
            }
        }
    }
}

@Composable
private fun WikiSuggestionRow(
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
private fun WikiSearchResultsHeader(
    query: String,
    resultCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wiki_search_results_title, query),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = pluralStringResource(
                id = R.plurals.wiki_search_results_count,
                count = resultCount,
                resultCount
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WikiSearchEmptyState(
    query: String,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wiki_search_empty_title, query),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.wiki_search_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onReset) {
            Text(text = stringResource(id = R.string.wiki_search_empty_action))
        }
    }
}

@Composable
private fun WikiCategorySection(
    category: WikiCategory,
    onTopicSelected: (String) -> Unit
) {
    val title = remember(category.title) { sanitizeCategoryTitle(category.title) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = category.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            category.topics.forEach { topic ->
                WikiTopicListItem(topic = topic, onTopicSelected = onTopicSelected)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WikiTopicListItem(
    topic: WikiTopic,
    onTopicSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTopicSelected(topic.id) },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(WikiCardCornerRadius),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = topic.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        )
    }
}

private fun sanitizeCategoryTitle(title: String): String =
    title.replaceFirst(Regex("^[^\\p{L}\\p{N}]+\\s*"), "")

private val WikiCardCornerRadius = 12.dp
