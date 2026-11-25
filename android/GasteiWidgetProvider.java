package com.gastei.app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/* Widget da tela inicial: disponível, renda e gastos do mês, botões de gasto e receita. Os valores são enviados pelo app a cada mudança. */
public class GasteiWidgetProvider extends AppWidgetProvider {
  @Override
  public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    for (int id : ids) m.updateAppWidget(id, views(c));
  }

  static RemoteViews views(Context c) {
    SharedPreferences p = c.getSharedPreferences("gastei_widget", Context.MODE_PRIVATE);
    RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.gastei_widget);
    v.setTextViewText(R.id.w_label, p.getString("label", "Disponível no mês"));
    v.setTextViewText(R.id.w_value, p.getString("value", "Abra o Gastify"));
    v.setTextViewText(R.id.w_pct, p.getString("sub", ""));
    v.setTextViewText(R.id.w_in, p.getString("inc", "R$ 0,00"));
    v.setTextViewText(R.id.w_out, p.getString("out", "R$ 0,00"));
    v.setProgressBar(R.id.w_bar, 100, p.getInt("pct", 0), false);
    int fl = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    Intent open = new Intent(c, MainActivity.class);
    open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    v.setOnClickPendingIntent(R.id.w_root, PendingIntent.getActivity(c, 1, open, fl));
    Intent add = new Intent(c, MainActivity.class);
    add.setAction("com.gastei.app.ADD");
    add.putExtra("gastei_action", "add");
    add.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    v.setOnClickPendingIntent(R.id.w_add, PendingIntent.getActivity(c, 2, add, fl));
    Intent addIn = new Intent(c, MainActivity.class);
    addIn.setAction("com.gastei.app.ADD_IN");
    addIn.putExtra("gastei_action", "add_in");
    addIn.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    v.setOnClickPendingIntent(R.id.w_addin, PendingIntent.getActivity(c, 3, addIn, fl));
    return v;
  }

  static void updateAll(Context c) {
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    int[] ids = m.getAppWidgetIds(new ComponentName(c, GasteiWidgetProvider.class));
    for (int id : ids) m.updateAppWidget(id, views(c));
  }
}
