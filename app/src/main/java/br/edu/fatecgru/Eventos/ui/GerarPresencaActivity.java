package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;
import br.edu.fatecgru.Eventos.model.Inscricao;
import br.edu.fatecgru.Eventos.model.Usuario;

public class GerarPresencaActivity extends BaseActivity {

    private Spinner spinnerCurso, spinnerSemestre;
    private ListView listViewPresenca;
    private Button btnImprimirLista, btnGerarPdf;

    private FirebaseFirestore db;
    private String eventoId;
    private Evento evento;

    private List<Usuario> allParticipants = new ArrayList<>();
    private List<Inscricao> allInscricoes = new ArrayList<>();
    private List<String> displayedParticipants = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final int REQUEST_STORAGE_PERMISSION = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gerar_presenca);

        Toolbar toolbar = findViewById(R.id.toolbar_gerar_presenca);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Gerar Lista de Presença");
        }

        eventoId = getIntent().getStringExtra("EVENTO_ID");
        db = FirebaseFirestore.getInstance();

        spinnerCurso = findViewById(R.id.spinnerFiltroCurso);
        spinnerSemestre = findViewById(R.id.spinnerFiltroSemestre);
        listViewPresenca = findViewById(R.id.listViewPresenca);
        btnImprimirLista = findViewById(R.id.btnImprimirLista);
        btnGerarPdf = findViewById(R.id.btnGerarPdf);

        setupSpinners();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayedParticipants);
        listViewPresenca.setAdapter(adapter);

        fetchEventData();

        btnImprimirLista.setOnClickListener(v -> selectPrinterAndPrint());
        btnGerarPdf.setOnClickListener(v -> {
            if (checkAndRequestStoragePermission()) {
                gerarPdf();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    private void setupSpinners() {
        List<String> cursos = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.cursos_array)));
        cursos.remove("Selecione o Curso");
        ArrayAdapter<String> cursoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cursos);
        cursoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurso.setAdapter(cursoAdapter);

        List<String> semestres = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.semestres_array)));
        semestres.remove("Selecione o Semestre");
        ArrayAdapter<String> semestreAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semestres);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemestre.setAdapter(semestreAdapter);
        spinnerSemestre.setSelection(semestres.indexOf("Geral"));

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndDisplayParticipants();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        spinnerCurso.setOnItemSelectedListener(filterListener);
        spinnerSemestre.setOnItemSelectedListener(filterListener);
    }

    private void fetchEventData() {
        if (eventoId == null) return;

        Task<DocumentSnapshot> eventoTask = db.collection("eventos").document(eventoId).get();
        Task<QuerySnapshot> inscricoesTask = db.collection("inscricoes").whereEqualTo("idEvento", eventoId).get();
        Task<QuerySnapshot> usuariosTask = db.collection("usuarios").get();

        Tasks.whenAllSuccess(eventoTask, inscricoesTask, usuariosTask).addOnSuccessListener(results -> {
            DocumentSnapshot eventoDoc = (DocumentSnapshot) results.get(0);
            if (eventoDoc.exists()) {
                evento = eventoDoc.toObject(Evento.class);
                if (evento != null) {
                    evento.setId(eventoDoc.getId());
                }
            }
            
            allInscricoes.clear();
            for (DocumentSnapshot doc : (QuerySnapshot) results.get(1)) {
                allInscricoes.add(doc.toObject(Inscricao.class));
            }

            allParticipants.clear();
            for (DocumentSnapshot doc : (QuerySnapshot) results.get(2)) {
                Usuario u = doc.toObject(Usuario.class);
                u.setId(doc.getId());
                allParticipants.add(u);
            }

            filterAndDisplayParticipants();
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao buscar dados.", Toast.LENGTH_SHORT).show());
    }

    private void filterAndDisplayParticipants() {
        if (spinnerCurso.getSelectedItem() == null || spinnerSemestre.getSelectedItem() == null || evento == null) return;

        String cursoFiltro = spinnerCurso.getSelectedItem().toString();
        String semestreFiltro = spinnerSemestre.getSelectedItem().toString();

        displayedParticipants.clear();

        for (Inscricao inscricao : allInscricoes) {
            for (Usuario usuario : allParticipants) {
                if (inscricao.getIdUsuario().equals(usuario.getId())) {
                    
                    boolean cursoMatch = cursoFiltro.equals("Geral") || (usuario.getCurso() != null && usuario.getCurso().equals(cursoFiltro));
                    boolean semestreMatch = semestreFiltro.equals("Geral") || (usuario.getSemestre() != null && usuario.getSemestre().equals(semestreFiltro));

                    if (cursoMatch && semestreMatch) {
                        String status = "Status: Cadastrado";
                        String observacao = "";

                        if (inscricao.getHoraEntrada() != null && inscricao.getHoraSaida() != null) {
                            status = "Status: Completo";
                            
                            if (!evento.isTempoTotal()) {
                                try {
                                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                    Date dateEntrada = format.parse(inscricao.getHoraEntrada());
                                    Date dateSaida = format.parse(inscricao.getHoraSaida());
                                    long diff = dateSaida.getTime() - dateEntrada.getTime();
                                    long diffMinutes = diff / (60 * 1000);

                                    if (diffMinutes < evento.getTempoMinimo()) {
                                        observacao = " (tempo mínimo não atingido)";
                                    }
                                } catch (ParseException e) {
                                    observacao = " (erro ao calcular tempo)";
                                }
                            }
                        } else if (inscricao.getHoraEntrada() != null) {
                            status = "Status: Incompleto";
                        }
                        displayedParticipants.add("Nome: " + usuario.getNome() + "\n" + status + observacao);
                    }
                    break;
                }
            }
        }
        adapter.notifyDataSetChanged();
        if(displayedParticipants.isEmpty()){
            Toast.makeText(this, "Nenhum participante encontrado com este filtro.", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildPrintableString() {
        StringBuilder builder = new StringBuilder();
        
        if (evento != null) {
            builder.append("Evento: ").append(evento.getNome()).append("\n");
            builder.append("Descrição: ").append(evento.getDescricao()).append("\n");

            if (evento.getData() != null && evento.getData().equals(evento.getDataTermino())) {
                builder.append("Data: ").append(evento.getData()).append("\n");
                builder.append("Horário: ").append(evento.getHorario()).append(" às ").append(evento.getHorarioTermino()).append("\n");
            } else {
                builder.append("Início: ").append(evento.getData()).append(" às ").append(evento.getHorario()).append("\n");
                builder.append("Término: ").append(evento.getDataTermino()).append(" às ").append(evento.getHorarioTermino()).append("\n");
            }

            builder.append("Local: ").append(evento.getLocal()).append("\n\n");
        } else {
             builder.append("Lista de Presença\n\n");
        }

        builder.append("--- PARTICIPANTES ---\n\n");

        if (displayedParticipants.isEmpty()) {
            builder.append("Nenhum participante encontrado.\n");
        } else {
            for(String userInfo : displayedParticipants){
                builder.append(userInfo).append("\n");
                builder.append("--------------------------------\n");
            }
        }
        return builder.toString();
    }

    private void gerarPdf() {
        String text = buildPrintableString();
        if (text.isEmpty()) {
            Toast.makeText(this, "A lista está vazia.", Toast.LENGTH_SHORT).show();
            return;
        }
        createPdfFromString(text);
    }

    private void createPdfFromString(String text) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        TextPaint paint = new TextPaint();
        paint.setTextSize(12);

        int margin = 40;
        int pageContentWidth = pageInfo.getPageWidth() - 2 * margin;
        int pageContentHeight = pageInfo.getPageHeight() - 2 * margin;

        float y = margin;
        int pageNumber = 1;

        String[] lines = text.split("\n");

        for (String line : lines) {
            StaticLayout lineLayout = new StaticLayout(line, paint, pageContentWidth, android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            if (y + lineLayout.getHeight() > pageContentHeight) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            canvas.save();
            canvas.translate(margin, y);
            lineLayout.draw(canvas);
            canvas.restore();

            y += lineLayout.getHeight();
        }

        try {
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.cps_transparente);
            if (logo != null) {
                int logoWidth = 200;
                int logoHeight = (int) (logo.getHeight() * ((float) logoWidth / logo.getWidth()));
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, true);

                if (y + scaledLogo.getHeight() + 20 > pageContentHeight) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                }

                float logoX = (pageInfo.getPageWidth() - scaledLogo.getWidth()) / 2f;
                float logoY = pageInfo.getPageHeight() - scaledLogo.getHeight() - margin;
                canvas.drawBitmap(scaledLogo, logoX, logoY, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        document.finishPage(page);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String baseName = "ListaPresenca-" + (evento != null ? evento.getNome().replaceAll("[^a-zA-Z0-9]", "-") : "Geral");
            File file = new File(downloadsDir, baseName + ".pdf");
            int count = 1;
            while(file.exists()){
                file = new File(downloadsDir, baseName + "-" + count + ".pdf");
                count++;
            }

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

    private void selectPrinterAndPrint() {
        final String textToPrint = buildPrintableString();
        if (textToPrint.isEmpty()) {
            Toast.makeText(this, "A lista está vazia.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkAndRequestBluetoothPermission()) return;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth não está ativado.", Toast.LENGTH_SHORT).show();
            return;
        }

        @SuppressLint("MissingPermission")
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.isEmpty()) {
            Toast.makeText(this, "Nenhuma impressora pareada encontrada.", Toast.LENGTH_SHORT).show();
            return;
        }

        final ArrayList<BluetoothDevice> deviceList = new ArrayList<>(pairedDevices);
        final ArrayList<String> deviceNameList = new ArrayList<>();
        for (BluetoothDevice device : deviceList) {
            deviceNameList.add(device.getName());
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione a Impressora")
                .setItems(deviceNameList.toArray(new String[0]), (dialog, which) -> printToList(deviceList.get(which), textToPrint))
                .show();
    }

    @SuppressLint("MissingPermission")
    private void printToList(BluetoothDevice printerDevice, String text) {
        new Thread(() -> {
            try {
                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                try (BluetoothSocket socket = printerDevice.createRfcommSocketToServiceRecord(uuid)) {
                    socket.connect();
                    try (OutputStream outputStream = socket.getOutputStream()) {
                        outputStream.write(text.getBytes(Charset.forName("IBM850")));
                        outputStream.flush();
                    }
                    runOnUiThread(() -> Toast.makeText(GerarPresencaActivity.this, "Lista enviada para impressão!", Toast.LENGTH_SHORT).show());
                }
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(GerarPresencaActivity.this, "Erro ao imprimir: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private boolean checkAndRequestBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 1);
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_toggle_night_mode) {
            toggleNightMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleNightMode() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("NightMode", false);
        SharedPreferences.Editor editor = prefs.edit();

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("NightMode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("NightMode", true);
        }
        editor.apply();
    }
}
