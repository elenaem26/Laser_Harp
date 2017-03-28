package ea.em.harp;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity implements OnLoadCompleteListener {

    final String LOG_TAG = "myLogs";
    final int MAX_STREAMS = 5;
    public static int REQUEST_BLUETOOTH = 1;

    SoundPool sp;
    int soundIdShot;

    BluetoothAdapter BTAdapter;
    BluetoothDevice harpDevice;
    BluetoothSocket bTSocket;
    int streamIDShot;
    int streamIDExplosion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);

        sp = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        sp.setOnLoadCompleteListener(this);
        soundIdShot = sp.load(this, R.raw.shot, 1);
        Log.d(LOG_TAG, "soundIdShot = " + soundIdShot);
        try {
            bluetooth();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onClick(View view) {
        sp.play(soundIdShot, 1, 1, 0, 0, 1);
    }

    @Override
    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        Log.d(LOG_TAG, "onLoadComplete, sampleId = " + sampleId + ", status = " + status);
    }

    private void bluetooth() throws IOException {
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
        Log.d("IS CONNECTED", connect(harpDevice, harpDevice.getUuids()[0].getUuid()) ? "CONNECTED" : "NOOT CONNECTED");
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
    }

    private void receiveData(BluetoothSocket socket) throws IOException{
        byte[] buffer = new byte[256];
        //ByteArrayInputStream input = new ByteArrayInputStream(buffer);
        InputStream inputStream = socket.getInputStream();
        inputStream.read(buffer);
        if (checkStreamIsNotEmpty(inputStream)) {
            DataInputStream mmInStream = new DataInputStream(inputStream);
            int bytes = mmInStream.read(buffer);
            String readMessage = new String(buffer, 0, bytes);
            Log.d("RECEIVED", readMessage);
            sp.play(soundIdShot, 1, 1, 0, 0, 1);
        }
    }

    private void read(BluetoothSocket socket) throws IOException {
        while (socket.isConnected()) {
           receiveData(socket);
        }
    }

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
}
