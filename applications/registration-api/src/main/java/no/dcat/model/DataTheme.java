package no.dcat.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.ToString;

import java.util.Map;

/**
 * Created by bjg on 24.02.2017.
 * Model class themes:data-theme.
 */
@Data
@ToString(includeFieldNames = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataTheme {
    private String id;
    private String uri;
    private String code;
    private String pickedDate;
    private String startUse;
    private Map<String, String> title;
    private ConceptSchema conceptSchema;

}