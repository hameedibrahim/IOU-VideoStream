package com.iou.videostream

import com.google.android.exoplayer2.ExoPlayer
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations


class VideoActivityTest {

    @Mock
    private lateinit var mockPlayer: ExoPlayer

    private lateinit var myClass: VideoViewActivity

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        myClass = VideoViewActivity()
        myClass.player = mockPlayer
    }

    @Test
    fun testInitPlayer() {
        myClass.initPlayer()
        verify(mockPlayer).playWhenReady = true
        verify(mockPlayer).setMediaSource(any())
        verify(mockPlayer).seekTo(anyLong())
        verify(mockPlayer).playWhenReady = anyBoolean()
        verify(mockPlayer).prepare()
    }
}