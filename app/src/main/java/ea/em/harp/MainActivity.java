package ea.em.harp;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Set;
import java.util.UUID;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class MainActivity extends Activity implements OnLoadCompleteListener {

    final String LOG_TAG = "myLogs";
    final int MAX_STREAMS = 6;
    public static int REQUEST_BLUETOOTH = 1;

    SoundPool sp;
    int note_c;
    int note_d;
    int note_e;
    int note_f;
    int note_g;
    int note_a;

    BluetoothAdapter BTAdapter;
    BluetoothDevice harpDevice;
    BluetoothSocket bTSocket;
    int streamIDShot;
    int streamIDExplosion;

    TextView myLabel;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        sp = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        sp.setOnLoadCompleteListener(this);
        setSounds("piano");
        Button openButton = (Button)findViewById(R.id.open);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    findBT();
                    openBT();
                } catch (IOException ex) {
                }
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });

    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        Log.d(LOG_TAG, "onLoadComplete, sampleId = " + sampleId + ", status = " + status);
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_piano:
                if (checked)
                    setSounds("piano");
                    break;
            case R.id.radio_cello:
                if (checked)
                    setSounds("cello");
                    break;
            case R.id.radio_guitarchanks:
                if (checked)
                    setSounds("guitarchanks");
                break;
            case R.id.radio_sample1:
                if (checked)
                    setSounds("sample1");
                break;
        }
    }

    private void setSounds(String type) {
        switch (type) {
            case "piano":
                note_c = sp.load(this, R.raw.piano_c, 1);
                note_d = sp.load(this, R.raw.piano_d, 1);
                note_e = sp.load(this, R.raw.piano_e, 1);
                note_f = sp.load(this, R.raw.piano_f, 1);
                note_g = sp.load(this, R.raw.piano_g, 1);
                note_a = sp.load(this, R.raw.piano_a, 1);
                break;
            case "cello":
                note_c = sp.load(this, R.raw.cello_c, 1);
                note_d = sp.load(this, R.raw.cello_d, 1);
                note_e = sp.load(this, R.raw.cello_e, 1);
                note_f = sp.load(this, R.raw.cello_f, 1);
                note_g = sp.load(this, R.raw.cello_g, 1);
                note_a = sp.load(this, R.raw.cello_a, 1);
                break;
            case "guitarchanks":
                note_c = sp.load(this, R.raw.guitarchanks_a, 1);
                note_d = sp.load(this, R.raw.guitarchanks_d, 1);
                note_e = sp.load(this, R.raw.guitarchanks_e, 1);
                note_f = sp.load(this, R.raw.guitarchanks_f, 1);
                note_g = sp.load(this, R.raw.guitarchanks_g, 1);
                note_a = sp.load(this, R.raw.guitarchanks_a, 1);
                break;
            case "sample1":
                note_c = sp.load(this, R.raw.sample1_a, 1);
                note_d = sp.load(this, R.raw.sample1_d, 1);
                note_e = sp.load(this, R.raw.sample1_e, 1);
                note_f = sp.load(this, R.raw.sample1_f, 1);
                note_g = sp.load(this, R.raw.sample1_g, 1);
                note_a = sp.load(this, R.raw.sample1_a, 1);
                break;
            default:
                note_c = sp.load(this, R.raw.piano_d, 1);
                note_d = sp.load(this, R.raw.piano_e, 1);
                note_e = sp.load(this, R.raw.piano_f, 1);
                note_f = sp.load(this, R.raw.piano_g, 1);
                note_g = sp.load(this, R.raw.piano_a, 1);
                note_a = sp.load(this, R.raw.piano_c, 1);
                break;
        }
    }

    /*private void bluetooth() throws IOException {
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        // Phone does not support Bluetooth so let the user know and exit.
        if (BTAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not compatible")
                    .setMessage("Your phone does not support Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        if (!BTAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_BLUETOOTH);
        }
        Set<BluetoothDevice> pairedDevices = BTAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("HC-06")) {
                    harpDevice = device;
                    break;
                }
            }
        }
        if (harpDevice == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Not found")
                    .setMessage("Harp device not found")
                    .setPositiveButton("Shit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        Log.d("IS CONNECTED", connect(harpDevice, harpDevice.getUuids()[0].getUuid()) ? "CONNECTED" : "NOT CONNECTED");
        read(bTSocket);
    }

    public boolean connect(BluetoothDevice bTDevice, UUID mUUID) {
        BluetoothSocket temp = null;
        try {
            temp = bTDevice.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            Log.d("CONNECTTHREAD","Could not create RFCOMM socket:" + e.toString());
            return false;
        }
        try {
            bTSocket = temp;
            bTSocket.connect();
        } catch(IOException e) {
            Log.d("CONNECTTHREAD","Could not connect: " + e.toString());
            try {
                bTSocket.close();
            } catch(IOException close) {
                Log.d("CONNECTTHREAD", "Could not close connection:" + e.toString());
                return false;
            }
        }
        return true;
    }

    public boolean cancel() {
        try {
            bTSocket.close();
        } catch(IOException e) {
            Log.d("CONNECTTHREAD","Could not close connection:" + e.toString());
            return false;
        }
        return true;
    }*/

   /* private void receiveData(BluetoothSocket socket) throws IOException{
        byte[] buffer = new byte[256];
        //ByteArrayInputStream input = new ByteArrayInputStream(buffer);
        InputStream inputStream = socket.getInputStream();
        inputStream.read(buffer);
        if (checkStreamIsNotEmpty(inputStream)) {
            DataInputStream mmInStream = new DataInputStream(inputStream);
            int bytes = mmInStream.read(buffer);
            Integer ok = mmInStream.readInt();
            //String readMessage = new Integer(buffer, 0, bytes);
            Log.d("RECEIVED", ok.toString());
            sp.play(soundIdShot, 1, 1, 0, 0, 1);
        }
    }*/

    /*private void read(BluetoothSocket socket) throws IOException {
        while (socket.isConnected()) {
           receiveData(socket);
        }
    }*/

    private boolean checkStreamIsNotEmpty(InputStream inputStream) throws IOException {
        PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
        int b;
        b = pushbackInputStream.read();
        if ( b == -1 ) {
            return false;
        }
        pushbackInputStream.unread(b);
        return true;
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-06"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void closeBT() throws IOException
    {
        stopWorker = true;
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    playSound(data);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            myLabel.setText(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    private void playSound(String data) {
        data = data.replaceAll("(\\r|\\n)", "");
        switch (data) {
            case "0":
                sp.play(note_c, 1, 1, 0, 0, 1);
                break;
            case "1":
                sp.play(note_d, 1, 1, 0, 0, 1);
                break;
            case "2":
                sp.play(note_e, 1, 1, 0, 0, 1);
                break;
            case "3":
                sp.play(note_f, 1, 1, 0, 0, 1);
                break;
            case "4":
                sp.play(note_g, 1, 1, 0, 0, 1);
                break;
            case "5":
                sp.play(note_a, 1, 1, 0, 0, 1);
                break;
        }
    }
}
