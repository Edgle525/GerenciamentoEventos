package br.edu.fatecgru.Eventos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Usuario;

public class ViewParticipantsActivity extends BaseActivity {

    private ListView listViewParticipants;
    private SearchView searchView;
    private FirebaseFirestore db;

    private List<Usuario> allUsersList = new ArrayList<>();
    private List<Usuario> displayedUsersList = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private List<String> userNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_participants);

        Toolbar toolbar = findViewById(R.id.toolbar_view_participants);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Usuários");
        }

        listViewParticipants = findViewById(R.id.listViewParticipants);
        searchView = findViewById(R.id.searchViewParticipants);
        db = FirebaseFirestore.getInstance();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userNames);
        listViewParticipants.setAdapter(adapter);

        loadAllUsers();

        listViewParticipants.setOnItemClickListener((parent, view, position, id) -> {
            Usuario selectedUser = displayedUsersList.get(position);
            Intent intent = new Intent(ViewParticipantsActivity.this, ParticipantHistoryActivity.class);
            intent.putExtra("USER_ID", selectedUser.getId());
            intent.putExtra("USER_NOME", selectedUser.getNome());
            intent.putExtra("USER_EMAIL", selectedUser.getEmail());
            intent.putExtra("USER_CPF", selectedUser.getCpf());
            startActivity(intent);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterUsers(newText);
                return true;
            }
        });
    }

    private void loadAllUsers() {
        db.collection("usuarios").orderBy("nome").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allUsersList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Usuario usuario = document.toObject(Usuario.class);
                    usuario.setId(document.getId());
                    allUsersList.add(usuario);
                }
                filterUsers("");
            } else {
                Toast.makeText(this, "Erro ao carregar usuários.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterUsers(String query) {
        displayedUsersList.clear();
        userNames.clear();

        if (query.isEmpty()) {
            displayedUsersList.addAll(allUsersList);
        } else {
            for (Usuario usuario : allUsersList) {
                if (usuario.getNome().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
                    displayedUsersList.add(usuario);
                }
            }
        }

        for (Usuario usuario : displayedUsersList) {
            userNames.add(usuario.getNome());
        }
        adapter.notifyDataSetChanged();
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
