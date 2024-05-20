package com.forcastflow.myapplication;

import android.os.Bundle;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class GlasgowEnglandThreeDay extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glasgow_england_threedays);

        Window window = getWindow();
        window.setNavigationBarColor(ContextCompat.getColor(this, android.R.color.white));
        window.setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));

        // Ensure icons and text are visible on a white background
        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                        View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
    }
}
