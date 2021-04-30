import java.io.*;
import java.util.*;

public class App {
    public static void main(String[] args) throws Exception {
        FileReader     fr = new FileReader("input.txt") ;
        BufferedReader br = new BufferedReader(fr) ;
        Scanner        sc = new Scanner(br) ;
        long startTime = System.currentTimeMillis();

        HashMap<String,Integer> map = new HashMap<String,Integer>();
        
        while(sc.hasNext()){
            String tmp = sc.next(); 
            int val = 0;
            if(map.containsKey(tmp)){
                val = map.get(tmp);
            }
            map.put(tmp,val+1);

            
        }

        printSorted(map,true);

        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("Time : "  + totalTime +" ms" );
        sc.close();
        fr.close();

    }

    private static void printSorted(HashMap map, boolean output){
        ArrayList<String> keyList = new ArrayList<String>(map.keySet());
        ArrayList<Integer> valList = new ArrayList<Integer>();

        Collections.sort(keyList);

        for(String key : keyList){
            valList.add((Integer)map.get(key));
        }
        while(valList.size()>0){
            int max = 0;
            int max_id = 0;
            for( int i=0; i<keyList.size(); ++i){
                if(valList.get(i) > max) {
                    max = valList.get(i);
                    max_id = i;
                }
            }

            if(output){System.out.println(keyList.get(max_id) + " " +valList.get(max_id));}
            valList.remove(max_id);
            keyList.remove(max_id);

        }
        
    }

    
}
