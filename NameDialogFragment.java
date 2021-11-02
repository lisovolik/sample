package com.evos.whitelabelblank.mvp.neworder.name;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;

import com.evos.whitelabelblank.MainApplication;
import com.evos.whitelabelblank.R;
import com.evos.whitelabelblank.mvp.neworder.name.NamePresenter.NameView;

import javax.inject.Inject;

/**
 *
 */

public class NameDialogFragment extends AppCompatDialogFragment implements NameView {

    private EditText editTextName;
    private Button buttonOK, buttonCancel;

    @Inject
    NamePresenter presenter;

    @NonNull
    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public void onAttach(final Context context) {
        ((MainApplication) context.getApplicationContext()).getNewOrderComponent().inject(this);
        super.onAttach(context);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.dialog_name, container, false);

        editTextName = result.findViewById(R.id.dialog_name_editText);
        editTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
                presenter.onNameChanged(s.toString());
            }
        });

        buttonCancel = result.findViewById(R.id.dialog_name_cancelButton);
        buttonCancel.setOnClickListener(v -> presenter.onCancelClick());

        buttonOK = result.findViewById(R.id.dialog_name_okButton);
        buttonOK.setOnClickListener(v -> presenter.onOkClick(editTextName.getText().toString()));

        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onViewAttached(this);
    }

    @Override
    public void onPause() {
        presenter.onViewDetached();
        super.onPause();
        dismiss();
    }

    @Override
    public void showOK() {
        dismiss();
    }

    @Override
    public void showCancel() {
        dismiss();
    }

    @Override
    public void hideOKButton() {
        buttonOK.setVisibility(View.GONE);
        buttonCancel.setBackgroundResource(R.drawable.shape_dialog_button_single);
    }

    @Override
    public void showOKButton() {
        buttonOK.setVisibility(View.VISIBLE);
        buttonCancel.setBackgroundResource(R.drawable.shape_dialog_button_left);
    }
}
