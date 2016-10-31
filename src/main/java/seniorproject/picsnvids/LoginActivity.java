package seniorproject.picsnvids;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Created by Anthony Lumpkins on 10/18/2016.
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    private Button loginButton;
    private EditText eventCode, userName;
    private String eventCodeString, userNameString;
    private Boolean nextActivity = false;
    public ObjectOutputStream objOut;
    private ObjectInputStream objIn;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        eventCode = (EditText) findViewById(R.id.eventCodeText);
        userName = (EditText) findViewById(R.id.userName);
        loginButton = (Button) findViewById(R.id.login);
        loginButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

        eventCodeString = eventCode.getText().toString();
        userNameString = userName.getText().toString();

        LoginClient lc = new LoginClient(eventCodeString, userNameString);
        lc.execute();
    }

    public void setNextActivity(Boolean bool){

        Intent loginIntent = new Intent(this, MainActivity.class);

        // Has the key been validated or not?
        if(bool == true) {
            loginIntent.putExtra("userName", userName.getText().toString());
            loginIntent.putExtra("eventCode", eventCode.getText().toString());
            startActivity(loginIntent);
        } else{
            Toast.makeText(this, "Invalid Key", Toast.LENGTH_LONG).show();
            eventCode.setText("");
            userName.setText("");
            eventCode.requestFocus();
        }
    }

    private class LoginClient extends AsyncTask<String, Integer, Boolean> {

        private String dstAddress = "192.168.0.4", response, name, event, serverResponse;
        private int port = 80;

        public LoginClient (String event, String name){
            this.event = event;
            this.name = name;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Socket socket = null;

            try{
                InetAddress connect = InetAddress.getByName(dstAddress);
                socket = new Socket(connect, port);

                objOut = new ObjectOutputStream(socket.getOutputStream());
                objOut.flush();

                objOut.writeObject("M");
                objOut.flush();
                objIn = new ObjectInputStream(socket.getInputStream());
                objIn.readObject().toString();
                objOut.writeObject(event);

                serverResponse = objIn.readObject().toString();

            }catch(UnknownHostException e){
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            }catch (IOException e){
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }catch(ClassNotFoundException c){
                c.printStackTrace();
            }

            finally {
                if(socket != null && objOut != null){
                    try{
                        objOut.writeObject("exit");
                        objIn.close();
                        objOut.close();
                        socket.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        protected void onPostExecute(Boolean result){
            super.onPostExecute(result);

            // Sends true of false if the key has been validated
            if(serverResponse.equals("Validated")){
                nextActivity = true;
                setNextActivity(nextActivity);
            }
            else{
                setNextActivity(nextActivity);
            }
        }
    }
}
