package no.dcat.harvester.crawler.handlers;

import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import no.dcat.datastore.Elasticsearch;
import no.dcat.datastore.domain.DcatSource;
import no.dcat.datastore.domain.dcat.builders.AbstractBuilder;
import no.dcat.datastore.domain.dcat.builders.DcatReader;
import no.dcat.datastore.domain.dcat.vocabulary.DCAT;
import no.dcat.datastore.domain.harvest.CatalogHarvestRecord;
import no.dcat.datastore.domain.harvest.ChangeInformation;
import no.dcat.datastore.domain.harvest.DatasetHarvestRecord;
import no.dcat.datastore.domain.harvest.DatasetLookup;
import no.dcat.datastore.domain.harvest.ValidationStatus;
import no.dcat.harvester.clean.HtmlCleaner;
import no.dcat.harvester.crawler.CrawlerResultHandler;
import no.dcat.harvester.crawler.notification.EmailNotificationService;
import no.dcat.harvester.crawler.notification.HarvestLogger;
import no.dcat.shared.Catalog;
import no.dcat.shared.Contact;
import no.dcat.shared.Dataset;
import no.dcat.shared.Distribution;
import no.dcat.shared.HarvestMetadata;
import no.dcat.shared.Publisher;
import no.dcat.shared.Subject;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.ConstantScoreQueryBuilder;
import org.elasticsearch.index.query.ExistsQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Handles harvesting of dcat data sources, and saving them into elasticsearch
 */
public class ElasticSearchResultHandler implements CrawlerResultHandler {

    public static final String DCAT_INDEX = "dcat";
    public static final String SUBJECT_TYPE = "subject";
    public static final String DATASET_TYPE = "dataset";
    public static final String HARVEST_INDEX = "harvest";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String DEFAULT_EMAIL_SENDER = "fellesdatakatalog@brreg.no";
    public static final String VALIDATION_EMAIL_RECEIVER = "fellesdatakatalog@brreg.no"; //temporary
    public static final String VALIDATION_EMAIL_SUBJECT = "Felles datakatalog harvestlogg";
    public static final String HARVESTLOG_DIRECTORY = "harvestlog/";
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    private LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
    private final Logger logger = logCtx.getLogger("main");

    private EmailNotificationService notificationService;

    String hostename;
    int port;
    String clustername;
    private final String themesHostname;
    String httpUsername;
    String httpPassword;
    String notificationEmailSender;


    /**
     * Creates a new elasticsearch code result handler connected to
     * a particular elasticsearch instance.
     *
     * @param hostname               host name where elasticsearch cluster is found
     * @param port                   port for connection to elasticserach cluster. Usually 9300
     * @param clustername            Name of elasticsearch cluster
     * @param themesHostname         hostname for reference-data service whitch provides themes service
     * @param httpUsername           username used for posting data to reference-data service
     * @param httpPassword           password used for posting data to reference-data service
     * @param notifactionEmailSender email address used as from: address in emails with validation results
     */
    public ElasticSearchResultHandler(String hostname, int port, String clustername, String themesHostname, String httpUsername, String httpPassword, String notifactionEmailSender, EmailNotificationService emailNotificationService) {
        this.hostename = hostname;
        this.port = port;
        this.clustername = clustername;
        this.themesHostname = themesHostname;
        this.httpUsername = httpUsername;
        this.httpPassword = httpPassword;
        this.notificationEmailSender = notifactionEmailSender;
        this.notificationService = emailNotificationService;

        logger.debug("ES clustername: " + this.clustername);
    }


    public ElasticSearchResultHandler(String hostname, int port, String clustername, String themesHostname, String httpUsername, String httpPassword) {
        this(hostname, port, clustername, themesHostname, httpUsername, httpPassword, DEFAULT_EMAIL_SENDER, null);
    }


    /**
     * Process a data catalog, represented as an RDF model
     *
     * @param dcatSource information about the source/provider of the data catalog
     * @param model      RDF model containing the data catalog
     */
    @Override
    public void process(DcatSource dcatSource, Model model, List<String> validationResults) {
        logger.debug("Processing results Elasticsearch: " + this.hostename + ":" + this.port + " cluster: " + this.clustername);

        try (Elasticsearch elasticsearch = new Elasticsearch(hostename, port, clustername)) {
            logger.trace("Start indexing");
            indexWithElasticsearch(dcatSource, model, elasticsearch, validationResults);
        } catch (Exception e) {
            logger.error("Exception: " + e.getMessage(), e);
            throw e;
        }
        logger.trace("finished");
    }

    /**
     * Index data catalog with Elasticsearch
     *
     * @param dcatSource        information about the source/provider of the data catalog
     * @param model             RDF model containing the data catalog
     * @param elasticsearch     The Elasticsearch instance where the data catalog should be stored
     * @param validationResults List of strings with result from validation rules execution
     */
    void indexWithElasticsearch(DcatSource dcatSource, Model model, Elasticsearch elasticsearch, List<String> validationResults) {
        //add special logger for the message that will be sent to dcatsource owner;
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String logFileName = HARVESTLOG_DIRECTORY + "harvest-" + dcatSource.getOrgnumber() + "-" + timestamp + ".log";
        HarvestLogger harvestlogger = new HarvestLogger(logFileName);
        Logger harvestLog = harvestlogger.getLogger();

        harvestLog.info("Harvest log for datasource ID: " + dcatSource.getId());


        // enable gson to read subtype of publisher
        RuntimeTypeAdapterFactory<Publisher> typeFactory = RuntimeTypeAdapterFactory
                .of(Publisher.class, "type")
                .registerSubtype(no.dcat.datastore.domain.dcat.Publisher.class, no.dcat.datastore.domain.dcat.Publisher.class.getName());

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .setDateFormat(DATE_FORMAT)
                .registerTypeAdapterFactory(typeFactory)
                .create();


        createIndexIfNotExists(elasticsearch, DCAT_INDEX);
        createIndexIfNotExists(elasticsearch, HARVEST_INDEX);

        Set<String> datasetsInSource = getSourceDatasetUris(model);

        // todo extract logs form reader and insert into elastic
        DcatReader reader = new DcatReader(model, themesHostname, httpUsername, httpPassword);
        List<Dataset> validDatasets = reader.getDatasets();
        List<Catalog> catalogs = reader.getCatalogs();

        if (validDatasets == null || validDatasets.isEmpty()) {
            throw new RuntimeException(
                    String.format("No valid datasets to index. %d datasets were found at source url %s",
                            datasetsInSource.size(),
                            dcatSource.getUrl()));
        }
        logger.info("Processing {} valid datasets. {} non valid datasets were ignored",
                validDatasets.size(), datasetsInSource.size() - validDatasets.size());
        //also route to harvest log - to be mailed to user
        harvestLog.info("Processing {} valid datasets. {} non valid datasets were ignored",
                validDatasets.size(), datasetsInSource.size() - validDatasets.size());

        logger.debug("Preparing bulkRequest");
        BulkRequestBuilder bulkRequest = elasticsearch.getClient().prepareBulk();
        saveSubjects(dcatSource, gson, bulkRequest, reader);

        Date harvestTime = new Date();
        logger.info("Found {} dataset documents in dcat source {}", validDatasets.size(), dcatSource.getId());
        //also route to harvest log - to be mailed to user
        harvestLog.info("Found {} dataset documents in dcat source {}", validDatasets.size(), dcatSource.getId());

        for (Catalog catalog : catalogs) {
            logger.info("Processing catalog {}", catalog.getUri());
            //also route to harvest log - to be mailed to user
            harvestLog.info("Processing catalog {}", catalog.getUri());

            CatalogHarvestRecord catalogRecord = new CatalogHarvestRecord();
            catalogRecord.setCatalogUri(catalog.getUri());
            catalogRecord.setHarvestUrl(dcatSource.getUrl());
            catalogRecord.setDataSourceId(dcatSource.getId());
            catalogRecord.setDate(harvestTime);
            catalogRecord.setValidDatasetUris(new HashSet<>());
            catalogRecord.setPublisher(catalog.getPublisher());

            ChangeInformation stats = new ChangeInformation();
            logger.debug("stats: " + stats.toString());
            for (Dataset dataset : validDatasets.stream().filter(d -> d.getCatalog().getUri().equals(catalog.getUri())).collect(Collectors.toList())) {

                catalogRecord.getValidDatasetUris().add(dataset.getUri());

                addDisplayFields(dataset);

                saveDatasetAndHarvestRecord(dcatSource, elasticsearch, validationResults, gson, bulkRequest, harvestTime, dataset, stats);

            }

            catalogRecord.setNonValidDatasetUris(new HashSet<>());
            catalogRecord.getNonValidDatasetUris().addAll(getDatasetsUris(model, catalog.getUri()));
            catalogRecord.getNonValidDatasetUris().removeAll(catalogRecord.getValidDatasetUris());

            deletePreviousDatasetsNotPresentInThisHarvest(elasticsearch, gson, catalogRecord, stats);
            catalogRecord.setChangeInformation(stats);

            saveCatalogHarvestRecord(dcatSource, validationResults, gson, bulkRequest, harvestTime, catalogRecord);

            logger.debug("/harvest/catalog/_indexRequest:\n{}", gson.toJson(catalogRecord));

            //add validation results to log to send to datasource owner
            harvestLog.info("Validation results for catalog {}:", catalog.getId());
            if (validationResults != null) {
                for (String validationResult : validationResults) {
                    harvestLog.info(validationResult);
                }
            } else {
                harvestLog.info("No validation results found for catalog {}", catalog.getId());
            }
        }

        if (notificationService != null) {
            //get contents from harvest log file
            notificationService.sendValidationResultNotification(
                    notificationEmailSender,
                    VALIDATION_EMAIL_RECEIVER, //TODO: replace with email lookop for catalog owners
                    VALIDATION_EMAIL_SUBJECT,
                    harvestlogger.getLogContents());
        } else {
            logger.warn("email notifcation service not set. Could not send email with validation results");
        }

        //delete file appender and log file
        harvestlogger.closeLog();

        BulkResponse bulkResponse = bulkRequest.execute().actionGet();
        if (bulkResponse.hasFailures()) {
            //TODO: process failures by iterating through each bulk response item?
        }

    }

    /**
     * Add extra fields to the dataset to help visualization.
     * <p>
     * <p>Assume that description contains basic htmltags. Swap description and descriptionFormatted and clean description. </p>
     *
     * @param dataset the dataset to enhance.
     */

    private void addDisplayFields(Dataset dataset) {
        if (dataset == null || dataset.getDescription() == null) {
            return;
        }

        dataset.setDescriptionFormatted(dataset.getDescription());

        final Map<String, String> descriptionCleaned = new HashMap<>();

        // remove formatting on description
        dataset.getDescription().forEach((key, value) -> {
            descriptionCleaned.put(key, HtmlCleaner.cleanAllHtmlTags(value));
        });

        dataset.setDescription(descriptionCleaned);
    }

    DatasetHarvestRecord findLastDatasetHarvestRecordWithContent(Dataset dataset, Elasticsearch elasticsearch, Gson gson) {
        TermQueryBuilder hasDatasetId = QueryBuilders.termQuery("datasetId", dataset.getId());
        ExistsQueryBuilder hasDatasetValue = QueryBuilders.existsQuery("dataset");

        BoolQueryBuilder datasetWithValueQuery = QueryBuilders.boolQuery();
        datasetWithValueQuery.should(hasDatasetId).should(hasDatasetValue);

        logger.debug("query: {}", datasetWithValueQuery.toString());

        SearchResponse lastDatasetRecordResponse = elasticsearch.getClient()
                .prepareSearch(HARVEST_INDEX).setTypes("dataset")
                .setQuery(datasetWithValueQuery)
                .addSort("date", SortOrder.DESC)
                .setSize(1).get();

        if (lastDatasetRecordResponse.getHits().getTotalHits() > 0) {

            DatasetHarvestRecord lastHarvestRecord =
                    gson.fromJson(lastDatasetRecordResponse.getHits().getAt(0).getSourceAsString(), DatasetHarvestRecord.class);
            if (lastHarvestRecord != null && lastHarvestRecord.getDatasetId().equals(dataset.getId())) {
                logger.debug("Found {} harvested at {}", lastHarvestRecord.getDatasetId(), dateFormat.format(lastHarvestRecord.getDate()));

                return lastHarvestRecord;
            }

        }

        return null;
    }

    DatasetHarvestRecord findFirstDatasetHarvestRecord(Dataset dataset, Elasticsearch elasticsearch, Gson gson) {

        TermQueryBuilder hasDatasetId = QueryBuilders.termQuery("datasetId", dataset.getId());
        logger.info("findFirstDataset: {}", hasDatasetId.toString());

        SearchResponse firstDatasetRecordResponse = elasticsearch.getClient()
                .prepareSearch(HARVEST_INDEX).setTypes("dataset")
                .setQuery(hasDatasetId)
                .addSort("date", SortOrder.ASC)
                .setSize(1).get();

        logger.debug("find first dataset harvest record query: {}", firstDatasetRecordResponse.getHits().toString());

        if (firstDatasetRecordResponse.getHits().getTotalHits() > 0) {
            DatasetHarvestRecord firstHarvestRecord =
                    gson.fromJson(firstDatasetRecordResponse.getHits().getAt(0).getSourceAsString(), DatasetHarvestRecord.class);

            logger.info("Found first harvest record for {} with date {}", firstHarvestRecord.getDatasetId(), dateFormat.format(firstHarvestRecord.getDate()));

            return firstHarvestRecord;
        }


        return null;
    }


    private void deletePreviousDatasetsNotPresentInThisHarvest(Elasticsearch elasticsearch, Gson gson,
                                                               CatalogHarvestRecord thisCatalogRecord, ChangeInformation stats) {

        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("catalogUri", thisCatalogRecord.getCatalogUri());
        ConstantScoreQueryBuilder csQueryBuilder = QueryBuilders.constantScoreQuery(termQueryBuilder);

        logger.debug("query: {}", csQueryBuilder.toString());

        SearchResponse lastCatalogRecordResponse = elasticsearch.getClient()
                .prepareSearch(HARVEST_INDEX).setTypes("catalog")
                .setQuery(csQueryBuilder)
                .addSort("date", SortOrder.DESC)
                .setSize(1).get();

        if (lastCatalogRecordResponse.getHits().getTotalHits() > 0) {
            try {
                CatalogHarvestRecord lastCatalogRecord =
                        gson.fromJson(lastCatalogRecordResponse.getHits().getAt(0).getSourceAsString(), CatalogHarvestRecord.class);

                if (lastCatalogRecord.getCatalogUri().equals(thisCatalogRecord.getCatalogUri())) {
                    logger.info("Last harvest for {} was {}", lastCatalogRecord.getCatalogUri(), dateFormat.format(lastCatalogRecord.getDate()));
                    logger.trace("found lastCatalogRecordResponse {}", gson.toJson(lastCatalogRecord));

                    Set<String> missingUris = new HashSet<>(lastCatalogRecord.getValidDatasetUris());
                    missingUris.removeAll(thisCatalogRecord.getValidDatasetUris());
                    if (missingUris.size() > 0) {
                        logger.info("There are {} datasets that were not harvested this time", missingUris.size());

                        for (String uri : missingUris) {
                            DatasetLookup lookup = findLookupDataset(elasticsearch.getClient(), uri, gson);
                            if (lookup != null && lookup.getDatasetId() != null) {
                                elasticsearch.deleteDocument(DCAT_INDEX, DATASET_TYPE, lookup.getDatasetId());
                                logger.info("deleted dataset {} with harvest uri {}", lookup.getDatasetId(), lookup.getHarvestUri());
                                stats.setDeletes(stats.getDeletes() + 1);
                            }
                        }
                    }
                }
            } catch (JsonParseException e) {
                logger.error("Unable to parse catalogHarvestRecord: {} ", lastCatalogRecordResponse.getHits().getAt(0).getSourceAsString());
            }
        }
    }

    private Set<String> getSourceDatasetUris(Model model) {
        Set<String> datasetsInSource = new HashSet<>();
        ResIterator datasetResourceIterator = model.listResourcesWithProperty(RDF.type, DCAT.Dataset);
        while (datasetResourceIterator.hasNext()) {
            Resource r = datasetResourceIterator.nextResource();
            datasetsInSource.add(r.getURI());
        }
        return datasetsInSource;
    }

    Set<String> getDatasetsUris(Model model, String catalogUri) {
        Set<String> datasetsInCatalog = new HashSet<>();
        Resource catalogResource = model.getResource(catalogUri);
        StmtIterator iterator = catalogResource.listProperties(DCAT.dataset);
        while (iterator.hasNext()) {
            Statement statement = iterator.next();
            datasetsInCatalog.add(statement.getObject().asResource().getURI());
        }
        return datasetsInCatalog;
    }

    DatasetLookup findLookupDataset(Client client, String uri, Gson gson) {
        GetResponse response = client.prepareGet(HARVEST_INDEX, "lookup", uri).get();

        if (response.isExists()) {
            return gson.fromJson(response.getSourceAsString(), DatasetLookup.class);
        }

        return null;
    }

    private HarvestMetadata createHarvestMetadata() {
        HarvestMetadata result = new HarvestMetadata();
        result.setChanged(new ArrayList<>());

        return result;
    }

    private DatasetLookup createDatasetLookup() {
        DatasetLookup result = new DatasetLookup();
        result.setHarvest(createHarvestMetadata());

        return result;
    }

    DatasetLookup findOrCreateDatasetLookupAndUpdateDatasetId(Dataset dataset, Elasticsearch elasticsearch, Gson gson, ChangeInformation stats, Date harvestTime) {
        String datasetId = null;

        // get dataset lookup entry
        DatasetLookup lookupEntry = findLookupDataset(elasticsearch.getClient(), dataset.getUri(), gson);

        if (lookupEntry == null) {

            datasetId = UUID.randomUUID().toString();
            logger.debug("new dataset {} with harvestUri {} identified", datasetId, dataset.getUri());

            lookupEntry = createDatasetLookup();
            lookupEntry.setHarvestUri(dataset.getUri());
            lookupEntry.setDatasetId(datasetId);
            lookupEntry.getHarvest().setFirstHarvested(harvestTime);
            lookupEntry.setIdentifier(dataset.getIdentifier());

            stats.setInserts(stats.getInserts() + 1);

        } else {
            datasetId = lookupEntry.getDatasetId();
            logger.debug("existing dataset {} with harvestUri {} identified", datasetId, dataset.getUri());

            if (lookupEntry.getHarvest() == null) {
                lookupEntry.setHarvest(createHarvestMetadata());
            }

            stats.setUpdates(stats.getUpdates() + 1);
        }

        dataset.setId(lookupEntry.getDatasetId());

        lookupEntry.getHarvest().setLastHarvested(harvestTime);

        return lookupEntry;
    }

    Date getFirstHarvestedDate(Dataset dataset, Elasticsearch elasticsearch, Gson gson) {
        DatasetHarvestRecord firstHarvestRecord = findFirstDatasetHarvestRecord(dataset, elasticsearch, gson);
        if (firstHarvestRecord != null) {
            return firstHarvestRecord.getDate();
        }

        return null;
    }

    // TODO remove after run in production
    DatasetHarvestRecord updateDatasetHarvestRecordsAndReturnLastChanged(Dataset dataset, Elasticsearch elasticsearch, Gson gson, BulkRequestBuilder bulkRequest, DatasetLookup lookupEntry) {

        logger.info("update dataset harvest records for dataset {} - {}", dataset.getId(), dataset.getUri());
        TermQueryBuilder hasDatasetId = QueryBuilders.termQuery("datasetId", dataset.getId());
        logger.info("findAllDatasetHarvestRecords and nullify not changed datasets: {}", hasDatasetId.toString());
        boolean runner = true;
        List<DatasetHarvestRecord> allRecords = new ArrayList<>();
        Map<DatasetHarvestRecord, String> recordIdMap = new HashMap<>();
        int from = 0;
        int size = 20;

        while (runner) {
            SearchResponse harvestRecordResponse = elasticsearch.getClient()
                    .prepareSearch(HARVEST_INDEX).setTypes("dataset")
                    .setQuery(hasDatasetId)
                    .addSort("date", SortOrder.ASC)
                    .setFrom(from)
                    .setSize(size).get();

            int numberOfHits = harvestRecordResponse.getHits().hits().length;
            if (numberOfHits > 0) {
                for (int i = 0; i < numberOfHits; i++) {
                    DatasetHarvestRecord harvestRecord =
                            gson.fromJson(harvestRecordResponse.getHits().getAt(i).getSourceAsString(), DatasetHarvestRecord.class);

                    if (harvestRecord.getDatasetId().equals(dataset.getId())) {
                        String recordId = harvestRecordResponse.getHits().getAt(i).getId();
                        recordIdMap.put(harvestRecord, recordId);
                        allRecords.add(harvestRecord);
                    }
                }

                from += size;
            } else {
                runner = false;
            }

        }
        DatasetHarvestRecord previousRecord = null;
        DatasetHarvestRecord lastChangedRecord = null;
        List<DatasetHarvestRecord> recordsWithNoChange = new ArrayList<>();
        for (DatasetHarvestRecord record : allRecords) {

            if (previousRecord != null) {
                if (isChanged(previousRecord, record.getDataset(), gson)) {
                    lookupEntry.getHarvest().getChanged().add(record.getDate());
                    lastChangedRecord = record;
                    lookupEntry.getHarvest().setLastChanged(record.getDate());
                } else {
                    recordsWithNoChange.add(record);
                }
            }

            previousRecord = record;
        }
        logger.info("Found {} harvest records to nullify ", recordsWithNoChange.size());

        recordsWithNoChange.forEach(record -> {
            String recordId = recordIdMap.get(record);
            logger.info("nullify: {} harvested at {}", recordId, dateFormat.format(record.getDate()));
            record.setDataset(null);
            record.setValidationStatus(null);
            bulkRequest.add(createBulkRequest(HARVEST_INDEX, "dataset", recordId, record, gson));
        });

        return lastChangedRecord;

    }


    void saveDatasetAndHarvestRecord(DcatSource dcatSource, Elasticsearch elasticsearch,
                                     List<String> catalogValidationResults, Gson gson, BulkRequestBuilder bulkRequest,
                                     Date harvestTime, Dataset dataset, ChangeInformation stats) {

        try {
            DatasetLookup lookupEntry = findOrCreateDatasetLookupAndUpdateDatasetId(dataset, elasticsearch, gson, stats, harvestTime);

            if (!lookupEntry.getDatasetId().equals(dataset.getId())) {
                throw new Exception(String.format("LookupEntry {} does not match datasetid {}", lookupEntry.getDatasetId(), dataset.getId()));
            }

            DatasetHarvestRecord lastHarvestRecordWithContent;

            // compensate if we do not have first harvest date (handles old way) harvest records.
            if (lookupEntry.getHarvest().getFirstHarvested() == null) {
                // this should be removed - TODO
                logger.info("Compensating actions for dataset: {}", dataset.getUri());
                lookupEntry.getHarvest().setFirstHarvested(getFirstHarvestedDate(dataset, elasticsearch, gson));
                lastHarvestRecordWithContent = updateDatasetHarvestRecordsAndReturnLastChanged(dataset, elasticsearch, gson, bulkRequest, lookupEntry);
            } else {
                // this should remain
                lastHarvestRecordWithContent = findLastDatasetHarvestRecordWithContent(dataset, elasticsearch, gson);
            }

            boolean isChanged = true;

            if (lastHarvestRecordWithContent != null) {
                if (!lastHarvestRecordWithContent.getDatasetId().equals(dataset.getId())) {
                    throw new Exception(String.format("LastHarvestRecordWithChange {} does not match datasetid {}", lastHarvestRecordWithContent.getDatasetId(), dataset.getId()));
                }
                // detect changes
                isChanged = isChanged(lastHarvestRecordWithContent, dataset, gson);
            }

            if (isChanged) {
                lookupEntry.getHarvest().getChanged().add(harvestTime);
                lookupEntry.getHarvest().setLastChanged(harvestTime);
            }

            // save lookup entry
            bulkRequest.add(createBulkRequest(HARVEST_INDEX, "lookup", dataset.getUri(), lookupEntry, gson));

            // create a new dataset harvest record
            DatasetHarvestRecord record = createDatasetHarvestRecord(dataset, dcatSource, isChanged, harvestTime, catalogValidationResults);
            // save dataset harvest record
            bulkRequest.add(createBulkRequest(HARVEST_INDEX, "dataset", null, record, gson));

            // add harvest metadata to dataset
            dataset.setHarvest(lookupEntry.getHarvest());

            logger.debug("Add dataset document {} to bulk request", dataset.getId());
            // save dataset
            bulkRequest.add(createBulkRequest(DCAT_INDEX, DATASET_TYPE, dataset.getId(), dataset, gson));

        } catch (Exception e) {
            logger.error("Unable to index {}. Reason: {}", dataset.getUri(), e.getMessage(), e);
        }

    }

    DatasetHarvestRecord createDatasetHarvestRecord(Dataset dataset, DcatSource dcatSource, boolean isChanged, Date harvestTime, List<String> catalogValidationResults) {
        DatasetHarvestRecord record = new DatasetHarvestRecord();

        record.setDatasetId(dataset.getId());
        record.setDatasetUri(dataset.getUri());
        record.setDcatSourceId(dcatSource.getId());

        record.setDate(harvestTime);

        if (isChanged) {
            record.setDataset(dataset);
            record.setValidationStatus(extractValidationStatus(filterValidationMessagesForDataset(catalogValidationResults, dataset)));
        }

        return record;
    }

    IndexRequest createBulkRequest(String index, String type, String id, Object data, Gson gson) {
        IndexRequest request = id == null ?
                new IndexRequest(index, type) : new IndexRequest(index, type, id);
        request.source(gson.toJson(data));

        return request;
    }

    boolean isChanged(DatasetHarvestRecord lastDatasetHarvestRecord, Dataset currentDataset, Gson gson) {

        if (lastDatasetHarvestRecord == null || currentDataset == null) {
            return false;
        }

        Dataset lastSavedDataset = lastDatasetHarvestRecord.getDataset();

        if (lastSavedDataset == null) {
            return false;
        }

        boolean isEqual = isJsonEqual(currentDataset, lastSavedDataset, gson);

        if (!isEqual && (hasWrongOrgpath(currentDataset) || hasWrongOrgpath(lastSavedDataset))) {
            // TODO HACK to fix earlier handling ov annet without orgnummer
            correctOrgpath(currentDataset);
            correctOrgpath(lastSavedDataset);

            isEqual = isJsonEqual(currentDataset, lastSavedDataset, gson);
        }

        if (!isEqual && currentDataset.getContactPoint() != null && lastSavedDataset.getContactPoint() != null) { // TODO HACK to fix random generated contact point uris

            List<Contact> contacts1 = currentDataset.getContactPoint();
            List<Contact> contacts2 = lastSavedDataset.getContactPoint();

            try {
                currentDataset.setContactPoint(null);
                lastSavedDataset.setContactPoint(null);

                boolean isEqualExceptContacts = isJsonEqual(currentDataset, lastSavedDataset, gson);

                if (isEqualExceptContacts) {
                    Contact contact1 = contacts1.get(0);
                    Contact contact2 = contacts2.get(0);

                    if (contact1 != null && contact2 != null) {
                        if (AbstractBuilder.hasGeneratedContactPrefix(contact1) && AbstractBuilder.hasGeneratedContactPrefix(contact2)) {

                            isEqual = stringCompare(contact1.getEmail(), contact2.getEmail()) &&
                                    stringCompare(contact1.getFullname(), contact2.getFullname()) &&
                                    stringCompare(contact1.getHasTelephone(), contact2.getHasTelephone()) &&
                                    stringCompare(contact1.getHasURL(), contact2.getHasURL()) &&
                                    stringCompare(contact1.getOrganizationName(), contact2.getOrganizationName()) &&
                                    stringCompare(contact1.getOrganizationUnit(), contact2.getOrganizationUnit());
                        }
                    }
                }
            } catch (Exception e) {
                // If this occurs the contacts are not comparable.
            } finally {
                currentDataset.setContactPoint(contacts1);
                lastSavedDataset.setContactPoint(contacts2);
            }
        }

        return !isEqual;
    }

    boolean hasWrongOrgpath(Dataset dataset) {
        if (dataset != null && dataset.getPublisher() != null && dataset.getPublisher().getOrgPath() != null) {
            return dataset.getPublisher().getOrgPath().startsWith("/ANNET/http");
        }

        return false;
    }

    void correctOrgpath(Dataset dataset) {
        if (hasWrongOrgpath(dataset)) {
            dataset.getPublisher().setOrgPath("/ANNET/" + dataset.getPublisher().getName());
            if (dataset.getCatalog() != null && dataset.getCatalog().getPublisher() != null) {
                dataset.getCatalog().getPublisher().setOrgPath("/ANNET/" + dataset.getCatalog().getPublisher().getName());
            }
        }
    }

    boolean isJsonEqual(Dataset d1, Dataset d2, Gson gson) {
        // compare json to check if the dataset objects are alike
        String c1 = gson.toJson(d1);
        String c2 = gson.toJson(d2);

        return c1.equals(c2);
    }

    boolean stringCompare(String string1, String string2) {
        if (string1 == null) {
            if (string2 == null) {
                return true;
            }
            return false;
        } else {
            if (string2 == null) {
                return false;
            }
            return string1.equals(string2);
        }
    }


    ValidationStatus extractValidationStatus(List<String> messages) {
        if (messages != null && !messages.isEmpty()) {
            ValidationStatus vs = new ValidationStatus();
            vs.setWarnings((int) messages.stream().filter(m -> m.contains("validation_warning")).count());
            vs.setErrors((int) messages.stream().filter(m -> m.contains("validation_error")).count());
            vs.setValidationMessages(messages);

            return vs;
        }

        return null;
    }


    List<String> filterValidationMessagesForDataset(List<String> catalogValidationResults, Dataset dataset) {
        List<String> messages = null;
        if (catalogValidationResults != null) {
            try {
                messages = catalogValidationResults.stream().filter(m ->
                        m.contains(dataset.getUri()) && m.contains("className='Dataset'")).collect(Collectors.toList());
                logger.debug("messages: {}", messages.toString());
                if (dataset.getDistribution() != null) {
                    for (Distribution distribution : dataset.getDistribution()) {
                        List<String> distMessages = catalogValidationResults.stream()
                                .filter(m -> m.contains(distribution.getUri()) && m.contains("className='Distribution'"))
                                .collect(Collectors.toList());
                        messages.addAll(distMessages);
                    }
                }
            } catch (Throwable t) {
                logger.warn("Unknown error collecting validation messages {}", t.getLocalizedMessage());
            }
        }
        return messages;
    }

    private void saveCatalogHarvestRecord(DcatSource dcatSource, List<String> validationResults, Gson gson, BulkRequestBuilder bulkRequest, Date harvestTime, CatalogHarvestRecord catalogRecord) {
        // get summary from fuseki
        dcatSource.getLastHarvest().ifPresent(harvest -> {
            catalogRecord.setMessage(harvest.getMessage());
            catalogRecord.setStatus(harvest.getStatus().toString());
        });

        List<String> catalogValidationMessages = null;
        if (validationResults != null) {
            catalogValidationMessages = validationResults.stream().filter(m ->
                    m.contains(catalogRecord.getCatalogUri()) && m.contains("className='Catalog'")).collect(Collectors.toList());
        }

        catalogRecord.setValidationMessages(catalogValidationMessages);

        IndexRequest catalogCrawlRequest = new IndexRequest(HARVEST_INDEX, "catalog");
        catalogCrawlRequest.source(gson.toJson(catalogRecord));

        bulkRequest.add(catalogCrawlRequest);
    }


    private void saveSubjects(DcatSource dcatSource, Gson gson, BulkRequestBuilder bulkRequest, DcatReader reader) {
        List<Subject> subjects = reader.getSubjects();
        List<Subject> filteredSubjects = subjects.stream().filter(s ->
                s.getPrefLabel() != null && s.getDefinition() != null && !s.getPrefLabel().isEmpty() && !s.getDefinition().isEmpty())
                .collect(Collectors.toList());

        logger.info("Total number of unique subject uris {} in dcat source {}.", subjects.size(), dcatSource.getId());
        logger.info("Adding {} subjects with prefLabel and definition to elastic", filteredSubjects.size());

        for (Subject subject : filteredSubjects) {

            IndexRequest indexRequest = new IndexRequest(DCAT_INDEX, SUBJECT_TYPE, subject.getUri());
            indexRequest.source(gson.toJson(subject));

            logger.debug("Add subject document {} to bulk request", subject.getUri());
            bulkRequest.add(indexRequest);
        }
    }

    private void createIndexIfNotExists(Elasticsearch elasticsearch, String indexName) {
        if (!elasticsearch.indexExists(indexName)) {
            logger.info("Creating index: " + indexName);
            elasticsearch.createIndex(indexName);
        } else {
            logger.debug("Index exists: " + indexName);
        }
    }

}
