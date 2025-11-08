package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class EditarEventoActivity extends BaseActivity {

    private EditText edtNome, edtData, edtHorario, edtDescricao, edtDataTermino, edtHorarioTermino, edtLocal, edtTempoMinimo;
    private Button btnSalvar, btnExcluir;
    private FirebaseFirestore db;
    private String eventoId;
    private static final String TAG = "EditarEventoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_evento);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Editar Evento");
        }

        db = FirebaseFirestore.getInstance();
        eventoId = getIntent().getStringExtra("EVENTO_ID");

        edtNome = findViewById(R.id.edtNomeEventoEditar);
        edtData = findViewById(R.id.edtDataEventoEditar);
        edtHorario = findViewById(R.id.edtHorarioEventoEditar);
        edtDataTermino = findViewById(R.id.edtDataTerminoEventoEditar);
        edtHorarioTermino = findViewById(R.id.edtHorarioTerminoEventoEditar);
        edtLocal = findViewById(R.id.edtLocalEventoEditar);
        edtDescricao = findViewById(R.id.edtDescricaoEventoEditar);
        edtTempoMinimo = findViewById(R.id.edtTempoMinimoPermanenciaEditar);
        btnSalvar = findViewById(R.id.btnSalvarEdicaoEvento);
        btnExcluir = findViewById(R.id.btnExcluirEvento);

        loadEvento();

        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
        btnExcluir.setOnClickListener(v -> confirmarExclusao());
    }

    private void loadEvento() {
        if (eventoId == null) return;

        db.collection("eventos").document(eventoId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Evento evento = documentSnapshot.toObject(Evento.class);
                        if (evento != null) {
                            edtNome.setText(evento.getNome());
                            edtData.setText(evento.getData());
                            edtHorario.setText(evento.getHorario());
                            edtDataTermino.setText(evento.getDataTermino());
                            edtHorarioTermino.setText(evento.getHorarioTermino());
                            edtLocal.setText(evento.getLocal());
                            edtDescricao.setText(evento.getDescricao());
                            edtTempoMinimo.setText(String.valueOf(evento.getTempoMinimo()));
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar dados do evento.", Toast.LENGTH_SHORT).show());
    }

    private void salvarAlteracoes() {
        String nome = edtNome.getText().toString().trim();
        String data = edtData.getText().toString().trim();
        String horario = edtHorario.getText().toString().trim();
        String dataTermino = edtDataTermino.getText().toString().trim();
        String horarioTermino = edtHorarioTermino.getText().toString().trim();
        String local = edtLocal.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();
        String tempoMinimoStr = edtTempoMinimo.getText().toString().trim();

        if (nome.isEmpty() || data.isEmpty() || horario.isEmpty() || dataTermino.isEmpty() || horarioTermino.isEmpty() || local.isEmpty() || descricao.isEmpty() || tempoMinimoStr.isEmpty()) {
            Toast.makeText(this, "Todos os campos são obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int tempoMinimo = Integer.parseInt(tempoMinimoStr);

        db.collection("eventos").document(eventoId)
                .update("nome", nome, "data", data, "horario", horario, "dataTermino", dataTermino, "horarioTermino", horarioTermino, "local", local, "descricao", descricao, "tempoMinimo", tempoMinimo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Evento atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar evento.", Toast.LENGTH_SHORT).show());
    }

    private void confirmarExclusao() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir este evento? Todas as inscrições relacionadas também serão removidas.")
                .setPositiveButton("Excluir", (dialog, which) -> deletarEventoEInscricoes())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deletarEventoEInscricoes() {
        if (eventoId == null) return;

        db.collection("inscricoes").whereEqualTo("idEvento", eventoId).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                WriteBatch batch = db.batch();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    batch.delete(doc.getReference());
                }

                batch.commit().addOnSuccessListener(aVoid -> {
                    db.collection("eventos").document(eventoId).delete()
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(EditarEventoActivity.this, "Evento excluído com sucesso.", Toast.LENGTH_SHORT).show();
                            finish(); 
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Erro ao excluir evento.", e));
                }).addOnFailureListener(e -> Log.e(TAG, "Erro ao excluir inscrições.", e));
            })
            .addOnFailureListener(e -> Log.e(TAG, "Erro ao buscar inscrições.", e));
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
