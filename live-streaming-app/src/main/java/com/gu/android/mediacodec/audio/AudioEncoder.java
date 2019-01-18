package com.gu.android.mediacodec.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.example.basemodule.data.CodecParams;
import com.gu.rtplibrary.utils.ByteUtil;

import java.nio.ByteBuffer;

public class AudioEncoder {
  private MediaCodec mediaCodec;
  private MediaCodec.BufferInfo bufferInfo;
  private AudioEncoderDataCallback mCallback;
  private static final String TAG = "AudioEncoder";

  public interface AudioEncoderDataCallback {
    void onAudioEncoderDataReady(byte[] data);

    void onAudioConfigDataReady(byte[] configData);
  }

  public AudioEncoder(AudioEncoderDataCallback callback) {
    this.mCallback = callback;
    createEncoder();
  }

  private int createEncoder() {
    // 防止重复创建编码器
    if (mediaCodec != null) {
      return 0;
    }

    try {
      mediaCodec = MediaCodec.createEncoderByType(CodecParams.MIME_TYPE_AUDIO_AAC);
    } catch (Exception e) {
      e.printStackTrace();
      return -1;
    }

    // AAC 硬编码器
    MediaFormat format = new MediaFormat();
    format.setString(MediaFormat.KEY_MIME, CodecParams.MIME_TYPE_AUDIO_AAC);
    format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CodecParams.AUDIO_CHANNEL); // 声道数（这里是数字）
    format.setInteger(MediaFormat.KEY_SAMPLE_RATE, CodecParams.AUDIO_SAMPLE_RATE); // 采样率
    format.setInteger(MediaFormat.KEY_BIT_RATE, CodecParams.AUDIO_BIT_RATE); // 码率
    format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);

    bufferInfo = new MediaCodec.BufferInfo(); // 记录编码完成的buffer的信息
    mediaCodec.configure(
        format,
        null,
        null,
        MediaCodec.CONFIGURE_FLAG_ENCODE); // MediaCodec.CONFIGURE_FLAG_ENCODE 标识为编码器
    mediaCodec.start();
    return 0;
  }

  public int encode(byte[] pcmData, int offset, int size) {
    if (mediaCodec == null) {
      return -1;
    }

    // 把数据拷贝到byte数组中
    byte[] data = new byte[size];
    System.arraycopy(pcmData, offset, data, 0, size);

    ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
    ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
    ByteBuffer inputBuffer;
    //  <0一直等待可用的byteBuffer 索引;=0 马上返回索引 ;>0 等待相应的毫秒数返回索引
    int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1); // 一直等待（阻塞）
    if (inputBufferIndex >= 0) { // 拿到可用的buffer索引了
      inputBuffer = inputBuffers[inputBufferIndex];
      inputBuffer.clear();
      inputBuffer.put(data);
      mediaCodec.queueInputBuffer(inputBufferIndex, 0, size, 0, 0); // 投放到编码队列里去
    }

    // 获取已经编码成的buffer的索引  0表示马上获取 ，>0表示最多等待多少毫秒获取
    int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
    ByteBuffer outputBuffer;

    while (outputBufferIndex >= 0) {
      // ------------添加头信息--------------
      int outBitsSize = bufferInfo.size;
      byte[] outData = new byte[outBitsSize];

      outputBuffer = outputBuffers[outputBufferIndex];
      outputBuffer.position(bufferInfo.offset);
      outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
      if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
          == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
        byte[] config = new byte[bufferInfo.size];
        outputBuffer.get(config, 0, bufferInfo.size);
        if (mCallback != null) {
          mCallback.onAudioConfigDataReady(config);
        }
        ByteUtil.printByte(config);
      } else {
        outputBuffer.get(outData, 0, outBitsSize);
        if (mCallback != null) {
          mCallback.onAudioEncoderDataReady(outData);
        }
      }
      mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
      outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
    }

    return 0;
  }

  public void release() {
    if (mediaCodec != null) {
      mediaCodec.stop();
      mediaCodec.release();
    }
    mCallback = null;
  }
}
