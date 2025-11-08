package br.edu.fatecgru.Eventos.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.StaticLayout;
import android.text.TextPaint;
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
    private String eventoNome;
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
                eventoNome = documentSnapshot.getString("nome");
                tvEventName.setText("Participantes: " + eventoNome);
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
        if (participantsList.isEmpty()) {
            Toast.makeText(this, "Não há participantes para gerar a lista.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        TextPaint paint = new TextPaint();
        paint.setTextSize(12);

        int margin = 40;
        int pageContentWidth = pageInfo.getPageWidth() - 2 * margin;
        int pageContentHeight = pageInfo.getPageHeight() - 2 * margin;
        float y = margin;
        int pageNumber = 1;

        String title = "Lista de Participantes - " + (eventoNome != null ? eventoNome : "");
        StaticLayout titleLayout = new StaticLayout(title, paint, pageContentWidth, android.text.Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        canvas.save();
        canvas.translate(margin, y);
        titleLayout.draw(canvas);
        canvas.restore();
        y += titleLayout.getHeight() + 20;

        for (Usuario user : participantsList) {
            String userText = "Nome: " + user.getNome() + "\n" +
                              "Curso: " + user.getCurso() + "\n" +
                              "Semestre: " + user.getSemestre() + "\n";
            StaticLayout userLayout = new StaticLayout(userText, paint, pageContentWidth, android.text.Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            if (y + userLayout.getHeight() > pageContentHeight) {
                document.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create();
                page = document.startPage(pageInfo);
                canvas = page.getCanvas();
                y = margin;
            }

            canvas.save();
            canvas.translate(margin, y);
            userLayout.draw(canvas);
            canvas.restore();
            y += userLayout.getHeight() + 10; // Espaçamento entre participantes
        }

        try {
            Bitmap logo = BitmapFactory.decodeResource(getResources(), R.drawable.cps_transparente);
            if (logo != null) {
                int logoWidth = 200;
                int logoHeight = (int) (logo.getHeight() * ((float) logoWidth / logo.getWidth()));
                Bitmap scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth, logoHeight, true);

                if (y + scaledLogo.getHeight() + 20 > pageContentHeight) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, ++pageNumber).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                }

                float logoX = (pageInfo.getPageWidth() - scaledLogo.getWidth()) / 2f;
                float logoY = pageInfo.getPageHeight() - scaledLogo.getHeight() - margin;
                canvas.drawBitmap(scaledLogo, logoX, logoY, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        document.finishPage(page);

        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            String baseName = "ListaParticipantes-" + (eventoNome != null ? eventoNome.replaceAll("[^a-zA-Z0-9]", "-") : "Geral");
            File file = new File(downloadsDir, baseName + ".pdf");
            int count = 1;
            while(file.exists()){
                file = new File(downloadsDir, baseName + "-" + count + ".pdf");
                count++;
            }
            
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
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(EventParticipantsActivity.this, "Nenhum participante inscrito neste evento.", Toast.LENGTH_SHORT).show();
                            return;
                        }
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
