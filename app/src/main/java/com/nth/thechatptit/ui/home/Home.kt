package com.nth.thechatptit.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.model.WorkoutType
import com.nth.thechatptit.ui.common.distanceInUserUnits
import com.nth.thechatptit.ui.common.distanceUnitLabel
import com.nth.thechatptit.ui.common.formatDistance
import com.nth.thechatptit.ui.common.formatPace
import com.nth.thechatptit.ui.common.formatSpeed
import com.nth.thechatptit.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun Home(
    modifier: Modifier,
    viewModel: HomeViewModel,
    onWorkoutClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val distanceUnit by viewModel.distanceUnit.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = displayName?.let { "Xin chào, $it" } ?: "Xin chào",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Sẵn sàng cho hoạt động hôm nay?", color = colors.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Làm mới")
                }
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(colors.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Cá nhân", tint = colors.onPrimaryContainer)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { scope.launch { viewModel.navigator.navigateTo(Routes.Record.route) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Bắt đầu hoạt động")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("Tổng quan tuần này", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(12.dp))
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            WeeklyStats(state, distanceUnit)
        }

        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hoạt động gần đây", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { scope.launch { viewModel.navigator.navigateTo(Routes.Profile.route) } }) {
                Text("Xem lịch sử")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (!state.isLoading && state.posts.isEmpty()) {
            EmptyFeed(onStart = { scope.launch { viewModel.navigator.navigateTo(Routes.Record.route) } })
        } else {
            state.posts.forEach { post ->
                PostCard(post, onWorkoutClick, distanceUnit)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
@Composable
private fun WeeklyStats(state: HomeUiState, distanceUnit: DistanceUnit) {
    val colors = MaterialTheme.colorScheme
    val statistics = state.statistics
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant) {
        Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HomeMetric(
                value = String.format(
                    Locale.US,
                    "%.1f",
                    distanceInUserUnits(statistics?.totalDistanceMeters ?: 0.0, distanceUnit)
                ),
                label = distanceUnitLabel(distanceUnit),
                modifier = Modifier.weight(1f)
            )
            HomeMetric(
                value = (statistics?.workoutCount ?: 0).toString(),
                label = "buổi",
                modifier = Modifier.weight(1f)
            )
            HomeMetric(
                value = formatDuration(statistics?.totalActiveDurationMillis ?: 0L),
                label = "thời gian",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeMetric(value: String, label: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun PostCard(item: HomePost, onWorkoutClick: (String) -> Unit, distanceUnit: DistanceUnit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val workout = item.workout
    val image = item.post.imageUrl
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onWorkoutClick(workout.id) },
        shape = RoundedCornerShape(14.dp),
        color = colors.surface
    ) {
        Column {
            if (!image.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (image.startsWith("/")) File(image) else image)
                        .build(),
                    contentDescription = "Tuyến đường",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(170.dp)
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (workout.type == WorkoutType.CYCLING) Icons.Default.DirectionsBike else Icons.Default.DirectionsRun,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    Column {
                        Text(
                            text = workout.title ?: if (workout.type == WorkoutType.CYCLING) "Buổi đạp xe" else "Buổi chạy bộ",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(formatDate(workout.startedAtEpochMillis), fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }
                item.post.caption?.takeIf { it.isNotBlank() }?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(it, color = colors.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    PostMetric(formatDistance(workout.distanceMeters, distanceUnit))
                    PostMetric(formatDuration(workout.activeDurationMillis))
                    PostMetric(
                        if (workout.type == WorkoutType.CYCLING) {
                            formatSpeed(workout.averageSpeedMetersPerSecond, distanceUnit)
                        } else {
                            formatPace(workout.averagePaceMillisPerKilometer, distanceUnit)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PostMetric(value: String) {
    Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyFeed(onStart: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Chưa có bài đăng nào", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Hoàn thành một hoạt động để chia sẻ thành tích của bạn.", color = colors.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onStart) {
                Text("Ghi hoạt động đầu tiên")
            }
        }
    }
}

private fun formatDate(epochMillis: Long): String = SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("vi"))
    .format(Date(epochMillis))

private fun formatDuration(milliseconds: Long): String {
    val totalMinutes = milliseconds / 60_000
    return if (totalMinutes >= 60) {
        String.format(Locale.US, "%dh %02dm", totalMinutes / 60, totalMinutes % 60)
    } else {
        String.format(Locale.US, "%dm", totalMinutes)
    }
}
