package com.gu.clientapp.mvp.task.decoder;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.example.basemodule.data.CodecParams;
import com.gu.clientapp.mvp.task.player.AudioPlayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AudioDecoderTask extends Thread {
  private static final String TAG = "AudioDecoderTask";
  // 用于播放解码后的pcm
  private AudioPlayer mPlayer;
  // 解码器
  private MediaCodec mDecoder;

  private volatile boolean stop;
  private volatile boolean started;

  private byte[] audioConfigData;
  private ArrayBlockingQueue<byte[]> dataQueue;

  public AudioDecoderTask(ArrayBlockingQueue<byte[]> dataQueue, byte[] audioConfigData) {
    this.audioConfigData = audioConfigData;
    this.dataQueue = dataQueue;
  }

  @Override
  public void run() {
    started = true;
    while (!stop) {
      try {
        byte[] data = dataQueue.poll(30, TimeUnit.MILLISECONDS);
        if (stop) break;
        if (data != null) {
          decode(data, 0, data.length);
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Log.e(TAG, TAG + " -------------------Thread finish!");
    release();
  }

  public void releaseThread() {
    stop = true;
  }

  public void stopThread() {
    if (mDecoder != null) {
      mDecoder.stop();
    }
  }

  public boolean isStarted() {
    return started;
  }

  public void startDecoder() {
    mDecoder.start();
  }
  /**
   * 初始化解码器
   *
   * @return 初始化失败返回false，成功返回true
   */
  public void prepare() {
    // 初始化AudioTrack
    mPlayer =
        new AudioPlayer(
            CodecParams.AUDIO_SAMPLE_RATE,
            CodecParams.AUDIO_CHANNEL,
            AudioFormat.ENCODING_PCM_16BIT);
    mPlayer.init();
    try {
      // 需要解码数据的类型
      String mine = "audio/mp4a-latm";
      // 初始化解码器
      mDecoder = MediaCodec.createDecoderByType(mine);
      // MediaFormat用于描述音视频数据的相关参数
      MediaFormat mediaFormat = new MediaFormat();
      // 数据类型
      mediaFormat.setString(MediaFormat.KEY_MIME, mine);
      // 声道个数
      mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CodecParams.AUDIO_CHANNEL);
      // 采样率
      mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, CodecParams.AUDIO_SAMPLE_RATE);
      // 比特率
      mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, CodecParams.AUDIO_BIT_RATE);
      // 用来标记AAC是否有adts头，1->有
      //      mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
      // 用来标记aac的类型
      mediaFormat.setInteger(
          MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
      // ByteBuffer key（暂时不了解该参数的含义，但必须设置）
      //      byte[] data = new byte[] {18, 16};
      ByteBuffer csd_0 = ByteBuffer.wrap(audioConfigData);
      mediaFormat.setByteBuffer("csd-0", csd_0);
      // 解码器配置
      mDecoder.configure(mediaFormat, null, null, 0);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (mDecoder != null) {
      mDecoder.start();
    }
  }

  /** aac解码+播放 */
  public void decode(byte[] buf, int offset, int length) {
    // 等待时间，0->不等待，-1->一直等待
    long kTimeOutUs = 0;
    try {
      // 输入ByteBuffer
      ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
      // 输出ByteBuffer
      ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();
      // 返回一个包含有效数据的input buffer的index,-1->不存在
      int inputBufIndex = mDecoder.dequeueInputBuffer(kTimeOutUs);
      if (inputBufIndex >= 0) {
        // 获取当前的ByteBuffer
        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
        // 清空ByteBuffer
        dstBuf.clear();
        // 填充数据
        dstBuf.put(buf, offset, length);
        // 将指定index的input buffer提交给解码器
        mDecoder.queueInputBuffer(inputBufIndex, 0, length, 0, 0);
      }
      // 编解码器缓冲区
      MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
      // 返回一个output buffer的index，-1->不存在
      int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);

      ByteBuffer outputBuffer;
      while (outputBufferIndex >= 0) {
        // 获取解码后的ByteBuffer
        outputBuffer = codecOutputBuffers[outputBufferIndex];
        // 用来保存解码后的数据
        byte[] outData = new byte[info.size];
        outputBuffer.get(outData);
        // 清空缓存
        outputBuffer.clear();
        // 播放解码后的数据
        mPlayer.playAudioTrack(outData, 0, info.size);
        // 释放已经解码的buffer
        mDecoder.releaseOutputBuffer(outputBufferIndex, false);
        // 解码未解完的数据
        outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);
      }
    } catch (Exception e) {
      Log.e(TAG, e.toString());
      e.printStackTrace();
    }
  }

  /** 释放资源 */
  public void release() {
    try {
      if (mPlayer != null) {
        mPlayer.release();
        mPlayer = null;
      }
      if (mDecoder != null) {
        mDecoder.stop();
        mDecoder.release();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
