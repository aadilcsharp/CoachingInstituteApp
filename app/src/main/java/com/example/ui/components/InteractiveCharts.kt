package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.CurrencyFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class ChartPoint(
    val label: String,
    val value: Double,
    val secondaryValue: Double = 0.0
)

data class DonutSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun InteractiveLineChart(
    points: List<ChartPoint>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (points.isEmpty() || points.all { it.value == 0.0 }) {
        Box(
            modifier = modifier.height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending trend data available yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(points.lastIndex) }
    val maxValue = remember(points) { points.maxOf { it.value }.coerceAtLeast(1.0) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Active Point Tooltip Bar
        val active = points.getOrNull(selectedIndex) ?: points.last()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = active.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = lineColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = CurrencyFormatter.format(active.value, currencySymbol),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = lineColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            val sectionWidth = size.width / points.size.coerceAtLeast(1)
                            val index = (offset.x / sectionWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIndex = index
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 30f
                val chartHeight = height - paddingBottom
                val stepX = if (points.size > 1) width / (points.size - 1) else width

                // Grid horizontal guideline
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.4f),
                    start = Offset(0f, chartHeight / 2),
                    end = Offset(width, chartHeight / 2),
                    strokeWidth = 1.dp.toPx()
                )

                val linePath = Path()
                val fillPath = Path()

                val coords = points.mapIndexed { idx, point ->
                    val x = if (points.size > 1) idx * stepX else width / 2
                    val ratio = (point.value / maxValue).toFloat().coerceIn(0f, 1f)
                    val y = chartHeight - (ratio * (chartHeight - 20f)) - 10f
                    Offset(x, y)
                }

                coords.forEachIndexed { i, offset ->
                    if (i == 0) {
                        linePath.moveTo(offset.x, offset.y)
                        fillPath.moveTo(offset.x, chartHeight)
                        fillPath.lineTo(offset.x, offset.y)
                    } else {
                        // Smooth cubic bezier
                        val prev = coords[i - 1]
                        val cx = (prev.x + offset.x) / 2
                        linePath.cubicTo(cx, prev.y, cx, offset.y, offset.x, offset.y)
                        fillPath.cubicTo(cx, prev.y, cx, offset.y, offset.x, offset.y)
                    }
                }

                if (coords.isNotEmpty()) {
                    fillPath.lineTo(coords.last().x, chartHeight)
                    fillPath.close()

                    // Draw area gradient
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.35f),
                                lineColor.copy(alpha = 0.0f)
                            ),
                            startY = 0f,
                            endY = chartHeight
                        )
                    )

                    // Draw main line
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw circles and selected indicator
                    coords.forEachIndexed { i, offset ->
                        val isSelected = i == selectedIndex
                        if (isSelected) {
                            // Vertical dashed line
                            drawLine(
                                color = lineColor.copy(alpha = 0.5f),
                                start = Offset(offset.x, 0f),
                                end = Offset(offset.x, chartHeight),
                                strokeWidth = 1.5.dp.toPx()
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 7.dp.toPx(),
                                center = offset
                            )
                            drawCircle(
                                color = lineColor,
                                radius = 5.dp.toPx(),
                                center = offset
                            )
                        } else {
                            drawCircle(
                                color = lineColor,
                                radius = 3.dp.toPx(),
                                center = offset
                            )
                        }
                    }
                }
            }
        }

        // Bottom X-axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEachIndexed { idx, point ->
                val isSelected = idx == selectedIndex
                Text(
                    text = point.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) lineColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InteractiveBarChart(
    bars: List<ChartPoint>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty() || bars.all { it.value == 0.0 && it.secondaryValue == 0.0 }) {
        Box(
            modifier = modifier.height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No monthly comparison data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(bars.lastIndex) }
    val maxVal = remember(bars) {
        bars.maxOf { maxOf(it.value, it.secondaryValue) }.coerceAtLeast(1.0)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend & Selection info
        val active = bars.getOrNull(selectedIndex) ?: bars.last()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).background(primaryColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Billed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(8.dp).background(secondaryColor, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Collected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Text(
                text = "${active.label}: Billed ${CurrencyFormatter.formatCompact(active.value, currencySymbol)} | Paid ${CurrencyFormatter.formatCompact(active.secondaryValue, currencySymbol)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(bars) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / bars.size.coerceAtLeast(1)
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, bars.lastIndex)
                            selectedIndex = idx
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingBottom = 20f
                val chartHeight = height - paddingBottom
                val slotWidth = width / bars.size

                val barWidth = (slotWidth * 0.28f).coerceAtLeast(6f)
                val spacing = 4f

                bars.forEachIndexed { i, bar ->
                    val isSelected = i == selectedIndex
                    val slotCenterX = (i * slotWidth) + (slotWidth / 2f)

                    // Selection highlight background
                    if (isSelected) {
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.08f),
                            topLeft = Offset(i * slotWidth, 0f),
                            size = Size(slotWidth, chartHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Bar 1: Billed (Primary)
                    val h1 = ((bar.value / maxVal).toFloat() * (chartHeight - 10f)).coerceAtLeast(2f)
                    val y1 = chartHeight - h1
                    val x1 = slotCenterX - barWidth - (spacing / 2f)
                    drawRoundRect(
                        color = if (isSelected) primaryColor else primaryColor.copy(alpha = 0.75f),
                        topLeft = Offset(x1, y1),
                        size = Size(barWidth, h1),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Bar 2: Collected (Secondary Emerald)
                    val h2 = ((bar.secondaryValue / maxVal).toFloat() * (chartHeight - 10f)).coerceAtLeast(2f)
                    val y2 = chartHeight - h2
                    val x2 = slotCenterX + (spacing / 2f)
                    drawRoundRect(
                        color = if (isSelected) secondaryColor else secondaryColor.copy(alpha = 0.75f),
                        topLeft = Offset(x2, y2),
                        size = Size(barWidth, h2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // Baseline
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, chartHeight),
                    end = Offset(width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        // X Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEachIndexed { idx, bar ->
                val isSelected = idx == selectedIndex
                Text(
                    text = bar.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveDonutChart(
    slices: List<DonutSlice>,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    val total = remember(slices) { slices.sumOf { it.value } }

    if (slices.isEmpty() || total <= 0.0) {
        Box(
            modifier = modifier.height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No category expense data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val activeSlice = slices.getOrNull(selectedIndex) ?: slices.first()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(190.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchVec = offset - center
                            var angle = Math.toDegrees(atan2(touchVec.y.toDouble(), touchVec.x.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var startA = -90f
                            slices.forEachIndexed { idx, s ->
                                val sweep = (s.value / total * 360.0).toFloat()
                                val normalizedStart = (startA % 360 + 360) % 360
                                val endA = normalizedStart + sweep

                                if (angle >= normalizedStart && angle <= endA) {
                                    selectedIndex = idx
                                    return@detectTapGestures
                                }
                                startA += sweep
                            }
                        }
                    }
            ) {
                val strokeWidth = 24.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                var startAngle = -90f
                slices.forEachIndexed { idx, slice ->
                    val sweepAngle = (slice.value / total * 360.0).toFloat()
                    val isSelected = idx == selectedIndex

                    drawArc(
                        color = if (isSelected) slice.color else slice.color.copy(alpha = 0.8f),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle - 2f, // subtle gap
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Center details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = activeSlice.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatCompact(activeSlice.value, currencySymbol),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val percentage = (activeSlice.value / total * 100).toInt()
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeSlice.color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend Badges
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.forEachIndexed { index, slice ->
                val isSelected = index == selectedIndex
                Surface(
                    onClick = { selectedIndex = index },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) slice.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(slice.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = slice.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val pct = (slice.value / total * 100).toInt()
                        Text(
                            text = "($pct%)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
