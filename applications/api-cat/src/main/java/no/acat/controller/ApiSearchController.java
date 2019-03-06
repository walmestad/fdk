package no.acat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.acat.model.ApiDocument;
import no.acat.model.queryresponse.AggregationBucket;
import no.acat.model.queryresponse.QueryResponse;
import no.acat.service.ElasticsearchService;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregator;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Date;

@CrossOrigin
@RestController
@RequestMapping(value = "/apis")
public class ApiSearchController {
    public static final String MISSING = "MISSING";
    public static final int MAX_AGGREGATIONS = 10000;
    public static final String AGGREGATE_API = "/aggregateApi";
    public static final long DAY_IN_MS = 1000 * 3600 * 24;
    private static final Logger logger = LoggerFactory.getLogger(ApiSearchController.class);
    private ElasticsearchService elasticsearch;
    private ObjectMapper mapper;

    @Autowired
    public ApiSearchController(ElasticsearchService elasticsearchService, ObjectMapper mapper) {
        this.elasticsearch = elasticsearchService;
        this.mapper = mapper;
    }

    @ApiOperation(value = "Queries the api catalog for api specifications",
        notes = "So far only simple queries is supported", response = QueryResponse.class)
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public QueryResponse search(
        @ApiParam("the query string")
        @RequestParam(value = "q", defaultValue = "", required = false)
            String query,

        @ApiParam("Filters on publisher's organization path (orgPath), e.g. /STAT/972417858/971040238")
        @RequestParam(value = "orgPath", defaultValue = "", required = false)
            String orgPath,

        @ApiParam("Filters on harvestSourceUri external identifier")
        @RequestParam(value = "harvestSourceUri", defaultValue = "", required = false)
            String harvestSourceUri,

        @ApiParam("Filters on format")
        @RequestParam(value = "format", defaultValue = "", required = false)
            String[] formats,

        @ApiParam("Calculate aggregations")
        @RequestParam(value = "aggregations", defaultValue = "false", required = false)
            String includeAggregations,

        @ApiParam("The title text")
        @RequestParam(value = "title", defaultValue = "", required = false)
            String title,

        @ApiParam("Specifies the sort field, at the present the only value is \"modified\". Default is no value, and results are sorted by relevance")
        @RequestParam(value = "sortfield", defaultValue = "", required = false)
            String sortfield,

        @ApiParam("Specifies the sort direction of the sorted result. The directions are: asc for ascending and desc for descending")
        @RequestParam(value = "sortdirection", defaultValue = "", required = false)
            String sortdirection,

        @PageableDefault()
            Pageable pageable
    ) {
        logger.debug("GET /apis?q={}", query);

        QueryBuilder searchQuery;

        if (!StringUtils.isEmpty(title)) {
                    QueryBuilder titleQuery = QueryBuilders.matchPhrasePrefixQuery("title", title).analyzer("norwegian").maxExpansions(15);
                    searchQuery = QueryBuilders.boolQuery().should(titleQuery);
        } else if (query.isEmpty()) {
            searchQuery = QueryBuilders.matchAllQuery();
        } else {
            // add * if query only contains one word
            if (!query.contains(" ")) {
                query = query + " " + query + "*";
            }
            searchQuery = QueryBuilders.simpleQueryStringQuery(query);
        }

        BoolQueryBuilder composedQuery = QueryBuilders.boolQuery().must(searchQuery);

        // Adding constant "should" term increases score for matching documents
        // Elasticsearch interprets string value "true" as matching with boolean true
        composedQuery.should(QueryUtil.createTermQuery("nationalComponent", "true").boost(2));

        if (!orgPath.isEmpty()) {
            composedQuery.filter(QueryUtil.createTermQuery("publisher.orgPath", orgPath));
        }

        if (!harvestSourceUri.isEmpty()) {
            composedQuery.filter(QueryUtil.createTermQuery("harvestSourceUri", harvestSourceUri));
        }

        if (formats != null && formats.length > 0) {
            composedQuery.filter(QueryUtil.createTermsQuery("formats", formats));
        }
        logger.debug("Built query:{}", composedQuery);

        String[] returnFields = {
            "id",
            "title",
            "titleFormatted",
            "description",
            "descriptionFormatted",
            "formats",
            "publisher.id",
            "publisher.orgPath",
            "publisher.name",
            "publisher.prefLabel.*",
            "nationalComponent",
            "isOpenAccess",
            "isOpenLicense",
            "isFree",
            "statusCode",
            "deprecationInfoExpirationDate",
            "deprecationInfoReplacedWithUrl"
        };

        int from = (int) pageable.getOffset();

        SearchRequestBuilder searchRequest = elasticsearch.getClient()
            .prepareSearch("acat")
            .setTypes("apidocument")
            .setQuery(composedQuery)
            .setFrom(checkAndAdjustFrom(from))
            .setSize(checkAndAdjustSize(pageable.getPageSize()))
            .setFetchSource(returnFields, null);

        if ("true".equals(includeAggregations)) {
            searchRequest
                .addAggregation(QueryUtil.createTermsAggregation("formats", "formats"))
                .addAggregation(QueryUtil.createTermsAggregation("orgPath", "publisher.orgPath"));
        }

        if ("modified".equals(sortfield)) {
            SortOrder sortOrder = "asc".equals(sortdirection.toLowerCase()) ? SortOrder.ASC : SortOrder.DESC;

            SortBuilder sortBuilder = SortBuilders.fieldSort("harvest.firstHarvested")
                .order(sortOrder)
                .missing("_last");

            logger.debug("sort: {}", sortBuilder.toString());
            searchRequest.addSort(sortBuilder);
        }

        SearchResponse elasticResponse = searchRequest.execute().actionGet();

        return convertFromElasticResponse(elasticResponse);
    }


    private int checkAndAdjustFrom(int from) {
        if (from < 0) {
            return 0;
        } else {
            return from;
        }
    }

    private int checkAndAdjustSize(int size) {
        if (size > 100) {
            return 100;
        }

        if (size < 0) {
            return 0;
        }

        return size;
    }


    /**
     * Aggregation based on orgPath.
     *
     * @param query the first part or complete orgPath
     * @return the aggregations of apis with terms, accessRights, subjects, publishers, orgPath and distributions
     */

    @CrossOrigin
    @ApiOperation(value = "Aggregates api count per organization path.")
    @RequestMapping(value = AGGREGATE_API, method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<String> aggregateAPIs(@RequestParam(value = "q", defaultValue = "") String query) {

        logger.info("{} of {}", AGGREGATE_API, query);

        QueryBuilder search;

        if ("".equals(query)) {
            search = QueryBuilders.matchAllQuery();
        } else {
            search = QueryBuilders.termQuery("publisher.orgPath", query);
        }

        logger.trace(search.toString());

        AggregationBuilder apisWithDistribution = AggregationBuilders.filter("distCount", QueryBuilders.existsQuery("distribution"));

        AggregationBuilder openAPIsWithDistribution = AggregationBuilders.filter("distOnPublicAccessCount",
            QueryBuilders.boolQuery()
                .must(QueryBuilders.existsQuery("distribution"))
                .must(QueryBuilders.termQuery("accessRights.code.raw", "PUBLIC"))
        );

        AggregationBuilder apisWithSubject = AggregationBuilders.filter("subjectCount", QueryBuilders.existsQuery("subject.prefLabel"));

        // set up search query with aggregations
        SearchRequestBuilder searchBuilder = elasticsearch.getClient().prepareSearch("acat")
            .setTypes("api")
            .setQuery(search)
            .setSize(0)
            .addAggregation(QueryUtil.createTermsAggregation("isFree", "isFree"))
            .addAggregation(QueryUtil.createTermsAggregation("isOpenAccess", "isOpenAccess"))
            .addAggregation(QueryUtil.createTermsAggregation("isOpenLicense", "isOpenLicense"))
            .addAggregation(QueryUtil.createTermsAggregation("orgPath", "publisher.orgPath"))
            .addAggregation(QueryUtil.createTermsAggregation("catalogs", "catalog.uri"))
            .addAggregation(QueryUtil.createTemporalAggregation("firstHarvested", "harvest.firstHarvested"))
            .addAggregation(AggregationBuilders.missing("missingFirstHarvested").field("harvest.firstHarvested"))
            .addAggregation(QueryUtil.createTemporalAggregation("lastChanged", "harvest.lastChanged"))
            .addAggregation(AggregationBuilders.missing("missingLastChanged").field("harvest.lastChanged"));

        // Execute search
        SearchResponse response = searchBuilder.execute().actionGet();

        logger.trace("Search response: " + response.toString());

        // return response
        return new ResponseEntity<>(response.toString(), HttpStatus.OK);
    }

    QueryResponse convertFromElasticResponse(SearchResponse elasticResponse) {
        logger.debug("converting response");
        QueryResponse queryResponse = new QueryResponse();
        convertHits(queryResponse, elasticResponse);
        convertAggregations(queryResponse, elasticResponse);
        return queryResponse;
    }

    void convertHits(QueryResponse queryResponse, SearchResponse elasticResponse) {

        queryResponse.setTotal(elasticResponse.getHits().getTotalHits());

        queryResponse.setHits(new ArrayList<>());
        for (SearchHit hit : elasticResponse.getHits().getHits()) {
            try {
                ApiDocument document = mapper.readValue(hit.getSourceAsString(), ApiDocument.class);
                queryResponse.getHits().add(document);
            } catch (Exception e) {
                logger.error("error {}", e.getMessage(), e);
            }
        }
    }

    void convertAggregations(QueryResponse queryResponse, SearchResponse elasticResponse) {
        if (elasticResponse.getAggregations() == null) {
            return;
        }
        queryResponse.setAggregations(new HashMap<>());
        Map<String, Aggregation> elasticAggregationsMap = elasticResponse.getAggregations().getAsMap();

        elasticAggregationsMap.forEach((aggregationName, aggregation) -> {
            no.acat.model.queryresponse.Aggregation outputAggregation = new no.acat.model.queryresponse.Aggregation() {{
                buckets = new ArrayList<>();
            }};

            ((Terms) aggregation).getBuckets().forEach((bucket) -> {
                outputAggregation.getBuckets().add(AggregationBucket.of(bucket.getKeyAsString(), bucket.getDocCount()));
            });
            queryResponse.getAggregations().put(aggregationName, outputAggregation);
        });
    }

    static class QueryUtil {
        static QueryBuilder createTermQuery(String term, String value) {
            return value.equals(MISSING) ?
                QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(term)) :
                QueryBuilders.termQuery(term, value);
        }

        static QueryBuilder createTermsQuery(String term, String[] values) {
            BoolQueryBuilder composedQuery = QueryBuilders.boolQuery();
            for (String value : values) {
                if (value.equals(MISSING)) {
                    composedQuery.filter(QueryBuilders.boolQuery().mustNot(QueryBuilders.existsQuery(term)));
                } else {
                    composedQuery.filter(QueryBuilders.termQuery(term, value));
                }
            }
            return composedQuery;
        }

        static AggregationBuilder createTermsAggregation(String aggregationName, String field) {
            return AggregationBuilders
                .terms(aggregationName)
                .missing(MISSING)
                .field(field)
                .size(MAX_AGGREGATIONS)
                .order(Terms.Order.count(false));
        }

        static RangeQueryBuilder createRangeQueryFromXdaysToNow(int days, String dateField) {
            long now = new Date().getTime();

            return QueryBuilders.rangeQuery(dateField).from(now - days * DAY_IN_MS).to(now).format("epoch_millis");
        }

        static AggregationBuilder createTemporalAggregation(String name, String dateField) {

            return AggregationBuilders.filters(name,
                new FiltersAggregator.KeyedFilter("last7days", QueryUtil.createRangeQueryFromXdaysToNow(7, dateField)),
                new FiltersAggregator.KeyedFilter("last30days", QueryUtil.createRangeQueryFromXdaysToNow(30, dateField)),
                new FiltersAggregator.KeyedFilter("last365days", QueryUtil.createRangeQueryFromXdaysToNow(365, dateField)));
        }
    }
}
