# AnalysisAndPackageMP4
MediaExtractor&amp;MediaMuxer解析和封装MP4文件。提取input.mp4文件中的视频数据，生成除去音频数据之后的纯视频output.mp4文件

一定要记得AndroidManifest.xml中添加权限，否则会报错“ java.io.IOException: Failed to instantiate extractor.”
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
如果是网络资源的话需要添加权限：<uses-permission android:name="android.permission.INTERNET"/>
