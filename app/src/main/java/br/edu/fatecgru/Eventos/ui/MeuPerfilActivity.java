package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.util.MaskUtils;
import de.hdodenhof.circleimageview.CircleImageView;

public class MeuPerfilActivity extends BaseActivity {

    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int MAX_IMAGE_DIMENSION = 400;

    private EditText edtNome, edtCpf, edtTelefone, edtEmail;
    private Spinner spinnerCurso, spinnerSemestre;
    private Button btnSalvar, btnExcluir;
    private CircleImageView profileImageView;
    private FloatingActionButton fabEditPhoto;

    private FirebaseFirestore db;
    private DocumentReference userRef;
    private FirebaseUser currentUser;

    private String imageBase64;
    private boolean isProfileComplete = false;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri == null) return;
                    try {
                        Bitmap processedBitmap = processImage(imageUri);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        processedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        imageBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT);
                        Glide.with(this).load(processedBitmap).into(profileImageView);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Falha ao processar a imagem.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meu_perfil);

        Toolbar toolbar = findViewById(R.id.toolbar_meu_perfil);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Meu Perfil");
        }

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        edtNome = findViewById(R.id.edtNomePerfil);
        edtCpf = findViewById(R.id.edtCpfPerfil);
        spinnerCurso = findViewById(R.id.spinnerCursoPerfil);
        spinnerSemestre = findViewById(R.id.spinnerSemestrePerfil);
        edtTelefone = findViewById(R.id.edtTelefonePerfil);
        edtEmail = findViewById(R.id.edtEmailPerfil);
        btnSalvar = findViewById(R.id.btnSalvarPerfil);
        btnExcluir = findViewById(R.id.btnExcluirPerfil);
        profileImageView = findViewById(R.id.profile_image);
        fabEditPhoto = findViewById(R.id.fab_edit_photo);

        setupSpinners();

        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = db.collection("usuarios").document(currentUser.getUid());
        loadUserProfile();

        fabEditPhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnSalvar.setOnClickListener(v -> saveUserProfile());
        btnExcluir.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> cursoAdapter = ArrayAdapter.createFromResource(this, R.array.cursos_array_cadastro, android.R.layout.simple_spinner_item);
        cursoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurso.setAdapter(cursoAdapter);

        ArrayAdapter<CharSequence> semestreAdapter = ArrayAdapter.createFromResource(this, R.array.semestres_array_cadastro, android.R.layout.simple_spinner_item);
        semestreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemestre.setAdapter(semestreAdapter);
    }

    private void loadUserProfile() {
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                edtNome.setText(documentSnapshot.getString("nome"));
                edtEmail.setText(documentSnapshot.getString("email"));

                String cpf = documentSnapshot.getString("cpf");
                String curso = documentSnapshot.getString("curso");

                isProfileComplete = !TextUtils.isEmpty(cpf) && !TextUtils.isEmpty(curso) && !curso.equals("Selecione o Curso");

                if (isProfileComplete) {
                    edtNome.setEnabled(false);
                    edtCpf.setText(MaskUtils.formatCpf(cpf));
                    edtCpf.setEnabled(false);
                    setSpinnerSelection(spinnerCurso, curso);
                    spinnerCurso.setEnabled(false);
                } else {
                    edtNome.setEnabled(true);
                    edtCpf.setEnabled(true);
                    edtCpf.setText(cpf);
                    edtCpf.addTextChangedListener(MaskUtils.insert("###.###.###-##", edtCpf));

                    spinnerCurso.setEnabled(true);
                    if (curso != null) {
                        setSpinnerSelection(spinnerCurso, curso);
                    }
                }

                if (documentSnapshot.contains("profileImageBase64")) {
                    String base64 = documentSnapshot.getString("profileImageBase64");
                    if (base64 != null && !base64.isEmpty()) {
                        byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        Glide.with(this).load(decodedByte).into(profileImageView);
                    }
                }

                String telefone = documentSnapshot.getString("telefone");
                edtTelefone.setText(telefone);
                edtTelefone.addTextChangedListener(MaskUtils.insert("(##) #####-####", edtTelefone));

                String semestre = documentSnapshot.getString("semestre");
                if (!TextUtils.isEmpty(semestre)) {
                    setSpinnerSelection(spinnerSemestre, semestre);
                }

            } else {
                Toast.makeText(this, "Dados do perfil não encontrados.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar o perfil.", Toast.LENGTH_SHORT).show());
    }

    private void showKeyboard(View view) {
        if (view.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean validateInputs() {
        if (edtNome.isEnabled() && TextUtils.isEmpty(edtNome.getText().toString().trim())) {
            edtNome.setError("Nome é obrigatório.");
            showKeyboard(edtNome);
            return false;
        }

        if (edtCpf.isEnabled()) {
            String unmaskedCpf = MaskUtils.unmask(edtCpf.getText().toString());
            if (TextUtils.isEmpty(unmaskedCpf)) {
                edtCpf.setError("CPF é obrigatório.");
                showKeyboard(edtCpf);
                return false;
            }
            if (unmaskedCpf.length() != 11) {
                edtCpf.setError("CPF inválido. Deve conter 11 dígitos.");
                showKeyboard(edtCpf);
                return false;
            }
        }

        String unmaskedTelefone = MaskUtils.unmask(edtTelefone.getText().toString());
        if (TextUtils.isEmpty(unmaskedTelefone)) {
            edtTelefone.setError("Telefone é obrigatório.");
            showKeyboard(edtTelefone);
            return false;
        }
        if (unmaskedTelefone.length() < 10) { // (##)####-#### (10) or (##)#####-#### (11)
            edtTelefone.setError("Telefone inválido.");
            showKeyboard(edtTelefone);
            return false;
        }

        if (spinnerCurso.isEnabled() && spinnerCurso.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione um curso válido.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerSemestre.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Selecione um semestre válido.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void saveUserProfile() {
        hideKeyboard();

        if (!validateInputs()) {
            return;
        }

        if (isProfileComplete) {
            proceedWithUpdate();
        } else {
            String unmaskedCpf = MaskUtils.unmask(edtCpf.getText().toString());
            db.collection("usuarios").whereEqualTo("cpf", unmaskedCpf).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean cpfInUseByOther = false;
                        for (DocumentSnapshot doc : task.getResult()) {
                            if (!doc.getId().equals(currentUser.getUid())) {
                                cpfInUseByOther = true;
                                break;
                            }
                        }

                        if (cpfInUseByOther) {
                            edtCpf.setError("Este CPF já está em uso.");
                            showKeyboard(edtCpf);
                        } else {
                            showConfirmationDialog();
                        }
                    } else {
                        Toast.makeText(this, "Erro ao verificar CPF.", Toast.LENGTH_SHORT).show();
                    }
                });
        }
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Confirmar Finalização do Cadastro")
            .setMessage("Você está prestes a finalizar seu cadastro. Após salvar, o Nome, CPF e Curso não poderão ser alterados. Deseja continuar?")
            .setPositiveButton("Confirmar", (dialog, which) -> proceedWithUpdate())
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void proceedWithUpdate() {
        Map<String, Object> updates = new HashMap<>();

        if (!isProfileComplete) {
            updates.put("nome", edtNome.getText().toString().trim());
            updates.put("cpf", MaskUtils.unmask(edtCpf.getText().toString()));
            updates.put("curso", spinnerCurso.getSelectedItem().toString());
        }

        updates.put("semestre", spinnerSemestre.getSelectedItem().toString());
        updates.put("telefone", MaskUtils.unmask(edtTelefone.getText().toString()));
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            updates.put("profileImageBase64", imageBase64);
        }

        if (updates.isEmpty()) {
            Toast.makeText(this, "Nenhuma alteração para salvar.", Toast.LENGTH_SHORT).show();
            return;
        }

        userRef.update(updates).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> Toast.makeText(this, "Erro ao salvar alterações.", Toast.LENGTH_SHORT).show());
    }

    private void checkPermissionAndOpenGallery() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private Bitmap processImage(Uri imageUri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            ExifInterface exifInterface = new ExifInterface(inputStream);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            try (InputStream bitmapStream = getContentResolver().openInputStream(imageUri)) {
                Bitmap originalBitmap = BitmapFactory.decodeStream(bitmapStream);
                Bitmap rotatedBitmap = rotateBitmap(originalBitmap, orientation);
                return resizeBitmap(rotatedBitmap, MAX_IMAGE_DIMENSION);
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.preScale(-1.0f, 1.0f);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.preScale(1.0f, -1.0f);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }
            return rotatedBitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int newWidth, newHeight;

        if (originalWidth > originalHeight) {
            newWidth = maxDimension;
            newHeight = (int) (originalHeight * (float) maxDimension / originalWidth);
        } else if (originalHeight > originalWidth) {
            newHeight = maxDimension;
            newWidth = (int) (originalWidth * (float) maxDimension / originalHeight);
        } else {
            newWidth = maxDimension;
            newHeight = maxDimension;
        }

        if (newWidth == originalWidth && newHeight == originalHeight) return bitmap;

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permissão negada.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();
        if (value != null) {
            int spinnerPosition = adapter.getPosition(value);
            spinner.setSelection(spinnerPosition);
        }
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir seu perfil? Esta ação é permanente e não pode ser desfeita.")
                .setPositiveButton("Sim, excluir", (dialog, which) -> deleteUserAccount())
                .setNegativeButton("Não", null)
                .show();
    }

    private void deleteUserAccount() {
        userRef.delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.delete().addOnCompleteListener(authTask -> {
                    if (authTask.isSuccessful()) {
                        Toast.makeText(this, "Perfil excluído com sucesso.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Falha ao excluir autenticação. Tente fazer login novamente.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(this, "Falha ao excluir dados do perfil.", Toast.LENGTH_SHORT).show();
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
}
