package fiit_ir_recommender.utils;

import fiit_ir_recommender.entity.Activity;
import fiit_ir_recommender.entity.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Robert Cuprik on 11/26/2016.
 */
public class UserBuilder {
    /**
     * USING user_id as key
     * @param activities
     * @return
     */
    public static Map<Long,User> createUsersByActivity(List<Activity> activities) {
        HashMap<Long,User> users = new HashMap<>();
        User u;

        for(Activity a:activities) {
            User.maxItemId = Math.max(a.deal_id,User.maxItemId);

            if(users.containsKey(a.user_id)) {
                u = users.get(a.user_id);
                u.addActivity(a);
            }
            else {
                u = new User();
                u.addActivity(a);
                u.setId(a.user_id);
                users.put(u.getId(),u);
            }
        }

        return users;
    }

}
