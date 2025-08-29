package com.github.JohannesLipp.TheStash;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;

public class AddItemDialog extends Dialog {

    public interface DialogListener {
        void onSave(String barcode, int day, int month, int year, int quantity);
    }

    private final String scannedBarcode;
    private final DialogListener listener;

    public AddItemDialog(Context context, @Nullable String scannedBarcode, DialogListener listener) {
        super(context);
        this.scannedBarcode = scannedBarcode;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_item);

        Window window = this.getWindow();
        if (window != null) {
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }

        EditText edtBarcode = findViewById(R.id.edtBarcode);
        EditText edtExpires = findViewById(R.id.edtExpires);
        Button btnSelectDate = findViewById(R.id.btnSelectDate);
        EditText edtQuantity = findViewById(R.id.edtQuantity);
        Button btnSave = findViewById(R.id.btnSave);

        btnSelectDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();

            DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                    (view, year, month, day) -> edtExpires.setText(String.format(Locale.GERMANY, "%02d.%02d.%04d", day, month + 1, year)),
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        // Prefill barcode from scanner, if present
        if (scannedBarcode != null && !scannedBarcode.isEmpty()) {
            edtBarcode.setText(scannedBarcode);
        }

        btnSave.setOnClickListener(v -> {
            String barcode = edtBarcode.getText().toString().trim();
            int day = Integer.parseInt(edtExpires.getText().toString().trim().split("\\.")[0]);
            int month = Integer.parseInt(edtExpires.getText().toString().trim().split("\\.")[1]);
            int year = Integer.parseInt(edtExpires.getText().toString().trim().split("\\.")[2]);
            int quantity = Integer.parseInt(edtQuantity.getText().toString().trim());

            listener.onSave(barcode, day, month, year, quantity);
            dismiss();
        });
    }
}