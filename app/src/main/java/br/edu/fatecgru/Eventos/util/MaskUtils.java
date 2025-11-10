package br.edu.fatecgru.Eventos.util;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MaskUtils {

    public static TextWatcher insert(final String mask, final EditText ediTxt) {
        return new TextWatcher() {
            boolean isUpdating;
            String old = "";

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String str = MaskUtils.unmask(s.toString());
                String mascara = "";
                if (isUpdating) {
                    old = str;
                    isUpdating = false;
                    return;
                }
                int i = 0;
                for (char m : mask.toCharArray()) {
                    if (m != '#' && (str.length() > old.length())) {
                        mascara += m;
                        continue;
                    } else if (m != '#' && str.length() < old.length() && str.length() != i) {
                        mascara += m;
                    } else {
                        try {
                            mascara += str.charAt(i);
                        } catch (Exception e) {
                            break;
                        }
                        i++;
                    }
                }
                isUpdating = true;
                ediTxt.setText(mascara);
                ediTxt.setSelection(mascara.length());
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void afterTextChanged(Editable s) {}
        };
    }

    public static TextWatcher timeMask(final EditText ediTxt) {
        return new TextWatcher() {
            private String current = "";

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    String clean = s.toString().replaceAll("[^\\d]", "");
                    String cleanC = current.replaceAll("[^\\d]", "");

                    int cl = clean.length();
                    int sel = cl;
                    for (int i = 2; i <= cl && i < 6; i += 2) {
                        sel++;
                    }
                    //Fix for pressing delete next to a forward slash
                    if (clean.equals(cleanC)) sel--;

                    if (clean.length() < 4) {
                        clean = clean + "0000".substring(clean.length());
                    }

                    if (clean.length() > 3) {
                        char h1 = clean.charAt(0);
                        char h2 = clean.charAt(1);
                        char m1 = clean.charAt(2);
                        char m2 = clean.charAt(3);

                        int h = Integer.parseInt("" + h1 + h2);
                        int m = Integer.parseInt("" + m1 + m2);

                        if (m > 59) {
                            m = 59;
                            clean = "" + h1 + h2 + m1 + '9';
                        }

                        current = String.format("%02d:%02d", h, m);

                    } else {
                        current = clean;
                    }

                    ediTxt.setText(current);
                    ediTxt.setSelection(Math.min(sel, ediTxt.length()));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {}
        };
    }

    public static String unmask(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    public static String formatCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
    }
}
