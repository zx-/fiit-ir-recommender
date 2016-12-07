package fiit_ir_recommender.utils;

import java.util.Map;

/**
 * Created by Gamer on 12/4/2016.
 */
public class Ident {
    public long deal_id;
    public long dealitem_id;
    public int timesSold = 0;
    public Map<String, Object> fields;

    public Ident(Map<String, Object> f){
        this.deal_id = new Long((int)f.get("deal_id"));
        this.dealitem_id = new Long((int)f.get("id"));
        this.fields = f;

        Object i = f.get("times_sold");
        if(i != null) {
            this.timesSold = (int) i;
        }
    }

    @Override
    public String toString() {
       return String.format("deal_id: %d dealitem_id: %d",deal_id,dealitem_id);
    }
}
