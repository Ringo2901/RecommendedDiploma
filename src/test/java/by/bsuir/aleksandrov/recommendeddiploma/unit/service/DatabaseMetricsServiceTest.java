package by.bsuir.aleksandrov.recommendeddiploma.unit.service;

import by.bsuir.aleksandrov.recommendeddiploma.model.DatabaseMetrics;
import by.bsuir.aleksandrov.recommendeddiploma.service.database.DatabaseMetricsService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Date;

import static org.mockito.Mockito.*;

class DatabaseMetricsServiceTest {

    @InjectMocks
    private DatabaseMetricsService service;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private com.mongodb.client.MongoDatabase mongoDatabase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mongoTemplate.getDb()).thenReturn(mongoDatabase);
    }

    @Test
    void testCollectAndSaveMetrics() {
        // Подготовка фейковых данных serverStatus
        Document connections = new Document("current", 42);
        Document mem = new Document("resident", 512);
        Document opcounters = new Document("query", 100L).append("insert", 60L).append("update", 40L);

        Document serverStatus = new Document()
                .append("connections", connections)
                .append("mem", mem)
                .append("opcounters", opcounters);

        when(mongoDatabase.runCommand(any(Document.class))).thenReturn(serverStatus);

        // Первый вызов (счётчики будут обнулены)
        service.collectAndSaveMetrics();

    }

    @Test
    void testConvertTimestampToReadable() {
        long testTimestamp = 1714819200000L; // 2024-05-04 00:00:00 GMT
        String readable = service.convertTimestampToReadable(testTimestamp);
        assert readable.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }
}

