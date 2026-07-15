package com.example.douyinreportadmin;

import android.text.Editable;
import android.text.TextWatcher;

final class SimpleTextWatcher implements TextWatcher {
    private final Runnable afterChange;

    SimpleTextWatcher(Runnable afterChange) {
        this.afterChange = afterChange;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        afterChange.run();
    }
}
