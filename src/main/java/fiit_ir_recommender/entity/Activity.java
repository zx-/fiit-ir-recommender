package fiit_ir_recommender.entity;

import com.csvreader.CsvReader;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Robert Cuprik on 11/23/2016.
 */
public class Activity {

    public long id;
    public long user_id;
    public long dealitem_id;
    public long deal_id;
    public long quantity;
    public double market_price;
    public double team_price;
    public long create_time;

    public static ArrayList<Activity> createFromCSV(CsvReader r) throws IOException {
        ArrayList<Activity> a = new ArrayList<>();

        r.readHeaders();
        while(r.readRecord()){
            a.add(new Activity(r.getValues()));
        }

        return a;
    }

    public Activity(String row[]){
        id = Long.valueOf(row[0]);
        user_id = Long.valueOf(row[1]);
        dealitem_id = Long.valueOf(row[2]);
        deal_id = Long.valueOf(row[3]);
        quantity = Long.valueOf(row[4]);
        market_price = Double.valueOf(row[5]);
        team_price = Double.valueOf(row[6]);
        create_time = Long.valueOf(row[7]);
    }

}
