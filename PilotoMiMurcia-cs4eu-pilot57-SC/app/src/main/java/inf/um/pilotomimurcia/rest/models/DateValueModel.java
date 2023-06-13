package inf.um.pilotomimurcia.rest.models;

public class DateValueModel {

    private String serialDate;
    private Number value;

    public String getSerialDate() {
        return serialDate;
    }

    public Number getValue() {
        return value;
    }

    public DateValueModel(String serialDate, Number value) {
        this.serialDate = serialDate;
        this.value = value;
    }
}
