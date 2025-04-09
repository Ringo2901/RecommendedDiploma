package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalTime;

@Document(collection = "dbMetrics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseMetrics {

    @Id
    private String id;
    private long timestamp;
    private String timestampReadable;
    private int connections;
    private int memory;
    private long reads;
    private long writes;

    public DatabaseMetrics(long timestamp, String timestampReadable, int connections, int memory, long reads, long writes) {
        this.timestamp = timestamp;
        this.timestampReadable = timestampReadable;
        this.connections = connections;
        this.memory = memory;
        this.reads = reads;
        this.writes = writes;
    }
}
