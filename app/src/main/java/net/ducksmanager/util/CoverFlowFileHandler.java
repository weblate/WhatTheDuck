package net.ducksmanager.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.ducksmanager.whattheduck.CoverSearch;
import net.ducksmanager.whattheduck.R;
import net.ducksmanager.whattheduck.WhatTheDuck;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CoverFlowFileHandler {

    public static final long MAX_COVER_FILESIZE = 2048 * 1024;

    public static CoverFlowFileHandler current;

    public interface TransformationCallback {
        void onComplete(File outputFile);

        void onFail(File uploadFile);
    }

    private File uploadFile = null;

    private Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            try {
                FileOutputStream ostream = new FileOutputStream(uploadFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, ostream);
                ostream.flush();
                ostream.close();

                callback.onComplete(uploadFile);
            } catch (IOException e) {
                WhatTheDuck.wtd.alert(CoverSearch.cls, R.string.internal_error);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            WhatTheDuck.wtd.alert(CoverSearch.cls, R.string.internal_error);
            callback.onFail(uploadFile);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    private TransformationCallback callback = null;

    public CoverFlowFileHandler() {
    }

    public Uri createEmptyFileForCamera(Context context) {
        if (uploadFile == null) {
            File imagePath = new File(context.getFilesDir(), CoverSearch.uploadTempDir);
            uploadFile = new File(imagePath, CoverSearch.uploadFileName);
        }
        uploadFile.getParentFile().mkdirs();
        try {
            if (uploadFile.exists()) {
                uploadFile.delete();
            }
            if (!uploadFile.createNewFile()) {
                WhatTheDuck.wtd.alert(context, R.string.internal_error);
            }
            return FileProvider.getUriForFile(context, "net.ducksmanager.whattheduck.fileprovider", uploadFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void resizeUntilFileSize(final Activity activity, long maxFileSize, final TransformationCallback callback) {
        this.callback = callback;

        long fileSize = uploadFile.length();

        if (fileSize < maxFileSize) {
            callback.onComplete(uploadFile);
        }
        else {
            Picasso.with(activity).load(uploadFile).resize(1600, 1600).centerInside().into(target);
        }
    }
}