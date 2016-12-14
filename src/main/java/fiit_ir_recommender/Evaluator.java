package fiit_ir_recommender;

import fiit_ir_recommender.entity.Activity;
import fiit_ir_recommender.entity.User;
import fiit_ir_recommender.utils.Ident;
import fiit_ir_recommender.utils.PrecisionAtK;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Gamer on 12/6/2016.
 */
public class Evaluator {

    public static void evaluate(User testUser, List<Ident> recommendations) {
        int TP = 0;

    }

    private static long getId(Activity a, boolean use_deal_id) {
        if(use_deal_id)
            return a.deal_id;

        return a.dealitem_id;
    }

    private static long getId(Ident i, boolean use_deal_id) {
        if(use_deal_id)
            return i.deal_id;

        return i.dealitem_id;
    }

    public static PrecisionAtK computePrecisionAtK(Map.Entry<User,List<Ident>> rEntry, boolean use_deal_id){
        int TP = 0;
        PrecisionAtK pak = new PrecisionAtK();
        User u = rEntry.getKey();
        List<Ident> recommendations = rEntry.getValue();

        Set<Long> ids = new HashSet<>();
        for(Activity a:u.activities) {
            ids.add(getId(a,use_deal_id));
        }

        int cnt = 0;
        for(Ident i:recommendations) {
            if(cnt++ >= PrecisionAtK.MAX_K) break;

            if(ids.contains(getId(i,use_deal_id))) TP++;
            pak.addTpAtK(cnt,TP);
            pak.addNAtK(cnt,Math.min(cnt,u.activities.size()));

        }

        return pak;
    }

    public static int getTp(Map.Entry<User,List<Ident>> rEntry, boolean use_deal_id){
        int TP = 0;
        User u = rEntry.getKey();
        List<Ident> recommendations = rEntry.getValue();

        Set<Long> ids = new HashSet<>();
        for(Activity a:u.activities) {
            ids.add(getId(a,use_deal_id));
        }

        for(Ident i:recommendations) {
            if(ids.contains(getId(i,use_deal_id))) TP++;
        }

        return TP;
    }

    public static int getN(Map.Entry<User,List<Ident>> rEntry){
        return Math.min(
                rEntry.getKey().activities.size(),
                rEntry.getValue().size());
    }

    public static double nDCG(Map.Entry<User,List<Ident>> rEntry, boolean use_deal_id) {
        double res = 0;

        User u = rEntry.getKey();
        List<Ident> recommendations = rEntry.getValue();

        Set<Long> ids = new HashSet<>();
        for(Activity a:u.activities) {
            ids.add(getId(a,use_deal_id));
        }

        int cnt = 0;
        int tp = 0;
        for(Ident i:recommendations) {
            cnt++;

            if(ids.contains(getId(i,use_deal_id))){
                if(cnt == 1) {
                    res += 1;
                } else {
                    res += 1/Math.log(cnt);
                }
                tp++;
            }
        }

        double idcg = 0;
        for(int i = 1; i <= tp; i++) {
            idcg += (i==1)?1:1/Math.log(i);
        }

        return (idcg>0)?res/idcg:0;
    }
}
