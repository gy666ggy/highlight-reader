/*
 * Book's Story — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2026 Acclorite
 * SPDX-License-Identifier: GPL-3.0-only
 */

package ua.acclorite.book_story.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    // 从当前颜色提取 HSV
    val initialHsv = remember(currentColor) { currentColor.toHsv() }
    var hue by remember { mutableFloatStateOf(initialHsv.hue) }
    var saturation by remember { mutableFloatStateOf(initialHsv.saturation) }
    var brightness by remember { mutableFloatStateOf(initialHsv.brightness) }

    val selectedColor = remember(hue, saturation, brightness) {
        Color.hsv(hue, saturation, brightness)
    }

    // 输入框状态
    var hexInput by remember(selectedColor) {
        val hex = selectedColor.toHex()
        mutableStateOf(hex)
    }
    var hexError by remember { mutableStateOf(false) }

    // 预置颜色面板可见性
    var showPresets by remember { mutableStateOf(false) }

    // 更新 hex 输入框
    fun updateHexFromColor(color: Color) {
        hexInput = color.toHex()
        hexError = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "选择颜色",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 颜色选择面板
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 饱和度-亮度面板
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        SaturationBrightnessPanel(
                            hue = hue,
                            saturation = saturation,
                            brightness = brightness,
                            onColorChange = { s, b ->
                                saturation = s
                                brightness = b
                                updateHexFromColor(Color.hsv(hue, s, b))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    // 色相滑块
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .fillMaxHeight()
                    ) {
                        HueSlider(
                            hue = hue,
                            onHueChange = { h ->
                                hue = h
                                updateHexFromColor(Color.hsv(h, saturation, brightness))
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 颜色预览（旧 → 新）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 旧颜色
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )

                    Text(
                        text = "→",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 新颜色
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedColor)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Hex 输入
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            // 校验并解析 hex
                            val parsed = Color.fromHex(input)
                            if (parsed != null) {
                                hexError = false
                                val hsv = parsed.toHsv()
                                hue = hsv.hue
                                saturation = hsv.saturation
                                brightness = hsv.brightness
                            } else {
                                hexError = input.length >= 7
                            }
                        },
                        isError = hexError,
                        label = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.width(120.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 预置颜色
                if (showPresets) {
                    PresetColorsGrid(
                        onColorSelect = { color ->
                            val hsv = color.toHsv()
                            hue = hsv.hue
                            saturation = hsv.saturation
                            brightness = hsv.brightness
                            updateHexFromColor(color)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showPresets = !showPresets }) {
                        Text(
                            text = if (showPresets) "隐藏预置" else "预置颜色",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { onColorSelected(selectedColor) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("确认")
                    }
                }
            }
        }
    }
}

/**
 * 饱和度-亮度选择面板
 */
@Composable
private fun SaturationBrightnessPanel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onColorChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val s = (offset.x / size.width).coerceIn(0f, 1f)
                val b = (1f - offset.y / size.height).coerceIn(0f, 1f)
                onColorChange(s, b)
            }
        }.pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val s = (change.position.x / size.width).coerceIn(0f, 1f)
                val b = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                onColorChange(s, b)
            }
        }
    ) {
        // 绘制饱和-亮度渐变
        val w = size.width
        val h = size.height

        // 水平渐变：白 → 纯色
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.White,
                1f to Color.hsv(hue, 1f, 1f)
            ),
            size = size
        )

        // 垂直渐变：透明 → 黑
        drawRect(
            brush = Brush.verticalGradient(
                0f to Color.Transparent,
                1f to Color.Black
            ),
            size = size
        )

        // 选择指示器
        val cx = saturation * w
        val cy = (1f - brightness) * h
        val indicatorRadius = 16f

        // 外圈
        drawCircle(
            color = Color.White,
            radius = indicatorRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.3f),
            radius = indicatorRadius + 1f,
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )
    }
}

/**
 * 色相滑块
 */
@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val h = (offset.y / size.height).coerceIn(0f, 1f) * 360f
                onHueChange(h)
            }
        }.pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val h = (change.position.y / size.height).coerceIn(0f, 1f) * 360f
                onHueChange(h)
            }
        }
    ) {
        // 彩虹色相条
        drawRect(
            brush = Brush.verticalGradient(
                (0..36).associate { i ->
                    val t = i / 36f
                    t to Color.hsv(t * 360f, 1f, 1f)
                }
            ),
            size = size
        )

        // 滑块指示器
        val indicatorY = (hue / 360f) * size.height
        val indicatorHeight = 8f
        drawRect(
            color = Color.White,
            topLeft = Offset(0f, indicatorY - indicatorHeight / 2),
            size = androidx.compose.ui.geometry.Size(size.width, indicatorHeight),
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(0f, indicatorY - indicatorHeight / 2 - 1f),
            size = androidx.compose.ui.geometry.Size(size.width, 1f),
        )
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(0f, indicatorY + indicatorHeight / 2),
            size = androidx.compose.ui.geometry.Size(size.width, 1f),
        )
    }
}

/**
 * 预置颜色网格
 */
@Composable
private fun PresetColorsGrid(
    onColorSelect: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetColors = listOf(
        Color(0xFFF44336) to "红",
        Color(0xFFE91E63) to "粉",
        Color(0xFF9C27B0) to "紫",
        Color(0xFF673AB7) to "深紫",
        Color(0xFF3F51B5) to "靛蓝",
        Color(0xFF2196F3) to "蓝",
        Color(0xFF03A9F4) to "浅蓝",
        Color(0xFF00BCD4) to "青",
        Color(0xFF009688) to "蓝绿",
        Color(0xFF4CAF50) to "绿",
        Color(0xFF8BC34A) to "浅绿",
        Color(0xFFCDDC39) to "黄绿",
        Color(0xFFFFEB3B) to "黄",
        Color(0xFFFFC107) to "琥珀",
        Color(0xFFFF9800) to "橙",
        Color(0xFFFF5722) to "深橙",
        Color(0xFF795548) to "棕",
        Color(0xFF607D8B) to "蓝灰",
        Color(0xFF9E9E9E) to "灰",
        Color(0xFF000000) to "黑",
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 4 行，每行 5 个
        presetColors.chunked(5).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (color, _) ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .pointerInput(color) {
                                detectTapGestures { onColorSelect(color) }
                            }
                    )
                }
            }
        }
    }
}

// ---- 扩展方法 ----

private data class HsvColor(val hue: Float, val saturation: Float, val brightness: Float)

private fun Color.toHsv(): HsvColor {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (red * 255).toInt().coerceIn(0, 255),
        (green * 255).toInt().coerceIn(0, 255),
        (blue * 255).toInt().coerceIn(0, 255),
        hsv
    )
    return HsvColor(hsv[0], hsv[1], hsv[2])
}

private fun Color.toHex(): String {
    val r = (red * 255).toInt().coerceIn(0, 255)
    val g = (green * 255).toInt().coerceIn(0, 255)
    val b = (blue * 255).toInt().coerceIn(0, 255)
    return "#${r.toString(16).uppercase().padStart(2, '0')}${g.toString(16).uppercase().padStart(2, '0')}${b.toString(16).uppercase().padStart(2, '0')}"
}

private fun Color.Companion.fromHex(hex: String): Color? {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6 && cleaned.length != 8) return null
    return try {
        val color = android.graphics.Color.parseColor("#$cleaned")
        Color(color)
    } catch (e: IllegalArgumentException) {
        null
    }
}