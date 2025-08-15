package com.github.JohannesLipp.TheStash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.FragmentManager;

import com.github.dewinjm.monthyearpicker.MonthFormat;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialog;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;

import java.util.Calendar;

public class AddItemDialog extends Dialog {

    public interface DialogListener {
        void onSave(String barcode, int month, int year, int quantity);
    }

    private final DialogListener listener;
    private final String scannedBarcode;

    public AddItemDialog(Context context, String scannedBarcode, DialogListener listener) {
        super(context);
        this.scannedBarcode = scannedBarcode;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_item);

        EditText edtBarcode = findViewById(R.id.edtBarcode);
        EditText edtExpires = findViewById(R.id.edtExpires);
        EditText edtQuantity = findViewById(R.id.edtQuantity);
        Button btnSave = findViewById(R.id.btnSave);


        edtExpires.setOnClickListener(v -> {
            //Set default values
            Calendar calendar = Calendar.getInstance();

            MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment
                    .getInstance(calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.YEAR),
                            "Select EXP Date",
                            MonthFormat.SHORT);

            dialogFragment.setOnDateSetListener((year, monthOfYear) -> edtExpires.setText(monthOfYear + "/" + year));
            dialogFragment.show(dialogFragment.getParentFragmentManager(), null);
        });

        // Prefill barcode from scanner, if present
        if (scannedBarcode != null && !scannedBarcode.isEmpty()) {
            edtBarcode.setText(scannedBarcode);
        }

        btnSave.setOnClickListener(v -> {
            String barcode = edtBarcode.getText().toString().trim();
            int month = Integer.parseInt(edtExpires.getText().toString().trim().split("/")[0]);
            int year = Integer.parseInt(edtExpires.getText().toString().trim().split("/")[1]);
            int quantity = Integer.parseInt(edtQuantity.getText().toString().trim());

            listener.onSave(barcode, month, year, quantity);
            dismiss();
        });
    }
}