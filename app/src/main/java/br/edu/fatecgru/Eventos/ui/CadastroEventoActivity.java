package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class CadastroEventoActivity extends BaseActivity {

    private EditText edtNomeEvento, edtDataEvento, edtHorarioEvento, edtDescricaoEvento, edtDataTerminoEvento, edtHorarioTerminoEvento, edtLocalEvento, edtTempoMinimoPermanencia;
    private Button btnCadastrarEvento, btnGerarQRCode;
    private TextView tvCursosPermitidos;
    private FirebaseFirestore db;
    private ArrayList<String> cursosSelecionados = new ArrayList<>();

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
        edtDataTerminoEvento = findViewById(R.id.edtDataTerminoEvento);
        edtHorarioTerminoEvento = findViewById(R.id.edtHorarioTerminoEvento);
        edtLocalEvento = findViewById(R.id.edtLocalEvento);
        edtDescricaoEvento = findViewById(R.id.edtDescricaoEvento);
        edtTempoMinimoPermanencia = findViewById(R.id.edtTempoMinimoPermanencia);
        tvCursosPermitidos = findViewById(R.id.tvCursosPermitidos);
        btnCadastrarEvento = findViewById(R.id.btnCadastrarEvento);
        btnGerarQRCode = findViewById(R.id.btnGerarQRCode);

        setupDateTimePickers();
        cursosSelecionados.add("Todos"); // Default to all courses

        tvCursosPermitidos.setOnClickListener(v -> showCursosDialog());
        btnCadastrarEvento.setOnClickListener(v -> cadastrarEvento());
    }

    private void setupDateTimePickers() {
        edtDataEvento.setFocusable(false);
        edtDataEvento.setOnClickListener(v -> showDatePickerDialog(edtDataEvento));

        edtHorarioEvento.setFocusable(false);
        edtHorarioEvento.setOnClickListener(v -> showTimePickerDialog(edtHorarioEvento));

        edtDataTerminoEvento.setFocusable(false);
        edtDataTerminoEvento.setOnClickListener(v -> showDatePickerDialog(edtDataTerminoEvento));

        edtHorarioTerminoEvento.setFocusable(false);
        edtHorarioTerminoEvento.setOnClickListener(v -> showTimePickerDialog(edtHorarioTerminoEvento));
    }

    private void showDatePickerDialog(EditText dateField) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    dateField.setText(sdf.format(selectedDate.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showTimePickerDialog(EditText timeField) {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    timeField.setText(time);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void showCursosDialog() {
        String[] cursos = getResources().getStringArray(R.array.cursos_array);
        List<String> cursosDialogList = new ArrayList<>(Arrays.asList(cursos));
        cursosDialogList.remove("Selecione o Curso");
        String[] cursosDialog = cursosDialogList.toArray(new String[0]);
        boolean[] checkedItems = new boolean[cursosDialog.length];

        ArrayList<String> tempCursosSelecionados = new ArrayList<>(cursosSelecionados);

        for (int i = 0; i < cursosDialog.length; i++) {
            if (tempCursosSelecionados.contains(cursosDialog[i])) {
                checkedItems[i] = true;
            }
        }

        new AlertDialog.Builder(this)
            .setTitle("Selecione os Cursos")
            .setMultiChoiceItems(cursosDialog, checkedItems, (dialog, which, isChecked) -> {
                if (isChecked) {
                    tempCursosSelecionados.add(cursosDialog[which]);
                } else {
                    tempCursosSelecionados.remove(cursosDialog[which]);
                }
            })
            .setPositiveButton("OK", (dialog, which) -> {
                cursosSelecionados.clear();
                cursosSelecionados.addAll(tempCursosSelecionados);

                if (cursosSelecionados.isEmpty() || cursosSelecionados.size() == cursosDialog.length) {
                    cursosSelecionados.clear(); 
                    cursosSelecionados.add("Todos");
                    tvCursosPermitidos.setText("Cursos Permitidos: Todos");
                } else {
                    cursosSelecionados.remove("Todos");
                    tvCursosPermitidos.setText("Cursos Permitidos: " + TextUtils.join(", ", cursosSelecionados));
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void cadastrarEvento() {
        String nome = edtNomeEvento.getText().toString().trim();
        String data = edtDataEvento.getText().toString().trim();
        String horario = edtHorarioEvento.getText().toString().trim();
        String dataTermino = edtDataTerminoEvento.getText().toString().trim();
        String horarioTermino = edtHorarioTerminoEvento.getText().toString().trim();
        String local = edtLocalEvento.getText().toString().trim();
        String descricao = edtDescricaoEvento.getText().toString().trim();
        String tempoMinimoStr = edtTempoMinimoPermanencia.getText().toString().trim();

        if (!validateInput(nome, data, horario, dataTermino, horarioTermino, local, descricao, tempoMinimoStr)) {
            return;
        }

        int tempoMinimo = Integer.parseInt(tempoMinimoStr);

        Evento novoEvento = new Evento();
        novoEvento.setNome(nome);
        novoEvento.setData(data);
        novoEvento.setHorario(horario);
        novoEvento.setDataTermino(dataTermino);
        novoEvento.setHorarioTermino(horarioTermino);
        novoEvento.setLocal(local);
        novoEvento.setDescricao(descricao);
        novoEvento.setTempoMinimo(tempoMinimo);

        if (cursosSelecionados.isEmpty() || (cursosSelecionados.contains("Todos"))){
            novoEvento.setCursosPermitidos(Arrays.asList("Todos"));
        } else {
            novoEvento.setCursosPermitidos(cursosSelecionados);
        }

        db.collection("eventos").add(novoEvento)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Evento cadastrado com sucesso!", Toast.LENGTH_LONG).show();
                    String eventoId = documentReference.getId();

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

    private boolean validateInput(String nome, String data, String horario, String dataTermino, String horarioTermino, String local, String descricao, String tempoMinimoStr) {
        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(data) || TextUtils.isEmpty(horario) || TextUtils.isEmpty(dataTermino) || TextUtils.isEmpty(horarioTermino) || TextUtils.isEmpty(local) || TextUtils.isEmpty(descricao) || TextUtils.isEmpty(tempoMinimoStr)) {
            Toast.makeText(this, "Todos os campos são obrigatórios", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (isDateTimeInPast(data, horario)) {
            Toast.makeText(this, "A data/hora de início não pode ser no passado.", Toast.LENGTH_LONG).show();
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
