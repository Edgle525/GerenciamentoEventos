package br.edu.fatecgru.Eventos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class AdminActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseFirestore db;

    private TextView tvEventosAtivos, tvEventosFinalizados, tvTotalUsuarios;
    private Button btnCadastrarNovoEvento;
    private CardView cardEventosAtivos, cardEventosFinalizados, cardTotalUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Toolbar toolbar = findViewById(R.id.toolbar_admin);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout_admin);
        navigationView = findViewById(R.id.nav_view_admin);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        tvEventosAtivos = findViewById(R.id.tv_eventos_ativos);
        tvEventosFinalizados = findViewById(R.id.tv_eventos_finalizados);
        tvTotalUsuarios = findViewById(R.id.tv_total_usuarios);
        btnCadastrarNovoEvento = findViewById(R.id.btn_cadastrar_novo_evento);
        cardEventosAtivos = findViewById(R.id.card_eventos_ativos);
        cardEventosFinalizados = findViewById(R.id.card_eventos_finalizados);
        cardTotalUsuarios = findViewById(R.id.card_total_usuarios);

        btnCadastrarNovoEvento.setOnClickListener(v -> startActivity(new Intent(this, CadastroEventoActivity.class)));
        cardEventosAtivos.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListarEventosAdminActivity.class);
            intent.putExtra("TIPO_EVENTO", "ATIVOS");
            startActivity(intent);
        });
        cardEventosFinalizados.setOnClickListener(v -> {
            Intent intent = new Intent(this, ListarEventosAdminActivity.class);
            intent.putExtra("TIPO_EVENTO", "FINALIZADOS");
            startActivity(intent);
        });
        cardTotalUsuarios.setOnClickListener(v -> startActivity(new Intent(this, ViewParticipantsActivity.class)));

        updateDashboard();
        updateNavHeader();
    }

    private void updateDashboard() {
        db.collection("eventos").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                int eventosAtivos = 0;
                int eventosFinalizados = 0;
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date agora = new Date();

                for (QueryDocumentSnapshot doc : value) {
                    Evento evento = doc.toObject(Evento.class);
                    if (evento.isArquivado()) {
                        continue;
                    }

                    String dataTerminoStr = evento.getDataTermino();
                    String horarioTerminoStr = evento.getHorarioTermino();

                    if (dataTerminoStr != null && !dataTerminoStr.isEmpty() && horarioTerminoStr != null && !horarioTerminoStr.isEmpty()) {
                        try {
                            Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);
                            if (dataTermino.after(agora)) {
                                eventosAtivos++;
                            } else {
                                eventosFinalizados++;
                            }
                        } catch (ParseException e) {
                            // Ignorar eventos com data mal formatada
                        }
                    } else {
                        // Eventos sem data de término são considerados ativos
                        eventosAtivos++;
                    }
                }
                tvEventosAtivos.setText(String.valueOf(eventosAtivos));
                tvEventosFinalizados.setText(String.valueOf(eventosFinalizados));
            }
        });

        db.collection("usuarios").addSnapshotListener((value, error) -> {
            if (error != null) return;
            if (value != null) {
                int userCount = 0;
                for(QueryDocumentSnapshot doc : value) {
                    String userType = doc.getString("tipo");
                    if(userType != null && !userType.equals("ADMIN")) {
                        userCount++;
                    }
                }
                tvTotalUsuarios.setText(String.valueOf(userCount));
            }
        });
    }

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        if (headerView == null) return;

        TextView navHeaderNome = headerView.findViewById(R.id.nav_header_nome);
        TextView navHeaderInfo = headerView.findViewById(R.id.nav_header_info);
        
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (navHeaderNome != null) {
            navHeaderNome.setText("Conta de Administrador");
        }
        if (navHeaderInfo != null) {
            if (currentUser != null) {
                navHeaderInfo.setText(currentUser.getEmail());
            } else {
                 navHeaderInfo.setText("admin@exemplo.com");
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_cadastrar_evento) {
            startActivity(new Intent(this, CadastroEventoActivity.class));
        } else if (id == R.id.nav_listar_eventos_ativos) {
            Intent intent = new Intent(this, ListarEventosAdminActivity.class);
            intent.putExtra("TIPO_EVENTO", "ATIVOS");
            startActivity(intent);
        } else if (id == R.id.nav_listar_eventos_finalizados) {
            Intent intent = new Intent(this, ListarEventosAdminActivity.class);
            intent.putExtra("TIPO_EVENTO", "FINALIZADOS");
            startActivity(intent);
        } else if (id == R.id.nav_view_participants) {
            startActivity(new Intent(this, ViewParticipantsActivity.class));
        } else if (id == R.id.nav_logout) {
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
