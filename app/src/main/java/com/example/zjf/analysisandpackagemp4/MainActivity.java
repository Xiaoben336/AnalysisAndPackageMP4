package com.example.zjf.analysisandpackagemp4;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String SDCARD_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String INPUT_FILEPATH = SDCARD_PATH + "/input.mp4";
    private static final String OUTPUT_FILEPATH = SDCARD_PATH + "/output.mp4";

    private TextView mLogView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 获取权限
        int checkWriteExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int checkReadExternalPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);if (checkWriteExternalPermission != PackageManager.PERMISSION_GRANTED ||
                checkReadExternalPermission != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                try {
                    transcode(INPUT_FILEPATH,OUTPUT_FILEPATH);
                } catch (IOException e) {
                    e.printStackTrace();
                    logout(e.getMessage());
                }
            }
        }).start();

        mLogView = (TextView)findViewById(R.id.LogView);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    protected boolean transcode(String input, String output) throws IOException {
        logout("start processing...");

        MediaMuxer muxer = null;

        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(input);

        logout("start demuxer: " + input);

        int mVideoTrackIndex = 1;
        for (int i =0;i < extractor.getTrackCount(); i++){
            MediaFormat format = extractor.getTrackFormat(i);
            //一个MIME类型包括一个类型（type），一个子类型（subtype）。此外可以加上一个或多个可选参数（optional parameter）。其格式为
            // 类型名 / 子类型名 [ ; 可选参数 ]   目前已被注册的类型名有application、audio、example、image、message、model、multipart、text，以及video
            //常见的mimetype配置列表:
            //Video Type	        Extension	    MIME Type
            //Flash	                .flv	        video/x-flv
            //MPEG-4	            .mp4	        video/mp4
            //iPhone Index	        .m3u8	        application/x-mpegURL
            //iPhone Segment	    .ts	            video/MP2T
            //3GP Mobile	        .3gp	        video/3gpp
            //QuickTime	            .mov	        video/quicktime
            //A/V Interleave	    .avi	        video/x-msvideo
            //Windows Media	        .wmv	        video/x-ms-wmv

            String mime = format.getString(MediaFormat.KEY_MIME);
            //此处可能有问题if (!mime.startsWith("video/")){

            if (!mime.startsWith("video")){
                logout("mime not video, continue search");
                continue;
            }
            extractor.selectTrack(i);
            muxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mVideoTrackIndex = muxer.addTrack(format);
            muxer.start();
            logout("start muxer: " + output);
        }

        if (muxer == null){
            logout("no video found !");
            return false;
        }

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        info.presentationTimeUs = 0;
        ByteBuffer buffer = ByteBuffer.allocate(1024*1024*25);

        while (true){
            int sampleSize = extractor.readSampleData(buffer,0);
            if (sampleSize < 0){
                logout("read sample data failed , break !");
                break;
            }
            info.offset = 0;
            info.size = sampleSize;
            info.flags = extractor.getSampleFlags();
            info.presentationTimeUs = extractor.getSampleTime();
            boolean keyframe = (info.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) > 0;
            logout("write sample " + keyframe + ", " + sampleSize + ", " + info.presentationTimeUs);
            muxer.writeSampleData(mVideoTrackIndex,buffer,info);
            extractor.advance();
        }
        extractor.release();

        muxer.stop();
        muxer.release();

        logout("process success !");

        return true;
    }

    private void logout(final String content){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,content);
                mLogView.setText(mLogView.getText() + "\n" + content);
            }
        });
    }
}
