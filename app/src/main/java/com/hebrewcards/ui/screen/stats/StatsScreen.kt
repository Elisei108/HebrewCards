package com.hebrewcards.ui.screen.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hebrewcards.domain.model.DeckType
import com.hebrewcards.ui.screen.dashboard.DashboardViewModel
import com.hebrewcards.ui.screen.dashboard.DeckUiItem
import com.hebrewcards.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    vm: DashboardViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors  = LocalAppColors.current

    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {
                    Text("Статистика", fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorFlashcard)
                }
            }

            uiState.decks.isEmpty() -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🌱", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text      = "Начни заниматься —",
                        fontSize  = 16.sp,
                        color     = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text      = "здесь появится статистика",
                        fontSize  = 16.sp,
                        color     = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                val totalLearning = uiState.decks.sumOf { it.progress.learning }

                LazyColumn(
                    modifier       = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Три плашки-числа сверху
                    item {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatsBadge(
                                icon     = "📚",
                                value    = uiState.totalWordsLearned.toString(),
                                label    = "слов знаю",
                                modifier = Modifier.weight(1f)
                            )
                            StatsBadge(
                                icon     = "📖",
                                value    = uiState.decks.size.toString(),
                                label    = "колод",
                                modifier = Modifier.weight(1f)
                            )
                            StatsBadge(
                                icon     = "✓",
                                value    = totalLearning.toString(),
                                label    = "изучаю",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text       = "По колодам",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = colors.textSecondary,
                            modifier   = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    items(uiState.decks, key = { it.deck.id }) { item ->
                        DeckStatCard(item = item)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsBadge(icon: String, value: String, label: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            Text(label, fontSize = 11.sp, color = colors.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DeckStatCard(item: DeckUiItem) {
    val colors = LocalAppColors.current
    Card(
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.deck.type == DeckType.VERB) "🔤" else "📖", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = item.deck.name,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color      = colors.textPrimary,
                    modifier   = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Прогресс-бар: доля известных слов
            LinearProgressIndicator(
                progress  = { item.progress.knownFraction },
                modifier  = Modifier.fillMaxWidth().height(6.dp),
                color     = ColorFlashcard,
                trackColor = colors.border
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("✓ ${item.progress.known}",    fontSize = 12.sp, color = ColorFlashcard)
                Text("↺ ${item.progress.learning}", fontSize = 12.sp, color = ColorWriting)
                Text("★ ${item.progress.new}",      fontSize = 12.sp, color = ColorChoice)
            }
        }
    }
}
