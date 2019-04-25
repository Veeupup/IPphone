package com.tanwei.phone;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class ClientMe extends JFrame implements ActionListener, KeyListener {

    static JTextArea jt_aArea=new JTextArea();
    JTextField jTextField=new JTextField(10);
    JButton btn_send=new JButton("发送");

    JTextField j_ip=new JTextField(10);
    JButton btn_ip=new JButton("对方IP");
    
    static PrintWriter pw=null;
    JPanel jp1=new JPanel();
    JPanel jp2=new JPanel();
    JScrollPane jsp=new JScrollPane(jt_aArea);
    public ClientMe () {
        Scanner in = new Scanner(System.in);
        String ip_addr = in.nextLine();
        in.close();

        // 发送消息
        jp1.add(jTextField);
        jp1.add(btn_send);

        // 发起连接
        jp2.add(j_ip);
        jp2.add(btn_ip);
        btn_ip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip = j_ip.getText();
                jt_aArea.append("点击按钮尝试连接"+ip+"\r\n");
                toConnect(ip);
            }
        });

        btn_send.addActionListener(this);
        btn_ip.addActionListener(this);
        jTextField.addKeyListener(this);
        this.add(jsp,"Center");
        this.add(jp1,"South");
        this.add(jp2, "North");

        this.setTitle("简易客户端");
        this.setSize(300, 400);
        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        toConnect(ip_addr);
    }

    public void keyTyped(KeyEvent e) {
        if(e.getSource()==jTextField){
            if(e.getKeyCode()==KeyEvent.VK_ENTER){
                String info=jTextField.getText();
                jt_aArea.append("客户端："+info+"\r\n");
                pw.println(info);
                //清空内存
                jTextField.setText("");
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
    }
    @Override
    public void keyPressed(KeyEvent e) {
        // TODO Auto-generated method stub
    }
    public void actionPerformed(ActionEvent arg) {
        //如果用户按下发送信息按钮
        if(arg.getSource()==btn_send){
            String info=jTextField.getText();
            jt_aArea.append("客户端："+info+"\r\n");
            pw.println(info);
        //清空内存
            jTextField.setText("");
        }
//        else if(arg.getSource()==btn_ip) {
//            String ip = j_ip.getText();
//            jt_aArea.append("尝试连接"+ip+"\r\n");
//            toConnect(ip);
//
//        }
    }
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        new ClientMe();
    }

    public static void toConnect(String ip_addr) {
        try {
            jt_aArea.append("尝试连接"+ip_addr+"\r\n");
            Socket s=new Socket(ip_addr,9999);
            InputStreamReader isr=new InputStreamReader(s.getInputStream());
            BufferedReader br=new BufferedReader(isr);
            pw=new PrintWriter(s.getOutputStream(),true);
            jt_aArea.append("连接成功"+ip_addr+"\r\n");
            InetAddress myip = InetAddress.getLocalHost();
            pw.println(myip+"向您发起连接\r\n");
            while(true){
                //不停的读取
                String info=br.readLine();
                jt_aArea.append("服务器端："+info+"\r\n");//换行
            }
        } catch (UnknownHostException e) {
            jt_aArea.append("IP输入有误！请检查后重新输入！"+"\r\n");
            System.out.println("IP输入有误！请检查后重新输入！");
//            System.exit(0);
        }
        catch (NoRouteToHostException e) {
            jt_aArea.append("IP输入不可达！请检查后重新输入！"+"\r\n");
            System.out.println("IP输入不可达！请检查后重新输入！");
//            System.exit(0);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


}
