package apt.tutorial;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LunchList extends Activity {
	public final static String ID_EXTRA="apt.tutorial._ID";
	Cursor model=null;
	RestaurantAdapter adapter=null;
	EditText name=null;
	EditText address=null;
	EditText notes=null;
	RadioGroup types=null;
	Restaurant current=null;
	AtomicBoolean isActive=new AtomicBoolean(true);
	int progress=0;
	SQLiteDatabase db=null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.main);
		
		db=(new RestaurantSQLiteHelper(this))
															 .getWritableDatabase();
		
		name=(EditText)findViewById(R.id.name);
		address=(EditText)findViewById(R.id.addr);
		notes=(EditText)findViewById(R.id.notes);
		types=(RadioGroup)findViewById(R.id.types);
		
		ListView list=(ListView)findViewById(R.id.restaurants);
		
		model=Restaurant.getAll(db);
		startManagingCursor(model);
		adapter=new RestaurantAdapter(model);
		list.setAdapter(adapter);
		list.setOnItemClickListener(onListClick);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		isActive.set(false);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		isActive.set(true);
		
		if (progress>0) {
			startWork();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		db.close();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication())
																	.inflate(R.menu.option, menu);

		return(super.onCreateOptionsMenu(menu));
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId()==R.id.add) {
			startActivity(new Intent(this, DetailForm.class));
			
			return(true);
		}
		else if (item.getItemId()==R.id.run) {
			startWork();
			
			return(true);
		}
		
		return(super.onOptionsItemSelected(item));
	}
	
	private void startWork() {
		setProgressBarVisibility(true);
		new Thread(longTask).start();			
	}
	
	private void doSomeLongWork(final int incr) {
		runOnUiThread(new Runnable() {
			public void run() {
				progress+=incr;
				setProgress(progress);
			}
		});
		
		SystemClock.sleep(250);	// should be something more useful!
	}
	
	private View.OnClickListener onSave=new View.OnClickListener() {
		public void onClick(View v) {
			current=new Restaurant();
			current.setName(name.getText().toString());
			current.setAddress(address.getText().toString());
			current.setNotes(notes.getText().toString());
			
			switch (types.getCheckedRadioButtonId()) {
				case R.id.sit_down:
					current.setType("sit_down");
					break;
				case R.id.take_out:
					current.setType("take_out");
					break;
				case R.id.delivery:
					current.setType("delivery");
					break;
			}
			
			current.save(db);
			model.requery();
		}
	};
	
	private AdapterView.OnItemClickListener onListClick=new AdapterView.OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent,
														View view, int position,
														long id) {
			Intent i=new Intent(LunchList.this, DetailForm.class);
			
			i.putExtra(ID_EXTRA, String.valueOf(id));
			startActivity(i);
		}
	};
		
	private Runnable longTask=new Runnable() {
		public void run() {
			for (int i=progress;
					 i<10000 && isActive.get();
					 i+=200) {
				doSomeLongWork(200);
			}
			
			if (isActive.get()) {
				runOnUiThread(new Runnable() {
					public void run() {
						setProgressBarVisibility(false);
						progress=0;
					}
				});
			}
		}
	};
	
	class RestaurantAdapter extends CursorAdapter {
		RestaurantAdapter(Cursor c) {
			super(LunchList.this, c);
		}
		
		@Override
		public void bindView(View row, Context ctxt,
												 Cursor c) {
			RestaurantWrapper wrapper=(RestaurantWrapper)row.getTag();
			
			wrapper.populateFrom(c);
		}
		
		@Override
		public View newView(Context ctxt, Cursor c,
												 ViewGroup parent) {
			LayoutInflater inflater=getLayoutInflater();
			View row=inflater.inflate(R.layout.row, parent, false);
			RestaurantWrapper wrapper=new RestaurantWrapper(row);
			
			row.setTag(wrapper);
			wrapper.populateFrom(c);
			
			return(row);
		}
	}
	
	class RestaurantWrapper {
		private TextView name=null;
		private TextView address=null;
		private ImageView icon=null;
		private View row=null;
		
		RestaurantWrapper(View row) {
			this.row=row;
		}
		
		void populateFrom(Cursor c) {
			getName().setText(c.getString(c.getColumnIndex("name")));
			getAddress().setText(c.getString(c.getColumnIndex("address")));
			String type=c.getString(c.getColumnIndex("type"));
			
			if (type.equals("sit_down")) {
				getIcon().setImageResource(R.drawable.ball_red);
			}
			else if (type.equals("take_out")) {
				getIcon().setImageResource(R.drawable.ball_yellow);
			}
			else {
				getIcon().setImageResource(R.drawable.ball_green);
			}
		}
		
		TextView getName() {
			if (name==null) {
				name=(TextView)row.findViewById(R.id.title);
			}
			
			return(name);
		}
		
		TextView getAddress() {
			if (address==null) {
				address=(TextView)row.findViewById(R.id.address);
			}
			
			return(address);
		}
		
		ImageView getIcon() {
			if (icon==null) {
				icon=(ImageView)row.findViewById(R.id.icon);
			}
			
			return(icon);
		}
	}
}