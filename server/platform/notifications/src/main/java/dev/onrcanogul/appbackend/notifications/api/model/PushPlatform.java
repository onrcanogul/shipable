package dev.onrcanogul.appbackend.notifications.api.model;

/** Which push service a device token belongs to. */
public enum PushPlatform {
    /** Apple Push Notification service. */
    APNS,
    /** Firebase Cloud Messaging. */
    FCM
}
