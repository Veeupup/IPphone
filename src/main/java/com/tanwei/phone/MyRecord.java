package com.tanwei.phone;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;

public class MyRecord extends JFrame implements ActionListener {

    //客户端在9000端口监听接收到的数据
    DatagramSocket ds = new DatagramSocket(9000);
    InetAddress loc = InetAddress.getLocalHost();
    // 建立接收缓冲
    byte[] buf = new byte[1024];

    //定义录音格式
    AudioFormat af = null;
    //定义目标数据行,可以从中读取音频数据,该 TargetDataLine 接口提供从目标数据行的缓冲区读取所捕获数据的方法。
    TargetDataLine td = null;
    //定义源数据行,源数据行是可以写入数据的数据行。它充当其混频器的源。应用程序将音频字节写入源数据行，这样可处理字节缓冲并将它们传递给混频器。
    SourceDataLine sd = null;
    //定义字节数组输入输出流
    ByteArrayInputStream bais = null;
    ByteArrayOutputStream baos = null;
    //定义音频输入流
    AudioInputStream ais = null;
    //定义停止录音的标志，来控制录音线程的运行
    Boolean stopflag = false;


    //定义所需要的组件
    JPanel jp1,jp2,jp3;
    JLabel jl1=null;
    JButton captureBtn,stopBtn,playBtn,saveBtn;
    public static void main(String[] args) throws SocketException, UnknownHostException {

        //创造一个实例
        MyRecord mr = new MyRecord();

    }
    //构造函数
    public MyRecord() throws SocketException, UnknownHostException {
        //组件初始化
        jp1 = new JPanel();
        jp2 = new JPanel();
        jp3 = new JPanel();

        //定义字体
        Font myFont = new Font("华文新魏",Font.BOLD,30);
        jl1 = new JLabel("IP电话");
        jl1.setFont(myFont);
        jp1.add(jl1);

        captureBtn = new JButton("开始对话");
        //对开始录音按钮进行注册监听
        captureBtn.addActionListener(this);
        captureBtn.setActionCommand("captureBtn");
        //对停止录音进行注册监听
        stopBtn = new JButton("停止对话");
        stopBtn.addActionListener(this);
        stopBtn.setActionCommand("stopBtn");
        //对播放录音进行注册监听
        playBtn = new JButton("播放对话");
        playBtn.addActionListener(this);
        playBtn.setActionCommand("playBtn");
        //对保存录音进行注册监听
        saveBtn = new JButton("保存对话");
        saveBtn.addActionListener(this);
        saveBtn.setActionCommand("saveBtn");


        this.add(jp1,BorderLayout.NORTH);
        this.add(jp2,BorderLayout.CENTER);
        this.add(jp3,BorderLayout.SOUTH);
        jp3.setLayout(null);
        jp3.setLayout(new GridLayout(1, 4,10,10));
        jp3.add(captureBtn);
        jp3.add(stopBtn);
        jp3.add(playBtn);
        jp3.add(saveBtn);
        //设置按钮的属性
        captureBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        //设置窗口的属性
        this.setSize(400,300);
        this.setTitle("IP电话");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        // UDP

    }

    public void actionPerformed(ActionEvent e) {

        if(e.getActionCommand().equals("captureBtn"))
        {
            //点击开始录音按钮后的动作
            //停止按钮可以启动
            captureBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            playBtn.setEnabled(false);
            saveBtn.setEnabled(false);

            //调用录音的方法
            capture();
        }else if (e.getActionCommand().equals("stopBtn")) {
            //点击停止录音按钮的动作
            captureBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            saveBtn.setEnabled(true);
            //调用停止录音的方法
            stop();

        }else if(e.getActionCommand().equals("playBtn"))
        {
            //调用播放录音的方法
            play();
        }else if(e.getActionCommand().equals("saveBtn"))
        {
            //调用保存录音的方法
            save();
        }

    }
    //开始录音
    public void capture()
    {
        try {
            //af为AudioFormat也就是音频格式
            af = getAudioFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class,af);
            td = (TargetDataLine)(AudioSystem.getLine(info));
            //打开具有指定格式的行，这样可使行获得所有所需的系统资源并变得可操作。
            td.open(af);
            //允许某一数据行执行数据 I/O
            td.start();

            //创建播放录音的线程
            Record record = new Record();
            Thread t1 = new Thread(record);
            t1.start();

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }
    }
    //停止录音
    public void stop()
    {
        stopflag = true;
    }
    //播放录音
    public void play()
    {
        //将baos中的数据转换为字节数据
        byte audioData[] = baos.toByteArray();
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
            Play py = new Play();
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
                if(baos != null)
                {
                    baos.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    //保存录音
    public void save()
    {
        //取得录音输入流
        af = getAudioFormat();

        byte audioData[] = baos.toByteArray();
        bais = new ByteArrayInputStream(audioData);
        ais = new AudioInputStream(bais,af, audioData.length / af.getFrameSize());
        //定义最终保存的文件名
        File file = null;
        //写入文件
        try {
            //以当前的时间命名录音的名字
            //将录音的文件存放到F盘下语音文件夹下
            File filePath = new File("F:/语音文件");
            if(!filePath.exists())
            {//如果文件不存在，则创建该目录
                filePath.mkdir();
            }
            file = new File(filePath.getPath()+"/"+System.currentTimeMillis()+".mp3");
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            //关闭流
            try {

                if(bais != null)
                {
                    bais.close();
                }
                if(ais != null)
                {
                    ais.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
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
    //录音类，因为要用到MyRecord类中的变量，所以将其做成内部类
    // 此时，我们将捕捉到的音频字符数组通过 UDP 发送
    class Record implements Runnable
    {

        //定义存放录音的字节数组,作为缓冲区
        byte bts[] = new byte[10000];
        //将字节数组包装到流里，最终存入到baos中
        //重写run函数
        public void run() {
            // 定义发送的数据报
            DatagramPacket dp_send = new DatagramPacket(bts,10000, loc, 3000);
            DatagramPacket dp_receive = new DatagramPacket(buf, 1024);
            try {
                ds.setSoTimeout(5000);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            baos = new ByteArrayOutputStream();
            try {
                System.out.println("ok3");
                stopflag = false;
                int i=0;
                while(stopflag != true)
                {
                    i++;

                    //当停止录音没按下时，该线程一直执行
                    //从数据行的输入缓冲区读取音频数据。
                    //要读取bts.length长度的字节,cnt 是实际读取的字节数
                    int cnt = td.read(bts, 0, bts.length);
                    if(cnt > 0)
                    {
                        baos.write(bts, 0, cnt);
                        // 如果能获取到输入流，那么就通过UDP发送
                        ds.send(new DatagramPacket(bts,bts.length, loc, 3000));
                        if(i == 10) {
                            i=0;
                            ds.send(new DatagramPacket("Sending Audio!!!".getBytes(), "Sending Audio!!!".length(), loc, 3000));
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }finally{
                try {
                    //关闭打开的字节数组流
                    if(baos != null)
                    {
                        baos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }finally{
                    td.drain();
                    td.close();
                }
            }
        }

    }
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
}
