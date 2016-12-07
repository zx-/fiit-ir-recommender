package fiit_ir_recommender.entity;

import com.csvreader.CsvReader;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.ArrayList;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * Created by Robert Cuprik on 11/23/2016.
 */
public class DealItem {
    public long id,
            deal_id;
    public String title_dealitem,
            coupon_text1,
            coupon_text2;
    public long coupon_begin_time,
            coupon_end_time;

    private int times_sold;
    private double average_price;

    public static ArrayList<DealItem> createFromCSV(CsvReader r) throws IOException {
        ArrayList<DealItem> a = new ArrayList<>();

        r.readHeaders();
        while(r.readRecord()){
            a.add(new DealItem(r.getValues()));
        }

        return a;
    }



    public DealItem(String row[]){
        id = Long.valueOf(row[0]);
        deal_id = Long.valueOf(row[1]);
        title_dealitem = row[2];
        coupon_text1 = row[3];
        coupon_text2 = row[4];
        coupon_begin_time = Long.valueOf(row[5]);
        coupon_end_time = Long.valueOf(row[6]);
    }

    public int getTimes_sold() {
        return times_sold;
    }

    public void setTimes_sold(int times_sold) {
        this.times_sold = times_sold;
    }

    public double getAverage_price() {
        return average_price;
    }

    public void setAverage_price(double average_price) {
        this.average_price = average_price;
    }

    public void accumulatePrice(double price) {
        this.average_price += price;
    }

    public void addSold() { this.times_sold++; }

    public void makeAverage() {
        this.average_price = this.average_price/this.times_sold;
    }

    public XContentBuilder getElDocument(boolean isTrain) throws IOException {
        return jsonBuilder()
                .startObject()
                    .field("times_sold", this.getTimes_sold())
                    .field("average_price", this.getAverage_price())
                    .field("deal_id", this.deal_id)
                    .field("id", this.id)
                    .field("title", this.title_dealitem)
                    .field("text1", this.coupon_text1)
                    .field("text2", this.coupon_text2)
                    .field("begin_time", this.coupon_begin_time)
                    .field("end_time", this.coupon_end_time)
                    .field("is_train", isTrain)
                .endObject();
    }


}
