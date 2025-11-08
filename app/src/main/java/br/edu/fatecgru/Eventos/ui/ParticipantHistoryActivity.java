package br.edu.fatecgru.Eventos.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.HistoricoAdapter;
import br.edu.fatecgru.Eventos.model.Inscricao;
import br.edu.fatecgru.Eventos.util.MaskUtils;

public class ParticipantHistoryActivity extends BaseActivity {

    private TextView tvParticipantName, tvParticipantEmail, tvParticipantCpf;
    private ListView listViewHistory;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participant_history);

        Toolbar toolbar = findViewById(R.id.toolbar_participant_history);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalhes do Usuário");
        }

        tvParticipantName = findViewById(R.id.tvParticipantName);
        tvParticipantEmail = findViewById(R.id.tvParticipantEmail);
        tvParticipantCpf = findViewById(R.id.tvParticipantCpf);
        listViewHistory = findViewById(R.id.listViewParticipantHistory);
        db = FirebaseFirestore.getInstance();

        String userId = getIntent().getStringExtra("USER_ID");
        String userName = getIntent().getStringExtra("USER_NOME");
        String userEmail = getIntent().getStringExtra("USER_EMAIL");
        String userCpf = getIntent().getStringExtra("USER_CPF");

        tvParticipantName.setText(userName);
        tvParticipantEmail.setText(userEmail);
        tvParticipantCpf.setText(userCpf != null && !userCpf.isEmpty() ? MaskUtils.formatCpf(userCpf) : "CPF não informado");

        if (userId != null) {
            loadParticipantHistory(userId);
        }
    }

    private void loadParticipantHistory(String userId) {
        db.collection("inscricoes").whereEqualTo("idUsuario", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Inscricao> historicoList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Inscricao inscricao = document.toObject(Inscricao.class);
                        historicoList.add(inscricao);
                    }
                    HistoricoAdapter adapter = new HistoricoAdapter(this, historicoList);
                    listViewHistory.setAdapter(adapter);
                    if (historicoList.isEmpty()){
                        Toast.makeText(this, "Este usuário não se inscreveu em nenhum evento.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar histórico do usuário.", Toast.LENGTH_SHORT).show());
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
