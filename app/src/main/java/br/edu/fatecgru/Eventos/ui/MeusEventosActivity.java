package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;
import br.edu.fatecgru.Eventos.util.NetworkUtils;

public class MeusEventosActivity extends AppCompatActivity {

    private ListView listViewEventos;
    private FirebaseFirestore db;
    private List<Evento> eventosList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> nomesEventos = new ArrayList<>();
    private String userIdLogado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meus_eventos);

        Toolbar toolbar = findViewById(R.id.toolbar_meus_eventos);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Eventos Disponíveis");
        }

        userIdLogado = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listViewEventos = findViewById(R.id.listViewMeusEventos);
        db = FirebaseFirestore.getInstance();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesEventos);
        listViewEventos.setAdapter(adapter);

        listViewEventos.setOnItemClickListener((parent, view, position, id) -> {
            Evento eventoClicado = eventosList.get(position);
            new AlertDialog.Builder(MeusEventosActivity.this)
                    .setTitle(eventoClicado.getNome())
                    .setMessage(
                            "Data: " + eventoClicado.getData() + "\n" +
                            "Horário: " + eventoClicado.getHorario() + "\n" +
                            "Descrição: " + eventoClicado.getDescricao()
                    )
                    .setPositiveButton("Inscrever-se", (dialog, which) -> {
                        inscreverUsuario(eventoClicado);
                    })
                    .setNegativeButton("Fechar", null)
                    .show();
        });
    }

    private void inscreverUsuario(Evento evento) {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> inscricao = new HashMap<>();
        inscricao.put("idUsuario", userIdLogado);
        inscricao.put("idEvento", evento.getId());
        inscricao.put("nomeEvento", evento.getNome());
        inscricao.put("dataEvento", evento.getData());
        inscricao.put("status", "Cadastrado");

        String docId = userIdLogado + "_" + evento.getId();

        db.collection("inscricoes").document(docId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                db.collection("inscricoes").document(docId).set(inscricao)
                        .addOnSuccessListener(aVoid -> Toast.makeText(MeusEventosActivity.this, "Inscrição realizada com sucesso!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(MeusEventosActivity.this, "Erro ao se inscrever.", Toast.LENGTH_SHORT).show());
            } else if (task.isSuccessful() && task.getResult().exists()) {
                Toast.makeText(MeusEventosActivity.this, "Você já está inscrito neste evento.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MeusEventosActivity.this, "Erro ao verificar inscrição.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEventosDisponiveis() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "Sem conexão com a internet.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("eventos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                eventosList.clear();
                nomesEventos.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Evento evento = document.toObject(Evento.class);
                    evento.setId(document.getId());
                    eventosList.add(evento);
                    nomesEventos.add(evento.getNome());
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MeusEventosActivity.this, "Erro ao carregar eventos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventosDisponiveis();
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
