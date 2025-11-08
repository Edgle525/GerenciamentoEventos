package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.HistoricoAdapter;
import br.edu.fatecgru.Eventos.model.Inscricao;

public class HistoricoActivity extends BaseActivity {

    private ListView listViewHistorico;
    private FirebaseFirestore db;
    private String userIdLogado;
    private List<Inscricao> historicoList = new ArrayList<>();
    private HistoricoAdapter adapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        Toolbar toolbar = findViewById(R.id.toolbar_historico);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Meu Histórico");
        }

        listViewHistorico = findViewById(R.id.listViewHistorico);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userIdLogado = mAuth.getCurrentUser().getUid();

        adapter = new HistoricoAdapter(this, historicoList);
        listViewHistorico.setAdapter(adapter);

        listViewHistorico.setOnItemClickListener((parent, view, position, id) -> {
            Inscricao inscricao = historicoList.get(position);
            if (inscricao == null) return;

            boolean isCompleto = inscricao.getHoraEntrada() != null && inscricao.getHoraSaida() != null;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(inscricao.getNomeEvento());

            if (isCompleto) {
                builder.setItems(new CharSequence[]{"Gerar Comprovante", "Cancelar Inscrição"}, (dialog, which) -> {
                    if (which == 0) {
                        gerarComprovante(inscricao);
                    } else {
                        confirmarCancelamento(inscricao);
                    }
                });
            } else {
                builder.setItems(new CharSequence[]{"Cancelar Inscrição"}, (dialog, which) -> {
                    confirmarCancelamento(inscricao);
                });
            }
            builder.show();
        });
    }

    private void confirmarCancelamento(Inscricao inscricao) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Cancelamento")
                .setMessage("Deseja realmente cancelar a inscrição em " + inscricao.getNomeEvento() + "?")
                .setPositiveButton("Sim", (dialog, which) -> cancelarInscricao(inscricao))
                .setNegativeButton("Não", null)
                .show();
    }

    private void gerarComprovante(Inscricao inscricao) {
        db.collection("usuarios").document(userIdLogado).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Toast.makeText(this, "Erro: Dados do usuário não encontrados.", Toast.LENGTH_SHORT).show();
                return;
            }

            String nomeParticipante = documentSnapshot.getString("nome");
            String emailParticipante = documentSnapshot.getString("email");

            Intent intent = new Intent(this, ComprovanteActivity.class);
            intent.putExtra("evento_id", inscricao.getIdEvento());
            intent.putExtra("user_id", userIdLogado);
            intent.putExtra("nome_evento", inscricao.getNomeEvento());
            intent.putExtra("data_evento", inscricao.getDataEvento());
            intent.putExtra("horario_evento", ""); 
            intent.putExtra("nome_participante", nomeParticipante);
            intent.putExtra("email_participante", emailParticipante);
            startActivity(intent);

        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show());
    }

    private void cancelarInscricao(Inscricao inscricao) {
        String docId = userIdLogado + "_" + inscricao.getIdEvento();
        db.collection("inscricoes").document(docId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Inscrição cancelada.", Toast.LENGTH_SHORT).show();
                    loadHistorico();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao cancelar.", Toast.LENGTH_SHORT).show());
    }

    private void loadHistorico() {
        db.collection("inscricoes").whereEqualTo("idUsuario", userIdLogado).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        historicoList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Inscricao inscricao = document.toObject(Inscricao.class);
                            inscricao.setIdEvento(document.getString("idEvento"));
                            historicoList.add(inscricao);
                        }
                        adapter.notifyDataSetChanged();
                        if (historicoList.isEmpty()) {
                            Toast.makeText(this, "Nenhuma inscrição encontrada.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Erro ao carregar histórico.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistorico();
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
