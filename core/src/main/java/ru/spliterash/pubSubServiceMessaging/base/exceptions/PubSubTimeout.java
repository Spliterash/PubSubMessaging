package ru.spliterash.pubSubServiceMessaging.base.exceptions;

public class PubSubTimeout extends PubSubException {
    public PubSubTimeout(String domain, String path) {
        super("Timeout on request to " + domain + " on path " + path);
    }
}
