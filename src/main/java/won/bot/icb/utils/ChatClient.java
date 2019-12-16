package won.bot.icb.utils;

import won.protocol.model.Connection;

public class ChatClient {

    private String atomURI;
    private String chatSocketURI;
    private String sourceCountryCode, targetCountryCode;
    private int connectionID;
    private Connection connection;
    private String sourceLat, sourceLon, targetLat, targetLon;

    public ChatClient(String atomURI, String chatSocketURI, String sourceCountryCode, String targetCountryCode, String sourceLat, String sourceLon, String targetLat, String targetLon) {
        this.atomURI = atomURI;
        this.chatSocketURI = chatSocketURI;
        this.sourceCountryCode = sourceCountryCode;
        this.targetCountryCode = targetCountryCode;
        this.sourceLat = sourceLat;
        this.sourceLon = sourceLon;
        this.targetLat = targetLat;
        this.targetLon = targetLon;
    }

    public String getChatSocketURI() {
        return chatSocketURI;
    }

    public void setChatSocketURI(String chatSocketURI) {
        this.chatSocketURI = chatSocketURI;
    }

    public String getSourceLat() {
        return sourceLat;
    }

    public void setSourceLat(String sourceLat) {
        this.sourceLat = sourceLat;
    }

    public String getSourceLon() {
        return sourceLon;
    }

    public void setSourceLon(String sourceLon) {
        this.sourceLon = sourceLon;
    }

    public String getTargetLat() {
        return targetLat;
    }

    public void setTargetLat(String targetLat) {
        this.targetLat = targetLat;
    }

    public String getTargetLon() {
        return targetLon;
    }

    public void setTargetLon(String targetLon) {
        this.targetLon = targetLon;
    }

    public String getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(String atomURI) {
        this.atomURI = atomURI;
    }

    public String getSourceCountryCode() {
        return sourceCountryCode;
    }

    public void setSourceCountryCode(String sourceCountryCode) {
        this.sourceCountryCode = sourceCountryCode;
    }

    public String getTargetCountryCode() {
        return targetCountryCode;
    }

    public void setTargetCountryCode(String targetCountryCode) {
        this.targetCountryCode = targetCountryCode;
    }

    public int getConnectionID() {
        return connectionID;
    }

    public void setConnectionID(int connectionID) {
        this.connectionID = connectionID;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public String toString() {
        return "ChatClient{" +
                "atomURI='" + atomURI + '\'' +
                ", sourceCountryCode='" + sourceCountryCode + '\'' +
                ", targetCountryCode='" + targetCountryCode + '\'' +
                ", connectionID='" + connectionID + '\'' +
                '}';
    }
}
