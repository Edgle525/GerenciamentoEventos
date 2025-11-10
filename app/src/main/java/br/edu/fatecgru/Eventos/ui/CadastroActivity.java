package br.edu.fatecgru.Eventos.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.util.MaskUtils;

public class CadastroActivity extends BaseActivity {

    private TextInputEditText edtNome, edtCpf, edtTelefone, edtEmail, edtSenha, edtConfirmarSenha;
    private Spinner spinnerCurso, spinnerSemestre;
    private Button btnCadastrar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cadastro de Usuário");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtNome = findViewById(R.id.edtNomeCadastro);
        edtCpf = findViewById(R.id.edtCpfCadastro);
        edtTelefone = findViewById(R.id.edtTelefoneCadastro);
        spinnerCurso = findViewById(R.id.spinnerCurso);
        spinnerSemestre = findViewById(R.id.spinnerSemestre);
        edtEmail = findViewById(R.id.edtEmailCadastro);
        edtSenha = findViewById(R.id.edtSenhaCadastro);
        edtConfirmarSenha = findViewById(R.id.edtConfirmarSenhaCadastro);
        btnCadastrar = findViewById(R.id.btnCadastrarUsuario);

        setupMasks();
        setupSpinners();

        btnCadastrar.setOnClickListener(v -> cadastrarUsuario());
    }

    private void setupMasks() {
        edtCpf.addTextChangedListener(MaskUtils.insert("###.###.###-##", edtCpf));
        edtTelefone.addTextChangedListener(MaskUtils.insert("(##) #####-####", edtTelefone));
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> cursoAdapter = ArrayAdapter.createFromResource(this,
                R.array.cursos_array_cadastro, android.R.layout.simple_spinner_item);
        cursoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurso.setAdapter(cursoAdapter);

        ArrayAdapter<CharSequence> semestreAdapter = ArrayAdapter.createFromResource(this,
                R.array.semestres_array_cadastro, android.R.layout.simple_spinner_item);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemestre.setAdapter(semestreAdapter);
    }

    private void cadastrarUsuario() {
        String nome = edtNome.getText().toString().trim();
        String cpf = MaskUtils.unmask(edtCpf.getText().toString());
        String telefone = MaskUtils.unmask(edtTelefone.getText().toString());
        String curso = spinnerCurso.getSelectedItem().toString();
        String semestre = spinnerSemestre.getSelectedItem().toString();
        String email = edtEmail.getText().toString().trim();
        String senha = edtSenha.getText().toString().trim();
        String confirmarSenha = edtConfirmarSenha.getText().toString().trim();

        if (nome.isEmpty() || cpf.isEmpty() || telefone.isEmpty() || email.isEmpty() || senha.isEmpty() || confirmarSenha.isEmpty() || curso.equals("Selecione o Curso") || semestre.equals("Selecione o Semestre")) {
            Toast.makeText(this, "Todos os campos são obrigatórios!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Por favor, insira um e-mail válido");
            return;
        }

        if (senha.length() < 6) {
            edtSenha.setError("A senha deve ter no mínimo 6 caracteres");
            return;
        }

        if (!senha.equals(confirmarSenha)) {
            edtConfirmarSenha.setError("As senhas não coincidem");
            return;
        }

        criarUsuarioNoAuth(nome, cpf, telefone, email, senha, curso, semestre);
    }

    private void criarUsuarioNoAuth(String nome, String cpf, String telefone, String email, String senha, String curso, String semestre) {
        mAuth.createUserWithEmailAndPassword(email, senha)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    String userId = user.getUid();
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("nome", nome);
                    userData.put("cpf", cpf);
                    userData.put("telefone", telefone);
                    userData.put("email", email);
                    userData.put("curso", curso);
                    userData.put("semestre", semestre);
                    userData.put("tipo", "USER");

                    db.collection("usuarios").document(userId).set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(CadastroActivity.this, "Cadastro realizado com sucesso!", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(CadastroActivity.this, UserActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(CadastroActivity.this, "Erro ao salvar dados: " + e.getMessage(), Toast.LENGTH_LONG).show());
                } else {
                    try {
                        throw task.getException();
                    } catch (FirebaseAuthWeakPasswordException e) {
                        edtSenha.setError("A senha é muito fraca.");
                    } catch (FirebaseAuthUserCollisionException e) {
                        edtEmail.setError("Este e-mail já está em uso.");
                    } catch (Exception e) {
                        Toast.makeText(CadastroActivity.this, "Erro ao cadastrar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
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
