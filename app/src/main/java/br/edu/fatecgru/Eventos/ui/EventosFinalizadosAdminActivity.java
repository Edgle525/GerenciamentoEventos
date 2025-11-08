package br.edu.fatecgru.Eventos.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.fatecgru.Eventos.R;

public class EventosFinalizadosAdminActivity extends AppCompatActivity {

    private static final String TAG = "EventosFinalizadosAdmin";
    private ListView listViewEventosFinalizadosAdmin;
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> nomesEventos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventos_finalizados_admin);

        Toolbar toolbar = findViewById(R.id.toolbar_eventos_finalizados_admin);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        listViewEventosFinalizadosAdmin = findViewById(R.id.listViewEventosFinalizadosAdmin);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesEventos);
        listViewEventosFinalizadosAdmin.setAdapter(adapter);

        loadEventosFinalizados();
    }

    private void loadEventosFinalizados() {
        db.collection("eventos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                nomesEventos.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date agora = new Date();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Map<String, Object> data = document.getData();

                        String nome = (data.get("nome") instanceof String) ? (String) data.get("nome") : null;
                        if (nome == null) {
                            Log.w(TAG, "Evento sem nome ou com formato inválido, pulando: " + document.getId());
                            continue;
                        }

                        String dataTerminoStr = (data.get("dataTermino") instanceof String) ? (String) data.get("dataTermino") : null;
                        String horarioTerminoStr = (data.get("horarioTermino") instanceof String) ? (String) data.get("horarioTermino") : null;

                        if (dataTerminoStr != null && !dataTerminoStr.isEmpty() && horarioTerminoStr != null && !horarioTerminoStr.isEmpty()) {
                            try {
                                Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);
                                if (dataTermino.before(agora)) {
                                    nomesEventos.add(nome);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Erro ao parsear data/hora para o evento: " + document.getId(), e);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro crítico ao processar evento: " + document.getId(), e);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Log.e(TAG, "Erro ao buscar eventos.", task.getException());
                Toast.makeText(EventosFinalizadosAdminActivity.this, "Erro ao carregar eventos finalizados.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
