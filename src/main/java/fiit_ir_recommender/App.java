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
            int TP = results.entrySet().parallelStream()
                        .mapToInt(Evaluator::getTp)
                        .sum();
            // count N - min(tp+fp,activity.size())
            int N = results.entrySet().parallelStream()
                    .mapToInt(Evaluator::getN)
                    .sum();

            // count PrecisionAtK for every recommendation
            List<PrecisionAtK> finalPak = Collections.synchronizedList(new ArrayList<>());
            results.entrySet()
                    .parallelStream()
                    .forEach((entry) -> finalPak.add(Evaluator.computePrecisionAtK(entry)));

            // Sum every Precision@K together for average
            PrecisionAtK avg = finalPak.parallelStream()
                    .reduce((precisionAtK, precisionAtK2) -> precisionAtK.combineWith(precisionAtK2))
                    .get();

            // Average nDCG
            double nDCG = results.entrySet().parallelStream()
                            .mapToDouble(Evaluator::nDCG)
                            .average().getAsDouble();


            endTime = System.currentTimeMillis();
            System.out.println("Eval: " + (endTime - startTime) + " milliseconds");

            // PRINT RESULTS & compute TP/N when needed
            System.out.println("Precision@K:");
            for(int i = 1; i<=PrecisionAtK.MAX_K; i++) {
                System.out.println(String.format(
                        "K: %d tp: %d n: %d -> %f",
                        i,avg.getTpAtK(i),avg.getNAtK(i),
                        (double)avg.getTpAtK(i)/(double)avg.getNAtK(i)
                ));
            }



            System.out.println();
            System.out.println("Precision:");
            System.out.println("TP: "+TP);
            System.out.println("N: "+N);
            System.out.println((double) TP/(double) N);

            System.out.println();
            System.out.println("Average nDCG: "+nDCG);

//            KMeansPlusPlusClusterer<User> cluterer = new KMeansPlusPlusClusterer<User>(20);
//            List<CentroidCluster<User>> clusters = cluterer.cluster(users.values());
//            clusters.size();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
