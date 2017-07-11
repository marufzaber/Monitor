import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;




public class Monitor {
	
	private static int qlq_count;
    private static int qbq_count;
    private static int qcq_count;
    private static int vcq_count;
    private static int rcq_count;
    
    private static boolean stop = false;
    private static String file_to_read = null;
    private static String file_to_write = null;
    private static double current_score;
    private static double [] wait_time =  new double[5]; 
    private static double [] run_time = new double[5];
    private static String seen;
    private static boolean empty = true;
    private static ArrayList<String> results = new ArrayList<String>();
    
    private static Socket socket; 
    private static int port = 25000;
    private static ServerSocket serverSocket;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		qlq_count = qbq_count = qcq_count = vcq_count = rcq_count = 1;
		current_score = Long.MAX_VALUE;
		file_to_read = "/Users/demigorgan/SourcererCC/clone-detector/run_environment.txt";
		file_to_write = "/Users/demigorgan/SourcererCC/clone-detector/config.txt";
		try{
		
			port = 25000;
			serverSocket = new ServerSocket(port);
			
		}
		catch(IOException e){
			e.printStackTrace();
		}
				
		
			
		while(true){	
			readFile();
			
			if(!stop && !empty){
				System.out.println(qlq_count+"  "+qbq_count+"  "+qcq_count+"  "+vcq_count+"  "+rcq_count);
				System.out.println(current_score+"   "+getScore());
				if(getScore() != current_score){
					 
					current_score = getScore();
					double cpu_usage = getCPUUsage();
					
					if(cpu_usage <= 100.0){
						System.out.println("CPU Load : "+cpu_usage);
						
						int pool_to_update = indexOfMax();		
						updateConfig(pool_to_update);
						writeFile();
					}
					
					else{
						System.out.println("\n\n\n\n\n\n\nSkipping CPU Load : "+getCPUUsage()+"\n\n\n\n\n\n\n\n\n");
					}
				}
			}
			
			try {
				Thread.sleep(1000);
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}	
	}
	
	private static double getCPUUsage(){
		ExecuteShellCommand com = new ExecuteShellCommand();
		//return Double.parseDouble(com.executeCommand("ls"));
		String cpu_by_processes = com.executeCommand("ps -A -o %cpu");
		String split[] = cpu_by_processes.split(" ");
		double cpu_load = 0;
		for(int i=2;i<split.length;i++){
			
			if(split[i].length()>0){
			   // System.out.println(split[i]);
				cpu_load += Double.parseDouble(split[i]);
			   
			}
		}
		return cpu_load;
		
	}
	
	private static void updateConfig(int pool){
		switch(pool){
		case 0:  
			qlq_count++;
			break;
		case 1: 
			qbq_count++;
			break;
		case 2:
			qcq_count++;
			break;
		case 3: 
			vcq_count++;
			break;
		case 4:
			rcq_count++;
			break;
		default:				
		}
	}
	
	private static int indexOfMax() {
	    double max = wait_time[0];
	    int maxIndex = 0;
	    for (int i = 1; i < wait_time.length; i++) {
	        if (wait_time[i] > max) {
	            maxIndex = i;
	            max = wait_time[i];
	        }
	    }
	    if(maxIndex == (wait_time.length -1)) return maxIndex;
	    return maxIndex+1;
	}
	
	private static void writeFile(){
	
    	
    		BufferedWriter out = null;
        	try{
        	    FileWriter fstream = new FileWriter(file_to_write, false); //true tells to append data.
        	    out = new BufferedWriter(fstream);
        	    out.write(qlq_count + " "+qbq_count+" "+ 
        	    		qcq_count +" "+ vcq_count +" "+ rcq_count+" DIRTY");    	    
        	    System.out.println("Written : "+qlq_count + " "+qbq_count+" "+ 
        	    		qcq_count +" "+ vcq_count +" "+ rcq_count+" DIRTY");    	         	    
        	}
        	catch (IOException e){
        	    System.err.println("Error: " + e.getMessage());
        	}
        	finally{
        		try{
    	    	    if(out != null) {
    	    	        out.close();
    	    	    }
        		}
        		catch (Exception e) {
        			e.printStackTrace();
    			}
        	}
    	
	}
	
	private static void readFile(){
		
    		try (BufferedReader br = new BufferedReader(new FileReader(file_to_read))) {
    			String sCurrentLine;
    			if ((sCurrentLine = br.readLine()) != null) {
    				empty = false;
    				stop = false;
                    if(!results.contains(sCurrentLine)){
                        System.out.println("Reading : "+sCurrentLine);
                        results.add(sCurrentLine);
                    }
                    if(sCurrentLine.contains("FINISHED"))
	    					stop = true;
	    				else if(sCurrentLine.length()!= 0){
	    					seen = sCurrentLine;
	    					String [] rt = sCurrentLine.split(" ");
	    					for(int i=0;i<5;i++){
	    						wait_time[i] = Double.parseDouble(rt[i]);
	    					}
	    					
	    					for(int i=5;i<10;i++){
	    						run_time[i-5] = Double.parseDouble(rt[i]);
	    					}   					
	    				}
    				
    				
    			}
    			else{
    				empty = true;
    			}
    		} catch (IOException e) {
    			e.printStackTrace();
    		}  		
    	}
    
	
	public static double getScore(){
		double score = 0;
		double [] weight = getRatio();
		for(int i=0;i<5;i++){
			score+=(weight[i] * wait_time[i]);
		}
		return score;
	}
	
	public static double [] getRatio(){
		double [] ratio = new double[5];
		double denominator = 0;
		
		for(int i=0;i<5;i++){	
			ratio[i]=run_time[i];		
			denominator += Math.pow(ratio[i], 2.0);				
		}	
		denominator = Math.sqrt(denominator);
		for(int i=0;i<5;i++){
			ratio[i]/=denominator;		
		}
		return ratio;
	}
}

class ExecuteShellCommand {

	public String executeCommand(String command) {
	
	    StringBuffer output = new StringBuffer();
	
	    Process p;
	    try {
	        p = Runtime.getRuntime().exec(command);
	        p.waitFor();
	        BufferedReader reader = 
	                        new BufferedReader(new InputStreamReader(p.getInputStream()));
	
	        String line = "";           
	        while ((line = reader.readLine())!= null) {
	            output.append(line + " ");
	        }
	
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    
	    return output.toString();
	
	}

}
	


