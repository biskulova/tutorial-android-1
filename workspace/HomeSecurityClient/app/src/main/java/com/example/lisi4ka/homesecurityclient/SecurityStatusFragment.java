package com.example.lisi4ka.homesecurityclient;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;

import com.example.lisi4ka.homesecuritymodel.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 * <p>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p>
 */
public class SecurityStatusFragment extends ListFragment {
    // Debugging
    private static final String TAG = "SecurityStatusFragment";

    // Intent request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;

    /**
     * Member object for the chat services
     */
    private BluetoothService mBluetoothService;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    private List<SecurityPoint> mSecurityPointList;

    private TSerializer tSerializer = new TSerializer();
    private TDeserializer tDeserializer = new TDeserializer();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SecurityStatusFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }

        /* TODO: read states list from savedInstanceState, last update Date also
        ArrayAdapter<SecurityPoint> adapter = new InteractiveArrayAdapter(getActivity(),
                getModel());
        setListAdapter(adapter);
        */
    }

    @Override
    public void onStart(){
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupCommunication() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup UI and BluetoothService
        } else if (mBluetoothService == null) {
            startBluetoothService();
        }
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!mSecurityPointList.isEmpty()){
            //mSecurityPointList.
            //outState.putStringArrayList();
        }
    }
        @Override
    public void onResume() {
        super.onResume();
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBluetoothService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        if (mBluetoothService != null) {
            mBluetoothService.stop();
        }

        tDeserializer = null;
        tSerializer = null;

        mSecurityPointList.clear();
        mSecurityPointList = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.security_status_menu, menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan:
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            case R.id.action_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data){
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_CANCELED) {
                    // Bluetooth is now enabled, so set up session
                    if (mBluetoothService == null) {
                        startBluetoothService();
                    }
                } else {
                    // User did not enable Bluetooth or an error occurred
                    //Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    mBluetoothService.connect(device, true);
                }
                break;
        }
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        SecurityPoint selectedPoint = getSecurityPointList().get(position);
        selectedPoint.setSelected(!selectedPoint.isSelected());

        try {
            SensorThrift sensor = new SensorThrift(selectedPoint.getName(), selectedPoint.isSelected());
            byte[] bytes = tSerializer.serialize(sensor);
            mBluetoothService.write(bytes);
        } catch (TException exception){
            Log.e(TAG, "Failed to serialize sensor data with name " + selectedPoint.getName(), exception);
        }
    }

    private List<SecurityPoint> getModel(){
        return getSecurityPointList();
    }

    private List<SecurityPoint> getSecurityPointList() {
        if (mSecurityPointList == null) {
            mSecurityPointList = new ArrayList<SecurityPoint>();
        }
        return mSecurityPointList;
    }

    public void startBluetoothService() {
        // Initialize the BluetoothService to perform bluetooth connections
        if (mBluetoothService == null) {
            mBluetoothService = new BluetoothService(getActivity(), mHandler);
        }
        Log.d(TAG, "startBluetoothService");
    }

    public void securityStatusReceived(byte [] statusData){
        if (statusData.length > 0){
            try {
                SensorListThrift sensorsList = null;
                tDeserializer.deserialize(sensorsList, statusData);
                // Security points list should refill each time with data from server, so clear it before
                List<SecurityPoint> list = getSecurityPointList();
                list.clear();

                for (SensorThrift sensor : sensorsList.getSensors()){
                    SecurityPoint securityPoint = new SecurityPoint(sensor.getName(), sensor.isState());
                    list.add(securityPoint);
                    if (sensor.isState())
                        list.get(list.indexOf(securityPoint)).setSelected(true);
                }
            }
            catch (TException exception) {
                Log.e(TAG, "Input BT data was not sensor list ", exception);
            }
            finally {
                // TODO: refresh UI
            }
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        final ActionBar actionBar = getActivity().getActionBar();

        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getActivity().getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String writeMessage = new String(writeBuf);
                    Log.d(TAG, "Command to server send " + writeMessage);
                    break;
                case Constants.MESSAGE_READ_STATUS:
                    byte[] readBuf = (byte[]) msg.obj;
                    securityStatusReceived(readBuf);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(getActivity(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getActivity(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}
