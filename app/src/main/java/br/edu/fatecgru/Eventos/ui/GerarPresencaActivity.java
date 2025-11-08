package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Inscricao;
import br.edu.fatecgru.Eventos.model.Usuario;

public class GerarPresencaActivity extends BaseActivity {

    private Spinner spinnerCurso, spinnerSemestre;
    private ListView listViewPresenca;
    private Button btnImprimirLista;

    private FirebaseFirestore db;
    private String eventoId;

    private List<Usuario> allParticipants = new ArrayList<>();
    private List<Inscricao> allInscricoes = new ArrayList<>();
    private List<String> displayedParticipants = new ArrayList<>();
    private ArrayAdapter<String> adapter;

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

        setupSpinners();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayedParticipants);
        listViewPresenca.setAdapter(adapter);

        fetchEventData();

        btnImprimirLista.setOnClickListener(v -> selectPrinterAndPrint());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> cursoAdapter = ArrayAdapter.createFromResource(this,
                R.array.cursos_array, android.R.layout.simple_spinner_item);
        cursoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurso.setAdapter(cursoAdapter);

        ArrayAdapter<CharSequence> semestreAdapter = ArrayAdapter.createFromResource(this,
                R.array.semestres_array, android.R.layout.simple_spinner_item);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemestre.setAdapter(semestreAdapter);

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

        Task<QuerySnapshot> inscricoesTask = db.collection("inscricoes").whereEqualTo("idEvento", eventoId).get();
        Task<QuerySnapshot> usuariosTask = db.collection("usuarios").get();

        Tasks.whenAllSuccess(inscricoesTask, usuariosTask).addOnSuccessListener(results -> {
            allInscricoes.clear();
            for (DocumentSnapshot doc : (QuerySnapshot) results.get(0)) {
                allInscricoes.add(doc.toObject(Inscricao.class));
            }

            allParticipants.clear();
            for (DocumentSnapshot doc : (QuerySnapshot) results.get(1)) {
                Usuario u = doc.toObject(Usuario.class);
                u.setId(doc.getId());
                allParticipants.add(u);
            }

            filterAndDisplayParticipants();
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao buscar dados.", Toast.LENGTH_SHORT).show());
    }

    private void filterAndDisplayParticipants() {
        String cursoFiltro = spinnerCurso.getSelectedItem().toString();
        String semestreFiltro = spinnerSemestre.getSelectedItem().toString();

        displayedParticipants.clear();

        for (Inscricao inscricao : allInscricoes) {
            for (Usuario usuario : allParticipants) {
                if (inscricao.getIdUsuario().equals(usuario.getId())) {
                    
                    boolean cursoMatch = cursoFiltro.equals("Geral") || cursoFiltro.equals("Selecione o Curso") || (usuario.getCurso() != null && usuario.getCurso().equals(cursoFiltro));
                    boolean semestreMatch = semestreFiltro.equals("Geral") || semestreFiltro.equals("Selecione o Semestre") || (usuario.getSemestre() != null && usuario.getSemestre().equals(semestreFiltro));

                    if (cursoMatch && semestreMatch) {
                        String status = "Status: Cadastrado";
                        if (inscricao.getHoraEntrada() != null && inscricao.getHoraSaida() != null) {
                            status = "Status: Completo";
                        } else if (inscricao.getHoraEntrada() != null) {
                            status = "Status: Incompleto";
                        }
                        displayedParticipants.add("Nome: " + usuario.getNome() + "\n" + status);
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
        String cursoFiltro = spinnerCurso.getSelectedItem().toString();
        String semestreFiltro = spinnerSemestre.getSelectedItem().toString();

        builder.append("Lista de Presenca\n");
        builder.append("Curso: ").append(cursoFiltro).append("\n");
        builder.append("Semestre: ").append(semestreFiltro).append("\n\n");

        for(String userInfo : displayedParticipants){
            builder.append(userInfo).append("\n");
            builder.append("--------------------------------\n");
        }
        return builder.toString();
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
                        outputStream.write(text.getBytes(StandardCharsets.UTF_8));
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
