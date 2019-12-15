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

public class TranslationAction extends BaseEventBotAction {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public TranslationAction(EventListenerContext ctx) {
        super(ctx);
    }

    @Override
    protected void doRun(Event event, EventListener executingListener) throws Exception {
        logger.info("TranslationEvent received");
        EventListenerContext ctx = getEventListenerContext();
        if (event instanceof MessageFromOtherAtomEvent
                && ctx.getBotContextWrapper() instanceof InternationalChatBotContextWrapper) {
            InternationalChatBotContextWrapper botContextWrapper = (InternationalChatBotContextWrapper) ctx.getBotContextWrapper();
            Connection senderCon = ((MessageFromOtherAtomEvent) event).getCon();
            String senderAtomUri = senderCon.getTargetAtomURI().toString();

            // find receiver URI
            ChatPartner sender = botContextWrapper.getChatPartner(senderAtomUri);
            String receiverAtomUri = sender.getConnectedAtomURI();

            String translateJSON; // = TranslatorAPI.handleRequest(sourceMessage);

            try {
                getEventListenerContext().getEventBus().publish(new ConnectionMessageCommandEvent(senderCon, "Still looking for Chat Partner..."));
            } catch (Exception te) {
                logger.error(te.getMessage());
            }

        }
    }
}
