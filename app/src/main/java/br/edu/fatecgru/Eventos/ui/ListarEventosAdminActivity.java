package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class ListarEventosAdminActivity extends BaseActivity {

    private static final String TAG = "ListarEventosAdmin";
    private ListView listViewEventosAtivos;
    private ListView listViewEventosFinalizados;
    private SearchView searchView;
    private FirebaseFirestore db;

    private List<Evento> allEventosAtivos = new ArrayList<>();
    private List<Evento> allEventosFinalizados = new ArrayList<>();
    private List<Evento> displayedEventosAtivos = new ArrayList<>();
    private List<Evento> displayedEventosFinalizados = new ArrayList<>();

    private ArrayAdapter<String> adapterAtivos;
    private ArrayAdapter<String> adapterFinalizados;
    private List<String> nomesEventosAtivos = new ArrayList<>();
    private List<String> nomesEventosFinalizados = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_eventos_admin);

        Toolbar toolbar = findViewById(R.id.toolbar_listar_eventos);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Ver/Editar Eventos");
        }

        listViewEventosAtivos = findViewById(R.id.listViewEventosAtivosAdmin);
        listViewEventosFinalizados = findViewById(R.id.listViewEventosFinalizadosAdmin);
        searchView = findViewById(R.id.searchViewEventosAdmin);
        db = FirebaseFirestore.getInstance();

        adapterAtivos = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesEventosAtivos);
        listViewEventosAtivos.setAdapter(adapterAtivos);

        adapterFinalizados = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesEventosFinalizados);
        listViewEventosFinalizados.setAdapter(adapterFinalizados);

        setupListViewListeners(listViewEventosAtivos, displayedEventosAtivos);
        setupListViewListeners(listViewEventosFinalizados, displayedEventosFinalizados);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void setupListViewListeners(ListView listView, List<Evento> eventosList) {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Evento eventoSelecionado = eventosList.get(position);
            if (eventoSelecionado == null) return;

            final CharSequence[] options = {"Ver QR Codes", "Editar Evento", "Gerar Lista de Presença"};

            new AlertDialog.Builder(this)
                .setTitle("Opções para '" + eventoSelecionado.getNome() + "'")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Ver QR Codes
                        Intent intent = new Intent(ListarEventosAdminActivity.this, EventQRCodeActivity.class);
                        intent.putExtra("evento_id", eventoSelecionado.getId());
                        intent.putExtra("nome_evento", eventoSelecionado.getNome());
                        intent.putExtra("data_evento", eventoSelecionado.getData());
                        intent.putExtra("horario_evento", eventoSelecionado.getHorario());
                        startActivity(intent);
                    } else if (which == 1){ // Editar Evento
                        Intent intent = new Intent(ListarEventosAdminActivity.this, EditarEventoActivity.class);
                        intent.putExtra("EVENTO_ID", eventoSelecionado.getId());
                        startActivity(intent);
                    } else { // Gerar Lista de Presença
                        Intent intent = new Intent(ListarEventosAdminActivity.this, GerarPresencaActivity.class);
                        intent.putExtra("EVENTO_ID", eventoSelecionado.getId());
                        startActivity(intent);
                    }
                })
                .show();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            final Evento eventoParaDeletar = eventosList.get(position);
            new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir o evento '" + eventoParaDeletar.getNome() + "'? Todas as inscrições relacionadas também serão removidas.")
                .setPositiveButton("Excluir", (dialog, which) -> deletarEventoEInscricoes(eventoParaDeletar))
                .setNegativeButton("Cancelar", null)
                .show();
            return true; 
        });
    }

    private void loadEventos() {
        db.collection("eventos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allEventosAtivos.clear();
                allEventosFinalizados.clear();

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date agora = new Date();
                long dezMinutos = 10 * 60 * 1000;

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Evento evento = document.toObject(Evento.class);
                        evento.setId(document.getId());

                        String dataTerminoStr = evento.getDataTermino();
                        String horarioTerminoStr = evento.getHorarioTermino();

                        if (dataTerminoStr != null && !dataTerminoStr.isEmpty() && horarioTerminoStr != null && !horarioTerminoStr.isEmpty()) {
                            Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);
                            if (agora.getTime() > dataTermino.getTime() + dezMinutos) {
                                allEventosFinalizados.add(evento);
                            } else {
                                allEventosAtivos.add(evento);
                            }
                        } else {
                            allEventosAtivos.add(evento);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar evento: " + document.getId(), e);
                    }
                }
                filter("");
            } else {
                Log.e(TAG, "Erro ao carregar eventos.", task.getException());
                Toast.makeText(this, "Falha ao carregar eventos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filter(String query) {
        displayedEventosAtivos.clear();
        displayedEventosFinalizados.clear();
        nomesEventosAtivos.clear();
        nomesEventosFinalizados.clear();

        if (query.isEmpty()) {
            displayedEventosAtivos.addAll(allEventosAtivos);
            displayedEventosFinalizados.addAll(allEventosFinalizados);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
            for (Evento evento : allEventosAtivos) {
                if (evento.getNome().toLowerCase(Locale.ROOT).contains(lowerCaseQuery) || evento.getData().contains(query)) {
                    displayedEventosAtivos.add(evento);
                }
            }
            for (Evento evento : allEventosFinalizados) {
                if (evento.getNome().toLowerCase(Locale.ROOT).contains(lowerCaseQuery) || evento.getData().contains(query)) {
                    displayedEventosFinalizados.add(evento);
                }
            }
        }

        for (Evento evento : displayedEventosAtivos) {
            nomesEventosAtivos.add(evento.getNome());
        }
        for (Evento evento : displayedEventosFinalizados) {
            nomesEventosFinalizados.add(evento.getNome() + " - " + evento.getDataTermino());
        }

        adapterAtivos.notifyDataSetChanged();
        adapterFinalizados.notifyDataSetChanged();
    }

    private void deletarEventoEInscricoes(final Evento evento) {
        final String eventoId = evento.getId();

        db.collection("inscricoes").whereEqualTo("idEvento", eventoId).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    batch.delete(doc.getReference());
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    db.collection("eventos").document(eventoId).delete()
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(ListarEventosAdminActivity.this, "Evento e inscrições excluídos com sucesso.", Toast.LENGTH_SHORT).show();
                            loadEventos();
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Erro ao excluir o evento.", e);
                            Toast.makeText(ListarEventosAdminActivity.this, "Erro ao excluir o evento.", Toast.LENGTH_SHORT).show();
                        });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao excluir inscrições.", e);
                    Toast.makeText(ListarEventosAdminActivity.this, "Erro ao excluir inscrições.", Toast.LENGTH_SHORT).show();
                });
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Erro ao buscar inscrições para exclusão.", e);
                Toast.makeText(ListarEventosAdminActivity.this, "Erro ao buscar inscrições para exclusão.", Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEventos();
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
