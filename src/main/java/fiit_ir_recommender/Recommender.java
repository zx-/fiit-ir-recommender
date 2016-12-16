package fiit_ir_recommender;

import fiit_ir_recommender.entity.DealItem;
import fiit_ir_recommender.entity.User;
import fiit_ir_recommender.utils.Config;
import fiit_ir_recommender.utils.ElasticWrapper;
import fiit_ir_recommender.utils.Ident;
import fiit_ir_recommender.utils.PrioQWrap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.collect.HppcMaps;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;

import java.util.*;

/**
 * Created by Gamer on 12/4/2016.
 */
public class Recommender {

    final double DI_C = 0.7;
    final double D_ID_C = 0.6;
    final double PRICE_C = 0.3;
    final double BUY_COUNT_C = PRICE_C;
    final int Q_SIZE = Config.NUMBER_OF_NEIGHBOURS;

    public static String RETURN_FIELD = "id";

    private final ElasticWrapper ew;

    public Recommender(ElasticWrapper e) { this.ew = e;}

    public List<Ident> recommendToUser(
            User testUser,
            Map<Long,User> trainUsers,
            HashMap<Long,DealItem> trainDealsHash,
            HashMap<Long,DealItem> testDealsHash){

        List<Ident> mostSold = Config.USE_MOST_SOLD?recommendNewUser(testUser):new ArrayList<>();

        if(!trainUsers.containsKey(testUser.getId())){
            //System.out.println("New user");
            return mostSold;
        }

        User userHistory = trainUsers.get(testUser.getId());
        List<PrioQWrap> neighbourhood = topClosest(userHistory,trainUsers);
        PrioQWrap currentUser = new PrioQWrap();
        currentUser.user = testUser;
        neighbourhood.add(currentUser);



        return combine(mostSold,ew.findSimilar(neighbourhood,testUser),userHistory);
    }

    private List<Ident> combine(List<Ident> mostSold, List<SearchResponse> similar, User userHistory) {

        List<Ident> sim = new ArrayList<>();

        for(SearchResponse r: similar) {
            for(SearchHit h:r.getHits()){
                Map<String, Object> fields = h.getSource();
                Ident i = new Ident(fields);
                sim.add(i);
            }
        }

        sim.sort((Ident i1,Ident i2) -> i2.timesSold - i1.timesSold);
        mostSold.sort((Ident i1,Ident i2) -> i2.timesSold - i1.timesSold);

        Set<Long> res = new HashSet<>();
        List<Ident> resultRecommendation = new ArrayList<>();
        boolean takeSim = true;
        int sim_i = 0,most_i =0;
        int counter = 0;

        if(sim.size() == 0) System.out.println("No similar found");

        if(Config.USE_MOST_SOLD) {
            while(res.size() != 10) {
                if(sim_i >= sim.size()) {
                    takeSim = false;
                }

                Ident candidate = (takeSim)?sim.get(sim_i++):mostSold.get(most_i++);
                takeSim = !takeSim;

                if(!res.contains(candidate.dealitem_id)) {
                    res.add(candidate.dealitem_id);
                    resultRecommendation.add(candidate);
                }

                if (counter++ > 100) break; // pre istotu :)
            }
        } else {

            int i = 0;
            while(res.size() < 10 && i < sim.size()){

                Ident candidate = sim.get(i++);

                if(!res.contains(candidate.dealitem_id)) {
                    res.add(candidate.dealitem_id);
                    resultRecommendation.add(candidate);
                }
            }

        }

        return  resultRecommendation;
    }

    public Ident hitToident(SearchHit h){
        Map<String, Object> fields = h.getSource();
        return new Ident( fields );
    }

    public List<Ident> recommendNewUser(User testUser){
        SearchResponse res = ew.newUserQuery(testUser);

        List<Ident> bestIds = new ArrayList<>();

        for(SearchHit h:res.getHits()){
            Map<String, Object> fields = h.getSource();
            Ident i = new Ident(fields);
            //System.out.println(i);
            bestIds.add(i);
        }

        return bestIds;
    }

    public double computeSimilarity(User a, User b) {
        if(a.activities.size() == 0 || b.activities.size() == 0)
            return 0;

        Set<Long> dealIds = new HashSet<>(a.dealIds);
        dealIds.retainAll(b.dealIds);

        Set<Long> dealitemIds = new HashSet<>(a.dealitemIds);
        dealitemIds.retainAll(b.dealitemIds);

        int a_num = a.getNumOfPurchases();
        int b_num = b.getNumOfPurchases();

        double a_price = a.getAverageTeamPrice();
        double b_price = b.getAverageTeamPrice();


        return DI_C * dealitemIds.size()
                + D_ID_C * dealIds.size()
                + PRICE_C * 1/( Math.max(a_price,b_price)/Math.min(a_price,b_price) )
                + BUY_COUNT_C * 1/( Math.max(a_num,b_num)/Math.min(a_num,b_num) );
    }

    public List<PrioQWrap> topClosest(User userHistory,Map<Long,User> trainUsers) {

        if(Q_SIZE == 0) return new ArrayList<>();

        PriorityQueue<PrioQWrap> front = new PriorityQueue<>(
                Q_SIZE,
                (PrioQWrap o1,PrioQWrap o2) -> Double.compare(o1.similarity,o2.similarity));

        for(User u:trainUsers.values()) {
            double sim = computeSimilarity(userHistory,u);
            if(sim > 0) {

                PrioQWrap w = new PrioQWrap();
                w.similarity = sim;
                w.user = u;

                front.add(w);
                if(front.size() > Q_SIZE) front.poll();
            }
        }

        return new ArrayList<>(front);
    }

}
