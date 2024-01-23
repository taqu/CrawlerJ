import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;
import tech.amikos.chromadb.Client;
import tech.amikos.chromadb.CohereEmbeddingFunction;
import tech.amikos.chromadb.Collection;
import tech.amikos.chromadb.EmbeddingFunction;
import tech.amikos.chromadb.handler.ApiException;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTreeUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainFrame extends JFrame
{
    public static final String DBURL = "jdbc:sqlite:crawler.sqlite";
    private static final String tika_config = """
<?xml version="1.0" encoding="UTF-8"?>
<properties>
    <detectors>
        <!-- All detectors except built-in container ones -->
        <detector class="org.apache.tika.detect.DefaultDetector">
            <!-- <detector-exclude class="org.apache.tika.parser.pkg.ZipContainerDetector"/> -->
        </detector>
    </detectors>
    <parsers>
        <parser class="org.apache.tika.parser.DefaultParser">
            <!-- ClassParser -->
            <mime-exclude>application/java-vm</mime-exclude>

            <mime-exclude>audio/x-wav</mime-exclude>
            <mime-exclude>audio/x-aiff</mime-exclude>
            <mime-exclude>audio/basic</mime-exclude>

            <mime-exclude>application/x-midi</mime-exclude>
            <mime-exclude>audio/midi</mime-exclude>


            <mime-exclude>application/pkcs7-signature</mime-exclude>
            <mime-exclude>application/pkcs7-mime</mime-exclude>
            <parser-exclude class="org.apache.tika.parser.crypto.Pkcs7Parser"/>
            <parser-exclude class="org.apache.tika.parser.executable.ExecutableParser"/>
            <parser-exclude class="org.apache.tika.parser.font.AdobeFontMetricParser"/>
            <parser-exclude class="org.apache.tika.parser.font.TrueTypeParser"/>
            <!--<parser-exclude class="org.apache.tika.parser.gdal.GDALParser"/>-->
            <!--<parser-exclude class="org.apache.tika.parser.geo.topic.GeoParser"/>-->
            <!-- <parser-exclude class="org.apache.tika.parser.geoinfo.GeographicInformationParser"/>-->
            <parser-exclude class="org.apache.tika.parser.pkg.CompressorParser"/>
            <parser-exclude class="org.apache.tika.parser.pkg.PackageParser"/>
            <!-- <parser-exclude class="org.apache.tika.parser.jdbc.SQLite3Parser"/> -->
        </parser>
    </parsers>
</properties>
""";

    private boolean changed_ = false;
    private Settings settings_ = new Settings();
    private TikaConfig tikaConfig_;
    private CrawlTask crawlTask_ = null;
    private JTextField textEndpoint_;
    private JTextField textPort_;
    private JTextField textEmbeddingHost_;
    private JTextField textEmbeddingPort_;
    private JTextField textCollection_;
    private JTextField textRoot_;
    private JTextField textSize_;
    private JTextField textOverlap_;
    private JButton buttonRun_;
    private JButton buttonCancel_;

    public void load_settings()
    {
        try{
            File file = new File("settings.xml");
            XmlMapper xmlMapper = new XmlMapper();
            settings_ = xmlMapper.readValue(file, Settings.class);
            textRoot_.setText(settings_.getRoot());
            textEndpoint_.setText(settings_.getHost());
            textPort_.setText(((Integer)settings_.getPort()).toString());
            textEmbeddingHost_.setText(settings_.getEmbeddingHost());
            textEmbeddingPort_.setText(((Integer)settings_.getEmbeddingPort()).toString());
            textCollection_.setText(settings_.getCollection());
            textSize_.setText(((Integer)settings_.getSize()).toString());
            textOverlap_.setText((((Integer)settings_.getOverlap()).toString()));
        } catch (IOException ignored) {
        }
    }

    private static int parseInt(String text, int x)
    {
        try{
           return Integer.parseInt(text);
        }catch (Exception ignored){
            return x;
        }
    }

    private static String parseCollection(String text, String old)
    {
        if(checkCollection(text)){
            return text;
        }
        return old;
    }

    private static boolean checkLowerCaseOrDigit(char c)
    {
        return Character.isAlphabetic(c)&&Character.isLowerCase(c) || Character.isDigit(c);
    }

    private static boolean collectionAllowedChar(char c)
    {
        return Character.isAlphabetic(c)
                || Character.isDigit(c)
                || '.' == c
                || '_' == c
                || '-' == c;
    }

    private static boolean checkCollection(String text)
    {
        if(text.length()<3 || 63<text.length()){
            return false;
        }
        if(!checkLowerCaseOrDigit(text.charAt(0))){
            return false;
        }
        if(!checkLowerCaseOrDigit(text.charAt(text.length()-1))){
            return false;
        }
        for(int i=0; i<text.length(); ++i){
            if(!collectionAllowedChar(text.charAt(i))){
                return false;
            }
        }
        Pattern pattern = Pattern.compile("^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$");
        Matcher matcher = pattern.matcher(text);
        if(matcher.matches()){
            return false;
        }
        return true;
    }

    private static boolean checkRoot(String text)
    {
        Path path = Paths.get(text);
        if(!Files.exists(path)){
            return false;
        }
        if(!Files.isDirectory(path)){
            return false;
        }
        return true;
    }
    private static String parseRoot(String text, String old)
    {
        if(checkRoot(text)){
            return text;
        }
        return old;
    }

    public void save_settings()
    {
        if(!changed_){
            return;
        }
        changed_ = false;
        settings_.setHost(textEndpoint_.getText());
        settings_.setPort(parseInt(textPort_.getText(), settings_.getPort()));
        settings_.setEmbeddingHost(textEmbeddingHost_.getText());
        settings_.setEmbeddingPort(parseInt(textEmbeddingPort_.getText(), settings_.getEmbeddingPort()));
        settings_.setCollection(parseCollection(textCollection_.getText(), settings_.getCollection()));
        settings_.setRoot(parseRoot(textRoot_.getText(), settings_.getRoot()));
        settings_.setSize(parseInt(textSize_.getText(), settings_.getSize()));
        settings_.setOverlap(parseInt(textOverlap_.getText(), settings_.getOverlap()));

        try{
            File file = new File("settings.xml");
            XmlMapper xmlMapper = new XmlMapper();
            xmlMapper.writeValue(file, settings_);
        } catch (IOException ignored) {
        }
    }

    private void setChanged()
    {
        changed_ = true;
    }
    MainFrame()
    {
        try{
            ReaderInputStream configInputStream = ReaderInputStream.builder().setReader(new StringReader(tika_config)).get();
            tikaConfig_ = new TikaConfig(configInputStream);
        } catch (TikaException e) {
        } catch (IOException e) {
        } catch (SAXException e) {
        }
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.X_AXIS));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Crawler");
        setSize(640, 320);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowClosing(this));
        {
            JPanel panelLeft = new JPanel();
            panelLeft.setLayout(new BoxLayout(panelLeft, BoxLayout.Y_AXIS));
            {
                JPanel panelRoot = new JPanel();
                panelRoot.setLayout(new BoxLayout(panelRoot, BoxLayout.X_AXIS));
                textRoot_ = new JFormattedTextField();
                textRoot_.setColumns(25);
                textRoot_.setEditable(false);
                textRoot_.setBackground(Color.GRAY);
                textRoot_.setAlignmentX(Component.LEFT_ALIGNMENT);

                JButton buttonOpen = new JButton("Open");
                buttonOpen.setAlignmentX(Component.LEFT_ALIGNMENT);
                buttonOpen.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        MainFrame mainFrame = (MainFrame)getFrames()[0];
                        mainFrame.onClickOpen();
                    }
                });
                panelRoot.add(new Label("Root:"));
                panelRoot.add(textRoot_);
                panelRoot.add(buttonOpen);
                panelLeft.add(panelRoot);
            }
            {
                JPanel panelVectorDB = new JPanel();
                panelVectorDB.setLayout(new BoxLayout(panelVectorDB, BoxLayout.Y_AXIS));

                {
                    JPanel panelEndpoint = new JPanel();
                    panelEndpoint.setLayout(new BoxLayout(panelEndpoint, BoxLayout.X_AXIS));
                    textEndpoint_ = new JFormattedTextField();
                    textEndpoint_.setColumns(10);
                    panelEndpoint.add(new Label("Endpoint:"));
                    panelEndpoint.add(textEndpoint_);
                    textEndpoint_.getDocument().addDocumentListener(new ValueChanged(this));

                    textPort_ = new JFormattedTextField();
                    textPort_.setColumns(5);
                    panelEndpoint.add(new Label("Port:"));
                    panelEndpoint.add(textPort_);

                    panelVectorDB.add(panelEndpoint);
                }
                {
                    JPanel panelEndpoint = new JPanel();
                    panelEndpoint.setLayout(new BoxLayout(panelEndpoint, BoxLayout.X_AXIS));
                    textEmbeddingHost_ = new JFormattedTextField();
                    textEmbeddingHost_.setColumns(10);
                    panelEndpoint.add(new Label("Embedding Endpoint:"));
                    panelEndpoint.add(textEmbeddingHost_);
                    textEmbeddingHost_.getDocument().addDocumentListener(new ValueChanged(this));

                    textEmbeddingPort_ = new JFormattedTextField();
                    textEmbeddingPort_.setColumns(5);
                    panelEndpoint.add(new Label("Embedding Port:"));
                    panelEndpoint.add(textEmbeddingPort_);

                    panelVectorDB.add(panelEndpoint);
                }
                {
                    JPanel panelCollection = new JPanel();
                    panelCollection.setLayout(new BoxLayout(panelCollection, BoxLayout.X_AXIS));
                    textCollection_ = new JFormattedTextField();
                    textCollection_.setColumns(16);
                    panelCollection.add(new Label("Collection:"));
                    panelCollection.add(textCollection_);

                    panelVectorDB.add(panelCollection);
                }
                {
                    JPanel panelSize = new JPanel();
                    panelSize.setLayout(new BoxLayout(panelSize, BoxLayout.X_AXIS));
                    textSize_ = new JFormattedTextField();
                    textSize_.setColumns(8);
                    panelSize.add(new Label("Size:"));
                    panelSize.add(textSize_);

                    textOverlap_ = new JFormattedTextField();
                    textOverlap_.setColumns(8);
                    panelSize.add(new Label("Overlap:"));
                    panelSize.add(textOverlap_);

                    panelVectorDB.add(panelSize);
                }
                panelLeft.add(panelVectorDB);
            }
            this.add(panelLeft);
        }
        {
            JPanel panelRight = new JPanel();
            panelRight.setLayout(new BoxLayout(panelRight, BoxLayout.Y_AXIS));
            {
                JList listLogs = new JList();
                listLogs.setMinimumSize(new Dimension(320, 168));
                listLogs.setMaximumSize(new Dimension(320, 168));
                listLogs.setVisibleRowCount(100);
                panelRight.add(listLogs);

                {
                    JPanel panelButtons = new JPanel();
                    panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
                    panelButtons.setAlignmentX(Component.RIGHT_ALIGNMENT);

                    buttonRun_ = new JButton("Run");
                    buttonRun_.setEnabled(true);
                    buttonRun_.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            MainFrame mainFrame = (MainFrame)getFrames()[0];
                            mainFrame.onClickRun();
                        }
                    });

                    buttonCancel_ = new JButton("Cancel");
                    buttonCancel_.setEnabled(false);
                    buttonCancel_.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            MainFrame mainFrame = (MainFrame)getFrames()[0];
                            mainFrame.onClickCancel();
                        }
                    });

                    panelButtons.add(buttonRun_);
                    panelButtons.add(buttonCancel_);
                    panelRight.add(panelButtons);
                }

            }
            this.add(panelRight);
        }
        load_settings();
        setVisible(true);
    }

    private class ValueChanged implements DocumentListener {

        private MainFrame mainFrame_;
        public ValueChanged(MainFrame mainFrame){
            mainFrame_ = mainFrame;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            mainFrame_.setChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            mainFrame_.setChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            mainFrame_.setChanged();
        }
    }

    private class WindowClosing extends WindowAdapter{
        private MainFrame mainFrame_;
        public WindowClosing(MainFrame mainFrame)
        {
           mainFrame_ = mainFrame;
        }

        public void windowClosing(WindowEvent e){
            mainFrame_.save_settings();
            System.exit(0);
        }
    }

    public void onClickOpen()
    {
        try{
            JFileChooser filechooser = new JFileChooser();
            filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int selected = filechooser.showDialog(this, "Open");
            if(selected == JFileChooser.APPROVE_OPTION){
                File file = filechooser.getSelectedFile();
                if(file.exists() && file.isDirectory()){
                    textRoot_.setText(file.getPath());
                }
            }
        }catch (Exception ignored)
        {

        }
    }

    public class CrawlTask extends Thread {
        private MainFrame mainFrame_;
        private Settings settings_;
        private boolean cancelRequested_ = false;
        private DocumentSplitter documentSplitter_;
        private Connection connection_;
        private Statement statement_;
        private HttpClient embeddingClient_;
        private Client chromaClient_;
        private Collection collection_;

        private final Tika tika_ = new Tika();
        public CrawlTask(MainFrame mainFrame, Settings settings) {
            mainFrame_ = mainFrame;
            settings_ = settings;
            documentSplitter_ = DocumentSplitters.recursive(settings_.getSize(), settings_.getOverlap());
        }
        private String getTableName()
        {
            //String path = settings_.getRoot().replace(':','_').replace('\\','_');
            String baseName = settings_.getCollection();
            return baseName.toLowerCase();
        }

        @Override
        public void run(){
            try{
                mainFrame_.onCrawlStart();
                connection_ = DriverManager.getConnection(MainFrame.DBURL);
                statement_ = connection_.createStatement();
                String query = "CREATE TABLE IF NOT EXISTS "
                        + getTableName()
                        + " (url TEXT PRIMARY KEY,"
                        + " last_update INTEGER)";
                statement_.execute(query);

                String chromaUrl = "http://" + settings_.getHost();
                chromaUrl = 0<settings_.getPort()? chromaUrl + ":" + settings_.getPort() : chromaUrl;
                chromaClient_ = new Client(chromaUrl);
                EmbeddingFunction embeddingFunction = new CohereEmbeddingFunction("");
                collection_ = chromaClient_.createCollection(settings_.getCollection(), null, true, embeddingFunction);

                embeddingClient_ = HttpClient.newHttpClient();
                File root = new File(settings_.getRoot());
                walk(root);
            }catch (Exception e){
                System.out.println(e);
            }finally {
                chromaClient_ = null;
                collection_ = null;
                if(null != statement_){
                    try{
                        statement_.close();
                    }catch (Exception e){
                    }
                }
                if(null != connection_){
                    try {
                        connection_.close();
                    }catch (Exception e){
                    }
                }
                mainFrame_.onCrawlEnd();
            }
        }
        private void walk(File root)
        {
            if(isCancelRequested()){
                return;
            }
            File[] files = root.listFiles();
            if(null==files){
                return;
            }
            for(int i=0; i<files.length; ++i){
                if(files[i].isDirectory()){
                    continue;
                }
                proc(files[i]);
            }
            for(int i=0; i<files.length; ++i){
                if(!files[i].isDirectory()){
                    continue;
                }
                walk(files[i]);
            }
        }
        private final Object lock_ = new Object();
        private boolean isCancelRequested()
        {
            synchronized (lock_){
                return cancelRequested_;
            }
        }
        public void requestCancel()
        {
            synchronized (lock_){
                cancelRequested_ = true;
            }
        }

        private void proc(File file)
        {
            String tableName = getTableName();
            try{
                String query = "SELECT * FROM " + tableName + " WHERE url='" + file.getAbsolutePath() + "'";
                ResultSet resultSet = statement_.executeQuery(query);
                while (resultSet.next()){
                    long last_update = resultSet.getLong("last_update");
                    if(file.lastModified()<=last_update){
                        return;
                    }
                }
            } catch (SQLException e) {
            }
            try(InputStream input = new BufferedInputStream(new FileInputStream(file.getAbsolutePath()))){
                tika_.detect(file);
                MediaType mediaType = MediaType.parse(tika_.detect(file));
                if(0 == mediaType.getType().compareToIgnoreCase("font")){
                    return;
                }else if(0 == mediaType.getType().compareToIgnoreCase("example")){
                    return;
                }
                BodyContentHandler handler = new BodyContentHandler(-1);
                org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
                Parser parser = new AutoDetectParser(tikaConfig_);
                parser.parse(input, handler, metadata, new ParseContext());
                String text = handler.toString();
                if(text.isBlank()){
                    return;
                }
                DocumentSplitter documentSplitter = DocumentSplitters.recursive(settings_.getSize(), settings_.getOverlap());
                Document document = new Document(text);
                java.util.List<TextSegment> segments = documentSplitter.split(document);
                int no = 0;
                for(TextSegment segment : segments){
                    upsert(segment, file, no);
                    ++no;
                }

                String query = "INSERT INTO " + tableName + "(url,last_update) VALUES(\'" + file.getAbsolutePath() + "\'," + file.lastModified() + ")"
                         + " ON CONFLICT(url) DO UPDATE SET last_update=excluded.last_update;";
                statement_.execute(query);

            }catch (IOException e){
                System.out.println(e);
                return;
            } catch (TikaException e) {
                return;
            } catch (SAXException e) {
                return;
            } catch (SQLException e) {
                System.out.println(e);
                return;
            }
        }

        private void upsert(TextSegment segment, File file, int no)
        {
            //Create embedding
            EmbeddingRequest embeddingRequest = new EmbeddingRequest();
            embeddingRequest.setModel_("multilingual-e5-large");
            embeddingRequest.setInput_(segment.text());
            try {
                ObjectMapper mapper = new JsonMapper();
                String content = mapper.writeValueAsString(embeddingRequest);
                String uri = 0 == settings_.getEmbeddingPort() ? "http://" + settings_.getEmbeddingHost() : "http://" + settings_.getEmbeddingHost() + ":" + settings_.getEmbeddingPort();
                uri += "/v1/embeddings";
                HttpRequest request = HttpRequest.newBuilder(URI.create(uri))
                        .setHeader("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(content))
                        .build();
                HttpResponse<String> response = embeddingClient_.send(request, HttpResponse.BodyHandlers.ofString());
                if(response.statusCode() != 200) {
                    return;
                }
                EmbeddingResponse embeddingResponse = mapper.readValue(response.body(), EmbeddingResponse.class);
                {
                    java.util.List<java.util.List<Float>> embeddings = new ArrayList<>();
                    embeddings.add(embeddingResponse.getData_().get(0).getEmbedding_());
                    java.util.List<String> documents = new ArrayList<>();
                    documents.add(segment.text());
                    java.util.List<java.util.Map<String,String>> metadatas = new ArrayList<Map<String, String>>();
                    Map<String,String> metadata = new HashMap<>();
                    metadata.put("url", file.getAbsolutePath());
                    metadatas.add(metadata);
                    String id = file.getAbsolutePath() + "_" + String.format("%04d", no);
                    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                    byte[] id_bytes = sha1.digest(id.getBytes());
                    java.util.List<String> ids = new ArrayList<>();
                    ids.add(String.format("%040x", new BigInteger(1, id_bytes)));
                    collection_.upsert(embeddings, metadatas, documents, ids);
                }
            } catch (IOException | InterruptedException e) {
                System.out.println(e.toString());
            } catch (ApiException e) {
                System.out.println(e.toString());
            } catch (NoSuchAlgorithmException e) {
                System.out.println(e.toString());
            }
        }
    }

    public void onCrawlStart()
    {
        buttonRun_.setEnabled(false);
        buttonCancel_.setEnabled(true);
    }

    public  void onCrawlEnd()
    {
        crawlTask_ = null;
        buttonRun_.setEnabled(true);
        buttonCancel_.setEnabled(false);
    }

    public void onClickRun()
    {
        if(null != crawlTask_){
            return;
        }
        save_settings();
        if(!checkRoot(settings_.getRoot())){
            return;
        }
        if(!checkCollection(settings_.getCollection())){
            return;
        }
        crawlTask_ = new CrawlTask(this, settings_);
        crawlTask_.start();
    }

    public void onClickCancel()
    {
        if(null == crawlTask_){
            return;
        }
        crawlTask_.requestCancel();
    }

    public void print(String message)
    {
        System.out.println(message);
    }
    public static void main(String[] args)
    {
        new MainFrame();
    }
}
