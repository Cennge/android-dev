package com.example.android_proj.shop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

private val Gradient = Brush.horizontalGradient(listOf(Palette.Primary, Palette.Pink))

/** Додаток «Список бажань»: кожен пункт — фото з інтернету + текстовий опис. */
@Composable
fun WishlistScreen() {
    val wishes = Wishlist.wishes
    var liked by rememberSaveable { mutableStateOf(setOf(1, 4)) }
    var done by rememberSaveable { mutableStateOf(setOf(4)) }

    LazyColumn(
        Modifier.fillMaxSize().background(Palette.Background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Header(wishes, done) }
        items(wishes, key = { it.id }) { wish ->
            WishCard(
                wish,
                liked = wish.id in liked,
                done = wish.id in done,
                onLike = { liked = liked.toggle(wish.id) },
                onDone = { done = done.toggle(wish.id) },
            )
        }
        item { Spacer(Modifier.navigationBarsPadding()) }
    }
}

private fun Set<Int>.toggle(id: Int) = if (id in this) this - id else this + id

@Composable
private fun Header(wishes: List<Wish>, done: Set<Int>) {
    val total = wishes.sumOf { it.price }
    val saved = wishes.sumOf { if (it.id in done) it.price else it.saved }
    val progress by animateFloatAsState(saved.toFloat() / total, spring(), label = "total")

    Column(Modifier.statusBarsPadding().padding(top = 16.dp)) {
        Text("МОЇ МРІЇ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = Palette.PrimaryText)
        Text(
            buildAnnotatedString {
                append("Список ")
                withStyle(SpanStyle(brush = Gradient)) { append("бажань") }
            },
            fontSize = 34.sp, fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))

        // підсумкова картка: скільки вже накопичено
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape)
                .background(Brush.linearGradient(listOf(Palette.Surface3, Palette.Surface2)))
                .border(1.dp, Palette.Border, shape).padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("Накопичено", fontSize = 13.sp, color = Palette.Muted)
                    Text(price(saved), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text("з ${price(total)}", fontSize = 13.sp, color = Palette.Muted)
            }
            Spacer(Modifier.height(14.dp))
            ProgressBar(progress, 10.dp)
            Spacer(Modifier.height(14.dp))
            Row {
                Stat("${wishes.size}", "бажань", Modifier.weight(1f))
                Stat("${done.size}", "виконано", Modifier.weight(1f))
                Stat("${(progress * 100).toInt()}%", "до мети", Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp, color = Palette.Muted)
    }
}

@Composable
private fun ProgressBar(progress: Float, height: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().height(height).clip(CircleShape).background(Palette.Surface4)) {
        Box(Modifier.fillMaxWidth(progress).height(height).clip(CircleShape).background(Gradient))
    }
}

@Composable
private fun WishCard(wish: Wish, liked: Boolean, done: Boolean, onLike: () -> Unit, onDone: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val progress by animateFloatAsState(if (done) 1f else wish.progress, spring(), label = "progress")
    val heartScale by animateFloatAsState(if (liked) 1.15f else 1f, spring(dampingRatio = 0.35f), label = "heart")

    Column(Modifier.fillMaxWidth().clip(shape).background(Palette.Surface2).border(1.dp, Palette.Border, shape)) {
        Box(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            // фото з інтернету (Coil); виконане бажання стає чорно-білим
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(wish.photo).crossfade(true).build(),
                contentDescription = wish.title,
                contentScale = ContentScale.Crop,
                colorFilter = if (done) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                modifier = Modifier.fillMaxSize().background(Palette.Surface3),
            )
            // затемнення знизу, щоб читався заголовок
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Palette.Surface2),
                ),
            )
            Chip(wish.priority.label, wish.priority.color, Modifier.align(Alignment.TopStart).padding(14.dp))
            Box(
                Modifier.align(Alignment.TopEnd).padding(12.dp).size(44.dp).scale(heartScale)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onLike),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Улюблене",
                    tint = if (liked) Palette.Pink else Color.White, modifier = Modifier.size(22.dp),
                )
            }
            Text(
                "#${wish.id}", fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp),
            )
            Text(
                wish.title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp),
            )
        }

        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp)) {
            Text(wish.description, fontSize = 14.sp, lineHeight = 20.sp, color = Palette.Muted)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price(wish.price), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text(
                    if (done) "Мрія здійснилась" else "ще ${price(wish.price - wish.saved)}",
                    fontSize = 12.sp, color = if (done) Palette.Green else Palette.Muted,
                )
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(progress, 6.dp)
            Spacer(Modifier.height(16.dp))
            DoneButton(done, onDone)
        }
    }
}

@Composable
private fun Chip(text: String, color: Color, modifier: Modifier) {
    Row(
        modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
private fun DoneButton(done: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (done) Palette.Green.copy(alpha = 0.15f) else Palette.Surface3, label = "done")
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(shape).background(bg)
            .border(1.dp, if (done) Palette.Green.copy(alpha = 0.5f) else Palette.Border, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (done) {
            Icon(Icons.Filled.Check, null, tint = Palette.Green, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            if (done) "Виконано" else "Позначити виконаним",
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (done) Palette.Green else Palette.Foreground,
        )
    }
}
