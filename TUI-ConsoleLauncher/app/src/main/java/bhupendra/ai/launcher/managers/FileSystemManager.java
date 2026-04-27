package bhupendra.ai.launcher.managers;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;
import android.util.Log;
import java.io.*;
import java.nio.channels.Channels;
import java.util.*;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.MimeTypes;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.managers.music.Song;



public class FileSystemManager {

    private static File folder = null;

    public static void init(Context context) {
        if (folder != null) return;
        folder = context.getExternalFilesDir(null);
        if (folder == null) {
            folder = context.getFilesDir();
        }
    }
    public static int nOfBytes(File file) {
            int count = 0;
            try {
                FileInputStream in = new FileInputStream(file);
    
                while(in.read() != -1) count++;
    
                return count;
            } catch (IOException e) {
                Tuils.log(e);
                return count;
            }
        }

    public static boolean containsExtension(String[] array, String value) {
            try {
                value = value.toLowerCase().trim();
                for (String s : array) {
                    if (value.endsWith(s)) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

    public static List<Song> getSongsInFolder(File folder) {
            List<Song> songs = new ArrayList<>();
    
            File[] files = folder.listFiles();
            if(files == null || files.length == 0) {
                return songs;
            }
    
            for (File file : files) {
                if (file.isDirectory()) {
                    List<Song> s = getSongsInFolder(file);
                    if(s != null) {
                        songs.addAll(s);
                    }
                }
                else if (containsExtension(MusicManager2.MUSIC_EXTENSIONS, file.getName())) {
                    songs.add(new Song(file));
                }
            }
    
            return songs;
        }

    public static String inputStreamToString(InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : Tuils.EMPTYSTRING;
        }

    public static String readerToString(Reader initialReader) throws IOException {
            char[] arr = new char[8 * 1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = initialReader.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            initialReader.close();
            return buffer.toString();
        }

    public static void toFile(String s) {
            try {
                RandomAccessFile f = new RandomAccessFile(new File(FileSystemManager.getFolder(), "crash.txt"), "rw");
                f.seek(0);
                f.write((new Date().toString() + Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
                OutputStream is = Channels.newOutputStream(f.getChannel());
                is.write(s.getBytes());
                f.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
    
                is.close();
                f.close();
            } catch (Exception e1) {}
        }

    public static void toFile(Object o) {
            if(o == null) return;
    
    //            RandomAccessFile f = new RandomAccessFile(new File(FileSystemManager.getFolder(), "crash.txt"), "rw");
    //            f.seek(0);
    //            f.write((new Date().toString() + Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
    //            OutputStream is = Channels.newOutputStream(f.getChannel());
    //            e.printStackTrace(new PrintStream(is));
    //            f.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
    //
    //            is.close();
    //            f.close();
    
            try {
                FileOutputStream stream = new FileOutputStream(new File(FileSystemManager.getFolder(), "crash.txt"));
                stream.write((Tuils.NEWLINE + Tuils.NEWLINE).getBytes());
    
                if(o instanceof Throwable) {
                    PrintStream ps = new PrintStream(stream);
                    ((Throwable) o).printStackTrace(ps);
                } else {
                    stream.write(o.toString().getBytes());
                }
    
                stream.write((Tuils.NEWLINE + "----------------------------").getBytes());
    
                stream.close();
            } catch (Exception e1) {}
        }

    public static Intent shareFile(Context c, File f) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    
            Uri u = buildFile(c, f);
    
            String mimetype = MimeTypes.getMimeType(f.getAbsolutePath(), f.isDirectory());
    
            intent.setDataAndType(u, mimetype);
    
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, u);
    
            return intent;
        }

    public static Intent openFile(Context c, File f) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    
            Uri u = buildFile(c, f);
            String mimetype = MimeTypes.getMimeType(f.getAbsolutePath(), f.isDirectory());
    
            intent.setDataAndType(u, mimetype);
    
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
    
            return intent;
        }

    public static long download(InputStream in, File file) throws Exception {
            OutputStream out = new FileOutputStream(file, false);
    
            byte data[] = new byte[1024];
    
            long bytes = 0;
    
            int count;
            while ((count = in.read(data)) != -1) {
                out.write(data, 0, count);
                bytes += count;
            }
    
            out.flush();
            out.close();
            in.close();
    
            return bytes;
        }

    public static void write(File file, String separator, String... ss) throws Exception {
            FileOutputStream headerStream = new FileOutputStream(file, false);
    
            for(int c = 0; c < ss.length - 1; c++) {
                headerStream.write(ss[c].getBytes());
                headerStream.write(separator.getBytes());
            }
            headerStream.write(ss[ss.length - 1].getBytes());
    
            headerStream.flush();
            headerStream.close();
        }

    public static void deleteContentOnly(File dir) {
            File[] files = dir.listFiles();
            if(files == null) return;
    
            for(File f : dir.listFiles()) {
                if(f.isDirectory()) delete(f);
                f.delete();
            }
        }

    public static void delete(File dir) {
            File[] files = dir.listFiles();
            if(files == null) return;
    
            for(File f : dir.listFiles()) {
                if(f.isDirectory()) delete(f);
                f.delete();
            }
            dir.delete();
        }

    public static boolean insertOld(File oldFile) {
            if(oldFile == null || !oldFile.exists()) return false;
    
            String oldPath = oldFile.getAbsolutePath();
    
            File oldFolder = new File(FileSystemManager.getFolder(), "old");
            if(!oldFolder.exists()) oldFolder.mkdir();
    
            File dest = new File(oldFolder, oldFile.getName());
            if(dest.exists()) dest.delete();
    
            return oldFile.renameTo(dest) && new File(oldPath).delete();
        }

    public static File getOld(String name) {
            File old = new File(FileSystemManager.getFolder(), "old");
            File file = new File(old, name);
    
            if(file.exists()) return file;
            return null;
        }

    public static String readFile(File file) throws IOException {
        FileInputStream in = new FileInputStream(file);
        String s = inputStreamToString(in);
        in.close();
        return s;
    }

    public static void saveFile(File file, String content) throws IOException {
        FileOutputStream out = new FileOutputStream(file, false);
        out.write(content.getBytes());
        out.flush();
        out.close();
    }

    public static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static void copyDirectory(File sourceLocation , File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            copyFile(sourceLocation, targetLocation);
        }
    }

    public static File getFolder() {
            if(folder != null) return folder;
            return null;
        }


    public static Uri buildFile(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".FILE_PROVIDER", file);
    }

}
