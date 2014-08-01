package cn.ljj.musicplayer.ui;

import java.util.ArrayList;
import java.util.List;

import cn.ljj.musicplayer.R;
import cn.ljj.musicplayer.playlist.SavedList;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class SavedListAdapter extends BaseAdapter {
	public static final String TAG = "SavedListAdapter";
	Context mContext = null;
	List<SavedList> mListItems = null;
	LayoutInflater mInflater = null;

	public SavedListAdapter(Context context, List<SavedList> list) {
		mContext = context;
		if (list == null) {
			list = new ArrayList<SavedList>();
		}
		mListItems = list;
		mInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
	public int getCount() {
		return mListItems.size() + 1;
	}

	@Override
	public Object getItem(int position) {
		SavedList item = null;
		try {
			item = mListItems.get(position);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return item;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		if (convertView != null && position < mListItems.size()
				&& mAddView != convertView) {
			view = convertView;
		} else {
			view = newView(parent, position);
		}
		bindView(view, position);
		return view;
	}

	private View newView(ViewGroup parent, int position) {
		View view = null;
		if (position < mListItems.size()) {
			view = mInflater
					.inflate(R.layout.list_item_playlist, parent, false);
		} else {
			view = getAddView(parent);
		}
		return view;
	}

	private void bindView(final View view, final int position) {
		if (position < mListItems.size()) {
			TextView name = (TextView) view.findViewById(R.id.list_name);
			final SavedList item = (SavedList) getItem(position);
			name.setText(item.getListName() + " (" + item.getCount() + ")");
			Button addButton = (Button) view.findViewById(R.id.btn_add_to_list);
			addButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemViewClick(position, R.id.btn_add_to_list, view);
				}
			});
			ImageView isPalying = (ImageView) view
					.findViewById(R.id.img_isplaying);
			if (item.isPlaying()) {
				isPalying.setVisibility(View.VISIBLE);
			} else {
				isPalying.setVisibility(View.GONE);
			}
		}
	}

	private View mAddView = null;

	private View getAddView(ViewGroup parent) {
		if (mAddView == null) {
			mAddView = mInflater.inflate(R.layout.layout_buttom_add_item,
					parent, false);
			initAddListButton();
		}
		return mAddView;
	}

	public void setOnItemViewClickListner(OnItemViewClickListner l) {
		mItemViewClickListner = l;
		initAddListButton();
	}

	private void initAddListButton() {
		if (mAddView != null) {
			Button btnAdd = (Button) mAddView.findViewById(R.id.btn_add_list);
			btnAdd.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					onItemViewClick(getCount(), R.id.btn_add_list, mAddView);
				}
			});
		}
	}

	public void onItemViewClick(int position, int resId, View parent) {
		if (mItemViewClickListner != null) {
			mItemViewClickListner.onItemViewClick(position, resId, parent);
		}
	}

	OnItemViewClickListner mItemViewClickListner;

	public interface OnItemViewClickListner {
		public void onItemViewClick(int position, int resId, View parent);
	}
}
