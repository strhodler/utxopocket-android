package com.strhodler.utxopocket.presentation.wiki

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.presentation.navigation.HideMainBottomBar
import com.strhodler.utxopocket.presentation.navigation.SetSecondaryTopBar

@Composable
fun WikiDetailRoute(
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenGlossaryEntry: (String) -> Unit,
    viewModel: WikiDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val title = state.topic?.title ?: stringResource(id = R.string.wiki_detail_missing_title)

    HideMainBottomBar()
    SetSecondaryTopBar(
        title = title,
        onBackClick = onBack
    )
    WikiDetailScreen(
        state = state,
        onTopicSelected = onOpenTopic,
        onGlossarySelected = onOpenGlossaryEntry
    )
}

@Composable
fun WikiDetailScreen(
    state: WikiDetailUiState,
    onTopicSelected: (String) -> Unit,
    onGlossarySelected: (String) -> Unit
) {
    if (state.isMissing || state.topic == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.wiki_detail_missing_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.wiki_detail_missing_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val topic = state.topic
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = topic.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(
                items = topic.sections,
                key = { section -> section.title ?: section.hashCode() }
            ) { section ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    section.title?.let { heading ->
                        Text(
                            text = heading,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    section.paragraphs.forEach { paragraph ->
                        Text(
                            text = paragraph,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            if (state.relatedTopics.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.wiki_detail_related_topics_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.relatedTopics.forEach { related ->
                                AssistChip(
                                    onClick = { onTopicSelected(related.id) },
                                    label = { Text(text = related.title) }
                                )
                            }
                        }
                    }
                }
            }
            if (state.relatedGlossary.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.wiki_detail_related_glossary_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            state.relatedGlossary.forEach { entry ->
                                AssistChip(
                                    onClick = { onGlossarySelected(entry.id) },
                                    label = { Text(text = entry.term) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
