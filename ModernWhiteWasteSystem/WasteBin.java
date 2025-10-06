import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class WasteBin {
    private String id, location, type;
    private int capacity, currentLevel;
    private LocalDateTime lastUpdated;

    public WasteBin(String id, String location, int capacity, String type) {
        this(id, location, capacity, type, new Random().nextInt(50));
    }

    public WasteBin(String id, String location, int capacity, String type, int level) {
        this.id = id;
        this.location = location;
        this.capacity = capacity;
        this.type = type;
        this.currentLevel = level;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public int getCurrentLevel() { return currentLevel; }

    public void setCurrentLevel(int level) { 
        this.currentLevel = level; 
        this.lastUpdated = LocalDateTime.now(); 
    }

    public String getStatus() {
        if(currentLevel >= 80) return "CRITICAL";
        if(currentLevel >= 60) return "WARNING";
        return "NORMAL";
    }

    public String getLastUpdated() {
        return lastUpdated.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
