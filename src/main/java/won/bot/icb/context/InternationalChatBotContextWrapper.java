package won.bot.icb.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.bot.context.BotContext;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;
import won.bot.icb.utils.ChatPartner;
import won.protocol.model.Coordinate;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.*;

public class InternationalChatBotContextWrapper extends ServiceAtomEnabledBotContextWrapper {
    private final String connectedSocketsMap;
    private final HashSet<ChatPartner> unmatchedChatPartners = new HashSet<>();
    private final HashSet<ChatPartner> matchedChatPartners = new HashSet<>();
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    /**
     * Adds a Chat Partner
     *
     * @param atomURI
     * @param ownCoord
     * @param reqCoord
     */
    public boolean addChatPartner(String atomURI, Coordinate ownCoord, Coordinate reqCoord) {

        String ownLong = Float.toString(ownCoord.getLongitude());
        String ownLat = Float.toString(ownCoord.getLatitude());

        String reqLong = Float.toString(reqCoord.getLongitude());
        String reqLat = Float.toString(reqCoord.getLatitude());

        Optional<String> ownCCOpt = won.bot.icb.api.InternationalChatBotAPI.countryCodeOfGPS(ownLat, ownLong);
        if (!ownCCOpt.isPresent()) {
            return false;
        }
        String ownCC = ownCCOpt.get();

        Optional<String> reqCCOpt = won.bot.icb.api.InternationalChatBotAPI.countryCodeOfGPS(reqLat, reqLong);
        if (!reqCCOpt.isPresent()) {
            return false;
        }
        String reqCC = reqCCOpt.get();

        ChatPartner toAdd = new ChatPartner(atomURI, ownCC, reqCC);

        unmatchedChatPartners.add(toAdd);

        return true;
    }

    public void matchChatPartners() {
        // TODO: this is stupid, maybe do it better?
        // try to match every atom with all other atoms
        for (ChatPartner ucp1 : unmatchedChatPartners) {
            for (ChatPartner ucp2 : unmatchedChatPartners) {
                // atoms match
                if (ucp1.getOwnCountryCode().equals(ucp2.getReqCountryCode()) && ucp2.getOwnCountryCode().equals(ucp1.getReqCountryCode())) {
                    ucp1.setConnectedAtomURI(ucp2.getAtomURI());
                    ucp2.setConnectedAtomURI(ucp1.getAtomURI());
                    matchedChatPartners.add(ucp1);
                    matchedChatPartners.add(ucp2);
                    unmatchedChatPartners.remove(ucp1);
                    unmatchedChatPartners.remove(ucp2);
                    logger.info("Successfully matched " + ucp1.getAtomURI() + " and " + ucp2.getAtomURI());
                    return;
                }
            }
        }
    }
}
