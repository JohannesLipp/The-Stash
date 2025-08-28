package com.github.JohannesLipp.TheStash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
        adapter = new FoodAdapter(this::showDeleteItemDialog, this::downloadFoodData);
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

    private void reloadAllItems() {
        new Thread(() -> {
            final List<FoodItem> items = db.foodItemDao().getAllItemsSorted();
            Log.d(TAG, "Loaded items: " + items);
            // Post the update back to the main UI thread
            runOnUiThread(() -> {
                if (adapter != null) {
                    adapter.setItems(items);
                    Log.d(TAG, "Updated adapter with items: " + items);
                }
            });
        }).start();
    }

    private void showAddItemDialog(@Nullable String scannedBarcode) {
        AddItemDialog dialog = new AddItemDialog(
                this,
                scannedBarcode,
                (barcode, day, month, year, quantity) ->
                        new Thread(() -> { // Perform DB operation on background thread
                            FoodItem newItem = new FoodItem(barcode, day, month, year, quantity);
                            db.foodItemDao().insert(newItem);
                            reloadAllItems();
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Item saved successfully", Toast.LENGTH_SHORT).show());

                            downloadFoodData(newItem);
                        }).start());
        dialog.show();
    }

    private void showDeleteItemDialog(FoodItem item) {
        DeleteItemDialog dialog = new DeleteItemDialog(this, quantityToRemove -> {
            if (quantityToRemove <= 0) { // Safety check or specific handling if needed
                Toast.makeText(MainActivity.this, "Please enter a valid quantity to remove.", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentQuantity = item.getCount();
            String itemDescription = item.getBarcode(); // Or another descriptive field

            if (quantityToRemove >= currentQuantity) {
                // Remove the item entirely
                new Thread(() -> { // Perform DB operation on background thread
                    db.foodItemDao().delete(item);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Item '" + itemDescription + "' removed.", Toast.LENGTH_SHORT).show());
                }).start();
            } else {
                new Thread(() -> { // Perform DB operation on background thread
                    db.foodItemDao().reduceQuantity(item.getId(), quantityToRemove);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Quantity for '" + itemDescription + "' updated", Toast.LENGTH_SHORT).show());
                }).start();
            }
            reloadAllItems();
        });
        dialog.show();
    }

    private class ProductDataCallbackHandler implements OpenFoodFacts.ProductDataCallback {
        private final FoodItem itemToUpdate;

        public ProductDataCallbackHandler(FoodItem itemToUpdate) {
            this.itemToUpdate = itemToUpdate;
        }

        @Override
        public void onSuccess(OpenFoodFactsResultDTO productData) {
            Log.d("MainActivity", "Download successful, updating DB & UI: " + productData);

            // Update database entry (product_name_de, or product_name as fallback; brands, image_url), and update the recyclerview if necessary
            new Thread(() -> {
                itemToUpdate.setName(productData.getProductName());
                itemToUpdate.setBrands(productData.getBrands());
                itemToUpdate.setImageUrl(productData.getImageUrl());
                db.foodItemDao().update(itemToUpdate); // Save the updated item to the database
                Log.d(TAG, "Updated item in DB: " + itemToUpdate);

                reloadAllItems(); // Refresh RecyclerView with text updates

                if (productData.getImageUrl() != null && !productData.getImageUrl().isEmpty()) {
                    downloadAndStoreImage(itemToUpdate, productData.getImageUrl());
                }

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Details updated for: " + itemToUpdate.getName(), Toast.LENGTH_SHORT).show());
            }).start();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("MainActivity", "Download failed. Error: " + errorMessage);
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get details: " + errorMessage.substring(0, Math.min(errorMessage.length(), 50)), Toast.LENGTH_LONG).show());
        }
    }

    private void downloadAndStoreImage(FoodItem itemToUpdateWithImage, String imageUrlString) {
        new Thread(() -> { // Network and DB operations on a background thread
            try {
                URL url = new URL(imageUrlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                connection.disconnect();

                if (bitmap != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    // Choose format and quality. JPEG is common for photos.
                    // PNG is good for graphics with transparency but often larger.
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream); // Compress to JPEG with 80% quality
                    byte[] imageByteArray = stream.toByteArray();
                    stream.close();

                    if (itemToUpdateWithImage != null) {
                        itemToUpdateWithImage.setImageData(imageByteArray);
                        db.foodItemDao().update(itemToUpdateWithImage);
                        Log.i(TAG, "Image downloaded and stored in DB for: " + itemToUpdateWithImage.getName());

                        // Refresh UI to show the image (if your adapter is set up for it)
                        reloadAllItems(); // This will re-bind the view, adapter needs to handle byte[] for image
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Image updated for " + itemToUpdateWithImage.getName(), Toast.LENGTH_SHORT).show());
                    } else {
                        Log.w(TAG, "Item not found for image update.");
                    }
                } else {
                    Log.e(TAG, "Failed to decode bitmap from URL: " + imageUrlString);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error downloading image: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to download image.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private boolean downloadFoodData(FoodItem foodItem) {
        Log.d(TAG, "Starting download for item ID: " + foodItem.getId() + ", Barcode: " + foodItem.getBarcode());
        OpenFoodFacts openFoodFacts = new OpenFoodFacts();
        openFoodFacts.fetchProductData(foodItem, new ProductDataCallbackHandler(foodItem));
        return true;
    }
}
