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
import br.edu.fatecgru.Eventos.model.Evento;

public class EventoFinalizadoAdapter extends ArrayAdapter<Evento> {

    public EventoFinalizadoAdapter(@NonNull Context context, @NonNull List<Evento> eventos) {
        super(context, 0, eventos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_evento_finalizado, parent, false);
        }

        Evento evento = getItem(position);

        if (evento != null) {
            TextView tvNomeEvento = convertView.findViewById(R.id.tvNomeEvento);
            TextView tvDataTermino = convertView.findViewById(R.id.tvDataTermino);

            tvNomeEvento.setText(evento.getNome());
            tvDataTermino.setText(evento.getDataTermino() + " " + evento.getHorarioTermino());
        }

        return convertView;
    }
}
