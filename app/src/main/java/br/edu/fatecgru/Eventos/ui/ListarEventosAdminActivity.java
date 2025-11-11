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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class ListarEventosAdminActivity extends BaseActivity {

    private static final String TAG = "ListarEventosAdmin";
    private ListView listViewEventos;
    private SearchView searchView;
    private FirebaseFirestore db;

    private List<Evento> allEventos = new ArrayList<>();
    private List<Evento> displayedEventos = new ArrayList<>();

    private ArrayAdapter<String> adapter;
    private List<String> nomesEventos = new ArrayList<>();
    private String tipoEvento;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar_eventos_admin);

        tipoEvento = getIntent().getStringExtra("TIPO_EVENTO");

        Toolbar toolbar = findViewById(R.id.toolbar_listar_eventos);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if ("ATIVOS".equals(tipoEvento)) {
                getSupportActionBar().setTitle("Eventos Ativos");
            } else {
                getSupportActionBar().setTitle("Eventos Finalizados");
            }
        }

        listViewEventos = findViewById(R.id.listViewEventosAdmin);
        searchView = findViewById(R.id.searchViewEventosAdmin);
        db = FirebaseFirestore.getInstance();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nomesEventos);
        listViewEventos.setAdapter(adapter);

        setupListViewListeners();

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

    private void setupListViewListeners() {
        listViewEventos.setOnItemClickListener((parent, view, position, id) -> {
            Evento eventoSelecionado = displayedEventos.get(position);
            if (eventoSelecionado == null) return;

            final CharSequence[] options;
            if ("FINALIZADOS".equals(tipoEvento)) {
                options = new CharSequence[]{"Ver QR Codes", "Excluir Evento", "Gerar Lista de Presença"};
            } else {
                options = new CharSequence[]{"Ver QR Codes", "Editar Evento", "Gerar Lista de Presença"};
            }

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
                    } else if (which == 1){ // Editar or Excluir (Archive) Evento
                        if ("FINALIZADOS".equals(tipoEvento)) {
                            arquivarEvento(eventoSelecionado, "O evento '" + eventoSelecionado.getNome() + "' será arquivado e não aparecerá mais na lista de finalizados. Deseja continuar?");
                        } else { // ATIVOS
                            Intent intent = new Intent(ListarEventosAdminActivity.this, EditarEventoActivity.class);
                            intent.putExtra("EVENTO_ID", eventoSelecionado.getId());
                            startActivity(intent);
                        }
                    } else { // Gerar Lista de Presença
                        Intent intent = new Intent(ListarEventosAdminActivity.this, GerarPresencaActivity.class);
                        intent.putExtra("EVENTO_ID", eventoSelecionado.getId());
                        startActivity(intent);
                    }
                })
                .show();
        });

        if ("ATIVOS".equals(tipoEvento)) {
            listViewEventos.setOnItemLongClickListener((parent, view, position, id) -> {
                final Evento eventoParaArquivar = displayedEventos.get(position);
                String message = "Deseja arquivar o evento '" + eventoParaArquivar.getNome() + "'? O evento não será mais exibido, mas o histórico de participação dos usuários será mantido.";
                arquivarEvento(eventoParaArquivar, message);
                return true;
            });
        }
    }

    private void arquivarEvento(final Evento evento, String message) {
        new AlertDialog.Builder(this)
            .setTitle("Confirmar Arquivamento")
            .setMessage(message)
            .setPositiveButton("Arquivar", (dialog, which) -> {
                db.collection("eventos").document(evento.getId())
                    .update("arquivado", true)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(ListarEventosAdminActivity.this, "Evento arquivado com sucesso.", Toast.LENGTH_SHORT).show();
                        loadEventos();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ListarEventosAdminActivity.this, "Erro ao arquivar evento.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Erro ao arquivar evento", e);
                    });
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void loadEventos() {
        db.collection("eventos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allEventos.clear();

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date agora = new Date();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Evento evento = document.toObject(Evento.class);
                        evento.setId(document.getId());

                        if (evento.isArquivado()) {
                            continue;
                        }

                        String dataTerminoStr = evento.getDataTermino();
                        String horarioTerminoStr = evento.getHorarioTermino();

                        if (dataTerminoStr != null && !dataTerminoStr.isEmpty() && horarioTerminoStr != null && !horarioTerminoStr.isEmpty()) {
                            Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);
                            if ("ATIVOS".equals(tipoEvento) && dataTermino.after(agora)) {
                                allEventos.add(evento);
                            } else if ("FINALIZADOS".equals(tipoEvento) && !dataTermino.after(agora)){
                                allEventos.add(evento);
                            }
                        } else if ("ATIVOS".equals(tipoEvento)){
                            allEventos.add(evento);
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
        displayedEventos.clear();
        nomesEventos.clear();

        if (query.isEmpty()) {
            displayedEventos.addAll(allEventos);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.ROOT);
            for (Evento evento : allEventos) {
                if (evento.getNome().toLowerCase(Locale.ROOT).contains(lowerCaseQuery) || evento.getData().contains(query)) {
                    displayedEventos.add(evento);
                }
            }
        }

        for (Evento evento : displayedEventos) {
            nomesEventos.add(evento.getNome());
        }

        adapter.notifyDataSetChanged();
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
