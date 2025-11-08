package br.edu.fatecgru.Eventos.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class CadastroEventoActivity extends BaseActivity {

    private EditText edtNomeEvento, edtDataEvento, edtHorarioEvento, edtDescricaoEvento;
    private Button btnCadastrarEvento, btnGerarQRCode;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro_evento);

        Toolbar toolbar = findViewById(R.id.toolbar_cadastro_evento);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cadastrar Evento");
        }

        db = FirebaseFirestore.getInstance();

        edtNomeEvento = findViewById(R.id.edtNomeEvento);
        edtDataEvento = findViewById(R.id.edtDataEvento);
        edtHorarioEvento = findViewById(R.id.edtHorarioEvento);
        edtDescricaoEvento = findViewById(R.id.edtDescricaoEvento);
        btnCadastrarEvento = findViewById(R.id.btnCadastrarEvento);
        btnGerarQRCode = findViewById(R.id.btnGerarQRCode);

        setupDateTimePickers();

        btnCadastrarEvento.setOnClickListener(v -> cadastrarEvento());
    }

    private void setupDateTimePickers() {
        edtDataEvento.setFocusable(false);
        edtDataEvento.setOnClickListener(v -> showDatePickerDialog());

        edtHorarioEvento.setFocusable(false);
        edtHorarioEvento.setOnClickListener(v -> showTimePickerDialog());
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    edtDataEvento.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    edtHorarioEvento.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void cadastrarEvento() {
        String nome = edtNomeEvento.getText().toString().trim();
        String data = edtDataEvento.getText().toString().trim();
        String horario = edtHorarioEvento.getText().toString().trim();
        String descricao = edtDescricaoEvento.getText().toString().trim();

        if (!validateInput(nome, data, horario, descricao)) {
            return;
        }

        Evento novoEvento = new Evento();
        novoEvento.setNome(nome);
        novoEvento.setData(data);
        novoEvento.setHorario(horario);
        novoEvento.setDescricao(descricao);

        db.collection("eventos").add(novoEvento)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Evento cadastrado com sucesso!", Toast.LENGTH_LONG).show();
                    String eventoId = documentReference.getId();

                    edtNomeEvento.setEnabled(false);
                    edtDataEvento.setEnabled(false);
                    edtHorarioEvento.setEnabled(false);
                    edtDescricaoEvento.setEnabled(false);
                    btnCadastrarEvento.setEnabled(false);

                    btnGerarQRCode.setVisibility(View.VISIBLE);
                    btnGerarQRCode.setOnClickListener(v -> {
                        Intent intent = new Intent(CadastroEventoActivity.this, EventQRCodeActivity.class);
                        intent.putExtra("evento_id", eventoId);
                        intent.putExtra("nome_evento", nome);
                        intent.putExtra("data_evento", data);
                        intent.putExtra("horario_evento", horario);
                        startActivity(intent);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w("CadastroEvento", "Erro ao cadastrar evento", e);
                    Toast.makeText(this, "Erro ao cadastrar evento: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean validateInput(String nome, String data, String horario, String descricao) {
        if (TextUtils.isEmpty(nome)) {
            edtNomeEvento.setError("O nome do evento é obrigatório.");
            return false;
        }
        if (TextUtils.isEmpty(data)) {
            edtDataEvento.setError("A data do evento é obrigatória.");
            return false;
        }
        if (TextUtils.isEmpty(horario)) {
            edtHorarioEvento.setError("O horário do evento é obrigatório.");
            return false;
        }
        if (isDateTimeInPast(data, horario)) {
            Toast.makeText(this, "Não é possível cadastrar um evento em uma data ou horário que já passou.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (TextUtils.isEmpty(descricao)) {
            edtDescricaoEvento.setError("A descrição do evento é obrigatória.");
            return false;
        }
        return true;
    }

    private boolean isDateTimeInPast(String dateStr, String timeStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Calendar selectedDateTime = Calendar.getInstance();
            selectedDateTime.setTime(sdf.parse(dateStr + " " + timeStr));

            return selectedDateTime.before(Calendar.getInstance());
        } catch (Exception e) {
            return true;
        }
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
