package by.bsuir.aleksandrov.recommendeddiploma.service.database;

import by.bsuir.aleksandrov.recommendeddiploma.model.DatabaseMetrics;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DatabaseMetricsService {

    @Autowired
    private MongoTemplate mongoTemplate;
    private long lastRead = 0;
    private long lastWrite = 0;

    // Этот метод будет вызываться каждые 5 минут
    @Scheduled(fixedRate = 5 * 60 * 1000)  // 5 минут в миллисекундах
    public void collectAndSaveMetrics() {
        Document serverStatus = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));

        Document connections = serverStatus.get("connections", Document.class);
        Document mem = serverStatus.get("mem", Document.class);
        Document opcounters = serverStatus.get("opcounters", Document.class);

        long timestamp = new Date().getTime();
        String timestampReadable = convertTimestampToReadable(timestamp);
        int connectionCount = connections != null ? connections.getInteger("current", 0) : 0;
        int memoryUsage = mem != null ? mem.getInteger("resident", 0) : 0;
        long currentReads = opcounters != null ? opcounters.getLong("query") : 0;
        long currentWrites = opcounters != null ? opcounters.getLong("insert") + opcounters.getLong("update") : 0;

        long tempWrite = lastWrite;
        long tempRead = lastRead;

        lastRead = currentReads;
        lastWrite = currentWrites;

        currentReads -= tempRead;
        currentWrites -= tempWrite;

        // Создаем объект метрик и сохраняем его в MongoDB
        DatabaseMetrics metrics = new DatabaseMetrics(timestamp, timestampReadable, connectionCount, memoryUsage, currentReads, currentWrites);
        mongoTemplate.insert(metrics);
    }


    // Преобразуем метку времени в человекочитаемый формат
    public String convertTimestampToReadable(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
}