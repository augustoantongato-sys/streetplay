package br.com.streetplay.core.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
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
    content: @Composable BoxScope.() -> Unit,
) {
    val accent = LocalAccent.current
    val scale = if (LocalAnimations.current) SPMotion.FocusScale else 1f
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
            focusedBorder = Border(BorderStroke(3.dp, accent), shape = shape)
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
        onClick = onClick, onLongClick = onLongClick,
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

@Composable
fun SPSectionTitle(text: String, modifier: Modifier = Modifier) =
    SPText(text, SPType.Title, modifier.padding(bottom = SPSpacing.xs))

@Composable
fun SPSettingsItem(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, value: String? = null) {
    val accent = LocalAccent.current
    SPFocusIndicator(onClick = onClick, modifier = modifier.fillMaxWidth()) {
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

@Composable
fun SPLogo(modifier: Modifier = Modifier, showWordmark: Boolean = true, size: Int = 44) {
    val accent = LocalAccent.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(size.dp).clip(RoundedCornerShape(size.dp / 4))
                .background(Brush.linearGradient(listOf(SPColors.OrangeSecondary, accent))),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = this.size.width
                val h = this.size.height
                val p = Path().apply {
                    moveTo(w * 0.38f, h * 0.27f)
                    lineTo(w * 0.38f, h * 0.73f)
                    lineTo(w * 0.74f, h * 0.5f)
                    close()
                }
                drawPath(p, SPColors.Background)
            }
        }
        if (showWordmark) {
            Spacer(Modifier.width(SPSpacing.sm + 2.dp))
            SPText("STREET", SPType.Title)
            Spacer(Modifier.width(6.dp))
            SPText("PLAY", SPType.Title, color = accent)
        }
    }
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
    val date = remember(now) { SimpleDateFormat("EEEE, d 'de' MMMM", locale).format(now) }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SPLogo()
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            SPText(time, SPType.Title)
            SPText(date, SPType.Caption)
        }
        Spacer(Modifier.width(SPSpacing.lg))
        SPText(net, SPType.Label, color = SPColors.TextSecondary)
        Spacer(Modifier.width(SPSpacing.md))
        SPButton("Configurações", onSettings)
    }
}

data class SPSidebarItem(val id: String, val glyph: String, val label: String)

/** Menu lateral: recolhido (só números) e expande com animação quando recebe foco. */
@Composable
fun SPSidebar(items: List<SPSidebarItem>, selectedId: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val accent = LocalAccent.current
    var expanded by remember { mutableStateOf(false) }
    val ms = if (LocalAnimations.current) SPMotion.Normal else 0
    val width by animateDpAsState(if (expanded) 270.dp else 88.dp, tween(ms), label = "sidebarWidth")
    Column(
        modifier.width(width).fillMaxHeight().background(SPColors.BackgroundSecondary)
            .onFocusChanged { expanded = it.hasFocus }.focusGroup()
            .padding(horizontal = 14.dp, vertical = SPSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(SPSpacing.sm),
    ) {
        SPLogo(showWordmark = expanded, size = 48, modifier = Modifier.padding(start = 4.dp, bottom = SPSpacing.lg))
        items.forEach { item ->
            val selected = item.id == selectedId
            SPFocusIndicator(
                onClick = { onSelect(item.id) }, modifier = Modifier.fillMaxWidth(),
                container = if (selected) accent.copy(alpha = 0.18f) else SPColors.BackgroundSecondary,
                focusedContainer = SPColors.CardFocused,
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        SPText(item.glyph, SPType.Label, color = if (selected) accent else SPColors.TextSecondary)
                    }
                    if (expanded) {
                        Spacer(Modifier.width(10.dp))
                        SPText(item.label, SPType.Body, color = if (selected) accent else SPColors.TextPrimary, maxLines = 1)
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
