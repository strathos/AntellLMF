package com.honkasalo.antelllmf;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			StringBuilder sb2 = new StringBuilder();
			StringBuilder sb3 = new StringBuilder();
			
			// RegExp pattern to match food properties
			Pattern p = Pattern.compile("(?<!^|/|/ )(M|VL|L|G)(?=,|\\)| )");
			Matcher m = null;
			
			// Inform user about reloading
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			v = new RemoteViews(context.getPackageName(), R.layout.widget_antell_lmf);
			v.setTextViewText(R.id.widgetText, "Reloading data...");
			appWidgetManager.updateAppWidget(new ComponentName(context, DesktopWidget.class), v);
			
			// Localize DAY_OF_WEEK
			cal.setTime(new Date());
			int day = cal.get(Calendar.DAY_OF_WEEK) - 1;
			if (day == 0) {
				day = day + 6;
			}
			
			// Override integer day for debugging
			//day = 2;
			//Log.d("WIDGET", "Day of Week: "+Integer.toString(day));
			
			// Set the url from where to get the actual url end for the html url (iframe)
           	String url = AntellLMF.getFrameUrl("http://www.antell.fi/docs/lunch.php?LM%20Ericsson,%20Helsinki");
			
           	// Use Jsoup to connect to the fixed url and make a Jsoup document from the html page
			try {
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
				String language = settings.getString("foodMenuLanguage","");
				
				boolean connection_established=false;
				for (int tries = 1; tries<5 && !connection_established; tries++) {
					try {
						doc = Jsoup.connect("http://www.antell.fi"+url).timeout(100*1000).get();
						Log.d("CONNECTION", "Connected and fetched page.");
						connection_established=true;
					} catch (Exception ex) {
						ex.printStackTrace();
					} 
        	    }
				
				// Title
				table = doc.select("div:containsOwn(Lounaslista)").first();
				sb.append(table.text().split("Lounaslista ")[1]);
				
				if (day < 6) {
					// Daily menu
					table = doc.select("table.lunchTable").get(day);

					for (Element row : table.select("tr")) {
						Element tds = row.select("td").first();
						
						// Use the RegExp pattern to find properties
						m = p.matcher(tds.text());
						sb2 = new StringBuilder();
						while (m.find()) {
							sb2.append(m.group());
						}
						
						// Print correct language, or the first one if no slash is found
						if (language.equals("Finnish") || !(tds.text().matches("(.*)(/|(,\\s?[A-Z]))(.*)"))) {
							sb.append(tds.text().split("/|,\\s?(?=[A-Z])| \\(")[0].replaceAll("MAANANTAI|TIISTAI|KESKIVIIKKO|TORSTAI|PERJANTAI","") + " " + sb2.toString() + "\n");
						} else {
							sb.append(tds.text().split("/|,\\s?(?=[A-Z])| \\(")[1].replaceAll("^\\s","").replaceAll("MAANANTAI|TIISTAI|KESKIVIIKKO|TORSTAI|PERJANTAI","") + " " + sb2.toString() + "\n");
						};
					}

					// Weekly specials
					table = doc.select("div:containsOwn(Week)").first();
					sb2 = new StringBuilder();
					
					// Split from "Week Wok" to a la carte and wok to get properties for specials, but only if they exist
					if (table.text().matches("M|VL|L|G")) {
						m = p.matcher(table.text().split("Week.+Wok")[0]);
						while (m.find()) {
							sb2.append(m.group());
						}
						m = p.matcher(table.text().split("Week.+Wok")[1]);
						while (m.find()) {
							sb3.append(m.group());
						}
					}
					
					// Check if daily menu was loaded (for example days when restaurant is closed)
					if (sb.toString().length() > 50) {
						if ( language.equals("Finnish")) {
							sb.append(table.text().replaceAll("(?<!^|/|/ )(M|VL|L|G).+(M|VL|L|G)", "").split("Week|/|: ")[2] + sb2.toString() + "\n");
							sb.append(table.text().replaceAll("(?<!^|/|/ )(M|VL|L|G).+(M|VL|L|G)", "").split("Week|/|: ")[5] + sb3.toString());
						} else {
							sb.append(table.text().split("Week|/|: ")[3].replaceAll("^\\s","") + sb2.toString() + "\n");
							sb.append(table.text().split("Week|/|: ")[6].replaceAll("^\\s","") + " " + sb3.toString());
						};
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
