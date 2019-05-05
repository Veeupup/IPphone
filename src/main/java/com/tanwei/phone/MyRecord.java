/**
 * IP电话客户端
 *
 * 为了便于消息传递，共享内存的方式
 *
 * 采用内部类的方式实现
 *
 * 使用 Swing 窗口构建基础GUI面板
 *
 * 使用线程池 cachedThreadPool 来管理多线程的运行
 *
 * 五个内部类功能说明：
 *
 * 1. class Record 将 采集到的音频输入发送 UDP 包   （线程）
 *
 * 2. class Play 将接收到缓冲区的字节缓冲转换成音频流输出   （线程）
 *
 * 3. class Call 拨打电话，向对方发送 TCP 连接请求 （线程）
 *
 * 4. class Server 开启 9999 端口监听 TCP 连接 （线程）
 *
 * 5. class UDPServer 开启 3333 端口接受收到的 UDP 包 （线程）
 *
 */

package com.tanwei.phone;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MyRecord extends JFrame implements ActionListener {

    // 新建线程池来处理 拨号、连接、播放音频等线程
    ExecutorService cachedThreadPool = Executors.newCachedThreadPool();

    static JTextArea jt_aArea=new JTextArea();

    // 电话类定义
    MyRecord mr;

    // TCP连接的输出流
    static PrintWriter pw=null;

    // 是否接听电话
    volatile int onPhone = -1;

    // 是否成功建立 TCP 连接的标志
    volatile static boolean is_Connected = false;

    //客户端在9000端口监听接收到的数据，这是UDP发送的端口
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
    JPanel jp1,jp3;
    JScrollPane jp2;
    JLabel jl1=null;
    JButton captureBtn,stopBtn,playBtn,saveBtn, tcp_btn;
    JTextField jtextIp;

    //构造函数
    public MyRecord() throws SocketException, UnknownHostException {

        //组件初始化
        jp1 = new JPanel();
        jp2 = new JScrollPane(jt_aArea);
        jp3 = new JPanel();


        //定义字体
        Font myFont = new Font("华文新魏",Font.BOLD,30);
        jl1 = new JLabel("IP电话");
        jl1.setFont(myFont);
        jp1.add(jl1);

        // 定义输入 IP 地址，尝试用 TCP 连接
        jtextIp = new JTextField(10);
        // 拨号按钮
        tcp_btn = new JButton("拨号连接");
        tcp_btn.addActionListener(this);
        tcp_btn.setActionCommand("tcp_btn");

        jp1.add(jtextIp);
        jp1.add(tcp_btn);


        jt_aArea.setEditable(false);
        // 下方按钮
        captureBtn = new JButton("开始通话");
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
        captureBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        playBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        //设置窗口的属性
        this.setSize(400,300);
        this.setTitle("IP电话");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);

        // 开始监听自己的 9999 端口，等待其他用户连接
        Server server = new Server();
        // 将自己本机连接的线程放到线程池中运行
        cachedThreadPool.execute(server);

        // UDP接收端口
        UDPServer udpServer = new UDPServer();
        cachedThreadPool.execute(udpServer);
    }

    public static void main(String[] args) throws SocketException, UnknownHostException {

        //创造一个客户端实例
        MyRecord mr = new MyRecord();

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

            // 获取对方的对方的 IP 并且向这个IP发送音频流数据
            String ip = jtextIp.getText();
            try {
                InetAddress to_ip = InetAddress.getByName(ip);
                //调用录音的方法
                capture(to_ip);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }

        }else if (e.getActionCommand().equals("stopBtn")) {
            //点击停止录音按钮的动作
            captureBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            playBtn.setEnabled(true);
            saveBtn.setEnabled(true);
            //调用停止录音的方法，停止向对方发送数据
            stop();

        }else if(e.getActionCommand().equals("playBtn"))
        {
            //调用播放录音的方法
            play();
        }else if(e.getActionCommand().equals("saveBtn"))
        {
            //调用保存录音的方法
            save();
        }else if(e.getActionCommand().equals("tcp_btn"))
        {
            // 对输入的IP拨号
            String ip = jtextIp.getText();
            jt_aArea.append("尝试连接"+ip+"\r\n");
            // 在线程池中开启线程来拨号
            Call call = new Call(ip);
            cachedThreadPool.execute(call);
        }

    }
    //开始通话，捕捉 麦克风 获取音频流的数据
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
            cachedThreadPool.execute(record);

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
            return;
        }
    }

    //开始通话，捕捉 麦克风 获取音频流的数据
    public void capture(InetAddress ip)
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

            //创建发送音频流的线程
            Record record = new Record(ip);
            cachedThreadPool.execute(record);

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

    //播放录音,这是本机的录音，保存到缓存中
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
            cachedThreadPool.execute(py);
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

    // 重载play方法，这是播放从UDP报文中获取到的报文
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
            Play py = new Play();
            cachedThreadPool.execute(py);
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

    // 保存自己的录音（测试用）
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
            File filePath = new File("./audio");
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
        InetAddress ip;
        Record() {}

        Record(InetAddress ip) {
            this.ip = ip;
        }

        //定义存放录音的字节数组,作为缓冲区
        byte bts[] = new byte[10000];
        //将字节数组包装到流里，最终存入到baos中
        //重写run函数
        public void run() {
            System.out.println("开始录音");
//            System.out.println(loc);
            // 定义发送的数据报
            DatagramPacket dp_send = new DatagramPacket(bts,1000, ip, 3000);
            DatagramPacket dp_receive = new DatagramPacket(buf, 1024);

            baos = new ByteArrayOutputStream();
            try {
                System.out.println("ok3");
                stopflag = false;
                int i=0;
                while(stopflag != true)
                {
                    i++;
                    //  当停止通话没按下时，该线程一直执行
                    //  从数据行的输入缓冲区读取音频数据到
                    //  要读取bts.length长度的字节,cnt 是实际读取的字节数
                    //  将 td 输入音频流中缓冲区的内容读取到 bts 缓存中
                    int cnt = td.read(bts, 0, bts.length);
                    if(cnt > 0)
                    {
                        // 从数据线的输入缓冲区读取音频数据到 输出流 baos 中
                        // 下面这一句是为了重放录音的时候，将从 td 中捕捉到的字符流保存到 baos 中
                        baos.write(bts, 0, cnt);
                        // 将 bts 数组，也就是从 td 中读取到的音频流缓冲通过 UDP 发送
                        // 这个时候收到的内容应该是一个 byte[] 数组
                        // 至此，发送音频流应该没问题了
//                        System.out.println("本地捕获到的字符流为：");
//                        System.out.println(Arrays.toString(bts));
                        ds.send(new DatagramPacket( bts, cnt, ip, 3000));
//                        if(i == 10) {
//                            i=0;
//                            ds.send(new DatagramPacket("Sending Audio!!!".getBytes(), "Sending Audio!!!".length(), ip, 3000));
//                        }
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
        //播放baos中的数据即可,我们这个时候将所有的数据都保存到了 baos 中了
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

    //  将本机作为服务端，开启 9999 端口等待连接
    //  接听其他人拨打的电话
    class Server implements Runnable
    {
        // 默认开启 9999 端口
        private int port = 9999;
        Server() {}
        // 可以指定端口
        Server(int port) {
            this.port = port;
        }
        public void run() {
            try {
                // 开启本机 9999 端口等待其他客户端连接
                ServerSocket ss = new ServerSocket(port);
                jt_aArea.append("本机服务端启动……"+"端口为"+port+"\r\n");

                while (!is_Connected) {
                    // 这个方法将会阻塞，一直等待其他的用户连接，然后才会继续进行
                    Socket s =  ss.accept();
                    synchronized (Server.class) {
                        String other_ip = s.getRemoteSocketAddress().toString().split(":")[0].substring(1);
                        InetAddress ip = InetAddress.getByName(other_ip);
                        System.out.println("对方的IP是"+other_ip+"\r\n");
                        int is_connect = JOptionPane.showConfirmDialog(mr,"IP为"+ip+"的用户呼叫您","是否接听电话",0);
                        if(is_connect == 0) {
                            // 确定接听
                            // 修改接听变量
                            is_Connected = true;

                            System.out.println("这个时候选择接听对方电话");
                            jt_aArea.append("已接听"+ip+"的用户电话\r\n");
                            // 这个时候获取本地的音频,向对方发送消息
                            capture(ip);
//                       InputStreamReader isr=new InputStreamReader(s.getInputStream());
//                       BufferedReader br=new BufferedReader(isr);
//                       pw=new PrintWriter(s.getOutputStream(),true);
                        }else {
                            // 拒绝对方电话
                            System.out.println("挂断对方电话");
                            jt_aArea.append("已拒绝"+ip+"的用户电话\r\n");
                            // 向对方发送 goodbye 让对方直到被拒绝
                            OutputStream out = s.getOutputStream();
                            out.write("goodbye".getBytes());
                            out.flush();
                            s.shutdownInput();
                            s.shutdownOutput();
                        }
                    }
                    // 当本次通话结束之后，又重新监听
//                    is_Connected = false;
                    System.out.println("结束一次");
                }


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    // TCP拨号连接线程,这是作为拨号端,新建线程进行拨号
    // TCP 拨号，单独用一个线程来拨号
    class Call implements Runnable
    {
        private String ip;
        Call(String ip) {
            this.ip = ip;
        }
        // 拨号
        public void run() {
            try {
                jt_aArea.append("尝试连接"+ip+"\r\n");
                // 默认连接对方的 9999 端口,这是规定的每个人的TCP端口
                // 只有连接成功，才能进行下一步的 UDP 传送数据
                Socket s=new Socket(ip,9999);
                InputStreamReader isr=new InputStreamReader(s.getInputStream());
                BufferedReader br=new BufferedReader(isr);
                pw=new PrintWriter(s.getOutputStream(),true);
                jt_aArea.append("连接成功"+ip+"\r\n");
                InetAddress myip = InetAddress.getLocalHost();
                pw.println(myip+"向您发起连接\r\n");

                // TCP 连接成功后，才能开始通话,这里是
                is_Connected = true;
                captureBtn.setEnabled(true);
                // TCP 服务端读取TCP发送的数据
//                while(true){
//                    //不停的读取
//                    String info=br.readLine();
//                    jt_aArea.append("服务器端："+info+"\r\n");//换行
//                }
            } catch (UnknownHostException e) {
                jt_aArea.append("IP输入有误！请检查后重新输入！"+"\r\n");
                System.out.println("IP输入有误！请检查后重新输入！");
            }
            catch (NoRouteToHostException e) {
                jt_aArea.append("IP输入不可达！请检查后重新输入！"+"\r\n");
                System.out.println("IP输入不可达！请检查后重新输入！");
            }
            catch (Exception e) {
                e.printStackTrace();
                // 出现异常，则断开连接标志
                is_Connected = false;
                captureBtn.setEnabled(false);
            }
        }
    }


    // UDP 接收服务
    // 现在已经能向某个用户连接并且发送音频了
    class UDPServer implements Runnable {

        // 当收到连接之后，开启此线程，然后从这里获取 UDP 发送的报文
        // 同时，从自己本机捕捉音频，将音频输出流转换为字符数组然后通过 UDP 发送
        public void run() {
            String str_send = "Hello UDPclient";
            byte[] buf = new byte[10000];
            //服务端在3000端口监听接收到的数据
            // 客户端接收UDP的端口
            DatagramSocket ds_receive = null;
            try {
                ds_receive = new DatagramSocket(3000);
            } catch (SocketException e) {
                e.printStackTrace();
            }

            //接收从客户端发送过来的数据
            DatagramPacket dp_receive = new DatagramPacket(buf, 10000);
            System.out.println("server is on，waiting for client to send data......");
            boolean f = true;
            int i=0;
            while(f){
                try{
                    //服务器端接收来自客户端的数据
                    ds_receive.receive(dp_receive);
                    // 这里是接听之后，才选择接听消息，不然就一直循环在这里，用volatile变量来同步
                    while (!is_Connected) {}
                    System.out.println("第："+i++ + "次收到UDP数据报，说明确实是按照缓冲来发送的，以下是接收到的字符流");
//            String str_receive = dp_receive.getData();
                    byte[] byte_receive = dp_receive.getData();
                    // 打印一下接收到的字节数组，看和发送的UDP数据报二者是否相同
//                    System.out.println(Arrays.toString( dp_receive.getData()));

                    // 现在这样做有一点问题，就是每次接收到数据的长度不一样，
                    // 然后每次都要开启一个新的线程来播放，等下得改
                    play(byte_receive);
                    //数据发动到连接端的9000端口
                    DatagramPacket dp_send= new DatagramPacket(str_send.getBytes(),str_send.length(),dp_receive.getAddress(),9000);
                    try {
                        ds_receive.send(dp_send);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //由于dp_receive在接收了数据之后，其内部消息长度值会变为实际接收的消息的字节数，
                    //所以这里要将dp_receive的内部消息长度重新置为10000
                    dp_receive.setLength(10000);
                }catch (IOException e) {
                    e.printStackTrace();
                }

            }
            ds.close();
        }

    }

}
