package com.github.JohannesLipp.TheStash;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

        openDatabase();

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
        if (itemId == R.id.action_refresh_data_and_pictures) {
            // Handle refresh pictures action
            Toast.makeText(this, "Refresh Data and Pictures clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement refresh data and pictures logic
            return true;
        } else if (itemId == R.id.action_export_database) {
            exportDatabaseAsJson();
            return true;
        } else if (itemId == R.id.action_settings) {
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
            // TODO: Implement settings logic
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportDatabaseAsJson() {
        Toast.makeText(this, "Exporting database as JSON...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            List<FoodItem> foodItems = database.foodItemDao().getAllItemsSorted();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            try {
                String jsonString = objectMapper.writeValueAsString(foodItems);
                Log.d(TAG, "Serialized JSON for export: " + jsonString.substring(0, Math.min(jsonString.length(), 500)) + "...");

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                String exportJsonFileName = currentDate + " The Stash Database Export.json";

                writeJsonToDownloads(MainActivity.this, jsonString, exportJsonFileName);

                Log.i(TAG, "Database exported successfully as JSON to Downloads directory: " + exportJsonFileName);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Database exported as JSON to Downloads", Toast.LENGTH_LONG).show());

            } catch (IOException e) { // Catches JsonProcessingException too
                Log.e(TAG, "Error exporting database as JSON", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error exporting JSON: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    /**
     * Writes a JSON string to a file in the public "Downloads" directory using MediaStore.
     *
     * @param context     The context.
     * @param jsonString  The JSON string to write.
     * @param displayName The desired display name for the file in Downloads.
     * @throws IOException If an error occurs during writing.
     */
    private void writeJsonToDownloads(Context context, String jsonString, String displayName) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");

        Uri collectionUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            // For older versions (pre-Q), direct file access.
            // This requires WRITE_EXTERNAL_STORAGE permission.
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                if (!downloadsDir.mkdirs()) {
                    throw new IOException("Failed to create Downloads directory (pre-Q).");
                }
            }
            File destFile = new File(downloadsDir, displayName);
            try (FileOutputStream fos = new FileOutputStream(destFile);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                writer.write(jsonString);
            }
            Log.d(TAG, "JSON file written to: " + destFile.getAbsolutePath() + " (pre-Q method)");
            return; // Exit if pre-Q handled
        }

        Uri itemUri = context.getContentResolver().insert(collectionUri, values);
        if (itemUri == null) {
            throw new IOException("Failed to create new MediaStore record for " + displayName);
        }

        try (OutputStream out = context.getContentResolver().openOutputStream(itemUri);
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            if (out == null) {
                throw new IOException("Failed to open output stream for " + itemUri);
            }
            writer.write(jsonString);
            Log.d(TAG, "JSON file written to MediaStore URI: " + itemUri);
        } catch (Exception e) {
            // If something goes wrong, try to delete the incomplete MediaStore entry
            Log.e(TAG, "Error writing JSON to MediaStore", e);
            context.getContentResolver().delete(itemUri, null, null);
        }
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

    private void openDatabase() {
        database = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Constants.DATABASE_NAME).build();
    }
}
