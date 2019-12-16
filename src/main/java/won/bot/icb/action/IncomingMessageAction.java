package won.bot.icb.action;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.icb.context.InternationalChatBotContextWrapper;
import won.bot.icb.utils.ChatClient;
import won.protocol.message.WonMessage;
import won.protocol.message.builder.WonMessageBuilder;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;

public class IncomingMessageAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public IncomingMessageAction(EventListenerContext ctx) {
        super(ctx);
    }

    public static String extractTextMessageFromWonMessage(WonMessage wonMessage) {
        if (wonMessage == null)
            return null;
        return WonRdfUtils.MessageUtils.getTextMessage(wonMessage);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("MessageEvent received");
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof MessageFromOtherAtomEvent
                && ctx.getBotContextWrapper() instanceof InternationalChatBotContextWrapper) {
            InternationalChatBotContextWrapper botContextWrapper = (InternationalChatBotContextWrapper) ctx.getBotContextWrapper();
            Connection senderCon = ((MessageFromOtherAtomEvent) event).getCon();
            String senderAtomUri = senderCon.getTargetAtomURI().toString();

            String sourceMessage = extractTextMessageFromWonMessage(((MessageFromOtherAtomEvent) event).getWonMessage());

            // decide if message from translation bot or client#
            // TODO: could split this with events
            if (senderAtomUri.equals(botContextWrapper.getTranslateURI())) { // message from bot
                logger.info("Message from translator");
                JsonParser jsonParser = new JsonParser();
                JsonElement parsed = jsonParser.parse(sourceMessage);

                String atomID = parsed.getAsJsonObject().get("reqID").getAsString();
                ChatClient sender = botContextWrapper.getMatchedChatClient(atomID);
                if(sender == null){
                    logger.info("weird ");
                }
                ChatClient receiver = botContextWrapper.getChatPartner(atomID, sender.getConnectionID());





            } else { // message from user
                logger.info("Message from user");
                // find receiver URI
                ChatClient sender = botContextWrapper.getMatchedChatClient(senderAtomUri);

                // no chat partner found
                if (sender == null) {
                    logger.info("Chat not found");
                    try {
                        getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(senderCon, "Still looking for Chat Partner..."));
                    } catch (Exception te) {
                        logger.error(te.getMessage());
                    }
                } else {
                    logger.info("Chat found");
                    String jsonString = new JSONObject()
                            .put("sourceLat", sender.getSourceLat())
                            .put("sourceLon", sender.getSourceLon())
                            .put("targetLat", sender.getTargetLat())
                            .put("targetLon", sender.getTargetLon())
                            .put("text", sourceMessage)
                            .put("reqID", sender.getAtomURI()).toString();

                    // TODO: SEND MESSAGE TO translate bot

                    // TEST
                    ChatClient receiver = botContextWrapper.getChatPartner(senderAtomUri, sender.getConnectionID());

                    try {
                        //getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(receiver.getConnection(), jsonString));

                        WonMessage wonMessage = WonMessageBuilder
                                .connectionMessage()
                                .sockets()
                                .sender(URI.create(((InternationalChatBotContextWrapper) ctx.getBotContextWrapper()).getBotChatSocketURI()))
                                .recipient(URI.create(receiver.getChatSocketURI()))
                                .content()
                                .text(jsonString)
                                .build();
                        ctx.getWonMessageSender().prepareAndSendMessage(wonMessage);

                    } catch (Exception te) {
                        logger.error(te.getMessage());
                    }

                }
            }
        }
    }
}