public class Building {
    private final String id;
    private final String name;
    private final String address;
    private final int floors;
    private final double latitude;
    private final double longitude;

    public Building(String id, String name, String address, int floors, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.floors = floors;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public int getFloors() { return floors; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}