package com.example.android_proj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.android_proj.shop.WishlistScreen
import com.example.android_proj.shop.ShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // «Список бажань» на Jetpack Compose
        setContent {
            ShopTheme {
                WishlistScreen()
            }
        }
    }
}
