package com.iou.videostream


import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Rational
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.util.Util


class VideoViewActivity : AppCompatActivity(), Player.Listener{

    public var player: ExoPlayer? = null
    private var playbackPosition = 0L
    private var playWhenReady = true
    private val SAMPLE_URL =
        "https://dc5jkysis23ep.cloudfront.net/streams/e9a35b6f9d0bc631f4afd283ff606cbc-6/playlist.m3u8"
    public lateinit var styledPlayerView : StyledPlayerView
    private lateinit var ivPausePlay  : ImageView
    private lateinit var ivMuteUnMute : ImageView
    private lateinit var ivFullScreenMode : ImageView
    private lateinit var ivPlayBackMenu : ImageView
    private var isFullScreenMode: Boolean = false
    val speeds = arrayOf(0.25f, 0.5f, 0.75, 1f, 1.25, 1.5f, 2f)
    private var isPlaying = false
    private var isPictureMode = false


    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videoview)
        styledPlayerView = findViewById(R.id.player_exo)
        ivPausePlay  = findViewById(R.id.ivPausePlay)
        ivMuteUnMute = findViewById(R.id.ivVolume)
        ivFullScreenMode = findViewById(R.id.ivFullScreenMode)
        ivPlayBackMenu = findViewById(R.id.ivMenu)
        clickListener()
        checkForNetwork()
    }




    private fun clickListener(){
        ivPausePlay.setOnClickListener{
            if (player?.isPlaying == true){
                player!!.pause()
                ivPausePlay.setImageResource(R.drawable.ic_play_arrow)
            }else{
                player!!.play()
                ivPausePlay.setImageResource(R.drawable.ic_pause_arrow)

            }
        }

        ivMuteUnMute.setOnClickListener{
            val currentVolume = player?.volume
            if (currentVolume == 0f) {
                player?.volume = 1f
                ivMuteUnMute.setImageResource(R.drawable.ic_volume_up)
            } else {
                player?.volume = 0f
                ivMuteUnMute.setImageResource(R.drawable.ic_volume_off)
            }
        }

        ivFullScreenMode.setOnClickListener {
            if (isFullScreenMode){
                exitFullScreenMode()
            }else{
                enterFullScreenMode()
            }
        }

        ivPlayBackMenu.setOnClickListener {
            showTrackSelectionDialog()
        }
    }



    @Suppress("DEPRECATION")
    private fun enterFullScreenMode(){
        ivFullScreenMode.setImageResource(R.drawable.ic_fullscreen_exit)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val params = styledPlayerView.getLayoutParams()
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = ViewGroup.LayoutParams.MATCH_PARENT
        styledPlayerView.layoutParams = params
        isFullScreenMode = true
    }

    @Suppress("DEPRECATION")
    private fun exitFullScreenMode(){
        ivFullScreenMode.setImageResource(R.drawable.ic_fullscreen)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        if (supportActionBar != null) {
            supportActionBar!!.show()
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val params = styledPlayerView.getLayoutParams()
        params.width = ViewGroup.LayoutParams.MATCH_PARENT
        params.height = (200 * applicationContext.resources.displayMetrics.density).toInt()
        styledPlayerView.layoutParams = params
        isFullScreenMode = false
    }




    public fun initPlayer() {
        player = ExoPlayer.Builder(this).build()
        player?.playWhenReady = true
        styledPlayerView.player = player
        val defaultHttpDataSourceFactory = DefaultHttpDataSource.Factory()
        val mediaItem = MediaItem.fromUri(SAMPLE_URL)
        val mediaSource =
            HlsMediaSource.Factory(defaultHttpDataSourceFactory).createMediaSource(mediaItem)
        player?.setMediaSource(mediaSource)
        player?.seekTo(playbackPosition)
        player?.playWhenReady = playWhenReady
        player?.prepare()

    }

    private fun releasePlayer() {
        player?.let {
            playbackPosition = it.currentPosition
            playWhenReady = it.playWhenReady
            it.release()
            player = null
        }
    }




    private fun playbackSpeed(){

        val playbackSpeeds = arrayOf("0.25x", "0.5x", "0.75x", "Normal", "1.25x", "1.5x", "2x")
        val currentPlaybackSpeed = styledPlayerView.player?.playbackParameters?.speed ?: 1f
        val dialogBuilder = AlertDialog.Builder(this)
            .setTitle("Playback Speed")
            .setSingleChoiceItems(playbackSpeeds, speeds.indexOf(currentPlaybackSpeed)) { dialog, which ->
                val selectedSpeed = speeds[which]
                styledPlayerView.player?.playbackParameters = PlaybackParameters(selectedSpeed as Float)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        dialogBuilder.show()
    }



    private fun showTrackSelectionDialog() {
        val popupMenu = PopupMenu(this@VideoViewActivity, ivPlayBackMenu, Gravity.TOP)
        popupMenu.menuInflater.inflate(R.menu.exo_menu, popupMenu.getMenu())
        popupMenu.setOnMenuItemClickListener { menuItem -> // Toast message on menu item clicked
            if (menuItem.title?.equals("PIP")!!){
              // enterPIPMode()
                enterPiPMode()
            }else{
                playbackSpeed()
            }
            true
        }
        popupMenu.show()
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initPlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24) {
            initPlayer()
        }
    }
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }


    private fun checkForNetwork(){
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        val connectivityManager = getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        connectivityManager.requestNetwork(networkRequest, networkCallback)
    }


    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Toast.makeText(this@VideoViewActivity, "Back to online:)", Toast.LENGTH_LONG).show()
        }
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
        }
        override fun onLost(network: Network) {
            super.onLost(network)
            Toast.makeText(this@VideoViewActivity, "Connection Lost!!!", Toast.LENGTH_LONG).show()

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("position", playbackPosition)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        playbackPosition = savedInstanceState.getLong("position",0)
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (player!!.playWhenReady) {
                    isPlaying = true
                    playbackPosition = player!!.currentPosition
                    player!!.playWhenReady = false
                }
                val aspectRatio = Rational(styledPlayerView.getWidth(), styledPlayerView.getHeight())
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
                enterPictureInPictureMode(params)
            }
        }
    }

    override fun onUserLeaveHint() {
        if (!isPictureMode) {
            enterPiPMode()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig!!)
        isPictureMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) {
            // Hide any UI controls that are not needed in PiP mode

        } else {
            // Restore the UI controls when exiting PiP mode
        }
    }

}