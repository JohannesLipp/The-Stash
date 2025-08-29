package com.github.JohannesLipp.TheStash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private FoodAdapter adapter;
    private AppDatabase database;


    private ActivityResultLauncher<Intent> barcodeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        database = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "food_database")
                .build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FoodAdapter(
                this::showDeleteItemDialog,
                foodItem -> ItemDataUpdater.downloadFoodDataAndImage(
                        foodItem,
                        database,
                        MainActivity.this,
                        adapter
                )
        );
        recyclerView.setAdapter(adapter);

        reloadAllItems();

        barcodeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String scannedBarcode = result.getData().getStringExtra("barcode");
                        showAddItemDialog(scannedBarcode);
                    } else {
                        showAddItemDialog(null);
                    }
                }
        );

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BarcodeScannerActivity.class);
            barcodeLauncher.launch(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh_pictures) {
            // Handle refresh pictures action
            Toast.makeText(this, "Refresh Pictures clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement refresh pictures logic
            return true;
        } else if (itemId == R.id.action_refresh_data) {
            // Handle refresh data action
            Toast.makeText(this, "Refresh Data clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement refresh data logic
            return true;
        } else if (itemId == R.id.action_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement settings logic
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void reloadAllItems() {
        new Thread(() -> {
            final List<FoodItem> items = database.foodItemDao().getAllItemsSorted();
            Log.d(TAG, "Loaded items: " + items);
            runOnUiThread(() -> adapter.setItems(items));
        }).start();
    }

    private void showAddItemDialog(@Nullable String scannedBarcode) {
        AddItemDialog dialog = new AddItemDialog(
                this,
                scannedBarcode,
                (barcode, day, month, year, quantity) ->
                        new Thread(() -> { // Perform DB operation on background thread
                            FoodItem newItem = new FoodItem(barcode, day, month, year, quantity);
                            long newId = database.foodItemDao().insert(newItem);
                            newItem.setId(newId);

                            reloadAllItems();
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Item saved successfully", Toast.LENGTH_SHORT).show());

                            ItemDataUpdater.downloadFoodDataAndImage(newItem, database, MainActivity.this, adapter);
                        }).start());
        dialog.show();
    }

    private void showDeleteItemDialog(FoodItem item) {
        DeleteItemDialog dialog = new DeleteItemDialog(this, quantityToRemove -> {
            // Perform DB operation on background thread
            new Thread(() -> {
                String toastText;
                if (quantityToRemove >= item.getCount()) {
                    database.foodItemDao().delete(item);
                    toastText = "Item removed";
                } else {
                    database.foodItemDao().reduceQuantity(item.getId(), quantityToRemove);
                    toastText = "Item quantity reduced";
                }

                // Update RecyclerView with latest database state
                List<FoodItem> foodItems = database.foodItemDao().getAllItemsSorted();
                runOnUiThread(() -> {
                    adapter.setItems(foodItems);
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        dialog.show();
    }
}
