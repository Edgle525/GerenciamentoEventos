package br.edu.fatecgru.Eventos.ui;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * Esta classe existe apenas para forçar a câmera do scanner de QR Code
 * a abrir no modo retrato (vertical), travando a orientação.
 */
public class CaptureActivityPortrait extends CaptureActivity {
    // A classe fica vazia, pois ela só precisa existir para ser referenciada
    // nas opções do scanner.
}
