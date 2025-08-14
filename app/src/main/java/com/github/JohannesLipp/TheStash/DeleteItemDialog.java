package com.github.JohannesLipp.TheStash;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

public class DeleteItemDialog extends Dialog {

    public interface DialogListener {
        void onDelete(int quantityToRemove);
    }

    private FoodItem item;
    private DialogListener listener;

    public DeleteItemDialog(Context context, FoodItem item, DialogListener listener) {
        super(context);
        this.item = item;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_item);

        EditText edtQuantity = findViewById(R.id.edtQuantity);
        Button btnDelete = findViewById(R.id.btnDelete);

        btnDelete.setOnClickListener(v -> {
            int quantityToRemove = Integer.parseInt(edtQuantity.getText().toString());
            listener.onDelete(quantityToRemove);
            dismiss();
        });
    }
}