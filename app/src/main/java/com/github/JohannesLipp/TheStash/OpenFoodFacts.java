package com.github.JohannesLipp.TheStash;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;

public class OpenFoodFacts {

    private static final String TAG = "OpenFoodFacts";
    // Staging API base URL for products:
    private static final String STAGING_API_URL = "https://staging.openfoodfacts.org/api/v2/product/";
    // For V3, it would be "https://staging.openfoodfacts.org/api/v3/product/" - adjust if needed based on exact endpoint

    public interface ProductDataCallback {
        void onSuccess(OpenFoodFactsResultDTO results);

        void onError(String errorMessage);
    }

    public static void fetchProductData(FoodItem foodItem, ExecutorService executorService, ProductDataCallback callback) {
        if (foodItem.getBarcode() == null || foodItem.getBarcode().trim().isEmpty()) {
            callback.onError("Barcode cannot be empty.");
            return;
        }

        Log.d(TAG, "Fetching product data for barcode: " + foodItem.getBarcode());
        executorService.execute(() -> {
            HttpURLConnection urlConnection = null;
            String productJsonString;

            try {
                URL url = new URL(STAGING_API_URL + foodItem.getBarcode() + ".json");
                Log.d(TAG, "Requesting URL: " + url);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Check server response
                Log.d(TAG, "Response code: " + urlConnection.getResponseCode());
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

                Log.d(TAG, "Successfully connected to server.");
                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    Log.e(TAG, "No data received from server.");
                    callback.onError("No data received from server.");
                    return;
                }

                productJsonString = readStream(inputStream);
                Log.d(TAG, "Raw JSON Response: " + productJsonString);

                if (productJsonString != null && !productJsonString.isEmpty()) {
                    Log.d(TAG, "Parsing JSON response...");
                    parseProductJson(productJsonString, callback);
                } else {
                    Log.e(TAG, "Empty response from server.");
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

    private static String readStream(InputStream inputStream) throws IOException {
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

    private static void parseProductJson(String jsonString, ProductDataCallback callback) throws JSONException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // Read the entire JSON string into a Jackson JsonNode
            JsonNode rootNode = objectMapper.readTree(jsonString);
            Log.d(TAG, "JSON root node: " + rootNode);

            // Check status (using Jackson's navigation)
            int status = rootNode.path("status").asInt(0);
            String statusVerbose = rootNode.path("status_verbose").asText("unknown status");
            Log.d(TAG, "Status: " + status + ", Status verbose: " + statusVerbose);

            if (status == 1) {
                JsonNode productNode = rootNode.path("product"); // Get the "product" node
                if (productNode.isMissingNode() || !productNode.isObject()) {
                    Log.w(TAG, "Product node is missing or not an object. Status verbose: " + statusVerbose);
                    callback.onError("Product data structure error: " + statusVerbose);
                    return;
                }

                Log.d(TAG, "Deserializing product node: " + productNode);
                OpenFoodFactsResultDTO productDTO = objectMapper.treeToValue(productNode, OpenFoodFactsResultDTO.class);
                Log.d(TAG, "Successfully deserialized product: " + productDTO);

                callback.onSuccess(productDTO);
            } else {
                Log.w(TAG, "Product not found or status indicates error. Status verbose: " + statusVerbose);
                callback.onError("Product not found: " + statusVerbose);
            }
        } catch (JsonProcessingException e) {
            Log.e(TAG, "Jackson JSON processing error", e);
            callback.onError("JSON parsing error: " + e.getMessage());
        }
    }
}
