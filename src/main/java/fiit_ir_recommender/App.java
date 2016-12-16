package fiit_ir_recommender;

import fiit_ir_recommender.entity.Activity;
import fiit_ir_recommender.entity.DealItem;
import fiit_ir_recommender.entity.User;
import fiit_ir_recommender.utils.*;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class App {

    public static void main( String[] args ){
        try {

            // INIT
            // load files & init maps
            List<Activity> trainActivities = DataLoader.loadActivities(DataLoader.TRAIN_ACTIVITY_PATH);
            Map<Long,User> trainUsers = UserBuilder
                    .createUsersByActivity(trainActivities);

            List<DealItem> trainDealItems = DataLoader.loadDealItems(DataLoader.TRAIN_DEALITEMS_PATH);

            ElasticWrapper el = new ElasticWrapper("192.168.3.3"); //elastic
            HashMap<Long,DealItem> trainDealsHash = el.parseDeals(trainDealItems,trainActivities,true,false);


            List<Activity> testActivities = DataLoader.loadActivities(DataLoader.TEST_ACTIVITY_PATH);
            Map<Long,User> testUsers = UserBuilder
                    .createUsersByActivity(testActivities);

            List<DealItem> testDealItems = DataLoader.loadDealItems(DataLoader.TEST_DEALITEMS_PATH);
            HashMap<Long,DealItem> testDealsHash = el.parseDeals(testDealItems,testActivities,false,false);

            // create recommender with elastic
            Recommender e = new Recommender(el);

            long startTime = System.currentTimeMillis();

            ConcurrentHashMap<User,List<Ident>> results = new ConcurrentHashMap<>();

            //compute recommendations for testUsers and push them to results
            testUsers.values()
                    .parallelStream()
                    //.limit(3000)
                    .forEach(user -> {
                        List<Ident> res = e.recommendToUser(user,trainUsers,trainDealsHash,testDealsHash);
                        results.put(user,res);
                    });

            long endTime = System.currentTimeMillis();
            System.out.println("Recommendation: " + (endTime - startTime) + " milliseconds");


            // EVALUATION
            startTime = System.currentTimeMillis();

            // count TP
            int TP = results.entrySet()
                        .parallelStream()
                        .mapToInt(entry -> Evaluator.getTp(entry,false))
                        .sum();

            int TP_deal_id = results.entrySet()
                    .parallelStream()
                    .mapToInt(entry -> Evaluator.getTp(entry,true))
                    .sum();

            // count N - min(tp+fp,activity.size())
            int N = results.entrySet()
                    .parallelStream()
                    .mapToInt(Evaluator::getN)
                    .sum();

            // count PrecisionAtK for every recommendation
            List<PrecisionAtK> finalPak = Collections.synchronizedList(new ArrayList<>());
            results.entrySet()
                    .parallelStream()
                    .forEach((entry) -> finalPak.add(Evaluator.computePrecisionAtK(entry,false)));

            // Sum every Precision@K together for average
            PrecisionAtK avg = finalPak
                    .parallelStream()
                    .reduce((precisionAtK, precisionAtK2) -> precisionAtK.combineWith(precisionAtK2))
                    .get();

            // deal_id
            List<PrecisionAtK> finalPak_deal_id = Collections.synchronizedList(new ArrayList<>());
            results.entrySet()
                    .parallelStream()
                    .forEach((entry) -> finalPak_deal_id.add(Evaluator.computePrecisionAtK(entry,true)));

            PrecisionAtK avg_deal_id = finalPak_deal_id
                    .parallelStream()
                    .reduce((precisionAtK, precisionAtK2) -> precisionAtK.combineWith(precisionAtK2))
                    .get();


            // Average nDCG
            double nDCG = results.entrySet()
                            .parallelStream()
                            .mapToDouble(entry -> Evaluator.nDCG(entry,false))
                            .average().getAsDouble();

            double nDCG_deal_id = results.entrySet()
                    .parallelStream()
                    .mapToDouble(entry -> Evaluator.nDCG(entry,true))
                    .average().getAsDouble();


            endTime = System.currentTimeMillis();
            System.out.println("Eval: " + (endTime - startTime) + " milliseconds");

            // PRINT RESULTS & compute TP/N when needed
            System.out.println("Precision@K (dealitem_id):");
            for(int i = 1; i<=PrecisionAtK.MAX_K; i++) {
                System.out.println(String.format(
                        "K: %d tp: %d n: %d -> %f",
                        i,avg.getTpAtK(i),avg.getNAtK(i),
                        (double)avg.getTpAtK(i)/(double)avg.getNAtK(i)
                ));
            }
            System.out.println();
            System.out.println("Precision@K (deal_id):");
            for(int i = 1; i<=PrecisionAtK.MAX_K; i++) {
                System.out.println(String.format(
                        "K: %d tp: %d n: %d -> %f",
                        i,avg_deal_id.getTpAtK(i),avg_deal_id.getNAtK(i),
                        (double)avg_deal_id.getTpAtK(i)/(double)avg_deal_id.getNAtK(i)
                ));
            }

            System.out.println();
            System.out.println("Precision (dealitem_id):");
            System.out.println("TP: "+TP);
            System.out.println("N: "+N);
            System.out.println((double) TP/(double) N);
            System.out.println();
            System.out.println("Precision (deal_id):");
            System.out.println("TP: "+TP_deal_id);
            System.out.println("N: "+N);
            System.out.println((double) TP_deal_id/(double) N);



            System.out.println();
            System.out.println("Average nDCG (dealitem_id): "+nDCG);
            System.out.println("Average nDCG (deal_id): "+nDCG_deal_id);

//            KMeansPlusPlusClusterer<User> cluterer = new KMeansPlusPlusClusterer<User>(20);
//            List<CentroidCluster<User>> clusters = cluterer.cluster(users.values());
//            clusters.size();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
