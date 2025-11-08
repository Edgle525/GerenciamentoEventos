package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.util.PrinterUtils;

public class EventQRCodeActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;

    private ImageView ivQRCodeEntrada, ivQRCodeSaida;
    private TextView tvEventName;
    private Button btnImprimir;
    private Bitmap qrCodeEntradaBitmap, qrCodeSaidaBitmap;
    private String eventoNome;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_qrcode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivQRCodeEntrada = findViewById(R.id.ivQRCodeEntrada);
        ivQRCodeSaida = findViewById(R.id.ivQRCodeSaida);
        tvEventName = findViewById(R.id.tvEventNameQRCode);
        btnImprimir = findViewById(R.id.btnImprimirQRCodes);

        String eventoId = getIntent().getStringExtra("evento_id");
        eventoNome = getIntent().getStringExtra("nome_evento");
        String eventoData = getIntent().getStringExtra("data_evento");
        String eventoHorario = getIntent().getStringExtra("horario_evento");

        if (eventoId == null || eventoNome == null || eventoData == null || eventoHorario == null) {
            Toast.makeText(this, "Erro ao receber dados completos do evento.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        tvEventName.setText(eventoNome);

        String infoEntrada = "ENTRADA;" + eventoId + ";" + eventoNome + ";" + eventoData + ";" + eventoHorario;
        String infoSaida = "SAIDA;" + eventoId + ";" + eventoNome + ";" + eventoData + ";" + eventoHorario;

        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

            final int QR_CODE_SIZE = 256;

            BitMatrix bitMatrixEntrada = multiFormatWriter.encode(infoEntrada, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
            qrCodeEntradaBitmap = barcodeEncoder.createBitmap(bitMatrixEntrada);
            ivQRCodeEntrada.setImageBitmap(qrCodeEntradaBitmap);

            BitMatrix bitMatrixSaida = multiFormatWriter.encode(infoSaida, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
            qrCodeSaidaBitmap = barcodeEncoder.createBitmap(bitMatrixSaida);
            ivQRCodeSaida.setImageBitmap(qrCodeSaidaBitmap);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_PERMISSION);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BLUETOOTH_PERMISSION);
                return false;
            }
        }
        return true;
    }

    @SuppressLint("MissingPermission")
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

        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "Nenhuma impressora pareada encontrada", Toast.LENGTH_SHORT).show();
            return;
        }

        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
        final ArrayList<String> deviceNameList = new ArrayList<>();
        for (BluetoothDevice device : deviceList) {
            deviceNameList.add(device.getName());
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione a Impressora")
                .setItems(deviceNameList.toArray(new String[0]), (dialog, which) -> printToDevice(deviceList.get(which)))
                .show();
    }

    @SuppressLint("MissingPermission")
    private void printToDevice(BluetoothDevice printerDevice) {
        new Thread(() -> {
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                try (BluetoothSocket socket = printerDevice.createRfcommSocketToServiceRecord(uuid)) {
                    socket.connect();
                    try (OutputStream outputStream = socket.getOutputStream()) {
                        String title = "QR Codes para o evento: " + eventoNome + "\n\n";
                        outputStream.write(title.getBytes());

                        byte[] qrEntradaBytes = PrinterUtils.decodeBitmap(qrCodeEntradaBitmap);
                        outputStream.write("ENTRADA:\n".getBytes());
                        sendDataInChunks(outputStream, qrEntradaBytes);
                        outputStream.write("\n\n".getBytes());

                        byte[] qrSaidaBytes = PrinterUtils.decodeBitmap(qrCodeSaidaBitmap);
                        outputStream.write("SAIDA:\n".getBytes());
                        sendDataInChunks(outputStream, qrSaidaBytes);
                        outputStream.write("\n\n\n".getBytes());
                        
                        outputStream.flush();
                    }
                    runOnUiThread(() -> Toast.makeText(EventQRCodeActivity.this, "QR Codes enviados para impressão!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(EventQRCodeActivity.this, "Erro ao imprimir: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void sendDataInChunks(OutputStream outputStream, byte[] data) throws IOException, InterruptedException {
        final int chunkSize = 256; // Aumentado para pacotes maiores
        for (int i = 0; i < data.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, data.length);
            outputStream.write(Arrays.copyOfRange(data, i, end));
            outputStream.flush();
            Thread.sleep(80); // Pausa de 80ms
        }
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
