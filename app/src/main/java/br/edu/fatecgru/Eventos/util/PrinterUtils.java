package br.edu.fatecgru.Eventos.util;

import android.graphics.Bitmap;

public class PrinterUtils {

    /**
     * Converte um Bitmap em um array de bytes para impressão em impressoras térmicas
     * que usam o comando ESC/POS "GS v 0".
     * A imagem é processada em "dot-density" vertical, um padrão comum.
     */
    public static byte[] decodeBitmap(Bitmap bmp) {
        // A largura da imagem em bytes deve ser um múltiplo de 8
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        int bytesWidth = (bmpWidth + 7) / 8;

        // Array para armazenar os dados da imagem em formato de "slices" verticais
        byte[] imageBytes = new byte[bytesWidth * bmpHeight];
        int k = 0;

        for (int y = 0; y < bmpHeight; y++) {
            for (int x = 0; x < bytesWidth; x++) {
                for (int b = 0; b < 8; b++) {
                    int x_col = x * 8 + b;
                    if (x_col < bmpWidth) {
                        int pixel = bmp.getPixel(x_col, y);
                        // Converte o pixel para escala de cinza e verifica se é escuro
                        if ((pixel & 0x00FFFFFF) < 0x00808080) { // Limiar de 50% de cinza
                            imageBytes[k] |= (byte) (1 << (7 - b));
                        }
                    }
                }
                k++;
            }
        }

        // Monta o comando final para a impressora
        // Comando: GS v 0 m xL xH yL yH [d1...dk]
        byte[] command = new byte[8 + imageBytes.length];
        command[0] = 0x1D; // GS
        command[1] = 0x76; // v
        command[2] = 0x30; // 0 (modo)
        command[3] = 0x00; // m = 0
        command[4] = (byte) (bytesWidth % 256); // xL - Largura em bytes (low byte)
        command[5] = (byte) (bytesWidth / 256); // xH - Largura em bytes (high byte)
        command[6] = (byte) (bmpHeight % 256);  // yL - Altura em pixels (low byte)
        command[7] = (byte) (bmpHeight / 256);  // yH - Altura em pixels (high byte)

        System.arraycopy(imageBytes, 0, command, 8, imageBytes.length);

        return command;
    }
}
