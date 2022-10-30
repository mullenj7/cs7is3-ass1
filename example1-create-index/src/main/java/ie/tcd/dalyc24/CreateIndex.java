package ie.tcd.dalyc24;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import javax.print.Doc;

import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.TopDocs;
import java.io.PrintWriter;


import java.util.HashMap;

// import org.apache.lucene.store.RAMDirectory;

public class CreateIndex {

  // Directory where the search index will be saved
  private static String INDEX_DIRECTORY = "../newIndex";
  private static int MAX_RESULTS = 50;

  public static void main(String[] args) throws IOException, ParseException {
    ArrayList<ArrayList> lines = organiseData("./cran/cran.all.1400");
    // Analyzer analyzer = new StandardAnalyzer();
    // Analyzer analyzer = new WhitespaceAnalyzer();
    Analyzer analyzer = new EnglishAnalyzer();
    // createIndex(lines, analyzer);
    runQuery(analyzer);

  }

  public static void runQuery(Analyzer analyzer) throws IOException, ParseException {
    try {

      Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));
      DirectoryReader ireader = DirectoryReader.open(directory);
      IndexSearcher isearcher = new IndexSearcher(ireader);
      String[] scores = new String[] { "BM25", "Classic", "Boolean" };

      isearcher.setSimilarity(new BM25Similarity());

      MultiFieldQueryParser queryParser = new MultiFieldQueryParser(new String[] { "title", "author", "text" },
          analyzer);
      queryParser.setAllowLeadingWildcard(true);

      PrintWriter iwriter = new PrintWriter("./results/query_results.txt", "UTF-8");


      ArrayList<ArrayList> lines = organiseData("./cran/cran.qry");
      for (int i = 0; i < lines.size(); i++) {
        ArrayList<String> term = lines.get(i);

        for (int j = 0; j < term.size(); j++) {
          String queryString = (term.get(j));
          System.out.println("term is " + queryString);

          Query query = queryParser.parse(queryString);
          TopDocs docs = isearcher.search(query, MAX_RESULTS);
          ScoreDoc[] hits = docs.scoreDocs;
          System.out.println("length is " + hits.length);
        }
      }
    } catch (IOException e) {
      System.out.println("Error " + e);
    } catch (ParseException e) {
      System.out.println("Error " + e);
    }

  }

  public static ArrayList<String> applyAnalyzer(String term, Analyzer analyzer) throws IOException {
    ArrayList<String> returner = new ArrayList<String>();

    TokenStream stream = analyzer.tokenStream("content", term);
    CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

    try {
      stream.reset();

      while (stream.incrementToken()) {
        returner.add(termAtt.toString());
      }

      stream.end();
    } finally {
      stream.close();
    }
    return returner;
  }

  public static void createIndex(ArrayList<ArrayList> lines, Analyzer analyzer) throws IOException {
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
    return (value.substring(2).trim()); // remove trailing spaces
  }

  public static ArrayList<ArrayList> organiseData(String filename) {
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
      nextArray.add(removePre(concatLine));
      lines.add(nextArray);

      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

}
