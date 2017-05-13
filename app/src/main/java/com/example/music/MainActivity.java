package com.example.music;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    //播放顺序
    private final int MUSIC_SEQUENCE_PLAY = 1;
    private final int MUSIC_RANDOM_PLAY = 2;
    private final int MUSIC_LOOP_PLAY = 3;

    private MediaPlayer mediaPlayer;
    private ImageButton startBtn;
    private ImageButton playOrderBtn;
    private ImageButton clockBtn;
    private ListView musicListView;
    private TimingThread timingThread;
    private TimingHandler timingHandler;
    private ContentResolver contentResolver;

    //用于存放音乐
    private List<Music> musicList;

    private boolean painting = true;
    private boolean clicking = true;
    private boolean stopping = true;
    private boolean firstTime = true;
    private boolean timeArrive = true;

    //用于记录当前时间
    private long currentTime;
    //定时按钮点击次数计数器
    private int timingClickCount = 1;
    //当前音乐播放序号
    private int musicCount = 0;
    //播放顺序计数器
    private int playOrderCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View content;
        musicListView = (ListView) this.findViewById(R.id.listView1);
        startBtn = (ImageButton) this.findViewById(R.id.imageButton2);
        playOrderBtn = (ImageButton) this.findViewById(R.id.imageButton);
        clockBtn = (ImageButton) this.findViewById(R.id.imageButton3);
        //取消状态栏和标题栏，操作栏
        //cancelBar();
        //content内容
        content = getWindow().findViewById(Window.ID_ANDROID_CONTENT);
        //初始化定时处理
        timingHandler = new TimingHandler();
        //将音乐传入List
        musicList = getMusics();
        //初始化，并填入第一首歌曲
        mediaPlayer = MediaPlayer.create(this,musicList.get(musicCount).getMusicUri());
        //初始化视图观察者
        ViewTreeObserver vto = content.getViewTreeObserver();
        //设置适配器和监听
        musicListView.setAdapter(new MusicAdapter(this, R.layout.music_item, musicList));
        vto.addOnPreDrawListener(new ButtonPreDrawListener());
        startBtn.setOnTouchListener(new ButtonOnTouchListener());
        startBtn.setOnClickListener(new ButtonOnClickListener());
        playOrderBtn.setOnClickListener(new ButtonOnClickListener());
        clockBtn.setOnClickListener(new ButtonOnClickListener());
        musicListView.setOnItemClickListener(new ListViewListener());
        //初始化定时线程
        timingThread = new TimingThread();
    }

    class PlayedListener implements MediaPlayer.OnCompletionListener {
        @Override
        public void onCompletion(MediaPlayer mp) {
            switch (playOrderCount) {
                case MUSIC_SEQUENCE_PLAY:
                    sequencePlay();
                    break;
                case MUSIC_RANDOM_PLAY:
                    randomPlay();
                    break;
                case MUSIC_LOOP_PLAY:
                    loopPlay();
                    break;
                default:
                    break;
            }
        }

        //顺序播放
        private void sequencePlay() {
            musicCount++;
            if (musicCount == musicList.size()) {
                musicCount = 0;
            }
            update();
        }

        //随机播放
        private void randomPlay() {
            Random random = new Random();
            musicCount = random.nextInt(musicList.size());
            update();
        }

        //单曲播放
        private void loopPlay() {
            mediaPlayer.start();
        }

    }

    class ButtonPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
        @Override
        public boolean onPreDraw() {
            if (painting) {
                painting = false;
            }
            return true;
        }
    }

    class ButtonOnTouchListener implements View.OnTouchListener {
        private int downX;
        private int movedX;
        private int movedLeft;
        private int movedRight;
        private int left, right, top, bottom;
        //震动器
        private Vibrator vibrator;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    down(v, event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    move(v, event);
                    break;
                case MotionEvent.ACTION_UP:
                    up(v, event);
                    break;
                default:
                    break;
            }
            return false;
        }

        //按下按钮
        private void down(View v, MotionEvent event) {
            left = v.getLeft();
            right = v.getRight();
            top = v.getTop();
            bottom = v.getBottom();
            downX = (int) event.getRawX();
        }

        //移动按钮
        private void move(View v, MotionEvent event) {
            movedX = (int) event.getRawX() - downX;
            movedLeft = left + movedX;
            movedRight = right + movedX;
            boolean overBorder = Math.abs(movedX) > 100;
            if (!overBorder) {
                boolean toLeft = movedX < 0;
                play(v, toLeft);
            }
        }

        //松开按钮
        private void up(View v, MotionEvent event) {
            clicking = (Math.abs(downX - event.getRawX()) < 5);
            //首次
            firstTime = true;
            if (stopping) {
                startBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.start));
            } else {
                startBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.stop));
            }
            v.layout(left, top, right, bottom);
        }

        //播放音乐
        private void play(View v, boolean toLeft) {
            //proper合适
            boolean proper = Math.abs(movedX) > 70 && firstTime;
            v.layout(movedLeft, top, movedRight, bottom);
            if (toLeft) {
                startBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.last));
                if (proper) {
                    firstTime = false;
                    vibrator.vibrate(3);
                    playLast();
                }
            } else {
                startBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.next));
                if (proper) {
                    firstTime = false;
                    vibrator.vibrate(3);
                    playNext();
                }
            }
        }

        //播放下一曲
        private void playNext() {
            mediaPlayer.stop();
            musicCount++;
            if (musicCount == musicList.size()) {
                musicCount = 0;
            }
            update();
        }

        //播放上一曲
        private void playLast() {
            mediaPlayer.stop();
            musicCount--;
            if (musicCount == -1) {
                musicCount = musicList.size() - 1;
            }
            update();
        }
    }

    class ButtonOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            ImageButton imageButton = (ImageButton) v;
            int btnType = v.getId();
            switch (btnType) {
                case R.id.imageButton2:
                    playBtnClick(imageButton);
                    break;
                case R.id.imageButton:
                    playOrderBtnClick(imageButton);
                    break;
                case R.id.imageButton3:
                    timingClick(imageButton);
                    break;
                default:
                    break;
            }
        }

        //点击播放按钮
        private void playBtnClick(ImageButton imageButton) {
            if (clicking) {
                if (stopping) {         //stop
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.stop));
                    mediaPlayer.setOnCompletionListener(new PlayedListener());
                    mediaPlayer.start();
                    stopping = false;
                } else {                    //start
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.start));
                    mediaPlayer.pause();
                    stopping = true;
                }
            }
        }

        //点击播放顺序按钮
        private void playOrderBtnClick(ImageButton imageButton) {
            playOrderCount = playOrderCount % 3 + 1;
            switch (playOrderCount) {
                case MUSIC_SEQUENCE_PLAY:
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.sequence));
                    break;
                case MUSIC_RANDOM_PLAY:
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.random));
                    break;
                case MUSIC_LOOP_PLAY:
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.loop));
                    break;
                default:
                    break;
            }
        }

        //点击定时按钮
        private void timingClick(ImageButton imageButton) {
            timingClickCount = timingClickCount % 3 + 1;
            currentTime = System.currentTimeMillis();
            switch (timingClickCount) {
                case 1:
                    if (timingThread.isAlive()) {
                        timingThread.interrupt();
                    }
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.clock));
                    break;
                case 2:
                    if (!timingThread.isAlive()) {
                        timingThread.start();
                    }
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.clock_one));
                    break;
                case 3:
                    if (!timingThread.isAlive()) {
                        timingThread.start();
                    }
                    imageButton.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.clock_two));
                    break;
                default:
                    break;
            }

        }
    }

    class ListViewListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            musicCount = position;
            if (stopping) {
                startBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.stop));
            }
            mediaPlayer.stop();
            update();
        }
    }

    class MusicAdapter extends ArrayAdapter<Music> {

        private int resourceId;

        MusicAdapter(Context context, int textViewResourceId, List<Music> objects) {
            super(context, textViewResourceId, objects);
            resourceId = textViewResourceId;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Music music = getItem(position);
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            } else {
                view = convertView;
            }
            TextView musicName = (TextView) view.findViewById(R.id.textView6);
            TextView musicTime = (TextView)view.findViewById(R.id.textView5);
            TextView musicAuthor = (TextView) view.findViewById(R.id.textView7);
            musicTime.setText(music.getMusicDuration());
            musicAuthor.setText(music.getAuthor());
            musicName.setText(music.getName());
            return view;
        }

    }

    class TimingThread extends Thread {

        //一小时
        private final long TIMING_ONE_HOUR = 3600000;
        //两小时
        private final long TIMING_TWO_HOURS = 7200000;

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(100);
                    switch (timingClickCount) {
                        case 2:
                            timeArrive = (System.currentTimeMillis() >= currentTime + TIMING_ONE_HOUR);
                            break;
                        case 3:
                            timeArrive = (System.currentTimeMillis() >= currentTime + TIMING_TWO_HOURS);
                            break;
                        default:
                            break;
                    }
                    if (timeArrive) {
                        Message message = new Message();
                        message.arg1 = 1;
                        timingHandler.sendMessage(message);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class TimingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == 1) {
                mediaPlayer.pause();
                stopping = true;
                timingClickCount = 1;
                timeArrive = false;
                clockBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.clock));
                startBtn.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.start));
            }
        }
    }

    private List getMusics() {
        List<Music> music = new ArrayList<>();
        //实例化内容解析器
        contentResolver = this.getContentResolver();
        //指针指向解析器查询 cancellation signal取消信号
        //EXTERNAL_CONTENT_URI外部内容url
        //audio音频  media媒体
        //DEFAULT_SORT_ORDER默认的排列顺序
        //cursor 指针
        Cursor cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String musicTitle;
                String musicArtist;
                long musicSize;
                String musicURI;
                int musicId;
                long musicDuration;
                //getColumnIndexOrThrow得到列索引或抛出
                //album唱片集
                musicTitle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                musicArtist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                musicSize = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                musicURI = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                musicId = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                musicDuration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                boolean noTitle = "".equals(musicTitle);
                boolean noArtist = "".equals(musicArtist);
                boolean musicFile = musicSize > 1024 * 1024;
                boolean titleTooLong = musicTitle.length()>10;
                boolean noTime = musicDuration == 0;
                boolean illegal = noTitle || noArtist || titleTooLong || !musicFile || noTime;
                if (!illegal) {
                    music.add(new Music(musicTitle, musicArtist, R.drawable.music, musicURI, musicId, musicDuration));
                }
                cursor.moveToNext();
            }
        }
        return music;
    }

    private void cancelBar(){
        //获取操作杆
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
        //VERSION版本
        //CODES代码
        //LOLLIPOP棒棒糖 --！版本名
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //获得当前窗口
            Window window = this.getWindow();
            //Params参数
            //FLAG_TRANSLUCENT_STATUS标记半透明状态
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //Decor装饰
            //setSystemUiVisibility设置系统界面可见性
            //FULLSCREEN全屏
            //STABLE稳定
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void update(){
        mediaPlayer.reset();
        mediaPlayer = MediaPlayer.create(MainActivity.this, musicList.get(musicCount).getMusicUri());
        if (stopping) {
            stopping = false;
        }
        mediaPlayer.setOnCompletionListener(new PlayedListener());
        mediaPlayer.start();
    }

}