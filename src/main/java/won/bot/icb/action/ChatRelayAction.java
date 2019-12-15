package won.bot.icb.action;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connectionmessage.ConnectionMessageCommandEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.icb.api.InternationalChatBotAPI;
import won.bot.icb.context.InternationalChatBotContextWrapper;
import won.bot.icb.utils.ChatPartner;
import won.protocol.message.WonMessage;
import won.protocol.model.Connection;
import won.protocol.util.WonRdfUtils;

import java.lang.invoke.MethodHandles;
import java.net.URI;

public class ChatRelayAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public ChatRelayAction(EventListenerContext ctx) {
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

            // find receiver URI
            ChatPartner sender = botContextWrapper.getChatPartner(senderAtomUri);
            String receiverAtomUri = sender.getConnectedAtomURI();

            String sourceMessage = extractTextMessageFromWonMessage(((MessageFromOtherAtomEvent) event).getWonMessage());
            String translateJSON; // = TranslatorAPI.handleRequest(sourceMessage);

            if (receiverAtomUri == null){
                try {
                    getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(senderCon, "Still looking for Chat Partner..."));
                } catch (Exception te) {
                    logger.error(te.getMessage());
                }
            } else {
                Connection receiverCon = sender.getConnection();
                try {
                    getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(receiverCon, sourceMessage));
                } catch (Exception te) {
                    logger.error(te.getMessage());
                }
            }
        }
    }
}