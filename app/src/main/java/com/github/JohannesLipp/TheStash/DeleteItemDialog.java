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

    private final DialogListener listener;

    public DeleteItemDialog(Context context, DialogListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_delete_item);

        EditText edtQuantity = findViewById(R.id.edtQuantity);
        Button btnDelete = findViewById(R.id.btnDelete);

        btnDelete.setOnClickListener(v -> {
            int quantityToRemove = Integer.parseInt(edtQuantity.getText().toString().trim());
            listener.onDelete(quantityToRemove);
            dismiss();
        });
    }
}