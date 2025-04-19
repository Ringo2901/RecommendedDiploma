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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProductSimilarityCalculator {
    private final Directory index;
    private final Analyzer analyzer;
    private final List<SchemaField> importanceCoefficients;
    private volatile IndexSearcher searcher;

    public ProductSimilarityCalculator(List<SchemaField> importanceCoefficients) throws IOException {
        this.index = new RAMDirectory();
        this.analyzer = new SimpleAnalyzer();
        this.importanceCoefficients = importanceCoefficients;
    }

    public synchronized void initializeIndex(List<Item> entities) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        try (IndexWriter writer = new IndexWriter(index, config)) {
            for (Item entity : entities) {
                addProductToIndex(writer, entity);
            }
        }
        initializeSearcher();
    }

    private void addProductToIndex(IndexWriter writer, Item product) throws IOException {
        Document doc = new Document();
        FieldType fieldType = new FieldType();
        fieldType.setStored(true);
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setTokenized(true);
        doc.add(new Field("text", product.toText(importanceCoefficients), fieldType));
        writer.addDocument(doc);
    }

    private synchronized void initializeSearcher() throws IOException {
        if (searcher == null) {
            searcher = new IndexSearcher(DirectoryReader.open(index));
        }
    }

    public double calculateSimilarity(Item product1, Item product2) throws IOException {
        initializeSearcher();

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
        for (double value : vector2.values()) {
            magnitude2 += Math.pow(value, 2);
        }

        return (magnitude1 == 0 || magnitude2 == 0) ? 0 : dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private Map<String, Double> convertToTFIDFVector(String text) throws IOException {
        Map<String, Double> tfidfVector = new ConcurrentHashMap<>();

        try (TokenStream tokenStream = analyzer.tokenStream("text", new StringReader(text))) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            AtomicInteger totalTerms = new AtomicInteger(0);
            Map<String, AtomicInteger> termFrequency = new ConcurrentHashMap<>();

            while (tokenStream.incrementToken()) {
                String term = termAttribute.toString();
                termFrequency.computeIfAbsent(term, k -> new AtomicInteger(0)).incrementAndGet();
                totalTerms.incrementAndGet();
            }

            IndexReader indexReader = DirectoryReader.open(index);
            int numDocs = indexReader.numDocs();

            termFrequency.forEach((term, freq) -> {
                double tf = (double) freq.get() / totalTerms.get();
                try {
                    double idf = Math.log((double) numDocs / (indexReader.docFreq(new Term("text", term)) + 1));
                    tfidfVector.put(term, tf * idf);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            tokenStream.end();
        }

        return tfidfVector;
    }
}

