package com.github.sunnyseaside.timezonefixer;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//No need to use ViewModel or LiveData for now, but might be a possibilty when adding filtering functionality
//Caution: don't store Activity when switching to ViewModel
public class PhotoDataManager {//} extends ViewModel {
    private Cursor cursor;
    private ContentResolver resolver;
    private static int COL_ID=0;
    private static int COL_DISPLAY_NAME=1;
    private static int COL_DATE_ADDED=2;
    private static int COL_DATE_MODIFIED=3;
    private static int COL_DATE_TAKEN=4;
    private static int COL_DATA=5;
    PhotoDataManager(Context context){
        resolver=context.getContentResolver();
        String[]projection={Media._ID,Media.DISPLAY_NAME,Media.DATE_ADDED,Media.DATE_MODIFIED,Media.DATE_TAKEN,Media.DATA};
        ///todo custom sort order
        cursor=resolver.query(Media.EXTERNAL_CONTENT_URI,projection,null,null,Media.DATE_TAKEN/*+" DESC"*/);//Media.DEFAULT_SORT_ORDER);

    }
    public interface ProgressListener{
        public void onProgress(int pos);
        public void onFinished();
    }
    public void fixAll(Handler handler, final ProgressListener listener){
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            PhotoData pd=new PhotoData(cursor,resolver);
            ExifInterface exif=pd.getExif();
            if(pd.shouldFix(exif)){
                pd.doFix(exif);
            }
            cursor.moveToNext();

            final int pos=cursor.getPosition();
            handler.post(new Runnable(){
                @Override public void run(){
                    listener.onProgress(pos);
                }
            });
        }
        handler.post(new Runnable(){
            @Override public void run(){
                listener.onFinished();
            }
        });
    }
    /**caution: only one query at once*/
    public PhotoData get(int pos){
        cursor.moveToPosition(pos);
        return new PhotoData(cursor,resolver);
    }
    public int getCount(){return cursor.getCount();}
    static public class PhotoData extends Object{
        private Cursor cursor;
        private ContentResolver resolver;

        private PhotoData(Cursor c,ContentResolver r){
            cursor=c;
            resolver=r;
        }
        private Uri getUri(){
            return Uri.withAppendedPath(Media.EXTERNAL_CONTENT_URI, cursor.getString(COL_ID));
        }
        private ExifInterface getExif(){
            try {
                ExifInterface exif;
                if (Build.VERSION.SDK_INT >= 24) {
                    Uri photoUri = getUri();
                    //todo update SDK 29
                    //if (Build.VERSION.SDK_INT >= 29)
                    //    photoUri = MediaStore.setRequireOriginal(photoUri);//TODO request permission
                    InputStream is = resolver.openInputStream(photoUri);
                    exif = new ExifInterface(is);
                } else {
                    String path = cursor.getString(COL_DATA);
                    exif = new ExifInterface(path);
                }
                return exif;
            }catch(IOException e){
                return null;
            }
        }
        private boolean shouldFix(ExifInterface exif){
                return exif.getAttribute(exif.TAG_DATETIME_ORIGINAL) != null && (
                        exif.getAttribute(exif.TAG_GPS_DATESTAMP) == null || exif.getAttribute(exif.TAG_GPS_TIMESTAMP) == null ||
                        exif.getAttribute(exif.TAG_GPS_ALTITUDE) == null || exif.getAttribute(exif.TAG_GPS_LONGITUDE) == null);

        }
        public boolean shouldFix(){return shouldFix(getExif());}
        private void setDateTaken(long t) {
            ContentValues values=new ContentValues();
            long date=getDateTaken();
            values.put(Media.DATE_TAKEN,t);
            resolver.update(getUri(),values,Media._ID+"=?",new String[]{cursor.getString(COL_ID)});
        }
        public void doFix(){doFix(getExif());}
        private void doFix(ExifInterface exif){
            //long date=getDateTaken()-x*3600*1000;///todo custom timezone
            String dateStr=exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if(dateStr!=null) {
                SimpleDateFormat fmt = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                //fmt.setTimeZone()
                try {
                    Date date = fmt.parse(dateStr);
                    setDateTaken(date.getTime());
                } catch (ParseException e) {

                }
                //DateTimeFormatter formatter=DateTimeFormatter.ofPattern("uuuu:MM:dd HH:mm:ss");
            }

        }
        public String getName(){return cursor.getString(COL_DISPLAY_NAME);}
        //public long getDateAdded(){return Long.parseLong(cursor.getString(1));}
        public long getDateAdded(){return cursor.getLong(COL_DATE_ADDED);}
        public long getDateModified(){return cursor.getLong(COL_DATE_MODIFIED);}
        //public long getDateModified(){return Long.parseLong(cursor.getString(2));}
        public long getDateTaken(){return cursor.getLong(COL_DATE_TAKEN);}
    }

}
