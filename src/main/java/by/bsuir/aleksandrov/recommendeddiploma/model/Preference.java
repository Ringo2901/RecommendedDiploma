package by.bsuir.aleksandrov.recommendeddiploma.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Preference {
    private String itemId;
    private double rating;
    private double minBound;
    private double maxBound;

    public boolean isLiked() {
        return rating >= maxBound / 2 + 1;
    }

    public boolean isContained(String id){
        return itemId.equals(id);
    }
}
