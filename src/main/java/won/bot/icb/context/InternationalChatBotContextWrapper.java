package won.bot.icb.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;
import won.bot.icb.utils.ChatClient;
import won.bot.icb.utils.ICBAtomModelWrapper;
import won.protocol.model.Coordinate;
import won.protocol.vocabulary.WXCHAT;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;

public class InternationalChatBotContextWrapper extends ServiceAtomEnabledBotContextWrapper {
    private final String connectedSocketsMap;
    private final HashSet<ChatClient> unmatchedChatClients = new HashSet<>();
    private final HashSet<ChatClient> matchedChatClients = new HashSet<>();
    private static String translateURI; // translation atom URI
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int conID = 0;

    public InternationalChatBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
        this.connectedSocketsMap = botName + ":connectedSocketsMap";
    }

    public Map<URI, Set<URI>> getConnectedSockets() {
        Map<String, List<Object>> connectedSockets = getBotContext().loadListMap(connectedSocketsMap);
        Map<URI, Set<URI>> connectedSocketsMapSet = new HashMap<>(connectedSockets.size());

        for (Map.Entry<String, List<Object>> entry : connectedSockets.entrySet()) {
            URI senderSocket = URI.create(entry.getKey());
            Set<URI> targetSocketsSet = new HashSet<>(entry.getValue().size());
            for (Object o : entry.getValue()) {
                targetSocketsSet.add((URI) o);
            }
            connectedSocketsMapSet.put(senderSocket, targetSocketsSet);
        }

        return connectedSocketsMapSet;
    }

    public void addConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().addToListMap(connectedSocketsMap, senderSocket.toString(), targetSocket);
    }

    public void removeConnectedSocket(URI senderSocket, URI targetSocket) {
        getBotContext().removeFromListMap(connectedSocketsMap, senderSocket.toString(), targetSocket);
    }

    public String getBotChatSocketURI() {
        return getServiceAtomUri().toString() + "#chatSocket";
    }

    public String getTranslateURI() {
        return translateURI;
    }

    public static void setTranslateURI(String translateURI) {
        InternationalChatBotContextWrapper.translateURI = translateURI;
    }

    /**
     * Adds a Chat Partner
     *
     * @param atomURI
     * @param ownCoord
     * @param reqCoord
     */
    public boolean addChatPartner(String atomURI, String chatSocketURI, Coordinate ownCoord, Coordinate reqCoord) {

        String sourceLong = Float.toString(ownCoord.getLongitude());
        String sourceLat = Float.toString(ownCoord.getLatitude());

        String targetLon = Float.toString(reqCoord.getLongitude());
        String targetLat = Float.toString(reqCoord.getLatitude());

        Optional<String> ownCCOpt = won.bot.icb.api.InternationalChatBotAPI.countryCodeOfGPS(sourceLat, sourceLong);
        if (!ownCCOpt.isPresent()) {
            return false;
        }
        String ownCC = ownCCOpt.get();

        Optional<String> reqCCOpt = won.bot.icb.api.InternationalChatBotAPI.countryCodeOfGPS(targetLat, targetLon);
        if (!reqCCOpt.isPresent()) {
            return false;
        }
        String reqCC = reqCCOpt.get();

        ChatClient toAdd = new ChatClient(atomURI, chatSocketURI, ownCC, reqCC, sourceLat, sourceLong, targetLat, targetLon);
        logger.info("Added Chatpartner: " + toAdd.toString());
        unmatchedChatClients.add(toAdd);

        return true;
    }


    public void matchChatPartners() {
        // TODO: this is stupid, maybe do it better?
        // try to match every atom with all other atoms
        for (ChatClient ucp1 : unmatchedChatClients) {
            for (ChatClient ucp2 : unmatchedChatClients) {
                // atoms match
                if (ucp1.getSourceCountryCode().equals(ucp2.getTargetCountryCode()) && ucp2.getSourceCountryCode().equals(ucp1.getTargetCountryCode())) {
                    ucp1.setConnectionID(conID);
                    ucp2.setConnectionID(conID++);
                    matchedChatClients.add(ucp1);
                    matchedChatClients.add(ucp2);
                    unmatchedChatClients.remove(ucp1);
                    unmatchedChatClients.remove(ucp2);
                    // TODO: start connection to both chat partners
                    String message = "Hello, let's connect! We found a partner for you! <3"; //optional welcome message

                    ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                            URI.create(getBotChatSocketURI()),
                            URI.create(ucp1.getChatSocketURI()),
                            message);
                    getEventBus().publish(connectCommandEvent);
                    // ----------------
                    logger.info("Successfully matched " + ucp1.getAtomURI() + " and " + ucp2.getAtomURI() + "with connection ID " + ucp1.getConnectionID());
                    return;
                }
            }
        }
    }

    public ChatClient getChatClient(String chatClientURI) {
        for (ChatClient c : matchedChatClients) {
            if (c.getAtomURI().equals(chatClientURI)) return c;
        }
        return null;
    }

    public ChatClient getChatPartner(String chatPartnerURI, int conID) {
        for (ChatClient c : matchedChatClients) {
            if (!c.getAtomURI().equals(chatPartnerURI) && c.getConnectionID() == conID) return c;
        }
        return null;
    }
}
