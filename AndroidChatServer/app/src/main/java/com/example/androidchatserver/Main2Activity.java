package com.example.androidchatserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import android.content.ClipData;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Bundle;

public class Main2Activity extends AppCompatActivity {

    static final int SocketServerPORT = 8080;//When selecting a port number for your server, select one that is greater than 1,023!

    TextView infoIp, infoPort, chatMsg;

    String msgLog = "";

    List<ChatClient> userList;
    List<ChatClient> groupList;
    ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        infoIp = (TextView) findViewById(R.id.infoip);
        infoPort = (TextView) findViewById(R.id.infoport);
        chatMsg = (TextView) findViewById(R.id.chatmsg);

        infoIp.setText(getIpAddress());// IP address returned by InetAddress.getLocalHost() might not be the right one to use.

        userList = new ArrayList<ChatClient>();//maintain a list of all the clients.
        groupList = new ArrayList<ChatClient>();//maintain a list of all the groups.
        ChatServerThread chatServerThread = new ChatServerThread();
        chatServerThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();//final chance to shutdown and close things.

        if (serverSocket != null) {
            try {
                serverSocket.close();//close the output and input stream before you close the socket.
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private class ChatServerThread extends Thread {

        @Override
        public void run() {
            Socket socket = null;//server obtains this Socket object from the return value of the accept() method of ServerSocket.

            try {
                //a specific type of socket used to listen for client requests
                serverSocket = new ServerSocket(SocketServerPORT);//Open the Server Socket
                Main2Activity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        infoPort.setText("I'm waiting here: "
                                + serverSocket.getLocalPort());//Returns the port number on which this socket is listening.
                    }
                });

                while (true) {
                    socket = serverSocket.accept();//create a socket object from the ServerSocket in order to listen for and accept connections from clients-wait for client requests.
                    //returns a Socket which you use on the server side to handle the connection to a particular client
                    ChatClient client = new ChatClient();
                    userList.add(client);
                    groupList.add(client);
                    ConnectThread connectThread = new ConnectThread(client, socket);//Multi-threaded ServerSocket
                    connectThread.start();
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    private class ConnectThread extends Thread {

        Socket socket;
        ChatClient connectClient;
        String msgToSend = "";




        ConnectThread(ChatClient client, Socket socket){
            connectClient = client;
            this.socket= socket;
            client.socket = socket;
            client.chatThread = this;
        }

        @Override
        public void run() {
            DataInputStream dataInputStream = null;
            DataOutputStream dataOutputStream = null;

            try {
                //create input/output streams to communicate with the client.
                dataInputStream = new DataInputStream(socket.getInputStream());//to receive input from the client
                dataOutputStream = new DataOutputStream(socket.getOutputStream());

                String n = dataInputStream.readUTF();
                String n1 = dataInputStream.readUTF();
                connectClient.name = n;
                connectClient.gname = n1;

                msgLog += connectClient.name + " connected@" +
                        connectClient.socket.getInetAddress() +
                        ":" + connectClient.socket.getPort() + connectClient.gname + "\n";

                Main2Activity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        chatMsg.setText(msgLog);
                    }
                });

                //dataOutputStream.writeUTF("Welcome " + n + "\n");
                //dataOutputStream.flush();//to write immediately

                //broadcastMsg(n + " join our chat.\n");

                while (true) {
                    if (dataInputStream.available() > 0) {
                        String newMsg = dataInputStream.readUTF();


                        msgLog += n + ": " + newMsg;
                        Main2Activity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }
                        });

                        broadcastMsg(n + ": " + newMsg);
                    }

                    if(!msgToSend.equals("")){
                        dataOutputStream.writeUTF(msgToSend);//perform communication with client.
                        dataOutputStream.flush();
                        msgToSend = "";
                    }

                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                userList.remove(connectClient);
                Main2Activity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Main2Activity.this,
                                connectClient.name + " removed.", Toast.LENGTH_LONG).show();

                        msgLog += "-- " + connectClient.name + " leaved\n";
                        Main2Activity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                chatMsg.setText(msgLog);
                            }
                        });

                        broadcastMsg("-- " + connectClient.name + " leaved\n");
                    }
                });
            }

        }

        private void sendMsg(String msg){
            msgToSend = msg;
        }

    }

    private void broadcastMsg(String msg){
        for(int i=0; i<userList.size(); i++){
            userList.get(i).chatThread.sendMsg(msg);
            msgLog += "- send to " + userList.get(i).name + "\n";
        }

        Main2Activity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                chatMsg.setText(msgLog);
            }
        });
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: "
                                + inetAddress.getHostAddress() + "\n";
                    }

                }

            }

        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something Wrong! " + e.toString() + "\n";
        }

        return ip;
    }

    class ChatClient {
        String name;
        Socket socket;
        String gname;
        ConnectThread chatThread;

    }

}
