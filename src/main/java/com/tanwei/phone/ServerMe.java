package com.tanwei.phone;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMe extends JFrame implements ActionListener, KeyListener {
    PrintWriter pw=null;
    JTextArea jt_aArea=new JTextArea();
    JTextField jTextField=new JTextField(10);
    JButton btn_send=new JButton("发送");
    JPanel jp1=new JPanel();
    JScrollPane jsp=new JScrollPane(jt_aArea);
    public ServerMe () {
        jp1.add(jTextField);
        jp1.add(btn_send);
        btn_send.addActionListener(this);
        jTextField.addKeyListener(this);
        this.add(jsp,"Center");
        this.add(jp1,"South");
        this.setTitle("简易服务器端");
        this.setSize(300, 300);
        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        try {
        //服务器监听
            ServerSocket ss=new ServerSocket(9999);
            jt_aArea.append("本机服务端启动……"+"\r\n");
        //等待客户端连接
            Socket s=ss.accept();
            InputStreamReader isr=new InputStreamReader(s.getInputStream());
            BufferedReader br=new BufferedReader(isr);
            pw=new PrintWriter(s.getOutputStream(),true);
        //读取从客户端发送来的信息
            while(true){
                String info =br.readLine();
                jt_aArea.append("客户端："+info+"\r\n");
            }
        } catch (Exception e) {
            System.out.println("连接失败");
        }
    }
    @Override
    public void keyTyped(KeyEvent e) {
// TODO Auto-generated method stub
        if(e.getSource()==jTextField){
            if(e.getKeyCode()==KeyEvent.VK_ENTER){
                String info=jTextField.getText();
                jt_aArea.append("服务端："+info+"\r\n");
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
            jt_aArea.append("服务器："+info+"\r\n");
            pw.println(info);
//清空内存
            jTextField.setText("");
        }
    }
    public static void main(String[] args) {
// TODO Auto-generated method stub
        new ServerMe();
    }



}
