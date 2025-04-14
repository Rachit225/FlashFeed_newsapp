package com.example.newsapp;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class BookmarkedNewsActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private NewsAdapter adapter;
    private List<NewsItem> bookmarkedNewsList;
    private DatabaseReference bookmarksRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bookmarked_news);

        recyclerView = findViewById(R.id.bookmarkedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookmarkedNewsList = new ArrayList<>();
        adapter = new NewsAdapter(bookmarkedNewsList, Glide.with(this));
        recyclerView.setAdapter(adapter);

        bookmarksRef = FirebaseDatabase.getInstance().getReference("Bookmarks");
        loadBookmarkedNews();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadBookmarkedNews() {
        bookmarksRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookmarkedNewsList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    NewsItem newsItem = snapshot.getValue(NewsItem.class);
                    if (newsItem != null) {
                        bookmarkedNewsList.add(newsItem);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(BookmarkedNewsActivity.this, "Error loading bookmarks", Toast.LENGTH_SHORT).show();
            }
        });
    }
}