package tk.jcchen.servicekiller.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
//import android.support.v4.app.NavUtils;
import android.text.TextUtils;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tk.jcchen.servicekiller.R;

public class AppGridActivity extends Activity implements RetainedFragment.RetainedCallbacks {
	
	private final static String TAG = "ServiceKiller";
	private GridView appGrid;
	private final int IMG_WIDTH = 80;
	private RetainedFragment mRetainedFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_app_grid);
		// Show the Up button in the action bar.
		setupActionBar();
		
		appGrid = (GridView) findViewById(R.id.grid_apps);
		appGrid.setChoiceMode(GridView.CHOICE_MODE_MULTIPLE_MODAL);
		appGrid.setMultiChoiceModeListener(new MultiChoiceModeListener());
		
		FragmentManager fm = getFragmentManager();
		mRetainedFragment = (RetainedFragment) fm.findFragmentByTag("work");
		if(mRetainedFragment == null) {
			mRetainedFragment = new RetainedFragment();
			fm.beginTransaction().add(mRetainedFragment, "work").commit();
		} else if(mRetainedFragment.mApps != null) {
			showGrid();
		}
	}


	
	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.app_grid, menu);
		MenuItem queryItem = menu.findItem(R.id.action_search);
		queryItem.setActionView(mRetainedFragment.mSearchView);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
//		switch (item.getItemId()) {
//		case android.R.id.home:
//			NavUtils.navigateUpFromSameTask(this);
//			return true;
//		}
		return super.onOptionsItemSelected(item);
	}

	
	@Override
	public void onPostExecute() {
		showGrid();
	}

	private void showGrid() {
		appGrid.setAdapter(mRetainedFragment.mAdapter);
		appGrid.setVisibility(View.VISIBLE);
	}
	

	public class MultiChoiceModeListener implements GridView.MultiChoiceModeListener {
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			
			ArrayList<String> checkedApps = new ArrayList<String>();
			int len = appGrid.getCount();
			SparseBooleanArray checked = appGrid.getCheckedItemPositions();
			for(int i = 0; i < len; i++) {
				if(checked.get(i)) {
					checkedApps.add(mRetainedFragment.mLabelIcons.get(i).getPackageName());
				}
			}
//			Toast.makeText(getBaseContext(), "Selected items:"+checkedApps.toString(), Toast.LENGTH_SHORT).show();
			
			// put the selected result to MainActivity
			Intent result = new Intent();
			result.putStringArrayListExtra("result", checkedApps);
			AppGridActivity.this.setResult(RESULT_OK, result);
			AppGridActivity.this.finish();
			
			return true;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			Resources res = getResources();
			int count = appGrid.getCheckedItemCount();
			mode.setTitle(R.string.title_select_item);
			mode.setSubtitle(res.getQuantityString(R.plurals.sub_title_select_item, count, count));

            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.app_grid_select, menu);

			return true;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			/*MenuItem addItem = menu.add(R.string.action_add);
			addItem.setIcon(R.drawable.ic_add);
			addItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);*/
			return true;
		}

		@Override
		public void onItemCheckedStateChanged(ActionMode mode, int position,
				long id, boolean checked) {
			Resources res = getResources();
			int selectCount = appGrid.getCheckedItemCount();
			mode.setSubtitle(res.getQuantityString(R.plurals.sub_title_select_item, selectCount, selectCount));
		}
		
	}

}



class CheckableView extends FrameLayout implements Checkable {

	private boolean mChecked;
	private FrameLayout mView;
	
	public CheckableView(Context context, ViewGroup parent) {
		super(context);
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mView = (FrameLayout) inflater.inflate(R.layout.app_grid_cell, parent, false);
		this.addView(mView);
	}
	
	public CheckableView(Context context, ViewGroup parent, Drawable img, CharSequence name) {
		this(context, parent);
		setView(img, name);
	}
	
	protected void setView(Drawable img, CharSequence name) {
		getImageView().setImageDrawable(img);
		setAppName(name);
	}
	
	protected void setAppName(CharSequence name) {
		((TextView)mView.findViewById(R.id.app_name)).setText(name);
	}
	
	protected ImageView getImageView() {
		return ((ImageView)mView.findViewById(R.id.app_img));
	}
	

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked(boolean checked) {
		mChecked = checked;
		
		if(checked) {
			mView.findViewById(R.id.app_overlay).setVisibility(VISIBLE);
		} else {
			mView.findViewById(R.id.app_overlay).setVisibility(INVISIBLE);
		}
	}

	@Override
	public void toggle() {
		setChecked(!mChecked);
	}
	
}

