package cn.ljj.musicplayer.playlist;

import java.util.ArrayList;
import java.util.List;
import cn.ljj.musicplayer.data.MusicInfo;
import cn.ljj.musicplayer.database.Logger;
import cn.ljj.musicplayer.database.MusicPlayerDatabase;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.MediaStore;

public class PlayListPersister {
	private Context mContext = null;
	private SQLiteDatabase db = null;
	private String TAG = "PlayListPersister";

	public PlayListPersister(Context context) {
		mContext = context;
		db = MusicPlayerDatabase.getInstance(mContext).getWritableDatabase();
	}

	public List<MusicInfo> load(String listName) {
		Logger.d(TAG, "load listName=" + listName);
		List<MusicInfo> list = new ArrayList<MusicInfo>();
		String sql = "select _id from " + MusicPlayerDatabase.TABLE_LIST
				+ " where " + MusicPlayerDatabase.LIST_NAME + " = '" + listName + "'";
		Cursor cursor = null;
		try {
			long listId = -1;
			cursor = db.rawQuery(sql, null);
			if (cursor.moveToFirst()) {
				listId = cursor.getLong(0);
			}
			cursor.close();

			sql = "select * from " + MusicPlayerDatabase.TABLE_MUSICS
					+ " where " + MusicPlayerDatabase.LIST_ID + " = " + listId;
			cursor = db.rawQuery(sql, null);
			list = getMusics(cursor);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return list;
	}

	private List<MusicInfo> getMusics(Cursor cursor) {
		List<MusicInfo> list = new ArrayList<MusicInfo>();
		int idIndex = cursor.getColumnIndex("_id");
		int nameIndex = cursor.getColumnIndex(MusicPlayerDatabase.NAME);
		int artistIndex = cursor.getColumnIndex(MusicPlayerDatabase.ARTIST);
		int albumIndex = cursor.getColumnIndex(MusicPlayerDatabase.ALBUM);
		int durationIndex = cursor.getColumnIndex(MusicPlayerDatabase.DURATION);
		int musicPathIndex = cursor
				.getColumnIndex(MusicPlayerDatabase.MUSIC_PATH);
		int lrcPathIndex = cursor.getColumnIndex(MusicPlayerDatabase.LRC_PATH);
		int picPathIndex = cursor.getColumnIndex(MusicPlayerDatabase.PIC_PATH);
		int listIdIndex = cursor.getColumnIndex(MusicPlayerDatabase.LIST_ID);
		while (cursor.moveToNext()) {
			long _id = cursor.getInt(idIndex);
			String name = cursor.getString(nameIndex);
			String artist = cursor.getString(artistIndex);
			String album = cursor.getString(albumIndex);
			int duration = cursor.getInt(durationIndex);
			String musicPath = cursor.getString(musicPathIndex);
			String lrcPath = cursor.getString(lrcPathIndex);
			String picPath = cursor.getString(picPathIndex);
			long listId = cursor.getLong(listIdIndex);

			MusicInfo music = new MusicInfo(name, musicPath);
			music.setId(_id);
			music.setAlbum(album);
			music.setArtist(artist);
			music.setDuration(duration);
			music.setLrcPath(lrcPath);
			music.setPicPath(picPath);
			music.setListId(listId);
			list.add(music);
		}
		return list;
	}

	public int persist(List<MusicInfo> list, String listName, boolean cover) {
		Logger.d(TAG, "persist list=" + list + "; listName=" + listName
				+ "; cover=" + cover);
		String sql = "select _id from " + MusicPlayerDatabase.TABLE_LIST
				+ " where " + MusicPlayerDatabase.LIST_NAME + " = '" + listName + "'";
		Cursor cursor = null;
		try {

			long listId = -1;
			cursor = db.rawQuery(sql, null);
			if (cursor.moveToFirst()) {
				listId = cursor.getLong(0);
			}
			cursor.close();
			if (listId != -1) {
				if (cover) {
					// clear musics under this list
					db.delete(MusicPlayerDatabase.TABLE_MUSICS,
							MusicPlayerDatabase.LIST_ID + " = " + listId, null);
				}
			} else {
				// create new list
				ContentValues values = new ContentValues();
				values.put(MusicPlayerDatabase.LIST_NAME, listName);
				listId = db.insert(MusicPlayerDatabase.TABLE_LIST,
						MusicPlayerDatabase.LIST_NAME, values);
			}
			if (listId == -1) {
				return -1;
			}
			// insert musics table
			db.beginTransaction();
			for (MusicInfo music : list) {
				ContentValues values = new ContentValues();
				values.put(MusicPlayerDatabase.LIST_ID, listId);
				values.put(MusicPlayerDatabase.NAME, music.getName());
				values.put(MusicPlayerDatabase.ALBUM, music.getAlbum());
				values.put(MusicPlayerDatabase.ARTIST, music.getArtist());
				values.put(MusicPlayerDatabase.DURATION, music.getDuration());
				values.put(MusicPlayerDatabase.MUSIC_PATH, music.getMusicPath());
				values.put(MusicPlayerDatabase.LRC_PATH, music.getLrcPath());
				values.put(MusicPlayerDatabase.PIC_PATH, music.getPicPath());
				db.insert(MusicPlayerDatabase.TABLE_MUSICS,
						MusicPlayerDatabase.NAME, values);
			}
			db.setTransactionSuccessful();
			db.endTransaction();
			// updata list count
			sql = "select * from " + MusicPlayerDatabase.TABLE_MUSICS
					+ " where " + MusicPlayerDatabase.LIST_ID + " = " + listId;
			cursor = db.rawQuery(sql, null);
			ContentValues values = new ContentValues();
			values.put(MusicPlayerDatabase.LIST_SIZE, cursor.getCount());
			db.update(MusicPlayerDatabase.TABLE_LIST, values,
					" _id = " + listId, null);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return list.size();
	}

	public List<MusicInfo> loadFromMediaStore() {
		Logger.d(TAG, "loadFromMediaStore");
		List<MusicInfo> list = new ArrayList<MusicInfo>();
		ContentResolver cr = mContext.getContentResolver();
		Cursor playlistCursor = null;
		try {
			playlistCursor = cr.query(
					MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null,
					null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
			while (playlistCursor.moveToNext()) {
				int isMusic = playlistCursor
						.getInt(playlistCursor
								.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
				if (isMusic == 1) {
					String name = playlistCursor
							.getString(playlistCursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME));
					String path = playlistCursor
							.getString(playlistCursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
					String duration = playlistCursor
							.getString(playlistCursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
					String artist = playlistCursor
							.getString(playlistCursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
					String album = playlistCursor
							.getString(playlistCursor
									.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
					int durationInt = Integer.parseInt(duration);
					String format = "";
					if(name.contains(".")){
						String temp = name;
						name = temp.substring(0,temp.lastIndexOf("."));
						format = temp.substring(temp.lastIndexOf(".")+1);
					}
					MusicInfo music = new MusicInfo(name, path);
					music.setDuration(durationInt);
					music.setAlbum(album);
					music.setArtist(artist);
					music.setFormat(format);
					list.add(music);
				}
			}
			playlistCursor.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (playlistCursor != null) {
				playlistCursor.close();
			}
		}
		return list;
	}

	public Long persist(MusicInfo music, String listName) {
		Logger.d(TAG, "persist music=" + music + "; listName=" + listName);
		String sql = "select * from " + MusicPlayerDatabase.TABLE_LIST
				+ " where " + MusicPlayerDatabase.LIST_NAME + " = '" + listName + "'";
		long musicId = -1;
		Cursor cursor = null;
		try {
			long listId = -1;
			cursor = db.rawQuery(sql, null);
			if (cursor.moveToFirst()) {
				listId = cursor.getLong(
						cursor.getColumnIndex("_id"));
			}
			cursor.close();
			cursor = null;
			if (listId != -1) {
				ContentValues values = new ContentValues();
				values.put(MusicPlayerDatabase.LIST_ID, listId);
				values.put(MusicPlayerDatabase.NAME, music.getName());
				values.put(MusicPlayerDatabase.ALBUM, music.getAlbum());
				values.put(MusicPlayerDatabase.ARTIST, music.getArtist());
				values.put(MusicPlayerDatabase.DURATION, music.getDuration());
				values.put(MusicPlayerDatabase.MUSIC_PATH, music.getMusicPath());
				values.put(MusicPlayerDatabase.LRC_PATH, music.getLrcPath());
				values.put(MusicPlayerDatabase.PIC_PATH, music.getPicPath());
				musicId = db.insert(MusicPlayerDatabase.TABLE_MUSICS,
						MusicPlayerDatabase.NAME, values);
				updateListSize(listId);
				music.setListId(listId);
				music.setId(musicId);
			}
		} catch (Exception e) {
			e.printStackTrace();
			musicId = -1;
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return musicId;
	}

	public long removeMusic(long musicId, long listId) {
		Logger.i(TAG, "removeMusic MusicId=" + musicId);
		if (musicId == -1 || listId == -1) {
			return -1;
		}
		db.delete(MusicPlayerDatabase.TABLE_MUSICS, "_id = " + musicId, null);
		updateListSize(listId);
		return listId;
	}

	public int deletePlayList(String listName) {
		Logger.d(TAG, "deletePlayList listName=" + listName);
		String sql = "select _id from " + MusicPlayerDatabase.TABLE_LIST
				+ " where " + MusicPlayerDatabase.LIST_NAME + " = '" + listName + "'";
		int ret = -1;
		Cursor cursor = null;
		try {

			long listId = -1;
			cursor = db.rawQuery(sql, null);
			if (cursor.moveToFirst()) {
				listId = cursor.getLong(0);
			}
			cursor.close();
			cursor = null;
			if (listId != -1) {
				ret = db.delete(MusicPlayerDatabase.TABLE_MUSICS,
						MusicPlayerDatabase.LIST_ID + " = " + listId, null);
				db.delete(MusicPlayerDatabase.TABLE_LIST, " _id = " + listId,
						null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return ret;
	}

	public List<SavedList> getAllSavedPlayList() {
		List<SavedList> list = new ArrayList<SavedList>();
		String sql = "select * from " + MusicPlayerDatabase.TABLE_LIST;
		Cursor cursor = null;
		try {
			cursor = db.rawQuery(sql, null);
			int idIndex = cursor.getColumnIndex("_id");
			int countIndex = cursor
					.getColumnIndex(MusicPlayerDatabase.LIST_SIZE);
			int nameIndex = cursor
					.getColumnIndex(MusicPlayerDatabase.LIST_NAME);
			int isPlayingIndex = cursor
					.getColumnIndex(MusicPlayerDatabase.LIST_IS_PLAYING);
			while (cursor.moveToNext()) {
				SavedList l = new SavedList();
				l.setCount(cursor.getInt(countIndex));
				l.setListId(cursor.getInt(idIndex));
				l.setListName(cursor.getString(nameIndex));
				l.setPlaying(cursor.getInt(isPlayingIndex) == 1);
				list.add(l);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		Logger.i(TAG, "getAllSavedPlayList list=" + list);
		return list;
	}
	
	public long update(MusicInfo music){
		long musicId = music.getId();
		if (musicId != -1) {
			ContentValues values = new ContentValues();
			values.put(MusicPlayerDatabase.NAME, music.getName());
			values.put(MusicPlayerDatabase.ALBUM, music.getAlbum());
			values.put(MusicPlayerDatabase.ARTIST, music.getArtist());
			values.put(MusicPlayerDatabase.DURATION, music.getDuration());
			values.put(MusicPlayerDatabase.MUSIC_PATH, music.getMusicPath());
			values.put(MusicPlayerDatabase.LRC_PATH, music.getLrcPath());
			values.put(MusicPlayerDatabase.PIC_PATH, music.getPicPath());
			musicId = db.update(MusicPlayerDatabase.TABLE_MUSICS, values, "_id="+musicId,null);
		}
		return musicId;
	}
	
	private void updateListSize(long listId){
		Cursor c = null;
		try{
			String sql = "select * from " + MusicPlayerDatabase.TABLE_MUSICS
					+ " where " + MusicPlayerDatabase.LIST_ID  + " = "+ listId;
			c = db.rawQuery(sql, null);
			int listSize = c.getCount();
			ContentValues values = new ContentValues();
			values.put(MusicPlayerDatabase.LIST_SIZE, listSize);
			db.update(MusicPlayerDatabase.TABLE_LIST, values, "_id="+listId, null);
		}finally{
			if(c != null){
				c.close();
			}
		}
	}

	public void updatePlaying(int id){
		clearPlaying();
		ContentValues values = new ContentValues();
		values.put(MusicPlayerDatabase.LIST_IS_PLAYING, 1);
		db.update(MusicPlayerDatabase.TABLE_LIST, values, "_id="+id, null);
	}
	
	public long updatePlaying(String listName){
		clearPlaying();
		ContentValues values = new ContentValues();
		values.put(MusicPlayerDatabase.LIST_IS_PLAYING, 1);
		return db.update(MusicPlayerDatabase.TABLE_LIST, values,
				MusicPlayerDatabase.LIST_NAME+" = '"+listName + "'", null);
	}

	public void clearPlaying(){
		ContentValues values = new ContentValues();
		values.put(MusicPlayerDatabase.LIST_IS_PLAYING, 0);
		db.update(MusicPlayerDatabase.TABLE_LIST, values, null, null);
		
	}
}
