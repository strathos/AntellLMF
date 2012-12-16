package com.honkasalo.antelllmf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class AntellLMF extends Activity {
	public static Context context;
	private TextView text;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make a variable for context for easier reuse
        context = this;
        
        // Set main activity layout
        setContentView(R.layout.activity_antell_lmf);
        setTitle("AntellLMF Weekly Menu");
        
        // Read language setting from preferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d("PREF:LANG",settings.getString("foodMenuLanguage","No Language set"));
        
        // Define TextView for food menu output
        text = (TextView) findViewById(R.id.foodMenu);

        // Fetch menu on background
        DownloadMenuTask task = new DownloadMenuTask();
        task.execute();
    };
    
    
    /* Reloads menu when navigated back to */
    @Override
	protected void onRestart() {
		super.onRestart();
		DownloadMenuTask task = new DownloadMenuTask();
        task.execute();
    }


    /* Defines and opens menu */
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_antell_lmf, menu);
        return true;
    }
    
	
	/* Listener for menu item clicks */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    		case R.id.menu_reload:
    			DownloadMenuTask task = new DownloadMenuTask();
    	        task.execute();
    	        return true;
    		case R.id.menu_settings:
    			Intent settings = new Intent(AntellLMF.this, MainPreferenceActivity.class);
    			startActivity(settings);
    			return true;
    		case R.id.menu_about:
    			Intent about = new Intent(AntellLMF.this, MainAboutActivity.class);
    			startActivity(about);
    			return true;
    		default:
    	}
    	return super.onOptionsItemSelected(item);
    }
    
    /* Extract iFrame url from main web page */
    public static String getFrameUrl(String url) {

        String frameUrl = "";
        try {
            Document doc = Jsoup.connect(url).timeout(10*1000).get();

            Element frame = doc.select("frame").first();
            String framesrc = frame.attr("src");

            // Remove possible leading dots from extracted url
            frameUrl = framesrc.replaceAll("\\.{2}", "");
            
        } catch (Exception error) {
            error.printStackTrace();
        }
        return frameUrl;
    }
    
    /* Get days menu and return as a string*/
    public FoodMenu getDaysMenu(String url) {
        FoodMenu menu = new FoodMenu();
    	Document doc=null;
        try {
        	
        	//Log.d("URL",url);

        	boolean connection_established=false;
        	for (int tries = 1; tries<5 && !connection_established; tries++) {
        	    try {
        	    	doc = Jsoup.connect(url).timeout(100*1000).get();
                    //Log.d("CONNECTION", "Connected and fetched page.");
                    connection_established=true;
        	    } catch (Exception ex) {
        	    	ex.printStackTrace();
        	    } 
        	        	    
        	}
        	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AntellLMF.context);
    		String language = settings.getString("foodMenuLanguage","");

            // Select a table according to week day number
    		menu.setWeekTitle(getMenuTitle(doc.select("div:containsOwn(Lounaslista)").first()));
    		if (language.equals("Finnish")) {
    			menu.setMonday("Maanantai\n"+getDailyMenu(doc.select("table.lunchTable").get(1)).replaceAll("MAANANTAI",""));
    			menu.setTuesday("Tiistai\n"+getDailyMenu(doc.select("table.lunchTable").get(2)).replaceAll("TIISTAI",""));
    			menu.setWednesday("Keskiviikko\n"+getDailyMenu(doc.select("table.lunchTable").get(3)).replaceAll("KESKIVIIKKO",""));
    			menu.setThursday("Torstai\n"+getDailyMenu(doc.select("table.lunchTable").get(4)).replaceAll("TORSTAI",""));
    			menu.setFriday("Perjantai\n"+getDailyMenu(doc.select("table.lunchTable").get(5)).replaceAll("PERJANTAI",""));
    		} else {
    			menu.setMonday("Monday\n"+getDailyMenu(doc.select("table.lunchTable").get(1)).replaceAll("MAANANTAI",""));
    			menu.setTuesday("Tuesday\n"+getDailyMenu(doc.select("table.lunchTable").get(2)).replaceAll("TIISTAI",""));
    			menu.setWednesday("Wednesday\n"+getDailyMenu(doc.select("table.lunchTable").get(3)).replaceAll("KESKIVIIKKO",""));
    			menu.setThursday("Thursday\n"+getDailyMenu(doc.select("table.lunchTable").get(4)).replaceAll("TORSTAI",""));
    			menu.setFriday("Friday\n"+getDailyMenu(doc.select("table.lunchTable").get(5)).replaceAll("PERJANTAI",""));
    		};
            menu.setWeeksSpecials(getWeeklySpecial(doc.select("div:containsOwn(Week)").first()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return menu;
    }
    
    public static String getMenuTitle(Element table) {
    	StringBuilder sb = new StringBuilder();
    	sb.append(table.text().split("Lounaslista ")[1]+"\n");
    	//Log.d("DATA",""+table.text());
    	return sb.toString();
    }

	public static String getDailyMenu(Element table) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AntellLMF.context);
		String language = settings.getString("foodMenuLanguage","");
		StringBuilder sb = new StringBuilder();
		Pattern p = Pattern.compile("(?<!^|/|/ )(M|VL|L|G)(?=,|\\)| )");
		for (Element row : table.select("tr")) {
	        Element tds = row.select("td").first();
	        Matcher m = p.matcher(tds.text());
	        StringBuilder sb2 = new StringBuilder();
			while (m.find()) {
				sb2.append(m.group());
			}
			if (language.equals("Finnish") || !(tds.text().matches("(.*)(/|(,\\s?[A-Z]))(.*)"))) {
	        	sb.append(tds.text().split("/|,\\s?(?=[A-Z])| \\(")[0] + " " + sb2.toString() + "\n");
	        } else {
	        	sb.append(tds.text().split("/|,\\s?(?=[A-Z])| \\(")[1].replaceAll("^\\s","") + " " + sb2.toString() + "\n");
	        };
	        //Log.d("DATA","Menu: "+tds.text());
	    }
	    return sb.toString();
	}
	
	public static String getWeeklySpecial(Element table) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AntellLMF.context);
		String language = settings.getString("foodMenuLanguage","");
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		StringBuilder sb3 = new StringBuilder();
		if (table.text().matches("M|VL|L|G")) {
			Pattern p = Pattern.compile("(?<!^|/)(M|VL|L|G)");
			Matcher m = p.matcher(table.text().split("Week.+Wok")[0]);
			while (m.find()) {
				sb2.append(m.group());
			}
			m = p.matcher(table.text().split("Week.+Wok")[1]);
			while (m.find()) {
				sb3.append(m.group());
			}
		}
		if ( language.equals("Finnish")) {
			sb.append("Viikon erikoiset\n\n");
			sb.append(table.text().replaceAll("(?<!^|/|/ )(M|VL|L|G).+(M|VL|L|G)", "").split("Week|/|: ")[2] + sb2.toString() + "\n");
			sb.append(table.text().replaceAll("(?<!^|/|/ )(M|VL|L|G).+(M|VL|L|G)", "").split("Week|/|: ")[5] + sb3.toString());
		} else {
			sb.append("Specials of the week\n\n");
			sb.append(table.text().split("Week|/|: ")[3].replaceAll("^\\s","") + sb2.toString() + "\n");
			sb.append(table.text().split("Week|/|: ")[6].replaceAll("^\\s","") + " " + sb3.toString());
		};
		//Log.d("DATA","Special: "+table.text());
	    return sb.toString();
	}
	
	private class DownloadMenuTask extends AsyncTask<Void, Void,String> {
		ProgressDialog dialog;
		
		protected void onPreExecute() {
			dialog = new ProgressDialog(AntellLMF.context);
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			dialog.setMessage("Loading data from Antell.fi");
			dialog.show();
		}
		
	    @Override
	    protected String doInBackground(Void... params) {
           	FoodMenu menu = new FoodMenu(); 
           	String url = getFrameUrl("http://www.antell.fi/docs/lunch.php?LM%20Ericsson,%20Helsinki");
           	//Log.d("URL", url);
           	menu = getDaysMenu("http://www.antell.fi"+url);
           	return menu.toString();
	    }

		@Override
		protected void onPostExecute(String result) {
			text.setText(result);
			dialog.dismiss();
			Toast toast = Toast.makeText(context, "Data reloaded", Toast.LENGTH_SHORT);
			toast.show();
		}
	}

}
