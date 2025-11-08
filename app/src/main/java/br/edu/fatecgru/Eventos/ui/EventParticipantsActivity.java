package br.edu.fatecgru.Eventos.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Inscricao;

public class EventParticipantsActivity extends AppCompatActivity {

    private ListView listViewParticipants;
    private TextView tvEventName;
    private FirebaseFirestore db;
    private String eventoId;
    private List<Inscricao> inscricoesList = new ArrayList<>();
    private ArrayAdapter<Inscricao> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_participants);

        listViewParticipants = findViewById(R.id.listViewParticipants);
        tvEventName = findViewById(R.id.tvEventName);
        db = FirebaseFirestore.getInstance();
        eventoId = getIntent().getStringExtra("EVENTO_ID");

        db.collection("eventos").document(eventoId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvEventName.setText("Participantes: " + documentSnapshot.getString("nome"));
            }
        });

        adapter = new ArrayAdapter<Inscricao>(this, android.R.layout.simple_list_item_2, android.R.id.text1, inscricoesList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(android.R.id.text1);
                TextView text2 = view.findViewById(android.R.id.text2);

                Inscricao inscricao = getItem(position);
                text1.setText(inscricao.getNomeUsuario());

                if (inscricao.getHoraEntrada() != null && inscricao.getHoraSaida() != null) {
                    text2.setText("Status: Concluído");
                    view.setBackgroundColor(Color.GREEN);
                } else {
                    text2.setText("Status: Inscrito");
                    view.setBackgroundColor(Color.TRANSPARENT);
                }
                return view;
            }
        };
        listViewParticipants.setAdapter(adapter);
        loadParticipants();
    }

    private void loadParticipants() {
        db.collection("inscricoes").whereEqualTo("idEvento", eventoId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        inscricoesList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Inscricao inscricao = document.toObject(Inscricao.class);
                            db.collection("usuarios").document(inscricao.getIdUsuario()).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            inscricao.setNomeUsuario(userDoc.getString("nome"));
                                            inscricoesList.add(inscricao);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }
                });
    }
}
