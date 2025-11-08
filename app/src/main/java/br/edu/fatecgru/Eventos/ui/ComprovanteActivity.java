package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;
import br.edu.fatecgru.Eventos.model.Inscricao;

public class ComprovanteActivity extends BaseActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;

    private Button btnImprimir, btnBaixarPdf;
    private FirebaseFirestore db;
    private String eventoId, userIdLogado, nomeEvento, dataEvento, horarioEvento, nomeParticipante, emailParticipante;
    private String curso, semestre;
    private BluetoothAdapter bluetoothAdapter;
    private int tempoMinimoEvento = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprovante);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnImprimir = findViewById(R.id.btnImprimirComprovante);
        btnBaixarPdf = findViewById(R.id.btnBaixarPdf);
        db = FirebaseFirestore.getInstance();

        getIntentData();
        fetchUserData();
        fetchEventData();

        btnImprimir.setOnClickListener(v -> {
            if (checkAndRequestBluetoothPermission()) {
                selectPrinterAndPrint();
            }
        });

        btnBaixarPdf.setOnClickListener(v -> {
            if (checkAndRequestStoragePermission()) {
                gerarPdf();
            }
        });
    }

    private void getIntentData(){
        eventoId = getIntent().getStringExtra("evento_id");
        userIdLogado = getIntent().getStringExtra("user_id");
        nomeEvento = getIntent().getStringExtra("nome_evento");
        dataEvento = getIntent().getStringExtra("data_evento");
        horarioEvento = getIntent().getStringExtra("horario_evento");
        nomeParticipante = getIntent().getStringExtra("nome_participante");
        emailParticipante = getIntent().getStringExtra("email_participante");
    }

    private void fetchUserData(){
        if (userIdLogado != null) {
            db.collection("usuarios").document(userIdLogado).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        curso = documentSnapshot.getString("curso");
                        semestre = documentSnapshot.getString("semestre");
                    } else {
                         Toast.makeText(this, "Dados de curso e semestre não encontrados.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->  Toast.makeText(this, "Falha ao buscar dados de curso e semestre.", Toast.LENGTH_SHORT).show());
        }
    }

    private void fetchEventData() {
        if (eventoId != null) {
            db.collection("eventos").document(eventoId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Evento evento = documentSnapshot.toObject(Evento.class);
                            if (evento != null) {
                                tempoMinimoEvento = evento.getTempoMinimo();
                            }
                        }
                    });
        }
    }

    private String buildProof(Inscricao inscricao) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        long diff = 0;
        try {
            Date dateEntrada = format.parse(inscricao.getHoraEntrada());
            Date dateSaida = format.parse(inscricao.getHoraSaida());
            if (dateEntrada == null || dateSaida == null) throw new ParseException("Data de entrada ou saída nula", 0);
            diff = dateSaida.getTime() - dateEntrada.getTime();
        } catch (ParseException | NullPointerException e) {
            runOnUiThread(() -> Toast.makeText(ComprovanteActivity.this, "Erro ao calcular a duração do evento.", Toast.LENGTH_SHORT).show());
            return null;
        }

        long diffMinutes = diff / (60 * 1000);
        String tempoPermanencia = String.format(Locale.getDefault(), "%d minutos", diffMinutes);

        String observacao = "";
        if (diffMinutes < tempoMinimoEvento) {
            observacao = "\nObservação: O usuário não cumpriu o tempo minimo de permanência no evento.\n";
        }

        return "--- COMPROVANTE DE PARTICIPACAO ---\n\n" +
                "Participante: " + nomeParticipante + "\n" +
                "E-mail: " + emailParticipante + "\n" +
                "Curso: " + (curso != null ? curso : "N/A") + "\n" +
                "Semestre: " + (semestre != null ? semestre : "N/A") + "\n\n" +
                "Evento: " + nomeEvento + "\n" +
                "Data: " + dataEvento + " " + horarioEvento + "\n\n" +
                "Entrada: " + inscricao.getHoraEntrada() + "\n" +
                "Saida: " + inscricao.getHoraSaida() + "\n" +
                "Permanencia: " + tempoPermanencia + "\n" +
                observacao + "\n" +
                "----------------------------------\n\n";
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

    private boolean checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private void gerarPdf() {
        db.collection("inscricoes").document(userIdLogado + "_" + eventoId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Inscricao inscricao = documentSnapshot.toObject(Inscricao.class);
                    if (inscricao != null && inscricao.getHoraEntrada() != null && inscricao.getHoraSaida() != null) {
                        String proofText = buildProof(inscricao);
                        if (proofText != null) {
                            createPdfFromString(proofText);
                        }
                    } else {
                        Toast.makeText(this, "Registro de entrada e/ou saída incompleto.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Inscrição não encontrada.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> Toast.makeText(this, "Erro ao buscar inscrição.", Toast.LENGTH_LONG).show());
    }

    private void createPdfFromString(String text) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        float y = 40;
        for (String line : text.split("\n")) {
            canvas.drawText(line, 40, y, paint);
            y += paint.descent() - paint.ascent();
        }

        document.finishPage(page);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, "Comprovante-" + nomeEvento.replaceAll("[^a-zA-Z0-9]", "-") + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();
            Toast.makeText(this, "PDF salvo em Downloads!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @SuppressLint("MissingPermission")
    private void selectPrinterAndPrint() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não suportado neste dispositivo", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "O Bluetooth não está ativado", Toast.LENGTH_SHORT).show();
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
        String docId = userIdLogado + "_" + eventoId;

        db.collection("inscricoes").document(docId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        runOnUiThread(() -> Toast.makeText(ComprovanteActivity.this, "Inscrição não encontrada.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    Inscricao inscricao = documentSnapshot.toObject(Inscricao.class);
                    if (inscricao == null || inscricao.getHoraEntrada() == null || inscricao.getHoraSaida() == null) {
                        runOnUiThread(() -> Toast.makeText(ComprovanteActivity.this, "Registro de entrada e/ou saída incompleto.", Toast.LENGTH_LONG).show());
                        return;
                    }

                    final String proofText = buildProof(inscricao);
                    if (proofText == null) return;

                    new Thread(() -> {
                        try {
                            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                            try (BluetoothSocket socket = printerDevice.createRfcommSocketToServiceRecord(uuid)) {
                                socket.connect();
                                try (OutputStream outputStream = socket.getOutputStream()) {
                                    outputStream.write(new byte[]{0x1B, 0x74, 2});
                                    outputStream.write(proofText.getBytes("CP850"));
                                    outputStream.flush();
                                }
                                runOnUiThread(() -> Toast.makeText(ComprovanteActivity.this, "Comprovante enviado para impressão!", Toast.LENGTH_SHORT).show());
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(ComprovanteActivity.this, "Erro ao conectar ou imprimir: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .addOnFailureListener(e -> runOnUiThread(() -> Toast.makeText(ComprovanteActivity.this, "Erro ao buscar inscrição: " + e.getMessage(), Toast.LENGTH_LONG).show()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectPrinterAndPrint();
            } else {
                Toast.makeText(this, "Permissão de Bluetooth negada. Não é possível imprimir.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                gerarPdf();
            } else {
                Toast.makeText(this, "Permissão de armazenamento negada. Não é possível salvar o PDF.", Toast.LENGTH_SHORT).show();
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
