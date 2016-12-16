package fiit_ir_recommender.utils;

import java.util.HashMap;

/**
 * Created by Gamer on 12/7/2016.
 */
public class PrecisionAtK {
    HashMap<Integer,Integer> TPatK = new HashMap<>();
    HashMap<Integer,Integer> NatK = new HashMap<>();
    public static int MAX_K = 10;

    public PrecisionAtK(){
        for(int i = 1; i <= MAX_K; i++){
            addTpAtK(i,0);
            addNAtK(i,0);
        }
    }

    public void addTpAtK(int k,int TP) {
        TPatK.put(k,TP);
    }
    public void addNAtK(int k,int TP) {
        NatK.put(k,TP);
    }

    public double getPrecisionAtK(int k) {
        if(getNAtK(k) == 0) return 0;
        return ((double) getTpAtK(k))/ ((double) getNAtK(k));
    }

    public int getTpAtK(int k) {return TPatK.get(k);}
    public int getNAtK(int k) {return NatK.get(k);}

    public PrecisionAtK combineWith(PrecisionAtK precisionAtK2) {
        precisionAtK2.TPatK.forEach(
                (key,val) -> this.TPatK.merge(key,val, (v1,v2) -> v1+v2 ));

        precisionAtK2.NatK.forEach(
                (key,val) -> this.NatK.merge(key,val, (v1,v2) -> v1+v2 ));

        return this;
    }
}
