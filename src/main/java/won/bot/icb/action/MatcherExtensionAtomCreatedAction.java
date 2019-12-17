package won.bot.icb.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.icb.context.InternationalChatBotContextWrapper;
import won.bot.icb.utils.ChatClient;
import won.bot.icb.utils.ICBAtomModelWrapper;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.model.Coordinate;
import won.protocol.util.DefaultAtomModelWrapper;
import won.protocol.util.linkeddata.LinkedDataSource;
import won.protocol.util.linkeddata.WonLinkedDataUtils;
import won.protocol.vocabulary.SCHEMA;
import won.protocol.vocabulary.WXCHAT;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class MatcherExtensionAtomCreatedAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // TranslateBot
    private static String translateBot = "ICBTranslateBot";
    private static URI translateBotSocketURI;
    private boolean translateBotFound = false;

    // InternationalChatBot
    private static String icbChatSocketURI;

    public MatcherExtensionAtomCreatedAction(EventListenerContext eventListenerContext) {
        super(eventListenerContext);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        EventListenerContext ctx = getEventListenerContext();
        if (!(event instanceof MatcherExtensionAtomCreatedEvent) || !(getEventListenerContext().getBotContextWrapper() instanceof InternationalChatBotContextWrapper)) {
            logger.error("MatcherExtensionAtomCreatedAction can only handle MatcherExtensionAtomCreatedEvent and only works with InternationalChatBotContextWrapper");
            return;
        }
        InternationalChatBotContextWrapper botContextWrapper = (InternationalChatBotContextWrapper) ctx.getBotContextWrapper();
        MatcherExtensionAtomCreatedEvent atomCreatedEvent = (MatcherExtensionAtomCreatedEvent) event;

        Map<URI, Set<URI>> connectedSocketsMapSet = botContextWrapper.getConnectedSockets();

        icbChatSocketURI = ((InternationalChatBotContextWrapper) ctx.getBotContextWrapper()).getBotChatSocketURI();

        // Atom that was created
        ICBAtomModelWrapper newAtom = new ICBAtomModelWrapper(atomCreatedEvent.getAtomData());

        // find TranslateBot
        try {
            if (!translateBotFound && newAtom.getContentPropertyStringValue(SCHEMA.NAME).equals(translateBot)) {
                logger.info("Bot found " + newAtom.getSomeName());
                translateBotSocketURI = new URI(getSocketURIStringOfAtom(newAtom,
                        ctx.getLinkedDataSource(),
                        URI.create(icbChatSocketURI)));
                logger.info("Bot Chat Socket URI: " + translateBotSocketURI);
                botContextWrapper.addConnectedSocket(URI.create(icbChatSocketURI), translateBotSocketURI);
                translateBotFound = true;
                botContextWrapper.setTranslateChatSocketURI(translateBotSocketURI.toString());
                ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                        URI.create(botContextWrapper.getBotChatSocketURI()),
                        translateBotSocketURI,
                        "Connection");
                ctx.getEventBus().publish(connectCommandEvent);
            }
        } catch (Exception e) {
            if (!(e instanceof won.protocol.exception.IncorrectPropertyCountException)) {
                logger.error("Connecting to Bot failed - Exception: " + e.toString());
            }
        }

        // filter ICB tag
        boolean passed = false;
        Collection<String> tags = newAtom.getAllTags();

        for (String t : tags) {
            if (t.equals("#ICB")) {
                passed = true;
                logger.info("New Atom with Tag for ICB found");
            }
        }

        if (passed) {
            String chatSocketURI = null;
            Collection<String> socketURIS = newAtom.getSocketUris();
            String chatSocketTypeURI = WXCHAT.ChatSocket.getURI();
            for (String s : socketURIS) {
                if (newAtom.getSocketType(s).isPresent() && newAtom.getSocketType(s).get().equals(chatSocketTypeURI)) {
                    chatSocketURI = s;
                }
            }

            if (chatSocketURI == null) {
                logger.error("Chat Socket URI is NULL");
            }

            if (addChatPartner(ctx, botContextWrapper, newAtom.getAtomUri(), chatSocketURI, newAtom.getLocationCoordinate(), newAtom.getSeeksLocationCoordinate())) {
                logger.info("New Chat Partner successfully added");
            } else {
                logger.error("Error while adding new Chat Partner");
            }

            // try to match chat partners
            logger.info("Trying to match chat partners");
            int conID = botContextWrapper.matchChatPartners();
            if (conID != -1) {
                ArrayList<ChatClient> clients = botContextWrapper.getConversationPartners(conID);
                clients.forEach(c -> {
                    WonMessage wonMessage = WonMessageBuilder
                            .connectionMessage()
                            .sockets()
                            .sender(URI.create(icbChatSocketURI))
                            .recipient(URI.create(c.getChatSocketURI()))
                            .content()
                            .text("We successfully matched you! Happy chatting!")
                            .build();
                    ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
                });
            }


        /*for(Map.Entry<URI, Set<URI>> entry : connectedSocketsMapSet.entrySet()) {
            URI senderSocket = entry.getKey();
            Set<URI> targetSocketsSet = entry.getValue();
            for(URI targetSocket : targetSocketsSet) {

                // Atom that was created
                ICBAtomModelWrapper newAtom = new ICBAtomModelWrapper(atomCreatedEvent.getAtomData());

                // find TranslateBot
                try {
                    if (!translateBotFound && newAtom.getContentPropertyStringValue(SCHEMA.NAME).equals(translateBot)) {
                        logger.info("Bot found " + newAtom.getSomeName());
                        translateBotSocketURI = new URI(getSocketURIStringOfAtom(newAtom,
                                ctx.getLinkedDataSource(),
                                URI.create(icbChatSocketURI)));
                        logger.info("Bot Chat Socket URI: " + translateBotSocketURI);
                        botContextWrapper.addConnectedSocket(URI.create(icbChatSocketURI), translateBotSocketURI);
                        translateBotFound = true;

                        ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                                URI.create(botContextWrapper.getBotChatSocketURI()),
                                translateBotSocketURI,
                                "Connection");
                        ctx.getEventBus().publish(connectCommandEvent);
                    }
                } catch (Exception e) {
                    logger.error("Connecting to Bot failed" + e.toString());
                }

                // filter ICB tag
                boolean passed = false;
                Collection<String> tags = newAtom.getAllTags();

                for (String t : tags) {
                    if(t.equals("#ICB")){
                        passed = true;
                        logger.info("New Atom with Tag for ICB found");
                    }
                }

                if(passed) {
                    String chatSocketURI = null;
                    Collection<String> socketURIS = newAtom.getSocketUris();
                    String chatSocketTypeURI = WXCHAT.ChatSocket.getURI();
                    for (String s: socketURIS) {
                        if(newAtom.getSocketType(s).isPresent() && newAtom.getSocketType(s).get().equals(chatSocketTypeURI)){
                            chatSocketURI = s;
                        }
                    }

                    if(chatSocketURI == null){
                        logger.error("Chat Socket URI is NULL");
                    }

                    if(addChatPartner(ctx, botContextWrapper, newAtom.getAtomUri(), chatSocketURI, newAtom.getLocationCoordinate(), newAtom.getSeeksLocationCoordinate())){
                        logger.info("New Chat Partner successfully added");
                    } else {
                        logger.error("Error while adding new Chat Partner");
                    }

                    // try to match chat partners
                    logger.info("Trying to match chat partners");
                    int conID = botContextWrapper.matchChatPartners();
                    if(conID != -1){

                    }

                    logger.info("TODO: Send MSG(" + senderSocket + "->" + targetSocket + ") that we registered that an ICB Atom was created, atomUri is: " + atomCreatedEvent.getAtomURI());
                    WonMessage wonMessage = WonMessageBuilder
                            .connectionMessage()
                            .sockets()
                            .sender(senderSocket)
                            .recipient(targetSocket)
                            .content()
                            .text("We registered that an ICB Atom was created, atomUri is: " + atomCreatedEvent.getAtomURI())
                            .build();
                    ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);
                }
            }*/
        }
    }

    private boolean addChatPartner(EventListenerContext ectx, InternationalChatBotContextWrapper bctx, String atomURI, String chatSocketURI, Coordinate ownCoord, Coordinate reqCoord) {
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

        String message = "Hello, we have seen that you are interested in chatting with somebody from " + reqCC.toUpperCase() + "! We " +
                "are currently waiting for a partner to match with you...";

        ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                URI.create(bctx.getBotChatSocketURI()),
                URI.create(toAdd.getChatSocketURI()),
                message);
        ectx.getEventBus().publish(connectCommandEvent);

        return bctx.addChatPartner(toAdd);

    }

    private String getSocketURIStringOfAtom(DefaultAtomModelWrapper defaultAtomModelWrapper, LinkedDataSource linkedDataSource, URI myAtomSocketURI) throws URISyntaxException {
        Collection<String> socketUris = defaultAtomModelWrapper.getSocketUris();
        String uri = "";
        for (String uris : socketUris) {
            if (WonLinkedDataUtils.isCompatibleSockets(linkedDataSource, new URI(uris), myAtomSocketURI)) {
                uri = uris;
            }
        }
        return uri;
    }
}