package fiit_ir_recommender.utils;

import com.csvreader.CsvReader;
import fiit_ir_recommender.entity.Activity;
import fiit_ir_recommender.entity.DealDetails;
import fiit_ir_recommender.entity.DealItem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by Robert Cuprik on 11/26/2016.
 */
public class DataLoader {

    public final static String TEST_ACTIVITY_PATH = "data\\test_activity_v2.csv";
    public final static String TRAIN_ACTIVITY_PATH = "data\\train_activity_v2.csv";
    public final static String TEST_DEAL_DETAILS_PATH = "data\\test_deal_details.csv";
    public final static String TRAIN_DEAL_DETAILS_PATH = "data\\train_deal_details.csv";
    public final static String TEST_DEALITEMS_PATH = "data\\test_dealitems.csv";
    public final static String TRAIN_DEALITEMS_PATH = "data\\train_dealitems.csv";

    public static List<Activity> loadActivities(String path) throws IOException {
        CsvReader a = new CsvReader(path,',', StandardCharsets.UTF_8);
        return Activity.createFromCSV(a);
    }

    public static List<DealItem> loadDealItems(String path) throws IOException {
        CsvReader di = new CsvReader(path,',',StandardCharsets.UTF_8);
        return DealItem.createFromCSV(di);
    }

    public static List<DealDetails> loadDealDetails(String path) throws IOException {
        CsvReader dd = new CsvReader(path,',',StandardCharsets.UTF_8);
        return DealDetails.createFromCSV(dd);
    }


}
