package com.epnfis.mediaplayer

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit


private const val TAG_SONG = "TAG_SONG"

enum class SongSource {
    RAW, URL, MEDIA
}

class MainActivity : AppCompatActivity() {
    var mPlayer: MediaPlayer? = null
    private var isFirstPlay = true
    private var startSongTime: Int = 0
    private var endSongTime: Int = 0
    private var forwardTime: Int = 5000
    private var backwardTime: Int = 5000
    private var delayTime: Long = 500
    private var loopMode = false
    private var handler:Handler = Handler()
    private lateinit var songSource:SongSource
    private var songSelectedIndex = 0
    private var songsFromRaw = mutableListOf<Pair<String,Int>>()
    private var songsFromUrl : MutableList<Pair<String,String>> = ArrayList()
    private var songsFromMedia : MutableList<Pair<String,Uri>> = arrayListOf()
    private lateinit var textViewSongName:TextView
    private lateinit var textViewStartTime:TextView
    private lateinit var textViewSongTime:TextView
    private lateinit var listViewSongs:ListView
    private lateinit var seekBarSong: SeekBar
    private lateinit var buttonSkipPrevious: ImageButton
    private lateinit var buttonLoopMode: ImageButton
    private lateinit var buttonBack: ImageButton
    private lateinit var buttonPlay:ImageButton
    private lateinit var buttonPause:ImageButton
    private lateinit var buttonStop:ImageButton
    private lateinit var buttonForward:ImageButton
    private lateinit var buttonSkipNext:ImageButton
    private val PERMISSION_READ_EXTERNAL_MEMORY = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        variablesInitializations()

        seekBarSong.isClickable=false
//        buttonPlay.isEnabled = true
//        buttonPause.isEnabled = false
//        buttonStop.isEnabled = false
        initialListOfSong()
        songSource = SongSource.RAW
        initMediaPlayerFromRaw()
        initListView()
        setOnClickListeners()
    }
    private fun variablesInitializations(){
        textViewSongName = findViewById(R.id.song_name)
        textViewStartTime = findViewById(R.id.start_time)
        textViewSongTime = findViewById(R.id.song_time)
        listViewSongs = findViewById(R.id.listViewSongs)
        seekBarSong = findViewById(R.id.seekbar)
        buttonSkipPrevious = findViewById(R.id.buttonSkipPrevious)
        buttonLoopMode = findViewById(R.id.buttonLoopMode)
        buttonBack = findViewById(R.id.backButton)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonPause = findViewById(R.id.buttonPause)
        buttonStop = findViewById(R.id.buttonStop)
        buttonForward = findViewById(R.id.buttonForward)
        buttonSkipNext = findViewById(R.id.buttonSkipNext)
    }
    private fun setOnClickListeners(){
        buttonSkipPrevious.setOnClickListener {
            var songSelectedIndexOld = songSelectedIndex
            when (songSource) {
                SongSource.RAW -> {
                    if (songsFromRaw.size == 0) return@setOnClickListener
                    if(songsFromRaw.size == 1){
                        songSelectedIndex = 0
                    }
                    else{
                        if(songSelectedIndex == 0)
                            songSelectedIndex = songsFromRaw.lastIndex
                        else
                            songSelectedIndex -= 1
                    }
                    initMediaPlayerFromRaw()
                    //playSong()
                }
                SongSource.URL -> {
                    if (songsFromUrl.size == 0) return@setOnClickListener
                    if(songsFromUrl.size == 1){
                        songSelectedIndex = 0
                    }
                    else{
                        if(songSelectedIndex == 0)
                            songSelectedIndex = songsFromUrl.lastIndex
                        else
                            songSelectedIndex -= 1
                    }
                    initMediaPlayerFromUrl()
                    //playSong()
                }
                SongSource.MEDIA -> {
                    if (songsFromMedia.size == 0) return@setOnClickListener
                    if(songsFromMedia.size == 1){
                        songSelectedIndex = 0
                    }
                    else{
                        if(songSelectedIndex == 0)
                            songSelectedIndex = songsFromMedia.lastIndex
                        else
                            songSelectedIndex -= 1
                    }
                    initMediaPlayerFromMedia()
                    //playSong()
                }
            }
            updateListViewColors(songSelectedIndexOld, songSelectedIndex)
        }
        buttonSkipNext.setOnClickListener {
            var songSelectedIndexOld = songSelectedIndex
            when (songSource) {
                SongSource.RAW -> {
                    if (songsFromRaw.size == 0) return@setOnClickListener
                    if(songsFromRaw.size == 1){
                        songSelectedIndex = 0
                    }
                    else{
                        if(songSelectedIndex == songsFromRaw.lastIndex)
                            songSelectedIndex = 0
                        else
                            songSelectedIndex += 1
                    }
                    initMediaPlayerFromRaw()
                    //playSong()
                }
                SongSource.URL -> {
                    if (songsFromUrl.size == 0) return@setOnClickListener
                    if(songsFromUrl.size == 1){
                        songSelectedIndex = 0
                    }
                    else{
                        if(songSelectedIndex == songsFromUrl.lastIndex)
                            songSelectedIndex = 0
                        else
                            songSelectedIndex += 1
                    }
                    initMediaPlayerFromUrl()
                    //playSong()
                }
                SongSource.MEDIA -> {
                    if (songsFromMedia.size == 0) return@setOnClickListener
                    if(songsFromMedia.size == 1){
                        songSelectedIndex = 0
                    }
                    else{
                        if(songSelectedIndex == songsFromMedia.lastIndex)
                            songSelectedIndex = 0
                        else
                            songSelectedIndex += 1
                    }
                    initMediaPlayerFromMedia()
                    //playSong()
                }
            }
            updateListViewColors(songSelectedIndexOld, songSelectedIndex)

        }
        buttonLoopMode.setOnClickListener {
            if(loopMode){
                buttonLoopMode.setImageResource(R.drawable.ic_loop_off)
            }
            else{
                buttonLoopMode.setImageResource(R.drawable.ic_loop)
            }
            loopMode = !loopMode
            if(mPlayer != null)
                mPlayer!!.isLooping = loopMode
            Toast.makeText(applicationContext, "Loop Mode: ${if (loopMode) "On" else "Off"}", Toast.LENGTH_SHORT).show()
        }
        buttonPlay.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                playSong()
                Toast.makeText(applicationContext, "Playing...", Toast.LENGTH_SHORT).show()
            }
        })
        buttonPause.setOnClickListener {
                mPlayer!!.pause()
//                buttonPause.isEnabled = false
//                buttonStop.isEnabled = false
//                buttonPlay.isEnabled = true
                Toast.makeText(applicationContext, "Audio Paused", Toast.LENGTH_SHORT).show()
        }
        buttonStop.setOnClickListener {
            if(mPlayer!= null && mPlayer!!.isPlaying) {
                try {
                    mPlayer!!.seekTo(0)
                    mPlayer?.stop()
                    mPlayer?.prepare()
                } catch (e:Exception){
                    e.printStackTrace()
                }
                //mPlayer?.stop()
                //mPlayer?.reset()
                seekBarSong.setProgress(0);
                seekBarSong.invalidate();
            }
//            buttonPlay.isEnabled = true
//            buttonPause.isEnabled = false
//            buttonStop.isEnabled = false
            Toast.makeText(applicationContext, "Audio Stopped", Toast.LENGTH_SHORT).show()
        }
        buttonForward.setOnClickListener {
                if (startSongTime + forwardTime <= endSongTime) {
                    startSongTime += forwardTime
                    mPlayer!!.seekTo(startSongTime)
                } else {
                    Toast.makeText(applicationContext,"Cannot jump forward 5 seconds",Toast.LENGTH_SHORT).show()
                }
//                if (!buttonPlay.isEnabled) {
//                    buttonPlay.isEnabled = true
//                }
        }
        buttonBack.setOnClickListener {
                if (startSongTime - backwardTime > 0) {
                    startSongTime -= backwardTime
                    mPlayer!!.seekTo(startSongTime)
                } else {
                    Toast.makeText(applicationContext,"Cannot jump backward 5 seconds",Toast.LENGTH_SHORT).show()
                }
        }

        seekBarSong.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mPlayer!!.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?){}
            override fun onStopTrackingTouch(seekBar: SeekBar?){}
        })

        listViewSongs.setOnItemClickListener{ adapterView, view, position, _ ->
            //if(songSelectedIndex != position){
                //Restore the original background color of the item selected last time
                adapterView.getChildAt(songSelectedIndex).setBackgroundColor(Color.parseColor("#ffffff"));
                //adapterView.getChildAt(songSelectedIndex).setBackgroundColor(Color.RED);
                //The background color of the currently selected item becomes white
                view.setBackgroundColor(Color.parseColor("#ECECEC"));
                //view.setBackgroundColor(Color.BLUE);
                //Refresh the number of the selected item
                songSelectedIndex = position;
            //}
            //songSelectedIndex = position
            when (songSource) {
                SongSource.RAW -> {
                    initMediaPlayerFromRaw()
                    //playSong()
                }
                SongSource.URL -> {
                    initMediaPlayerFromUrl()
                    //playSong()
                }
                SongSource.MEDIA -> {
                    initMediaPlayerFromMedia()
                    //playSong()
                }
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_canciones,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_item_playFromRaw -> {
                Toast.makeText(applicationContext, "Menu Play From Internal App Song File", Toast.LENGTH_SHORT).show()
                songSelectedIndex = 0
                initMediaPlayerFromRaw()
                initListView()
                //playSong()
                return true
            }
            R.id.menu_item_playFromURL -> {
                Toast.makeText(applicationContext, "Menu Play From Internet", Toast.LENGTH_SHORT).show()
                songSelectedIndex = 0
                initMediaPlayerFromUrl()
                initListView()
                //playSong()
                return true
            }
            R.id.menu_item_playFromMedia -> {
                Toast.makeText(applicationContext, "Menu Play Internal Storage", Toast.LENGTH_SHORT).show()
                songSelectedIndex = 0
                initMediaPlayerFromMedia()
                initListView()
                //playSong()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateListViewColors(oldIndex:Int, newIndex:Int){
        val v1: View = listViewSongs.getChildAt(oldIndex)
        val txtview1 = v1.findViewById<View>(android.R.id.text1) as TextView
        txtview1.setBackgroundColor(Color.parseColor("#ffffff"));

        val v2: View = listViewSongs.getChildAt(newIndex)
        val txtview2 = v2.findViewById<View>(android.R.id.text1) as TextView
        txtview2.setBackgroundColor(Color.parseColor("#ECECEC"));
        /*
        for (i in 0 until listViewSongs.getChildCount()) {
            val v: View = listViewSongs.getChildAt(i)
            val txtview = v.findViewById<View>(android.R.id.text1) as TextView
            txtview.setTextColor(Color.RED)
        }*/
    }

    private fun initialListOfSong(){
        // initSongsFromRaw
        songsFromRaw.add(resources.getResourceEntryName(R.raw.bensound_buddy) to R.raw.bensound_buddy)
        songsFromRaw.add(resources.getResourceEntryName(R.raw.bensound_clearday) to R.raw.bensound_clearday)
        songsFromRaw.add(resources.getResourceEntryName(R.raw.bensound_sunny) to R.raw.bensound_sunny)
        // initSongsFromUrl
        //https://file-examples.com/wp-content/uploads/2017/11/file_example_MP3_700KB.mp3   //This mp3 bibrate 700Kb is not possible use with MediaPlayer
        //val urlSong = "https://www.learningcontainer.com/wp-content/uploads/2020/02/Kalimba.mp3"  //We have problems to get Duration Time of this track
        songsFromUrl.add("bensound-ukulele.mp3" to "https://www.bensound.com/bensound-music/bensound-ukulele.mp3")
        songsFromUrl.add("bensound-creativeminds.mp3" to "https://www.bensound.com/bensound-music/bensound-creativeminds.mp3")
        songsFromUrl.add("bensound-anewbeginning.mp3" to "https://www.bensound.com/bensound-music/bensound-anewbeginning.mp3")

        // initSongsFromMedia
        checkForPermissionToReadMedia()
        songsFromMedia.clear()
        songsFromMedia.addAll(getMp3FilesFromMedia())
    }

    private fun initListView(){
        when (songSource) {
            SongSource.RAW -> {
                var songNameList = songsFromRaw.map { it.first }
//                var songNameList = arrayListOf<String>()
//                for((song,_) in songsFromRaw){
//                    songNameList.add(song)
//                }
                var myAdapter = ArrayAdapter( this,android.R.layout.simple_list_item_1, songNameList)
                listViewSongs.adapter = myAdapter
            }
            SongSource.URL -> {
                var songNameList = songsFromUrl.map { it.first }
//                var songNameList = arrayListOf<String>()
//                for((song,_) in songsFromUrl){
//                    songNameList.add(song)
//                }
                var myAdapter = ArrayAdapter( this,android.R.layout.simple_list_item_1, songNameList)
                listViewSongs.adapter = myAdapter
            }
            SongSource.MEDIA -> {
                var songNameList = songsFromMedia.map { it.first }
//                var songNameList = arrayListOf<String>()
//                for((song,_) in songsFromMedia){
//                    songNameList.add(song)
//                }
                var myAdapter = ArrayAdapter( this,android.R.layout.simple_list_item_1, songNameList)
                listViewSongs.adapter = myAdapter
            }
        }
    }

    private fun initMediaPlayerFromRaw(){
        songSource = SongSource.RAW
        if (mPlayer != null) {
            mPlayer!!.release()
            mPlayer = null
        }
        //val mySongName = getString(R.raw.bensound_clearday.toInt()).substringAfterLast("/")
        //val mySongName = getString(songsFromRaw[songSelectedIndex].second).substringAfterLast("/")
        val mySongName = songsFromRaw[songSelectedIndex].first
        Log.d(TAG_SONG, mySongName)
        textViewSongName.text = mySongName
        mPlayer = MediaPlayer.create(this, songsFromRaw[songSelectedIndex].second)
        //mPlayer!!.isLooping = loopMode

//        buttonPlay.isEnabled = false
//        buttonPause.isEnabled = true
//        buttonStop.isEnabled = true
        isFirstPlay = true
        mPlayer!!.setOnPreparedListener {
            Toast.makeText(this@MainActivity, "setOnPreparedListener From Raw", Toast.LENGTH_SHORT).show()
        }
        mPlayer!!.setOnCompletionListener(object : MediaPlayer.OnCompletionListener {
            override fun onCompletion(mp: MediaPlayer?) {
                try {
                    //mPlayer!!.start()
                    //seekBarSong.progress = startSongTime
                    //mPlayer!!.isLooping = true
                    Toast.makeText(this@MainActivity, "setOnCompletionListener", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun initMediaPlayerFromUrl(){
        songSource = SongSource.URL
        if (mPlayer != null) {
            mPlayer!!.release()
            mPlayer = null
        }

        //val urlSong = "https://www.bensound.com/bensound-music/bensound-ukulele.mp3"
        val urlSong = songsFromUrl[songSelectedIndex].second

        val mySongName = songsFromUrl[songSelectedIndex].first //urlSong.substringAfterLast("/")
        textViewSongName.text = mySongName
        Log.d(TAG_SONG, mySongName)
        //mPlayer = MediaPlayer.create(applicationContext, Uri.parse(urlSong))
        mPlayer = MediaPlayer()
        mPlayer?.setDataSource(urlSong)
        //listViewSongs.isEnabled = false
        mPlayer?.prepareAsync()
        //mPlayer?.prepare()
        Toast.makeText(this@MainActivity, "Buffering From URL", Toast.LENGTH_SHORT).show()
        mPlayer?.setOnPreparedListener {
            //mp -> mp.start()
//            buttonPlay.isEnabled = false
//            buttonPause.isEnabled = true
//            buttonStop.isEnabled = true
            //listViewSongs.isEnabled = true
            Toast.makeText(this@MainActivity, "Ready to play From URL", Toast.LENGTH_SHORT).show()
            isFirstPlay = true
            //it.start();

            //Log.d("URI", Uri.parse(urlSong).toString())

//            val mRetriever = MediaMetadataRetriever()
//            //mRetriever.setDataSource(applicationContext, Uri.parse(urlSong))
//            mRetriever.setDataSource(urlSong, HashMap())
//            val s = mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

            endSongTime = it!!.duration.toLong().toInt()
            startSongTime = it!!.currentPosition.toLong().toInt()
            seekBarSong.max = endSongTime

            textViewSongTime.text = String.format(
                "%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(endSongTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(endSongTime.toLong()) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(endSongTime.toLong())))

            textViewStartTime.text = String.format(
                "%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(startSongTime.toLong()),
                TimeUnit.MILLISECONDS.toSeconds(startSongTime.toLong()) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(startSongTime.toLong())))

            seekBarSong.progress = mPlayer!!.currentPosition
            handler.postDelayed(updateSongTime, delayTime)

//            buttonPlay.isEnabled = false
//            buttonPause.isEnabled = true
//            buttonStop.isEnabled = true
        }
    }
    private fun initMediaPlayerFromMedia(){
        songSource = SongSource.MEDIA
        if (mPlayer != null) {
            mPlayer!!.release()
            mPlayer = null
        }
        songsFromMedia.clear()
        songsFromMedia.addAll(getMp3FilesFromMedia())

        if(songsFromMedia.size == 0){
            Toast.makeText(applicationContext, "First download songs to device", Toast.LENGTH_SHORT).show()
            return
        }


        val mySongName = songsFromMedia[songSelectedIndex].first
        Log.d(TAG_SONG, mySongName)
        textViewSongName.text = mySongName
        mPlayer = MediaPlayer.create(this, songsFromMedia[songSelectedIndex].second)

        isFirstPlay = true
        mPlayer!!.setOnPreparedListener {
            Toast.makeText(this@MainActivity, "setOnPreparedListener From Media", Toast.LENGTH_SHORT).show()
        }
        mPlayer!!.setOnCompletionListener {
            try {
                //mPlayer!!.start()
                //seekBarSong.progress = startSongTime
                //mPlayer!!.isLooping = true
                Toast.makeText(this@MainActivity, "setOnCompletionListener", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun playSong(){
        Toast.makeText(applicationContext, "Audio Playing", Toast.LENGTH_SHORT).show()
        if(mPlayer==null) return
        endSongTime = mPlayer!!.duration.toLong().toInt()
        startSongTime = mPlayer!!.currentPosition.toLong().toInt()
        if(! mPlayer!!.isPlaying ) {
            mPlayer!!.start()
        }
        if (isFirstPlay) {
            seekBarSong.max = endSongTime
            isFirstPlay = !isFirstPlay
        }

        textViewSongTime.text = String.format(
            "%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(endSongTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(endSongTime.toLong()) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(endSongTime.toLong())))

        textViewStartTime.text = String.format(
            "%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(startSongTime.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(startSongTime.toLong()) - TimeUnit.MINUTES.toSeconds(
                TimeUnit.MILLISECONDS.toMinutes(startSongTime.toLong())))

        seekBarSong.progress = mPlayer!!.currentPosition
        handler.postDelayed(updateSongTime, delayTime)
//        buttonPlay.isEnabled = false
//        buttonPause.isEnabled = true
//        buttonStop.isEnabled = true
    }

    private val updateSongTime: Runnable = object : Runnable {
        override fun run() {
            if(mPlayer!=null && mPlayer!!.isPlaying) {
                startSongTime = mPlayer!!.currentPosition
                textViewStartTime.text = String.format(
                    "%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(startSongTime.toLong()),
                    TimeUnit.MILLISECONDS.toSeconds(startSongTime.toLong()) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(startSongTime.toLong())
                    )
                )
                seekBarSong.progress = startSongTime
                handler.postDelayed(this, delayTime)
            }
            else{
                Toast.makeText(this@MainActivity, "Song playback ended", Toast.LENGTH_SHORT).show()
                //Log.d(TAG_SONG, "Song playback ended")
            }
        }
    }


    private fun checkForPermissionToReadMedia() {
        val permissionCheck =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        println("permissionCheck=$permissionCheck - PackageManager.PERMISSION_GRANTED=${PackageManager.PERMISSION_GRANTED}")
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_READ_EXTERNAL_MEMORY
            )
        }
    }

    private fun hasPermissionToReadMedia(permissionToCheck: String): Boolean {
        val permissionCheck = ContextCompat.checkSelfPermission(this, permissionToCheck)
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_READ_EXTERNAL_MEMORY -> if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                songsFromMedia.clear()
                songsFromMedia.addAll(getMp3FilesFromMedia())

            }
            else {
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    private fun getMp3FilesFromMedia(): ArrayList<Pair<String,Uri>> {
        val listOfAllMp3Files = ArrayList<Pair<String,Uri>>()
        if (hasPermissionToReadMedia(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            println("hasPermissionToReadMedia=true")

            val resolver = applicationContext.contentResolver
            // Find all audio files on the primary external storage device.
            // On API <= 28, use VOLUME_EXTERNAL instead.
            //val audioCollection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME
            )


            //val columns = arrayOf(MediaStore.Audio.Media._ID)
            val cursor: Cursor? = this.contentResolver
                .query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, //MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Audio.Media._ID
                )
            if (cursor != null) {
                val idColumn: Int = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn =    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id: Long = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    listOfAllMp3Files.add(name to contentUri)
                }
            }
        }
        else{
            println("hasPermissionToReadMedia=false")
        }
        return listOfAllMp3Files
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mPlayer!= null)
            mPlayer!!.release()
        mPlayer = null
    }
}