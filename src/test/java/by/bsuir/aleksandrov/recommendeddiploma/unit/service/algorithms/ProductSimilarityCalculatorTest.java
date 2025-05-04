package by.bsuir.aleksandrov.recommendeddiploma.unit.service.algorithms;

import by.bsuir.aleksandrov.recommendeddiploma.model.Item;
import by.bsuir.aleksandrov.recommendeddiploma.model.SchemaField;
import by.bsuir.aleksandrov.recommendeddiploma.service.algorithms.TF_IDF.ProductSimilarityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSimilarityCalculatorTest {

    private ProductSimilarityCalculator calculator;

    private static class DummyItem extends Item {
        private final Map<String, String> fields;

        public DummyItem(Map<String, String> fields) {
            super("1", null);
            this.fields = fields;
        }

        @Override
        public String toText(List<SchemaField> schemaFields) {
            StringBuilder sb = new StringBuilder();
            for (SchemaField field : schemaFields) {
                String val = fields.getOrDefault(field.getName(), "");
                sb.append(" ".repeat((int) Math.round(field.getWeight()))).append(val).append(" ");
            }
            return sb.toString().trim();
        }
    }

    private List<SchemaField> schema;

    @BeforeEach
    void setup() throws IOException {
        schema = List.of(
                new SchemaField("name", 2.0),
                new SchemaField("category", 1.0)
        );
        calculator = new ProductSimilarityCalculator(schema);
    }

    @Test
    void testIndexInitialization() throws IOException {
        Item item1 = new DummyItem(Map.of("name", "laptop", "category", "electronics"));
        Item item2 = new DummyItem(Map.of("name", "phone", "category", "electronics"));

        calculator.initializeIndex(List.of(item1, item2));
        double similarity = calculator.calculateSimilarity(item1, item2);

        assertTrue(similarity >= 0 && similarity <= 1, "Similarity should be between 0 and 1");
    }

    @Test
    void testCosineSimilarity_sameItems() throws IOException {
        Item item = new DummyItem(Map.of("name", "tablet", "category", "tech"));
        calculator.initializeIndex(List.of(item));

        double sim = calculator.calculateSimilarity(item, item);
        assertEquals(1.0, sim, 0.0001);
    }

    @Test
    void testCosineSimilarity_differentItems() throws IOException {
        Item item1 = new DummyItem(Map.of("name", "laptop", "category", "tech"));
        Item item2 = new DummyItem(Map.of("name", "banana", "category", "fruit"));

        calculator.initializeIndex(List.of(item1, item2));
        double sim = calculator.calculateSimilarity(item1, item2);

        assertTrue(sim < 0.5, "Completely different items should have low similarity");
    }

    @Test
    void testCosineSimilarity_emptyVectors() throws IOException {
        // Fields not used in schema; will result in empty vectors
        Item item1 = new DummyItem(Map.of("irrelevant", "xxx"));
        Item item2 = new DummyItem(Map.of("irrelevant", "yyy"));

        calculator.initializeIndex(List.of(item1, item2));
        double sim = calculator.calculateSimilarity(item1, item2);

        assertEquals(0.0, sim);
    }
}

