package ie.tcd.dalyc24;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TopDocs;
import java.io.PrintWriter;

public class CreateIndex {

  // Directory where the search index will be saved
  private static String INDEX_DIRECTORY = "../newIndex";
  private static int MAX_RESULTS = 50; // want top 50 results

  public static void main(String[] args) throws IOException, ParseException {
    ArrayList<ArrayList> lines = organiseData("./cran/cran.all.1400");
    String analyzerName = "";

    Analyzer analyzer = new SimpleAnalyzer();
    analyzerName = "simple";

    // Analyzer analyzer = new EnglishAnalyzer();
    // analyzerName = "english";

    //createIndex(lines, analyzer);
    runQuery(analyzer, analyzerName);

  }

  public static void runQuery(Analyzer analyzer, String analyzerName) throws IOException, ParseException { 
    try {

      Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
      DirectoryReader ireader = DirectoryReader.open(directory);
      IndexSearcher isearcher = new IndexSearcher(ireader);

      // setting scoring method
      String scoringMethod = "BM25";
      isearcher.setSimilarity(new BM25Similarity());
      // String scoringMethod = "Classic";
      // isearcher.setSimilarity(new ClassicSimilarity());

      
      String fileName = "./results/"+analyzerName+"_"+scoringMethod+"query_results.txt";

      MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[] { "title", "author", "text" },
          analyzer);
      queryParser.setAllowLeadingWildcard(true); // prevents error with '?' in text

      PrintWriter iwriter = new PrintWriter(fileName, "UTF-8");


      ArrayList<ArrayList> lines = organiseData("./cran/cran.qry");
      for (int i = 0; i < lines.size(); i++) {
        ArrayList<String> term = lines.get(i);

        for (int j = 0; j < term.size(); j++) { // for each query run query and store results in text file
          String queryString = (term.get(j));
          Query query = queryParser.parse(queryString);
          TopDocs docs = isearcher.search(query, MAX_RESULTS);
          ScoreDoc[] hits = docs.scoreDocs;
          //System.out.println("length is " + hits.length);
          for (int z = 0; z < hits.length; z++) {  // queryId / Q0(ignored) / docId / rank(ignored) / score / standard
            Document doc = isearcher.doc(hits[z].doc);
            iwriter.println((i + 1) + " 0 " + doc.get("id") + " " + z + " " + hits[z].score + " " + scoringMethod);
          }
        }
      }

      iwriter.flush();
      iwriter.close();
    } catch (IOException e) {
      System.out.println("Error " + e);
    } catch (ParseException e) {
      System.out.println("Error " + e);
    }

  }

  public static void createIndex(ArrayList<ArrayList> lines, Analyzer analyzer) throws IOException { // creates index
    Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
    IndexWriterConfig config = new IndexWriterConfig(analyzer);

    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

    IndexWriter iwriter = new IndexWriter(directory, config);
    for (int i = 0; i < lines.size(); i++) {
      Document doc = new Document();

      ArrayList<String> item = lines.get(i);
      doc.add(new StringField("id", String.valueOf(i + 1), Field.Store.YES));
      doc.add(new TextField("title", item.get(0), Field.Store.YES));
      doc.add(new TextField("author", item.get(1), Field.Store.YES));
      doc.add(new TextField("text", item.get(3), Field.Store.YES));
      iwriter.addDocument(doc);
    }

    // Commit changes and close everything
    iwriter.close();
    directory.close();
  }

  public static String removePre(String value) {
    return (value.substring(2).trim()); // remove trailing spaces and precursor e.g .I or .W
  }

  public static ArrayList<ArrayList> organiseData(String filename) { // prepares data for indexing/searching
    BufferedReader reader;
    ArrayList<ArrayList> lines = new ArrayList<ArrayList>();
    ArrayList<String> nextArray = new ArrayList<String>();
    String concatLine = "";
    try {
      reader = new BufferedReader(new FileReader(filename));
      String line = reader.readLine();
      while (line != null) {
        if (line.startsWith(".I")) {
          if (concatLine != "") {
            nextArray.add(removePre(concatLine));
          }
          if (nextArray.size() > 0) {
            lines.add(nextArray);
          }
          nextArray = new ArrayList<String>();
          concatLine = "";
        } else if (line.startsWith(".A") || line.startsWith(".B") || line.startsWith(".W") || line.startsWith(".T")) {
          if (concatLine != "") {
            nextArray.add(removePre(concatLine));
          }
          concatLine = line;
        } else {
          concatLine += line;
        }
        line = reader.readLine();
      }
      nextArray.add(removePre(concatLine)); // flush values
      lines.add(nextArray);

      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

}
