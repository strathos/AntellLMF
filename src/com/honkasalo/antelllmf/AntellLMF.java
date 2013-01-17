package com.honkasalo.antelllmf;

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
    		menu.setWeekTitle(getMenuTitle(doc.select("table.title").first()));
    		if (language.equals("Finnish")) {
    			menu.setMonday("Maanantai\n"+getDailyMenu(doc.select("table.monFi").first()).replaceAll("Maanantai",""));
    			menu.setTuesday("Tiistai\n"+getDailyMenu(doc.select("table.tueFi").first()).replaceAll("Tiistai",""));
    			menu.setWednesday("Keskiviikko\n"+getDailyMenu(doc.select("table.wedFi").first()).replaceAll("Keskiviikko",""));
    			menu.setThursday("Torstai\n"+getDailyMenu(doc.select("table.thuFi").first()).replaceAll("Torstai",""));
    			menu.setFriday("Perjantai\n"+getDailyMenu(doc.select("table.friFi").first()).replaceAll("Perjantai",""));
    			menu.setWeeksSpecials("\nViikon erikoiset\n"+getWeeklySpecial(doc.select("table.speFi").first()).replaceAll("Viikon erikoiset",""));
    		} else {
    			menu.setMonday("Monday\n"+getDailyMenu(doc.select("table.monEn").first()).replaceAll("Monday",""));
    			menu.setTuesday("Tuesday\n"+getDailyMenu(doc.select("table.tueEn").first()).replaceAll("Tuesday",""));
    			menu.setWednesday("Wednesday\n"+getDailyMenu(doc.select("table.wedEn").first()).replaceAll("Wednesday",""));
    			menu.setThursday("Thursday\n"+getDailyMenu(doc.select("table.thuEn").first()).replaceAll("Thursday",""));
    			menu.setFriday("Friday\n"+getDailyMenu(doc.select("table.friEn").first()).replaceAll("Friday",""));
    			menu.setWeeksSpecials("\nWeekly Specials\n"+getWeeklySpecial(doc.select("table.speEn").first()).replaceAll("Weekly Specials",""));
    		};
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
		StringBuilder sb = new StringBuilder();
		for (Element row : table.select("tr")) {
	        Element tds = row.select("td").first();
	        sb.append(tds.text() + "\n");
	        //Log.d("DATA","Menu: "+tds.text());
	    }
	    return sb.toString();
	}
	
	public static String getWeeklySpecial(Element table) {
		StringBuilder sb = new StringBuilder();
		for (Element row : table.select("tr")) {
	        Element tds = row.select("td").first();
	        sb.append(tds.text() + "\n");
	    }
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
           	menu = getDaysMenu("http://unska.com/kari/antell_lmf_menu.php");
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
