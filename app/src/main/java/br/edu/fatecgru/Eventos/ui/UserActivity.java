package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;
import de.hdodenhof.circleimageview.CircleImageView;

public class UserActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "UserActivity";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ListView listViewEventos;
    private TextView tvProfileWarning;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private List<Evento> eventosList = new ArrayList<>();
    private EventoAtivoAdapter adapter;
    private String userIdLogado;
    private boolean isProfileComplete = false;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Leitura cancelada", Toast.LENGTH_LONG).show();
                } else {
                    handleScanResult(result.getContents());
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startScanner();
                } else {
                    Toast.makeText(this, "Permissão da câmera negada.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        mAuth = FirebaseAuth.getInstance();
        userIdLogado = mAuth.getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Toolbar toolbar = findViewById(R.id.toolbar_user);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawer_layout_user);
        navigationView = findViewById(R.id.nav_view_user);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        tvProfileWarning = findViewById(R.id.tvProfileWarning);
        listViewEventos = findViewById(R.id.listViewEventosUser);
        adapter = new EventoAtivoAdapter(this, eventosList);
        listViewEventos.setAdapter(adapter);

        updateNavHeader();
        checkProfileCompleteness();

        listViewEventos.setOnItemClickListener((parent, view, position, id) -> {
            Evento eventoClicado = eventosList.get(position);
            if (!isProfileComplete) {
                Toast.makeText(this, "Por favor, complete seu perfil para se inscrever em eventos.", Toast.LENGTH_LONG).show();
                return;
            }
            new AlertDialog.Builder(UserActivity.this)
                    .setTitle(eventoClicado.getNome())
                    .setMessage(
                            "Data: " + eventoClicado.getData() + "\n" +
                                    "Horário: " + eventoClicado.getHorario() + "\n" +
                                    "Descrição: " + eventoClicado.getDescricao()
                    )
                    .setPositiveButton("Inscrever-se", (dialog, which) -> inscreverUsuario(eventoClicado))
                    .setNegativeButton("Fechar", null)
                    .show();
        });

        FloatingActionButton fabScan = findViewById(R.id.fab_scan);
        fabScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startScanner();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        tvProfileWarning.setOnClickListener(v -> {
            Intent intent = new Intent(this, MeuPerfilActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_toggle_night_mode) {
            toggleNightMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleNightMode() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("NightMode", false);
        SharedPreferences.Editor editor = prefs.edit();

        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            editor.putBoolean("NightMode", false);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            editor.putBoolean("NightMode", true);
        }
        editor.apply();
    }

    private void checkProfileCompleteness() {
        db.collection("usuarios").document(userIdLogado).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String cpf = documentSnapshot.getString("cpf");
                String telefone = documentSnapshot.getString("telefone");
                String curso = documentSnapshot.getString("curso");
                String semestre = documentSnapshot.getString("semestre");

                if (TextUtils.isEmpty(cpf) || TextUtils.isEmpty(telefone) || TextUtils.isEmpty(curso) || TextUtils.isEmpty(semestre) || curso.equals("Selecione o Curso") || semestre.equals("Selecione o Semestre")) {
                    tvProfileWarning.setVisibility(View.VISIBLE);
                    isProfileComplete = false;
                } else {
                    tvProfileWarning.setVisibility(View.GONE);
                    isProfileComplete = true;
                }
                loadEventos(curso);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkProfileCompleteness();
        updateNavHeader();
    }

    private void loadEventos(String cursoUsuario) {
        db.collection("eventos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                eventosList.clear();
                boolean isCursoValido = cursoUsuario != null && !cursoUsuario.isEmpty() && !cursoUsuario.equals("Selecione o Curso");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date agora = new Date();

                for (QueryDocumentSnapshot document : task.getResult()) {
                    try {
                        Evento evento = document.toObject(Evento.class);
                        if (evento == null) continue;

                        String dataTerminoStr = evento.getDataTermino();
                        String horarioTerminoStr = evento.getHorarioTermino();

                        if (dataTerminoStr != null && !dataTerminoStr.isEmpty() && horarioTerminoStr != null && !horarioTerminoStr.isEmpty()) {
                            Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);
                            if (dataTermino.after(agora)) {
                                List<String> cursosPermitidos = evento.getCursosPermitidos();
                                boolean isEventoVisivel = false;
                                if (cursosPermitidos == null || cursosPermitidos.isEmpty() || cursosPermitidos.contains("Geral")) {
                                    isEventoVisivel = true;
                                } else if (isCursoValido && cursosPermitidos.contains(cursoUsuario)) {
                                    isEventoVisivel = true;
                                }

                                if (isEventoVisivel) {
                                    evento.setId(document.getId());
                                    eventosList.add(evento);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar evento: " + document.getId(), e);
                    }
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(UserActivity.this, "Erro ao carregar eventos.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void inscreverUsuario(Evento evento) {
        if (!isProfileComplete) {
            Toast.makeText(this, "Por favor, complete seu perfil para se inscrever em eventos.", Toast.LENGTH_LONG).show();
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
                        .addOnSuccessListener(aVoid -> Toast.makeText(UserActivity.this, "Inscrição realizada com sucesso!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(UserActivity.this, "Erro ao se inscrever.", Toast.LENGTH_SHORT).show());
            } else if (task.isSuccessful() && task.getResult().exists()) {
                Toast.makeText(this, "Você já está inscrito neste evento.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(UserActivity.this, "Erro ao verificar inscrição.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleScanResult(String contents) {
        if (contents == null || contents.isEmpty()) {
            Toast.makeText(this, "QR Code inválido.", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] parts = contents.split(";");
        if (parts.length < 5) {
            Toast.makeText(this, "QR Code inválido ou com formato incorreto.", Toast.LENGTH_SHORT).show();
            return;
        }

        String tipo = parts[0];
        String idEvento = parts[1];

        db.collection("eventos").document(idEvento).get().addOnCompleteListener(eventTask -> {
            if (eventTask.isSuccessful() && eventTask.getResult().exists()) {
                Evento evento = eventTask.getResult().toObject(Evento.class);
                if (evento != null) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        Date dataTermino = sdf.parse(evento.getDataTermino() + " " + evento.getHorarioTermino());
                        if (new Date().after(dataTermino)) {
                            Toast.makeText(this, "Este evento já foi finalizado.", Toast.LENGTH_LONG).show();
                            return;
                        }
                    } catch (ParseException e) {
                        //
                    }
                    verificarInscricaoEProcessar(tipo, idEvento, evento);
                } else {
                    Toast.makeText(this, "Evento não encontrado.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Este evento não existe mais ou foi cancelado.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void verificarInscricaoEProcessar(String tipo, String idEvento, Evento evento) {
        String docId = userIdLogado + "_" + idEvento;

        db.collection("inscricoes").document(docId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    if (tipo.equals("ENTRADA") && document.getString("horaEntrada") != null) {
                        Toast.makeText(this, "Entrada já registrada para este evento.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tipo.equals("SAIDA") && document.getString("horaSaida") != null) {
                        Toast.makeText(this, "Saída já registrada para este evento.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tipo.equals("SAIDA") && document.getString("horaEntrada") == null) {
                        Toast.makeText(this, "Você precisa registrar a entrada antes de registrar a saída.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        Date agora = new Date();

                        Date dataInicioEvento = sdf.parse(evento.getData() + " " + evento.getHorario());
                        Date dataTerminoEvento = sdf.parse(evento.getDataTermino() + " " + evento.getHorarioTermino());

                        long dezMinutos = 10 * 60 * 1000;

                        if (tipo.equals("ENTRADA")) {
                            if (agora.getTime() > dataInicioEvento.getTime() + dezMinutos) {
                                Toast.makeText(this, "Fora do horário de entrada.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else if (tipo.equals("SAIDA")) {
                            String horaEntradaStr = document.getString("horaEntrada");
                            if (horaEntradaStr != null && !evento.isTempoTotal()) {
                                try {
                                    SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                    Date dateEntrada = format.parse(horaEntradaStr);
                                    long diff = new Date().getTime() - dateEntrada.getTime();
                                    long diffMinutes = diff / (60 * 1000);
                                    if (diffMinutes >= evento.getTempoMinimo()) {
                                        // Pode registrar a saída
                                    } else {
                                        Toast.makeText(this, "Tempo mínimo de permanência ainda não atingido.", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                } catch (ParseException e) {
                                    //
                                }
                            }
                            
                            if (evento.isTempoTotal()) {
                                if (agora.getTime() < dataTerminoEvento.getTime() - dezMinutos) {
                                    Toast.makeText(this, "Ainda não está na hora de registrar a saída.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            } else {
                                if (agora.getTime() > dataTerminoEvento.getTime() + dezMinutos) {
                                    Toast.makeText(this, "Fora do horário de saída.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        }

                        String fieldToUpdate = tipo.equals("ENTRADA") ? "horaEntrada" : "horaSaida";
                        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                        db.collection("inscricoes").document(docId).update(fieldToUpdate, currentTime)
                                .addOnSuccessListener(aVoid -> {
                                    if (tipo.equals("SAIDA")) {
                                        String horaEntradaStr = document.getString("horaEntrada");
                                        boolean participacaoCompleta = true;
                                        if (horaEntradaStr != null) {
                                            try {
                                                SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
                                                Date dateEntrada = format.parse(horaEntradaStr);
                                                Date dateSaida = format.parse(currentTime);
                                                long diff = dateSaida.getTime() - dateEntrada.getTime();
                                                long diffMinutes = diff / (60 * 1000);
                                                if (diffMinutes < evento.getTempoMinimo()) {
                                                    participacaoCompleta = false;
                                                }
                                            } catch (ParseException e) {
                                                participacaoCompleta = false; 
                                            }
                                        }

                                        boolean finalParticipacaoCompleta = participacaoCompleta;
                                        new AlertDialog.Builder(UserActivity.this)
                                                .setTitle("Saída Registrada!")
                                                .setMessage("Evento concluído com sucesso. Deseja gerar seu comprovante de participação?")
                                                .setPositiveButton("Gerar Comprovante", (dialog, which) -> {
                                                    db.collection("usuarios").document(userIdLogado).get().addOnSuccessListener(userDoc -> {
                                                        if (userDoc.exists()) {
                                                            Intent intent = new Intent(UserActivity.this, ComprovanteActivity.class);
                                                            intent.putExtra("evento_id", idEvento);
                                                            intent.putExtra("user_id", userIdLogado);
                                                            intent.putExtra("nome_evento", evento.getNome());
                                                            intent.putExtra("data_evento", evento.getData());
                                                            intent.putExtra("horario_evento", evento.getHorario());
                                                            intent.putExtra("nome_participante", userDoc.getString("nome"));
                                                            intent.putExtra("email_participante", userDoc.getString("email"));
                                                            intent.putExtra("participacao_completa", finalParticipacaoCompleta);
                                                            startActivity(intent);
                                                        } else {
                                                            Toast.makeText(this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                                })
                                                .setNegativeButton("Agora não", null)
                                                .show();
                                    } else {
                                        Toast.makeText(UserActivity.this, "Entrada registrada com sucesso!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao registrar ponto.", Toast.LENGTH_SHORT).show());
                    } catch (ParseException e) {
                        Toast.makeText(this, "Erro ao processar as datas do evento.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "Você não está inscrito no evento '" + evento.getNome() + "'.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Erro ao verificar inscrição.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderNome = headerView.findViewById(R.id.nav_header_nome);
        TextView navHeaderInfo = headerView.findViewById(R.id.nav_header_info);
        CircleImageView navHeaderProfileImage = headerView.findViewById(R.id.nav_header_profile_image);

        db.collection("usuarios").document(userIdLogado).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                navHeaderNome.setText(documentSnapshot.getString("nome"));
                navHeaderInfo.setText(documentSnapshot.getString("email"));

                if (documentSnapshot.contains("profileImageBase64")) {
                    String base64 = documentSnapshot.getString("profileImageBase64");
                    if (base64 != null && !base64.isEmpty()) {
                        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        Glide.with(UserActivity.this).load(decodedByte).into(navHeaderProfileImage);
                    }
                }
            }
        });
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Aponte para o QR Code");
        options.setBeepEnabled(true);
        options.setCaptureActivity(CaptureActivityPortrait.class);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_escanear) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startScanner();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        } else if (id == R.id.nav_historico) {
            startActivity(new Intent(this, HistoricoActivity.class));
        } else if (id == R.id.nav_eventos_finalizados) {
            startActivity(new Intent(this, EventosFinalizadosActivity.class));
        } else if (id == R.id.nav_meu_perfil) {
            startActivity(new Intent(this, MeuPerfilActivity.class));
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
