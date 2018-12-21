package sample.plugin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class StopListWordParam {

    private String stopWord;
    private ErrorLevel level = ErrorLevel.WARN;

    public enum ErrorLevel {
        WARN, ERROR
    }

}