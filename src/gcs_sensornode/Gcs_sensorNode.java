/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gcs_sensornode;

import com.hazelcast.core.*;
import com.hazelcast.config.*;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Formatter;
import java.util.Map;
import jssc.SerialPort;
import jssc.SerialPortException;

/**
 *
 * @author akrv
 */
public class Gcs_sensorNode {

    /**
     * @param args the command line arguments
     */
    
    static SerialPort serialPort;
    
    public static void main(String[] args) {
        // TODO code application logic here
        Config cfg = new Config();
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
        Map<String, String[]> mapSensors = instance.getMap("mapSensors");
        Map<String, String[]> snifferMap = instance.getMap("snifferMap");
        Map<String, String> macAddr = instance.getMap("macAddr");
        Map<String, String> gatewayNames = instance.getMap("gatewayNames");
        
        // GCS Variables //
        Boolean macAddrExists = false;
        boolean executeOnceHazel = false;
        String timeStampCur;
        
        //initialization of the serial
        System.out.println("Comport to Init: " + args[0]);
        SerialInit(args[0]);
        
        
        while (true) {
            /*
            * rcvBuffer fills the serial messages
            * parsing the buffer and to put in hazelcast messages.
            */
        //snifferMap.forEach( (k,v) -> System.out.println("Key: " + k + ": Value: " + Arrays.toString(v)));
        System.out.println("Waiting for Data from " + args[0]);
        byte[] rcvBuffer;
        double x, y;
        double lux;
        float temp;
        byte[] mac;
        String macString; // convert mac address from hex to readable string
        String[] sensorValue; //hazelcast Values
                
            rcvBuffer = SerialgetBytes(13);
            while (true) {
                if (rcvBuffer[0] == 0x00) {
                    if (rcvBuffer[1] == 0x12) {
                        if (rcvBuffer[2] == 0x4B) {
                            break;
                        }
                    }
                }
                rcvBuffer = SerialgetBytes(13);
            }

        mac = Arrays.copyOfRange(rcvBuffer, 0, 8);
        macString = bytesToHexString(mac);
        
        temp = (float) rcvBuffer[12];
        lux = (double) ByteBuffer.wrap(Arrays.copyOfRange(rcvBuffer, 9, 11)).getShort();
        x = 54; // RSSI from Serial port
        y = 22; // TimeOfArrival from serial port
        //TODO receive data about x = RSSI, y = timeOfArrival
        //RSSI and timeOfArrival is used for localization
        // localization method locates the Node coordinates and puts them in location map.
        //time stamp added from the current machine
            /*
             *hazelcast mapping
             * timeStampCur is the key
             * String array with 5 the values MAC,Temp,Lux,x,y
             */
        timeStampCur = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSSS").format(new Date());
        sensorValue = new String[] {macString, String.valueOf(temp),String.valueOf(lux),String.valueOf(x),String.valueOf(y),timeStampCur};
        mapSensors.put(timeStampCur, sensorValue);
        /* printing the sensor information */
        for (String s: sensorValue) {
            System.out.println(s);
        }
        macAddrExists = addMacAddr(macString, macAddr);
        if (!executeOnceHazel){
           Collection<String> gatewayNamesSet = gatewayNames.values();
           if(!gatewayNamesSet.contains(args[1])){
               int numOfNodes = gatewayNames.size();
               gatewayNames.put(String.valueOf(numOfNodes+1) ,args[1]);
           }
           executeOnceHazel = true;
           
        }
        //sensorData = null;
        
        
        }
    }

        //initialize the serialport
    static void SerialInit(String port) {
        serialPort = new SerialPort(port);
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }
    //Close the connection
    static void Serialclose() {
        try {
            serialPort.closePort();
        } catch (SerialPortException ex) {
            System.out.println(ex);
        }
    }
    //data is set in the rcvBuffer in main method
    static byte[] SerialgetBytes(int count) {
        try {
            return serialPort.readBytes(count);
            
        } catch (SerialPortException ex) {
            System.out.println(ex);
            return null;
        }
    }

    //coverts the mac address to readable string
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        Formatter formatter = new Formatter(sb);
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }

        return sb.toString();
    }
    // insert mac address in hazelcast Set if not available already
    private static Boolean addMacAddr(String macString, Map<String, String> macAddr) {
        Collection<String> macAddrSet = macAddr.values();
        if (!macAddrSet.contains(macString)){
                    macAddr.put(String.valueOf(macAddrSet.size()+1), macString);
                    System.out.println("macAddr: " + macString + " has been updated");
                    return true;
        } else {
                    //System.out.println("macAddr already exists in hazelmap");
                    return false;
        }
    }
    
}
