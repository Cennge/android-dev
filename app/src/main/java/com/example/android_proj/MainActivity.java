package com.example.android_proj;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    // ключі, за якими SimpleAdapter бере дані з кожного Map
    private static final String KEY_IMAGE = "image";
    private static final String KEY_TITLE = "title";
    private static final String KEY_DESCRIPTION = "description";
    private static final String KEY_PRICE = "price";

    // масив URL картинок (Unsplash) — картинка відповідає тексту
    private final String[] images = {
            "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=300&q=80",
            "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=300&q=80",
            "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=300&q=80",
            "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300&q=80",
            "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=300&q=80",
    };
    private final String[] titles = {"Навушники", "Фотоапарат", "Годинник", "Кросівки", "Рюкзак"};
    private final String[] descriptions = {
            "Повнорозмірні, з шумозаглушенням",
            "Миттєві знімки, як у старі часи",
            "Кроки, пульс і сповіщення",
            "Легкі, для бігу та прогулянок",
            "Місткий, з відділом для ноутбука",
    };
    private final String[] prices = {"5 299 ₴", "4 199 ₴", "8 999 ₴", "3 499 ₴", "2 299 ₴"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // дані для SimpleAdapter: список Map, один Map = один пункт
        List<Map<String, Object>> data = new ArrayList<>();
        for (int i = 0; i < titles.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put(KEY_IMAGE, images[i]);
            item.put(KEY_TITLE, titles[i]);
            item.put(KEY_DESCRIPTION, descriptions[i]);
            item.put(KEY_PRICE, prices[i]);
            data.add(item);
        }

        String[] from = {KEY_IMAGE, KEY_TITLE, KEY_DESCRIPTION, KEY_PRICE};
        int[] to = {R.id.image, R.id.title, R.id.description, R.id.price};
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.item_row, from, to);

        // SimpleAdapter сам не вміє вантажити картинки з інтернету —
        // для CircleImageView завантажуємо URL через Glide
        adapter.setViewBinder((view, value, text) -> {
            if (view instanceof CircleImageView) {
                Glide.with(view)
                        .load((String) value)
                        .dontAnimate() // CircleImageView не підтримує анімацію переходу Glide
                        .into((CircleImageView) view);
                return true;
            }
            return false; // текст SimpleAdapter ставить сам
        });

        ListView list = findViewById(R.id.list);
        list.setAdapter(adapter);
    }
}
