package com.github.JohannesLipp.TheStash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class AddItemDialog extends Dialog {

    public interface DialogListener {
        void onSave(String barcode, int month, int year, int quantity);
    }

    private final DialogListener listener;

    public AddItemDialog(Context context, DialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_item);

        EditText edtBarcode = findViewById(R.id.edtBarcode);
        EditText edtMonth = findViewById(R.id.edtMonth);
        EditText edtYear = findViewById(R.id.edtYear);
        EditText edtQuantity = findViewById(R.id.edtQuantity);
        Button btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String barcode = edtBarcode.getText().toString();
            int month = Integer.parseInt(edtMonth.getText().toString());
            int year = Integer.parseInt(edtYear.getText().toString());
            int quantity = Integer.parseInt(edtQuantity.getText().toString());

            listener.onSave(barcode, month, year, quantity);
            dismiss();
        });
    }
}