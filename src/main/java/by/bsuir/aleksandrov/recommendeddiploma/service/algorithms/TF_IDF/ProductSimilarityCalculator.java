package by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.TF_IDF;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import by.bsuir.aleksandrov.recommendeddiploma.model.SchemaField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductSimilarityCalculator {
    private final Directory index;
    private IndexSearcher searcher;
    private final Analyzer analyzer;
    private final List<SchemaField> importanceCoefficients;

    public ProductSimilarityCalculator(List<SchemaField> importanceCoefficients) throws IOException {
        this.index = new RAMDirectory();
        this.analyzer = new SimpleAnalyzer();
        this.importanceCoefficients = importanceCoefficients;
    }

    public void initializeIndex(List<Item> entities) throws IOException {
        for (Item entity : entities) {
            addProductToIndex(entity);
        }
    }

    public void addProductToIndex(Item product) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(index, config)) {
            Document doc = new Document();
            FieldType fieldType = new FieldType();
            fieldType.setStored(true);
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
            fieldType.setTokenized(true);
            doc.add(new Field("text", product.toText(importanceCoefficients), fieldType));
            writer.addDocument(doc);
        }
    }

    private void initializeSearcher() throws IOException {
        this.searcher = new IndexSearcher(DirectoryReader.open(index));
    }

    public double calculateSimilarity(Item product1, Item product2) throws IOException {
        if (searcher == null) {
            initializeSearcher();
        }

        String text1 = product1.toText(importanceCoefficients);
        String text2 = product2.toText(importanceCoefficients);

        Map<String, Double> vector1 = convertToTFIDFVector(text1);
        Map<String, Double> vector2 = convertToTFIDFVector(text2);

        return calculateCosineSimilarity(vector1, vector2);
    }

    private double calculateCosineSimilarity(Map<String, Double> vector1, Map<String, Double> vector2) {
        double dotProduct = 0;
        double magnitude1 = 0;
        double magnitude2 = 0;

        for (Map.Entry<String, Double> entry : vector1.entrySet()) {
            String term = entry.getKey();
            double tfidf1 = entry.getValue();
            double tfidf2 = vector2.getOrDefault(term, 0.0);

            dotProduct += tfidf1 * tfidf2;
            magnitude1 += Math.pow(tfidf1, 2);
        }
        for (Map.Entry<String, Double> entry : vector2.entrySet()) {
            magnitude2 += Math.pow(entry.getValue(), 2);
        }

        return (magnitude1 == 0 || magnitude2 == 0) ? 0 : dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private Map<String, Double> convertToTFIDFVector(String text) throws IOException {
        Map<String, Double> tfidfVector = new HashMap<>();

        try (TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(text))) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            int totalTerms = 0;
            Map<String, Integer> termFrequency = new HashMap<>();
            while (tokenStream.incrementToken()) {
                String term = termAttribute.toString();
                termFrequency.put(term, termFrequency.getOrDefault(term, 0) + 1);
                totalTerms++;
            }

            IndexReader indexReader = DirectoryReader.open(index);
            int numDocs = indexReader.numDocs();
            for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
                String term = entry.getKey();
                double tf = (double) entry.getValue() / totalTerms;
                double idf = Math.log((double) numDocs / (indexReader.docFreq(new Term("text", term)) + 1));
                tfidfVector.put(term, tf * idf);
            }

            tokenStream.end();
        }

        return tfidfVector;
    }
}
