package com.gastei.app;

import android.content.Context;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.WebView;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/* Abre a tela de impressão do Android com a página atual (o relatório), onde dá para "Salvar como PDF". */
@CapacitorPlugin(name = "GasteiPrint")
public class GasteiPrintPlugin extends Plugin {
  @PluginMethod
  public void print(final PluginCall call) {
    final String name = call.getString("name", "Gastei");
    getActivity().runOnUiThread(new Runnable() {
      public void run() {
        WebView wv = getBridge().getWebView();
        PrintManager pm = (PrintManager) getActivity().getSystemService(Context.PRINT_SERVICE);
        PrintAttributes attrs = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build();
        pm.print(name, wv.createPrintDocumentAdapter(name), attrs);
        call.resolve();
      }
    });
  }
}
