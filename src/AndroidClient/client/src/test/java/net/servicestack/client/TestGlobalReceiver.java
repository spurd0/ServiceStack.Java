package net.servicestack.client;

import net.servicestack.client.sse.ServerEventMessage;
import net.servicestack.client.sse.ServerEventReceiver;

import static chat.chatdtos.*;

/**
 * Created by mythz on 2/12/2017.
 */

public class TestGlobalReceiver extends ServerEventReceiver {
    public static CustomType CustomTypeReceived;
    public static CustomType NoSuchMethodReceived;
    public static String NoSuchMethodSelector;
    public static SetterType SetterTypeReceived;

    public void setterType(SetterType value) {
        SetterTypeReceived = value;
    }

    public void customType(CustomType request)
    {
        CustomTypeReceived = request;
    }

    @Override
    public void noSuchMethod(String selector, Object message)
    {
        ServerEventMessage msg = (ServerEventMessage)message;
        NoSuchMethodReceived = JsonUtils.fromJson(msg.getJson(), CustomType.class);
        NoSuchMethodSelector = selector;
    }
}
