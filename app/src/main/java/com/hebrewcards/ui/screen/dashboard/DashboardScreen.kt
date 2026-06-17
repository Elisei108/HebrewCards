package com.hebrewcards.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hebrewcards.data.db.entity.Deck
import com.hebrewcards.data.repository.DeckProgress
import com.hebrewcards.domain.model.DeckType
import com.hebrewcards.ui.navigation.Routes
import com.hebrewcards.ui.theme.*

@Composable
fun DashboardScreen(
    navController: NavController,
    vm: DashboardViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colors = LocalAppColors.current

    Scaffold(
        containerColor = colors.background,
        bottomBar = { BottomNavBar(navController) },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { navController.navigate(Routes.ADD_DECK) },
                containerColor = ColorFlashcard,
                contentColor   = Color.White,
                shape          = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить колоду")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorFlashcard)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatsCard(
                    totalWordsLearned = uiState.totalWordsLearned,
                    currentStreak     = uiState.currentStreak,
                    maxStreak         = uiState.maxStreak
                )
            }

            item {
                Text(
                    text       = "Мои колоды",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = colors.textPrimary,
                    modifier   = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            if (uiState.decks.isEmpty()) {
                item { EmptyDecksHint(colors) }
            } else {
                items(uiState.decks, key = { it.deck.id }) { item ->
                    DeckCard(
                        item    = item,
                        onClick = { navController.navigate(Routes.deck(item.deck.id)) },
                        onDelete = { vm.deleteDeck(item.deck) },
                        onReset  = { vm.resetProgress(item.deck.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun StatsCard(totalWordsLearned: Int, currentStreak: Int, maxStreak: Int) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("📚", totalWordsLearned.toString(), "слов изучено")
        StatDivider(colors)
        StatItem("🔥", currentStreak.toString(), "дней подряд")
        StatDivider(colors)
        StatItem("⚡", maxStreak.toString(), "макс. дней")
    }
}

@Composable
private fun StatItem(icon: String, value: String, label: String) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Text(label, fontSize = 11.sp, color = colors.textSecondary)
    }
}

@Composable
private fun StatDivider(colors: AppColors) {
    Box(Modifier.height(40.dp).width(1.dp).background(colors.border))
}

@Composable
private fun DeckCard(
    item: DeckUiItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onReset: () -> Unit
) {
    val colors = LocalAppColors.current
    var menuExpanded     by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showResetDialog  by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (item.deck.type == DeckType.VERB) "🔤" else "📖", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = item.deck.name,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color      = colors.textPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, "Меню", tint = colors.textSecondary)
                    }
                    DropdownMenu(
                        expanded         = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor   = colors.surface2
                    ) {
                        DropdownMenuItem(
                            text        = { Text("Сбросить прогресс", color = ColorChoice) },
                            onClick     = { menuExpanded = false; showResetDialog = true },
                            leadingIcon = { Icon(Icons.Default.Refresh, null, tint = ColorChoice) }
                        )
                        DropdownMenuItem(
                            text        = { Text("Удалить колоду", color = ColorDestructive) },
                            onClick     = { menuExpanded = false; showDeleteDialog = true },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = ColorDestructive) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            DeckProgressBar(item.progress)
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("✓ ${item.progress.known}",   fontSize = 12.sp, color = ColorFlashcard)
                Text("↺ ${item.progress.learning}", fontSize = 12.sp, color = ColorWriting)
                Text("★ ${item.progress.new}",      fontSize = 12.sp, color = ColorChoice)
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title        = "Удалить колоду?",
            text         = "«${item.deck.name}» и весь прогресс будут удалены безвозвратно.",
            confirmText  = "Удалить",
            confirmColor = ColorDestructive,
            onConfirm    = { showDeleteDialog = false; onDelete() },
            onDismiss    = { showDeleteDialog = false }
        )
    }
    if (showResetDialog) {
        ConfirmDialog(
            title        = "Сбросить прогресс?",
            text         = "Все карточки вернутся в статус «Новые».",
            confirmText  = "Сбросить",
            confirmColor = ColorChoice,
            onConfirm    = { showResetDialog = false; onReset() },
            onDismiss    = { showResetDialog = false }
        )
    }
}

@Composable
private fun DeckProgressBar(progress: DeckProgress) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxWidth().height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(colors.border)
    ) {
        Box(
            modifier = Modifier.fillMaxHeight()
                .fillMaxWidth(progress.knownFraction)
                .clip(RoundedCornerShape(3.dp)).background(ColorFlashcard)
        )
    }
}

@Composable
private fun EmptyDecksHint(colors: AppColors) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🇮🇱", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text("Колод пока нет", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Нажми + чтобы добавить первую колоду", fontSize = 14.sp, color = colors.textSecondary)
    }
}

@Composable
private fun BottomNavBar(navController: NavController) {
    val colors = LocalAppColors.current
    NavigationBar(containerColor = colors.surface) {
        NavigationBarItem(
            selected = true,
            onClick  = {},
            icon     = { Icon(Icons.Default.Home, null) },
            label    = { Text("Колоды") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = ColorFlashcard,
                selectedTextColor   = ColorFlashcard,
                indicatorColor      = ColorFlashcard.copy(alpha = 0.15f),
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = { navController.navigate(Routes.STATS) },
            icon     = { Icon(Icons.Default.BarChart, null) },
            label    = { Text("Статистика") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = ColorFlashcard,
                selectedTextColor   = ColorFlashcard,
                indicatorColor      = ColorFlashcard.copy(alpha = 0.15f),
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = { navController.navigate(Routes.SETTINGS) },
            icon     = { Icon(Icons.Default.Settings, null) },
            label    = { Text("Настройки") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = ColorFlashcard,
                selectedTextColor   = ColorFlashcard,
                indicatorColor      = ColorFlashcard.copy(alpha = 0.15f),
                unselectedIconColor = colors.textSecondary,
                unselectedTextColor = colors.textSecondary
            )
        )
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = colors.surface,
        title            = { Text(title, color = colors.textPrimary, fontWeight = FontWeight.SemiBold) },
        text             = { Text(text, color = colors.textSecondary) },
        confirmButton    = { TextButton(onClick = onConfirm) { Text(confirmText, color = confirmColor, fontWeight = FontWeight.SemiBold) } },
        dismissButton    = { TextButton(onClick = onDismiss) { Text("Отмена", color = colors.textSecondary) } }
    )
}
