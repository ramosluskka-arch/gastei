package com.gastei.app;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
  @Override
  public void onCreate(Bundle savedInstanceState) {
    registerPlugin(GasteiPrintPlugin.class);
    registerPlugin(GasteiBillingPlugin.class);
    super.onCreate(savedInstanceState);
  }
}
