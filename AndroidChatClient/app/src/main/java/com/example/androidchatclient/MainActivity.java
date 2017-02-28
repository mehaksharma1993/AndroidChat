package com.example.androidchatclient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Dialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ListViewCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

	static final int SocketServerPORT = 8080;

	LinearLayout loginPanel, chatPanel;

	EditText editTextUserName, editTextAddress;
    EditText editTextGroup;
	Button buttonConnect;
	TextView chatMsg, textPort;

	EditText editTextSay;
	Button buttonSend,buttonShow;
	Button buttonDisconnect;

	String msgLog = "";

	ChatClientThread chatClientThread = null;
	String msgType = "chat";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//userList = (TextView)findViewById(R.id.userList);


		loginPanel = (LinearLayout)findViewById(R.id.loginpanel);
		chatPanel = (LinearLayout)findViewById(R.id.chatpanel);
		editTextUserName = (EditText) findViewById(R.id.username);
		editTextAddress = (EditText) findViewById(R.id.address);
        editTextGroup = (EditText) findViewById(R.id.group);
		textPort = (TextView) findViewById(R.id.port);
		textPort.setText("port: " + SocketServerPORT);
		buttonConnect = (Button) findViewById(R.id.connect);
        buttonShow = (Button) findViewById(R.id.showUsers);
		buttonDisconnect = (Button) findViewById(R.id.disconnect);
		chatMsg = (TextView) findViewById(R.id.chatmsg);

		buttonShow.setOnClickListener(buttonShowUserOnClickListener);
		buttonConnect.setOnClickListener(buttonConnectOnClickListener);
		buttonDisconnect.setOnClickListener(buttonDisconnectOnClickListener);
		editTextSay = (EditText)findViewById(R.id.say);
		buttonSend = (Button)findViewById(R.id.send);
		buttonSend.setOnClickListener(buttonSendOnClickListener);
	}



	OnClickListener buttonShowUserOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(chatClientThread==null){
				return;
			}
			msgType = "userList";
			chatClientThread.sendMsg("getUserList1234");

		}

	};
	OnClickListener buttonDisconnectOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if(chatClientThread==null){
				return;
			}
			chatClientThread.disconnect();
		}

	};


	OnClickListener buttonSendOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (editTextSay.getText().toString().equals("")) {
				return;
			}

			if(chatClientThread==null){
				return;
			}

			chatClientThread.sendMsg(editTextSay.getText().toString() + "\n");
			editTextSay.setText("");
			
		}

	};

	OnClickListener buttonConnectOnClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			String textUserName = editTextUserName.getText().toString();
			if (textUserName.equals("")) {
				Toast.makeText(MainActivity.this, "Enter User Name",
						Toast.LENGTH_LONG).show();
				return;
			}

			String textAddress = editTextAddress.getText().toString();
			if (textAddress.equals("")) {
				Toast.makeText(MainActivity.this, "Enter Addresse",
						Toast.LENGTH_LONG).show();
				return;
			}
            String textGroup = editTextGroup.getText().toString();
            if (textGroup.equals("")) {
                Toast.makeText(MainActivity.this, "Enter Group Name:",
                        Toast.LENGTH_LONG).show();
                return;
            }

			msgLog = "";
			chatMsg.setText(msgLog);
			loginPanel.setVisibility(View.GONE);
			chatPanel.setVisibility(View.VISIBLE);

			chatClientThread = new ChatClientThread(
					textUserName, textAddress, SocketServerPORT,textGroup);
			chatClientThread.start();//calls run method in a new thread.
		}

	};

	private class ChatClientThread extends Thread
    //extends thread because we want separate thread for each client
    {

		String name;
		String dstAddress;
		int dstPort;
        String group;

		String msgToSend = "";
		boolean goOut = false;

		ChatClientThread(String name, String address, int port, String group) {
			this.name = name;
			dstAddress = address;
			dstPort = port;
            this.group=group;
		}

		@Override
		public void run() {
			Socket socket = null;
			DataOutputStream dataOutputStream = null;
			DataInputStream dataInputStream = null;

			try {
				socket = new Socket(dstAddress, dstPort);//create a socket object.
				dataOutputStream = new DataOutputStream(
						socket.getOutputStream());
				dataInputStream = new DataInputStream(socket.getInputStream());
				dataOutputStream.writeUTF(name);
                dataOutputStream.writeUTF(group);
				dataOutputStream.flush();

				while (!goOut) {
					if (dataInputStream.available() > 0) {
						if(msgType.equals("chat")) {
							msgLog += dataInputStream.readUTF();

							MainActivity.this.runOnUiThread(new Runnable() {

								@Override
								public void run() {
									chatMsg.setText(msgLog);
								}
							});
						} else if(msgType.equals("userList")) {
							msgType = "chat";
							msgToSend = "";
						}
					}

					if(!msgToSend.equals("")){
						dataOutputStream.writeUTF(msgToSend);
						dataOutputStream.flush();
						msgToSend = "";
					}
				}

			} catch (UnknownHostException e) {
				e.printStackTrace();
				final String eString = e.toString();
				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
					}

				});
			} catch (IOException e) {
				e.printStackTrace();
				final String eString = e.toString();
				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						Toast.makeText(MainActivity.this, eString, Toast.LENGTH_LONG).show();
					}

				});
			} finally {
				if (socket != null) {
					try {
						socket.close();
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

				if (dataInputStream != null) {
					try {
						dataInputStream.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						loginPanel.setVisibility(View.VISIBLE);
						chatPanel.setVisibility(View.GONE);
					}

				});
			}

		}

		private void sendMsg(String msg){
			msgToSend = msg;
		}

		private void disconnect(){
			goOut = true;
		}
	}

}