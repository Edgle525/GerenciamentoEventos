package br.edu.fatecgru.Eventos.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class CadastroEventoActivity extends BaseActivity {

    private EditText edtNomeEvento, edtDataEvento, edtHorarioEvento, edtDescricaoEvento, edtDataTerminoEvento, edtHorarioTerminoEvento, edtLocalEvento, edtTempoMinimoPermanencia;
    private Button btnCadastrarEvento, btnGerarQRCode;
    private TextView tvCursosPermitidos;
    private CheckBox cbTempoTotal;
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
        cbTempoTotal = findViewById(R.id.cbTempoTotal);
        tvCursosPermitidos = findViewById(R.id.tvCursosPermitidos);
        btnCadastrarEvento = findViewById(R.id.btnCadastrarEvento);
        btnGerarQRCode = findViewById(R.id.btnGerarQRCode);

        setupDateTimePickers();
        cursosSelecionados.add("Geral");
        tvCursosPermitidos.setText("Cursos Permitidos: Geral");

        edtTempoMinimoPermanencia.setOnClickListener(v -> showDurationPickerDialog());

        cbTempoTotal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            edtTempoMinimoPermanencia.setEnabled(!isChecked);
            if (isChecked) {
                calculateAndSetTotalTime();
            } else {
                edtTempoMinimoPermanencia.setText("");
            }
        });

        tvCursosPermitidos.setOnClickListener(v -> showCursosDialog());
        btnCadastrarEvento.setOnClickListener(v -> cadastrarEvento());
    }

    private void showDurationPickerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_duration_picker, null);

        NumberPicker pickerHoras = view.findViewById(R.id.picker_horas);
        NumberPicker pickerMinutos = view.findViewById(R.id.picker_minutos);

        pickerHoras.setMinValue(0);
        pickerHoras.setMaxValue(23);

        pickerMinutos.setMinValue(0);
        pickerMinutos.setMaxValue(59);

        String currentTime = edtTempoMinimoPermanencia.getText().toString();
        if (!currentTime.isEmpty()) {
            try {
                String[] parts = currentTime.split(":");
                pickerHoras.setValue(Integer.parseInt(parts[0]));
                pickerMinutos.setValue(Integer.parseInt(parts[1]));
            } catch (Exception e) {
                // ignore
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecionar Duração")
                .setView(view)
                .setPositiveButton("OK", (dialog, which) -> {
                    String duracao = String.format(Locale.getDefault(), "%02d:%02d", pickerHoras.getValue(), pickerMinutos.getValue());
                    edtTempoMinimoPermanencia.setText(duracao);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void calculateAndSetTotalTime() {
        String dataInicioStr = edtDataEvento.getText().toString();
        String horarioInicioStr = edtHorarioEvento.getText().toString();
        String dataTerminoStr = edtDataTerminoEvento.getText().toString();
        String horarioTerminoStr = edtHorarioTerminoEvento.getText().toString();

        if (!dataInicioStr.isEmpty() && !horarioInicioStr.isEmpty() && !dataTerminoStr.isEmpty() && !horarioTerminoStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                Date dataInicio = sdf.parse(dataInicioStr + " " + horarioInicioStr);
                Date dataTermino = sdf.parse(dataTerminoStr + " " + horarioTerminoStr);

                long diff = dataTermino.getTime() - dataInicio.getTime();
                long diffMinutes = diff / (60 * 1000);

                int horas = (int) diffMinutes / 60;
                int minutos = (int) diffMinutes % 60;
                String tempoFormatado = String.format(Locale.getDefault(), "%02d:%02d", horas, minutos);

                edtTempoMinimoPermanencia.setText(tempoFormatado);
            } catch (ParseException e) {
                Toast.makeText(this, "Datas ou horários inválidos.", Toast.LENGTH_SHORT).show();
            }
        }
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
        String[] cursosArray = getResources().getStringArray(R.array.cursos_array);
        final List<String> cursosDialogList = new ArrayList<>(Arrays.asList(cursosArray));
        cursosDialogList.remove("Selecione o Curso");

        final String geralOption = "Geral";
        if (!cursosDialogList.contains(geralOption)) {
            cursosDialogList.add(0, geralOption);
        }

        final String[] cursosDialog = cursosDialogList.toArray(new String[0]);
        final boolean[] checkedItems = new boolean[cursosDialog.length];

        final ArrayList<String> tempCursosSelecionados = new ArrayList<>(cursosSelecionados);

        if (tempCursosSelecionados.contains(geralOption)) {
            Arrays.fill(checkedItems, true);
        } else {
            for (int i = 0; i < cursosDialog.length; i++) {
                checkedItems[i] = tempCursosSelecionados.contains(cursosDialog[i]);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Selecione os Cursos")
                .setMultiChoiceItems(cursosDialog, checkedItems, (dialog, which, isChecked) -> {
                    final AlertDialog alertDialog = (AlertDialog) dialog;
                    final ListView listView = alertDialog.getListView();
                    final int geralIndex = cursosDialogList.indexOf(geralOption);

                    if (which == geralIndex) {
                        for (int i = 0; i < cursosDialog.length; i++) {
                            listView.setItemChecked(i, isChecked);
                        }
                    } else {
                        if (!isChecked) {
                            listView.setItemChecked(geralIndex, false);
                        } else {
                            boolean allOthersChecked = true;
                            for (int i = 0; i < cursosDialog.length; i++) {
                                if (i == geralIndex) continue;
                                if (!listView.isItemChecked(i)) {
                                    allOthersChecked = false;
                                    break;
                                }
                            }
                            if (allOthersChecked) {
                                listView.setItemChecked(geralIndex, true);
                            }
                        }
                    }
                })
                .setPositiveButton("OK", (dialog, which) -> {
                    final AlertDialog alertDialog = (AlertDialog) dialog;
                    final ListView listView = alertDialog.getListView();
                    final int geralIndex = cursosDialogList.indexOf(geralOption);

                    cursosSelecionados.clear();

                    if (listView.isItemChecked(geralIndex)) {
                        cursosSelecionados.add(geralOption);
                        tvCursosPermitidos.setText("Cursos Permitidos: " + geralOption);
                    } else {
                        for (int i = 0; i < cursosDialog.length; i++) {
                            if (i == geralIndex) continue;
                            if (listView.isItemChecked(i)) {
                                cursosSelecionados.add(cursosDialog[i]);
                            }
                        }

                        if (cursosSelecionados.isEmpty()) {
                            cursosSelecionados.add(geralOption);
                            tvCursosPermitidos.setText("Cursos Permitidos: " + geralOption);
                        } else {
                            tvCursosPermitidos.setText("Cursos Permitidos: " + TextUtils.join(", ", cursosSelecionados));
                        }
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

        int tempoMinimoEmMinutos;
        try {
            String[] parts = tempoMinimoStr.split(":");
            int horas = Integer.parseInt(parts[0]);
            int minutos = Integer.parseInt(parts[1]);
            tempoMinimoEmMinutos = (horas * 60) + minutos;
        } catch (Exception e) {
            Toast.makeText(this, "Formato de tempo mínimo inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        Evento novoEvento = new Evento();
        novoEvento.setNome(nome);
        novoEvento.setData(data);
        novoEvento.setHorario(horario);
        novoEvento.setDataTermino(dataTermino);
        novoEvento.setHorarioTermino(horarioTermino);
        novoEvento.setLocal(local);
        novoEvento.setDescricao(descricao);
        novoEvento.setTempoMinimo(tempoMinimoEmMinutos);
        novoEvento.setTempoTotal(cbTempoTotal.isChecked());

        if (cursosSelecionados.isEmpty() || (cursosSelecionados.contains("Geral"))){
            novoEvento.setCursosPermitidos(Arrays.asList("Geral"));
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
