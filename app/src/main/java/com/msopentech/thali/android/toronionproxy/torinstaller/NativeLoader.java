
package com.msopentech.thali.android.toronionproxy.torinstaller;

import android.content.Context;
import android.os.Build;
import com.strhodler.utxopocket.common.logging.SecureLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NativeLoader {

    private final static String LIB_NAME = "tor";
    private final static String LIB_SO_NAME = "tor.so";
    private final static String FALLBACK_LIB_SO_NAME = "libtor.so";
    private final static String LIB_DIR = "lib";
    private final static String JNI_DIR = "jni";

    private final static String TAG = "TorNativeLoader";

    private static boolean loadFromZip(Context context, File destLocalFile, String arch) {


        ZipFile zipFile = null;
        InputStream stream = null;

        try {
            zipFile = new ZipFile(context.getApplicationInfo().sourceDir);
            ZipEntry entry = findLibraryEntry(zipFile, arch);
            if (entry == null) {
                throw new Exception("Unable to find file in apk:" + "lib/" + arch + "/" + LIB_NAME);
            }

            //how we wrap this in another stream because the native .so is zipped itself
            stream = zipFile.getInputStream(entry);

            OutputStream out = new FileOutputStream(destLocalFile);
            byte[] buf = new byte[4096];
            int len;
            while ((len = stream.read(buf)) > 0) {
                Thread.yield();
                out.write(buf, 0, len);
            }
            out.close();

            destLocalFile.setReadable(true, false);
            destLocalFile.setExecutable(true, false);
            destLocalFile.setWritable(true);

            return true;
        } catch (Exception e) {
            SecureLog.e(TAG, e, e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception e) {
                    SecureLog.e(TAG, e, e.getMessage());
                }
            }
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    SecureLog.e(TAG, e, e.getMessage());
                }
            }
        }
        return false;
    }

    public static synchronized File initNativeLibs(Context context, File destLocalFile) {

        try {
            String folder = Build.CPU_ABI;
            if (loadFromZip(context, destLocalFile, folder)) {
                return destLocalFile;
            }

        } catch (Throwable e) {
            SecureLog.e(TAG, e, e.getMessage());
        }


        return null;
    }

    private static ZipEntry findLibraryEntry(ZipFile zipFile, String arch) {
        String[] candidateDirs = new String[]{LIB_DIR, JNI_DIR};
        String[] candidateNames = new String[]{LIB_SO_NAME, FALLBACK_LIB_SO_NAME};
        for (String dir : candidateDirs) {
            for (String name : candidateNames) {
                ZipEntry entry = zipFile.getEntry(dir + "/" + arch + "/" + name);
                if (entry != null) {
                    return entry;
                }
            }
        }
        return null;
    }
}
