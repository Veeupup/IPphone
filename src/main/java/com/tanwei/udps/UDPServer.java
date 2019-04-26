package com.tanwei.udps;

import com.tanwei.phone.MyRecord;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * 这是通信后，能够收到语音消息的UDP类
 * 同时在这里可以发送我们的语音消息
 * 被动接收UDP数据
 * 监听3000端口
 */
public class UDPServer {


    public static void main(String[] args)throws IOException{
        UDPServer udpServer = new UDPServer();


        String str_send = "Hello UDPclient";
        byte[] buf = new byte[10000];
        //服务端在3000端口监听接收到的数据
        DatagramSocket ds = new DatagramSocket(3000);
        //接收从客户端发送过来的数据
        DatagramPacket dp_receive = new DatagramPacket(buf, 10000);
        System.out.println("server is on，waiting for client to send data......");
        boolean f = true;
        int i=0;
        // 服务端可以一直处于接收状态，直到某个条件触发，这个先留着后面写
        while(f){
            //服务器端接收来自客户端的数据
            ds.receive(dp_receive);
            System.out.println("第："+i++ + "次收到UDP数据报，说明确实是按照缓冲来发送的");
//            String str_receive = dp_receive.getData();
            byte[] byte_receive = dp_receive.getData();
            // 打印一下接收到的字节数组，看和发送的UDP数据报二者是否相同
            System.out.println(Arrays.toString( dp_receive.getData()));

            // 现在这样做有一点问题，就是每次接收到数据的长度不一样，
            // 然后每次都要开启一个新的线程来播放，等下得改
            udpServer.play(byte_receive);
            //数据发动到连接端的9000端口
            DatagramPacket dp_send= new DatagramPacket(str_send.getBytes(),str_send.length(),dp_receive.getAddress(),9000);
            ds.send(dp_send);
            //由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
            //所以这里要将dp_receive的内部消息长度重新置为10000
            dp_receive.setLength(10000);
        }
        ds.close();
    }


    //定义录音格式
    AudioFormat af = null;
    //定义字节数组输入输出流
    ByteArrayInputStream bais = null;
    //播放录音
    public void play(byte[] audioData)
    {
        //  将接收到的 byte[] 转换为输入流，然后才能去播放
        //  byte audioData[] = baos.toByteArray();
        //转换为输入流
        bais = new ByteArrayInputStream(audioData);
        af = getAudioFormat();
        ais = new AudioInputStream(bais, af, audioData.length/af.getFrameSize());

        try {
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, af);
            sd = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sd.open(af);
            sd.start();
            //创建播放进程
            UDPServer.Play py = new UDPServer.Play();
            Thread t2 = new Thread(py);
            t2.start();
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            try {
                //关闭流
                if(ais != null)
                {
                    ais.close();
                }
                if(bais != null)
                {
                    bais.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //定义源数据行,源数据行是可以写入数据的数据行。它充当其混频器的源。应用程序将音频字节写入源数据行，这样可处理字节缓冲并将它们传递给混频器。
    SourceDataLine sd = null;
    //定义音频输入流
    AudioInputStream ais = null;
    //播放类,同样也做成内部类
    class Play implements Runnable
    {
        //播放baos中的数据即可
        public void run() {
            byte bts[] = new byte[10000];
            try {
                int cnt;
                //读取数据到缓存数据
                while ((cnt = ais.read(bts, 0, bts.length)) != -1)
                {
                    if (cnt > 0)
                    {
                        //写入缓存数据
                        //将音频数据写入到混频器
                        sd.write(bts, 0, cnt);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }finally{
                sd.drain();
                sd.close();
            }


        }
    }

    //设置AudioFormat的参数
    public AudioFormat getAudioFormat()
    {
        //下面注释部分是另外一种音频格式，两者都可以
        AudioFormat.Encoding encoding = AudioFormat.Encoding.
                PCM_SIGNED ;
        float rate = 8000f;
        int sampleSize = 16;
        String signedString = "signed";
        boolean bigEndian = true;
        int channels = 1;
        return new AudioFormat(encoding, rate, sampleSize, channels,
                (sampleSize / 8) * channels, rate, bigEndian);
//		//采样率是每秒播放和录制的样本数
//		float sampleRate = 16000.0F;
//		// 采样率8000,11025,16000,22050,44100
//		//sampleSizeInBits表示每个具有此格式的声音样本中的位数
//		int sampleSizeInBits = 16;
//		// 8,16
//		int channels = 1;
//		// 单声道为1，立体声为2
//		boolean signed = true;
//		// true,false
//		boolean bigEndian = true;
//		// true,false
//		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,bigEndian);
    }

}
