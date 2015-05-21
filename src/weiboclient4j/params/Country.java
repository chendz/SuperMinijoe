package weiboclient4j.params;

import weiboclient4j.PlaceService;

/**
 * @author Hover Ruan
 */
public class Country extends StringParam implements
        PlaceService.CreatePoiParam {

    public Country(String value) {
        super(value);
    }

    protected String paramKey() {
        return "country";
    }
}
