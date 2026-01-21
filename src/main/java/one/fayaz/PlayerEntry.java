package one.fayaz;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerEntry {
    private final UUID uuid;
    private final String name;
    private final int color;
    List<ClaimData> claims;


    public PlayerEntry(UUID uuid, String name, int color) {
        this.uuid = uuid;
        this.name = name;
        this.color = color;
        this.claims = new ArrayList<>();
    }

    public PlayerEntry(UUID uuid, String name, int color, List<ClaimData> claims) {
        this.uuid = uuid;
        this.name = name;
        this.color = color;
        this.claims = new ArrayList<>(claims);
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

    public List<ClaimData> getClaims() {
        return claims;
    }

    public void addClaim(String id, GoalType type) {
        claims.add(new ClaimData(id, type));
    }

    public int getScore() {
        return claims.size();
    }
}