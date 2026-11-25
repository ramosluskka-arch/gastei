package com.gastei.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.speech.RecognizerIntent;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.ArrayList;

/* Widget da tela inicial, atalho "+ Gasto" do widget e reconhecimento de voz do Android. */
@CapacitorPlugin(name = "GasteiNative")
public class GasteiNativePlugin extends Plugin {

  @PluginMethod
  public void widget(PluginCall call) {
    SharedPreferences.Editor e = getContext().getSharedPreferences("gastei_widget", Context.MODE_PRIVATE).edit();
    e.putString("label", call.getString("label", "Disponível no mês"));
    e.putString("value", call.getString("value", ""));
    e.putString("sub", call.getString("sub", ""));
    e.putString("inc", call.getString("inc", "R$ 0,00"));
    e.putString("out", call.getString("out", "R$ 0,00"));
    e.putInt("pct", call.getInt("pct", 0));
    e.apply();
    GasteiWidgetProvider.updateAll(getContext());
    call.resolve();
  }

  @PluginMethod
  public void action(PluginCall call) {
    Intent it = getActivity().getIntent();
    String a = it != null ? it.getStringExtra("gastei_action") : null;
    if (it != null) it.removeExtra("gastei_action");
    JSObject o = new JSObject();
    o.put("action", a == null ? "" : a);
    call.resolve(o);
  }

  @PluginMethod
  public void listen(PluginCall call) {
    Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
    i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Fale o gasto. Ex.: mercado 45 reais no pix");
    try {
      startActivityForResult(call, i, "speechDone");
    } catch (Exception ex) {
      call.reject("indisponivel");
    }
  }

  @ActivityCallback
  private void speechDone(PluginCall call, ActivityResult result) {
    if (call == null) return;
    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
      call.reject("cancelado", "cancel");
      return;
    }
    ArrayList<String> r = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    JSObject o = new JSObject();
    o.put("text", r != null && !r.isEmpty() ? r.get(0) : "");
    call.resolve(o);
  }
}
