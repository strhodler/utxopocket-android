package com.msopentech.thali.android.toronionproxy.torinstaller;/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */
/* See LICENSE for licensing information */

import android.content.Context;
import android.content.pm.ApplicationInfo;
import com.strhodler.utxopocket.common.logging.SecureLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.TimeoutException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class TorResourceInstaller implements TorServiceConstants {

    File installFolder;
    Context context;

    File fileTorrc;
    File fileTor;

    public TorResourceInstaller(Context context, File installFolder) {
        this.installFolder = installFolder;
        this.context = context;
    }

    public File getTorrcFile() {
        return fileTorrc;
    }

    public File getTorFile() {
        return fileTor;
    }

    /**
     * private void deleteDirectory(File file) {
     * if( file.exists() ) {
     * if (file.isDirectory()) {
     * File[] files = file.listFiles();
     * for(int i=0; i<files.length; i++) {
     * if(files[i].isDirectory()) {
     * deleteDirectory(files[i]);
     * }
     * else {
     * files[i].delete();
     * }
     * }
     * }
     * <p>
     * file.delete();
     * }
     * }
     **/

    //
    /*
     * Extract the Tor resources from the APK file using ZIP
     *
     * @File path to the Tor executable
     */
    public File installResources() throws IOException, TimeoutException {

        File existingBinary = findExistingNativeBinary();
        if (existingBinary != null && existingBinary.exists()) {
            fileTor = existingBinary;
            if (fileTor.canExecute()) {
                return fileTor;
            }
            if (fileTor.setExecutable(true, false) && fileTor.canExecute()) {
                return fileTor;
            }
            SecureLog.w(TAG, "Native tor binary exists but is not executable, attempting fallback extraction");
        }

        if (!installFolder.exists() && !installFolder.mkdirs()) {
            throw new IOException("Unable to prepare tor install folder at " + installFolder.getAbsolutePath());
        }

        fileTor = new File(installFolder, TOR_ASSET_KEY);
        if (fileTor.exists() && !fileTor.delete()) {
            SecureLog.w(TAG, "Failed to delete stale tor binary at " + fileTor.getAbsolutePath());
        }

        File preparedBinary = NativeLoader.initNativeLibs(context, fileTor);
        if (preparedBinary != null && preparedBinary.exists()) {
            setExecutable(fileTor);
            if (fileTor.exists() && fileTor.canExecute()) {
                return fileTor;
            }
        }

        SecureLog.w(TAG, "Tor binary could not be prepared at " + fileTor.getAbsolutePath());
        return null;
    }


    // Return Full path to the directory where native JNI libraries are stored.
    private static String getNativeLibraryDir(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        return appInfo.nativeLibraryDir;
    }

    private File findExistingNativeBinary() {
        File nativeDir = new File(getNativeLibraryDir(context));
        if (!nativeDir.exists()) {
            return null;
        }

        File candidate = new File(nativeDir, TOR_ASSET_KEY);
        if (candidate.exists()) {
            return candidate;
        }

        File fallback = new File(nativeDir, "lib" + TOR_ASSET_KEY);
        if (fallback.exists()) {
            return fallback;
        }

        return null;
    }


    public boolean updateTorConfigCustom(File fileTorRcCustom, String extraLines) throws IOException, FileNotFoundException, TimeoutException {
        if (fileTorRcCustom.exists()) {
            fileTorRcCustom.delete();
            SecureLog.d("torResources", "deleting existing torrc.custom");
        } else
            fileTorRcCustom.createNewFile();

        FileOutputStream fos = new FileOutputStream(fileTorRcCustom, false);
        PrintStream ps = new PrintStream(fos);
        ps.print(extraLines);
        ps.close();

        return true;
    }


    /*
     * Write the inputstream contents to the file
     */
    private static boolean streamToFile(InputStream stm, File outFile, boolean append, boolean zip) throws IOException {
        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

        OutputStream stmOut = new FileOutputStream(outFile.getAbsolutePath(), append);
        ZipInputStream zis = null;

        if (zip) {
            zis = new ZipInputStream(stm);
            ZipEntry ze = zis.getNextEntry();
            stm = zis;

        }

        while ((bytecount = stm.read(buffer)) > 0) {

            stmOut.write(buffer, 0, bytecount);

        }

        stmOut.close();
        stm.close();

        if (zis != null)
            zis.close();


        return true;

    }


    private void setExecutable(File fileBin) {
        if (fileBin == null) {
            return;
        }
        if (!fileBin.setReadable(true, false)) {
            SecureLog.w(TAG, "Failed to mark tor binary readable");
        }
        if (!fileBin.setExecutable(true, false)) {
            SecureLog.w(TAG, "Failed to mark tor binary executable");
        }
        fileBin.setWritable(false, false);
        fileBin.setWritable(true, true);
    }

    private static File[] listf(String directoryName) {

        // .............list file
        File directory = new File(directoryName);

        // get all the files from a directory
        File[] fList = directory.listFiles();

        if (fList != null)
            for (File file : fList) {
                if (file.isFile()) {
                    SecureLog.d(TAG, file.getAbsolutePath());
                } else if (file.isDirectory()) {
                    listf(file.getAbsolutePath());
                }
            }

        return fList;
    }

}
