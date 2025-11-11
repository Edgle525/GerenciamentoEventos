package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class EditarEventoActivity extends BaseActivity {

    private EditText edtNome, edtData, edtHorario, edtDescricao, edtDataTermino, edtHorarioTermino, edtLocal, edtTempoMinimo;
    private TextView tvCursosPermitidos;
    private Button btnSalvar, btnExcluir;
    private FirebaseFirestore db;
    private String eventoId;
    private ArrayList<String> cursosSelecionados = new ArrayList<>();
    private static final String TAG = "EditarEventoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_evento);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Editar Evento");
        }

        db = FirebaseFirestore.getInstance();
        eventoId = getIntent().getStringExtra("EVENTO_ID");

        edtNome = findViewById(R.id.edtNomeEventoEditar);
        edtData = findViewById(R.id.edtDataEventoEditar);
        edtHorario = findViewById(R.id.edtHorarioEventoEditar);
        edtDataTermino = findViewById(R.id.edtDataTerminoEventoEditar);
        edtHorarioTermino = findViewById(R.id.edtHorarioTerminoEventoEditar);
        edtLocal = findViewById(R.id.edtLocalEventoEditar);
        edtDescricao = findViewById(R.id.edtDescricaoEventoEditar);
        edtTempoMinimo = findViewById(R.id.edtTempoMinimoPermanenciaEditar);
        tvCursosPermitidos = findViewById(R.id.tvCursosPermitidosEditar);
        btnSalvar = findViewById(R.id.btnSalvarEdicaoEvento);
        btnExcluir = findViewById(R.id.btnExcluirEvento);

        loadEvento();

        edtTempoMinimo.setFocusable(false);
        edtTempoMinimo.setOnClickListener(v -> showDurationPickerDialog());
        tvCursosPermitidos.setOnClickListener(v -> showCursosDialog());
        btnSalvar.setOnClickListener(v -> salvarAlteracoes());
        btnExcluir.setOnClickListener(v -> confirmarArquivamento());
    }

    private void showDurationPickerDialog() {
        int hour = 0;
        int minute = 0;
        String currentVal = edtTempoMinimo.getText().toString();
        if (!currentVal.isEmpty() && currentVal.contains(":")) {
            String[] parts = currentVal.split(":");
            hour = Integer.parseInt(parts[0]);
            minute = Integer.parseInt(parts[1]);
        }

        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .setTitleText("Duração Mínima (HH:mm)")
                .build();

        picker.show(getSupportFragmentManager(), "duration_picker");

        picker.addOnPositiveButtonClickListener(v -> {
            int selectedHour = picker.getHour();
            int selectedMinute = picker.getMinute();
            String duration = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
            edtTempoMinimo.setText(duration);
        });
    }

    private void loadEvento() {
        if (eventoId == null) return;

        db.collection("eventos").document(eventoId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Evento evento = documentSnapshot.toObject(Evento.class);
                        if (evento != null) {
                            edtNome.setText(evento.getNome());
                            edtData.setText(evento.getData());
                            edtHorario.setText(evento.getHorario());
                            edtDataTermino.setText(evento.getDataTermino());
                            edtHorarioTermino.setText(evento.getHorarioTermino());
                            edtLocal.setText(evento.getLocal());
                            edtDescricao.setText(evento.getDescricao());

                            int totalMinutes = evento.getTempoMinimo();
                            int hours = totalMinutes / 60;
                            int minutes = totalMinutes % 60;
                            String tempoFormatado = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes);
                            edtTempoMinimo.setText(tempoFormatado);

                            if (evento.getCursosPermitidos() != null && !evento.getCursosPermitidos().isEmpty()) {
                                cursosSelecionados.clear(); // Clear before loading
                                cursosSelecionados.addAll(evento.getCursosPermitidos());
                                if (cursosSelecionados.isEmpty()) {
                                    tvCursosPermitidos.setText("Selecione os cursos permitidos");
                                } else {
                                    tvCursosPermitidos.setText("Cursos Permitidos: " + TextUtils.join(", ", cursosSelecionados));
                                }
                            } else {
                                tvCursosPermitidos.setText("Selecione os cursos permitidos");
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao carregar dados do evento.", Toast.LENGTH_SHORT).show());
    }

    private void showCursosDialog() {
        String[] cursos = getResources().getStringArray(R.array.cursos_array);
        List<String> cursosDialogList = new ArrayList<>(Arrays.asList(cursos));
        cursosDialogList.remove("Selecione o Curso");
        String[] cursosDialog = cursosDialogList.toArray(new String[0]);
        boolean[] checkedItems = new boolean[cursosDialog.length];

        // Create a temporary list for selections in the dialog
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

                if (cursosSelecionados.isEmpty()) {
                    tvCursosPermitidos.setText("Selecione os cursos permitidos");
                } else {
                    tvCursosPermitidos.setText("Cursos Permitidos: " + TextUtils.join(", ", cursosSelecionados));
                }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void salvarAlteracoes() {
        String nome = edtNome.getText().toString().trim();
        String data = edtData.getText().toString().trim();
        String horario = edtHorario.getText().toString().trim();
        String dataTermino = edtDataTermino.getText().toString().trim();
        String horarioTermino = edtHorarioTermino.getText().toString().trim();
        String local = edtLocal.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();
        String tempoMinimoStr = edtTempoMinimo.getText().toString().trim();

        if (cursosSelecionados.isEmpty()) {
            Toast.makeText(this, "Por favor, selecione ao menos um curso.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (nome.isEmpty() || data.isEmpty() || horario.isEmpty() || dataTermino.isEmpty() || horarioTermino.isEmpty() || local.isEmpty() || descricao.isEmpty() || tempoMinimoStr.isEmpty()) {
            Toast.makeText(this, "Todos os campos são obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isEndAfterStart(data, horario, dataTermino, horarioTermino)) {
            return;
        }

        int tempoMinimo;
        if (tempoMinimoStr.contains(":")) {
            String[] parts = tempoMinimoStr.split(":");
            tempoMinimo = Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } else {
            tempoMinimo = Integer.parseInt(tempoMinimoStr);
        }

        db.collection("eventos").document(eventoId)
                .update("nome", nome,
                        "data", data,
                        "horario", horario,
                        "dataTermino", dataTermino,
                        "horarioTermino", horarioTermino,
                        "local", local,
                        "descricao", descricao,
                        "tempoMinimo", tempoMinimo,
                        "cursosPermitidos", cursosSelecionados)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Evento atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Erro ao atualizar evento.", Toast.LENGTH_SHORT).show());
    }

    private boolean isEndAfterStart(String startDate, String startTime, String endDate, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date start = sdf.parse(startDate + " " + startTime);
            Date end = sdf.parse(endDate + " " + endTime);
            if (!end.after(start)) {
                Toast.makeText(this, "O horário de término deve ser posterior ao horário de início.", Toast.LENGTH_LONG).show();
                return false;
            }
        } catch (ParseException e) {
            Toast.makeText(this, "Formato de data ou hora inválido.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void confirmarArquivamento() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar Arquivamento")
                .setMessage("Tem certeza que deseja arquivar este evento? Ele não será mais exibido, mas o histórico de participação será mantido.")
                .setPositiveButton("Arquivar", (dialog, which) -> arquivarEvento())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void arquivarEvento() {
        if (eventoId == null) return;

        db.collection("eventos").document(eventoId)
                .update("arquivado", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditarEventoActivity.this, "Evento arquivado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditarEventoActivity.this, "Erro ao arquivar evento.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Erro ao arquivar evento", e);
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
