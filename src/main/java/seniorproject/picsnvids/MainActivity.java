package seniorproject.picsnvids;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static int RESULT_LOAD_IMAGE = 1;
    private Button uploadButton;
    private ImageView uploadImage;
    private Bitmap bitmap;
    private EditText text;
    Intent userIntent;
    private String name, eventCode, imagePath, nameTest;
    private Context context;
    private FileInputStream in = null;
    private BufferedInputStream bis;
    private OutputStream ops;
    private File pictureDirectory, imageFile;
    public ObjectOutputStream objOut;
    private ObjectInputStream objIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_media);

        Toolbar tool = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tool);

        context = this;

        uploadImage = (ImageView) findViewById(R.id.Image);
        uploadImage.setOnClickListener(this);
        uploadButton = (Button) findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(this);

        Bundle b = getIntent().getExtras();
        name = b.getString("userName");
        eventCode = b.getString("eventCode");
    }

    @Override
    public void onClick(View v){

        if(v.getId() == R.id.Image){
            Intent cameraIntent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String picturePath = pictureDirectory.getPath();

            Uri data = Uri.parse(picturePath);

            cameraIntent.setDataAndType(data, "image/*");

            startActivityForResult(cameraIntent, RESULT_LOAD_IMAGE);
        }
        else if(v.getId() == R.id.uploadButton){

            Client client = new Client(eventCode, name);
            client.execute();
        }
    }

    public byte[] getImageArr(Bitmap bitmap){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

        if(bitmap != null && !bitmap.isRecycled()){
            bitmap.isRecycled();
            bitmap = null;
        }

        byte[] imageBytes = baos.toByteArray();
       // String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return imageBytes;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {

            if(resultCode == RESULT_OK){
                if(requestCode == RESULT_LOAD_IMAGE){

                    Uri imageUri = data.getData();
                    InputStream inputStream;

                    try{
                        inputStream = getContentResolver().openInputStream(imageUri);
                        bitmap = BitmapFactory.decodeStream(inputStream);

                        // Set selected image in the ImageView in MainActivity
                        uploadImage.setImageBitmap(bitmap);

                    }catch (FileNotFoundException f){
                        f.printStackTrace();
                    }

                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "An error occured", Toast.LENGTH_SHORT).show();
        }
    }

    public static String getRealPath(Context context, Uri uri){
        String filePath = "";
        String wholeID = DocumentsContract.getDocumentId(uri);

        // Split at colon, use second item in the array
        String id = wholeID.split(":")[1];

        String[] column = { MediaStore.Images.Media.DATA };

        // where id is equal to
        String sel = MediaStore.Images.Media._ID + "=?";

        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, new String[]{ id }, null);

        int columnIndex = cursor.getColumnIndex(column[0]);

        if (cursor.moveToFirst()) {
            filePath = cursor.getString(columnIndex);
        }
        cursor.close();
        return filePath;
    }

    private class Client extends AsyncTask<String, Integer, Boolean>{

        private String dstAddress = "192.168.0.4", response = "80", name, event;
        private int dstPort;
        private TextView responseView;
        private int port = 80;
        //private Context context;
        private Intent i = new Intent();



        public Client (String event, String name){
            this.event = event;
            this.name = name;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
           // Toast.makeText(context, progress[0], Toast.LENGTH_LONG ).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Socket connectionSocket = null;

            // PLACES IMAGE INTO A BYTE ARRAY TO BE SENT TO SERVER
            byte[] buffer = getImageArr(bitmap);

            try{
                InetAddress connect = InetAddress.getByName(dstAddress);
                connectionSocket = new Socket(connect, port);

                objOut = new ObjectOutputStream(connectionSocket.getOutputStream());
                objOut.flush();

                objOut.writeObject("M");
                objOut.flush();
                objIn = new ObjectInputStream(connectionSocket.getInputStream());
                objOut.flush();
                objIn.readObject().toString();

                objOut.flush();
                objOut.writeObject(event);
                objOut.flush();

                objIn.readObject().toString();
                objOut.flush();

                objOut.writeObject(buffer);
                objOut.flush();

            }catch(UnknownHostException e){
                e.printStackTrace();
                response = "UnknownHostException: " + e.toString();
            }catch (IOException e){
                e.printStackTrace();
                response = "IOException: " + e.toString();
            }catch(ClassNotFoundException c){
                c.printStackTrace();
            }finally {
                if(connectionSocket != null && objOut != null){
                    try{
                        objOut.writeObject("exit");
                        objIn.close();
                        objOut.close();
                        connectionSocket.close();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        protected void onPostExecute(Boolean result){
            super.onPostExecute(result);

            uploadImage.setImageBitmap(null);
        }

    }
}
