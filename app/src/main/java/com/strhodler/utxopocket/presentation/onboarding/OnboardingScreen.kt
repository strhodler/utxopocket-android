package com.strhodler.utxopocket.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.strhodler.utxopocket.R
import com.strhodler.utxopocket.domain.model.BitcoinNetwork
import com.strhodler.utxopocket.presentation.components.network.NetworkSelector

private const val TutorialSlideCount = 6
private val SlideTextMaxWidth = 520.dp

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        state = state,
        onGetStarted = viewModel::onGetStarted,
        onSkipTutorial = { viewModel.complete(onFinished) },
        onNext = viewModel::onNext,
        onPrevious = viewModel::onPrevious,
        onComplete = { viewModel.complete(onFinished) },
        onSlideChanged = viewModel::onSlideIndexSelected,
        onNetworkSelected = viewModel::onNetworkSelected
    )
}

@Composable
private fun OnboardingScreen(
    state: OnboardingUiState,
    onGetStarted: () -> Unit,
    onSkipTutorial: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onSlideChanged: (Int) -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        when (state.step) {
            OnboardingStep.Welcome -> WelcomeStep(onGetStarted, onSkipTutorial)
            OnboardingStep.SlideOne,
            OnboardingStep.SlideTwo,
            OnboardingStep.SlideThree,
            OnboardingStep.SlideFour,
            OnboardingStep.SlideFive,
            OnboardingStep.SlideNetwork -> TutorialStep(
                state = state,
                onNext = onNext,
                onPrevious = onPrevious,
                onComplete = onComplete,
                onSlideChanged = onSlideChanged,
                onNetworkSelected = onNetworkSelected
            )
        }
    }
}

@Composable
private fun WelcomeStep(
    onGetStarted: () -> Unit,
    onSkipTutorial: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = SlideTextMaxWidth)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = SlideTextMaxWidth)
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(TutorialSlideCount) { idx ->
                    Surface(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx == 0) 32.dp else 6.dp),
                        shape = CircleShape,
                        color = if (idx == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkipTutorial,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.onboarding_skip))
                }
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.onboarding_get_started))
                }
            }
        }
    }
}

private data class TutorialSlide(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val content: (@Composable () -> Unit)? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TutorialStep(
    state: OnboardingUiState,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onSlideChanged: (Int) -> Unit,
    onNetworkSelected: (BitcoinNetwork) -> Unit
) {
    val slides = listOf(
        TutorialSlide(
            titleRes = R.string.onboarding_slide_watch_title,
            descriptionRes = R.string.onboarding_slide_watch_description
        ),
        TutorialSlide(
            titleRes = R.string.onboarding_slide_descriptors_title,
            descriptionRes = R.string.onboarding_slide_descriptors_description
        ),
        TutorialSlide(
            titleRes = R.string.onboarding_slide_bips_title,
            descriptionRes = R.string.onboarding_slide_bips_description
        ),
        TutorialSlide(
            titleRes = R.string.onboarding_slide_privacy_title,
            descriptionRes = R.string.onboarding_slide_privacy_description
        ),
        TutorialSlide(
            titleRes = R.string.onboarding_slide_trust_title,
            descriptionRes = R.string.onboarding_slide_trust_description
        ),
        TutorialSlide(
            titleRes = R.string.onboarding_slide_network_title,
            descriptionRes = R.string.onboarding_slide_network_description,
            content = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = SlideTextMaxWidth),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    NetworkSelector(
                        selectedNetwork = state.selectedNetwork,
                        onNetworkSelected = onNetworkSelected,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    )
    val index = state.step.toSlideIndex() ?: 0
    val pagerState = rememberPagerState(
        initialPage = index,
        pageCount = { slides.size }
    )

    LaunchedEffect(index) {
        if (pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (state.step.toSlideIndex() != pagerState.currentPage) {
            onSlideChanged(pagerState.currentPage)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) { page ->
            val slide = slides[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(id = slide.titleRes),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = SlideTextMaxWidth)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = slide.descriptionRes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = SlideTextMaxWidth)
                )
                slide.content?.let { content ->
                    Spacer(modifier = Modifier.height(24.dp))
                    content()
                }
            }
        }
        val currentPage = pagerState.currentPage
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(slides.size) { idx ->
                    Surface(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx == currentPage) 32.dp else 6.dp),
                        shape = CircleShape,
                        color = if (idx == currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {}
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentPage > 0) {
                    OutlinedButton(
                        onClick = onPrevious,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                } else {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .height(0.dp)
                    )
                }
                Button(
                    onClick = {
                        if (currentPage < slides.lastIndex) {
                            onNext()
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (currentPage < slides.lastIndex) {
                            stringResource(R.string.onboarding_next)
                        } else {
                            stringResource(R.string.onboarding_finish)
                        }
                    )
                }
            }
        }
    }
}

