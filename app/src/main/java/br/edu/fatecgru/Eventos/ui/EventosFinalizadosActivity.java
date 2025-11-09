package br.edu.fatecgru.Eventos.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class EventosFinalizadosActivity extends BaseActivity {

    private static final String TAG = "EventosFinalizados";
    private ListView listViewEventosFinalizados;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ArrayAdapter<String> adapter;
    private List<String> nomesEventos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eventos_finalizados);

        Toolbar toolbar = findViewById(R.id.toolbar_eventos_finalizados);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Eventos Finalizados");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        listViewEventosFinalizados = findViewById(R.id.listViewEventosFinalizados);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesEventos);
        listViewEventosFinalizados.setAdapter(adapter);

        fetchUserCourseAndLoadEvents();
    }

    private void fetchUserCourseAndLoadEvents() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("usuarios").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String cursoUsuario = document.getString("curso");
                    loadEventosFinalizados(cursoUsuario);
                } else {
                    Toast.makeText(this, "Perfil de usuário não encontrado.", Toast.LENGTH_SHORT).show();
                    loadEventosFinalizados(null); // Carrega apenas eventos "Geral"
                }
            } else {
                Toast.makeText(this, "Erro ao buscar perfil do usuário.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEventosFinalizados(String cursoUsuario) {
        db.collection("eventos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                nomesEventos.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date agora = new Date();
                boolean isCursoValido = cursoUsuario != null && !cursoUsuario.isEmpty() && !cursoUsuario.equals("Selecione o Curso");

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Evento evento = document.toObject(Evento.class);
                        if (evento == null) {
                            Log.e(TAG, "Evento nulo para o documento: " + document.getId());
                            continue;
                        }

                        String dataTerminoStr = evento.getDataTermino();
                        String horarioTerminoStr = evento.getHorarioTermino();

                        if (dataTerminoStr != null && !dataTerminoStr.isEmpty() && horarioTerminoStr != null && !horarioTerminoStr.isEmpty()) {
                            Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);
                            long dezMinutosApos = 10 * 60 * 1000;
                            if (agora.getTime() > dataTermino.getTime() + dezMinutosApos) {

                                List<String> cursosPermitidos = evento.getCursosPermitidos();
                                boolean isEventoVisivel = false;
                                if (cursosPermitidos == null || cursosPermitidos.isEmpty() || cursosPermitidos.contains("Geral")) {
                                    isEventoVisivel = true;
                                } else if (isCursoValido && cursosPermitidos.contains(cursoUsuario)) {
                                    isEventoVisivel = true;
                                }

                                if (isEventoVisivel) {
                                    nomesEventos.add(evento.getNome());
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar evento: " + document.getId(), e);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Erro ao carregar eventos finalizados.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
