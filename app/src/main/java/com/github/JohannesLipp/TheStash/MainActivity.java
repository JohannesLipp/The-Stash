package com.github.JohannesLipp.TheStash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private FoodAdapter adapter;
    private AppDatabase db;

    private ActivityResultLauncher<Intent> barcodeLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "food_database")
                .build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FoodAdapter(this::showDeleteDialog, this::downloadFoodData);
        recyclerView.setAdapter(adapter);

        loadItems();

        barcodeLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String scannedBarcode = result.getData().getStringExtra("barcode");
                        openAddDialog(scannedBarcode);
                    } else {
                        openAddDialog(null);
                    }
                }
        );

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BarcodeScannerActivity.class);
            barcodeLauncher.launch(intent);
        });
    }

    private void loadItems() {
        new Thread(() -> {
            final List<FoodItem> items = db.foodItemDao().getAllItemsSorted();
            // Post the update back to the main UI thread
            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setItems(items);
                }
            });
        }).start();
    }

    private void openAddDialog(String scannedBarcode) {
        AddItemDialog dialog = new AddItemDialog(this, scannedBarcode, (barcode, day, month, year, quantity) -> {
            new Thread(() -> { // Perform DB operation on background thread
                FoodItem newItem = new FoodItem(barcode, day, month, year, quantity);
                db.foodItemDao().insert(newItem);
                loadItems();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Item saved successfully", Toast.LENGTH_SHORT).show());

                downloadFoodData(newItem);
            }).start();
        });
        dialog.show();
    }

    private void showDeleteDialog(FoodItem item) {
        DeleteItemDialog dialog = new DeleteItemDialog(this, quantityToRemove -> {
            if (quantityToRemove <= 0) { // Safety check or specific handling if needed
                Toast.makeText(MainActivity.this, "Please enter a valid quantity to remove.", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentQuantity = item.getCount();
            String itemDescription = item.getBarcode(); // Or another descriptive field

            if (quantityToRemove >= currentQuantity) {
                // Remove the item entirely
                db.foodItemDao().delete(item);
                Toast.makeText(MainActivity.this, "Item '" + itemDescription + "' removed.", Toast.LENGTH_SHORT).show();
            } else {
                db.foodItemDao().reduceQuantity(item.getId(), quantityToRemove);
                Toast.makeText(MainActivity.this, "Quantity for '" + itemDescription + "' updated", Toast.LENGTH_SHORT).show();
            }
            loadItems();
        });
        dialog.show();
    }

    private class ProductDataCallbackHandler implements OpenFoodFacts.ProductDataCallback {

        @Override
        public void onSuccess(@NonNull FoodItem foodItem, OpenFoodFactsResultDTO product) {
            Log.i("MainActivity", "Download successful: " + product);

            // Update database entry (product_name_de, or product_name as fallback; brands, image_url), and update the recyclerview if necessary

            foodItem.setName(product.getProductName());
            foodItem.setBrands(product.getBrands());
            foodItem.setImageUrl(product.getImageUrl());

            new Thread(() -> {
                db.foodItemDao().update(foodItem); // Save the updated item to the database
                Log.d(TAG, "Updated item in DB: " + foodItem);

                loadItems(); // Update the RecyclerView by reloading items from the database

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Details updated for: " + foodItem.getName(), Toast.LENGTH_SHORT).show();
                });
            }).start();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("MainActivity", "Download failed. Error: " + errorMessage);
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Failed to get details: " + errorMessage.substring(0, Math.min(errorMessage.length(), 50)), Toast.LENGTH_LONG).show();
            });
        }
    }

    private boolean downloadFoodData(FoodItem foodItem) {
        OpenFoodFacts openFoodFacts = new OpenFoodFacts();
        openFoodFacts.fetchProductData(foodItem, new ProductDataCallbackHandler());
        return true;
    }
}
