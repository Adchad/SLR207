import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class App {
    static int secondsTimeout = 10;
    static String file_name;
    static ArrayList<String> machineList;
    static int nbMachines;

    public static void main(String[] args) throws Exception {
        
        int mode = Integer.valueOf(args[0]);
        if(mode !=2){
            file_name = args[1];
        }
        
        machineList = parseMachine();
        nbMachines = machineList.size();

        if(mode == 0){
            map();
        }
        else if(mode == 1){
            shuffle();
        }
        else if(mode == 2){
            reduce();
            gather();
        }
    }


    private static int map() throws InterruptedException, IOException{
        
        String output_name = "UM" + file_name.charAt(1) + ".txt";

        ProcessBuilder pb = new ProcessBuilder("mkdir" ,  "-p" , "/tmp/Adam/maps");
        Process p = pb.start();

        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println("mkdir failure");
            p.destroy();
            return -1;
        }

        FileReader     fr = new FileReader("/tmp/Adam/splits/"+ file_name) ;
        BufferedReader br = new BufferedReader(fr) ;
        Scanner        sc = new Scanner(br) ;

        File output = new File("/tmp/Adam/maps/" + output_name);

        output.createNewFile();
        FileWriter fw = new FileWriter(output);

        while(sc.hasNext()){
            fw.write(sc.next() + " 1\n" );
        }

        fw.flush();
        fw.close();
        sc.close();
        br.close();

        System.out.println("map success");
        return 0;
    }


    private static int shuffle() throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("mkdir" ,  "-p" , "/tmp/Adam/shuffles");
        Process p = pb.start();

        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println("mkdir failure");
            p.destroy();
            return -1;
        }

        FileReader     fr = new FileReader("/tmp/Adam/maps/"+ file_name) ;
        BufferedReader br = new BufferedReader(fr) ;
        Scanner        sc = new Scanner(br) ;

        while(sc.hasNextLine()){
            String line = sc.nextLine();
            String word = line.split(" ")[0];
            int hash = word.hashCode();
            File output = new File( "/tmp/Adam/shuffles/" + hash + "-" + java.net.InetAddress.getLocalHost().getHostName() +".txt");
            output.createNewFile();
            
            FileWriter fw = new FileWriter(output, true);
            fw.write(line+"\n");
            fw.flush();
            fw.close();

            int numeroMachine = hash % nbMachines;

            createDir(machineList.get(numeroMachine));
            sendFile(machineList.get(numeroMachine), output.getAbsolutePath());

        }

        sc.close();
        fr.close();

        System.out.println("Shuffle finished");


        return 0;
    }


    private static void createDir(String hostname) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("ssh" ,  "achader@" + hostname , " mkdir -p /tmp/Adam/shufflesreceived");
        Process p = pb.start();

        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println(hostname + " : mkdir failure");
            p.destroy();
            return;
        }

        
        p.destroy();
    }

    private static void sendFile(String hostname, String path) throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("scp" , path,  "achader@" + hostname +":/tmp/Adam/shufflesreceived/");
        Process p = pb.start();

        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println(hostname + " : send failure");
            p.destroy();
            return;
        }

       
        p.destroy();
    }

    private static ArrayList<String> parseMachine() throws FileNotFoundException{
        ArrayList<String> machineList = new ArrayList<String>();

        FileReader     fr = new FileReader("/tmp/Adam/machines.txt") ;
        BufferedReader br = new BufferedReader(fr) ;
        Scanner        sc = new Scanner(br) ;

        while(sc.hasNextLine()){
            String line = sc.nextLine();
            machineList.add(line);
        }
        return machineList;
    }


    private static void reduce() throws IOException, InterruptedException{
        ProcessBuilder pb = new ProcessBuilder("mkdir" ,  "-p" , "/tmp/Adam/reduces");
        Process p = pb.start();
        boolean timeoutStatus = p.waitFor(secondsTimeout, TimeUnit.SECONDS);

        if (!timeoutStatus){
            System.out.println("mkdir failure");
            p.destroy();
            return;
        }

        File shuffleReceivedDir = new File("/tmp/Adam/shufflesreceived");

        String[] shufflesList = shuffleReceivedDir.list();
        HashMap<String, Integer> reduceMap = new HashMap<String, Integer>();

        for(String pathname : shufflesList){
            FileReader     fr = new FileReader("/tmp/Adam/shufflesreceived/"+pathname) ;
            BufferedReader br = new BufferedReader(fr) ;
            Scanner        sc = new Scanner(br) ;

            while(sc.hasNextLine()){
                String word = sc.nextLine().split(" ")[0];
                int val = 0;
                if(reduceMap.containsKey(word)){
                    val = reduceMap.get(word);
                }
                reduceMap.put(word,val+1);
            }
        }

        for( Map.Entry<String,Integer> el : reduceMap.entrySet() ){
            int hash = el.getKey().hashCode();
            File destFile = new File("/tmp/Adam/reduces/"+hash+".txt");
            destFile.createNewFile();
            FileWriter fw = new FileWriter(destFile);
            fw.write(el.getKey() + " " + el.getValue());
            fw.flush();
            fw.close();
        }


        
    }


    private static void gather() throws IOException{

        File reducedDir = new File("/tmp/Adam/reduces");

        String[] reducedFiles = reducedDir.list();
        
        File gathered = new File("/tmp/Adam/gathered_reduce.txt");

        gathered.createNewFile();
        FileWriter fw = new FileWriter(gathered);

        for(String file_name : reducedFiles){
            File currentFile = new File(reducedDir, file_name);
            Scanner sc = new Scanner(currentFile);

            fw.write(sc.nextLine()+"\n");
            fw.flush();
            
        }
        fw.close();

    }
}
