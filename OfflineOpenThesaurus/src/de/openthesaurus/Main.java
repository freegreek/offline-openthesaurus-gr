package de.openthesaurus;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteCursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TwoLineListItem;

public class Main extends Activity {
	
	private DataBaseHelper dataBaseHelper;
	private Cursor cursor;
	private AutoCompleteCursor autoCompleteCursor;
	
	
	private AutoCompleteTextView autoCompleteTextView;
	private ProgressDialog progressDialog;
	private ListView listView;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        dataBaseHelper = new DataBaseHelper(this);
        autoCompleteCursor = new AutoCompleteCursor(this, dataBaseHelper.getTermCursor(), 0,dataBaseHelper);
        autoCompleteCursor.setSearchOn(true);
  
        progressDialog = new ProgressDialog(this);        
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);        
        progressDialog.setMessage("Installiere Datenbank...");
		progressDialog.setCancelable(false);
		
		
		progressDialog.show();

		
		
		//start database operations in a background thread
        new Thread(new Runnable() {
            public void run() {
            	createDatabase(dataBaseHelper);
            	openDatabase(dataBaseHelper);
            	progressDialog.dismiss();
            }
        }).start();		

        initUIElements();
        
    }
    
    
    private void initUIElements(){
    	
		autoCompleteTextView = (AutoCompleteTextView)findViewById(R.id.AutoCompleteTextView01);
        autoCompleteTextView.setThreshold(2);
        autoCompleteTextView.setAdapter(autoCompleteCursor);
        autoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				String searchItem = ((SQLiteCursor)arg0.getItemAtPosition(arg2)).getString(0);
				
				querySynonym(searchItem,dataBaseHelper);
			}
        	
		});
        
        listView = getListView();
        listView.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				
				String item = ((TwoLineListItem)arg1).getText1().getText().toString();
	
				//set the selected item
				((AutoCompleteCursor)autoCompleteTextView.getAdapter()).setSearchOn(false);
				autoCompleteTextView.setText(item);
				
				querySynonym(item,dataBaseHelper);
	
				((AutoCompleteCursor)autoCompleteTextView.getAdapter()).setSearchOn(true);
			}
        	
		});

    }
    

	private void openDatabase(DataBaseHelper myDbHelper) {
		try {
			myDbHelper.openDataBase();
		} catch (SQLException sqle) {

			throw sqle;
		}
	}

	private void createDatabase(DataBaseHelper myDbHelper) throws Error {
		try {
			myDbHelper.createDataBase();
		} catch (IOException ioe) {

			// create an alert dialog with the input text
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Exception was thrown! ");
			builder.setCancelable(false);
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert = builder.create();

			alert.show();

			throw new Error("Unable to create database");
		}
	}
    
    /**
     * This method sends a query to the database.
     * 
     * @param item
     */
    public void querySynonym(String item,DataBaseHelper myDbHelper){
    	
    	if(item.length()<4) return;
    	
    	try {
    		
			cursor = myDbHelper.getSynonymCursor(item);
			startManagingCursor(cursor);

			// set the db content to the list view
			String[] from = new String[] { "word","category_name" };

			int[] to = new int[] { android.R.id.text1,android.R.id.text2};

			SimpleCursorAdapter listAdapter = new SimpleCursorAdapter(this,
					android.R.layout.simple_list_item_2, cursor, from, to);

			 ListView viewList = getListView();
			 viewList.setAdapter(listAdapter);
			 

		} catch (SQLException sqle) {

			throw sqle;

		}
    }
    
    private ListView getListView(){
    	
    	return (ListView)findViewById(R.id.ListView01);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
        case R.id.item01:

        	// create an alert dialog with the input text
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Jeder kann bei OpenThesaurus mitmachen und Fehler " +
					"korrigieren oder neue Synonyme einfügen. Die Suchfunktion zeigt alle " +
					"Bedeutungen, in denen ein Wort vorkommt (z.B. roh -> roh, ungekocht " +
					"und einen anderen Eintrag für roh, rau, grob, unsanft). \n\n" +
					"http://code.google.com/p/offline-openthesaurus-de/");
			builder.setCancelable(false);
			builder.setPositiveButton("OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alert = builder.create();

			alert.show();
			return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    
    
    private class AutoCompleteCursor extends CursorAdapter {
    	private int columnIndex;
    	private DataBaseHelper myDbHelper;
    	private Boolean isSearchOn;
    	

		public AutoCompleteCursor(Context context, Cursor c, int col,DataBaseHelper myDbHelper) {
			super(context, c);
			this.columnIndex = col;
			this.myDbHelper=myDbHelper;
		}

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            
            final TextView view = (TextView) inflater.inflate(
                    R.layout.suggest_item, parent, false);
            
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {        	
            ((TextView) view).setText(cursor.getString(columnIndex));
        }
        
        @Override
        public String convertToString(Cursor cursor) {
        	String clickedItem = cursor.getString(columnIndex);
        	
            return clickedItem;
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            if (constraint != null && isSearchOn) {
            	return myDbHelper.getAutocompleteCursor(constraint.toString());
            }
            else {
            	return null;
            }
        }
        
		public Boolean isSearchOn() {
			return isSearchOn;
		}

		public void setSearchOn(Boolean isSearchOn) {
			this.isSearchOn = isSearchOn;
		}
        
    }
    
}