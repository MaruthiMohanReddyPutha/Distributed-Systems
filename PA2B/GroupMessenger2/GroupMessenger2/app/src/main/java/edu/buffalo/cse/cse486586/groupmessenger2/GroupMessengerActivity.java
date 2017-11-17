package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = "GroupMessengerActivity";
    static final String[] PORT_STORE = {"11108", "11112", "11116", "11120", "11124"};
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static String CURRENT_PORT = "";
    static final int SERVER_PORT = 10000;
    int messageCounter = 0;
    Uri appStoreURI = null;
    int sequenceProposed = 0;
    int sequenceAgreed = 0;
    boolean isSequenceFormed = false;
    LinkedList<String> socketPortStore = new LinkedList<String>();
    ArrayList<MessageInfo> messageStore = new ArrayList<MessageInfo>();
    String failedNode = "";
    boolean errorFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        //CODE TAKEN FROM PA1
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        CURRENT_PORT = myPort;

        // Populate the LinkedList with the Port IDs
        for(int i = 0; i < PORT_STORE.length; i++){
            socketPortStore.add(PORT_STORE[i]);
        }

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * BUILDING THE URI
         * Taken from the OnPTestClickListener.java source class
         */
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        uriBuilder.scheme("content");
        appStoreURI = uriBuilder.build();

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                final EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                tv.append("\t"+msg);
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    // Idea taken from PA1 Source code
    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     * <p>
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            try{
                Log.d(TAG, "Called the Server Task Class");
                Socket receivedSkt = null;
                while(true){
                    receivedSkt = serverSocket.accept();
                    Log.d("Server Socket "," Ready to read !!!");

                    /*ObjectInputStream in = new ObjectInputStream(receivedSkt.getInputStream());
                    MessageInfo msg = (MessageInfo)in.readObject();*/

                    BufferedReader inputStream = new BufferedReader(new InputStreamReader(receivedSkt.getInputStream()));
                    String receivedMsg = inputStream.readLine();
                    Log.d("Message Received ",receivedMsg);
                    String[] msgParts = receivedMsg.split("::");        //Splitting the message through the message seperator
                    Log.d("Split Msg Array ",String.valueOf(msgParts.length));
                    MessageInfo msg = null;
                    if(msgParts != null && !Boolean.valueOf(msgParts[msgParts.length-1]) && !msgParts[0].equals("FAILED")){       //If the last boolean sent is true that is an Proposal case
                        msg = new MessageInfo(msgParts[0], -1, Integer.valueOf(msgParts[1]), Integer.valueOf(msgParts[2]), false);
                        //MessageInfo msg = new MessageInfo(msgToSend.trim(), -1, Integer.valueOf(CURRENT_PORT), Integer.valueOf(portSck), false);
                    }else if(msgParts != null && Boolean.valueOf(msgParts[msgParts.length-1]) && !msgParts[0].equals("FAILED")){
                        msg = new MessageInfo(msgParts[0], Integer.valueOf(msgParts[1]), Integer.valueOf(msgParts[2]), Integer.valueOf(msgParts[3]), true);
                    }else if(msgParts != null && msgParts[0].equals("FAILED")){
                        //String msg_fail = "FAILED"+"::"+portSck+"::"+CURRENT_PORT+"::"+port+"::false";
                        msg = new MessageInfo(msgParts[1],-1, Integer.valueOf(msgParts[2]), Integer.valueOf(msgParts[3]), false);
                        msg.isFailure = true;
                        Log.d("Failure state ","detected");
                    }

                    if(!msg.isFailure) {
                        if (!msg.isAgreed) {
                            messageStore.add(msg);
                            sequenceProposed = sequenceAgreed + 1;  // Incrementing the sequence number
                            PrintWriter out = new PrintWriter(receivedSkt.getOutputStream(), true);
                            out.println(sequenceProposed);
                            out.flush();
                            //out.close();
                        } else {
                            int agreed = msg.agreedSequence;
                            sequenceAgreed = agreed;            // Updating the last sequence agreed in the current emulator
                            for (MessageInfo m : messageStore) {
                                if (m.msgHash == msg.msgHash) {
                                    Log.d("Message marked ", " Deliverable");
                                    m.agreedSequence = agreed;
                                    m.isDeliverable = true;     // Marking the status of the message to be "Deliverable"
                                    break;
                                }
                            }
                            Collections.sort(messageStore);     //Message list to sort as per the custom order
                            Iterator<MessageInfo> msgList = messageStore.iterator();    //Getting the iterator to do concurrent modification of list
                            while (msgList.hasNext()) {
                                MessageInfo msgElement = msgList.next();
                                if (msgElement.isDeliverable) {
                                    Log.d("Delivering the ", " deliverable message ");
                                    publishProgress(msgElement.msg);    //Delivering the messages that are deliverable
                                    msgList.remove();                   //Removing the delivered message from the list
                                } else {
                                    break;
                                }
                            }
                            PrintWriter out = new PrintWriter(receivedSkt.getOutputStream(), true); //Writing a OK back to client
                            out.println("OK");
                            out.flush();
                        }
                    }else{  //If one of the ports has failed now we have to remove the undeliverable messages if any at the top of the queue
                        Log.d("Deletion mode ", CURRENT_PORT);
                        // socketPortStore.remove(msg.msg);    //removing the failed port
                        int portFailed = Integer.valueOf(msg.msg);
                        //Setting the value for the failed node
                        failedNode = msg.msg;

                        if(messageStore != null && !messageStore.isEmpty()) {
                            Log.d("Message store ","not null");
                            Iterator<MessageInfo> msgList = messageStore.iterator();    //Getting the iterator to do concurrent modification of list
                            while (msgList.hasNext()) {
                                MessageInfo msgElement = msgList.next();
                                if (!msgElement.isDeliverable && (msgElement.sourcePort == portFailed)) {
                                    Log.d("Message removed","Yes");
                                    msgList.remove();                   //Removing the delivered message from the list
                                }
                            }
                            // acknowledging the client
                            PrintWriter out = new PrintWriter(receivedSkt.getOutputStream(), true); //Writing a OK back to client
                            out.println("OK");
                            out.flush();
                        }
                    }
                    receivedSkt.close();
                }
            }catch (Exception e){
                Log.e(TAG, "Server Socket Acceptance failed.");
                e.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(String... strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();
            /*
             * Inserting the message into the content provider
             */
            ContentValues cv = new ContentValues();
            cv.put("key", Integer.toString(messageCounter));
            cv.put("value",strReceived);
            getContentResolver().insert(appStoreURI, cv);
            messageCounter++;   //Incrementing the value of the message counter by one

            Log.d("Str Recvd",strReceived);
            TextView textView = (TextView) findViewById(R.id.textView1);
            textView.append(strReceived+"\t\n");
        }
    }


    // Idea taken from PA1 Source code
    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            // A socket variable to store the connection that has caused the exception
            Socket socket = null;

            try {
                Log.d(TAG, msgs[0]+" "+msgs[1]);
                String msgToSend = msgs[0];

                ArrayList<Integer> sequenceNumberList = new ArrayList<Integer>();   //Array list to store the list of sequence numbers

                for(String portSck: socketPortStore){
                    Log.d("Failed Node : ",failedNode);
                    Log.d("Client->Server Port: ",portSck);
                    if(!portSck.equals(failedNode)) {

                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(portSck));
                        try {
                            String msg = msgToSend.trim() + "::" + CURRENT_PORT + "::" + portSck + "::false";
                            Log.d("Message sent : ", msg);

                            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); //Writing a OK back to clients
                            out.println(msg);
                            out.flush();

                            //Acknowledgement from the Server that message has been delivered successfully
                            BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            int sequenceNum = Integer.valueOf(inputStream.readLine());
                            Log.d("Sequence No. received ", String.valueOf(sequenceNum));
                            sequenceNumberList.add(sequenceNum);

                            out.close();
                            inputStream.close();

                            //Close the socket
                            socket.close();
                        }catch (NumberFormatException e){
                            Log.d("EXCEPTION ","NumberFormatException found!");
                            for (String port : socketPortStore) {
                                try {
                                    if (!port.equals(portSck)) {
                                        Log.d("Broadcast done to : ", port);
                                        Socket failureMessengerSocket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(port));
                                        String msg_fail = "FAILED" + "::" + portSck + "::" + CURRENT_PORT + "::" + port + "::false";
                                        Log.d("BROADCAST ",msg_fail);
                                        PrintWriter out_failure = new PrintWriter(failureMessengerSocket.getOutputStream(), true); //Writing a OK back to clients
                                        out_failure.println(msg_fail);
                                        out_failure.flush();
                                        Log.d("BROADCAST ","MSG SENT");

                                        BufferedReader inputStream_failure = new BufferedReader(new InputStreamReader(failureMessengerSocket.getInputStream()));
                                        String ack = inputStream_failure.readLine();
                                        Log.d("BROADCAST (ack) ",ack);
                                        if (ack != null) {
                                            Log.d("Message Status : ", "Removed Successfully.");
                                        }

                                        out_failure.close();
                                        inputStream_failure.close();

                                        failureMessengerSocket.close();
                                    }
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                int maxSequenceNum = Collections.max(sequenceNumberList) + 1;
                Log.d("MAX SEQ No. ",String.valueOf(maxSequenceNum));

                for(String soc: socketPortStore){
                    if(!soc.equals(failedNode)) {
                        socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}), Integer.parseInt(soc));
                        String msg = msgToSend.trim()+"::"+maxSequenceNum+"::"+CURRENT_PORT+"::"+soc+"::true";

                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true); //Writing a OK back to clients
                        out.println(msg);
                        out.flush();

                        // Receiving the ack
                        BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String ack = inputStream.readLine();
                        if (ack != null) {
                            Log.d("Recd after agreement: ", ack);
                        }else{      //a node has failed because it did not respond with a valid ACK
                            String failedPort = String.valueOf(socket.getPort());
                            //Broadcasting the failure information to all others
                        }

                        out.close();
                        inputStream.close();

                        // close the socket
                        socket.close();
                    }
                }

            } catch (SocketTimeoutException e){
                Log.e(TAG, "Socket timed out!");
                e.printStackTrace();
            }
            catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ClientTask socket IOException");
                e.printStackTrace();
            } catch(NumberFormatException e){
                Log.e(TAG, "Number format exception is found !");
            }
            catch (Exception e){
                Log.e(TAG, "Some Exception in Client Call");
                e.printStackTrace();
            }

            return null;
        }
    }
}
