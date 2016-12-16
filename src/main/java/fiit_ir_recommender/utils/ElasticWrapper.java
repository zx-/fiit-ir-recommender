package fiit_ir_recommender.utils;

import fiit_ir_recommender.entity.Activity;
import fiit_ir_recommender.entity.DealItem;
import fiit_ir_recommender.entity.User;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

/**
 * Created by Gamer on 12/1/2016.
 */
public class ElasticWrapper {
    TransportClient client;

    public ElasticWrapper (String address) throws UnknownHostException {
        client = TransportClient.builder().build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(address), 9300));

    }

    public ElasticWrapper() throws UnknownHostException {
        this("http://rarcoo.synology.me");
    }

    public HashMap<Long,DealItem> parseDeals(List<DealItem> deals, List<Activity> activities, boolean isTrain, boolean persist) throws IOException {
        HashMap<Long,DealItem> dealHash = new HashMap<>();

        int i = 0;
        for(DealItem d:deals) {
            dealHash.put(d.id,d);
        }

        DealItem d;
        for(Activity a:activities) {

            d = dealHash.get(a.dealitem_id);
            if(d == null) {
                i++;
            } else {
                d.accumulatePrice(a.team_price);
                d.addSold();
            }
        }

        System.out.println(i);

        int j = 0;
        for(DealItem d_fin: dealHash.values()){

            d_fin.makeAverage();
            if(persist)
                client.prepareIndex("deals", "deal")
                        .setSource(d_fin.getElDocument(isTrain))
                        .get();
            j++;

        }
        //System.out.println(j);

        return dealHash;

    }

    public BoolQueryBuilder shouldsByActivitiesTime(List<Activity> activities) {
        BoolQueryBuilder shoulds =  QueryBuilders.boolQuery();

        for(Activity a:activities) {
            shoulds.should(QueryBuilders
                    .boolQuery()
                    .must(rangeQuery("begin_time").lte(a.create_time))
                    .must(rangeQuery("end_time").gte(a.create_time)));
        }

        return shoulds;
    }

    public SearchResponse newUserQuery(User u){
        List<Activity> activities = u.getActivities();
        BoolQueryBuilder shoulds =  shouldsByActivitiesTime(activities);

        QueryBuilder qb = QueryBuilders
                .boolQuery()
                .must(QueryBuilders.termQuery("is_train",false))
                .must(shoulds);

        SearchRequestBuilder response = client
                .prepareSearch("deals")
                .setTypes("deal")
                .setQuery(qb)
                .addSort("times_sold", SortOrder.DESC)
                .setSize(10)
                .setFetchSource(true);

        SearchResponse res = response.get();
        return res;
    }

    public List<String> findDocElIds (List<PrioQWrap> docs) {
        List<String> ids = new ArrayList<>();
        Set<Long> qIds = new HashSet<>();

        for(PrioQWrap p:docs){
            qIds.addAll(p.user.getActivities()
                    .stream()
                    .map(a -> a.dealitem_id)
                    .collect(Collectors.toList()));
        }

        SearchRequestBuilder response = client
                .prepareSearch("deals")
                .setTypes("deal")
                .setQuery(QueryBuilders.termsQuery("id",qIds))
                .addField("_id")
                .setSize(qIds.size());

        //System.out.println(response.toString());
        //System.out.println("Query ids");
        SearchResponse res = response.get();

        for(SearchHit h:res.getHits()){
            ids.add(h.getId());
        }

        return ids;
    }

    public List<SearchResponse> findSimilar(List<PrioQWrap> docs, User testUser) {

        List<String> idsToSim = findDocElIds(docs);
        BoolQueryBuilder shouldsTimes =  shouldsByActivitiesTime(testUser.activities);

        List<QueryBuilder> mlts =  new ArrayList<>();

        for(String id:idsToSim) {
            mlts.add(
                    QueryBuilders
                            .moreLikeThisQuery("title", "text1")
                            .addLikeItem(new MoreLikeThisQueryBuilder.Item("deals","deal",id))
                            .minTermFreq(2)
                            .maxQueryTerms(12)
            );
        }
        List<SearchResponse> responses = new ArrayList<>();

        for(QueryBuilder mlt:mlts) {
            QueryBuilder qb = QueryBuilders
                    .boolQuery()
                    .must(QueryBuilders.termQuery("is_train",false))
                    .must(rangeQuery("times_sold").gte(Config.LEAST_TIMES_SOLD))
                    .must(shouldsTimes)
                    .must(mlt);

            SearchRequestBuilder response = client
                    .prepareSearch("deals")
                    .setTypes("deal")
                    .setQuery(qb)
                    .setFetchSource(true)
                    .setSize(Config.NUMBER_OF_DOC_FETCH);

            //System.out.println(response.toString());
            //System.out.println("Query mlt");
            responses.add(response.get());
        }
        return responses;
    }

}
