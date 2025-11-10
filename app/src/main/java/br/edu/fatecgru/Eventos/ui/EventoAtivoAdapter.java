package br.edu.fatecgru.Eventos.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class EventoAtivoAdapter extends ArrayAdapter<Evento> {

    private int expandedPosition = -1;
    private final InscricaoListener inscricaoListener;

    public interface InscricaoListener {
        void onInscreverClick(Evento evento);
    }

    public EventoAtivoAdapter(@NonNull Context context, @NonNull List<Evento> eventos, @NonNull InscricaoListener listener) {
        super(context, 0, eventos);
        this.inscricaoListener = listener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_evento_ativo, parent, false);
        }

        Evento evento = getItem(position);
        final boolean isExpanded = position == expandedPosition;

        if (evento != null) {
            TextView tvNomeEvento = convertView.findViewById(R.id.tvNomeEventoAtivo);
            TextView tvDataEvento = convertView.findViewById(R.id.tvDataEventoAtivo);
            TextView tvLocalEvento = convertView.findViewById(R.id.tvLocalEventoAtivo);
            LinearLayout layoutDetalhes = convertView.findViewById(R.id.layout_detalhes_evento);
            
            TextView tvHorarioEvento = convertView.findViewById(R.id.tvHorarioEvento);
            TextView tvHorarioTerminoEvento = convertView.findViewById(R.id.tvHorarioTerminoEvento);
            TextView tvTempoMinimo = convertView.findViewById(R.id.tvTempoMinimo);
            TextView tvCursosPermitidos = convertView.findViewById(R.id.tvCursosPermitidos);
            TextView tvDescricaoEvento = convertView.findViewById(R.id.tvDescricaoEvento);
            Button btnInscrever = convertView.findViewById(R.id.btnInscrever);

            tvNomeEvento.setText(evento.getNome());
            tvDataEvento.setText("Data: " + evento.getData());
            tvLocalEvento.setText("Local: " + evento.getLocal());

            layoutDetalhes.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            if (isExpanded) {
                tvHorarioEvento.setText("Horário de Início: " + evento.getHorario());
                tvHorarioTerminoEvento.setText("Horário de Término: " + evento.getHorarioTermino());
                tvTempoMinimo.setText("Tempo Mínimo: " + evento.getTempoMinimo() + " minutos");
                tvCursosPermitidos.setText("Cursos: " + String.join(", ", evento.getCursosPermitidos()));
                tvDescricaoEvento.setText(evento.getDescricao());
                btnInscrever.setOnClickListener(v -> {
                    if (inscricaoListener != null) {
                        inscricaoListener.onInscreverClick(evento);
                    }
                });
            }

            convertView.setOnClickListener(v -> {
                expandedPosition = isExpanded ? -1 : position;
                notifyDataSetChanged();
            });
        }

        return convertView;
    }
}
