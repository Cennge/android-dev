package com.example.android_proj.shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Головна", Icons.Outlined.Home),
    FAVORITES("Обране", Icons.Outlined.FavoriteBorder),
    CART("Кошик", Icons.Outlined.ShoppingCart),
}

// =====================================================================================  screen

@Composable
fun ShopApp() {
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var category by rememberSaveable { mutableStateOf(Category.ALL) }
    var query by rememberSaveable { mutableStateOf("") }
    val favorites = remember { mutableStateListOf(2, 9) }
    val cart = remember { mutableStateMapOf<Int, Int>() }
    var opened by remember { mutableStateOf<Product?>(null) }

    val cartCount = cart.values.sum()
    val cartTotal = cart.entries.sumOf { (id, n) -> Catalog.products.first { it.id == id }.price * n }
    val browsing = query.isBlank() && tab == Tab.HOME
    val shown = Catalog.products.filter { p ->
        (category == Category.ALL || p.category == category) &&
            p.name.contains(query.trim(), ignoreCase = true) &&
            when (tab) {
                Tab.HOME -> true
                Tab.FAVORITES -> p.id in favorites
                Tab.CART -> p.id in cart
            }
    }
    val addToCart = { p: Product, n: Int -> cart[p.id] = (cart[p.id] ?: 0) + n }
    val toggleFavorite = { p: Product -> if (!favorites.remove(p.id)) favorites.add(p.id) }

    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = top + 12.dp, bottom = 130.dp),
            verticalItemSpacing = 14.dp,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) { Header(cartCount) }
            item(key = "search", span = StaggeredGridItemSpan.FullLine) { SearchBar(query) { query = it } }
            if (browsing) {
                item(key = "promo", span = StaggeredGridItemSpan.FullLine) { PromoPager() }
            }
            item(key = "categories", span = StaggeredGridItemSpan.FullLine) {
                CategoryRow(category) { category = it }
            }
            if (browsing && category == Category.ALL) {
                item(key = "hot", span = StaggeredGridItemSpan.FullLine) { HotSection { opened = it } }
            }
            if (tab == Tab.CART && cartCount > 0) {
                item(key = "summary", span = StaggeredGridItemSpan.FullLine) {
                    CartSummary(cartCount, cartTotal) { cart.clear() }
                }
            }
            item(key = "title", span = StaggeredGridItemSpan.FullLine) {
                SectionTitle(
                    when (tab) {
                        Tab.HOME -> "Усі товари"
                        Tab.FAVORITES -> "Обране"
                        Tab.CART -> "У кошику"
                    },
                    "${shown.size} шт",
                )
            }
            if (shown.isEmpty()) {
                item(key = "empty", span = StaggeredGridItemSpan.FullLine) { EmptyState(tab) }
            }
            itemsIndexed(shown, key = { _, p -> p.id }) { index, p ->
                ProductCard(
                    product = p,
                    tall = index % 4 == 0 || index % 4 == 3,
                    favorite = p.id in favorites,
                    inCart = cart[p.id] ?: 0,
                    onFavorite = { toggleFavorite(p) },
                    onAdd = { addToCart(p, 1) },
                    onOpen = { opened = p },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        // fades the list under the status bar
        Box(
            Modifier.fillMaxWidth().height(top + 20.dp)
                .background(Brush.verticalGradient(0f to Palette.Background, 0.65f to Palette.Background, 1f to Palette.Background.copy(alpha = 0f))),
        )

        BottomBar(
            current = tab,
            cartCount = cartCount,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
        )

        opened?.let { p ->
            ProductSheet(
                product = p,
                favorite = p.id in favorites,
                onFavorite = { toggleFavorite(p) },
                onAdd = { n -> addToCart(p, n) },
                onDismiss = { opened = null },
            )
        }
    }
}

// =====================================================================================  header

@Composable
private fun Header(cartCount: Int) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Palette.Primary, Palette.Violet))),
                contentAlignment = Alignment.Center,
            ) {
                Text("V", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Доброго дня 👋", fontSize = 13.sp, color = Palette.Muted)
                Text("Vibe Store", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            RoundIcon(Icons.Outlined.Notifications, dot = true)
            Spacer(Modifier.width(8.dp))
            Box {
                RoundIcon(Icons.Outlined.ShoppingCart)
                CountBadge(cartCount, Modifier.align(Alignment.TopEnd))
            }
        }
        Spacer(Modifier.height(22.dp))
        Text(
            buildAnnotatedString {
                append("Знайди свій\n")
                withStyle(SpanStyle(brush = Brush.linearGradient(listOf(Palette.PrimaryLight, Palette.Pink, Palette.Violet)))) {
                    append("ідеальний стиль")
                }
            },
            fontSize = 34.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.8).sp,
        )
    }
}

@Composable
private fun RoundIcon(icon: ImageVector, dot: Boolean = false) {
    Box(
        Modifier.size(46.dp).clip(CircleShape).background(Palette.Surface3).border(1.dp, Palette.Border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = Palette.Foreground)
        if (dot) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 11.dp, end = 12.dp).size(8.dp)
                    .clip(CircleShape).background(Palette.Primary),
            )
        }
    }
}

/** Animated counter: the new number slides in from below. */
@Composable
private fun CountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    ring: Color = Palette.Background,
    inverted: Boolean = false,
) {
    AnimatedVisibility(count > 0, modifier, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(if (inverted) Palette.OnPrimary else Palette.Primary)
                .border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = count,
                transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                label = "badge",
            ) { n ->
                Text("$n", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (inverted) Palette.PrimaryText else Palette.OnPrimary)
            }
        }
    }
}

// =====================================================================================  search

@Composable
private fun SearchBar(query: String, onQuery: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val shape = RoundedCornerShape(18.dp)
        Row(
            Modifier.weight(1f).height(54.dp).clip(shape).background(Palette.Surface3)
                .border(1.dp, Palette.Border, shape).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = Palette.Muted)
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(fontFamily = Inter, fontSize = 15.sp, color = Palette.Foreground),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Palette.Primary),
                modifier = Modifier.weight(1f),
                decorationBox = { field ->
                    if (query.isEmpty()) Text("Кросівки, навушники…", fontSize = 15.sp, color = Palette.Muted)
                    field()
                },
            )
            AnimatedVisibility(query.isNotEmpty()) {
                Icon(
                    Icons.Filled.Close, "Очистити", tint = Palette.Muted,
                    modifier = Modifier.clip(CircleShape).clickable { onQuery("") }.padding(4.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Palette.PrimaryLight, Palette.Primary))),
            contentAlignment = Alignment.Center,
        ) {
            FilterIcon(Palette.OnPrimary)
        }
    }
}

/** "Sliders" icon drawn on a Canvas: three lines with knobs at different positions. */
@Composable
private fun FilterIcon(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val knobs = listOf(0.3f, 0.7f, 0.45f)
        knobs.forEachIndexed { i, k ->
            val y = size.height * (0.2f + i * 0.3f)
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Palette.Primary, radius = 4.dp.toPx(), center = Offset(size.width * k, y))
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(size.width * k, y), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        }
    }
}

// =====================================================================================  promo carousel

@Composable
private fun PromoPager() {
    val promos = Catalog.promos
    val pager = rememberPagerState { promos.size }

    // auto-scroll every 4 s; restarts after the user swipes
    LaunchedEffect(pager.settledPage) {
        delay(4000)
        pager.animateScrollToPage((pager.settledPage + 1) % promos.size, animationSpec = tween(700, easing = FastOutSlowInEasing))
    }

    Column {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.bleed(16.dp),
        ) { page ->
            val offset = abs((pager.currentPage - page) + pager.currentPageOffsetFraction).coerceIn(0f, 1f)
            PromoCard(
                promos[page],
                Modifier.graphicsLayer {
                    // pages shrink and fade a little while they slide away
                    val scale = 1f - 0.08f * offset
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - 0.35f * offset
                },
                imageShift = offset,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(promos.size) { i ->
                val selected = i == pager.currentPage
                val width by animateDpAsState(if (selected) 24.dp else 6.dp, spring(dampingRatio = 0.6f), label = "dot")
                val color by animateColorAsState(if (selected) Palette.Primary else Palette.Surface4, label = "dotColor")
                Box(Modifier.height(6.dp).width(width).clip(CircleShape).background(color))
            }
        }
    }
}

@Composable
private fun PromoCard(promo: Promo, modifier: Modifier, imageShift: Float) {
    Box(
        modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(promo.colors)),
    ) {
        // decorative light circles
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.10f), radius = size.height * 0.95f, center = Offset(size.width * 0.92f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.height * 0.45f, center = Offset(size.width * 0.55f, size.height * 1.1f))
        }
        Photo(
            promo.photo,
            Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).size(132.dp)
                .graphicsLayer {
                    rotationZ = 8f - 14f * imageShift // parallax: the photo tilts while swiping
                    translationX = 60f * imageShift
                }
                .shadow(20.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp)),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 22.dp).fillMaxWidth(0.52f)) {
            Text(promo.tag.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, color = Color.White.copy(alpha = 0.8f))
            Text(promo.title, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = (-0.5).sp)
            Text(promo.subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f), maxLines = 2)
            Spacer(Modifier.height(12.dp))
            Text(
                "Купити  →",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16181C),
                modifier = Modifier.clip(CircleShape).background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

// =====================================================================================  categories

@Composable
private fun CategoryRow(current: Category, onSelect: (Category) -> Unit) {
    LazyRow(
        modifier = Modifier.bleed(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Category.entries) { c ->
            val selected = c == current
            val bg by animateColorAsState(if (selected) Palette.Primary else Palette.Surface3, tween(250), label = "chipBg")
            val fg by animateColorAsState(if (selected) Palette.OnPrimary else Palette.Foreground, tween(250), label = "chipFg")
            val border by animateColorAsState(if (selected) Palette.Primary else Palette.Border, label = "chipBorder")
            Row(
                Modifier.clip(CircleShape).background(bg).border(1.dp, border, CircleShape)
                    .clickable { onSelect(c) }.padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(c.emoji, fontSize = 15.sp)
                Spacer(Modifier.width(7.dp))
                Text(c.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
            }
        }
    }
}

// =====================================================================================  hot deals

@Composable
private fun HotSection(onOpen: (Product) -> Unit) {
    Column {
        SectionTitle("🔥 Гарячі знижки", "до −${Catalog.hot.maxOf { it.discount ?: 0 }}%")
        Spacer(Modifier.height(12.dp))
        LazyRow(
            modifier = Modifier.bleed(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Catalog.hot, key = { it.id }) { p -> HotCard(p) { onOpen(p) } }
        }
    }
}

@Composable
private fun HotCard(p: Product, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier.width(280.dp).clip(shape)
            .background(Brush.horizontalGradient(listOf(Palette.Surface3, Palette.Surface2)))
            .border(1.dp, Palette.Border, shape).clickable(onClick = onClick).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Photo(p.photo, Modifier.size(100.dp).clip(RoundedCornerShape(18.dp)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            DiscountBadge(p.discount ?: 0)
            Spacer(Modifier.height(6.dp))
            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Rating(p)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(price(p.price), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(6.dp))
                p.oldPrice?.let {
                    Text(price(it), fontSize = 11.sp, color = Palette.Muted, textDecoration = TextDecoration.LineThrough)
                }
            }
        }
    }
}

// =====================================================================================  product card

@Composable
private fun ProductCard(
    product: Product,
    tall: Boolean,
    favorite: Boolean,
    inCart: Int,
    onFavorite: () -> Unit,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    val shape = RoundedCornerShape(26.dp)

    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Palette.Surface2)
            .border(1.dp, Palette.Border, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(if (tall) 214.dp else 158.dp).clip(RoundedCornerShape(20.dp))) {
            Photo(product.photo, Modifier.fillMaxSize())
            product.discount?.let { DiscountBadge(it, Modifier.align(Alignment.TopStart).padding(8.dp), solid = true) }
            FavoriteButton(favorite, onFavorite, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
        Column(Modifier.padding(start = 6.dp, end = 4.dp, top = 10.dp, bottom = 4.dp)) {
            Text(
                product.category.label.uppercase(),
                fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Palette.Muted,
            )
            Text(product.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Rating(product)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    product.oldPrice?.let {
                        Text(price(it), fontSize = 11.sp, color = Palette.Muted, textDecoration = TextDecoration.LineThrough)
                    }
                    Text(price(product.price), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                AddButton(inCart, onAdd)
            }
        }
    }
}

@Composable
private fun FavoriteButton(favorite: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val pop = remember { Animatable(1f) }
    val tint by animateColorAsState(if (favorite) Palette.Pink else Color.White, label = "heart")
    Box(
        modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.38f))
            .clickable {
                onToggle()
                scope.launch {
                    pop.animateTo(1.4f, tween(110))
                    pop.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            "Обране",
            tint = tint,
            modifier = Modifier.size(18.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
    }
}

@Composable
private fun AddButton(inCart: Int, onAdd: () -> Unit) {
    val bg by animateColorAsState(if (inCart > 0) Palette.Foreground else Palette.Primary, label = "addBg")
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable(onClick = onAdd),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = inCart,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "add",
        ) { n ->
            if (n == 0) Icon(Icons.Filled.Add, "У кошик", tint = Palette.OnPrimary)
            else Text("$n", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Palette.Background)
        }
    }
}

@Composable
private fun DiscountBadge(percent: Int, modifier: Modifier = Modifier, solid: Boolean = false) {
    Text(
        "−$percent%",
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (solid) Palette.OnPrimary else Palette.PrimaryText,
        modifier = modifier.clip(CircleShape)
            .background(if (solid) Palette.Primary else Palette.Primary.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun Rating(p: Product) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, null, Modifier.size(14.dp), tint = Palette.Gold)
        Spacer(Modifier.width(3.dp))
        Text("${p.rating}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text("  (${p.reviews})", fontSize = 12.sp, color = Palette.Muted)
    }
}

// =====================================================================================  misc blocks

@Composable
private fun SectionTitle(title: String, trailing: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(trailing, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.PrimaryText)
    }
}

@Composable
private fun CartSummary(count: Int, total: Int, onClear: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(Brush.linearGradient(listOf(Palette.Primary.copy(alpha = 0.22f), Palette.Violet.copy(alpha = 0.18f))))
            .border(1.dp, Palette.Primary.copy(alpha = 0.35f), shape).padding(18.dp),
    ) {
        Text("РАЗОМ · $count ТОВ.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, color = Palette.PrimaryText)
        AnimatedContent(
            targetState = total,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "total",
        ) { t -> Text(price(t), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Оформити замовлення", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Palette.OnPrimary,
                modifier = Modifier.clip(CircleShape).background(Palette.Primary).padding(horizontal = 18.dp, vertical = 11.dp),
            )
            Text(
                "Очистити", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).background(Palette.Surface4).clickable(onClick = onClear)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun EmptyState(tab: Tab) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            when (tab) {
                Tab.FAVORITES -> "💔"
                Tab.CART -> "🛒"
                Tab.HOME -> "🔍"
            },
            fontSize = 48.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            when (tab) {
                Tab.FAVORITES -> "Тут з'являться товари, які ти вподобаєш"
                Tab.CART -> "Кошик поки що порожній"
                Tab.HOME -> "Нічого не знайдено"
            },
            color = Palette.Muted, fontSize = 14.sp,
        )
    }
}

// =====================================================================================  bottom bar

@Composable
private fun BottomBar(current: Tab, cartCount: Int, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .shadow(28.dp, CircleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Palette.Surface3, CircleShape)
            .border(1.dp, Palette.Border, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { t ->
            val selected = t == current
            val bg by animateColorAsState(if (selected) Palette.Primary else Color.Transparent, tween(300), label = "tabBg")
            val fg by animateColorAsState(if (selected) Palette.OnPrimary else Palette.Muted, tween(300), label = "tabFg")
            Row(
                // no clip here, so the cart badge can stick out of the pill
                Modifier.background(bg, CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(t) }
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Icon(t.icon, t.label, Modifier.size(22.dp), tint = fg)
                    if (t == Tab.CART) {
                        CountBadge(cartCount, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-8).dp), ring = if (selected) Palette.Primary else Palette.Surface3, inverted = selected)
                    }
                }
                AnimatedVisibility(selected, enter = expandHorizontally() + fadeIn(), exit = shrinkHorizontally() + fadeOut()) {
                    Text(t.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(start = if (t == Tab.CART && cartCount > 0) 14.dp else 8.dp))
                }
            }
        }
    }
}

// =====================================================================================  product sheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSheet(
    product: Product,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onAdd: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var quantity by remember { mutableIntStateOf(1) }
    var color by remember { mutableIntStateOf(0) }
    var size by remember { mutableStateOf(product.sizes.getOrNull(2)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = Palette.Surface2,
        dragHandle = {
            Box(Modifier.padding(vertical = 12.dp).size(44.dp, 5.dp).clip(CircleShape).background(Palette.Surface4))
        },
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            Box(Modifier.fillMaxWidth().height(290.dp).clip(RoundedCornerShape(28.dp))) {
                Photo(product.photo, Modifier.fillMaxSize())
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)),
                    ),
                )
                product.discount?.let { DiscountBadge(it, Modifier.align(Alignment.TopStart).padding(14.dp), solid = true) }
                FavoriteButton(favorite, onFavorite, Modifier.align(Alignment.TopEnd).padding(14.dp))
                Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, Modifier.size(16.dp), tint = Palette.Gold)
                    Text(" ${product.rating} · ${product.reviews} відгуків", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(product.category.label.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, color = Palette.PrimaryText)
            Text(product.name, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(6.dp))
            Text(product.description, fontSize = 14.sp, lineHeight = 21.sp, color = Palette.Muted)

            Spacer(Modifier.height(18.dp))
            Label("Колір")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                product.colors.forEachIndexed { i, c ->
                    val ring by animateColorAsState(if (i == color) Palette.Primary else Color.Transparent, label = "ring")
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).border(2.dp, ring, CircleShape).clickable { color = i }.padding(5.dp)
                            .clip(CircleShape).background(c).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    )
                }
            }

            if (product.sizes.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Label("Розмір")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.sizes.forEach { s ->
                        val selected = s == size
                        val bg by animateColorAsState(if (selected) Palette.Foreground else Palette.Surface3, label = "sizeBg")
                        val fg by animateColorAsState(if (selected) Palette.Background else Palette.Foreground, label = "sizeFg")
                        Box(
                            Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(14.dp)).background(bg)
                                .border(1.dp, if (selected) Color.Transparent else Palette.Border, RoundedCornerShape(14.dp))
                                .clickable { size = s },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(s, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper(quantity, onChange = { quantity = it })
                Spacer(Modifier.width(12.dp))
                Row(
                    Modifier.weight(1f).height(58.dp).clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Palette.PrimaryLight, Palette.Primary)))
                        .clickable {
                            onAdd(quantity)
                            scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(20.dp), tint = Palette.OnPrimary)
                    Spacer(Modifier.width(8.dp))
                    AnimatedContent(targetState = product.price * quantity, label = "sum") { sum ->
                        Text("В кошик · ${price(sum)}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Palette.OnPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        color = Palette.Muted, modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Stepper(value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.height(58.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Surface3)
            .border(1.dp, Palette.Border, RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−") { if (value > 1) onChange(value - 1) }
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                val up = targetState > initialState
                (slideInVertically { if (up) it else -it } + fadeIn()) togetherWith (slideOutVertically { if (up) -it else it } + fadeOut())
            },
            label = "qty",
        ) { n ->
            Text("$n", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        StepperButton("+") { if (value < 9) onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(symbol: String, onClick: () -> Unit) {
    Box(Modifier.size(46.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

// =====================================================================================  helpers

/** Network photo with a crossfade, placeholder colour while loading. */
@Composable
private fun Photo(url: String, modifier: Modifier) {
    val context = LocalContext.current
    AsyncImage(
        model = remember(url) { ImageRequest.Builder(context).data(url).crossfade(true).build() },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(Palette.Surface3),
    )
}

/** Lets a horizontally scrolling row run edge to edge although its parent has side padding. */
private fun Modifier.bleed(horizontal: Dp) = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}
