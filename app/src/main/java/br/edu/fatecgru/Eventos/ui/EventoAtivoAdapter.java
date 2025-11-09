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

public class EventoAtivoAdapter extends ArrayAdapter<Evento> {

    public EventoAtivoAdapter(@NonNull Context context, @NonNull List<Evento> eventos) {
        super(context, 0, eventos);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_evento_ativo, parent, false);
        }

        Evento evento = getItem(position);

        if (evento != null) {
            TextView tvNomeEvento = convertView.findViewById(R.id.tvNomeEventoAtivo);
            TextView tvDataEvento = convertView.findViewById(R.id.tvDataEventoAtivo);
            TextView tvLocalEvento = convertView.findViewById(R.id.tvLocalEventoAtivo);

            tvNomeEvento.setText(evento.getNome());
            tvDataEvento.setText("Data: " + evento.getData());
            tvLocalEvento.setText("Local: " + evento.getLocal());
        }

        return convertView;
    }
}