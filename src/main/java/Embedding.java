import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Embedding {
    public Embedding()
    {
        object_ = "embedding";
    }

    public int getIndex_() {
        return index_;
    }

    public void setIndex_(int index_) {
        this.index_ = index_;
    }

    public List<Float> getEmbedding_() {
        return embedding_;
    }

    public void setEmbedding_(List<Float> embedding_) {
        this.embedding_ = embedding_;
    }

    public String getObject_() {
        return object_;
    }

    public void setObject_(String object_) {
        this.object_ = object_;
    }

    @JsonProperty("index")
    private int index_;
    @JsonProperty("embedding")
    private List<Float> embedding_;
    @JsonProperty("object")
    private String object_;
}
