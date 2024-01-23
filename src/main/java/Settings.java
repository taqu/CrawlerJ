import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "settings")
public class Settings
{
    public static final String Empty = "";

    public String getHost() {
        return host_;
    }

    public void setHost(String host) {
        this.host_ = host;
    }

    @JacksonXmlProperty(localName = "host")
    private String host_ = Empty;

    public int getPort() {
        return port_;
    }

    public void setPort(int port_) {
        this.port_ = port_;
    }

    @JacksonXmlProperty(localName = "port")
    private int port_ = 0;
    @JacksonXmlProperty(localName = "embedding_host")
    private String embedding_host_ = Empty;
    @JacksonXmlProperty(localName = "embedding_port")
    private  int embedding_port_ = 0;

    public String getCollection() {
        return collection_;
    }

    public void setCollection(String collection_) {
        this.collection_ = collection_;
    }

    @JacksonXmlProperty(localName = "collection")
    private String collection_ = Empty;

    public String getRoot() {
        return root_;
    }

    public void setRoot(String root_) {
        this.root_ = root_;
    }

    @JacksonXmlProperty(localName = "root")
    private String root_ = Empty;

    public int getSize() {
        return size_;
    }

    public void setSize(int size_) {
        this.size_ = size_;
    }

    @JacksonXmlProperty(localName = "size")
    private int size_ = 480;

    public int getOverlap() {
        return overlap_;
    }

    public void setOverlap(int overlap_) {
        this.overlap_ = overlap_;
    }

    @JacksonXmlProperty(localName = "overlap")
    private int overlap_ = 48;

    public void setEmbeddingHost(String text) {
        embedding_host_ = text;
    }

    public void setEmbeddingPort(int port) {
        embedding_port_ = port;
    }

    public int getEmbeddingPort() {
        return embedding_port_;
    }

    public String getEmbeddingHost() {
        return embedding_host_;
    }
}
