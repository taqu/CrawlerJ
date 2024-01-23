import com.fasterxml.jackson.annotation.JsonProperty;

public class EmbeddingUsage {
    public int getPrompt_tokens_() {
        return prompt_tokens_;
    }

    public void setPrompt_tokens_(int prompt_tokens_) {
        this.prompt_tokens_ = prompt_tokens_;
    }

    public int getTotal_tokens_() {
        return total_tokens_;
    }

    public void setTotal_tokens_(int total_tokens_) {
        this.total_tokens_ = total_tokens_;
    }

    @JsonProperty("prompt_tokens")
    private int prompt_tokens_;
    @JsonProperty("total_tokens")
    private int total_tokens_;
}
