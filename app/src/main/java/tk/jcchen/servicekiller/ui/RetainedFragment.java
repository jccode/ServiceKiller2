package tk.jcchen.servicekiller.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.SearchView;

import java.util.ArrayList;
import java.util.List;

import tk.jcchen.servicekiller.R;

public class RetainedFragment extends Fragment implements SearchView.OnQueryTextListener {

	
	public interface RetainedCallbacks {
		void onPostExecute();
	}
	
	private final static String TAG = "ServiceKiller";
	private RetainedCallbacks mCallback;
	private RelativeLayout progressbarComp;
	List<ResolveInfo> mApps = null;
	List<IconEntity> mLabelIcons = null;
	AppsAdapter mAdapter = null;
	SearchView mSearchView = null;
	

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallback = (RetainedCallbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		createSearchView();
		
		new AsyncTask<Void, Void, Pair<List<ResolveInfo>,List<IconEntity>>>() {

			@Override
			protected void onPreExecute() {
			}

			@Override
			protected void onPostExecute(Pair<List<ResolveInfo>,List<IconEntity>> result) {
				mApps = result.first;
				mLabelIcons = result.second;
				mAdapter = new AppsAdapter();
				
				progressbarComp.setVisibility(View.GONE);
				mCallback.onPostExecute();
			}

			@Override
			protected Pair<List<ResolveInfo>,List<IconEntity>> doInBackground(Void... params) {
				List<ResolveInfo> apps = loadApps();
				List<IconEntity> labelIcons = initLableIcons(apps);
				return new Pair<List<ResolveInfo>, List<IconEntity>>(apps, labelIcons);
			}

			@Override
			protected void onProgressUpdate(Void... values) {
				super.onProgressUpdate(values);
			}
			
		}.execute();
	}
	

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		progressbarComp = (RelativeLayout) getActivity().findViewById(R.id.progressbarComp);
		if(mApps == null) {
			progressbarComp.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallback = null;
		progressbarComp = null;
	}
	

	@Override
	public void onDestroy() {
		super.onDestroy();
		mApps = null;
		mLabelIcons = null;
		mAdapter = null;
		mSearchView = null;
	}

	private List<ResolveInfo> loadApps() {
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
		return getActivity().getPackageManager().queryIntentActivities(mainIntent, 0);
	}
	
	private List<IconEntity> initLableIcons(List<ResolveInfo> apps) {
		List<IconEntity> labelIcons = new ArrayList<IconEntity>(apps.size());
		PackageManager pm = getActivity().getPackageManager();
		for(ResolveInfo info : apps) {
			ActivityInfo i = info.activityInfo;
			labelIcons.add(new IconEntity(i.loadLabel(pm).toString(), i.loadIcon(pm), i.packageName));
		}
		return labelIcons;
	}
	
	private void createSearchView() {
		mSearchView = new SearchView(getActivity());
		mSearchView.setOnQueryTextListener(this);
	}
	
	@Override
	public boolean onQueryTextChange(String newText) {
		if(TextUtils.isEmpty(newText)) {
			mAdapter.getFilter().filter("");
//			appGrid.clearTextFilter();
		} else {
			mAdapter.getFilter().filter(newText.toString());
		}
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}
	
	
	public class AppsAdapter extends BaseAdapter implements Filterable {
		
		private List<IconEntity> origData;
		
		public AppsAdapter() {
			origData = new ArrayList<IconEntity>(mLabelIcons);
		}

		@Override
		public int getCount() {
			return mLabelIcons.size();
		}

		@Override
		public Object getItem(int position) {
			return mLabelIcons.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			
			CheckableView view;
			if(convertView == null) {
				view = new CheckableView(getActivity(), parent);
			} else {
				view = (CheckableView) convertView;
			}
			
			IconEntity lableIcon = mLabelIcons.get(position);
			view.setView(lableIcon.getImage(), lableIcon.getName());
			
			return view;
		}

		@Override
		public Filter getFilter() {
			return new Filter() {

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					constraint = constraint.toString().toLowerCase();
					FilterResults result = new FilterResults();
					if(constraint != null && constraint.toString().length() > 0) {
						List<IconEntity> founded = new ArrayList<IconEntity>();
						for(IconEntity item : origData) {
							if(item.getName().toLowerCase().contains(constraint)) {
								founded.add(item);
							}
						}
						result.values = founded;
						result.count = founded.size();
						
					} else {
						result.values = origData;
						result.count = origData.size();
					}
					
					return result;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					mLabelIcons = (List<IconEntity>) results.values;
					notifyDataSetChanged();
				}
				
			};
		}
		
	}
}