package com.github.JohannesLipp.TheStash;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FoodAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "food_database")
                .allowMainThreadQueries()
                .build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FoodAdapter(this::showDeleteDialog);
        recyclerView.setAdapter(adapter);

        loadItems();

        fabAdd.setOnClickListener(v -> openAddDialog());
    }

    private void loadItems() {
        List<FoodItem> items = db.foodItemDao().getAllItemsSorted();
        adapter.setItems(items);
    }

    private void openAddDialog() {
        AddItemDialog dialog = new AddItemDialog(this, (barcode, month, year, quantity) -> {
            FoodItem newItem = new FoodItem(barcode, month, year, quantity);
            db.foodItemDao().insert(newItem);
            loadItems();
            Toast.makeText(MainActivity.this, "Entry saved", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void showDeleteDialog(FoodItem item) {
        DeleteItemDialog dialog = new DeleteItemDialog(this, item, quantityToRemove -> {
            db.foodItemDao().reduceQuantity(item.getId(), quantityToRemove);
            loadItems();
            Toast.makeText(MainActivity.this, "Count reduced", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }
}