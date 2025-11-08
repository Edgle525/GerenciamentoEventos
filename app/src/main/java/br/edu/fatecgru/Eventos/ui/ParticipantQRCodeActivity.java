package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.util.PrinterUtils;

public class ParticipantQRCodeActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private ImageView ivQRCodeEntrada, ivQRCodeSaida;
    private Button btnImprimir;
    private BluetoothAdapter bluetoothAdapter;
    private Bitmap qrCodeEntradaBitmap, qrCodeSaidaBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participant_qrcode);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ivQRCodeEntrada = findViewById(R.id.ivQRCodeEntrada);
        ivQRCodeSaida = findViewById(R.id.ivQRCodeSaida);
        btnImprimir = findViewById(R.id.btnImprimir);

        long eventoId = getIntent().getLongExtra("evento_id", -1);
        long userId = getIntent().getLongExtra("user_id", -1);

        String infoEntrada = "ENTRADA;" + eventoId + ";" + userId;
        String infoSaida = "SAIDA;" + eventoId + ";" + userId;

        try {
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrixEntrada = multiFormatWriter.encode(infoEntrada, BarcodeFormat.QR_CODE, 250, 250);
            BarcodeEncoder barcodeEncoderEntrada = new BarcodeEncoder();
            qrCodeEntradaBitmap = barcodeEncoderEntrada.createBitmap(bitMatrixEntrada);
            ivQRCodeEntrada.setImageBitmap(qrCodeEntradaBitmap);

            BitMatrix bitMatrixSaida = multiFormatWriter.encode(infoSaida, BarcodeFormat.QR_CODE, 250, 250);
            BarcodeEncoder barcodeEncoderSaida = new BarcodeEncoder();
            qrCodeSaidaBitmap = barcodeEncoderSaida.createBitmap(bitMatrixSaida);
            ivQRCodeSaida.setImageBitmap(qrCodeSaidaBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
        final ArrayList<String> deviceNameList = new ArrayList<>();

        for (BluetoothDevice device : deviceList) {
            deviceNameList.add(device.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecione a Impressora");
        builder.setItems(deviceNameList.toArray(new String[0]), (dialog, which) -> {
            BluetoothDevice selectedDevice = deviceList.get(which);
            printToDevice(selectedDevice);
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void printToDevice(BluetoothDevice printerDevice) {
        new Thread(() -> {
            BluetoothSocket bluetoothSocket = null;
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> Toast.makeText(ParticipantQRCodeActivity.this, "Permissão de Bluetooth negada", Toast.LENGTH_SHORT).show());
                    return;
                }

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                bluetoothSocket = printerDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                OutputStream outputStream = bluetoothSocket.getOutputStream();

                byte[] selectPortuguese = {0x1B, 0x74, 3};
                Charset charset = Charset.forName("CP860");

                String entradaText = "Leia para registrar a entrada\n";
                outputStream.write(selectPortuguese);
                outputStream.write(entradaText.getBytes(charset));
                if (qrCodeEntradaBitmap != null) {
                    writeBitmapInChunks(outputStream, qrCodeEntradaBitmap);
                }

                String saidaText = "\nLeia para registrar a saida\n";
                outputStream.write(selectPortuguese);
                outputStream.write(saidaText.getBytes(charset));
                if (qrCodeSaidaBitmap != null) {
                    writeBitmapInChunks(outputStream, qrCodeSaidaBitmap);
                }

                outputStream.flush();
                runOnUiThread(() -> Toast.makeText(ParticipantQRCodeActivity.this, "Impresso com sucesso!", Toast.LENGTH_SHORT).show());

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(ParticipantQRCodeActivity.this, "Erro ao imprimir: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                if (bluetoothSocket != null) {
                    try {
                        bluetoothSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void writeBitmapInChunks(OutputStream stream, Bitmap bitmap) throws IOException, InterruptedException {
        byte[] command = PrinterUtils.decodeBitmap(bitmap);
        if (command == null) return;

        int chunkSize = 256;
        for (int i = 0; i < command.length; i += chunkSize) {
            int end = Math.min(i + chunkSize, command.length);
            stream.write(command, i, end - i);
            stream.flush();
            Thread.sleep(100);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
