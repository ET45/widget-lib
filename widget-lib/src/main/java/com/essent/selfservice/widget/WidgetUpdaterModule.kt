package nl.essent.selfservice.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

class WidgetUpdaterModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "WidgetUpdater"
    }

    @ReactMethod
    fun updateWidget(prices: String) {
        val context = reactApplicationContext
        // Save to SharedPreferences
        val prefs: SharedPreferences =
            context.getSharedPreferences("MyWidgetPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("expectedSupplyAddressPath", prices).apply()

        // Notify the widget to update
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
        context.sendBroadcast(intent)

        Log.d("WidgetUpdater", "Widget updated with new prices: $prices")
    }
}
