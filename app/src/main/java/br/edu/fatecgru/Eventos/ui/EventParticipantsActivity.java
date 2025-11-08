package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Inscricao;
import br.edu.fatecgru.Eventos.model.Usuario;

public class EventParticipantsActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE_PERMISSION = 3;

    private ListView listViewParticipants;
    private TextView tvEventName;
    private Button btnBaixarListaPdf;
    private FirebaseFirestore db;
    private String eventoId;
    private List<Usuario> participantsList = new ArrayList<>();
    private ParticipantAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_participants);

        listViewParticipants = findViewById(R.id.listViewParticipants);
        tvEventName = findViewById(R.id.tvEventName);
        btnBaixarListaPdf = findViewById(R.id.btnBaixarListaPdf);
        db = FirebaseFirestore.getInstance();
        eventoId = getIntent().getStringExtra("EVENTO_ID");

        db.collection("eventos").document(eventoId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                tvEventName.setText("Participantes: " + documentSnapshot.getString("nome"));
            }
        });

        adapter = new ParticipantAdapter(this, participantsList);
        listViewParticipants.setAdapter(adapter);
        loadParticipants();

        btnBaixarListaPdf.setOnClickListener(v -> {
            if (checkAndRequestStoragePermission()) {
                createPdfFromParticipantList();
            }
        });
    }

    private boolean checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private void createPdfFromParticipantList() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12);

        float y = 40;
        canvas.drawText("Lista de Participantes - " + tvEventName.getText().toString(), 40, y, paint);
        y += (paint.descent() - paint.ascent()) * 2;

        for (Usuario user : participantsList) {
            canvas.drawText("Nome: " + user.getNome(), 40, y, paint);
            y += paint.descent() - paint.ascent();
            canvas.drawText("Curso: " + user.getCurso(), 40, y, paint);
            y += paint.descent() - paint.ascent();
            canvas.drawText("Semestre: " + user.getSemestre(), 40, y, paint);
            y += (paint.descent() - paint.ascent()) * 2;
        }

        document.finishPage(page);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, "ListaParticipantes-" + tvEventName.getText().toString().replaceAll("[^a-zA-Z0-9]", "-") + ".pdf");
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();
            Toast.makeText(this, "PDF da lista salvo em Downloads!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao salvar PDF da lista: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void loadParticipants() {
        db.collection("inscricoes").whereEqualTo("idEvento", eventoId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        participantsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Inscricao inscricao = document.toObject(Inscricao.class);
                            db.collection("usuarios").document(inscricao.getIdUsuario()).get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            Usuario user = userDoc.toObject(Usuario.class);
                                            if(user != null) {
                                                participantsList.add(user);
                                                adapter.notifyDataSetChanged();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private class ParticipantAdapter extends ArrayAdapter<Usuario> {

        public ParticipantAdapter(Context context, List<Usuario> users) {
            super(context, 0, users);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_participant, parent, false);
            }

            TextView tvName = convertView.findViewById(R.id.tvParticipantName);
            TextView tvCourse = convertView.findViewById(R.id.tvParticipantCourse);
            TextView tvSemester = convertView.findViewById(R.id.tvParticipantSemester);

            Usuario user = getItem(position);

            if (user != null) {
                tvName.setText(user.getNome());
                tvCourse.setText("Curso: " + user.getCurso());
                tvSemester.setText("Semestre: " + user.getSemestre());
            }

            return convertView;
        }
    }

     @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createPdfFromParticipantList();
            } else {
                Toast.makeText(this, "Permissão de armazenamento negada. Não é possível salvar o PDF.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
