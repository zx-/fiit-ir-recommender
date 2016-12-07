package fiit_ir_recommender.entity;

import org.apache.commons.math3.ml.clustering.Clusterable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.OptionalDouble;
import java.util.Set;

/**
 * Created by Robert Cuprik on 11/26/2016.
 */
public class User implements Clusterable {

    public static long maxItemId = 0;
    public static int numOfOtherDims = 3;

    public long id;
    private double[] location =null;

    public ArrayList<Activity> activities = new ArrayList<>();
    public Set<Long> dealIds = new HashSet<>();
    public Set<Long> dealitemIds = new HashSet<>();

    @Override
    public double[] getPoint() {
        if (location != null) {
            return location;
        }

        return generateLocation();
    }

    private double[] generateLocation() {
        location = new double[(int) maxItemId + numOfOtherDims + 1];

        for(Activity a:activities) {
            location[(int) a.deal_id]++;
        }

        int end = (int) maxItemId;
        location[end] = activities.size(); // num of purchases


        location[end+1] = activities // average team price
                .stream()
                .mapToDouble(a -> a.team_price)
                .average()
                .getAsDouble();

        location[end+2] = activities // average market price
                .stream()
                .mapToDouble(a -> a.market_price)
                .average()
                .getAsDouble();

        return location;
    }

    public double getAverageTeamPrice(){
        return activities // average team price
                .stream()
                .mapToDouble(a -> a.team_price)
                .average()
                .getAsDouble();
    }

    public int getNumOfPurchases(){
        return activities.size();
    }

    /**
     * Returns user_id
     */
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void addActivity(Activity a) {
        dealitemIds.add(a.dealitem_id);
        dealIds.add(a.deal_id);
        activities.add(a);
    }

    public ArrayList<Activity> getActivities() {
        return activities;
    }
}
