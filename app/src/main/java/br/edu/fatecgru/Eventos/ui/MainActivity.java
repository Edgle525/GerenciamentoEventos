package br.edu.fatecgru.Eventos.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.Eventos.R;

public class MainActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";

    private EditText edtUsuario, edtSenha;
    private Button btnAcessar;
    private TextView tvCadastrar;
    private SignInButton btnGoogleLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isNightMode = prefs.getBoolean("NightMode", false);
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtUsuario = findViewById(R.id.edtUsuario);
        edtSenha = findViewById(R.id.edtSenha);
        btnAcessar = findViewById(R.id.btnAcessar);
        tvCadastrar = findViewById(R.id.tvCadastrar);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnAcessar.setOnClickListener(v -> realizarLogin());
        tvCadastrar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CadastroActivity.class);
            startActivity(intent);
        });
        btnGoogleLogin.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Falha no login com Google.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        verificarTipoUsuario(user, true);
                    } else {
                        Toast.makeText(MainActivity.this, "Falha na autenticação com Firebase.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void realizarLogin() {
        String email = edtUsuario.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();

        if (email.isEmpty() || senha.isEmpty()) {
            Toast.makeText(this, "Email e senha são obrigatórios!", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, senha)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        verificarTipoUsuario(mAuth.getCurrentUser(), false);
                    } else {
                        handleLoginFailure(task.getException());
                    }
                });
    }

    private void verificarTipoUsuario(FirebaseUser user, boolean isNewGoogleUser) {
        if (user == null) return;

        db.collection("usuarios").document(user.getUid()).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            navigateToScreen(user, document.getString("tipo"));
                        } else if (isNewGoogleUser) {
                            criarPerfilUsuarioGoogle(user);
                        } else {
                            Toast.makeText(MainActivity.this, "Dados do usuário não encontrados.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Erro ao buscar dados do usuário.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void criarPerfilUsuarioGoogle(FirebaseUser user) {
        String userId = user.getUid();
        String nome = user.getDisplayName();
        String email = user.getEmail();

        Map<String, Object> userData = new HashMap<>();
        userData.put("nome", nome);
        userData.put("email", email);
        userData.put("cpf", "");
        userData.put("telefone", "");
        userData.put("curso", "");
        userData.put("semestre", "");
        userData.put("tipo", "USER");

        db.collection("usuarios").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> navigateToScreen(user, "USER"))
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Erro ao criar perfil.", Toast.LENGTH_SHORT).show());
    }

    private void navigateToScreen(FirebaseUser user, String userType) {
        Intent intent;
        if ("ADMIN".equals(userType)) {
            intent = new Intent(MainActivity.this, AdminActivity.class);
        } else {
            intent = new Intent(MainActivity.this, UserActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void handleLoginFailure(Exception exception) {
        try {
            throw exception;
        } catch (FirebaseAuthInvalidUserException | FirebaseAuthInvalidCredentialsException e) {
            Toast.makeText(MainActivity.this, "E-mail ou senha inválidos.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Falha na autenticação: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
