package com.github.JohannesLipp.TheStash;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenFoodFacts {

    private static final String TAG = "OpenFoodFacts";
    // Staging API documentation: https://openfoodfacts.github.io/openfoodfacts-server/api/v3/api-docs-staging/
    // Staging API base URL for products:
    private static final String STAGING_API_URL = "https://staging.openfoodfacts.org/api/v2/product/";
    // For V3, it would be "https://staging.openfoodfacts.org/api/v3/product/" - adjust if needed based on exact endpoint

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface ProductDataCallback {
        void onSuccess(String productName, String brands, String quantity, String imageUrl); // Add more fields as needed
        void onError(String errorMessage);
    }

    /**
     * Fetches product data from the Open Food Facts staging API for a given barcode.
     *
     * @param barcode The barcode of the product to fetch.
     * @param callback The callback to handle the success or error response.
     */
    public void fetchProductData(String barcode, ProductDataCallback callback) {
        if (barcode == null || barcode.trim().isEmpty()) {
            callback.onError("Barcode cannot be empty.");
            return;
        }

        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String productJsonString;

            try {
                URL url = new URL(STAGING_API_URL + barcode + ".json");
                Log.d(TAG, "Requesting URL: " + url);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Check server response
                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned HTTP error code: " + responseCode + " for URL: " + url);
                    // Read error stream if available
                    InputStream errorStream = urlConnection.getErrorStream();
                    String errorResponse = "Server error (HTTP " + responseCode + ")";
                    if (errorStream != null) {
                        errorResponse += ": " + readStream(errorStream);
                    }
                    final String finalErrorResponse = errorResponse;
                    // Ensure callback is on main thread if updating UI, not strictly necessary here
                    // but good practice if callback were to interact with UI directly.
                    // For now, invoking directly from executor thread.
                    callback.onError(finalErrorResponse);
                    return;
                }

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    callback.onError("No data received from server.");
                    return;
                }

                productJsonString = readStream(inputStream);
                Log.d(TAG, "Raw JSON Response: " + productJsonString);

                if (productJsonString != null && !productJsonString.isEmpty()) {
                    parseProductJson(productJsonString, callback);
                } else {
                    callback.onError("Empty response from server.");
                }

            } catch (IOException e) {
                Log.e(TAG, "Error during network operation", e);
                callback.onError("Network error: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing JSON response", e);
                callback.onError("JSON parsing error: " + e.getMessage());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            buffer.append(line).append("\n");
        }
        if (buffer.length() == 0) {
            return null;
        }
        return buffer.toString();
    }

    private void parseProductJson(String jsonString, ProductDataCallback callback) throws JSONException {
        JSONObject productJson = new JSONObject(jsonString);

        // According to Open Food Facts API:
        // status: 1 if product found, 0 if not found
        int status = productJson.optInt("status", 0);
        String statusVerbose = productJson.optString("status_verbose", "unknown status");

        if (status == 1 && productJson.has("product")) {
            JSONObject product = productJson.getJSONObject("product");

            // Extract desired fields (these are common fields, verify against actual JSON structure)
            String productName = product.optString("product_name", "N/A");
            if (productName.isEmpty()) productName = product.optString("name", "N/A"); // Fallback

            String brands = product.optString("brands", "N/A");
            String quantity = product.optString("quantity", "N/A");
            String imageUrl = product.optString("image_url", null);
            // You can add more fields like ingredients_text, nutriments, categories etc.

            Log.i(TAG, "Product Name: " + productName + ", Brands: " + brands + ", Quantity: " + quantity);
            callback.onSuccess(productName, brands, quantity, imageUrl);
        } else {
            Log.w(TAG, "Product not found or status indicates error. Status verbose: " + statusVerbose);
            callback.onError("Product not found: " + statusVerbose);
        }
    }

    // Call this method when the object is no longer needed, e.g., in onDestroy of an Activity/ViewModel
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
