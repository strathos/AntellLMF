package com.honkasalo.antelllmf;

import java.util.Calendar;
import java.util.Date;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class DesktopWidget extends AppWidgetProvider {
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		// Set RemoteView
		RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_antell_lmf);
		
		// Create new intent
		Intent intent = new Intent(context, DesktopWidget.class);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[0]);
		intent.setAction("update");
		
		// Create new PendingIntent for reload button
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		v.setOnClickPendingIntent(R.id.refreshWidget, pi);
		
		// Create new PendingIntent to open main activity by clicking the widget
		Intent mainAppIntent = new Intent(context, AntellLMF.class);
		PendingIntent mainApp = PendingIntent.getActivity(context, 0, mainAppIntent, 0);
		v.setOnClickPendingIntent(R.id.widgetText, mainApp);
		
		// Manually run the asynchTask
		new DownloadMenuTask().execute(context);
		
		// Update the widget
		appWidgetManager.updateAppWidget(appWidgetIds, v);
		
	}
	
	// Functionality for reload button
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		
		if (intent.getAction().equals("update")) {
			new DownloadMenuTask().execute(context);
		}
	}
	
	// Background task for updating the widget
	private class DownloadMenuTask extends AsyncTask<Context, Void,String> {
		private Context context;
		private RemoteViews v;
				
		@Override
	    protected String doInBackground(Context... params) {
	    	context = params[0];
	    	Calendar cal = Calendar.getInstance();
			Document doc = null;
			Element table = null;
			StringBuilder sb = new StringBuilder();
			
			// Inform user about reloading
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			v = new RemoteViews(context.getPackageName(), R.layout.widget_antell_lmf);
			v.setTextViewText(R.id.widgetText, "Reloading data...");
			appWidgetManager.updateAppWidget(new ComponentName(context, DesktopWidget.class), v);
			
			// Language setting
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
			String language = settings.getString("foodMenuLanguage","");
			
			// Localize DAY_OF_WEEK
			cal.setTime(new Date());
			int day = cal.get(Calendar.DAY_OF_WEEK) - 1;
			if (day == 0) {
				day = day + 6;
			}
			
			// Override integer day for debugging
			//day = 6;
			//Log.d("WIDGET", "Day of Week: "+Integer.toString(day));
			
			// Select tables based on language set
			String dayTable = "";
			String speTable = "";
			if (language.equals("Finnish")) {
				switch(day) {
				case 1:
					dayTable = "monFi";
					break;
				case 2:
					dayTable = "tueFi";
					break;
				case 3:
					dayTable = "wedFi";
					break;
				case 4:
					dayTable = "thuFi";
					break;
				case 5:
					dayTable = "friFi";
					break;
				}
				speTable = "speFi";
			} else {
				switch(day) {
				case 1:
					dayTable = "monEn";
					break;
				case 2:
					dayTable = "tueEn";
					break;
				case 3:
					dayTable = "wedEn";
					break;
				case 4:
					dayTable = "thuEn";
					break;
				case 5:
					dayTable = "friEn";
					break;
				}
				speTable = "speEn";
			}
						
           	// Use Jsoup to connect to the url and make a Jsoup document from the html page
			try {
				boolean connection_established=false;
				for (int tries = 1; tries<5 && !connection_established; tries++) {
					try {
						doc = Jsoup.connect("http://unska.com/kari/antell_lmf_menu.php").timeout(100*1000).get();
						Log.d("CONNECTION", "Connected and fetched page.");
						connection_established=true;
					} catch (Exception ex) {
						ex.printStackTrace();
					} 
        	    }
				
				// Title
				table = doc.select("table.title").first();
				sb.append(table.text().split("Lounaslista ")[1]);
				
				if (day < 6) {
					// Daily menu
					table = doc.select("table."+dayTable).first();

					for (Element row : table.select("tr")) {
						Element tds = row.select("td").first();
						sb.append(tds.text().replaceAll("Maanantai|Tiistai|Keskiviikko|Torstai|Perjantai|Monday|Tuesday|Wednesday|Thursday|Friday","") + "\n");
					}

					// Weekly specials
					table = doc.select("table."+speTable).first();
					if (sb.toString().length() > 50) {
						for (Element row : table.select("tr")) {
							Element tds = row.select("td").first();
							if ((tds.text().contains("Viikon erikoiset")) || (tds.text().contains("Weekly Specials"))) {
								continue;
							}
							sb.append(tds.text() + "\n");
						}
					} else {
						sb.append("No menu available for today.");
					}
				} else {
					sb.append("\n\nWeekend, no menu available.");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
           	return sb.toString();
	    }
		
		@Override
		protected void onPostExecute(String result) {
			// Print built food menu to widget
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			v = new RemoteViews(context.getPackageName(), R.layout.widget_antell_lmf);
			v.setTextViewText(R.id.widgetText, result);
			appWidgetManager.updateAppWidget(new ComponentName(context, DesktopWidget.class), v);
		}
	}

}
