import java.awt.image.SampleModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.jfree.util.WaitingImageObserver;

import lejos.hardware.lcd.LCD;
import lejos.hardware.motor.Motor;
import lejos.hardware.port.SensorPort;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.robotics.SampleProvider;
import lejos.robotics.chassis.Chassis;
import lejos.robotics.chassis.Wheel;
import lejos.robotics.chassis.WheeledChassis;
import lejos.robotics.navigation.MovePilot;




public class MainApplication {



	
	
	
	public static void main(String[] args) throws IOException, InterruptedException 
	{

		LCD.drawString("Anschnallen!", 0, 4);
		
		
		Wheel wheel1 = WheeledChassis.modelWheel(Motor.B, 43.2).offset(-49);
		Wheel wheel2 = WheeledChassis.modelWheel(Motor.D, 43.2).offset(49);
		Chassis chassis = new WheeledChassis(new Wheel[] { wheel1, wheel2 }, WheeledChassis.TYPE_DIFFERENTIAL);
		MovePilot pilot = new MovePilot(chassis);
		
		// Get an instance of the Ultrasonic EV3 sensor
		// Get an instance of this sensor in measurement mode
		
		
		EV3ColorSensor colorSensor = new EV3ColorSensor(SensorPort.S1);
		SampleProvider color= colorSensor.getColorIDMode();
		
		EV3UltrasonicSensor sonicSensor = new EV3UltrasonicSensor(SensorPort.S2);
		SampleProvider distance= sonicSensor.getDistanceMode();
		

		
		//Aufgabe3.1
		doAufgabe3_1(pilot, distance, color);

		
	}
	
	/**Steers the bot the way requested in task 3.1 
	 * @throws IOException 
	 * @throws InterruptedException */
	private static void doAufgabe3_1(MovePilot pilot, SampleProvider distance, SampleProvider color) throws IOException, InterruptedException {
		//Aufgabe 3.1:

		Socket server = null;
		float[] sampleColor = new float[color.sampleSize()];
		float[] sampleSonic = new float[distance.sampleSize()];
		
		try
	    {
			server = new Socket( "10.0.1.37", 6666 );
	    }
	    catch ( UnknownHostException e ) {
	      e.printStackTrace();
	    }
	    catch ( IOException e ) {
	      e.printStackTrace();
	    }
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		String order;
		Boolean end = false;
		
		
		while(end == false)
		{

			order = rcvMsg(server);
			Command command = trimCmd(order);
			
			switch (command.cmd)
			{
		
			case "sensor":
				//Looking right
				Motor.A.rotate(90);
				sensorsToLCD(distance, color);
				distance.fetchSample(sampleSonic, 0);
				color.fetchSample(sampleColor, 0);
				String sonic = "Dist: "+sampleSonic[0];
				String colorId = "Color ID: "+sampleColor[0];
				writeMsg(server, "" + sonic +"|"+colorId);
				Thread.sleep(100);
				
				//Looking left
				Motor.A.rotate(-180);
				sensorsToLCD(distance, color);
				distance.fetchSample(sampleSonic, 0);
				color.fetchSample(sampleColor, 0);
				sonic = "Dist: "+sampleSonic[0];
				colorId = "Color ID: "+sampleColor[0];
				writeMsg(server, "" + sonic +"|"+colorId);
				Thread.sleep(100);
				
				//Looking forward
				Motor.A.rotate(90);
				sensorsToLCD(distance, color);
				distance.fetchSample(sampleSonic, 0);
				color.fetchSample(sampleColor, 0);
				sonic = "Dist: "+sampleSonic[0];
				colorId = "Color ID: "+sampleColor[0];
				writeMsg(server, "" + sonic +"|"+colorId);
				
				
//				sensorsToLCD(distance, color);
//				distance.fetchSample(sampleSonic, 0);
//				color.fetchSample(sampleColor, 0);
//				String sonic = "Dist: "+sampleSonic[0];
//				String colorId = "Color ID: "+sampleColor[0];
//				writeMsg(server, "" + sonic +" | ColorID: "+colorId);
				
				Thread.sleep(100);
				writeMsg(server, "DONE");
				break;
		
			case "vor":
				pilot.travel(command.value);
				writeMsg(server, "DONE");
				break;
		
			case "zurueck":
				pilot.travel(-command.value);
				writeMsg(server, "DONE");
				break;
				
			case "links":
				pilot.rotate(-command.value);
				writeMsg(server, "DONE");
				break;		
				
			case "rechts":
				pilot.rotate(command.value);
				writeMsg(server, "DONE");
				break;
				
			case "monte":
				monteCarlo(server, pilot, distance, color);
				writeMsg(server, "DONE");
				break;
				
			case "end":
				end = true;
				writeMsg(server, "CLOSE");
				pilot.stop();
				break;
				
			default:
				writeMsg(server, "WRONG");
				break;
			}
		}
	}
	
	/**Writes the color-and ultrasonic sensor values to the bot's LCD */
	private static void sensorsToLCD(SampleProvider distance, SampleProvider color) {
		// initialize an array of floats for fetching samples. 
		// Ask the SampleProvider how long the array should be
		float[] sampleSonic = new float[distance.sampleSize()];
		distance.fetchSample(sampleSonic, 0);
					
		LCD.clear();
		System.out.println("Dist: "+sampleSonic[0]);
			
		//Same for the color sensor:
		float[] sampleColor = new float[color.sampleSize()];
		color.fetchSample(sampleColor, 0);
			        
		LCD.clear();
		
		//ColorId Mode:
		System.out.println("ColorID: "+sampleColor[0]);
		
		//Color RGB Mode:
		//System.out.println("R: " + sampleColor[0]);
		//System.out.println("G: " + sampleColor[1]);
		//System.out.println("B: " + sampleColor[2]);
	}
	
    private static void writeMsg(Socket socket, String msg) throws IOException 
    {
    	PrintWriter printWriter =new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    	printWriter.print(msg + "\r\n");
    	printWriter.flush();
//    	System.out.println("Client said: " + msg);
    }
    
    private static String rcvMsg(Socket socket) throws IOException 
    {
    	BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    	String msg = bufferedReader.readLine(); // blockiert bis Nachricht empfangen
    	return msg;
    }
    
    
    
    private static Command trimCmd (String cmd)
    {
    	
    	for (int i = 0; i < cmd.length(); i++)
    	{
    		if(cmd.charAt(i) == '_')
    		{
    			String command = cmd.substring(0, i);
    			String value = cmd.substring(i+1);
    			return new Command(command, value);
    		}
    	}
    	return null;
    	  	
    }
	
    
    

    private static void monteCarlo(Socket server, MovePilot pilot, SampleProvider distance, SampleProvider color) throws IOException
    {
		float[] sampleSonic = new float[distance.sampleSize()];
		float[] sampleColor = new float[color.sampleSize()];
    	sensorsToLCD(distance, color);
    	color.fetchSample(sampleColor,0);
    	String colorId = "" + sampleColor[0];
    	String sonic = "" + sampleSonic[0];
    	
    	if(colorId.equals("7.0")) 
    	{
    		//looking forward
			distance.fetchSample(sampleSonic, 0);
			sonic = "Dist: "+sampleSonic[0];
			writeMsg(server, "" + sonic + " 0");
			
			//Looking right
			Motor.A.rotate(90);
			distance.fetchSample(sampleSonic, 0);
			sonic = "Dist: "+sampleSonic[0];
			writeMsg(server, "" + sonic + " 0");
			Motor.A.rotate(-90);
			
			//Looking left
			Motor.A.rotate(-90);
			distance.fetchSample(sampleSonic, 0);
			sonic = "Dist: "+sampleSonic[0];
			writeMsg(server, "" + sonic + " 0");
			Motor.A.rotate(90);
    		
    	}
    	else 
    	{
    		//TODO: find the street
    	}
    	
    }
}