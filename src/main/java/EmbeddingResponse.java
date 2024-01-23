import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class EmbeddingResponse {
    public String getObject_() {
        return object_;
    }

    public void setObject_(String object_) {
        this.object_ = object_;
    }

    public String getModel_() {
        return model_;
    }

    public void setModel_(String model_) {
        this.model_ = model_;
    }

    public List<Embedding> getData_() {
        return data_;
    }

    public void setData_(List<Embedding> data_) {
        this.data_ = data_;
    }

    public EmbeddingUsage getUsage_() {
        return usage_;
    }

    public void setUsage_(EmbeddingUsage usage_) {
        this.usage_ = usage_;
    }

    @JsonProperty("object")
    private String object_;
    @JsonProperty("model")
    private String model_;
    @JsonProperty("data")
    private List<Embedding> data_;
    @JsonProperty("usage")
    private EmbeddingUsage usage_;
}
