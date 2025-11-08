package br.edu.fatecgru.Eventos.model;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import br.edu.fatecgru.Eventos.R;

public class HistoricoAdapter extends ArrayAdapter<Inscricao> {

    public HistoricoAdapter(Context context, List<Inscricao> inscricoes) {
        super(context, 0, inscricoes);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_historico, parent, false);
        }

        Inscricao inscricao = getItem(position);

        TextView tvNomeEvento = convertView.findViewById(R.id.tvNomeEventoHistorico);
        TextView tvDataEvento = convertView.findViewById(R.id.tvDataEventoHistorico);
        TextView tvStatus = convertView.findViewById(R.id.tvStatusHistorico);
        LinearLayout layout = convertView.findViewById(R.id.item_historico_layout);

        if (inscricao != null) {
            tvNomeEvento.setText(inscricao.getNomeEvento());
            tvDataEvento.setText(inscricao.getDataEvento());

            String status;
            if (inscricao.getHoraEntrada() != null && inscricao.getHoraSaida() != null) {
                status = "Completo";
                layout.setBackgroundColor(Color.parseColor("#C8E6C9")); // Verde claro
            } else if (inscricao.getHoraEntrada() != null) {
                status = "Incompleto (Saída pendente)";
                layout.setBackgroundColor(Color.TRANSPARENT); // CORREÇÃO: Usar cor transparente
            } else {
                status = "Cadastrado";
                layout.setBackgroundColor(Color.TRANSPARENT); // CORREÇÃO: Usar cor transparente
            }
            tvStatus.setText(status);
        }

        return convertView;
    }
}
