import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {

    private static int secondsTimeout = 1000;
    public static void main(String[] args) throws Exception {
        System.out.println("MASTER...");


        FileReader     fr = new FileReader("../Resources/ip_list") ;
        BufferedReader bre = new BufferedReader(fr) ;
        Scanner        sc = new Scanner(bre) ;

        ArrayList<String> ipList = new ArrayList<String>();


        System.out.println("Splitting...");


        while(sc.hasNextLine()){
            String tmp = sc.nextLine();
            ipList.add(tmp);
        }
        splitFile("../Resources/input.txt", ipList.size());


        for(int i = 0; i<ipList.size(); ++i){
            String hostname = ipList.get(i); 
            ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "hostname");
            Process p = pb.start();
            InputStream is = p.getInputStream();
            boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

            if (timeoutStatus){
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
    
                while ((line = br.readLine()) != null){
                    //System.out.println("Sending split to : "+ line);
                }
                InputStream es = p.getErrorStream();
                BufferedReader ber = new BufferedReader(new InputStreamReader(es));
                String eLine;
                while ((eLine = ber.readLine()) != null){
                    System.out.println("Error : "+ eLine);
                }
                sendSplit(hostname, i);
            } else {
                System.out.println("TIMEOUT");
                p.destroy();
            }
        }
        for(int i = 0; i<ipList.size(); ++i){
            String hostname = ipList.get(i); 
            sendIp(hostname);
            
        }

        long startTime = System.currentTimeMillis();
        for(int i = 0; i<ipList.size(); ++i){
            String hostname = ipList.get(i); 
            launchMap(hostname, i);
        }
        long endTime =System.currentTimeMillis();
        double mapDuration = (endTime - startTime)/1000.0;
        System.out.println("MAP FINISHED: took " + mapDuration+"s");

        startTime = System.currentTimeMillis();
        for(int i = 0; i<ipList.size(); ++i){
            String hostname = ipList.get(i); 
            launchShuffle(hostname, i);
        }
        endTime =System.currentTimeMillis();
        double shuffleDuration = (endTime - startTime)/1000.0;
        System.out.println("SHUFFLE FINISHED: took " + shuffleDuration+"s");

        startTime = System.currentTimeMillis();
        for(int i = 0; i<ipList.size(); ++i){
            String hostname = ipList.get(i); 
            launchReduce(hostname, i);
        }
        endTime =System.currentTimeMillis();
        double reduceDuration = (endTime - startTime)/1000.0;
        System.out.println("REDUCE FINISHED: took " + reduceDuration+"s");

        System.out.println("RESULTS :");
        for(int i = 0; i<ipList.size(); ++i){
            String hostname = ipList.get(i); 
            fetchResults(hostname);
        }

    }

    private static int splitFile(String file_name, int processNumber) throws IOException{
        
        File file = new File(file_name);
    
        int totalSize =  (int) file.length();
        int splitSize = (int) (totalSize/processNumber);
    
        FileInputStream fs = new FileInputStream(file);
    
        byte[] data = new byte[totalSize];
        fs.read(data);
        ArrayList<Integer> splitArray = new ArrayList<Integer>();
        splitArray.add(0);

        ProcessBuilder pb = new ProcessBuilder("rm", "-rf", "../Resources/splits");
        Process p = pb.start();


        File directory = new File("../Resources/splits/");
        directory.mkdir();

    
        int startOffset = 0;
        for(int i = 0 ; i<processNumber-1 ; ++i ){
            int j=startOffset;
            if(i!=0 && i!=processNumber-2){
                while((char)data[splitSize*i+j]!=' ' && (char)data[splitSize*i+j]!='\n' ){
                    j++;
                }
                splitArray.add(splitSize*i+j);
            }
            startOffset += j;
        }
        splitArray.add(totalSize);
    
        //System.out.println(data.length);


        for(int i = 0 ; i<splitArray.size()-1 ; i++ ){
            byte[] currentByteArray = new byte[splitSize];
            //System.out.println("start: "+splitArray.get(i) + " , stop: "+splitArray.get(i+1));
            currentByteArray = Arrays.copyOfRange(data, splitArray.get(i), splitArray.get(i+1));
            File currentSplit = new File(directory,"S"+i+".txt");
            currentSplit.createNewFile();
    
            PrintWriter pw = new PrintWriter(currentSplit);
            pw.write(new String(currentByteArray, Charset.defaultCharset()));
            pw.flush();
            pw.close();
        }
    

        return 0;
    }
    

    private static int sendSplit(String hostname,int number) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "mkdir /tmp/Adam/splits");
        Process p = pb.start();
        
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println(hostname + " : mkdir failure");
            p.destroy();
            return -1 ;
        }

        pb = new ProcessBuilder("scp","../Resources/splits/S"+number+".txt",  "achader@"+hostname+":/tmp/Adam/splits");
        p = pb.start();

        timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            //System.out.println(hostname + " : split send failure");
            p.destroy();
            return -1 ;
        }else{
            //System.out.println(hostname + " : split send success");
            return 0;
        }
    }



    private static int launchMap(String hostname,int number) throws InterruptedException, IOException{
        //System.out.println("Map : " + hostname);

        ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "java -jar /tmp/Adam/slave.jar 0 S"+number+".txt");
        Process p = pb.start();
        InputStream is = p.getInputStream();
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (timeoutStatus){
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null){
                //System.out.println(hostname +": " + line);
            }
        } else {
            System.out.println("TIMEOUT");
            p.destroy();
        }


        return 0;

    }

    private static int launchShuffle(String hostname,int number) throws InterruptedException, IOException{
        //System.out.println("Shuffle : " + hostname);

        ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "java -jar /tmp/Adam/slave.jar 1 UM"+number+".txt");
        Process p = pb.start();
        InputStream is = p.getInputStream();
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (timeoutStatus){
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null){
                //System.out.println(hostname +": " + line);
            }
        } else {
            System.out.println("TIMEOUT");
            p.destroy();
        }
        return 0;
    }

    private static int launchReduce(String hostname,int number) throws InterruptedException, IOException{
        //System.out.println("Shuffle : " + hostname);

        ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "java -jar /tmp/Adam/slave.jar 2");
        Process p = pb.start();
        InputStream is = p.getInputStream();
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (timeoutStatus){
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null){
                System.out.println(hostname +": " + line);
            }
        } else {
            System.out.println("TIMEOUT");
            p.destroy();
        }
        return 0;
    }



    private static int sendIp(String hostname) throws InterruptedException, IOException{
        //System.out.println("Sending ip list : " + hostname);


        ProcessBuilder pb = new ProcessBuilder("scp","../Resources/ip_list", "achader@"+hostname+":/tmp/Adam/machines.txt" );
        Process p = pb.start();
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            //System.out.println(hostname + " : send failure");
            p.destroy();
            return -1 ;
        }

        //System.out.println(hostname + " : send success");

        return 0;
    }

    private static int fetchResults(String hostname) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "cat /tmp/Adam/gathered_reduce.txt" );
        Process p = pb.start();
        InputStream is = p.getInputStream();
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (timeoutStatus){
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null){
                System.out.println(line);
            }
        } else {
            System.out.println("TIMEOUT");
            p.destroy();
        }


        return 0;
    }
}