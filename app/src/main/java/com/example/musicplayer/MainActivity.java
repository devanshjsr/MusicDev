package com.example.musicplayer;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.view.Menu;
import android.view.MenuInflater;
import android.webkit.PermissionRequest;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.os.IBinder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.single.PermissionListener;

public class MainActivity extends AppCompatActivity implements MediaPlayerControl {

    private ArrayList<Song> songList,newplay,recent;
    private Song arr[]= arr=new Song[10];
    private int c=0;
    private SearchView search;
    private ListView songView,x;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;
    private MusicController controller;
    private boolean paused=false, playbackPaused=false;


    DataBaseHelper myDB;

    ArrayAdapter<Song> language_adapter;

    ArrayList<Song> search_result_arraylist;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        songView = (ListView) findViewById(R.id.song_list);

        x= findViewById(R.id.song_list);
        songList = new ArrayList<Song>();
        newplay = new ArrayList<Song>();
        recent =new ArrayList<Song>();

        search_result_arraylist=new ArrayList<Song>();
        language_adapter=new ArrayAdapter<Song>(this,R.layout.support_simple_spinner_dropdown_item,songList);

         myDB=new DataBaseHelper(this);

        Collections.sort(songList, new Comparator<Song>(){
            public int compare(Song a, Song b){
                return a.getTitle().compareTo(b.getTitle());
            }
        });

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);
        runtimePermission();
        setController();

    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }


        public void runtimePermission(){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        getSongList();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {


                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }

                }).check();
    }


    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void getSongList() {
        //retrieve song info

        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null,null);


        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }while (musicCursor.moveToNext());
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }



    public void songPicked(View view){

        if(musicSrv!=null) {
            int pos = Integer.parseInt(view.getTag().toString());
            musicSrv.setSong(pos);
            musicSrv.playSong();

            if (playbackPaused) {
                setController();
                playbackPaused = false;
            }
        }else{
            Toast.makeText(MainActivity.this,"null reference",Toast.LENGTH_LONG).show();
        }
        controller.show(0);
    }

    public void add(View view)
    {
        if(musicSrv!=null) {
            int pos = Integer.parseInt(view.getTag().toString());

            Song entry = songList.get(pos);


            long id1 = entry.getID();
            String name1 = entry.getTitle();
            String singer1 = entry.getArtist();



            boolean isInserted = myDB.insertData((int) id1,name1,singer1);
            if(isInserted==true){
                Toast.makeText(MainActivity.this,"Song Added ",Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(MainActivity.this,"Something Went Wrong, Please try again",Toast.LENGTH_SHORT).show();
            }

        }
        else {
            Toast.makeText(MainActivity.this,"Error",Toast.LENGTH_LONG).show();
        }

    }

    public void remove(View view)
    {
        if(musicSrv!=null) {
            int pos = Integer.parseInt(view.getTag().toString());

            Cursor cursor2 = myDB.getAllData();
            if(cursor2.getCount()==0){
                Toast.makeText(MainActivity.this,"Empty Playlist",Toast.LENGTH_SHORT).show();
            }
            ArrayList<Song> playList2 = new ArrayList<>();

            while(cursor2.moveToNext()){
                playList2.add(new Song(cursor2.getInt(0),cursor2.getString(1),cursor2.getString(2)));
            }

            Song entry = playList2.get(pos);


            long id1 = entry.getID();
            String name1 = entry.getTitle();
            String singer1 = entry.getArtist();

            Integer deletedRow = myDB.deleteData(Integer.toString((int)id1));

            if(deletedRow>0){
                Toast.makeText(MainActivity.this,"Song Removed", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(MainActivity.this, "Try Again", Toast.LENGTH_SHORT).show();
            }

        }
        else {
            Toast.makeText(MainActivity.this,"Error",Toast.LENGTH_LONG).show();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                musicSrv.setShuffle();
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
            case R.id.create_playlist:
                Playlist list1=new Playlist(this,songList);
                songView.setAdapter(list1);
                break;
            case R.id.open_playlist:
                Cursor cursor = myDB.getAllData();
                if(cursor.getCount()==0){
                    Toast.makeText(MainActivity.this,"Empty Playlist",Toast.LENGTH_SHORT).show();
                }
                ArrayList<Song> playList = new ArrayList<>();

                while(cursor.moveToNext()){
                    playList.add(new Song(cursor.getInt(0),cursor.getString(1),cursor.getString(2)));
                }

                SongAdapter songAdt1 = new SongAdapter(this,playList);
                songView.setAdapter(songAdt1);
                musicSrv.setList(playList);
                break;
            case R.id.recent:
                SongAdapter songAdt3 = new SongAdapter(this,musicSrv.newlist());
                songView.setAdapter(songAdt3);
                musicSrv.setList(musicSrv.newlist());
                break;
            case R.id.app_bar_search:
                SearchView searchView = (SearchView) item.getActionView();
                searchView.setFocusable(false);
                searchView.setQueryHint("Search");
                Toast.makeText(MainActivity.this,"null reference",Toast.LENGTH_LONG).show();
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {



                    @Override
                    public boolean onQueryTextSubmit(String s) {

                        search_result_arraylist.clear();

                        Toast.makeText(MainActivity.this,"null reference",Toast.LENGTH_LONG).show();
                        for(int i =0 ;i < songList.size();i++){

                            Song sam=songList.get(i);
                           String name=sam.getTitle();
                           String singer=sam.getArtist();
                           if(name.equalsIgnoreCase(s)==true|| singer.equalsIgnoreCase(s)==true)
                            {

                                search_result_arraylist.add(sam);

                            }
                        }

                        language_adapter = new ArrayAdapter<Song>(MainActivity.this,android.R.layout.simple_list_item_1,search_result_arraylist);
                        songView.setAdapter(language_adapter);
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        return false;
                    }
                });
                break;
            case R.id.open_tracks:
                SongAdapter songAdt2 = new SongAdapter(this,songList);
                songView.setAdapter(songAdt2);
                musicSrv.setList(songList);
                break;
            case R.id.edit_playlist:
                Cursor cursor2 = myDB.getAllData();
                if(cursor2.getCount()==0){
                    Toast.makeText(MainActivity.this,"Empty Playlist",Toast.LENGTH_SHORT).show();
                }
                ArrayList<Song> playList2 = new ArrayList<>();

                while(cursor2.moveToNext()){
                    playList2.add(new Song(cursor2.getInt(0),cursor2.getString(1),cursor2.getString(2)));
                }

                EditPlaylist edit=new EditPlaylist(this,playList2);
                songView.setAdapter(edit);
                break;



        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    @Override
    public void start() {
      paused=false;
      musicSrv.go();
    }



    @Override
    public void pause() {
        playbackPaused=true;
        musicSrv.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
        return musicSrv.getDur();
        else return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicSrv!=null && musicBound && musicSrv.isPng())
        return musicSrv.getPosn();
        else return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicSrv.seek(pos);

    }

    @Override
    public boolean isPlaying() {
        if(musicSrv!=null && musicBound)
        return musicSrv.isPng();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    private void setController(){
        //set the controller up
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });

        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.blank));
        controller.setEnabled(true);
    }

    private void playNext(){
        musicSrv.playNext();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    private void playPrev(){
        musicSrv.playPrev();
        if(playbackPaused){
            setController();
            playbackPaused=false;
        }
        controller.show(0);
    }

    @Override
    protected void onPause(){
        super.onPause();
        paused=true;
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(paused){
            setController();
            paused=false;
        }
    }

    @Override
    protected void onStop() {
        controller.hide();
        super.onStop();
    }


    public ArrayList<Song> bridge(){
        ArrayList<Song> a= new ArrayList<Song>();
       // Toast.makeText(MainActivity.this,"ss",Toast.LENGTH_SHORT).show();
        return a;

    }


}

