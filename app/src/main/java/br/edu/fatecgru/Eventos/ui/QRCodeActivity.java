package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.util.PrinterUtils;

public class QRCodeActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private ImageView ivQRCodeEntrada, ivQRCodeSaida;
    private Button btnImprimir;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivQRCodeEntrada = findViewById(R.id.ivQRCodeEntrada);
        ivQRCodeSaida = findViewById(R.id.ivQRCodeSaida);
        btnImprimir = findViewById(R.id.btnImprimir);

        String eventoId = getIntent().getStringExtra("evento_id");
        String nomeEvento = getIntent().getStringExtra("nome_evento");
        String dataEvento = getIntent().getStringExtra("data_evento");
        String horarioEvento = getIntent().getStringExtra("horario_evento");

        String infoEntrada = "ENTRADA;" + eventoId + ";" + nomeEvento + ";" + dataEvento + ";" + horarioEvento;
        String infoSaida = "SAIDA;" + eventoId + ";" + nomeEvento + ";" + dataEvento + ";" + horarioEvento;

        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

            BitMatrix bitMatrixEntrada = multiFormatWriter.encode(infoEntrada, BarcodeFormat.QR_CODE, 300, 300);
            Bitmap bitmapEntrada = barcodeEncoder.createBitmap(bitMatrixEntrada);
            ivQRCodeEntrada.setImageBitmap(bitmapEntrada);

            BitMatrix bitMatrixSaida = multiFormatWriter.encode(infoSaida, BarcodeFormat.QR_CODE, 300, 300);
            Bitmap bitmapSaida = barcodeEncoder.createBitmap(bitMatrixSaida);
            ivQRCodeSaida.setImageBitmap(bitmapSaida);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao gerar QR Codes", Toast.LENGTH_SHORT).show();
        }

        btnImprimir.setOnClickListener(v -> {
            if (checkAndRequestBluetoothPermission()) {
                selectPrinterAndPrint();
            }
        });
    }

    private boolean checkAndRequestBluetoothPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
            return false;
        }
        return true;
    }

    private void selectPrinterAndPrint() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não suportado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Por favor, ative o Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
        final ArrayList<String> deviceNameList = new ArrayList<>();
        for (BluetoothDevice device : deviceList) {
            deviceNameList.add(device.getName());
        }

        if (deviceNameList.isEmpty()) {
            Toast.makeText(this, "Nenhuma impressora pareada encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione a Impressora")
                .setItems(deviceNameList.toArray(new String[0]), (dialog, which) -> printToDevice(deviceList.get(which)))
                .show();
    }

    private void printToDevice(BluetoothDevice printerDevice) {
        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                BluetoothSocket socket = printerDevice.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                OutputStream outputStream = socket.getOutputStream();

                BitmapDrawable drawableEntrada = (BitmapDrawable) ivQRCodeEntrada.getDrawable();
                Bitmap bitmapEntrada = drawableEntrada.getBitmap();

                BitmapDrawable drawableSaida = (BitmapDrawable) ivQRCodeSaida.getDrawable();
                Bitmap bitmapSaida = drawableSaida.getBitmap();

                outputStream.write(PrinterUtils.decodeBitmap(bitmapEntrada));
                outputStream.write("\n\n".getBytes());
                outputStream.write(PrinterUtils.decodeBitmap(bitmapSaida));
                outputStream.write("\n\n".getBytes());

                outputStream.flush();
                socket.close();

                runOnUiThread(() -> Toast.makeText(QRCodeActivity.this, "QR Codes enviados para impressão!", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(QRCodeActivity.this, "Erro ao imprimir: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectPrinterAndPrint();
            } else {
                Toast.makeText(this, "Permissão de Bluetooth negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
