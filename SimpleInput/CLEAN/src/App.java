import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {
    private static int secondsTimeout = 5;
    public static void main(String[] args) throws Exception {
        System.out.println("CLEAN...");

        FileReader     fr = new FileReader("../Resources/ip_list") ;
        BufferedReader bre = new BufferedReader(fr) ;
        Scanner        sc = new Scanner(bre) ;

        while(sc.hasNextLine()){
            String hostname = sc.nextLine();
            ProcessBuilder pb = new ProcessBuilder("ssh", "achader@"+hostname, "hostname");
            Process p = pb.start();
            InputStream is = p.getInputStream();
            boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

            if (timeoutStatus){
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
    
                while ((line = br.readLine()) != null){
                    System.out.println("Clean for : "+ line);
                }
                InputStream es = p.getErrorStream();
                BufferedReader ber = new BufferedReader(new InputStreamReader(es));
                String eLine;
                while ((eLine = ber.readLine()) != null){
                    System.out.println("Error : "+ eLine);
                }
                
                clean(hostname);
                
            } else {
                System.out.println("TIMEOUT");
                p.destroy();
            }
        }

    }


    private static void clean(String hostname) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ssh" ,  "achader@" + hostname , " rm -rf /tmp/Adam/");
        Process p = pb.start();

        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println(hostname + " : cleaning failure");
            p.destroy();
            return;
        }
        System.out.println(hostname + " : cleaning success");


    }
}
