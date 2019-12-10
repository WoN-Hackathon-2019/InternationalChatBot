package won.bot.icb.context;

import won.bot.framework.bot.context.BotContext;
import won.bot.framework.extensions.serviceatom.ServiceAtomEnabledBotContextWrapper;
import won.bot.icb.utils.ChatPartner;
import won.protocol.model.Coordinate;

import java.net.URI;
import java.util.*;

public class InternationalChatBotContextWrapper extends ServiceAtomEnabledBotContextWrapper {
    private final String connectedSocketsMap;
    private final HashSet<ChatPartner> waitingChatPartners = new HashSet<>();

    public InternationalChatBotContextWrapper(BotContext botContext, String botName) {
        super(botContext, botName);
        this.connectedSocketsMap = botName + ":connectedSocketsMap";
    }

    public Map<URI, Set<URI>> getConnectedSockets() {
        Map<String, List<Object>> connectedSockets = getBotContext().loadListMap(connectedSocketsMap);
        Map<URI, Set<URI>> connectedSocketsMapSet = new HashMap<>(connectedSockets.size());

        for(Map.Entry<String, List<Object>> entry : connectedSockets.entrySet()) {
            URI senderSocket = URI.create(entry.getKey());
            Set<URI> targetSocketsSet = new HashSet<>(entry.getValue().size());
            for(Object o : entry.getValue()) {
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
     * @param atomURI
     * @param ownCoord
     * @param reqCoord
     */
    public boolean addChatPartner(String atomURI, Coordinate ownCoord, Coordinate reqCoord){

        String ownLong = Float.toString(ownCoord.getLongitude());
        String ownLat = Float.toString(ownCoord.getLatitude());

        String reqLong = Float.toString(reqCoord.getLongitude());
        String reqLat = Float.toString(reqCoord.getLatitude());

        Optional<String> ownCCOpt = won.bot.icb.api.InternationalChatBotAPI.countryCodeOfGPS(ownLat, ownLong);
        if(!ownCCOpt.isPresent()){
            return false;
        }
        String ownCC = ownCCOpt.get();

        Optional<String> reqCCOpt = won.bot.icb.api.InternationalChatBotAPI.countryCodeOfGPS(reqLat, reqLong);
        if(!reqCCOpt.isPresent()){
            return false;
        }
        String reqCC = reqCCOpt.get();

        ChatPartner toAdd = new ChatPartner(atomURI, ownCC, reqCC);

        return true;
    }


}
