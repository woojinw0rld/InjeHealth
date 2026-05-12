package com.example.injehealth.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

public class PhotoFileHelper {

    private static final String AUTHORITY_SUFFIX = ".fileprovider";

    /**
     * Uri → getFilesDir()/<subdir>/<fileName> 복사 후 상대경로 반환.
     * 운동/식단 사진 저장 공통 메서드.
     */
    public static String copyToSubdir(Context context, android.net.Uri uri, String subdir, String fileName) throws java.io.IOException {
        java.io.File dir = new java.io.File(context.getFilesDir(), subdir);
        if (!dir.exists()) dir.mkdirs();
        java.io.File dest = new java.io.File(dir, fileName);
        try (java.io.InputStream in = context.getContentResolver().openInputStream(uri);
             java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
            if (in == null) throw new java.io.IOException("Cannot open URI: " + uri);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
        return subdir + "/" + fileName;
    }

    /**
     * 우리가 만든 로컬 파일이면 삭제. content://fileprovider 경로만 삭제 대상.
     * 갤러리 content URI(media store)는 무시.
     */
    public static void deleteIfLocal(Context context, String persisted) {
        if (persisted == null || persisted.isEmpty()) return;
        String authority = context.getPackageName() + AUTHORITY_SUFFIX;
        Uri uri = Uri.parse(persisted);
        // fileprovider URI인 경우만 삭제
        if (authority.equals(uri.getAuthority())) {
            // FileProvider URI → 실제 파일 경로 역산 불가, Pictures 디렉토리에서 파일명으로 찾기
            String path = uri.getPath(); // /diet_photos/diet_xxx.jpg
            if (path != null) {
                String fileName = new File(path).getName();
                File storageDir = context.getExternalFilesDir("Pictures");
                if (storageDir != null) {
                    File target = new File(storageDir, fileName);
                    if (target.exists()) {
                        target.delete();
                    }
                }
            }
        }
    }
}
