package com.example.newsapp;

import androidx.core.content.ContextCompat;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ViewPagerAdapter extends PagerAdapter {
    List<SliderItems> sliderItems;
    LayoutInflater mLayoutInflater;
    Context context;
    ArrayList<String> title;
    ArrayList<String> newslink;
    ArrayList<String> desc;
    ArrayList<String> head;
    VerticalViewerPage verticalViewerPage;
    int newposition;
    float x1, x2;
    private TextToSpeech tts;
    private boolean isTTSReady = false;
    private DatabaseReference bookmarksRef;

    public ViewPagerAdapter(Context context, List<SliderItems> sliderItems, ArrayList<String> title,
            ArrayList<String> desc,
            ArrayList<String> newslink, ArrayList<String> head, VerticalViewerPage verticalViewerPage) {
        this.context = context;
        this.sliderItems = sliderItems;
        this.desc = desc;
        this.title = title;
        this.head = head;
        this.newslink = newslink;
        this.verticalViewerPage = verticalViewerPage;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        bookmarksRef = FirebaseDatabase.getInstance().getReference("Bookmarks");

        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isTTSReady = true;
                }
            }
        });
    }

    @Override
    public int getCount() {
        return sliderItems.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == ((LinearLayout) object);
    }

    @SuppressLint("ClickableViewAccessibility")
    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.item_container, container, false);
        ImageView imageView = itemView.findViewById(R.id.imageView);
        ImageView imageView2 = itemView.findViewById(R.id.imageView2);
        TextView titles = itemView.findViewById(R.id.headline);
        TextView descrip = itemView.findViewById(R.id.desc);
        TextView heads = itemView.findViewById(R.id.head);
        TextView lightValueTextView = itemView.findViewById(R.id.lightValue);
        ImageButton bookmarkButton = itemView.findViewById(R.id.bookmarkButton);

        // Check if article is bookmarked
        bookmarksRef.child(title.get(position)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    bookmarkButton.setImageResource(R.drawable.baseline_bookmark_24);
                } else {
                    bookmarkButton.setImageResource(R.drawable.baseline_bookmark_border_24);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error checking bookmark status", Toast.LENGTH_SHORT).show();
            }
        });

        // Bookmark button click listener
        bookmarkButton.setOnClickListener(v -> {
            String currentTitle = title.get(position);
            bookmarksRef.child(currentTitle).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Remove bookmark
                        bookmarksRef.child(currentTitle).removeValue();
                        bookmarkButton.setImageResource(R.drawable.baseline_bookmark_border_24);
                        Toast.makeText(context, "Article removed from bookmarks", Toast.LENGTH_SHORT).show();
                    } else {
                        // Add bookmark
                        NewsItem bookmarkedNews = new NewsItem(
                                newslink.get(position),
                                sliderItems.get(position).getImage(),
                                head.get(position),
                                currentTitle,
                                desc.get(position));
                        bookmarksRef.child(currentTitle).setValue(bookmarkedNews);
                        bookmarkButton.setImageResource(R.drawable.baseline_bookmark_24);
                        Toast.makeText(context, "Article bookmarked", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, "Error updating bookmark", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Light Sensor Setup for this item
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Handler handler = new Handler();

        if (lightSensor != null) {
            SensorEventListener lightSensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    float lux = event.values[0];
                    handler.post(() -> {
                        lightValueTextView.setText(String.format(Locale.getDefault(), "%.2f lx", lux));

                        // Dark mode implementation
                        androidx.cardview.widget.CardView cardView = itemView.findViewById(R.id.card_view);
                        TextView headline = itemView.findViewById(R.id.headline);
                        TextView description = itemView.findViewById(R.id.desc);
                        TextView head = itemView.findViewById(R.id.head);
                        TextView swipeText = itemView.findViewById(R.id.swipetext);
                        TextView tapHere = itemView.findViewById(R.id.taphere);

                        if (lux < 30) {
                            // Dark theme colors
                            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.dark_background));
                            headline.setTextColor(ContextCompat.getColor(context, R.color.dark_text));
                            description.setTextColor(ContextCompat.getColor(context, R.color.dark_text));
                            head.setTextColor(ContextCompat.getColor(context, R.color.dark_text));
                            swipeText.setTextColor(ContextCompat.getColor(context, R.color.dark_secondary_text));
                            tapHere.setTextColor(ContextCompat.getColor(context, R.color.dark_secondary_text));
                        } else {
                            // Light theme colors
                            cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.light_background));
                            headline.setTextColor(ContextCompat.getColor(context, R.color.light_text));
                            description.setTextColor(ContextCompat.getColor(context, R.color.light_text));
                            head.setTextColor(ContextCompat.getColor(context, R.color.light_text));
                            swipeText.setTextColor(ContextCompat.getColor(context, R.color.light_secondary_text));
                            tapHere.setTextColor(ContextCompat.getColor(context, R.color.light_secondary_text));
                        }
                    });
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };
            sensorManager.registerListener(lightSensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            lightValueTextView.setText("No sensor");
        }

        // Rest of the original code remains unchanged
        ImageButton ttsButton = itemView.findViewById(R.id.ttsButton);
        ttsButton.setOnClickListener(v -> {
            if (isTTSReady) {
                tts.stop();
                tts.speak(title.get(position), TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        ImageView translateAndSpeak = itemView.findViewById(R.id.translateAndSpeak);
        translateAndSpeak.setOnClickListener(v -> {
            if (isTTSReady) {
                String englishText = desc.get(position);
                String hindiText = translateToHindi(englishText);
                tts.stop();
                tts.setLanguage(new Locale("hi", "IN"));
                tts.speak(hindiText, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });

        ImageButton shareButton = itemView.findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> {
            if (newslink != null && position < newslink.size()) {
                String urlToShare = newslink.get(position);
                if (urlToShare != null && !urlToShare.isEmpty()) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, urlToShare);
                    context.startActivity(Intent.createChooser(shareIntent, "Share news via"));
                }
            }
        });

        titles.setText(title.get(position));
        descrip.setText(desc.get(position));
        heads.setText(head.get(position));

        Glide.with(context)
                .load(sliderItems.get(position).getImage())
                .centerCrop()
                .into(imageView);
        Glide.with(context)
                .load(sliderItems.get(position).getImage())
                .override(12, 12)
                .centerCrop()
                .into(imageView2);

        verticalViewerPage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                newposition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        verticalViewerPage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x1 = event.getX();
                        break;
                    case MotionEvent.ACTION_UP:
                        x2 = event.getX();
                        float deltaX = x2 - x1;
                        if (deltaX > 300) {
                            Intent i = new Intent(context, NewsDetailActivity.class);
                            if (position == 1) {
                                i.putExtra("url", newslink.get(0));
                                context.startActivity(i);
                            } else {
                                i.putExtra("url", newslink.get(newposition));
                                context.startActivity(i);
                            }
                        }
                        break;
                }
                return false;
            }
        });

        container.addView(itemView);
        return itemView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((LinearLayout) object);
    }

    private String translateToHindi(String english) {
        switch (english.trim().toLowerCase()) {
            case "description":
                return "विवरण";
            case "swipe left to read more":
                return "और पढ़ने के लिए बाएँ स्वाइप करें";
            case "tap to read more":
                return "और पढ़ने के लिए टैप करें";
            default:
                return "अनुवाद उपलब्ध नहीं है";
        }
    }

    private void showError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        Log.e("MLKitTranslation", message);
    }
}