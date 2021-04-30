import java.io.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {
    private static int secondsTimeout = 3;
    public static void main(String[] args) throws Exception {
        System.out.println("DEPLOY...");

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
                    System.out.println("Deploy for : "+ line);
                }
                InputStream es = p.getErrorStream();
                BufferedReader ber = new BufferedReader(new InputStreamReader(es));
                String eLine;
                while ((eLine = ber.readLine()) != null){
                    System.out.println("Error : "+ eLine);
                }
                
                deploy(hostname, "../Resources/slave.jar");
                
            } else {
                System.out.println("TIMEOUT");
                p.destroy();
            }
        }

    }


    private static void deploy(String hostname, String filename) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ssh" ,  "achader@" + hostname , " mkdir /tmp/Adam");
        Process p = pb.start();

        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println(hostname + " : mkdir failure");
            p.destroy();
            return;
        }

        pb = new ProcessBuilder("scp", filename ,  "achader@" + hostname + ":/tmp/Adam/");
        p = pb.start();

        timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println(hostname + " : deployment failure");
            p.destroy();
        }else{
            System.out.println(hostname + " : deployment success");
        }
    }
}
