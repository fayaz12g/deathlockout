package one.fayaz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEntry {
    private final UUID uuid;
    private final String name;
    private final int color;
    private final List<String> deaths;

    public PlayerEntry(UUID uuid, String name, int color) {
        this.uuid = uuid;
        this.name = name;
        this.color = color;
        this.deaths = new ArrayList<>();
    }

    public PlayerEntry(UUID uuid, String name, int color, List<String> deaths) {
        this.uuid = uuid;
        this.name = name;
        this.color = color;
        this.deaths = new ArrayList<>(deaths);
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public List<String> getDeaths() {
        return deaths;
    }

    public void addDeath(String death) {
        deaths.add(death);
    }

    public int getScore() {
        return deaths.size();
    }
}