package fiit_ir_recommender.entity;

import com.csvreader.CsvReader;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Robert Cuprik on 11/23/2016.
 */
public class DealDetails {
    long id;
    String title_deal,title_desc,title_city;
    long deal_id, partner_id;
    double gpslat = 0,gpslong = 0;

    public static ArrayList<DealDetails> createFromCSV(CsvReader r) throws IOException {
        ArrayList<DealDetails> a = new ArrayList<>();

        r.readHeaders();
        while(r.readRecord()){
            a.add(new DealDetails(r.getValues()));
        }

        return a;
    }

    public DealDetails(String row[]){
        id = Long.valueOf(row[0]);
        title_deal = row[1];
        title_desc = row[2];
        title_city = row[3];
        deal_id = Long.valueOf(row[4]);
        partner_id = Long.valueOf(row[5]);

        try {
            gpslat = Double.valueOf(row[6]);
            gpslong = Double.valueOf(row[7]);
        } catch (NumberFormatException e) {}
    }
}
