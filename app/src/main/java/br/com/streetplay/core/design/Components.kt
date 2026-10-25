package br.com.streetplay.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import br.com.streetplay.R
import br.com.streetplay.core.common.LocalIconLoader
import br.com.streetplay.core.common.networkLabel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SPText(
    text: String,
    style: TextStyle = SPType.Body,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
) = BasicText(
    text = text, modifier = modifier,
    style = if (color == Color.Unspecified) style else style.copy(color = color),
    maxLines = maxLines, overflow = TextOverflow.Ellipsis,
)

/** Base de todo elemento focável: foco sempre visível (borda na cor de destaque + escala). */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SPFocusIndicator(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(SPRadius.md),
    container: Color = SPColors.Card,
    focusedContainer: Color = SPColors.CardFocused,
    focusScale: Float = SPMotion.FocusScale,
    idleBorder: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val accent = LocalAccent.current
    val scale = if (LocalAnimations.current) focusScale else 1f
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = shape, focusedShape = shape, pressedShape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container, contentColor = SPColors.TextPrimary,
            focusedContainerColor = focusedContainer, focusedContentColor = SPColors.TextPrimary,
            pressedContainerColor = focusedContainer, pressedContentColor = SPColors.TextPrimary,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = scale),
        border = ClickableSurfaceDefaults.border(
            border = if (idleBorder != null) Border(BorderStroke(1.dp, idleBorder), shape = shape) else Border.None,
            focusedBorder = Border(BorderStroke(3.dp, accent), shape = shape),
        ),
        content = content,
    )
}

@Composable
fun SPCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(SPRadius.lg)).background(SPColors.Card).padding(SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.sm), content = content,
    )
}

@Composable
fun SPButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    val accent = LocalAccent.current
    SPFocusIndicator(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(50),
        container = if (selected) accent.copy(alpha = 0.22f) else SPColors.Card,
    ) {
        Box(Modifier.padding(horizontal = 22.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            SPText(text, SPType.Label, color = if (selected) accent else SPColors.TextPrimary, maxLines = 1)
        }
    }
}

/** Cartão grande (grades de Aplicativos e Favoritos). */
@Composable
fun SPAppCard(
    label: String,
    packageName: String?,
    installed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    large: Boolean = false,
    subtitle: String? = null,
) {
    val loader = LocalIconLoader.current
    val accent = LocalAccent.current
    val icon by produceState<ImageBitmap?>(null, packageName, installed) {
        value = if (packageName != null && installed) loader(packageName) else null
    }
    SPFocusIndicator(
        onClick = onClick, onLongClick = onLongClick, idleBorder = SPColors.Border,
        modifier = modifier.size(if (large) 220.dp else 160.dp, if (large) 150.dp else 128.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(SPSpacing.md).alpha(if (installed) 1f else 0.45f),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(if (large) 64.dp else 52.dp), contentAlignment = Alignment.Center) {
                val bmp = icon
                if (bmp != null) Image(bitmap = bmp, contentDescription = null, modifier = Modifier.fillMaxSize())
                else SPText(label.take(1).uppercase(), SPType.Display, color = accent)
            }
            Spacer(Modifier.height(10.dp))
            SPText(label, SPType.Label, maxLines = 1)
            if (subtitle != null) SPText(subtitle, SPType.Caption, maxLines = 1)
        }
    }
}

/** Cartão compacto das fileiras da Home: ícone real do app instalado, ou nome na cor da marca. */
@Composable
fun SPTile(
    label: String,
    packageName: String?,
    installed: Boolean,
    brand: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val loader = LocalIconLoader.current
    val icon by produceState<ImageBitmap?>(null, packageName, installed) {
        value = if (packageName != null && installed) loader(packageName) else null
    }
    SPFocusIndicator(
        onClick = onClick, onLongClick = onLongClick, modifier = modifier,
        container = SPColors.TileBase, focusedContainer = SPColors.TileFocused,
        focusScale = 1.06f, idleBorder = SPColors.Border,
    ) {
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(brand.copy(alpha = 0.30f), brand.copy(alpha = 0.05f)))),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.padding(horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val bmp = icon
                if (bmp != null) {
                    Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(3.dp))
                    SPText(label, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White), maxLines = 1)
                } else {
                    SPText(label, TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = lerp(brand, Color.White, 0.35f), maxLines = 1)
                    SPText("Não instalado", TextStyle(fontSize = 10.sp, color = SPColors.TextSecondary), maxLines = 1)
                }
            }
        }
    }
}

/** Cartão de ação (ex.: "Mais Apps"). */
@Composable
fun SPActionTile(label: String, icon: SPIconKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SPFocusIndicator(
        onClick = onClick, modifier = modifier,
        container = SPColors.TileBase, focusedContainer = SPColors.TileFocused,
        focusScale = 1.06f, idleBorder = SPColors.Border,
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            SPIcon(icon, size = 26.dp)
            Spacer(Modifier.height(4.dp))
            SPText(label, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White), maxLines = 1)
        }
    }
}

@Composable
fun SPSectionTitle(text: String, modifier: Modifier = Modifier) =
    SPText(text, SPType.Title, modifier.padding(bottom = SPSpacing.xs))

@Composable
fun SPSettingsItem(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, value: String? = null) {
    val accent = LocalAccent.current
    SPFocusIndicator(onClick = onClick, modifier = modifier.fillMaxWidth(), idleBorder = SPColors.Border) {
        Row(Modifier.padding(horizontal = SPSpacing.lg, vertical = SPSpacing.md), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SPText(title, SPType.Body)
                if (subtitle != null) SPText(subtitle, SPType.Caption)
            }
            if (value != null) SPText(value, SPType.Label, color = accent)
        }
    }
}

@Composable
fun SPEmptyState(title: String, text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(SPSpacing.sm)) {
            SPText(title, SPType.Title)
            SPText(text, SPType.Body, color = SPColors.TextSecondary)
        }
    }
}

@Composable
fun SPSearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(SPColors.Card)
            .padding(horizontal = SPSpacing.lg, vertical = 12.dp),
    ) {
        if (value.isEmpty()) SPText(placeholder, SPType.Body, color = SPColors.TextSecondary)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = SPType.Body, cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        )
        if (focused) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(2.dp).background(accent))
    }
}

/** Logotipo oficial STREET PLAY (arte do designer). */
@Composable
fun SPLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_sidebar), contentDescription = "Street Play",
        modifier = modifier, contentScale = ContentScale.Fit, alignment = Alignment.CenterStart,
    )
}

@Composable
fun SPHeader(onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(Date()) }
    var net by remember { mutableStateOf(context.networkLabel()) }
    LaunchedEffect(Unit) {
        while (true) { now = Date(); net = context.networkLabel(); delay(5_000) }
    }
    val locale = remember { Locale("pt", "BR") }
    val time = remember(now) { SimpleDateFormat("HH:mm", locale).format(now) }
    val date = remember(now) { SimpleDateFormat("d 'de' MMM 'de' yyyy", locale).format(now).replace(".", "") }
    val connected = net != "Sem rede" && net != "Rede indisponível"
    Row(modifier.fillMaxWidth().height(68.dp).padding(end = 23.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.weight(1f))
        SPIcon(SPIconKind.WIFI, tint = if (connected) Color.White else SPColors.TextSecondary, size = 26.dp)
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(1.dp).height(26.dp).background(SPColors.Border))
        Spacer(Modifier.width(10.dp))
        SPFocusIndicator(
            onClick = onSettings, shape = CircleShape,
            container = Color.Transparent, focusedContainer = SPColors.CardFocused, focusScale = 1.1f,
        ) { Box(Modifier.padding(8.dp)) { SPIcon(SPIconKind.SETTINGS, size = 24.dp) } }
        Spacer(Modifier.width(18.dp))
        Column(horizontalAlignment = Alignment.End) {
            SPText(time, TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White))
            SPText(date, TextStyle(fontSize = 11.sp, color = SPColors.TextSecondary))
        }
    }
}

data class SPSidebarItem(val id: String, val icon: SPIconKind, val label: String)

/** Menu lateral fixo: item selecionado em laranja cheio; foco com borda. */
@Composable
fun SPSidebar(items: List<SPSidebarItem>, selectedId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    Box(modifier.width(237.dp).fillMaxHeight().background(SPColors.Sidebar)) {
        // detalhes laranja no canto inferior esquerdo (como no design)
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawPath(Path().apply {
                moveTo(0f, h - 190.dp.toPx()); lineTo(150.dp.toPx(), h); lineTo(105.dp.toPx(), h); lineTo(0f, h - 120.dp.toPx()); close()
            }, accent.copy(alpha = 0.30f))
            drawPath(Path().apply {
                moveTo(0f, h - 110.dp.toPx()); lineTo(95.dp.toPx(), h); lineTo(0f, h); close()
            }, Brush.linearGradient(listOf(SPColors.OrangeSecondary, accent)))
            drawRect(accent.copy(alpha = 0.18f), topLeft = androidx.compose.ui.geometry.Offset(w - 1.dp.toPx(), 0f),
                size = androidx.compose.ui.geometry.Size(1.dp.toPx(), h))
        }
        Column(Modifier.fillMaxSize().focusGroup().padding(horizontal = 14.dp)) {
            SPLogo(Modifier.padding(start = 6.dp, top = 14.dp, bottom = 14.dp).fillMaxWidth().height(66.dp))
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { item ->
                    val selected = item.id == selectedId
                    SPFocusIndicator(
                        onClick = { onSelect(item.id) }, modifier = Modifier.fillMaxWidth(),
                        container = if (selected) accent else Color.Transparent,
                        focusedContainer = if (selected) accent else SPColors.CardFocused,
                        focusScale = 1.03f,
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            SPIcon(item.icon, tint = if (selected) Color.White else Color(0xFFE6E6E6), size = 23.dp)
                            Spacer(Modifier.width(16.dp))
                            SPText(item.label, SPType.Body.copy(fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SPDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val focus = remember { FocusRequester() }
        Column(
            Modifier.width(460.dp).clip(RoundedCornerShape(SPRadius.lg)).background(SPColors.BackgroundSecondary)
                .padding(SPSpacing.lg).focusRequester(focus).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SPText(title, SPType.Title)
            content()
        }
        LaunchedEffect(Unit) { delay(60); runCatching { focus.requestFocus() } }
    }
}
