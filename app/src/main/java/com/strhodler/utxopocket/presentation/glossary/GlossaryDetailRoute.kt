package com.strhodler.utxopocket.presentation.glossary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AssistChip
import com.strhodler.utxopocket.presentation.common.ScreenScaffoldInsets
import com.strhodler.utxopocket.presentation.common.applyScreenPadding

@Composable
fun GlossaryDetailRoute(
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    viewModel: GlossaryDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GlossaryDetailScreen(
        state = state,
        onBack = onBack,
        onOpenWikiTopic = onOpenWikiTopic
    )
}

@Composable
private fun GlossaryDetailScreen(
    state: GlossaryDetailUiState,
    onBack: () -> Unit,
    onOpenWikiTopic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (state) {
        is GlossaryDetailUiState.Success -> state.entry.term
        else -> stringResource(id = R.string.glossary_title)
    }

    SetSecondaryTopBar(
        title = title,
        onBackClick = onBack
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = ScreenScaffoldInsets
    ) { paddingValues ->
        when (state) {
            GlossaryDetailUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            GlossaryDetailUiState.NotFound -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.glossary_detail_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    TextButton(onClick = onBack) {
                        Text(text = stringResource(id = R.string.glossary_detail_back_to_list))
                    }
                }
            }

            is GlossaryDetailUiState.Success -> {
                GlossaryDetailContent(
                    entry = state.entry,
                    relatedWiki = state.relatedWiki,
                    onOpenWikiTopic = onOpenWikiTopic,
                    paddingValues = paddingValues
                )
            }
        }
    }
}

@Composable
private fun GlossaryDetailContent(
    entry: GlossaryEntry,
    relatedWiki: List<com.strhodler.utxopocket.presentation.wiki.WikiTopic>,
    onOpenWikiTopic: (String) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .applyScreenPadding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = entry.term,
            style = MaterialTheme.typography.headlineSmall
        )
        entry.definition.forEach { paragraph ->
            Text(
                text = paragraph,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (relatedWiki.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.glossary_detail_related_wiki_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    relatedWiki.forEach { topic ->
                        AssistChip(
                            onClick = { onOpenWikiTopic(topic.id) },
                            label = { Text(text = topic.title) }
                        )
                    }
                }
            }
        }
    }
}
