package com.example.android_proj.shop

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

/** Стрічка карток товарів: 1 або 2 картки по ширині екрана, фото — з інтернету. */
@Composable
fun ProductFeed() {
    var columns by rememberSaveable { mutableIntStateOf(2) }
    val products = Catalog.products

    Column(Modifier.fillMaxSize().background(Palette.Background).statusBarsPadding()) {
        // шапка: заголовок + перемикач 1 / 2 колонки
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Товари", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("${products.size} позицій", fontSize = 13.sp, color = Palette.Muted)
            }
            ColumnsToggle(columns) { columns = it }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(products, key = { it.id }) { product ->
                ProductCard(product, wide = columns == 1, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, wide: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier.clip(shape).background(Palette.Surface2).border(1.dp, Palette.Border, shape).padding(8.dp),
    ) {
        Box {
            // фото товару з інтернету (Coil)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(product.photo).crossfade(true).build(),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(if (wide) 16f / 10f else 1f)
                    .clip(RoundedCornerShape(16.dp)).background(Palette.Surface3),
            )
            product.discount?.let {
                Text(
                    "−$it%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Palette.OnPrimary,
                    modifier = Modifier.padding(8.dp).clip(CircleShape).background(Palette.Primary)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        Column(Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 4.dp)) {
            Text(
                product.category.label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp, color = Palette.Muted,
            )
            Text(
                product.name, fontSize = if (wide) 20.sp else 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (wide) {
                Spacer(Modifier.height(4.dp))
                Text(product.description, fontSize = 13.sp, lineHeight = 18.sp, color = Palette.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = Palette.Gold)
                Text(" ${product.rating}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("  (${product.reviews})", fontSize = 12.sp, color = Palette.Muted)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price(product.price), fontSize = if (wide) 20.sp else 16.sp, fontWeight = FontWeight.ExtraBold)
                product.oldPrice?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(price(it), fontSize = 11.sp, color = Palette.Muted, textDecoration = TextDecoration.LineThrough)
                }
            }
        }
    }
}

/** Перемикач «1 картка / 2 картки в ряд». */
@Composable
private fun ColumnsToggle(columns: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Palette.Surface3).border(1.dp, Palette.Border, CircleShape).padding(4.dp),
    ) {
        listOf(1, 2).forEach { n ->
            val selected = n == columns
            val bg by animateColorAsState(if (selected) Palette.Primary else Color.Transparent, label = "toggle")
            val fg = if (selected) Palette.OnPrimary else Palette.Muted
            Box(
                Modifier.size(44.dp, 36.dp).clip(CircleShape).background(bg).clickable { onChange(n) },
                contentAlignment = Alignment.Center,
            ) {
                LayoutIcon(n, fg)
            }
        }
    }
}

/** Іконка розкладки, намальована на Canvas: одна широка картка або сітка 2×2. */
@Composable
private fun LayoutIcon(columns: Int, color: Color) {
    Canvas(Modifier.size(18.dp)) {
        val gap = 2.5.dp.toPx()
        val r = CornerRadius(2.dp.toPx())
        if (columns == 1) {
            val h = (size.height - gap) / 2
            repeat(2) { i -> drawRoundRect(color, Offset(0f, i * (h + gap)), Size(size.width, h), r) }
        } else {
            val s = (size.width - gap) / 2
            repeat(4) { i -> drawRoundRect(color, Offset((i % 2) * (s + gap), (i / 2) * (s + gap)), Size(s, s), r) }
        }
    }
}
