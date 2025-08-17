package com.github.JohannesLipp.TheStash;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {

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
                .allowMainThreadQueries() // For demo purposes only. Use background threads in production!
                .build();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        //adapter = new FoodAdapter(this::showDeleteDialog);
        adapter = new FoodAdapter(this::downloadFoodData);
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

    /**
     * Loads all food items from the database and updates the RecyclerView.
     */
    private void loadItems() {
        List<FoodItem> items = db.foodItemDao().getAllItemsSorted();
        adapter.setItems(items);
    }

    /**
     * Opens the dialog to add a new item with an optional scanned barcode.
     *
     * @param scannedBarcode the barcode string from the scanner (or null if not scanned)
     */
    private void openAddDialog(String scannedBarcode) {
        AddItemDialog dialog = new AddItemDialog(this, scannedBarcode, (barcode, day, month, year, quantity) -> {
            FoodItem newItem = new FoodItem(barcode, day, month, year, quantity);
            db.foodItemDao().insert(newItem);
            loadItems();
            Toast.makeText(MainActivity.this, "Item saved successfully", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    /**
     * Opens the dialog to delete or reduce quantity of an existing item.
     * If quantity reaches zero, the item is removed.
     *
     * @param item the FoodItem to modify
     */
    private void showDeleteDialog(FoodItem item) {
        DeleteItemDialog dialog = new DeleteItemDialog(this, quantityToRemove -> {
            if (quantityToRemove <= 0) { // Safety check or specific handling if needed
                Toast.makeText(MainActivity.this, "Please enter a valid quantity to remove.", Toast.LENGTH_SHORT).show();
                return;
            }

            int currentQuantity = item.getQuantity();
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

    private class ProductDataCallback implements OpenFoodFacts.ProductDataCallback {

        @Override
        public void onSuccess(OpenFoodFactsResultDTO product) {
            Log.i("MainActivity", "Download successful: " + product);
            // Toast.makeText(MainActivity.this, "Download successful. Product name: " + productName + ",", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String errorMessage) {
            Log.e("MainActivity", "Download failed. Error: " + errorMessage);
            //Toast.makeText(MainActivity.this, "Download failed. Error: " + errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadFoodData(FoodItem item) {
        OpenFoodFacts openFoodFacts = new OpenFoodFacts();
        openFoodFacts.fetchProductData(item.getBarcode(), new ProductDataCallback());
    }
}
