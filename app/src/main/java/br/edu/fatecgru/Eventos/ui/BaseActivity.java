package br.edu.fatecgru.Eventos.ui;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.edu.fatecgru.Eventos.R;

public abstract class BaseActivity extends AppCompatActivity {

    private ConnectivityManager.NetworkCallback networkCallback;
    private AlertDialog offlineDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupOfflineDialog();
    }

    private void setupOfflineDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_offline, null);
        builder.setView(dialogView);
        builder.setCancelable(false);
        offlineDialog = builder.create();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerNetworkCallback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterNetworkCallback();
    }

    private void registerNetworkCallback() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        checkInitialNetworkState(cm);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                runOnUiThread(() -> {
                    if (offlineDialog.isShowing()) {
                        offlineDialog.dismiss();
                    }
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);
                runOnUiThread(() -> {
                    if (!offlineDialog.isShowing()) {
                        offlineDialog.show();
                    }
                });
            }
        };
        cm.registerDefaultNetworkCallback(networkCallback);
    }

    private void unregisterNetworkCallback() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null && networkCallback != null) {
            cm.unregisterNetworkCallback(networkCallback);
        }
    }

    private void checkInitialNetworkState(ConnectivityManager cm) {
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (capabilities == null || !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            if (!offlineDialog.isShowing()) {
                offlineDialog.show();
            }
        } else {
            if (offlineDialog.isShowing()) {
                offlineDialog.dismiss();
            }
        }
    }
}
