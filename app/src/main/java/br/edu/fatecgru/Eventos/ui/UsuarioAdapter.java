package br.edu.fatecgru.Eventos.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Usuario;

public class UsuarioAdapter extends ArrayAdapter<Usuario> {

    public UsuarioAdapter(@NonNull Context context, @NonNull List<Usuario> usuarios) {
        super(context, 0, usuarios);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_usuario, parent, false);
        }

        Usuario usuario = getItem(position);

        if (usuario != null) {
            TextView tvNome = convertView.findViewById(R.id.tvNomeUsuario);
            TextView tvEmail = convertView.findViewById(R.id.tvEmailUsuario);
            TextView tvCurso = convertView.findViewById(R.id.tvCursoUsuario);

            tvNome.setText(usuario.getNome());
            tvEmail.setText(usuario.getEmail());
            tvCurso.setText("Curso: " + usuario.getCurso());
        }

        return convertView;
    }
}
