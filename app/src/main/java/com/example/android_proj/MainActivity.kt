package com.example.android_proj

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.android_proj.shop.ShopApp
import com.example.android_proj.shop.ShopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // весь інтерфейс — Jetpack Compose, без XML-розмітки
        setContent {
            ShopTheme {
                ShopApp()
            }
        }
    }
}
