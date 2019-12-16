package won.bot.icb.impl;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import won.bot.framework.bot.base.EventBot;
import won.bot.framework.eventbot.EventListenerContext;
import won.bot.framework.eventbot.action.BaseEventBotAction;
import won.bot.framework.eventbot.behaviour.ExecuteWonMessageCommandBehaviour;
import won.bot.framework.eventbot.bus.EventBus;
import won.bot.framework.eventbot.event.Event;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandResultEvent;
import won.bot.framework.eventbot.event.impl.command.connect.ConnectCommandSuccessEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.CloseFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.ConnectFromOtherAtomEvent;
import won.bot.framework.eventbot.event.impl.wonmessage.MessageFromOtherAtomEvent;
import won.bot.framework.eventbot.filter.impl.AtomUriInNamedListFilter;
import won.bot.framework.eventbot.filter.impl.CommandResultFilter;
import won.bot.framework.eventbot.filter.impl.NotFilter;
import won.bot.framework.eventbot.listener.EventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnEventListener;
import won.bot.framework.eventbot.listener.impl.ActionOnFirstEventListener;
import won.bot.framework.extensions.matcher.MatcherBehaviour;
import won.bot.framework.extensions.matcher.MatcherExtension;
import won.bot.framework.extensions.matcher.MatcherExtensionAtomCreatedEvent;
import won.bot.framework.extensions.serviceatom.ServiceAtomBehaviour;
import won.bot.framework.extensions.serviceatom.ServiceAtomExtension;
import won.bot.icb.action.IncomingMessageAction;
import won.bot.icb.action.MatcherExtensionAtomCreatedAction;
import won.bot.icb.context.InternationalChatBotContextWrapper;
import won.bot.icb.utils.ChatClient;
import won.protocol.model.Connection;
import won.protocol.util.linkeddata.WonLinkedDataUtils;

public class InternationalChatBot extends EventBot implements MatcherExtension, ServiceAtomExtension {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int registrationMatcherRetryInterval;
    private MatcherBehaviour matcherBehaviour;
    private ServiceAtomBehaviour serviceAtomBehaviour;

    // bean setter, used by spring
    public void setRegistrationMatcherRetryInterval(final int registrationMatcherRetryInterval) {
        this.registrationMatcherRetryInterval = registrationMatcherRetryInterval;
    }

    @Override
    public ServiceAtomBehaviour getServiceAtomBehaviour() {
        return serviceAtomBehaviour;
    }

    @Override
    public MatcherBehaviour getMatcherBehaviour() {
        return matcherBehaviour;
    }

    @Override
    protected void initializeEventListeners() {
        EventListenerContext ctx = getEventListenerContext();
        if (!(getBotContextWrapper() instanceof InternationalChatBotContextWrapper)) {
            logger.error(getBotContextWrapper().getBotName() + " does not work without a InternationalChatBotContextWrapper");
            throw new IllegalStateException(
                    getBotContextWrapper().getBotName() + " does not work without a InternationalChatBotContextWrapper");
        }
        EventBus bus = getEventBus();
        InternationalChatBotContextWrapper botContextWrapper = (InternationalChatBotContextWrapper) getBotContextWrapper();
        // register listeners for event.impl.command events used to tell the bot to send
        // messages
        ExecuteWonMessageCommandBehaviour wonMessageCommandBehaviour = new ExecuteWonMessageCommandBehaviour(ctx);
        wonMessageCommandBehaviour.activate();
        // activate ServiceAtomBehaviour
        serviceAtomBehaviour = new ServiceAtomBehaviour(ctx);
        serviceAtomBehaviour.activate();

        // set up matching extension
        // as this is an extension, it can be activated and deactivated as needed
        // if activated, a MatcherExtensionAtomCreatedEvent is sent every time a new
        // atom is created on a monitored node
        matcherBehaviour = new MatcherBehaviour(ctx, "InternationalChatBotMatchingExtension", registrationMatcherRetryInterval);
        matcherBehaviour.activate();
        // create filters to determine which atoms the bot should react to
        NotFilter noOwnAtoms = new NotFilter(
                new AtomUriInNamedListFilter(ctx, ctx.getBotContextWrapper().getAtomCreateListName()));
        // filter to prevent reacting to serviceAtom<->ownedAtom events;
        NotFilter noInternalServiceAtomEventFilter = getNoInternalServiceAtomEventFilter();

        bus.subscribe(ConnectFromOtherAtomEvent.class, noInternalServiceAtomEventFilter, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) {
                EventListenerContext ctx = getEventListenerContext();
                ConnectFromOtherAtomEvent connectFromOtherAtomEvent = (ConnectFromOtherAtomEvent) event;
                try {
                    String message = "Thank you for accepting the connection! Hold the line for a chat partner...";
                    final ConnectCommandEvent connectCommandEvent = new ConnectCommandEvent(
                            connectFromOtherAtomEvent.getRecipientSocket(),
                            connectFromOtherAtomEvent.getSenderSocket(), message);
                    ctx.getEventBus().subscribe(ConnectCommandSuccessEvent.class, new ActionOnFirstEventListener(ctx,
                            new CommandResultFilter(connectCommandEvent), new BaseEventBotAction(ctx) {
                        @Override
                        protected void doRun(Event event, EventListener executingListener) {
                            ConnectCommandResultEvent connectionMessageCommandResultEvent = (ConnectCommandResultEvent) event;
                            if (!connectionMessageCommandResultEvent.isSuccess()) {
                                logger.error("Failure when trying to open a received Request: "
                                        + connectionMessageCommandResultEvent.getMessage());
                            } else {
                                logger.info(
                                        "Add an established connection " +
                                                connectCommandEvent.getLocalSocket()
                                                + " -> "
                                                + connectCommandEvent.getTargetSocket()
                                                +
                                                " to the botcontext ");
                                botContextWrapper.addConnectedSocket(
                                        connectCommandEvent.getLocalSocket(),
                                        connectCommandEvent.getTargetSocket());

                                Optional<URI> connectionURI = WonLinkedDataUtils.getConnectionURIForSocketAndTargetSocket(
                                        connectCommandEvent.getLocalSocket(),
                                        connectCommandEvent.getTargetSocket(),
                                        ctx.getLinkedDataSource());

                                if(!connectionURI.isPresent()){
                                    logger.error("Connection not found");
                                }

                                Optional<Connection> con = WonLinkedDataUtils.getConnectionForConnectionURI(connectionURI.get(), ctx.getLinkedDataSource());
                                ChatClient client = botContextWrapper.getChatClient(connectCommandEvent.getTargetAtomURI().toString());
                                logger.info(connectCommandEvent.getTargetSocket().toString());
                                if(client!=null) {
                                    logger.info(botContextWrapper.getChatClient(connectCommandEvent.getTargetAtomURI().toString()).toString());
                                    try {
                                        client.setConnection(con.get());
                                        logger.info(con.get().toString());
                                        logger.info("Connection successfully added to ChatClient");
                                    } catch(Exception e){
                                        logger.error("Error while setting Connection: " + e.toString());
                                    }
                                } else {
                                    logger.error("Client is null");
                                }

                            }
                        }
                    }));
                    ctx.getEventBus().publish(connectCommandEvent);
                } catch (Exception te) {
                    logger.error(te.getMessage(), te);
                }
            }
        });

        // listen for the MatcherExtensionAtomCreatedEvent
        bus.subscribe(MatcherExtensionAtomCreatedEvent.class, new MatcherExtensionAtomCreatedAction(ctx));
        bus.subscribe(CloseFromOtherAtomEvent.class, new BaseEventBotAction(ctx) {
            @Override
            protected void doRun(Event event, EventListener executingListener) {
                EventListenerContext ctx = getEventListenerContext();
                CloseFromOtherAtomEvent closeFromOtherAtomEvent = (CloseFromOtherAtomEvent) event;
                URI targetSocketUri = closeFromOtherAtomEvent.getSocketURI();
                URI senderSocketUri = closeFromOtherAtomEvent.getTargetSocketURI();
                logger.info("Remove a closed connection " + senderSocketUri + " -> " + targetSocketUri
                        + " from the botcontext ");
                botContextWrapper.removeConnectedSocket(senderSocketUri, targetSocketUri);
            }
        });

        // listen for the MessageFromOtherAtomEvent
        bus.subscribe(MessageFromOtherAtomEvent.class,
                new ActionOnEventListener(ctx, "ReceivedTextMessage", new IncomingMessageAction(ctx)));


    }
}
