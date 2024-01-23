import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public class EmbeddingRequest {
    public String getInput_() {
        return input_;
    }

    public void setInput_(String input_) {
        this.input_ = input_;
    }

    public String getModel_() {
        return model_;
    }

    public void setModel_(String model_) {
        this.model_ = model_;
    }

    public Optional<String> getEncoding_format_() {
        return encoding_format_;
    }

    public void setEncoding_format_(Optional<String> encoding_format_) {
        this.encoding_format_ = encoding_format_;
    }

    public Optional<String> getUser_() {
        return user_;
    }

    public void setUser_(Optional<String> user_) {
        this.user_ = user_;
    }

    @JsonProperty("input")
    private String input_;
    @JsonProperty("model")
    private String model_;
    @JsonProperty("encoding_format")
    private Optional<String> encoding_format_;
    @JsonProperty("user")
    private Optional<String> user_;
}
