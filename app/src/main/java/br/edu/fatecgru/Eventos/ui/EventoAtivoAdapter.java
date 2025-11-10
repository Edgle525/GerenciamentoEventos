package br.edu.fatecgru.Eventos.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;
import br.edu.fatecgru.Eventos.R;
import br.edu.fatecgru.Eventos.model.Evento;

public class EventoAtivoAdapter extends ArrayAdapter<Evento> {

    private final OnInscreverClickListener mListener;

    public interface OnInscreverClickListener {
        void onInscreverClick(Evento evento);
    }

    // Constructor for UserActivity
    public EventoAtivoAdapter(@NonNull Context context, @NonNull List<Evento> eventos, @Nullable OnInscreverClickListener listener) {
        super(context, 0, eventos);
        mListener = listener;
    }

    // Constructor for ListarEventosAdminActivity
    public EventoAtivoAdapter(@NonNull Context context, @NonNull List<Evento> eventos) {
        this(context, eventos, null);
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
            ImageView expandIcon = convertView.findViewById(R.id.expand_icon);

            LinearLayout hiddenPart = convertView.findViewById(R.id.hidden_part);
            TextView tvDescricao = convertView.findViewById(R.id.tvDescricaoEvento);
            TextView tvHorarioInicio = convertView.findViewById(R.id.tvHorarioInicio);
            TextView tvHorarioTermino = convertView.findViewById(R.id.tvHorarioTermino);
            Button btnInscrever = convertView.findViewById(R.id.btnInscrever);

            tvNomeEvento.setText(evento.getNome());
            tvDataEvento.setText("Data: " + evento.getData());
            tvLocalEvento.setText("Local: " + evento.getLocal());

            View mainContent = convertView.findViewById(R.id.main_content);

            if (mListener != null) { // User View
                tvDescricao.setText(evento.getDescricao());
                tvHorarioInicio.setText("Início: " + evento.getHorario());
                tvHorarioTermino.setText("Término: " + evento.getHorarioTermino());
                
                mainContent.setOnClickListener(v -> {
                    boolean isVisible = hiddenPart.getVisibility() == View.VISIBLE;
                    hiddenPart.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                    animateExpandIcon(expandIcon, isVisible ? 180f : 0f, isVisible ? 0f : 180f);
                });

                boolean isExpanded = hiddenPart.getVisibility() == View.VISIBLE;
                expandIcon.setRotation(isExpanded ? 180f : 0f);
                expandIcon.setVisibility(View.VISIBLE);

                btnInscrever.setOnClickListener(v -> mListener.onInscreverClick(evento));
                btnInscrever.setVisibility(View.VISIBLE);

            } else { // Admin View
                hiddenPart.setVisibility(View.GONE);
                expandIcon.setVisibility(View.GONE);
                btnInscrever.setVisibility(View.GONE);
                mainContent.setOnClickListener(null);
            }
        }

        return convertView;
    }

    private void animateExpandIcon(ImageView icon, float from, float to) {
        RotateAnimation rotate = new RotateAnimation(from, to, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(300);
        rotate.setFillAfter(true);
        icon.startAnimation(rotate);
    }
}
