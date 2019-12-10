package won.bot.icb.utils;

public class ChatPartner {

    private String atomURI;
    private String ownCountryCode, reqCountryCode;

    public ChatPartner(String atomURI, String ownCountryCode, String reqCountryCode) {
        this.atomURI = atomURI;
        this.ownCountryCode = ownCountryCode;
        this.reqCountryCode = reqCountryCode;
    }

    public String getAtomURI() {
        return atomURI;
    }

    public void setAtomURI(String atomURI) {
        this.atomURI = atomURI;
    }

    public String getOwnCountryCode() {
        return ownCountryCode;
    }

    public void setOwnCountryCode(String ownCountryCode) {
        this.ownCountryCode = ownCountryCode;
    }

    public String getReqCountryCode() {
        return reqCountryCode;
    }

    public void setReqCountryCode(String reqCountryCode) {
        this.reqCountryCode = reqCountryCode;
    }

    @Override
    public String toString() {
        return "ChatPartner{" +
                "atomURI='" + atomURI + '\'' +
                ", ownCountryCode='" + ownCountryCode + '\'' +
                ", reqCountryCode='" + reqCountryCode + '\'' +
                '}';
    }
}
