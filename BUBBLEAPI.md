*   [Android Developers](https://developer.android.com/)
*   [Develop](https://developer.android.com/develop)
*   [Core areas](https://developer.android.com/develop/core-areas)
*   [UI](https://developer.android.com/develop/ui)
*   [Views](https://developer.android.com/develop/ui/views/layout/declaring-layout)

Was this helpful?

# Use bubbles to let users participate in conversations bookmark\_borderbookmark Stay organized with collections Save and categorize content based on your preferences.

*   On this page
*   [The bubble API](#bubble-api)
    *   [Create an expanded bubble](#expanded-bubble)
    *   [Bubble content lifecycle](#content-lifecycle)
*   [When bubbles appear](#bubbles-appear)
*   [Launching activities from bubbles](#launching-activities)
*   [Best practices](#best-practices)
*   [Sample app](#sample)

Bubbles make it easier for users to see and participate in conversations.

**Figure 1.** A chat bubble.

Bubbles are built into the notification system. They float on top of other app content and follow the user wherever they go. Users can expand bubbles to reveal and interact with the app content, and they can collapse them when they're not using them.

When the device is locked, or the always-on-display is active, bubbles appear as notifications normally do.

Bubbles are an opt-out feature. When an app presents its first bubble, a permission dialog offers two choices:

*   Block all bubbles from your app. Notifications aren't blocked, but they never appear as bubbles.
*   Allow all bubbles from your app. All notifications sent with `BubbleMetaData` appear as bubbles.

## The bubble API

Bubbles are created using the notification API, so send your notification as normal. If you want your notification to display as a bubble, attach extra data to it.

The expanded view of a bubble is created from an activity that you choose. Configure the activity to display properly as a bubble. The activity must be [resizeable](/guide/topics/manifest/activity-element#resizeableActivity) and [embedded](/guide/topics/manifest/activity-element#embedded). If it lacks either of these requirements, it displays as a notification instead.

The following code demonstrates how to implement a bubble:

```
<activity
  android:name=".bubbles.BubbleActivity"
  android:theme="@style/AppTheme.NoActionBar"
  android:label="@string/title_activity_bubble"
  android:allowEmbedded="true"
  android:resizeableActivity="true"
/>
```

If your app shows multiple bubbles of the same type, like multiple chat conversations with different contacts, the activity must be able to launch multiple instances. On devices running Android 10 and lower, notifications aren't shown as bubbles unless you explicitly set [`documentLaunchMode`](/guide/topics/manifest/activity-element#dlmode) to `"always"`. Beginning with Android 11, you don't need to explicitly set this value, as the system automatically sets all conversations' `documentLaunchMode` to `"always"`.

To send a bubble, follow these steps:

1.  [Create a notification](/training/notify-user/build-notification) as you normally do.
2.  Call [`BubbleMetadata.Builder(PendingIntent, Icon)`](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\)) or [`BubbleMetadata.Builder(String)`](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\)) to create a `BubbleMetadata` object.
3.  Use [`setBubbleMetadata()`](/reference/android/app/Notification.Builder#setBubbleMetadata\(android.app.Notification.BubbleMetadata\)) to add the metadata to the notification.
4.  If targeting Android 11 or higher, make sure the bubble metadata or notification references a sharing shortcut.
5.  Modify your app to **not** cancel notifications that appear as bubbles. To check if the notification activity is launched as a bubble, call [`Activity#isLaunchedFromBubble()`](/reference/android/app/Activity#isLaunchedFromBubble\(\)). Canceling a notification removes the bubble from the screen. Opening a bubble automatically hides the notification associated with it.

These steps are shown in the following example:

[Kotlin](#kotlin)[Java](#java) More

// Create a bubble intent.
val target \= Intent(context, BubbleActivity::class.java)
val bubbleIntent \= PendingIntent.getActivity(context, 0, target, 0 /\* flags \*/)
val category \= "com.example.category.IMG\_SHARE\_TARGET"

val chatPartner \= Person.Builder()
    .setName("Chat partner")
    .setImportant(true)
    .build()

// Create a sharing shortcut.
val shortcutId \= generateShortcutId()
val shortcut \=
   ShortcutInfo.Builder(mContext, shortcutId)
       .setCategories(setOf(category))
       .setIntent(Intent(Intent.ACTION\_DEFAULT))
       .setLongLived(true)
       .setShortLabel(chatPartner.name)
       .build()

// Create a bubble metadata.
val bubbleData \= Notification.BubbleMetadata.Builder(bubbleIntent,
            Icon.createWithResource(context, R.drawable.icon))
    .setDesiredHeight(600)
    .build()

// Create a notification, referencing the sharing shortcut.
val builder \= Notification.Builder(context, CHANNEL\_ID)
    .setContentIntent(contentIntent)
    .setSmallIcon(smallIcon)
    .setBubbleMetadata(bubbleData)
    .setShortcutId(shortcutId)
    .addPerson(chatPartner)

// Create a bubble intent.
Intent target \= new Intent(mContext, BubbleActivity.class);
PendingIntent bubbleIntent \=
    PendingIntent.getActivity(mContext, 0, target, 0 /\* flags \*/);

private val CATEGORY\_TEXT\_SHARE\_TARGET \=
    "com.example.category.IMG\_SHARE\_TARGET"

Person chatPartner \= new Person.Builder()
        .setName("Chat partner")
        .setImportant(true)
        .build();

// Create a sharing shortcut.
private String shortcutId \= generateShortcutId();
ShortcutInfo shortcut \=
   new ShortcutInfo.Builder(mContext, shortcutId)
       .setCategories(Collections.singleton(CATEGORY\_TEXT\_SHARE\_TARGET))
       .setIntent(Intent(Intent.ACTION\_DEFAULT))
       .setLongLived(true)
       .setShortLabel(chatPartner.getName())
       .build();

// Create a bubble metadata.
Notification.BubbleMetadata bubbleData \=
    new Notification.BubbleMetadata.Builder(bubbleIntent,
            Icon.createWithResource(context, R.drawable.icon))
        .setDesiredHeight(600)
        .build();

// Create a notification, referencing the sharing shortcut.
Notification.Builder builder \=
    new Notification.Builder(mContext, CHANNEL\_ID)
        .setContentIntent(contentIntent)
        .setSmallIcon(smallIcon)
        .setBubbleMetadata(bubbleData)
        .setShortcutId(shortcutId)
        .addPerson(chatPartner);

**Note:** The first time you send the notification to display a bubble, make sure it's in a notification channel with [`IMPORTANCE_MIN`](/reference/android/app/NotificationManager#IMPORTANCE_MIN) or higher.

If your app is in the foreground when a bubble is sent, importance is ignored and your bubble is always shown, unless the user blocks bubbles or notifications from your app.

### Create an expanded bubble

You can configure your bubble to present it in expanded state automatically. We recommend only using this feature if the user performs an action that results in a bubble, like tapping a button to start a new chat. In this case, it also makes sense to suppress the initial notification sent when a bubble is created.

There are methods you can use to set flags that enable these behaviors: [`setAutoExpandBubble()`](/reference/android/app/Notification.BubbleMetadata.Builder#setAutoExpandBubble\(boolean\)) and [`setSuppressNotification()`](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressNotification\(boolean\)).

The following example shows how to configure a bubble to automatically present in an expanded state:

[Kotlin](#kotlin)[Java](#java) More

val bubbleMetadata \= Notification.BubbleMetadata.Builder()
    .setDesiredHeight(600)
    .setIntent(bubbleIntent)
    .setAutoExpandBubble(true)
    .setSuppressNotification(true)
    .build()

Notification.BubbleMetadata bubbleData \=
    new Notification.BubbleMetadata.Builder()
        .setDesiredHeight(600)
        .setIntent(bubbleIntent)
        .setAutoExpandBubble(true)
        .setSuppressNotification(true)
        .build();

### Bubble content lifecycle

When a bubble is expanded, the content activity goes through the normal [process lifecycle](/guide/components/activities/process-lifecycle), resulting in the application becoming a foreground process, if it isn't already.

When the bubble is collapsed or dismissed, the activity is destroyed. This might result in the process being cached and later killed, depending on whether the app has other foreground components running.

## When bubbles appear

To reduce interruptions for the user, bubbles only appear under certain circumstances.

If an app targets Android 11 or higher, a notification doesn't appear as a bubble unless it meets the [conversation requirements](/guide/topics/ui/conversations). If an app targets Android 10 or lower, the notification appears as a bubble only if one or more of the following conditions are met:

*   The notification uses [`MessagingStyle`](/reference/android/app/Notification.MessagingStyle) and has a [`Person`](/reference/android/app/Person) added.
*   The notification is from a call to [`Service.startForeground`](/reference/android/app/Service#startForeground\(int,%20android.app.Notification\)), has a [`category`](/reference/android/app/Notification.Builder#setCategory\(java.lang.String\)) of [`CATEGORY_CALL`](/reference/android/app/Notification#CATEGORY_CALL), and has a `Person` added.
*   The app is in the foreground when the notification is sent.

If none of these conditions are met, the notification is shown instead of a bubble.

**Note:** Android 10 doesn't support Bubbles out-of-the-box. To see them appearing, you will need to enable [Developer Options](/studio/debug/dev-options), search for "bubbles" in the Settings menu, and enable the Bubbles settings.

## Launching activities from bubbles

When a bubble launches a new activity, the new activity will either launch within the same task and the same bubbled window, or in a new task in fullscreen, collapsing the bubble that launched it.

To launch a new activity in the same task as the bubble: 1. Use the activity context when launching intents, `activity.startActivity(intent)`, and 1. Don't set the `FLAG_ACTIVITY_NEW_TASK` flag on the intent.

Otherwise, the new activity is started in a new task and the bubble is collapsed.

Keep in mind that a bubble represents a specific conversation, so activities launched within the bubble should be related to that conversation. Additionally, launching an activity within the bubble increases the task stack of the bubble and could potentially complicate the user experience, specifically around navigation.

## Best practices

*   Send a notification as a bubble only if it is important, such as when it is part of an ongoing communication or if the user explicitly requests a bubble for content. Bubbles use screen real estate and cover other app content.
*   Make sure your bubble notification also works as a normal notification. When the user disables the bubble, a bubble notification is shown as a normal notification.
*   Call `super.onBackPressed` when overriding [`onBackPressed`](/reference/android/app/Activity#onBackPressed\(\)) in the bubble activity. Otherwise, your bubble might not behave correctly.

When a collapsed bubble receives an updated message, the bubble shows a badge icon to indicate an unread message. When the user opens the message in the associated app, follow these steps:

*   [Update](https://developer.android.com/training/notify-user/build-notification#Updating) the `BubbleMetadata` to suppress the notification. Call [`BubbleMetadata.Builder.setSuppressNotification()`](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressNotification\(boolean\)). This removes the badge icon to indicate that the user interacted with the message.
*   Set [`Notification.Builder.setOnlyAlertOnce()`](/reference/android/app/Notification.Builder#setOnlyAlertOnce\(boolean\)) to `true` to suppress the sound or vibration that accompanies the `BubbleMetadata` update.

## Sample app

The [SociaLite](https://github.com/android/socialite) sample app is a conversation app that uses bubbles. For demonstration purposes, this app uses chatbots. In real-world applications, use bubbles for messages by humans.

# Notification.BubbleMetadata bookmark\_borderbookmark Stay organized with collections Save and categorize content based on your preferences.

* * *

[Kotlin](/reference/kotlin/android/app/Notification.BubbleMetadata) |Java

`public static final class Notification.BubbleMetadata`  
`extends [Object](/reference/java/lang/Object)` `implements [Parcelable](/reference/android/os/Parcelable)`

[java.lang.Object](/reference/java/lang/Object)

   ↳

android.app.Notification.BubbleMetadata

  

* * *

Encapsulates the information needed to display a notification as a bubble.

A bubble is used to display app content in a floating window over the existing foreground activity. A bubble has a collapsed state represented by an icon and an expanded state that displays an activity. These may be defined via `[Builder.Builder(PendingIntent, Icon)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\))` or they may be defined via an existing shortcut using `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))`.

**Notifications with a valid and allowed bubble will display in collapsed state outside of the notification shade on unlocked devices. When a user interacts with the collapsed bubble, the bubble activity will be invoked and displayed.**

**See also:**

*   `[Notification.Builder.setBubbleMetadata(BubbleMetadata)](/reference/android/app/Notification.Builder#setBubbleMetadata\(android.app.Notification.BubbleMetadata\))`

## Summary

### Nested classes

`class`

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

Builder to construct a `[BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)` object. 

### Inherited constants

From interface `[android.os.Parcelable](/reference/android/os/Parcelable)`

`int`

`[CONTENTS_FILE_DESCRIPTOR](/reference/android/os/Parcelable#CONTENTS_FILE_DESCRIPTOR)`

Descriptor bit used with `[describeContents()](/reference/android/os/Parcelable#describeContents\(\))`: indicates that the Parcelable object's flattened representation includes a file descriptor.

`int`

`[PARCELABLE_WRITE_RETURN_VALUE](/reference/android/os/Parcelable#PARCELABLE_WRITE_RETURN_VALUE)`

Flag for use with `[writeToParcel(Parcel, int)](/reference/android/os/Parcelable#writeToParcel\(android.os.Parcel,%20int\))`: the object being written is a return value, that is the result of a function such as "`Parcelable someFunction()`", "`void someFunction(out Parcelable)`", or "`void someFunction(inout Parcelable)`".

### Fields

`public static final [Creator](/reference/android/os/Parcelable.Creator)<[Notification.BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)>`

`[CREATOR](/reference/android/app/Notification.BubbleMetadata#CREATOR)`

### Public methods

`int`

`[describeContents](/reference/android/app/Notification.BubbleMetadata#describeContents\(\))()`

Describe the kinds of special objects contained in this Parcelable instance's marshaled representation.

`boolean`

`[getAutoExpandBubble](/reference/android/app/Notification.BubbleMetadata#getAutoExpandBubble\(\))()`

`[PendingIntent](/reference/android/app/PendingIntent)`

`[getDeleteIntent](/reference/android/app/Notification.BubbleMetadata#getDeleteIntent\(\))()`

`int`

`[getDesiredHeight](/reference/android/app/Notification.BubbleMetadata#getDesiredHeight\(\))()`

`int`

`[getDesiredHeightResId](/reference/android/app/Notification.BubbleMetadata#getDesiredHeightResId\(\))()`

`[Icon](/reference/android/graphics/drawable/Icon)`

`[getIcon](/reference/android/app/Notification.BubbleMetadata#getIcon\(\))()`

`[PendingIntent](/reference/android/app/PendingIntent)`

`[getIntent](/reference/android/app/Notification.BubbleMetadata#getIntent\(\))()`

`[String](/reference/java/lang/String)`

`[getShortcutId](/reference/android/app/Notification.BubbleMetadata#getShortcutId\(\))()`

`boolean`

`[isBubbleSuppressable](/reference/android/app/Notification.BubbleMetadata#isBubbleSuppressable\(\))()`

Indicates whether the bubble should be visually suppressed from the bubble stack if the user is viewing the same content outside of the bubble.

`boolean`

`[isBubbleSuppressed](/reference/android/app/Notification.BubbleMetadata#isBubbleSuppressed\(\))()`

Indicates whether the bubble is currently visually suppressed from the bubble stack.

`boolean`

`[isNotificationSuppressed](/reference/android/app/Notification.BubbleMetadata#isNotificationSuppressed\(\))()`

Indicates whether the notification associated with the bubble is being visually suppressed from the notification shade.

`void`

`[writeToParcel](/reference/android/app/Notification.BubbleMetadata#writeToParcel\(android.os.Parcel,%20int\))([Parcel](/reference/android/os/Parcel) out, int flags)`

Flatten this object in to a Parcel.

### Inherited methods

From class `[java.lang.Object](/reference/java/lang/Object)`

`[Object](/reference/java/lang/Object)`

`[clone](/reference/java/lang/Object#clone\(\))()`

Creates and returns a copy of this object.

`boolean`

`[equals](/reference/java/lang/Object#equals\(java.lang.Object\))([Object](/reference/java/lang/Object) obj)`

Indicates whether some other object is "equal to" this one.

`void`

`[finalize](/reference/java/lang/Object#finalize\(\))()`

Called by the garbage collector on an object when garbage collection determines that there are no more references to the object.

`final [Class](/reference/java/lang/Class)<?>`

`[getClass](/reference/java/lang/Object#getClass\(\))()`

Returns the runtime class of this `Object`.

`int`

`[hashCode](/reference/java/lang/Object#hashCode\(\))()`

Returns a hash code value for the object.

`final void`

`[notify](/reference/java/lang/Object#notify\(\))()`

Wakes up a single thread that is waiting on this object's monitor.

`final void`

`[notifyAll](/reference/java/lang/Object#notifyAll\(\))()`

Wakes up all threads that are waiting on this object's monitor.

`[String](/reference/java/lang/String)`

`[toString](/reference/java/lang/Object#toString\(\))()`

Returns a string representation of the object.

`final void`

`[wait](/reference/java/lang/Object#wait\(long,%20int\))(long timeoutMillis, int nanos)`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_, or until a certain amount of real time has elapsed.

`final void`

`[wait](/reference/java/lang/Object#wait\(long\))(long timeoutMillis)`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_, or until a certain amount of real time has elapsed.

`final void`

`[wait](/reference/java/lang/Object#wait\(\))()`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_.

From interface `[android.os.Parcelable](/reference/android/os/Parcelable)`

`abstract int`

`[describeContents](/reference/android/os/Parcelable#describeContents\(\))()`

Describe the kinds of special objects contained in this Parcelable instance's marshaled representation.

`abstract void`

`[writeToParcel](/reference/android/os/Parcelable#writeToParcel\(android.os.Parcel,%20int\))([Parcel](/reference/android/os/Parcel) dest, int flags)`

Flatten this object in to a Parcel.

## Fields

### CREATOR

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final [Creator](/reference/android/os/Parcelable.Creator)<[Notification.BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)\> CREATOR

## Public methods

### describeContents

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int describeContents ()

Describe the kinds of special objects contained in this Parcelable instance's marshaled representation. For example, if the object will include a file descriptor in the output of `[writeToParcel(android.os.Parcel, int)](/reference/android/os/Parcelable#writeToParcel\(android.os.Parcel,%20int\))`, the return value of this method must include the `[CONTENTS_FILE_DESCRIPTOR](/reference/android/os/Parcelable#CONTENTS_FILE_DESCRIPTOR)` bit.

Returns

`int`

a bitmask indicating the set of special object types marshaled by this Parcelable object instance. Value is either `0` or `[CONTENTS_FILE_DESCRIPTOR](/reference/android/os/Parcelable#CONTENTS_FILE_DESCRIPTOR)`

### getAutoExpandBubble

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean getAutoExpandBubble ()

Returns

`boolean`

whether this bubble should auto expand when it is posted

**See also:**

*   `[Notification.BubbleMetadata.Builder.setAutoExpandBubble(boolean)](/reference/android/app/Notification.BubbleMetadata.Builder#setAutoExpandBubble\(boolean\))`

### getDeleteIntent

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [PendingIntent](/reference/android/app/PendingIntent) getDeleteIntent ()

Returns

`[PendingIntent](/reference/android/app/PendingIntent)`

the pending intent to send when the bubble is dismissed by a user, if one exists This value may be `null`.

### getDesiredHeight

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getDesiredHeight ()

Returns

`int`

the ideal height, in DPs, for the floating window that app content defined by `[getIntent()](/reference/android/app/Notification.BubbleMetadata#getIntent\(\))` for this bubble. A value of 0 indicates a desired height has not been set.

### getDesiredHeightResId

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getDesiredHeightResId ()

Returns

`int`

the resId of ideal height for the floating window that app content defined by `[getIntent()](/reference/android/app/Notification.BubbleMetadata#getIntent\(\))` for this bubble. A value of 0 indicates a res value has not been provided for the desired height.

### getIcon

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Icon](/reference/android/graphics/drawable/Icon) getIcon ()

Returns

`[Icon](/reference/android/graphics/drawable/Icon)`

the icon that will be displayed for this bubble when it is collapsed, or `null` if the bubble is created via `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))`.

### getIntent

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [PendingIntent](/reference/android/app/PendingIntent) getIntent ()

Returns

`[PendingIntent](/reference/android/app/PendingIntent)`

the pending intent used to populate the floating window for this bubble, or null if this bubble is created via `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))`.

### getShortcutId

Added in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [String](/reference/java/lang/String) getShortcutId ()

Returns

`[String](/reference/java/lang/String)`

the shortcut id used for this bubble if created via `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))` or null if created via `[Builder.Builder(PendingIntent, Icon)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\))`.

### isBubbleSuppressable

Added in [API level 31](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isBubbleSuppressable ()

Indicates whether the bubble should be visually suppressed from the bubble stack if the user is viewing the same content outside of the bubble. For example, the user has a bubble with Alice and then opens up the main app and navigates to Alice's page.

To match the activity and the bubble notification, the bubble notification should have a `[LocusId](/reference/android/content/LocusId)` set that matches a locus id set on the activity.

Returns

`boolean`

whether this bubble should be suppressed when the same content is visible outside of the bubble.

**See also:**

*   `[Notification.BubbleMetadata.Builder.setSuppressableBubble(boolean)](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressableBubble\(boolean\))`

### isBubbleSuppressed

Added in [API level 31](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isBubbleSuppressed ()

Indicates whether the bubble is currently visually suppressed from the bubble stack.

Returns

`boolean`

**See also:**

*   `[Notification.BubbleMetadata.Builder.setSuppressableBubble(boolean)](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressableBubble\(boolean\))`

### isNotificationSuppressed

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isNotificationSuppressed ()

Indicates whether the notification associated with the bubble is being visually suppressed from the notification shade. When `true` the notification is hidden, when `false` the notification shows as normal.

Apps sending bubbles may set this flag so that the bubble is posted **without** the associated notification in the notification shade.

Generally the app should only set this flag if the user has performed an action to request or create a bubble, or if the user has seen the content in the notification and the notification is no longer relevant.

The system will update this flag with `true` to hide the notification from the user once the bubble has been expanded.

Returns

`boolean`

whether this bubble should suppress the notification when it is posted

**See also:**

*   `[Notification.BubbleMetadata.Builder.setSuppressNotification(boolean)](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressNotification\(boolean\))`

### writeToParcel

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void writeToParcel ([Parcel](/reference/android/os/Parcel) out, 
                int flags)

Flatten this object in to a Parcel.

Parameters

`out`

`Parcel`: The Parcel in which the object should be written. This value cannot be `null`.

`flags`

`int`: Additional flags about how the object should be written. May be 0 or `[Parcelable.PARCELABLE_WRITE_RETURN_VALUE](/reference/android/os/Parcelable#PARCELABLE_WRITE_RETURN_VALUE)`. Value is either `0` or a combination of `[Parcelable.PARCELABLE_WRITE_RETURN_VALUE](/reference/android/os/Parcelable#PARCELABLE_WRITE_RETURN_VALUE)`, and android.os.Parcelable.PARCELABLE\_ELIDE\_DUPLICATES

# Notification.BubbleMetadata.Builder bookmark\_borderbookmark Stay organized with collections Save and categorize content based on your preferences.

* * *

[Kotlin](/reference/kotlin/android/app/Notification.BubbleMetadata.Builder) |Java

`public static final class Notification.BubbleMetadata.Builder`  
`extends [Object](/reference/java/lang/Object)`

[java.lang.Object](/reference/java/lang/Object)

   ↳

android.app.Notification.BubbleMetadata.Builder

  

* * *

Builder to construct a `[BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)` object.

## Summary

### Public constructors

`[Builder](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(\))()`

_This constructor is deprecated. use `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))` for a bubble created via a `[ShortcutInfo](/reference/android/content/pm/ShortcutInfo)` or `[Builder.Builder(PendingIntent, Icon)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\))` for a bubble created via a `[PendingIntent](/reference/android/app/PendingIntent)`._

`[Builder](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\))([PendingIntent](/reference/android/app/PendingIntent) intent, [Icon](/reference/android/graphics/drawable/Icon) icon)`

Creates a `[BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)` based on the provided intent and icon.

`[Builder](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))([String](/reference/java/lang/String) shortcutId)`

Creates a `[BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)` based on a `[ShortcutInfo](/reference/android/content/pm/ShortcutInfo)`.

### Public methods

`[Notification.BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)`

`[build](/reference/android/app/Notification.BubbleMetadata.Builder#build\(\))()`

Creates the `[BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)` defined by this builder.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setAutoExpandBubble](/reference/android/app/Notification.BubbleMetadata.Builder#setAutoExpandBubble\(boolean\))(boolean shouldExpand)`

Sets whether the bubble will be posted in its expanded state.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setDeleteIntent](/reference/android/app/Notification.BubbleMetadata.Builder#setDeleteIntent\(android.app.PendingIntent\))([PendingIntent](/reference/android/app/PendingIntent) deleteIntent)`

Sets an intent to send when this bubble is explicitly removed by the user.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setDesiredHeight](/reference/android/app/Notification.BubbleMetadata.Builder#setDesiredHeight\(int\))(int height)`

Sets the desired height in DPs for the expanded content of the bubble.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setDesiredHeightResId](/reference/android/app/Notification.BubbleMetadata.Builder#setDesiredHeightResId\(int\))(int heightResId)`

Sets the desired height via resId for the expanded content of the bubble.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setIcon](/reference/android/app/Notification.BubbleMetadata.Builder#setIcon\(android.graphics.drawable.Icon\))([Icon](/reference/android/graphics/drawable/Icon) icon)`

Sets the icon for the bubble.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setIntent](/reference/android/app/Notification.BubbleMetadata.Builder#setIntent\(android.app.PendingIntent\))([PendingIntent](/reference/android/app/PendingIntent) intent)`

Sets the intent for the bubble.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setSuppressNotification](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressNotification\(boolean\))(boolean shouldSuppressNotif)`

Sets whether the bubble will be posted **without** the associated notification in the notification shade.

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

`[setSuppressableBubble](/reference/android/app/Notification.BubbleMetadata.Builder#setSuppressableBubble\(boolean\))(boolean suppressBubble)`

Indicates whether the bubble should be visually suppressed from the bubble stack if the user is viewing the same content outside of the bubble.

### Inherited methods

From class `[java.lang.Object](/reference/java/lang/Object)`

`[Object](/reference/java/lang/Object)`

`[clone](/reference/java/lang/Object#clone\(\))()`

Creates and returns a copy of this object.

`boolean`

`[equals](/reference/java/lang/Object#equals\(java.lang.Object\))([Object](/reference/java/lang/Object) obj)`

Indicates whether some other object is "equal to" this one.

`void`

`[finalize](/reference/java/lang/Object#finalize\(\))()`

Called by the garbage collector on an object when garbage collection determines that there are no more references to the object.

`final [Class](/reference/java/lang/Class)<?>`

`[getClass](/reference/java/lang/Object#getClass\(\))()`

Returns the runtime class of this `Object`.

`int`

`[hashCode](/reference/java/lang/Object#hashCode\(\))()`

Returns a hash code value for the object.

`final void`

`[notify](/reference/java/lang/Object#notify\(\))()`

Wakes up a single thread that is waiting on this object's monitor.

`final void`

`[notifyAll](/reference/java/lang/Object#notifyAll\(\))()`

Wakes up all threads that are waiting on this object's monitor.

`[String](/reference/java/lang/String)`

`[toString](/reference/java/lang/Object#toString\(\))()`

Returns a string representation of the object.

`final void`

`[wait](/reference/java/lang/Object#wait\(long,%20int\))(long timeoutMillis, int nanos)`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_, or until a certain amount of real time has elapsed.

`final void`

`[wait](/reference/java/lang/Object#wait\(long\))(long timeoutMillis)`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_, or until a certain amount of real time has elapsed.

`final void`

`[wait](/reference/java/lang/Object#wait\(\))()`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_.

## Public constructors

### Builder

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public Builder ()

**This constructor is deprecated.**  
use `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))` for a bubble created via a `[ShortcutInfo](/reference/android/content/pm/ShortcutInfo)` or `[Builder.Builder(PendingIntent, Icon)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\))` for a bubble created via a `[PendingIntent](/reference/android/app/PendingIntent)`.

### Builder

Added in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public Builder ([PendingIntent](/reference/android/app/PendingIntent) intent, 
                [Icon](/reference/android/graphics/drawable/Icon) icon)

Creates a `[BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)` based on the provided intent and icon.

The icon will be used to represent the bubble when it is collapsed. An icon should be representative of the content within the bubble. If your app produces multiple bubbles, the icon should be unique for each of them.

The intent that will be used when the bubble is expanded. This will display the app content in a floating window over the existing foreground activity. The intent should point to a resizable activity.

When the activity is launched from a bubble, `[Activity.isLaunchedFromBubble()](/reference/android/app/Activity#isLaunchedFromBubble\(\))` will return with `true`.

Note that the pending intent used here requires PendingIntent.FLAG\_MUTABLE.

Parameters

`intent`

`PendingIntent`: This value cannot be `null`.

`icon`

`Icon`: This value cannot be `null`.

Throws

`[NullPointerException](/reference/java/lang/NullPointerException)`

if intent is null.

`[NullPointerException](/reference/java/lang/NullPointerException)`

if icon is null.

### Builder

Added in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public Builder ([String](/reference/java/lang/String) shortcutId)

Creates a `[BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)` based on a `[ShortcutInfo](/reference/android/content/pm/ShortcutInfo)`. To create a shortcut bubble, ensure that the shortcut associated with the provided is published as a dynamic shortcut that was built with `[ShortcutInfo.Builder.setLongLived(boolean)](/reference/android/content/pm/ShortcutInfo.Builder#setLongLived\(boolean\))` being true, otherwise your notification will not be able to bubble.

The shortcut icon will be used to represent the bubble when it is collapsed.

The shortcut activity will be used when the bubble is expanded. This will display the shortcut activity in a floating window over the existing foreground activity.

When the activity is launched from a bubble, `[Activity.isLaunchedFromBubble()](/reference/android/app/Activity#isLaunchedFromBubble\(\))` will return with `true`.

If the shortcut has not been published when the bubble notification is sent, no bubble will be produced. If the shortcut is deleted while the bubble is active, the bubble will be removed.

Throws

`[NullPointerException](/reference/java/lang/NullPointerException)`

if shortcutId is null.

**See also:**

*   `[ShortcutInfo](/reference/android/content/pm/ShortcutInfo)`
*   `[ShortcutInfo.Builder.setLongLived(boolean)](/reference/android/content/pm/ShortcutInfo.Builder#setLongLived\(boolean\))`
*   `[ShortcutManager.addDynamicShortcuts(List)](/reference/android/content/pm/ShortcutManager#addDynamicShortcuts\(java.util.List<android.content.pm.ShortcutInfo>\))`

## Public methods

### build

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata](/reference/android/app/Notification.BubbleMetadata) build ()

Creates the `[BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)` defined by this builder.

Returns

`[Notification.BubbleMetadata](/reference/android/app/Notification.BubbleMetadata)`

This value cannot be `null`.

Throws

`[NullPointerException](/reference/java/lang/NullPointerException)`

if required elements have not been set.

### setAutoExpandBubble

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setAutoExpandBubble (boolean shouldExpand)

Sets whether the bubble will be posted in its expanded state.

This flag has no effect if the app posting the bubble is not in the foreground. The app is considered foreground if it is visible and on the screen, note that a foreground service does not qualify.

Generally, this flag should only be set if the user has performed an action to request or create a bubble.

Setting this flag is optional; it defaults to `false`.

Parameters

`shouldExpand`

`boolean`

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

### setDeleteIntent

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setDeleteIntent ([PendingIntent](/reference/android/app/PendingIntent) deleteIntent)

Sets an intent to send when this bubble is explicitly removed by the user.

Setting a delete intent is optional.

Parameters

`deleteIntent`

`PendingIntent`: This value may be `null`.

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

### setDesiredHeight

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setDesiredHeight (int height)

Sets the desired height in DPs for the expanded content of the bubble.

This height may not be respected if there is not enough space on the screen or if the provided height is too small to be useful.

If `[setDesiredHeightResId(int)](/reference/android/app/Notification.BubbleMetadata.Builder#setDesiredHeightResId\(int\))` was previously called on this builder, the previous value set will be cleared after calling this method, and this value will be used instead.

A desired height (in DPs or via resID) is optional.

Parameters

`height`

`int`

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

**See also:**

*   `[setDesiredHeightResId(int)](/reference/android/app/Notification.BubbleMetadata.Builder#setDesiredHeightResId\(int\))`

### setDesiredHeightResId

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setDesiredHeightResId (int heightResId)

Sets the desired height via resId for the expanded content of the bubble.

This height may not be respected if there is not enough space on the screen or if the provided height is too small to be useful.

If `[setDesiredHeight(int)](/reference/android/app/Notification.BubbleMetadata.Builder#setDesiredHeight\(int\))` was previously called on this builder, the previous value set will be cleared after calling this method, and this value will be used instead.

A desired height (in DPs or via resID) is optional.

Parameters

`heightResId`

`int`

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

**See also:**

*   `[setDesiredHeight(int)](/reference/android/app/Notification.BubbleMetadata.Builder#setDesiredHeight\(int\))`

### setIcon

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setIcon ([Icon](/reference/android/graphics/drawable/Icon) icon)

Sets the icon for the bubble. Can only be used if the bubble was created via `[Builder.Builder(PendingIntent, Icon)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(android.app.PendingIntent,%20android.graphics.drawable.Icon\))`.

The icon will be used to represent the bubble when it is collapsed. An icon should be representative of the content within the bubble. If your app produces multiple bubbles, the icon should be unique for each of them.

It is recommended to use an `[Icon](/reference/android/graphics/drawable/Icon)` of type `[Icon.TYPE_URI](/reference/android/graphics/drawable/Icon#TYPE_URI)` or `[Icon.TYPE_URI_ADAPTIVE_BITMAP](/reference/android/graphics/drawable/Icon#TYPE_URI_ADAPTIVE_BITMAP)`

Parameters

`icon`

`Icon`: This value cannot be `null`.

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

Throws

`[NullPointerException](/reference/java/lang/NullPointerException)`

if icon is null.

`[IllegalStateException](/reference/java/lang/IllegalStateException)`

if this builder was created via `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))`.

### setIntent

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setIntent ([PendingIntent](/reference/android/app/PendingIntent) intent)

Sets the intent for the bubble.

The intent that will be used when the bubble is expanded. This will display the app content in a floating window over the existing foreground activity. The intent should point to a resizable activity.

Parameters

`intent`

`PendingIntent`: This value cannot be `null`.

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

Throws

`[NullPointerException](/reference/java/lang/NullPointerException)`

if intent is null.

`[IllegalStateException](/reference/java/lang/IllegalStateException)`

if this builder was created via `[Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))`.

### setSuppressNotification

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setSuppressNotification (boolean shouldSuppressNotif)

Sets whether the bubble will be posted **without** the associated notification in the notification shade.

Generally, this flag should only be set if the user has performed an action to request or create a bubble, or if the user has seen the content in the notification and the notification is no longer relevant.

Setting this flag is optional; it defaults to `false`.

Parameters

`shouldSuppressNotif`

`boolean`

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

### setSuppressableBubble

Added in [API level 31](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder) setSuppressableBubble (boolean suppressBubble)

Indicates whether the bubble should be visually suppressed from the bubble stack if the user is viewing the same content outside of the bubble. For example, the user has a bubble with Alice and then opens up the main app and navigates to Alice's page.

To match the activity and the bubble notification, the bubble notification should have a locus id set that matches a locus id set on the activity.

Parameters

`suppressBubble`

`boolean`

Returns

`[Notification.BubbleMetadata.Builder](/reference/android/app/Notification.BubbleMetadata.Builder)`

This value cannot be `null`.

**See also:**

*   `[Notification.Builder.setLocusId(LocusId)](/reference/android/app/Notification.Builder#setLocusId\(android.content.LocusId\))`
*   `[Activity.setLocusContext(LocusId, Bundle)](/reference/android/app/Activity#setLocusContext\(android.content.LocusId,%20android.os.Bundle\))`

  # Activity bookmark\_borderbookmark Stay organized with collections Save and categorize content based on your preferences.

* * *

[Kotlin](/reference/kotlin/android/app/Activity) |Java

`public class Activity`  
`extends [ContextThemeWrapper](/reference/android/view/ContextThemeWrapper)` `implements [ComponentCallbacks2](/reference/android/content/ComponentCallbacks2), [KeyEvent.Callback](/reference/android/view/KeyEvent.Callback), [LayoutInflater.Factory2](/reference/android/view/LayoutInflater.Factory2), [View.OnCreateContextMenuListener](/reference/android/view/View.OnCreateContextMenuListener), [Window.Callback](/reference/android/view/Window.Callback)`

[java.lang.Object](/reference/java/lang/Object)

   ↳

[android.content.Context](/reference/android/content/Context)

 

   ↳

[android.content.ContextWrapper](/reference/android/content/ContextWrapper)

 

 

   ↳

[android.view.ContextThemeWrapper](/reference/android/view/ContextThemeWrapper)

 

 

 

   ↳

android.app.Activity

Known direct subclasses

[AccountAuthenticatorActivity](/reference/android/accounts/AccountAuthenticatorActivity), [ActivityGroup](/reference/android/app/ActivityGroup), [AliasActivity](/reference/android/app/AliasActivity), [ExpandableListActivity](/reference/android/app/ExpandableListActivity), [ListActivity](/reference/android/app/ListActivity), [NativeActivity](/reference/android/app/NativeActivity)

[AccountAuthenticatorActivity](/reference/android/accounts/AccountAuthenticatorActivity)

_This class was deprecated in API level 30. Applications should extend Activity themselves. This class is not compatible with AppCompat, and the functionality it provides is not complex._ 

[ActivityGroup](/reference/android/app/ActivityGroup)

_This class was deprecated in API level 13. Use the new `[Fragment](/reference/android/app/Fragment)` and `[FragmentManager](/reference/android/app/FragmentManager)` APIs instead; these are also available on older platforms through the Android compatibility package._ 

[AliasActivity](/reference/android/app/AliasActivity)

_This class was deprecated in API level 30. Use `<activity-alias>` or subclass Activity directly._ 

[ExpandableListActivity](/reference/android/app/ExpandableListActivity)

_This class was deprecated in API level 30. Use `[RecyclerView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.html)` or use `[ExpandableListView](/reference/android/widget/ExpandableListView)` directly_ 

[ListActivity](/reference/android/app/ListActivity)

_This class was deprecated in API level 30. Use `[ListFragment](https://developer.android.com/reference/androidx/fragment/app/ListFragment.html)` or `[RecyclerView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.html)` to implement your Activity instead._ 

[NativeActivity](/reference/android/app/NativeActivity)

Convenience for implementing an activity that will be implemented purely in native code. 

Known indirect subclasses

[LauncherActivity](/reference/android/app/LauncherActivity), [PreferenceActivity](/reference/android/preference/PreferenceActivity), [TabActivity](/reference/android/app/TabActivity)

[LauncherActivity](/reference/android/app/LauncherActivity)

_This class was deprecated in API level 30. Applications can implement this UI themselves using `[RecyclerView](https://developer.android.com/reference/androidx/recyclerview/widget/RecyclerView.html)` and `[PackageManager.queryIntentActivities(Intent, int)](/reference/android/content/pm/PackageManager#queryIntentActivities\(android.content.Intent,%20int\))`_ 

[PreferenceActivity](/reference/android/preference/PreferenceActivity)

_This class was deprecated in API level 29. Use the [AndroidX](/jetpack/androidx) [Preference Library](/reference/androidx/preference/package-summary) for consistent behavior across all devices. For more information on using the AndroidX Preference Library see [Settings](/guide/topics/ui/settings)._ 

[TabActivity](/reference/android/app/TabActivity)

_This class was deprecated in API level 13. New applications should use Fragments instead of this class; to continue to run on older devices, you can use the v4 support library which provides a version of the Fragment API that is compatible down to `[Build.VERSION_CODES.DONUT](/reference/android/os/Build.VERSION_CODES#DONUT)`._ 

  

* * *

An activity is a single, focused thing that the user can do. Almost all activities interact with the user, so the Activity class takes care of creating a window for you in which you can place your UI with `[setContentView(View)](/reference/android/app/Activity#setContentView\(android.view.View\))`. While activities are often presented to the user as full-screen windows, they can also be used in other ways: as floating windows (via a theme with `[R.attr.windowIsFloating](/reference/android/R.attr#windowIsFloating)` set), [Multi-Window mode](https://developer.android.com/guide/topics/ui/multi-window) or embedded into other windows. There are two methods almost all subclasses of Activity will implement:

*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` is where you initialize your activity. Most importantly, here you will usually call `[setContentView(int)](/reference/android/app/Activity#setContentView\(int\))` with a layout resource defining your UI, and using `[findViewById(int)](/reference/android/app/Activity#findViewById\(int\))` to retrieve the widgets in that UI that you need to interact with programmatically.
*   `[onPause()](/reference/android/app/Activity#onPause\(\))` is where you deal with the user pausing active interaction with the activity. Any changes made by the user should at this point be committed (usually to the `[ContentProvider](/reference/android/content/ContentProvider)` holding the data). In this state the activity is still visible on screen.

To be of use with `[Context.startActivity()](/reference/android/content/Context#startActivity\(android.content.Intent\))`, all activity classes must have a corresponding `[<activity>](/reference/android/R.styleable#AndroidManifestActivity)` declaration in their package's `AndroidManifest.xml`.

Topics covered here:

1.  [Fragments](#Fragments)
2.  [Activity Lifecycle](#ActivityLifecycle)
3.  [Configuration Changes](#ConfigurationChanges)
4.  [Starting Activities and Getting Results](#StartingActivities)
5.  [Saving Persistent State](#SavingPersistentState)
6.  [Permissions](#Permissions)
7.  [Process Lifecycle](#ProcessLifecycle)

### Developer Guides

The Activity class is an important part of an application's overall lifecycle, and the way activities are launched and put together is a fundamental part of the platform's application model. For a detailed perspective on the structure of an Android application and how activities behave, please read the [Application Fundamentals](/guide/topics/fundamentals) and [Tasks and Back Stack](/guide/components/tasks-and-back-stack) developer guides.

You can also find a detailed discussion about how to create activities in the [Activities](/guide/components/activities) developer guide.

### Fragments

The `[FragmentActivity](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html)` subclass can make use of the `[Fragment](https://developer.android.com/reference/androidx/fragment/app/Fragment.html)` class to better modularize their code, build more sophisticated user interfaces for larger screens, and help scale their application between small and large screens.

For more information about using fragments, read the [Fragments](/guide/components/fragments) developer guide.

### Activity Lifecycle

Activities in the system are managed as [activity stacks](https://developer.android.com/guide/components/activities/tasks-and-back-stack). When a new activity is started, it is usually placed on the top of the current stack and becomes the running activity -- the previous activity always remains below it in the stack, and will not come to the foreground again until the new activity exits. There can be one or multiple activity stacks visible on screen.

An activity has essentially four states:

*   If an activity is in the foreground of the screen (at the highest position of the topmost stack), it is _active_ or _running_. This is usually the activity that the user is currently interacting with.
*   If an activity has lost focus but is still presented to the user, it is _visible_. It is possible if a new non-full-sized or transparent activity has focus on top of your activity, another activity has higher position in multi-window mode, or the activity itself is not focusable in current windowing mode. Such activity is completely alive (it maintains all state and member information and remains attached to the window manager).
*   If an activity is completely obscured by another activity, it is _stopped_ or _hidden_. It still retains all state and member information, however, it is no longer visible to the user so its window is hidden and it will often be killed by the system when memory is needed elsewhere.
*   The system can drop the activity from memory by either asking it to finish, or simply killing its process, making it _destroyed_. When it is displayed again to the user, it must be completely restarted and restored to its previous state.

The following diagram shows the important state paths of an activity. The square rectangles represent callback methods you can implement to perform operations when the activity moves between states. The colored ovals are major states the activity can be in.

![State diagram for the Android activity lifecycle.](https://developer.android.com/images/activity_lifecycle.png)

There are three key loops you may be interested in monitoring within your activity:

*   The **entire lifetime** of an activity happens between the first call to `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` through to a single final call to `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))`. An activity will do all setup of "global" state in onCreate(), and release all remaining resources in onDestroy(). For example, if it has a thread running in the background to download data from the network, it may create that thread in onCreate() and then stop the thread in onDestroy().
*   The **visible lifetime** of an activity happens between a call to `[onStart()](/reference/android/app/Activity#onStart\(\))` until a corresponding call to `[onStop()](/reference/android/app/Activity#onStop\(\))`. During this time the user can see the activity on-screen, though it may not be in the foreground and interacting with the user. Between these two methods you can maintain resources that are needed to show the activity to the user. For example, you can register a `[BroadcastReceiver](/reference/android/content/BroadcastReceiver)` in onStart() to monitor for changes that impact your UI, and unregister it in onStop() when the user no longer sees what you are displaying. The onStart() and onStop() methods can be called multiple times, as the activity becomes visible and hidden to the user.
*   The **foreground lifetime** of an activity happens between a call to `[onResume()](/reference/android/app/Activity#onResume\(\))` until a corresponding call to `[onPause()](/reference/android/app/Activity#onPause\(\))`. During this time the activity is visible, active and interacting with the user. An activity can frequently go between the resumed and paused states -- for example when the device goes to sleep, when an activity result is delivered, when a new intent is delivered -- so the code in these methods should be fairly lightweight.

The entire lifecycle of an activity is defined by the following Activity methods. All of these are hooks that you can override to do appropriate work when the activity changes state. All activities will implement `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` to do their initial setup; many will also implement `[onPause()](/reference/android/app/Activity#onPause\(\))` to commit changes to data and prepare to pause interacting with the user, and `[onStop()](/reference/android/app/Activity#onStop\(\))` to handle no longer being visible on screen. You should always call up to your superclass when implementing these methods.

 public class Activity extends ApplicationContext {
     protected void onCreate(Bundle savedInstanceState);

     protected void onStart();

     protected void onRestart();

     protected void onResume();

     protected void onPause();

     protected void onStop();

     protected void onDestroy();
 }
 

In general the movement through an activity's lifecycle looks like this:

Method

Description

Killable?

Next

`[onCreate()](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`

Called when the activity is first created. This is where you should do all of your normal static set up: create views, bind data to lists, etc. This method also provides you with a Bundle containing the activity's previously frozen state, if there was one.

Always followed by `onStart()`.

No

`onStart()`

    

`[onRestart()](/reference/android/app/Activity#onRestart\(\))`

Called after your activity has been stopped, prior to it being started again.

Always followed by `onStart()`

No

`onStart()`

`[onStart()](/reference/android/app/Activity#onStart\(\))`

Called when the activity is becoming visible to the user.

Followed by `onResume()` if the activity comes to the foreground, or `onStop()` if it becomes hidden.

No

`onResume()` or `onStop()`

    

`[onResume()](/reference/android/app/Activity#onResume\(\))`

Called when the activity will start interacting with the user. At this point your activity is at the top of its activity stack, with user input going to it.

Always followed by `onPause()`.

No

`onPause()`

`[onPause()](/reference/android/app/Activity#onPause\(\))`

Called when the activity loses foreground state, is no longer focusable or before transition to stopped/hidden or destroyed state. The activity is still visible to user, so it's recommended to keep it visually active and continue updating the UI. Implementations of this method must be very quick because the next activity will not be resumed until this method returns.

Followed by either `onResume()` if the activity returns back to the front, or `onStop()` if it becomes invisible to the user.

**Pre-`[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)`**

`onResume()` or  
`onStop()`

`[onStop()](/reference/android/app/Activity#onStop\(\))`

Called when the activity is no longer visible to the user. This may happen either because a new activity is being started on top, an existing one is being brought in front of this one, or this one is being destroyed. This is typically used to stop animations and refreshing the UI, etc.

Followed by either `onRestart()` if this activity is coming back to interact with the user, or `onDestroy()` if this activity is going away.

**Yes**

`onRestart()` or  
`onDestroy()`

`[onDestroy()](/reference/android/app/Activity#onDestroy\(\))`

The final call you receive before your activity is destroyed. This can happen either because the activity is finishing (someone called `[Activity.finish](/reference/android/app/Activity#finish\(\))` on it), or because the system is temporarily destroying this instance of the activity to save space. You can distinguish between these two scenarios with the `[isFinishing()](/reference/android/app/Activity#isFinishing\(\))` method.

**Yes**

_nothing_

Note the "Killable" column in the above table -- for those methods that are marked as being killable, after that method returns the process hosting the activity may be killed by the system _at any time_ without another line of its code being executed. Because of this, you should use the `[onPause()](/reference/android/app/Activity#onPause\(\))` method to write any persistent data (such as user edits) to storage. In addition, the method `[onSaveInstanceState(android.os.Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` is called before placing the activity in such a background state, allowing you to save away any dynamic instance state in your activity into the given Bundle, to be later received in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` if the activity needs to be re-created. See the [Process Lifecycle](#ProcessLifecycle) section for more information on how the lifecycle of a process is tied to the activities it is hosting. Note that it is important to save persistent data in `[onPause()](/reference/android/app/Activity#onPause\(\))` instead of `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` because the latter is not part of the lifecycle callbacks, so will not be called in every situation as described in its documentation.

Be aware that these semantics will change slightly between applications targeting platforms starting with `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` vs. those targeting prior platforms. Starting with Honeycomb, an application is not in the killable state until its `[onStop()](/reference/android/app/Activity#onStop\(\))` has returned. This impacts when `[onSaveInstanceState(android.os.Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` may be called (it may be safely called after `[onPause()](/reference/android/app/Activity#onPause\(\))`) and allows an application to safely wait until `[onStop()](/reference/android/app/Activity#onStop\(\))` to save persistent state.

For applications targeting platforms starting with `[Build.VERSION_CODES.P](/reference/android/os/Build.VERSION_CODES#P)` `[onSaveInstanceState(android.os.Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` will always be called after `[onStop()](/reference/android/app/Activity#onStop\(\))`, so an application may safely perform fragment transactions in `[onStop()](/reference/android/app/Activity#onStop\(\))` and will be able to save persistent state later.

For those methods that are not marked as being killable, the activity's process will not be killed by the system starting from the time the method is called and continuing after it returns. Thus an activity is in the killable state, for example, between after `onStop()` to the start of `onResume()`. Keep in mind that under extreme memory pressure the system can kill the application process at any time.

### Configuration Changes

If the configuration of the device (as defined by the `[Resources.Configuration](/reference/android/content/res/Configuration)` class) changes, then anything displaying a user interface will need to update to match that configuration. Because Activity is the primary mechanism for interacting with the user, it includes special support for handling configuration changes.

Unless you specify otherwise, a configuration change (such as a change in screen orientation, language, input devices, etc.) will cause your current activity to be _destroyed_, going through the normal activity lifecycle process of `[onPause()](/reference/android/app/Activity#onPause\(\))`, `[onStop()](/reference/android/app/Activity#onStop\(\))`, and `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` as appropriate. If the activity had been in the foreground or visible to the user, once `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` is called in that instance then a new instance of the activity will be created, with whatever savedInstanceState the previous instance had generated from `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`.

This is done because any application resource, including layout files, can change based on any configuration value. Thus the only safe way to handle a configuration change is to re-retrieve all resources, including layouts, drawables, and strings. Because activities must already know how to save their state and re-create themselves from that state, this is a convenient way to have an activity restart itself with a new configuration.

In some special cases, you may want to bypass restarting of your activity based on one or more types of configuration changes. This is done with the `[android:configChanges](/reference/android/R.attr#configChanges)` attribute in its manifest. For any types of configuration changes you say that you handle there, you will receive a call to your current activity's `[onConfigurationChanged(Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` method instead of being restarted. If a configuration change involves any that you do not handle, however, the activity will still be restarted and `[onConfigurationChanged(Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` will not be called.

### Starting Activities and Getting Results

The `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))` method is used to start a new activity, which will be placed at the top of the activity stack. It takes a single argument, an `[Intent](/reference/android/content/Intent)`, which describes the activity to be executed.

Sometimes you want to get a result back from an activity when it ends. For example, you may start an activity that lets the user pick a person in a list of contacts; when it ends, it returns the person that was selected. To do this, you call the `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))` version with a second integer parameter identifying the call. The result will come back through your `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))` method.

When an activity exits, it can call `[setResult(int)](/reference/android/app/Activity#setResult\(int\))` to return data back to its parent. It must always supply a result code, which can be the standard results RESULT\_CANCELED, RESULT\_OK, or any custom values starting at RESULT\_FIRST\_USER. In addition, it can optionally return back an Intent containing any additional data it wants. All of this information appears back on the parent's `Activity.onActivityResult()`, along with the integer identifier it originally supplied.

If a child activity fails for any reason (such as crashing), the parent activity will receive a result with the code RESULT\_CANCELED.

 public class MyActivity extends Activity {
     ...

     static final int PICK\_CONTACT\_REQUEST \= 0;

     public boolean onKeyDown(int keyCode, KeyEvent event) {
         if (keyCode \== KeyEvent.KEYCODE\_DPAD\_CENTER) {
             // When the user center presses, let them pick a contact.
             startActivityForResult(
                 new Intent(Intent.ACTION\_PICK,
                 new Uri("content://contacts")),
                 PICK\_CONTACT\_REQUEST);
            return true;
         }
         return false;
     }

     protected void onActivityResult(int requestCode, int resultCode,
             Intent data) {
         if (requestCode \== PICK\_CONTACT\_REQUEST) {
             if (resultCode \== RESULT\_OK) {
                 // A contact was picked.  Here we will just display it
                 // to the user.
                 startActivity(new Intent(Intent.ACTION\_VIEW, data));
             }
         }
     }
 }
 

### Saving Persistent State

There are generally two kinds of persistent state that an activity will deal with: shared document-like data (typically stored in a SQLite database using a [content provider](/reference/android/content/ContentProvider)) and internal state such as user preferences.

For content provider data, we suggest that activities use an "edit in place" user model. That is, any edits a user makes are effectively made immediately without requiring an additional confirmation step. Supporting this model is generally a simple matter of following two rules:

*   When creating a new document, the backing database entry or file for it is created immediately. For example, if the user chooses to write a new email, a new entry for that email is created as soon as they start entering data, so that if they go to any other activity after that point this email will now appear in the list of drafts.
    
*   When an activity's `onPause()` method is called, it should commit to the backing content provider or file any changes the user has made. This ensures that those changes will be seen by any other activity that is about to run. You will probably want to commit your data even more aggressively at key times during your activity's lifecycle: for example before starting a new activity, before finishing your own activity, when the user switches between input fields, etc.
    

This model is designed to prevent data loss when a user is navigating between activities, and allows the system to safely kill an activity (because system resources are needed somewhere else) at any time after it has been stopped (or paused on platform versions before `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)`). Note this implies that the user pressing BACK from your activity does _not_ mean "cancel" -- it means to leave the activity with its current contents saved away. Canceling edits in an activity must be provided through some other mechanism, such as an explicit "revert" or "undo" option.

See the [content package](/reference/android/content/ContentProvider) for more information about content providers. These are a key aspect of how different activities invoke and propagate data between themselves.

The Activity class also provides an API for managing internal persistent state associated with an activity. This can be used, for example, to remember the user's preferred initial display in a calendar (day view or week view) or the user's default home page in a web browser.

Activity persistent state is managed with the method `[getPreferences(int)](/reference/android/app/Activity#getPreferences\(int\))`, allowing you to retrieve and modify a set of name/value pairs associated with the activity. To use preferences that are shared across multiple application components (activities, receivers, services, providers), you can use the underlying `[Context.getSharedPreferences()](/reference/android/content/Context#getSharedPreferences\(java.lang.String,%20int\))` method to retrieve a preferences object stored under a specific name. (Note that it is not possible to share settings data across application packages -- for that you will need a content provider.)

Here is an excerpt from a calendar activity that stores the user's preferred view mode in its persistent settings:

 public class CalendarActivity extends Activity {
     ...

     static final int DAY\_VIEW\_MODE \= 0;
     static final int WEEK\_VIEW\_MODE \= 1;

     private SharedPreferences mPrefs;
     private int mCurViewMode;

     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);

         mPrefs \= getSharedPreferences(getLocalClassName(), MODE\_PRIVATE);
         mCurViewMode \= mPrefs.getInt("view\_mode", DAY\_VIEW\_MODE);
     }

     protected void onPause() {
         super.onPause();

         SharedPreferences.Editor ed \= mPrefs.edit();
         ed.putInt("view\_mode", mCurViewMode);
         ed.commit();
     }
 }
 

### Permissions

The ability to start a particular Activity can be enforced when it is declared in its manifest's `[<activity>](/reference/android/R.styleable#AndroidManifestActivity)` tag. By doing so, other applications will need to declare a corresponding `[<uses-permission>](/reference/android/R.styleable#AndroidManifestUsesPermission)` element in their own manifest to be able to start that activity.

When starting an Activity you can set `[Intent.FLAG_GRANT_READ_URI_PERMISSION](/reference/android/content/Intent#FLAG_GRANT_READ_URI_PERMISSION)` and/or `[Intent.FLAG_GRANT_WRITE_URI_PERMISSION](/reference/android/content/Intent#FLAG_GRANT_WRITE_URI_PERMISSION)` on the Intent. This will grant the Activity access to the specific URIs in the Intent. Access will remain until the Activity has finished (it will remain across the hosting process being killed and other temporary destruction). As of `[Build.VERSION_CODES.GINGERBREAD](/reference/android/os/Build.VERSION_CODES#GINGERBREAD)`, if the Activity was already created and a new Intent is being delivered to `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, any newly granted URI permissions will be added to the existing ones it holds.

See the [Security and Permissions](/guide/topics/security/security) document for more information on permissions and security in general.

### Process Lifecycle

The Android system attempts to keep an application process around for as long as possible, but eventually will need to remove old processes when memory runs low. As described in [Activity Lifecycle](#ActivityLifecycle), the decision about which process to remove is intimately tied to the state of the user's interaction with it. In general, there are four states a process can be in based on the activities running in it, listed here in order of importance. The system will kill less important processes (the last ones) before it resorts to killing more important processes (the first ones).

1.  The **foreground activity** (the activity at the top of the screen that the user is currently interacting with) is considered the most important. Its process will only be killed as a last resort, if it uses more memory than is available on the device. Generally at this point the device has reached a memory paging state, so this is required in order to keep the user interface responsive.
    
2.  A **visible activity** (an activity that is visible to the user but not in the foreground, such as one sitting behind a foreground dialog or next to other activities in multi-window mode) is considered extremely important and will not be killed unless that is required to keep the foreground activity running.
    
3.  A **background activity** (an activity that is not visible to the user and has been stopped) is no longer critical, so the system may safely kill its process to reclaim memory for other foreground or visible processes. If its process needs to be killed, when the user navigates back to the activity (making it visible on the screen again), its `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` method will be called with the savedInstanceState it had previously supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` so that it can restart itself in the same state as the user last left it.
    
4.  An **empty process** is one hosting no activities or other application components (such as `[Service](/reference/android/app/Service)` or `[BroadcastReceiver](/reference/android/content/BroadcastReceiver)` classes). These are killed very quickly by the system as memory becomes low. For this reason, any background operation you do outside of an activity must be executed in the context of an activity BroadcastReceiver or Service to ensure that the system knows it needs to keep your process around.
    

Sometimes an Activity may need to do a long-running operation that exists independently of the activity lifecycle itself. An example may be a camera application that allows you to upload a picture to a web site. The upload may take a long time, and the application should allow the user to leave the application while it is executing. To accomplish this, your Activity should start a `[Service](/reference/android/app/Service)` in which the upload takes place. This allows the system to properly prioritize your process (considering it to be more important than other non-visible applications) for the duration of the upload, independent of whether the original activity is paused, stopped, or finished.

## Summary

### Nested classes

`interface`

`[Activity.ScreenCaptureCallback](/reference/android/app/Activity.ScreenCaptureCallback)`

Interface for observing screen captures of an `[Activity](/reference/android/app/Activity)`. 

### Constants

`int`

`[DEFAULT_KEYS_DIALER](/reference/android/app/Activity#DEFAULT_KEYS_DIALER)`

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to launch the dialer during default key handling.

`int`

`[DEFAULT_KEYS_DISABLE](/reference/android/app/Activity#DEFAULT_KEYS_DISABLE)`

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to turn off default handling of keys.

`int`

`[DEFAULT_KEYS_SEARCH_GLOBAL](/reference/android/app/Activity#DEFAULT_KEYS_SEARCH_GLOBAL)`

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to specify that unhandled keystrokes will start a global search (typically web search, but some platforms may define alternate methods for global search)

See `[android.app.SearchManager](/reference/android/app/SearchManager)` for more details.

`int`

`[DEFAULT_KEYS_SEARCH_LOCAL](/reference/android/app/Activity#DEFAULT_KEYS_SEARCH_LOCAL)`

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to specify that unhandled keystrokes will start an application-defined search.

`int`

`[DEFAULT_KEYS_SHORTCUT](/reference/android/app/Activity#DEFAULT_KEYS_SHORTCUT)`

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to execute a menu shortcut in default key handling.

`int`

`[FULLSCREEN_MODE_REQUEST_ENTER](/reference/android/app/Activity#FULLSCREEN_MODE_REQUEST_ENTER)`

Request type of `[requestFullscreenMode(int, android.os.OutcomeReceiver)](/reference/android/app/Activity#requestFullscreenMode\(int,%20android.os.OutcomeReceiver<java.lang.Void,java.lang.Throwable>\))`, to request enter fullscreen mode from multi-window mode.

`int`

`[FULLSCREEN_MODE_REQUEST_EXIT](/reference/android/app/Activity#FULLSCREEN_MODE_REQUEST_EXIT)`

Request type of `[requestFullscreenMode(int, android.os.OutcomeReceiver)](/reference/android/app/Activity#requestFullscreenMode\(int,%20android.os.OutcomeReceiver<java.lang.Void,java.lang.Throwable>\))`, to request exiting the requested fullscreen mode and restore to the previous multi-window mode.

`int`

`[OVERRIDE_TRANSITION_CLOSE](/reference/android/app/Activity#OVERRIDE_TRANSITION_CLOSE)`

Request type of `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))` or `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`, to override the closing transition.

`int`

`[OVERRIDE_TRANSITION_OPEN](/reference/android/app/Activity#OVERRIDE_TRANSITION_OPEN)`

Request type of `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))` or `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`, to override the opening transition.

`int`

`[RESULT_CANCELED](/reference/android/app/Activity#RESULT_CANCELED)`

Standard activity result: operation canceled.

`int`

`[RESULT_FIRST_USER](/reference/android/app/Activity#RESULT_FIRST_USER)`

Start of user-defined activity results.

`int`

`[RESULT_OK](/reference/android/app/Activity#RESULT_OK)`

Standard activity result: operation succeeded.

### Inherited constants

From class `[android.content.Context](/reference/android/content/Context)`

`[String](/reference/java/lang/String)`

`[ACCESSIBILITY_SERVICE](/reference/android/content/Context#ACCESSIBILITY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[AccessibilityManager](/reference/android/view/accessibility/AccessibilityManager)` for giving the user feedback for UI events through the registered event listeners.

`[String](/reference/java/lang/String)`

`[ACCOUNT_SERVICE](/reference/android/content/Context#ACCOUNT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[AccountManager](/reference/android/accounts/AccountManager)` for receiving intents at a time of your choosing.

`[String](/reference/java/lang/String)`

`[ACTIVITY_SERVICE](/reference/android/content/Context#ACTIVITY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ActivityManager](/reference/android/app/ActivityManager)` for interacting with the global system state.

`[String](/reference/java/lang/String)`

`[ADVANCED_PROTECTION_SERVICE](/reference/android/content/Context#ADVANCED_PROTECTION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve an `[AdvancedProtectionManager](/reference/android/security/advancedprotection/AdvancedProtectionManager)`

`[String](/reference/java/lang/String)`

`[ALARM_SERVICE](/reference/android/content/Context#ALARM_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[AlarmManager](/reference/android/app/AlarmManager)` for receiving intents at a time of your choosing.

`[String](/reference/java/lang/String)`

`[APPWIDGET_SERVICE](/reference/android/content/Context#APPWIDGET_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[AppWidgetManager](/reference/android/appwidget/AppWidgetManager)` for accessing AppWidgets.

`[String](/reference/java/lang/String)`

`[APP_FUNCTION_SERVICE](/reference/android/content/Context#APP_FUNCTION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve an `[AppFunctionManager](/reference/android/app/appfunctions/AppFunctionManager)` for executing app functions.

`[String](/reference/java/lang/String)`

`[APP_OPS_SERVICE](/reference/android/content/Context#APP_OPS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[AppOpsManager](/reference/android/app/AppOpsManager)` for tracking application operations on the device.

`[String](/reference/java/lang/String)`

`[APP_SEARCH_SERVICE](/reference/android/content/Context#APP_SEARCH_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve an `[AppSearchManager](/reference/android/app/appsearch/AppSearchManager)` for indexing and querying app data managed by the system.

`[String](/reference/java/lang/String)`

`[AUDIO_SERVICE](/reference/android/content/Context#AUDIO_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[AudioManager](/reference/android/media/AudioManager)` for handling management of volume, ringer modes and audio routing.

`[String](/reference/java/lang/String)`

`[BATTERY_SERVICE](/reference/android/content/Context#BATTERY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[BatteryManager](/reference/android/os/BatteryManager)` for managing battery state.

`int`

`[BIND_ABOVE_CLIENT](/reference/android/content/Context#BIND_ABOVE_CLIENT)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: indicates that the client application binding to this service considers the service to be more important than the app itself.

`int`

`[BIND_ADJUST_WITH_ACTIVITY](/reference/android/content/Context#BIND_ADJUST_WITH_ACTIVITY)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: If binding from an activity, allow the target service's process importance to be raised based on whether the activity is visible to the user, regardless whether another flag is used to reduce the amount that the client process's overall importance is used to impact it.

`int`

`[BIND_ALLOW_ACTIVITY_STARTS](/reference/android/content/Context#BIND_ALLOW_ACTIVITY_STARTS)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: If binding from an app that is visible, the bound service is allowed to start an activity from background.

`int`

`[BIND_ALLOW_OOM_MANAGEMENT](/reference/android/content/Context#BIND_ALLOW_OOM_MANAGEMENT)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: allow the process hosting the bound service to go through its normal memory management.

`int`

`[BIND_AUTO_CREATE](/reference/android/content/Context#BIND_AUTO_CREATE)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: automatically create the service as long as the binding exists.

`int`

`[BIND_DEBUG_UNBIND](/reference/android/content/Context#BIND_DEBUG_UNBIND)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: include debugging help for mismatched calls to unbind.

`int`

`[BIND_EXTERNAL_SERVICE](/reference/android/content/Context#BIND_EXTERNAL_SERVICE)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: The service being bound is an `[isolated](/reference/android/R.attr#isolatedProcess)`, `[external](/reference/android/R.attr#externalService)` service.

`long`

`[BIND_EXTERNAL_SERVICE_LONG](/reference/android/content/Context#BIND_EXTERNAL_SERVICE_LONG)`

Works in the same way as `[BIND_EXTERNAL_SERVICE](/reference/android/content/Context#BIND_EXTERNAL_SERVICE)`, but it's defined as a `long` value that is compatible to `[BindServiceFlags](/reference/android/content/Context.BindServiceFlags)`.

`int`

`[BIND_IMPORTANT](/reference/android/content/Context#BIND_IMPORTANT)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: this service is very important to the client, so should be brought to the foreground process level when the client is.

`int`

`[BIND_INCLUDE_CAPABILITIES](/reference/android/content/Context#BIND_INCLUDE_CAPABILITIES)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: If binding from an app that has specific capabilities due to its foreground state such as an activity or foreground service, then this flag will allow the bound app to get the same capabilities, as long as it has the required permissions as well.

`int`

`[BIND_NOT_FOREGROUND](/reference/android/content/Context#BIND_NOT_FOREGROUND)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: don't allow this binding to raise the target service's process to the foreground scheduling priority.

`int`

`[BIND_NOT_PERCEPTIBLE](/reference/android/content/Context#BIND_NOT_PERCEPTIBLE)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: If binding from an app that is visible or user-perceptible, lower the target service's importance to below the perceptible level.

`int`

`[BIND_PACKAGE_ISOLATED_PROCESS](/reference/android/content/Context#BIND_PACKAGE_ISOLATED_PROCESS)`

Flag for `[bindIsolatedService(Intent, BindServiceFlags, String, Executor, ServiceConnection)](/reference/android/content/Context#bindIsolatedService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.lang.String,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: Bind the service into a shared isolated process, but only with other isolated services from the same package that declare the same process name.

`int`

`[BIND_SHARED_ISOLATED_PROCESS](/reference/android/content/Context#BIND_SHARED_ISOLATED_PROCESS)`

Flag for `[bindIsolatedService(Intent, BindServiceFlags, String, Executor, ServiceConnection)](/reference/android/content/Context#bindIsolatedService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.lang.String,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: Bind the service into a shared isolated process.

`int`

`[BIND_WAIVE_PRIORITY](/reference/android/content/Context#BIND_WAIVE_PRIORITY)`

Flag for `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`: don't impact the scheduling or memory management priority of the target service's hosting process.

`[String](/reference/java/lang/String)`

`[BIOMETRIC_SERVICE](/reference/android/content/Context#BIOMETRIC_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[BiometricManager](/reference/android/hardware/biometrics/BiometricManager)` for handling biometric and PIN/pattern/password authentication.

`[String](/reference/java/lang/String)`

`[BLOB_STORE_SERVICE](/reference/android/content/Context#BLOB_STORE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[BlobStoreManager](/reference/android/app/blob/BlobStoreManager)` for contributing and accessing data blobs from the blob store maintained by the system.

`[String](/reference/java/lang/String)`

`[BLUETOOTH_SERVICE](/reference/android/content/Context#BLUETOOTH_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[BluetoothManager](/reference/android/bluetooth/BluetoothManager)` for using Bluetooth.

`[String](/reference/java/lang/String)`

`[BUGREPORT_SERVICE](/reference/android/content/Context#BUGREPORT_SERVICE)`

Service to capture a bugreport.

`[String](/reference/java/lang/String)`

`[CAMERA_SERVICE](/reference/android/content/Context#CAMERA_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[CameraManager](/reference/android/hardware/camera2/CameraManager)` for interacting with camera devices.

`[String](/reference/java/lang/String)`

`[CAPTIONING_SERVICE](/reference/android/content/Context#CAPTIONING_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[CaptioningManager](/reference/android/view/accessibility/CaptioningManager)` for obtaining captioning properties and listening for changes in captioning preferences.

`[String](/reference/java/lang/String)`

`[CARRIER_CONFIG_SERVICE](/reference/android/content/Context#CARRIER_CONFIG_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[CarrierConfigManager](/reference/android/telephony/CarrierConfigManager)` for reading carrier configuration values.

`[String](/reference/java/lang/String)`

`[CHOOSER_SERVICE](/reference/android/content/Context#CHOOSER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ChooserManager](/reference/android/service/chooser/ChooserManager)`.

`[String](/reference/java/lang/String)`

`[CLIPBOARD_SERVICE](/reference/android/content/Context#CLIPBOARD_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ClipboardManager](/reference/android/content/ClipboardManager)` for accessing and modifying the contents of the global clipboard.

`[String](/reference/java/lang/String)`

`[COMPANION_DEVICE_SERVICE](/reference/android/content/Context#COMPANION_DEVICE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[CompanionDeviceManager](/reference/android/companion/CompanionDeviceManager)` for managing companion devices

`[String](/reference/java/lang/String)`

`[CONNECTIVITY_DIAGNOSTICS_SERVICE](/reference/android/content/Context#CONNECTIVITY_DIAGNOSTICS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ConnectivityDiagnosticsManager](/reference/android/net/ConnectivityDiagnosticsManager)` for performing network connectivity diagnostics as well as receiving network connectivity information from the system.

`[String](/reference/java/lang/String)`

`[CONNECTIVITY_SERVICE](/reference/android/content/Context#CONNECTIVITY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ConnectivityManager](/reference/android/net/ConnectivityManager)` for handling management of network connections.

`[String](/reference/java/lang/String)`

`[CONSUMER_IR_SERVICE](/reference/android/content/Context#CONSUMER_IR_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ConsumerIrManager](/reference/android/hardware/ConsumerIrManager)` for transmitting infrared signals from the device.

`[String](/reference/java/lang/String)`

`[CONTACT_KEYS_SERVICE](/reference/android/content/Context#CONTACT_KEYS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[E2eeContactKeysManager](/reference/android/provider/E2eeContactKeysManager)` to managing contact keys.

`int`

`[CONTEXT_IGNORE_SECURITY](/reference/android/content/Context#CONTEXT_IGNORE_SECURITY)`

Flag for use with `[createPackageContext(String, int)](/reference/android/content/Context#createPackageContext\(java.lang.String,%20int\))`: ignore any security restrictions on the Context being requested, allowing it to always be loaded.

`int`

`[CONTEXT_INCLUDE_CODE](/reference/android/content/Context#CONTEXT_INCLUDE_CODE)`

Flag for use with `[createPackageContext(String, int)](/reference/android/content/Context#createPackageContext\(java.lang.String,%20int\))`: include the application code with the context.

`int`

`[CONTEXT_RESTRICTED](/reference/android/content/Context#CONTEXT_RESTRICTED)`

Flag for use with `[createPackageContext(String, int)](/reference/android/content/Context#createPackageContext\(java.lang.String,%20int\))`: a restricted context may disable specific features.

`[String](/reference/java/lang/String)`

`[CREDENTIAL_SERVICE](/reference/android/content/Context#CREDENTIAL_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[CredentialManager](/reference/android/credentials/CredentialManager)` to authenticate a user to your app.

`[String](/reference/java/lang/String)`

`[CROSS_PROFILE_APPS_SERVICE](/reference/android/content/Context#CROSS_PROFILE_APPS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[CrossProfileApps](/reference/android/content/pm/CrossProfileApps)` for cross profile operations.

`int`

`[DEVICE_ID_DEFAULT](/reference/android/content/Context#DEVICE_ID_DEFAULT)`

The default device ID, which is the ID of the primary (non-virtual) device.

`int`

`[DEVICE_ID_INVALID](/reference/android/content/Context#DEVICE_ID_INVALID)`

Invalid device ID.

`[String](/reference/java/lang/String)`

`[DEVICE_LOCK_SERVICE](/reference/android/content/Context#DEVICE_LOCK_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[DeviceLockManager](/reference/android/devicelock/DeviceLockManager)`.

`[String](/reference/java/lang/String)`

`[DEVICE_POLICY_SERVICE](/reference/android/content/Context#DEVICE_POLICY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[DevicePolicyManager](/reference/android/app/admin/DevicePolicyManager)` for working with global device policy management.

`[String](/reference/java/lang/String)`

`[DISPLAY_HASH_SERVICE](/reference/android/content/Context#DISPLAY_HASH_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to access `[DisplayHashManager](/reference/android/view/displayhash/DisplayHashManager)` to handle display hashes.

`[String](/reference/java/lang/String)`

`[DISPLAY_SERVICE](/reference/android/content/Context#DISPLAY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[DisplayManager](/reference/android/hardware/display/DisplayManager)` for interacting with display devices.

`[String](/reference/java/lang/String)`

`[DOMAIN_VERIFICATION_SERVICE](/reference/android/content/Context#DOMAIN_VERIFICATION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to access `[DomainVerificationManager](/reference/android/content/pm/verify/domain/DomainVerificationManager)` to retrieve approval and user state for declared web domains.

`[String](/reference/java/lang/String)`

`[DOWNLOAD_SERVICE](/reference/android/content/Context#DOWNLOAD_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[DownloadManager](/reference/android/app/DownloadManager)` for requesting HTTP downloads.

`[String](/reference/java/lang/String)`

`[DROPBOX_SERVICE](/reference/android/content/Context#DROPBOX_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[DropBoxManager](/reference/android/os/DropBoxManager)` instance for recording diagnostic logs.

`[String](/reference/java/lang/String)`

`[EUICC_SERVICE](/reference/android/content/Context#EUICC_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[EuiccManager](/reference/android/telephony/euicc/EuiccManager)` to manage the device eUICC (embedded SIM).

`[String](/reference/java/lang/String)`

`[FILE_INTEGRITY_SERVICE](/reference/android/content/Context#FILE_INTEGRITY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve an `[FileIntegrityManager](/reference/android/security/FileIntegrityManager)`.

`[String](/reference/java/lang/String)`

`[FINGERPRINT_SERVICE](/reference/android/content/Context#FINGERPRINT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[FingerprintManager](/reference/android/hardware/fingerprint/FingerprintManager)` for handling management of fingerprints.

`[String](/reference/java/lang/String)`

`[GAME_SERVICE](/reference/android/content/Context#GAME_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[GameManager](/reference/android/app/GameManager)`.

`[String](/reference/java/lang/String)`

`[GRAMMATICAL_INFLECTION_SERVICE](/reference/android/content/Context#GRAMMATICAL_INFLECTION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[GrammaticalInflectionManager](/reference/android/app/GrammaticalInflectionManager)`.

`[String](/reference/java/lang/String)`

`[HARDWARE_PROPERTIES_SERVICE](/reference/android/content/Context#HARDWARE_PROPERTIES_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[HardwarePropertiesManager](/reference/android/os/HardwarePropertiesManager)` for accessing the hardware properties service.

`[String](/reference/java/lang/String)`

`[HEALTHCONNECT_SERVICE](/reference/android/content/Context#HEALTHCONNECT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[HealthConnectManager](/reference/android/health/connect/HealthConnectManager)`.

`[String](/reference/java/lang/String)`

`[INPUT_METHOD_SERVICE](/reference/android/content/Context#INPUT_METHOD_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[InputMethodManager](/reference/android/view/inputmethod/InputMethodManager)` for accessing input methods.

`[String](/reference/java/lang/String)`

`[INPUT_SERVICE](/reference/android/content/Context#INPUT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[InputManager](/reference/android/hardware/input/InputManager)` for interacting with input devices.

`[String](/reference/java/lang/String)`

`[IPSEC_SERVICE](/reference/android/content/Context#IPSEC_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[IpSecManager](/reference/android/net/IpSecManager)` for encrypting Sockets or Networks with IPSec.

`[String](/reference/java/lang/String)`

`[JOB_SCHEDULER_SERVICE](/reference/android/content/Context#JOB_SCHEDULER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[JobScheduler](/reference/android/app/job/JobScheduler)` instance for managing occasional background tasks.

`[String](/reference/java/lang/String)`

`[KEYGUARD_SERVICE](/reference/android/content/Context#KEYGUARD_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[KeyguardManager](/reference/android/app/KeyguardManager)` for controlling keyguard.

`[String](/reference/java/lang/String)`

`[KEYSTORE_SERVICE](/reference/android/content/Context#KEYSTORE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[KeyStoreManager](/reference/android/security/keystore/KeyStoreManager)` for accessing [Android Keystore](/privacy-and-security/keystore) functions.

`[String](/reference/java/lang/String)`

`[LAUNCHER_APPS_SERVICE](/reference/android/content/Context#LAUNCHER_APPS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[LauncherApps](/reference/android/content/pm/LauncherApps)` for querying and monitoring launchable apps across profiles of a user.

`[String](/reference/java/lang/String)`

`[LAYOUT_INFLATER_SERVICE](/reference/android/content/Context#LAYOUT_INFLATER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[LayoutInflater](/reference/android/view/LayoutInflater)` for inflating layout resources in this context.

`[String](/reference/java/lang/String)`

`[LOCALE_SERVICE](/reference/android/content/Context#LOCALE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[LocaleManager](/reference/android/app/LocaleManager)`.

`[String](/reference/java/lang/String)`

`[LOCATION_SERVICE](/reference/android/content/Context#LOCATION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[LocationManager](/reference/android/location/LocationManager)` for controlling location updates.

`[String](/reference/java/lang/String)`

`[MEDIA_COMMUNICATION_SERVICE](/reference/android/content/Context#MEDIA_COMMUNICATION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[MediaCommunicationManager](/reference/android/media/MediaCommunicationManager)` for managing `[MediaSession2](/reference/android/media/MediaSession2)`.

`[String](/reference/java/lang/String)`

`[MEDIA_METRICS_SERVICE](/reference/android/content/Context#MEDIA_METRICS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[MediaMetricsManager](/reference/android/media/metrics/MediaMetricsManager)` for interacting with media metrics on the device.

`[String](/reference/java/lang/String)`

`[MEDIA_PROJECTION_SERVICE](/reference/android/content/Context#MEDIA_PROJECTION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[MediaProjectionManager](/reference/android/media/projection/MediaProjectionManager)` instance for managing media projection sessions.

`[String](/reference/java/lang/String)`

`[MEDIA_QUALITY_SERVICE](/reference/android/content/Context#MEDIA_QUALITY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[MediaQualityManager](/reference/android/media/quality/MediaQualityManager)` for standardize picture and audio API parameters.

`[String](/reference/java/lang/String)`

`[MEDIA_ROUTER_SERVICE](/reference/android/content/Context#MEDIA_ROUTER_SERVICE)`

Use with `[getSystemService(Class)](/reference/android/content/Context#getSystemService\(java.lang.Class<T>\))` to retrieve a `[MediaRouter](/reference/android/media/MediaRouter)` for controlling and managing routing of media.

`[String](/reference/java/lang/String)`

`[MEDIA_SESSION_SERVICE](/reference/android/content/Context#MEDIA_SESSION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[MediaSessionManager](/reference/android/media/session/MediaSessionManager)` for managing media Sessions.

`[String](/reference/java/lang/String)`

`[MIDI_SERVICE](/reference/android/content/Context#MIDI_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[MidiManager](/reference/android/media/midi/MidiManager)` for accessing the MIDI service.

`int`

`[MODE_APPEND](/reference/android/content/Context#MODE_APPEND)`

File creation mode: for use with `[openFileOutput(String, int)](/reference/android/content/Context#openFileOutput\(java.lang.String,%20int\))`, if the file already exists then write data to the end of the existing file instead of erasing it.

`int`

`[MODE_ENABLE_WRITE_AHEAD_LOGGING](/reference/android/content/Context#MODE_ENABLE_WRITE_AHEAD_LOGGING)`

Database open flag: when set, the database is opened with write-ahead logging enabled by default.

`int`

`[MODE_MULTI_PROCESS](/reference/android/content/Context#MODE_MULTI_PROCESS)`

_This constant was deprecated in API level 23. MODE\_MULTI\_PROCESS does not work reliably in some versions of Android, and furthermore does not provide any mechanism for reconciling concurrent modifications across processes. Applications should not attempt to use it. Instead, they should use an explicit cross-process data management approach such as `[ContentProvider](/reference/android/content/ContentProvider)`._

`int`

`[MODE_NO_LOCALIZED_COLLATORS](/reference/android/content/Context#MODE_NO_LOCALIZED_COLLATORS)`

Database open flag: when set, the database is opened without support for localized collators.

`int`

`[MODE_PRIVATE](/reference/android/content/Context#MODE_PRIVATE)`

File creation mode: the default mode, where the created file can only be accessed by the calling application (or all applications sharing the same user ID).

`int`

`[MODE_WORLD_READABLE](/reference/android/content/Context#MODE_WORLD_READABLE)`

_This constant was deprecated in API level 17. Creating world-readable files is very dangerous, and likely to cause security holes in applications. It is strongly discouraged; instead, applications should use more formal mechanism for interactions such as `[ContentProvider](/reference/android/content/ContentProvider)`, `[BroadcastReceiver](/reference/android/content/BroadcastReceiver)`, and `[Service](/reference/android/app/Service)`. There are no guarantees that this access mode will remain on a file, such as when it goes through a backup and restore._

`int`

`[MODE_WORLD_WRITEABLE](/reference/android/content/Context#MODE_WORLD_WRITEABLE)`

_This constant was deprecated in API level 17. Creating world-writable files is very dangerous, and likely to cause security holes in applications. It is strongly discouraged; instead, applications should use more formal mechanism for interactions such as `[ContentProvider](/reference/android/content/ContentProvider)`, `[BroadcastReceiver](/reference/android/content/BroadcastReceiver)`, and `[Service](/reference/android/app/Service)`. There are no guarantees that this access mode will remain on a file, such as when it goes through a backup and restore._

`[String](/reference/java/lang/String)`

`[NETWORK_STATS_SERVICE](/reference/android/content/Context#NETWORK_STATS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[NetworkStatsManager](/reference/android/app/usage/NetworkStatsManager)` for querying network usage stats.

`[String](/reference/java/lang/String)`

`[NFC_SERVICE](/reference/android/content/Context#NFC_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[NfcManager](/reference/android/nfc/NfcManager)` for using NFC.

`[String](/reference/java/lang/String)`

`[NOTIFICATION_SERVICE](/reference/android/content/Context#NOTIFICATION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[NotificationManager](/reference/android/app/NotificationManager)` for informing the user of background events.

`[String](/reference/java/lang/String)`

`[NSD_SERVICE](/reference/android/content/Context#NSD_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[NsdManager](/reference/android/net/nsd/NsdManager)` for handling management of network service discovery

`[String](/reference/java/lang/String)`

`[OVERLAY_SERVICE](/reference/android/content/Context#OVERLAY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[OverlayManager](/reference/android/content/om/OverlayManager)` for managing overlay packages.

`[String](/reference/java/lang/String)`

`[PEOPLE_SERVICE](/reference/android/content/Context#PEOPLE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to access a `[PeopleManager](/reference/android/app/people/PeopleManager)` to interact with your published conversations.

`[String](/reference/java/lang/String)`

`[PERFORMANCE_HINT_SERVICE](/reference/android/content/Context#PERFORMANCE_HINT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[PerformanceHintManager](/reference/android/os/PerformanceHintManager)` for accessing the performance hinting service.

`[String](/reference/java/lang/String)`

`[PERSISTENT_DATA_BLOCK_SERVICE](/reference/android/content/Context#PERSISTENT_DATA_BLOCK_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[PersistentDataBlockManager](/reference/android/service/persistentdata/PersistentDataBlockManager)` instance for interacting with a storage device that lives across factory resets.

`[String](/reference/java/lang/String)`

`[POWER_SERVICE](/reference/android/content/Context#POWER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[PowerManager](/reference/android/os/PowerManager)` for controlling power management, including "wake locks," which let you keep the device on while you're running long tasks.

`[String](/reference/java/lang/String)`

`[PRINT_SERVICE](/reference/android/content/Context#PRINT_SERVICE)`

`[PrintManager](/reference/android/print/PrintManager)` for printing and managing printers and print tasks.

`[String](/reference/java/lang/String)`

`[PROFILING_SERVICE](/reference/android/content/Context#PROFILING_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve an `[ProfilingManager](/reference/android/os/ProfilingManager)`.

`int`

`[RECEIVER_EXPORTED](/reference/android/content/Context#RECEIVER_EXPORTED)`

Flag for `[registerReceiver(BroadcastReceiver, IntentFilter)](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter\))`: The receiver can receive broadcasts from other Apps.

`int`

`[RECEIVER_NOT_EXPORTED](/reference/android/content/Context#RECEIVER_NOT_EXPORTED)`

Flag for `[registerReceiver(BroadcastReceiver, IntentFilter)](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter\))`: The receiver cannot receive broadcasts from other Apps.

`int`

`[RECEIVER_VISIBLE_TO_INSTANT_APPS](/reference/android/content/Context#RECEIVER_VISIBLE_TO_INSTANT_APPS)`

Flag for `[registerReceiver(BroadcastReceiver, IntentFilter)](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter\))`: The receiver can receive broadcasts from Instant Apps.

`[String](/reference/java/lang/String)`

`[RESTRICTIONS_SERVICE](/reference/android/content/Context#RESTRICTIONS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[RestrictionsManager](/reference/android/content/RestrictionsManager)` for retrieving application restrictions and requesting permissions for restricted operations.

`[String](/reference/java/lang/String)`

`[ROLE_SERVICE](/reference/android/content/Context#ROLE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[RoleManager](/reference/android/app/role/RoleManager)` for managing roles.

`[String](/reference/java/lang/String)`

`[SATELLITE_SERVICE](/reference/android/content/Context#SATELLITE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[SatelliteManager](/reference/android/telephony/satellite/SatelliteManager)` for accessing satellite functionality.

`[String](/reference/java/lang/String)`

`[SEARCH_SERVICE](/reference/android/content/Context#SEARCH_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[SearchManager](/reference/android/app/SearchManager)` for handling searches.

`[String](/reference/java/lang/String)`

`[SECURITY_STATE_SERVICE](/reference/android/content/Context#SECURITY_STATE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[SecurityStateManager](/reference/android/os/SecurityStateManager)` for accessing the security state manager service.

`[String](/reference/java/lang/String)`

`[SENSOR_SERVICE](/reference/android/content/Context#SENSOR_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[SensorManager](/reference/android/hardware/SensorManager)` for accessing sensors.

`[String](/reference/java/lang/String)`

`[SHORTCUT_SERVICE](/reference/android/content/Context#SHORTCUT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[ShortcutManager](/reference/android/content/pm/ShortcutManager)` for accessing the launcher shortcut service.

`[String](/reference/java/lang/String)`

`[STATUS_BAR_SERVICE](/reference/android/content/Context#STATUS_BAR_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[StatusBarManager](/reference/android/app/StatusBarManager)` for interacting with the status bar and quick settings.

`[String](/reference/java/lang/String)`

`[STORAGE_SERVICE](/reference/android/content/Context#STORAGE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[StorageManager](/reference/android/os/storage/StorageManager)` for accessing system storage functions.

`[String](/reference/java/lang/String)`

`[STORAGE_STATS_SERVICE](/reference/android/content/Context#STORAGE_STATS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[StorageStatsManager](/reference/android/app/usage/StorageStatsManager)` for accessing system storage statistics.

`[String](/reference/java/lang/String)`

`[SYSTEM_HEALTH_SERVICE](/reference/android/content/Context#SYSTEM_HEALTH_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[SystemHealthManager](/reference/android/os/health/SystemHealthManager)` for accessing system health (battery, power, memory, etc) metrics.

`[String](/reference/java/lang/String)`

`[TELECOM_SERVICE](/reference/android/content/Context#TELECOM_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TelecomManager](/reference/android/telecom/TelecomManager)` to manage telecom-related features of the device.

`[String](/reference/java/lang/String)`

`[TELEPHONY_IMS_SERVICE](/reference/android/content/Context#TELEPHONY_IMS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve an `[ImsManager](/reference/android/telephony/ims/ImsManager)`.

`[String](/reference/java/lang/String)`

`[TELEPHONY_PHONE_NUMBER_SERVICE](/reference/android/content/Context#TELEPHONY_PHONE_NUMBER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[PhoneNumberManager](/reference/android/telephony/PhoneNumberManager)` for parsing phone numbers.

`[String](/reference/java/lang/String)`

`[TELEPHONY_SERVICE](/reference/android/content/Context#TELEPHONY_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TelephonyManager](/reference/android/telephony/TelephonyManager)` for handling management the telephony features of the device.

`[String](/reference/java/lang/String)`

`[TELEPHONY_SUBSCRIPTION_SERVICE](/reference/android/content/Context#TELEPHONY_SUBSCRIPTION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[SubscriptionManager](/reference/android/telephony/SubscriptionManager)` for handling management the telephony subscriptions of the device.

`[String](/reference/java/lang/String)`

`[TETHERING_SERVICE](/reference/android/content/Context#TETHERING_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TetheringManager](/reference/android/net/TetheringManager)` for managing tethering functions.

`[String](/reference/java/lang/String)`

`[TEXT_CLASSIFICATION_SERVICE](/reference/android/content/Context#TEXT_CLASSIFICATION_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TextClassificationManager](/reference/android/view/textclassifier/TextClassificationManager)` for text classification services.

`[String](/reference/java/lang/String)`

`[TEXT_SERVICES_MANAGER_SERVICE](/reference/android/content/Context#TEXT_SERVICES_MANAGER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TextServicesManager](/reference/android/view/textservice/TextServicesManager)` for accessing text services.

`[String](/reference/java/lang/String)`

`[TV_AD_SERVICE](/reference/android/content/Context#TV_AD_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TvAdManager](/reference/android/media/tv/ad/TvAdManager)` for interacting with TV client-side advertisement services on the device.

`[String](/reference/java/lang/String)`

`[TV_INPUT_SERVICE](/reference/android/content/Context#TV_INPUT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TvInputManager](/reference/android/media/tv/TvInputManager)` for interacting with TV inputs on the device.

`[String](/reference/java/lang/String)`

`[TV_INTERACTIVE_APP_SERVICE](/reference/android/content/Context#TV_INTERACTIVE_APP_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[TvInteractiveAppManager](/reference/android/media/tv/interactive/TvInteractiveAppManager)` for interacting with TV interactive applications on the device.

`[String](/reference/java/lang/String)`

`[UI_MODE_SERVICE](/reference/android/content/Context#UI_MODE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[UiModeManager](/reference/android/app/UiModeManager)` for controlling UI modes.

`[String](/reference/java/lang/String)`

`[USAGE_STATS_SERVICE](/reference/android/content/Context#USAGE_STATS_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[UsageStatsManager](/reference/android/app/usage/UsageStatsManager)` for querying device usage stats.

`[String](/reference/java/lang/String)`

`[USB_SERVICE](/reference/android/content/Context#USB_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[UsbManager](/reference/android/hardware/usb/UsbManager)` for access to USB devices (as a USB host) and for controlling this device's behavior as a USB device.

`[String](/reference/java/lang/String)`

`[USER_SERVICE](/reference/android/content/Context#USER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[UserManager](/reference/android/os/UserManager)` for managing users on devices that support multiple users.

`[String](/reference/java/lang/String)`

`[VIBRATOR_MANAGER_SERVICE](/reference/android/content/Context#VIBRATOR_MANAGER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[VibratorManager](/reference/android/os/VibratorManager)` for accessing the device vibrators, interacting with individual ones and playing synchronized effects on multiple vibrators.

`[String](/reference/java/lang/String)`

`[VIBRATOR_SERVICE](/reference/android/content/Context#VIBRATOR_SERVICE)`

_This constant was deprecated in API level 31. Use `[VibratorManager](/reference/android/os/VibratorManager)` to retrieve the default system vibrator._

`[String](/reference/java/lang/String)`

`[VIRTUAL_DEVICE_SERVICE](/reference/android/content/Context#VIRTUAL_DEVICE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[VirtualDeviceManager](/reference/android/companion/virtual/VirtualDeviceManager)` for managing virtual devices.

`[String](/reference/java/lang/String)`

`[VPN_MANAGEMENT_SERVICE](/reference/android/content/Context#VPN_MANAGEMENT_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[VpnManager](/reference/android/net/VpnManager)` to manage profiles for the platform built-in VPN.

`[String](/reference/java/lang/String)`

`[WALLPAPER_SERVICE](/reference/android/content/Context#WALLPAPER_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a com.android.server.WallpaperService for accessing wallpapers.

`[String](/reference/java/lang/String)`

`[WIFI_AWARE_SERVICE](/reference/android/content/Context#WIFI_AWARE_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[WifiAwareManager](/reference/android/net/wifi/aware/WifiAwareManager)` for handling management of Wi-Fi Aware.

`[String](/reference/java/lang/String)`

`[WIFI_P2P_SERVICE](/reference/android/content/Context#WIFI_P2P_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[WifiP2pManager](/reference/android/net/wifi/p2p/WifiP2pManager)` for handling management of Wi-Fi peer-to-peer connections.

`[String](/reference/java/lang/String)`

`[WIFI_RTT_RANGING_SERVICE](/reference/android/content/Context#WIFI_RTT_RANGING_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[WifiRttManager](/reference/android/net/wifi/rtt/WifiRttManager)` for ranging devices with wifi.

`[String](/reference/java/lang/String)`

`[WIFI_SERVICE](/reference/android/content/Context#WIFI_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[WifiManager](/reference/android/net/wifi/WifiManager)` for handling management of Wi-Fi access.

`[String](/reference/java/lang/String)`

`[WINDOW_SERVICE](/reference/android/content/Context#WINDOW_SERVICE)`

Use with `[getSystemService(java.lang.String)](/reference/android/content/Context#getSystemService\(java.lang.String\))` to retrieve a `[WindowManager](/reference/android/view/WindowManager)` for accessing the system's window manager.

From interface `[android.content.ComponentCallbacks2](/reference/android/content/ComponentCallbacks2)`

`int`

`[TRIM_MEMORY_BACKGROUND](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_BACKGROUND)`

Level for `[onTrimMemory(int)](/reference/android/content/ComponentCallbacks2#onTrimMemory\(int\))`: the process has gone on to the LRU list.

`int`

`[TRIM_MEMORY_COMPLETE](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_COMPLETE)`

_This constant was deprecated in API level 35. Apps are not notified of this level since API level 34_

`int`

`[TRIM_MEMORY_MODERATE](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_MODERATE)`

_This constant was deprecated in API level 35. Apps are not notified of this level since API level 34_

`int`

`[TRIM_MEMORY_RUNNING_CRITICAL](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_RUNNING_CRITICAL)`

_This constant was deprecated in API level 35. Apps are not notified of this level since API level 34_

`int`

`[TRIM_MEMORY_RUNNING_LOW](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_RUNNING_LOW)`

_This constant was deprecated in API level 35. Apps are not notified of this level since API level 34_

`int`

`[TRIM_MEMORY_RUNNING_MODERATE](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_RUNNING_MODERATE)`

_This constant was deprecated in API level 35. Apps are not notified of this level since API level 34_

`int`

`[TRIM_MEMORY_UI_HIDDEN](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_UI_HIDDEN)`

Level for `[onTrimMemory(int)](/reference/android/content/ComponentCallbacks2#onTrimMemory\(int\))`: the process had been showing a user interface, and is no longer doing so.

### Fields

`protected static final int[]`

`[FOCUSED_STATE_SET](/reference/android/app/Activity#FOCUSED_STATE_SET)`

### Public constructors

`[Activity](/reference/android/app/Activity#Activity\(\))()`

### Public methods

`void`

`[addContentView](/reference/android/app/Activity#addContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))([View](/reference/android/view/View) view, [ViewGroup.LayoutParams](/reference/android/view/ViewGroup.LayoutParams) params)`

Add an additional content view to the activity.

`void`

`[clearOverrideActivityTransition](/reference/android/app/Activity#clearOverrideActivityTransition\(int\))(int overrideType)`

Clears the animations which are set from `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))`.

`void`

`[closeContextMenu](/reference/android/app/Activity#closeContextMenu\(\))()`

Programmatically closes the most recently opened context menu, if showing.

`void`

`[closeOptionsMenu](/reference/android/app/Activity#closeOptionsMenu\(\))()`

Progammatically closes the options menu.

`[PendingIntent](/reference/android/app/PendingIntent)`

`[createPendingResult](/reference/android/app/Activity#createPendingResult\(int,%20android.content.Intent,%20int\))(int requestCode, [Intent](/reference/android/content/Intent) data, int flags)`

Create a new PendingIntent object which you can hand to others for them to use to send result data back to your `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))` callback.

`final void`

`[dismissDialog](/reference/android/app/Activity#dismissDialog\(int\))(int id)`

_This method was deprecated in API level 15. Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package._

`final void`

`[dismissKeyboardShortcutsHelper](/reference/android/app/Activity#dismissKeyboardShortcutsHelper\(\))()`

Dismiss the Keyboard Shortcuts screen.

`boolean`

`[dispatchGenericMotionEvent](/reference/android/app/Activity#dispatchGenericMotionEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) ev)`

Called to process generic motion events.

`boolean`

`[dispatchKeyEvent](/reference/android/app/Activity#dispatchKeyEvent\(android.view.KeyEvent\))([KeyEvent](/reference/android/view/KeyEvent) event)`

Called to process key events.

`boolean`

`[dispatchKeyShortcutEvent](/reference/android/app/Activity#dispatchKeyShortcutEvent\(android.view.KeyEvent\))([KeyEvent](/reference/android/view/KeyEvent) event)`

Called to process a key shortcut event.

`boolean`

`[dispatchPopulateAccessibilityEvent](/reference/android/app/Activity#dispatchPopulateAccessibilityEvent\(android.view.accessibility.AccessibilityEvent\))([AccessibilityEvent](/reference/android/view/accessibility/AccessibilityEvent) event)`

Called to process population of `[AccessibilityEvent](/reference/android/view/accessibility/AccessibilityEvent)`s.

`boolean`

`[dispatchTouchEvent](/reference/android/app/Activity#dispatchTouchEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) ev)`

Called to process touch screen events.

`boolean`

`[dispatchTrackballEvent](/reference/android/app/Activity#dispatchTrackballEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) ev)`

Called to process trackball events.

`void`

`[dump](/reference/android/app/Activity#dump\(java.lang.String,%20java.io.FileDescriptor,%20java.io.PrintWriter,%20java.lang.String[]\))([String](/reference/java/lang/String) prefix, [FileDescriptor](/reference/java/io/FileDescriptor) fd, [PrintWriter](/reference/java/io/PrintWriter) writer, [String[]](/reference/java/lang/String) args)`

Print the Activity's state into the given stream.

`boolean`

`[enterPictureInPictureMode](/reference/android/app/Activity#enterPictureInPictureMode\(android.app.PictureInPictureParams\))([PictureInPictureParams](/reference/android/app/PictureInPictureParams) params)`

Puts the activity in picture-in-picture mode if possible in the current system state.

`void`

`[enterPictureInPictureMode](/reference/android/app/Activity#enterPictureInPictureMode\(\))()`

Puts the activity in picture-in-picture mode if possible in the current system state.

`<T extends [View](/reference/android/view/View)> T`

`[findViewById](/reference/android/app/Activity#findViewById\(int\))(int id)`

Finds a view that was identified by the `android:id` XML attribute that was processed in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`.

`void`

`[finish](/reference/android/app/Activity#finish\(\))()`

Call this when your activity is done and should be closed.

`void`

`[finishActivity](/reference/android/app/Activity#finishActivity\(int\))(int requestCode)`

Force finish another activity that you had previously started with `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`.

`void`

`[finishActivityFromChild](/reference/android/app/Activity#finishActivityFromChild\(android.app.Activity,%20int\))([Activity](/reference/android/app/Activity) child, int requestCode)`

_This method was deprecated in API level 30. Use `[finishActivity(int)](/reference/android/app/Activity#finishActivity\(int\))` instead._

`void`

`[finishAffinity](/reference/android/app/Activity#finishAffinity\(\))()`

Finish this activity as well as all activities immediately below it in the current task that have the same affinity.

`void`

`[finishAfterTransition](/reference/android/app/Activity#finishAfterTransition\(\))()`

Reverses the Activity Scene entry Transition and triggers the calling Activity to reverse its exit Transition.

`void`

`[finishAndRemoveTask](/reference/android/app/Activity#finishAndRemoveTask\(\))()`

Call this when your activity is done and should be closed and the task should be completely removed as a part of finishing the root activity of the task.

`void`

`[finishFromChild](/reference/android/app/Activity#finishFromChild\(android.app.Activity\))([Activity](/reference/android/app/Activity) child)`

_This method was deprecated in API level 30. Use `[finish()](/reference/android/app/Activity#finish\(\))` instead._

`[ActionBar](/reference/android/app/ActionBar)`

`[getActionBar](/reference/android/app/Activity#getActionBar\(\))()`

Retrieve a reference to this activity's ActionBar.

`final [Application](/reference/android/app/Application)`

`[getApplication](/reference/android/app/Activity#getApplication\(\))()`

Return the application that owns this activity.

`[ComponentCaller](/reference/android/app/ComponentCaller)`

`[getCaller](/reference/android/app/Activity#getCaller\(\))()`

Returns the ComponentCaller instance of the app that started this activity.

`[ComponentName](/reference/android/content/ComponentName)`

`[getCallingActivity](/reference/android/app/Activity#getCallingActivity\(\))()`

Return the name of the activity that invoked this activity.

`[String](/reference/java/lang/String)`

`[getCallingPackage](/reference/android/app/Activity#getCallingPackage\(\))()`

Return the name of the package that invoked this activity.

`int`

`[getChangingConfigurations](/reference/android/app/Activity#getChangingConfigurations\(\))()`

If this activity is being destroyed because it can not handle a configuration parameter being changed (and thus its `[onConfigurationChanged(android.content.res.Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` method is _not_ being called), then you can use this method to discover the set of changes that have occurred while in the process of being destroyed.

`[ComponentName](/reference/android/content/ComponentName)`

`[getComponentName](/reference/android/app/Activity#getComponentName\(\))()`

Returns the complete component name of this activity.

`[Scene](/reference/android/transition/Scene)`

`[getContentScene](/reference/android/app/Activity#getContentScene\(\))()`

Retrieve the `[Scene](/reference/android/transition/Scene)` representing this window's current content.

`[TransitionManager](/reference/android/transition/TransitionManager)`

`[getContentTransitionManager](/reference/android/app/Activity#getContentTransitionManager\(\))()`

Retrieve the `[TransitionManager](/reference/android/transition/TransitionManager)` responsible for default transitions in this window.

`[ComponentCaller](/reference/android/app/ComponentCaller)`

`[getCurrentCaller](/reference/android/app/Activity#getCurrentCaller\(\))()`

Returns the ComponentCaller instance of the app that re-launched this activity with a new intent via `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` or `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`.

`[View](/reference/android/view/View)`

`[getCurrentFocus](/reference/android/app/Activity#getCurrentFocus\(\))()`

Calls `[Window.getCurrentFocus()](/reference/android/view/Window#getCurrentFocus\(\))` on the Window of this Activity to return the currently focused view.

`[FragmentManager](/reference/android/app/FragmentManager)`

`[getFragmentManager](/reference/android/app/Activity#getFragmentManager\(\))()`

_This method was deprecated in API level 28. Use `[FragmentActivity.getSupportFragmentManager()](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html#getSupportFragmentManager\(\))`_

`[ComponentCaller](/reference/android/app/ComponentCaller)`

`[getInitialCaller](/reference/android/app/Activity#getInitialCaller\(\))()`

Returns the ComponentCaller instance of the app that initially launched this activity.

`[Intent](/reference/android/content/Intent)`

`[getIntent](/reference/android/app/Activity#getIntent\(\))()`

Returns the intent that started this activity.

`[Object](/reference/java/lang/Object)`

`[getLastNonConfigurationInstance](/reference/android/app/Activity#getLastNonConfigurationInstance\(\))()`

Retrieve the non-configuration instance data that was previously returned by `[onRetainNonConfigurationInstance()](/reference/android/app/Activity#onRetainNonConfigurationInstance\(\))`.

`[String](/reference/java/lang/String)`

`[getLaunchedFromPackage](/reference/android/app/Activity#getLaunchedFromPackage\(\))()`

Returns the package name of the app that initially launched this activity.

`int`

`[getLaunchedFromUid](/reference/android/app/Activity#getLaunchedFromUid\(\))()`

Returns the uid of the app that initially launched this activity.

`[LayoutInflater](/reference/android/view/LayoutInflater)`

`[getLayoutInflater](/reference/android/app/Activity#getLayoutInflater\(\))()`

Convenience for calling `[Window.getLayoutInflater()](/reference/android/view/Window#getLayoutInflater\(\))`.

`[LoaderManager](/reference/android/app/LoaderManager)`

`[getLoaderManager](/reference/android/app/Activity#getLoaderManager\(\))()`

_This method was deprecated in API level 28. Use `[FragmentActivity.getSupportLoaderManager()](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html#getSupportLoaderManager\(\))`_

`[String](/reference/java/lang/String)`

`[getLocalClassName](/reference/android/app/Activity#getLocalClassName\(\))()`

Returns class name for this activity with the package prefix removed.

`int`

`[getMaxNumPictureInPictureActions](/reference/android/app/Activity#getMaxNumPictureInPictureActions\(\))()`

Return the number of actions that will be displayed in the picture-in-picture UI when the user interacts with the activity currently in picture-in-picture mode.

`final [MediaController](/reference/android/media/session/MediaController)`

`[getMediaController](/reference/android/app/Activity#getMediaController\(\))()`

Gets the controller which should be receiving media key and volume events while this activity is in the foreground.

`[MenuInflater](/reference/android/view/MenuInflater)`

`[getMenuInflater](/reference/android/app/Activity#getMenuInflater\(\))()`

Returns a `[MenuInflater](/reference/android/view/MenuInflater)` with this context.

`[OnBackInvokedDispatcher](/reference/android/window/OnBackInvokedDispatcher)`

`[getOnBackInvokedDispatcher](/reference/android/app/Activity#getOnBackInvokedDispatcher\(\))()`

Returns the `[OnBackInvokedDispatcher](/reference/android/window/OnBackInvokedDispatcher)` instance associated with the window that this activity is attached to.

`final [Activity](/reference/android/app/Activity)`

`[getParent](/reference/android/app/Activity#getParent\(\))()`

_This method was deprecated in API level 35. `[ActivityGroup](/reference/android/app/ActivityGroup)` is deprecated._

`[Intent](/reference/android/content/Intent)`

`[getParentActivityIntent](/reference/android/app/Activity#getParentActivityIntent\(\))()`

Obtain an `[Intent](/reference/android/content/Intent)` that will launch an explicit target activity specified by this activity's logical parent.

`[SharedPreferences](/reference/android/content/SharedPreferences)`

`[getPreferences](/reference/android/app/Activity#getPreferences\(int\))(int mode)`

Retrieve a `[SharedPreferences](/reference/android/content/SharedPreferences)` object for accessing preferences that are private to this activity.

`[Uri](/reference/android/net/Uri)`

`[getReferrer](/reference/android/app/Activity#getReferrer\(\))()`

Return information about who launched this activity.

`int`

`[getRequestedOrientation](/reference/android/app/Activity#getRequestedOrientation\(\))()`

Returns the current requested orientation of the activity, which is either the orientation requested in the app manifest or the last orientation given to `[setRequestedOrientation(int)](/reference/android/app/Activity#setRequestedOrientation\(int\))`.

`final [SearchEvent](/reference/android/view/SearchEvent)`

`[getSearchEvent](/reference/android/app/Activity#getSearchEvent\(\))()`

During the onSearchRequested() callbacks, this function will return the `[SearchEvent](/reference/android/view/SearchEvent)` that triggered the callback, if it exists.

`final [SplashScreen](/reference/android/window/SplashScreen)`

`[getSplashScreen](/reference/android/app/Activity#getSplashScreen\(\))()`

Get the interface that activity use to talk to the splash screen.

`[Object](/reference/java/lang/Object)`

`[getSystemService](/reference/android/app/Activity#getSystemService\(java.lang.String\))([String](/reference/java/lang/String) name)`

Return the handle to a system-level service by name.

`int`

`[getTaskId](/reference/android/app/Activity#getTaskId\(\))()`

Return the identifier of the task this activity is in.

`final [CharSequence](/reference/java/lang/CharSequence)`

`[getTitle](/reference/android/app/Activity#getTitle\(\))()`

`final int`

`[getTitleColor](/reference/android/app/Activity#getTitleColor\(\))()`

`[VoiceInteractor](/reference/android/app/VoiceInteractor)`

`[getVoiceInteractor](/reference/android/app/Activity#getVoiceInteractor\(\))()`

Retrieve the active `[VoiceInteractor](/reference/android/app/VoiceInteractor)` that the user is going through to interact with this activity.

`final int`

`[getVolumeControlStream](/reference/android/app/Activity#getVolumeControlStream\(\))()`

Gets the suggested audio stream whose volume should be changed by the hardware volume controls.

`[Window](/reference/android/view/Window)`

`[getWindow](/reference/android/app/Activity#getWindow\(\))()`

Retrieve the current `[Window](/reference/android/view/Window)` for the activity.

`[WindowManager](/reference/android/view/WindowManager)`

`[getWindowManager](/reference/android/app/Activity#getWindowManager\(\))()`

Retrieve the window manager for showing custom windows.

`boolean`

`[hasWindowFocus](/reference/android/app/Activity#hasWindowFocus\(\))()`

Returns true if this activity's _main_ window currently has window focus.

`void`

`[invalidateOptionsMenu](/reference/android/app/Activity#invalidateOptionsMenu\(\))()`

Declare that the options menu has changed, so should be recreated.

`boolean`

`[isActivityTransitionRunning](/reference/android/app/Activity#isActivityTransitionRunning\(\))()`

Returns whether there are any activity transitions currently running on this activity.

`boolean`

`[isChangingConfigurations](/reference/android/app/Activity#isChangingConfigurations\(\))()`

Check to see whether this activity is in the process of being destroyed in order to be recreated with a new configuration.

`final boolean`

`[isChild](/reference/android/app/Activity#isChild\(\))()`

_This method was deprecated in API level 35. `[ActivityGroup](/reference/android/app/ActivityGroup)` is deprecated._

`boolean`

`[isDestroyed](/reference/android/app/Activity#isDestroyed\(\))()`

Returns true if the final `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` call has been made on the Activity, so this instance is now dead.

`boolean`

`[isFinishing](/reference/android/app/Activity#isFinishing\(\))()`

Check to see whether this activity is in the process of finishing, either because you called `[finish()](/reference/android/app/Activity#finish\(\))` on it or someone else has requested that it finished.

`boolean`

`[isImmersive](/reference/android/app/Activity#isImmersive\(\))()`

Bit indicating that this activity is "immersive" and should not be interrupted by notifications if possible.

`boolean`

`[isInMultiWindowMode](/reference/android/app/Activity#isInMultiWindowMode\(\))()`

Returns true if the activity is currently in multi-window mode.

`boolean`

`[isInPictureInPictureMode](/reference/android/app/Activity#isInPictureInPictureMode\(\))()`

Returns true if the activity is currently in picture-in-picture mode.

`boolean`

`[isLaunchedFromBubble](/reference/android/app/Activity#isLaunchedFromBubble\(\))()`

Indicates whether this activity is launched from a bubble.

`boolean`

`[isLocalVoiceInteractionSupported](/reference/android/app/Activity#isLocalVoiceInteractionSupported\(\))()`

Queries whether the currently enabled voice interaction service supports returning a voice interactor for use by the activity.

`boolean`

`[isTaskRoot](/reference/android/app/Activity#isTaskRoot\(\))()`

Return whether this activity is the root of a task.

`boolean`

`[isVoiceInteraction](/reference/android/app/Activity#isVoiceInteraction\(\))()`

Check whether this activity is running as part of a voice interaction with the user.

`boolean`

`[isVoiceInteractionRoot](/reference/android/app/Activity#isVoiceInteractionRoot\(\))()`

Like `[isVoiceInteraction()](/reference/android/app/Activity#isVoiceInteraction\(\))`, but only returns `true` if this is also the root of a voice interaction.

`final [Cursor](/reference/android/database/Cursor)`

`[managedQuery](/reference/android/app/Activity#managedQuery\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, [String[]](/reference/java/lang/String) projection, [String](/reference/java/lang/String) selection, [String[]](/reference/java/lang/String) selectionArgs, [String](/reference/java/lang/String) sortOrder)`

_This method was deprecated in API level 15. Use `[CursorLoader](/reference/android/content/CursorLoader)` instead._

`boolean`

`[moveTaskToBack](/reference/android/app/Activity#moveTaskToBack\(boolean\))(boolean nonRoot)`

Move the task containing this activity to the back of the activity stack.

`boolean`

`[navigateUpTo](/reference/android/app/Activity#navigateUpTo\(android.content.Intent\))([Intent](/reference/android/content/Intent) upIntent)`

Navigate from this activity to the activity specified by upIntent, finishing this activity in the process.

`boolean`

`[navigateUpToFromChild](/reference/android/app/Activity#navigateUpToFromChild\(android.app.Activity,%20android.content.Intent\))([Activity](/reference/android/app/Activity) child, [Intent](/reference/android/content/Intent) upIntent)`

_This method was deprecated in API level 30. Use `[navigateUpTo(android.content.Intent)](/reference/android/app/Activity#navigateUpTo\(android.content.Intent\))` instead._

`void`

`[onActionModeFinished](/reference/android/app/Activity#onActionModeFinished\(android.view.ActionMode\))([ActionMode](/reference/android/view/ActionMode) mode)`

Notifies the activity that an action mode has finished.

`void`

`[onActionModeStarted](/reference/android/app/Activity#onActionModeStarted\(android.view.ActionMode\))([ActionMode](/reference/android/view/ActionMode) mode)`

Notifies the Activity that an action mode has been started.

`void`

`[onActivityReenter](/reference/android/app/Activity#onActivityReenter\(int,%20android.content.Intent\))(int resultCode, [Intent](/reference/android/content/Intent) data)`

Called when an activity you launched with an activity transition exposes this Activity through a returning activity transition, giving you the resultCode and any additional data from it.

`void`

`[onActivityResult](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent,%20android.app.ComponentCaller\))(int requestCode, int resultCode, [Intent](/reference/android/content/Intent) data, [ComponentCaller](/reference/android/app/ComponentCaller) caller)`

Same as `[onActivityResult(int, int, android.content.Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`, but with an extra parameter for the ComponentCaller instance associated with the app that sent the result.

`void`

`[onAttachFragment](/reference/android/app/Activity#onAttachFragment\(android.app.Fragment\))([Fragment](/reference/android/app/Fragment) fragment)`

_This method was deprecated in API level 28. Use `[FragmentActivity.onAttachFragment(androidx.fragment.app.Fragment)](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html#onAttachFragment\(androidx.fragment.app.Fragment\))`_

`void`

`[onAttachedToWindow](/reference/android/app/Activity#onAttachedToWindow\(\))()`

Called when the main window associated with the activity has been attached to the window manager.

`void`

`[onBackPressed](/reference/android/app/Activity#onBackPressed\(\))()`

_This method was deprecated in API level 33. Use `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)` or `androidx.activity.OnBackPressedCallback` to handle back navigation instead._

_Starting from Android 13 (API level 33), back event handling is moving to an ahead-of-time model and `[Activity.onBackPressed()](/reference/android/app/Activity#onBackPressed\(\))` and `[KeyEvent.KEYCODE_BACK](/reference/android/view/KeyEvent#KEYCODE_BACK)` should not be used to handle back events (back gesture or back button click). Instead, an `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)` should be registered using `[Activity.getOnBackInvokedDispatcher()](/reference/android/app/Activity#getOnBackInvokedDispatcher\(\))` `[.registerOnBackInvokedCallback(priority, callback)](/reference/android/window/OnBackInvokedDispatcher#registerOnBackInvokedCallback\(int,%20android.window.OnBackInvokedCallback\))`._

`void`

`[onConfigurationChanged](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))([Configuration](/reference/android/content/res/Configuration) newConfig)`

Called by the system when the device configuration changes while your activity is running.

`void`

`[onContentChanged](/reference/android/app/Activity#onContentChanged\(\))()`

This hook is called whenever the content view of the screen changes (due to a call to `[Window.setContentView](/reference/android/view/Window#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))` or `[Window.addContentView](/reference/android/view/Window#addContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))`).

`boolean`

`[onContextItemSelected](/reference/android/app/Activity#onContextItemSelected\(android.view.MenuItem\))([MenuItem](/reference/android/view/MenuItem) item)`

This hook is called whenever an item in a context menu is selected.

`void`

`[onContextMenuClosed](/reference/android/app/Activity#onContextMenuClosed\(android.view.Menu\))([Menu](/reference/android/view/Menu) menu)`

This hook is called whenever the context menu is being closed (either by the user canceling the menu with the back/menu button, or when an item is selected).

`void`

`[onCreate](/reference/android/app/Activity#onCreate\(android.os.Bundle,%20android.os.PersistableBundle\))([Bundle](/reference/android/os/Bundle) savedInstanceState, [PersistableBundle](/reference/android/os/PersistableBundle) persistentState)`

Same as `[onCreate(android.os.Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` but called for those activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`.

`void`

`[onCreateContextMenu](/reference/android/app/Activity#onCreateContextMenu\(android.view.ContextMenu,%20android.view.View,%20android.view.ContextMenu.ContextMenuInfo\))([ContextMenu](/reference/android/view/ContextMenu) menu, [View](/reference/android/view/View) v, [ContextMenu.ContextMenuInfo](/reference/android/view/ContextMenu.ContextMenuInfo) menuInfo)`

Called when a context menu for the `view` is about to be shown.

`[CharSequence](/reference/java/lang/CharSequence)`

`[onCreateDescription](/reference/android/app/Activity#onCreateDescription\(\))()`

Generate a new description for this activity.

`void`

`[onCreateNavigateUpTaskStack](/reference/android/app/Activity#onCreateNavigateUpTaskStack\(android.app.TaskStackBuilder\))([TaskStackBuilder](/reference/android/app/TaskStackBuilder) builder)`

Define the synthetic task stack that will be generated during Up navigation from a different task.

`boolean`

`[onCreateOptionsMenu](/reference/android/app/Activity#onCreateOptionsMenu\(android.view.Menu\))([Menu](/reference/android/view/Menu) menu)`

Initialize the contents of the Activity's standard options menu.

`boolean`

`[onCreatePanelMenu](/reference/android/app/Activity#onCreatePanelMenu\(int,%20android.view.Menu\))(int featureId, [Menu](/reference/android/view/Menu) menu)`

Default implementation of `[Window.Callback.onCreatePanelMenu(int, Menu)](/reference/android/view/Window.Callback#onCreatePanelMenu\(int,%20android.view.Menu\))` for activities.

`[View](/reference/android/view/View)`

`[onCreatePanelView](/reference/android/app/Activity#onCreatePanelView\(int\))(int featureId)`

Default implementation of `[Window.Callback.onCreatePanelView(int)](/reference/android/view/Window.Callback#onCreatePanelView\(int\))` for activities.

`boolean`

`[onCreateThumbnail](/reference/android/app/Activity#onCreateThumbnail\(android.graphics.Bitmap,%20android.graphics.Canvas\))([Bitmap](/reference/android/graphics/Bitmap) outBitmap, [Canvas](/reference/android/graphics/Canvas) canvas)`

_This method was deprecated in API level 28. Method doesn't do anything and will be removed in the future._

`[View](/reference/android/view/View)`

`[onCreateView](/reference/android/app/Activity#onCreateView\(android.view.View,%20java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))([View](/reference/android/view/View) parent, [String](/reference/java/lang/String) name, [Context](/reference/android/content/Context) context, [AttributeSet](/reference/android/util/AttributeSet) attrs)`

Standard implementation of `[LayoutInflater.Factory2.onCreateView(View, String, Context, AttributeSet)](/reference/android/view/LayoutInflater.Factory2#onCreateView\(android.view.View,%20java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))` used when inflating with the LayoutInflater returned by `[Context.getSystemService(Class)](/reference/android/content/Context#getSystemService\(java.lang.Class<T>\))`.

`[View](/reference/android/view/View)`

`[onCreateView](/reference/android/app/Activity#onCreateView\(java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))([String](/reference/java/lang/String) name, [Context](/reference/android/content/Context) context, [AttributeSet](/reference/android/util/AttributeSet) attrs)`

Standard implementation of `[LayoutInflater.Factory.onCreateView(String, Context, AttributeSet)](/reference/android/view/LayoutInflater.Factory#onCreateView\(java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))` used when inflating with the LayoutInflater returned by `[Context.getSystemService(Class)](/reference/android/content/Context#getSystemService\(java.lang.Class<T>\))`.

`void`

`[onDetachedFromWindow](/reference/android/app/Activity#onDetachedFromWindow\(\))()`

Called when the main window associated with the activity has been detached from the window manager.

`void`

`[onEnterAnimationComplete](/reference/android/app/Activity#onEnterAnimationComplete\(\))()`

Activities cannot draw during the period that their windows are animating in.

`boolean`

`[onGenericMotionEvent](/reference/android/app/Activity#onGenericMotionEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) event)`

Called when a generic motion event was not handled by any of the views inside of the activity.

`void`

`[onGetDirectActions](/reference/android/app/Activity#onGetDirectActions\(android.os.CancellationSignal,%20java.util.function.Consumer<java.util.List<android.app.DirectAction>>\))([CancellationSignal](/reference/android/os/CancellationSignal) cancellationSignal, [Consumer](/reference/java/util/function/Consumer)<[List](/reference/java/util/List)<[DirectAction](/reference/android/app/DirectAction)>> callback)`

Returns the list of direct actions supported by the app.

`boolean`

`[onKeyDown](/reference/android/app/Activity#onKeyDown\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a key was pressed down and not handled by any of the views inside of the activity.

`boolean`

`[onKeyLongPress](/reference/android/app/Activity#onKeyLongPress\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Default implementation of `[KeyEvent.Callback.onKeyLongPress()](/reference/android/view/KeyEvent.Callback#onKeyLongPress\(int,%20android.view.KeyEvent\))`: always returns false (doesn't handle the event).

`boolean`

`[onKeyMultiple](/reference/android/app/Activity#onKeyMultiple\(int,%20int,%20android.view.KeyEvent\))(int keyCode, int repeatCount, [KeyEvent](/reference/android/view/KeyEvent) event)`

Default implementation of `[KeyEvent.Callback.onKeyMultiple()](/reference/android/view/KeyEvent.Callback#onKeyMultiple\(int,%20int,%20android.view.KeyEvent\))`: always returns false (doesn't handle the event).

`boolean`

`[onKeyShortcut](/reference/android/app/Activity#onKeyShortcut\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a key shortcut event is not handled by any of the views in the Activity.

`boolean`

`[onKeyUp](/reference/android/app/Activity#onKeyUp\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a key was released and not handled by any of the views inside of the activity.

`void`

`[onLocalVoiceInteractionStarted](/reference/android/app/Activity#onLocalVoiceInteractionStarted\(\))()`

Callback to indicate that `[startLocalVoiceInteraction(android.os.Bundle)](/reference/android/app/Activity#startLocalVoiceInteraction\(android.os.Bundle\))` has resulted in a voice interaction session being started.

`void`

`[onLocalVoiceInteractionStopped](/reference/android/app/Activity#onLocalVoiceInteractionStopped\(\))()`

Callback to indicate that the local voice interaction has stopped either because it was requested through a call to `[stopLocalVoiceInteraction()](/reference/android/app/Activity#stopLocalVoiceInteraction\(\))` or because it was canceled by the user.

`void`

`[onLowMemory](/reference/android/app/Activity#onLowMemory\(\))()`

This is called when the overall system is running low on memory, and actively running processes should trim their memory usage.

`boolean`

`[onMenuItemSelected](/reference/android/app/Activity#onMenuItemSelected\(int,%20android.view.MenuItem\))(int featureId, [MenuItem](/reference/android/view/MenuItem) item)`

Default implementation of `[Window.Callback.onMenuItemSelected(int, MenuItem)](/reference/android/view/Window.Callback#onMenuItemSelected\(int,%20android.view.MenuItem\))` for activities.

`boolean`

`[onMenuOpened](/reference/android/app/Activity#onMenuOpened\(int,%20android.view.Menu\))(int featureId, [Menu](/reference/android/view/Menu) menu)`

Called when a panel's menu is opened by the user.

`void`

`[onMultiWindowModeChanged](/reference/android/app/Activity#onMultiWindowModeChanged\(boolean\))(boolean isInMultiWindowMode)`

_This method was deprecated in API level 26. Use `[onMultiWindowModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onMultiWindowModeChanged\(boolean,%20android.content.res.Configuration\))` instead._

`void`

`[onMultiWindowModeChanged](/reference/android/app/Activity#onMultiWindowModeChanged\(boolean,%20android.content.res.Configuration\))(boolean isInMultiWindowMode, [Configuration](/reference/android/content/res/Configuration) newConfig)`

Called by the system when the activity changes from fullscreen mode to multi-window mode and visa-versa.

`boolean`

`[onNavigateUp](/reference/android/app/Activity#onNavigateUp\(\))()`

This method is called whenever the user chooses to navigate Up within your application's activity hierarchy from the action bar.

`boolean`

`[onNavigateUpFromChild](/reference/android/app/Activity#onNavigateUpFromChild\(android.app.Activity\))([Activity](/reference/android/app/Activity) child)`

_This method was deprecated in API level 30. Use `[onNavigateUp()](/reference/android/app/Activity#onNavigateUp\(\))` instead._

`void`

`[onNewIntent](/reference/android/app/Activity#onNewIntent\(android.content.Intent,%20android.app.ComponentCaller\))([Intent](/reference/android/content/Intent) intent, [ComponentCaller](/reference/android/app/ComponentCaller) caller)`

Same as `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, but with an extra parameter for the ComponentCaller instance associated with the app that sent the intent.

`boolean`

`[onOptionsItemSelected](/reference/android/app/Activity#onOptionsItemSelected\(android.view.MenuItem\))([MenuItem](/reference/android/view/MenuItem) item)`

This hook is called whenever an item in your options menu is selected.

`void`

`[onOptionsMenuClosed](/reference/android/app/Activity#onOptionsMenuClosed\(android.view.Menu\))([Menu](/reference/android/view/Menu) menu)`

This hook is called whenever the options menu is being closed (either by the user canceling the menu with the back/menu button, or when an item is selected).

`void`

`[onPanelClosed](/reference/android/app/Activity#onPanelClosed\(int,%20android.view.Menu\))(int featureId, [Menu](/reference/android/view/Menu) menu)`

Default implementation of `[Window.Callback.onPanelClosed(int, Menu)](/reference/android/view/Window.Callback#onPanelClosed\(int,%20android.view.Menu\))` for activities.

`void`

`[onPerformDirectAction](/reference/android/app/Activity#onPerformDirectAction\(java.lang.String,%20android.os.Bundle,%20android.os.CancellationSignal,%20java.util.function.Consumer<android.os.Bundle>\))([String](/reference/java/lang/String) actionId, [Bundle](/reference/android/os/Bundle) arguments, [CancellationSignal](/reference/android/os/CancellationSignal) cancellationSignal, [Consumer](/reference/java/util/function/Consumer)<[Bundle](/reference/android/os/Bundle)> resultListener)`

This is called to perform an action previously defined by the app.

`void`

`[onPictureInPictureModeChanged](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))(boolean isInPictureInPictureMode, [Configuration](/reference/android/content/res/Configuration) newConfig)`

Called by the system when the activity changes to and from picture-in-picture mode.

`void`

`[onPictureInPictureModeChanged](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean\))(boolean isInPictureInPictureMode)`

_This method was deprecated in API level 26. Use `[onPictureInPictureModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))` instead._

`boolean`

`[onPictureInPictureRequested](/reference/android/app/Activity#onPictureInPictureRequested\(\))()`

This method is called by the system in various cases where picture in picture mode should be entered if supported.

`void`

`[onPictureInPictureUiStateChanged](/reference/android/app/Activity#onPictureInPictureUiStateChanged\(android.app.PictureInPictureUiState\))([PictureInPictureUiState](/reference/android/app/PictureInPictureUiState) pipState)`

Called by the system when the activity is in PiP and has state changes.

`void`

`[onPostCreate](/reference/android/app/Activity#onPostCreate\(android.os.Bundle,%20android.os.PersistableBundle\))([Bundle](/reference/android/os/Bundle) savedInstanceState, [PersistableBundle](/reference/android/os/PersistableBundle) persistentState)`

This is the same as `[onPostCreate(android.os.Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))` but is called for activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`.

`void`

`[onPrepareNavigateUpTaskStack](/reference/android/app/Activity#onPrepareNavigateUpTaskStack\(android.app.TaskStackBuilder\))([TaskStackBuilder](/reference/android/app/TaskStackBuilder) builder)`

Prepare the synthetic task stack that will be generated during Up navigation from a different task.

`boolean`

`[onPrepareOptionsMenu](/reference/android/app/Activity#onPrepareOptionsMenu\(android.view.Menu\))([Menu](/reference/android/view/Menu) menu)`

Prepare the Screen's standard options menu to be displayed.

`boolean`

`[onPreparePanel](/reference/android/app/Activity#onPreparePanel\(int,%20android.view.View,%20android.view.Menu\))(int featureId, [View](/reference/android/view/View) view, [Menu](/reference/android/view/Menu) menu)`

Default implementation of `[Window.Callback.onPreparePanel(int, View, Menu)](/reference/android/view/Window.Callback#onPreparePanel\(int,%20android.view.View,%20android.view.Menu\))` for activities.

`void`

`[onProvideAssistContent](/reference/android/app/Activity#onProvideAssistContent\(android.app.assist.AssistContent\))([AssistContent](/reference/android/app/assist/AssistContent) outContent)`

This is called when the user is requesting an assist, to provide references to content related to the current activity.

`void`

`[onProvideAssistData](/reference/android/app/Activity#onProvideAssistData\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) data)`

This is called when the user is requesting an assist, to build a full `[Intent.ACTION_ASSIST](/reference/android/content/Intent#ACTION_ASSIST)` Intent with all of the context of the current application.

`void`

`[onProvideKeyboardShortcuts](/reference/android/app/Activity#onProvideKeyboardShortcuts\(java.util.List<android.view.KeyboardShortcutGroup>,%20android.view.Menu,%20int\))([List](/reference/java/util/List)<[KeyboardShortcutGroup](/reference/android/view/KeyboardShortcutGroup)> data, [Menu](/reference/android/view/Menu) menu, int deviceId)`

Called when Keyboard Shortcuts are requested for the current window.

`[Uri](/reference/android/net/Uri)`

`[onProvideReferrer](/reference/android/app/Activity#onProvideReferrer\(\))()`

Override to generate the desired referrer for the content currently being shown by the app.

`void`

`[onRequestPermissionsResult](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))(int requestCode, [String[]](/reference/java/lang/String) permissions, int[] grantResults)`

Callback for the result from requesting permissions.

`void`

`[onRequestPermissionsResult](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[],%20int\))(int requestCode, [String[]](/reference/java/lang/String) permissions, int[] grantResults, int deviceId)`

Callback for the result from requesting permissions.

`void`

`[onRestoreInstanceState](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle,%20android.os.PersistableBundle\))([Bundle](/reference/android/os/Bundle) savedInstanceState, [PersistableBundle](/reference/android/os/PersistableBundle) persistentState)`

This is the same as `[onRestoreInstanceState(android.os.Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` but is called for activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`.

`[Object](/reference/java/lang/Object)`

`[onRetainNonConfigurationInstance](/reference/android/app/Activity#onRetainNonConfigurationInstance\(\))()`

Called by the system, as part of destroying an activity due to a configuration change, when it is known that a new instance will immediately be created for the new configuration.

`void`

`[onSaveInstanceState](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle,%20android.os.PersistableBundle\))([Bundle](/reference/android/os/Bundle) outState, [PersistableBundle](/reference/android/os/PersistableBundle) outPersistentState)`

This is the same as `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` but is called for activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`.

`boolean`

`[onSearchRequested](/reference/android/app/Activity#onSearchRequested\(android.view.SearchEvent\))([SearchEvent](/reference/android/view/SearchEvent) searchEvent)`

This hook is called when the user signals the desire to start a search.

`boolean`

`[onSearchRequested](/reference/android/app/Activity#onSearchRequested\(\))()`

Called when the user signals the desire to start a search.

`void`

`[onStateNotSaved](/reference/android/app/Activity#onStateNotSaved\(\))()`

_This method was deprecated in API level 29. starting with `[Build.VERSION_CODES.P](/reference/android/os/Build.VERSION_CODES#P)` onSaveInstanceState is called after `[onStop()](/reference/android/app/Activity#onStop\(\))`, so this hint isn't accurate anymore: you should consider your state not saved in between `onStart` and `onStop` callbacks inclusively._

`void`

`[onTopResumedActivityChanged](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))(boolean isTopResumedActivity)`

Called when activity gets or loses the top resumed position in the system.

`boolean`

`[onTouchEvent](/reference/android/app/Activity#onTouchEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) event)`

Called when a touch screen event was not handled by any of the views inside of the activity.

`boolean`

`[onTrackballEvent](/reference/android/app/Activity#onTrackballEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) event)`

Called when the trackball was moved and not handled by any of the views inside of the activity.

`void`

`[onTrimMemory](/reference/android/app/Activity#onTrimMemory\(int\))(int level)`

Called when the operating system has determined that it is a good time for a process to trim unneeded memory from its process.

`void`

`[onUserInteraction](/reference/android/app/Activity#onUserInteraction\(\))()`

Called whenever a key, touch, or trackball event is dispatched to the activity.

`void`

`[onVisibleBehindCanceled](/reference/android/app/Activity#onVisibleBehindCanceled\(\))()`

_This method was deprecated in API level 26. This method's functionality is no longer supported as of `[Build.VERSION_CODES.O](/reference/android/os/Build.VERSION_CODES#O)` and will be removed in a future release._

`void`

`[onWindowAttributesChanged](/reference/android/app/Activity#onWindowAttributesChanged\(android.view.WindowManager.LayoutParams\))([WindowManager.LayoutParams](/reference/android/view/WindowManager.LayoutParams) params)`

This is called whenever the current window attributes change.

`void`

`[onWindowFocusChanged](/reference/android/app/Activity#onWindowFocusChanged\(boolean\))(boolean hasFocus)`

Called when the current `[Window](/reference/android/view/Window)` of the activity gains or loses focus.

`[ActionMode](/reference/android/view/ActionMode)`

`[onWindowStartingActionMode](/reference/android/app/Activity#onWindowStartingActionMode\(android.view.ActionMode.Callback,%20int\))([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback, int type)`

Called when an action mode is being started for this window.

`[ActionMode](/reference/android/view/ActionMode)`

`[onWindowStartingActionMode](/reference/android/app/Activity#onWindowStartingActionMode\(android.view.ActionMode.Callback\))([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback)`

Give the Activity a chance to control the UI for an action mode requested by the system.

`void`

`[openContextMenu](/reference/android/app/Activity#openContextMenu\(android.view.View\))([View](/reference/android/view/View) view)`

Programmatically opens the context menu for a particular `view`.

`void`

`[openOptionsMenu](/reference/android/app/Activity#openOptionsMenu\(\))()`

Programmatically opens the options menu.

`void`

`[overrideActivityTransition](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))(int overrideType, int enterAnim, int exitAnim, int backgroundColor)`

Customizes the animation and background color for activity transitions.

`void`

`[overrideActivityTransition](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))(int overrideType, int enterAnim, int exitAnim)`

Customizes the animation for activity transitions.

`void`

`[overridePendingTransition](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))(int enterAnim, int exitAnim)`

_This method was deprecated in API level 34. Use `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))`} instead._

`void`

`[overridePendingTransition](/reference/android/app/Activity#overridePendingTransition\(int,%20int,%20int\))(int enterAnim, int exitAnim, int backgroundColor)`

_This method was deprecated in API level 34. Use `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`} instead._

`void`

`[postponeEnterTransition](/reference/android/app/Activity#postponeEnterTransition\(\))()`

Postpone the entering activity transition when Activity was started with `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`.

`void`

`[recreate](/reference/android/app/Activity#recreate\(\))()`

Cause this Activity to be recreated with a new instance.

`void`

`[registerActivityLifecycleCallbacks](/reference/android/app/Activity#registerActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))([Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks) callback)`

Register an `[Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks)` instance that receives lifecycle callbacks for only this Activity.

`void`

`[registerComponentCallbacks](/reference/android/app/Activity#registerComponentCallbacks\(android.content.ComponentCallbacks\))([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)`

Add a new `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` to the base application of the Context, which will be called at the same times as the ComponentCallbacks methods of activities and other components are called.

`void`

`[registerForContextMenu](/reference/android/app/Activity#registerForContextMenu\(android.view.View\))([View](/reference/android/view/View) view)`

Registers a context menu to be shown for the given view (multiple views can show the context menu).

`void`

`[registerScreenCaptureCallback](/reference/android/app/Activity#registerScreenCaptureCallback\(java.util.concurrent.Executor,%20android.app.Activity.ScreenCaptureCallback\))([Executor](/reference/java/util/concurrent/Executor) executor, [Activity.ScreenCaptureCallback](/reference/android/app/Activity.ScreenCaptureCallback) callback)`

Registers a screen capture callback for this activity.

`boolean`

`[releaseInstance](/reference/android/app/Activity#releaseInstance\(\))()`

Ask that the local app instance of this activity be released to free up its memory.

`final void`

`[removeDialog](/reference/android/app/Activity#removeDialog\(int\))(int id)`

_This method was deprecated in API level 15. Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package._

`void`

`[reportFullyDrawn](/reference/android/app/Activity#reportFullyDrawn\(\))()`

Report to the system that your app is now fully drawn, for diagnostic and optimization purposes.

`[DragAndDropPermissions](/reference/android/view/DragAndDropPermissions)`

`[requestDragAndDropPermissions](/reference/android/app/Activity#requestDragAndDropPermissions\(android.view.DragEvent\))([DragEvent](/reference/android/view/DragEvent) event)`

Create `[DragAndDropPermissions](/reference/android/view/DragAndDropPermissions)` object bound to this activity and controlling the access permissions for content URIs associated with the `[DragEvent](/reference/android/view/DragEvent)`.

`void`

`[requestFullscreenMode](/reference/android/app/Activity#requestFullscreenMode\(int,%20android.os.OutcomeReceiver<java.lang.Void,java.lang.Throwable>\))(int request, [OutcomeReceiver](/reference/android/os/OutcomeReceiver)<[Void](/reference/java/lang/Void), [Throwable](/reference/java/lang/Throwable)> approvalCallback)`

Request to put the activity into fullscreen.

`final void`

`[requestOpenInBrowserEducation](/reference/android/app/Activity#requestOpenInBrowserEducation\(\))()`

Requests to show the \\u201cOpen in browser\\u201d education.

`final void`

`[requestPermissions](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int,%20int\))([String[]](/reference/java/lang/String) permissions, int requestCode, int deviceId)`

Requests permissions to be granted to this application.

`final void`

`[requestPermissions](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))([String[]](/reference/java/lang/String) permissions, int requestCode)`

Requests permissions to be granted to this application.

`final void`

`[requestShowKeyboardShortcuts](/reference/android/app/Activity#requestShowKeyboardShortcuts\(\))()`

Request the Keyboard Shortcuts screen to show up.

`boolean`

`[requestVisibleBehind](/reference/android/app/Activity#requestVisibleBehind\(boolean\))(boolean visible)`

_This method was deprecated in API level 26. This method's functionality is no longer supported as of `[Build.VERSION_CODES.O](/reference/android/os/Build.VERSION_CODES#O)` and will be removed in a future release._

`final boolean`

`[requestWindowFeature](/reference/android/app/Activity#requestWindowFeature\(int\))(int featureId)`

Enable extended window features.

`final <T extends [View](/reference/android/view/View)> T`

`[requireViewById](/reference/android/app/Activity#requireViewById\(int\))(int id)`

Finds a view that was identified by the `android:id` XML attribute that was processed in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`, or throws an IllegalArgumentException if the ID is invalid, or there is no matching view in the hierarchy.

`final void`

`[runOnUiThread](/reference/android/app/Activity#runOnUiThread\(java.lang.Runnable\))([Runnable](/reference/java/lang/Runnable) action)`

Runs the specified action on the UI thread.

`void`

`[setActionBar](/reference/android/app/Activity#setActionBar\(android.widget.Toolbar\))([Toolbar](/reference/android/widget/Toolbar) toolbar)`

Set a `[Toolbar](/reference/android/widget/Toolbar)` to act as the `[ActionBar](/reference/android/app/ActionBar)` for this Activity window.

`void`

`[setAllowCrossUidActivitySwitchFromBelow](/reference/android/app/Activity#setAllowCrossUidActivitySwitchFromBelow\(boolean\))(boolean allowed)`

Specifies whether the activities below this one in the task can also start other activities or finish the task.

`void`

`[setContentTransitionManager](/reference/android/app/Activity#setContentTransitionManager\(android.transition.TransitionManager\))([TransitionManager](/reference/android/transition/TransitionManager) tm)`

Set the `[TransitionManager](/reference/android/transition/TransitionManager)` to use for default transitions in this window.

`void`

`[setContentView](/reference/android/app/Activity#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))([View](/reference/android/view/View) view, [ViewGroup.LayoutParams](/reference/android/view/ViewGroup.LayoutParams) params)`

Set the activity content to an explicit view.

`void`

`[setContentView](/reference/android/app/Activity#setContentView\(android.view.View\))([View](/reference/android/view/View) view)`

Set the activity content to an explicit view.

`void`

`[setContentView](/reference/android/app/Activity#setContentView\(int\))(int layoutResID)`

Set the activity content from a layout resource.

`final void`

`[setDefaultKeyMode](/reference/android/app/Activity#setDefaultKeyMode\(int\))(int mode)`

Select the default key handling for this activity.

`void`

`[setEnterSharedElementCallback](/reference/android/app/Activity#setEnterSharedElementCallback\(android.app.SharedElementCallback\))([SharedElementCallback](/reference/android/app/SharedElementCallback) callback)`

When `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.view.View, String)](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.view.View,%20java.lang.String\))` was used to start an Activity, callback will be called to handle shared elements on the _launched_ Activity.

`void`

`[setExitSharedElementCallback](/reference/android/app/Activity#setExitSharedElementCallback\(android.app.SharedElementCallback\))([SharedElementCallback](/reference/android/app/SharedElementCallback) callback)`

When `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.view.View, String)](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.view.View,%20java.lang.String\))` was used to start an Activity, callback will be called to handle shared elements on the _launching_ Activity.

`final void`

`[setFeatureDrawable](/reference/android/app/Activity#setFeatureDrawable\(int,%20android.graphics.drawable.Drawable\))(int featureId, [Drawable](/reference/android/graphics/drawable/Drawable) drawable)`

Convenience for calling `[Window.setFeatureDrawable(int, Drawable)](/reference/android/view/Window#setFeatureDrawable\(int,%20android.graphics.drawable.Drawable\))`.

`final void`

`[setFeatureDrawableAlpha](/reference/android/app/Activity#setFeatureDrawableAlpha\(int,%20int\))(int featureId, int alpha)`

Convenience for calling `[Window.setFeatureDrawableAlpha(int, int)](/reference/android/view/Window#setFeatureDrawableAlpha\(int,%20int\))`.

`final void`

`[setFeatureDrawableResource](/reference/android/app/Activity#setFeatureDrawableResource\(int,%20int\))(int featureId, int resId)`

Convenience for calling `[Window.setFeatureDrawableResource(int, int)](/reference/android/view/Window#setFeatureDrawableResource\(int,%20int\))`.

`final void`

`[setFeatureDrawableUri](/reference/android/app/Activity#setFeatureDrawableUri\(int,%20android.net.Uri\))(int featureId, [Uri](/reference/android/net/Uri) uri)`

Convenience for calling `[Window.setFeatureDrawableUri(int, Uri)](/reference/android/view/Window#setFeatureDrawableUri\(int,%20android.net.Uri\))`.

`void`

`[setFinishOnTouchOutside](/reference/android/app/Activity#setFinishOnTouchOutside\(boolean\))(boolean finish)`

Sets whether this activity is finished when touched outside its window's bounds.

`void`

`[setImmersive](/reference/android/app/Activity#setImmersive\(boolean\))(boolean i)`

Adjust the current immersive mode setting.

`void`

`[setInheritShowWhenLocked](/reference/android/app/Activity#setInheritShowWhenLocked\(boolean\))(boolean inheritShowWhenLocked)`

Specifies whether this `[Activity](/reference/android/app/Activity)` should be shown on top of the lock screen whenever the lockscreen is up and this activity has another activity behind it with the showWhenLock attribute set.

`void`

`[setIntent](/reference/android/app/Activity#setIntent\(android.content.Intent\))([Intent](/reference/android/content/Intent) newIntent)`

Changes the intent returned by `[getIntent()](/reference/android/app/Activity#getIntent\(\))`.

`void`

`[setIntent](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))([Intent](/reference/android/content/Intent) newIntent, [ComponentCaller](/reference/android/app/ComponentCaller) newCaller)`

Changes the intent returned by `[getIntent()](/reference/android/app/Activity#getIntent\(\))`, and ComponentCaller returned by `[getCaller()](/reference/android/app/Activity#getCaller\(\))`.

`void`

`[setLocusContext](/reference/android/app/Activity#setLocusContext\(android.content.LocusId,%20android.os.Bundle\))([LocusId](/reference/android/content/LocusId) locusId, [Bundle](/reference/android/os/Bundle) bundle)`

Sets the `[LocusId](/reference/android/content/LocusId)` for this activity.

`final void`

`[setMediaController](/reference/android/app/Activity#setMediaController\(android.media.session.MediaController\))([MediaController](/reference/android/media/session/MediaController) controller)`

Sets a `[MediaController](/reference/android/media/session/MediaController)` to send media keys and volume changes to.

`void`

`[setPictureInPictureParams](/reference/android/app/Activity#setPictureInPictureParams\(android.app.PictureInPictureParams\))([PictureInPictureParams](/reference/android/app/PictureInPictureParams) params)`

Updates the properties of the picture-in-picture activity, or sets it to be used later when `[enterPictureInPictureMode()](/reference/android/app/Activity#enterPictureInPictureMode\(\))` is called.

`final void`

`[setProgress](/reference/android/app/Activity#setProgress\(int\))(int progress)`

_This method was deprecated in API level 24. No longer supported starting in API 21._

`final void`

`[setProgressBarIndeterminate](/reference/android/app/Activity#setProgressBarIndeterminate\(boolean\))(boolean indeterminate)`

_This method was deprecated in API level 24. No longer supported starting in API 21._

`final void`

`[setProgressBarIndeterminateVisibility](/reference/android/app/Activity#setProgressBarIndeterminateVisibility\(boolean\))(boolean visible)`

_This method was deprecated in API level 24. No longer supported starting in API 21._

`final void`

`[setProgressBarVisibility](/reference/android/app/Activity#setProgressBarVisibility\(boolean\))(boolean visible)`

_This method was deprecated in API level 24. No longer supported starting in API 21._

`void`

`[setRecentsScreenshotEnabled](/reference/android/app/Activity#setRecentsScreenshotEnabled\(boolean\))(boolean enabled)`

If set to false, this indicates to the system that it should never take a screenshot of the activity to be used as a representation in recents screen.

`void`

`[setRequestedOrientation](/reference/android/app/Activity#setRequestedOrientation\(int\))(int requestedOrientation)`

Change the desired orientation of this activity.

`final void`

`[setResult](/reference/android/app/Activity#setResult\(int,%20android.content.Intent\))(int resultCode, [Intent](/reference/android/content/Intent) data)`

Call this to set the result that your activity will return to its caller.

`final void`

`[setResult](/reference/android/app/Activity#setResult\(int\))(int resultCode)`

Call this to set the result that your activity will return to its caller.

`final void`

`[setSecondaryProgress](/reference/android/app/Activity#setSecondaryProgress\(int\))(int secondaryProgress)`

_This method was deprecated in API level 24. No longer supported starting in API 21._

`void`

`[setShouldDockBigOverlays](/reference/android/app/Activity#setShouldDockBigOverlays\(boolean\))(boolean shouldDockBigOverlays)`

Specifies a preference to dock big overlays like the expanded picture-in-picture on TV (see `[PictureInPictureParams.Builder.setExpandedAspectRatio](/reference/android/app/PictureInPictureParams.Builder#setExpandedAspectRatio\(android.util.Rational\))`).

`void`

`[setShowWhenLocked](/reference/android/app/Activity#setShowWhenLocked\(boolean\))(boolean showWhenLocked)`

Specifies whether an `[Activity](/reference/android/app/Activity)` should be shown on top of the lock screen whenever the lockscreen is up and the activity is resumed.

`void`

`[setTaskDescription](/reference/android/app/Activity#setTaskDescription\(android.app.ActivityManager.TaskDescription\))([ActivityManager.TaskDescription](/reference/android/app/ActivityManager.TaskDescription) taskDescription)`

Sets information describing the task with this activity for presentation inside the Recents System UI.

`void`

`[setTheme](/reference/android/app/Activity#setTheme\(int\))(int resid)`

Set the base theme for this context.

`void`

`[setTitle](/reference/android/app/Activity#setTitle\(java.lang.CharSequence\))([CharSequence](/reference/java/lang/CharSequence) title)`

Change the title associated with this activity.

`void`

`[setTitle](/reference/android/app/Activity#setTitle\(int\))(int titleId)`

Change the title associated with this activity.

`void`

`[setTitleColor](/reference/android/app/Activity#setTitleColor\(int\))(int textColor)`

_This method was deprecated in API level 21. Use action bar styles instead._

`boolean`

`[setTranslucent](/reference/android/app/Activity#setTranslucent\(boolean\))(boolean translucent)`

Convert an activity, which particularly with `[R.attr.windowIsTranslucent](/reference/android/R.attr#windowIsTranslucent)` or `[R.attr.windowIsFloating](/reference/android/R.attr#windowIsFloating)` attribute, to a fullscreen opaque activity, or convert it from opaque back to translucent.

`void`

`[setTurnScreenOn](/reference/android/app/Activity#setTurnScreenOn\(boolean\))(boolean turnScreenOn)`

Specifies whether the screen should be turned on when the `[Activity](/reference/android/app/Activity)` is resumed.

`void`

`[setVisible](/reference/android/app/Activity#setVisible\(boolean\))(boolean visible)`

Control whether this activity's main window is visible.

`final void`

`[setVolumeControlStream](/reference/android/app/Activity#setVolumeControlStream\(int\))(int streamType)`

Suggests an audio stream whose volume should be changed by the hardware volume controls.

`void`

`[setVrModeEnabled](/reference/android/app/Activity#setVrModeEnabled\(boolean,%20android.content.ComponentName\))(boolean enabled, [ComponentName](/reference/android/content/ComponentName) requestedComponent)`

Enable or disable virtual reality (VR) mode for this Activity.

`boolean`

`[shouldDockBigOverlays](/reference/android/app/Activity#shouldDockBigOverlays\(\))()`

Returns whether big overlays should be docked next to the activity as set by `[setShouldDockBigOverlays(boolean)](/reference/android/app/Activity#setShouldDockBigOverlays\(boolean\))`.

`boolean`

`[shouldShowRequestPermissionRationale](/reference/android/app/Activity#shouldShowRequestPermissionRationale\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Gets whether you should show UI with rationale before requesting a permission.

`boolean`

`[shouldShowRequestPermissionRationale](/reference/android/app/Activity#shouldShowRequestPermissionRationale\(java.lang.String,%20int\))([String](/reference/java/lang/String) permission, int deviceId)`

Gets whether you should show UI with rationale before requesting a permission.

`boolean`

`[shouldUpRecreateTask](/reference/android/app/Activity#shouldUpRecreateTask\(android.content.Intent\))([Intent](/reference/android/content/Intent) targetIntent)`

Returns true if the app should recreate the task when navigating 'up' from this activity by using targetIntent.

`boolean`

`[showAssist](/reference/android/app/Activity#showAssist\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) args)`

Ask to have the current assistant shown to the user.

`final boolean`

`[showDialog](/reference/android/app/Activity#showDialog\(int,%20android.os.Bundle\))(int id, [Bundle](/reference/android/os/Bundle) args)`

_This method was deprecated in API level 15. Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package._

`final void`

`[showDialog](/reference/android/app/Activity#showDialog\(int\))(int id)`

_This method was deprecated in API level 15. Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package._

`void`

`[showLockTaskEscapeMessage](/reference/android/app/Activity#showLockTaskEscapeMessage\(\))()`

Shows the user the system defined message for telling the user how to exit lock task mode.

`[ActionMode](/reference/android/view/ActionMode)`

`[startActionMode](/reference/android/app/Activity#startActionMode\(android.view.ActionMode.Callback,%20int\))([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback, int type)`

Start an action mode of the given type.

`[ActionMode](/reference/android/view/ActionMode)`

`[startActionMode](/reference/android/app/Activity#startActionMode\(android.view.ActionMode.Callback\))([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback)`

Start an action mode of the default type `[ActionMode.TYPE_PRIMARY](/reference/android/view/ActionMode#TYPE_PRIMARY)`.

`void`

`[startActivities](/reference/android/app/Activity#startActivities\(android.content.Intent[],%20android.os.Bundle\))([Intent[]](/reference/android/content/Intent) intents, [Bundle](/reference/android/os/Bundle) options)`

Launch a new activity.

`void`

`[startActivities](/reference/android/app/Activity#startActivities\(android.content.Intent[]\))([Intent[]](/reference/android/content/Intent) intents)`

Same as `[startActivities(android.content.Intent[], android.os.Bundle)](/reference/android/app/Activity#startActivities\(android.content.Intent[],%20android.os.Bundle\))` with no options specified.

`void`

`[startActivity](/reference/android/app/Activity#startActivity\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

Same as `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))` with no options specified.

`void`

`[startActivity](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [Bundle](/reference/android/os/Bundle) options)`

Launch a new activity.

`void`

`[startActivityForResult](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))([Intent](/reference/android/content/Intent) intent, int requestCode)`

Same as calling `[startActivityForResult(android.content.Intent, int, android.os.Bundle)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int,%20android.os.Bundle\))` with no options.

`void`

`[startActivityForResult](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, int requestCode, [Bundle](/reference/android/os/Bundle) options)`

Launch an activity for which you would like a result when it finished.

`void`

`[startActivityFromChild](/reference/android/app/Activity#startActivityFromChild\(android.app.Activity,%20android.content.Intent,%20int\))([Activity](/reference/android/app/Activity) child, [Intent](/reference/android/content/Intent) intent, int requestCode)`

_This method was deprecated in API level 30. Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int)`_

`void`

`[startActivityFromChild](/reference/android/app/Activity#startActivityFromChild\(android.app.Activity,%20android.content.Intent,%20int,%20android.os.Bundle\))([Activity](/reference/android/app/Activity) child, [Intent](/reference/android/content/Intent) intent, int requestCode, [Bundle](/reference/android/os/Bundle) options)`

_This method was deprecated in API level 30. Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int,Bundle)`_

`void`

`[startActivityFromFragment](/reference/android/app/Activity#startActivityFromFragment\(android.app.Fragment,%20android.content.Intent,%20int,%20android.os.Bundle\))([Fragment](/reference/android/app/Fragment) fragment, [Intent](/reference/android/content/Intent) intent, int requestCode, [Bundle](/reference/android/os/Bundle) options)`

_This method was deprecated in API level 28. Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int,Bundle)`_

`void`

`[startActivityFromFragment](/reference/android/app/Activity#startActivityFromFragment\(android.app.Fragment,%20android.content.Intent,%20int\))([Fragment](/reference/android/app/Fragment) fragment, [Intent](/reference/android/content/Intent) intent, int requestCode)`

_This method was deprecated in API level 28. Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int)`_

`boolean`

`[startActivityIfNeeded](/reference/android/app/Activity#startActivityIfNeeded\(android.content.Intent,%20int,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, int requestCode, [Bundle](/reference/android/os/Bundle) options)`

A special variation to launch an activity only if a new activity instance is needed to handle the given Intent.

`boolean`

`[startActivityIfNeeded](/reference/android/app/Activity#startActivityIfNeeded\(android.content.Intent,%20int\))([Intent](/reference/android/content/Intent) intent, int requestCode)`

Same as calling `[startActivityIfNeeded(android.content.Intent, int, android.os.Bundle)](/reference/android/app/Activity#startActivityIfNeeded\(android.content.Intent,%20int,%20android.os.Bundle\))` with no options.

`void`

`[startIntentSender](/reference/android/app/Activity#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int\))([IntentSender](/reference/android/content/IntentSender) intent, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags)`

Same as calling `[startIntentSender(android.content.IntentSender, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options.

`void`

`[startIntentSender](/reference/android/app/Activity#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))([IntentSender](/reference/android/content/IntentSender) intent, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags, [Bundle](/reference/android/os/Bundle) options)`

Like `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))`, but taking a IntentSender to start; see `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` for more information.

`void`

`[startIntentSenderForResult](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int\))([IntentSender](/reference/android/content/IntentSender) intent, int requestCode, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags)`

Same as calling `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options.

`void`

`[startIntentSenderForResult](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))([IntentSender](/reference/android/content/IntentSender) intent, int requestCode, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags, [Bundle](/reference/android/os/Bundle) options)`

Like `[startActivityForResult(android.content.Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`, but allowing you to use a IntentSender to describe the activity to be started.

`void`

`[startIntentSenderFromChild](/reference/android/app/Activity#startIntentSenderFromChild\(android.app.Activity,%20android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))([Activity](/reference/android/app/Activity) child, [IntentSender](/reference/android/content/IntentSender) intent, int requestCode, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags, [Bundle](/reference/android/os/Bundle) options)`

_This method was deprecated in API level 30. Use `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` instead._

`void`

`[startIntentSenderFromChild](/reference/android/app/Activity#startIntentSenderFromChild\(android.app.Activity,%20android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int\))([Activity](/reference/android/app/Activity) child, [IntentSender](/reference/android/content/IntentSender) intent, int requestCode, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags)`

_This method was deprecated in API level 30. Use `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int\))` instead._

`void`

`[startLocalVoiceInteraction](/reference/android/app/Activity#startLocalVoiceInteraction\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) privateOptions)`

Starts a local voice interaction session.

`void`

`[startLockTask](/reference/android/app/Activity#startLockTask\(\))()`

Request to put this activity in a mode where the user is locked to a restricted set of applications.

`void`

`[startManagingCursor](/reference/android/app/Activity#startManagingCursor\(android.database.Cursor\))([Cursor](/reference/android/database/Cursor) c)`

_This method was deprecated in API level 15. Use the new `[CursorLoader](/reference/android/content/CursorLoader)` class with `[LoaderManager](/reference/android/app/LoaderManager)` instead; this is also available on older platforms through the Android compatibility package._

`boolean`

`[startNextMatchingActivity](/reference/android/app/Activity#startNextMatchingActivity\(android.content.Intent,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [Bundle](/reference/android/os/Bundle) options)`

Special version of starting an activity, for use when you are replacing other activity components.

`boolean`

`[startNextMatchingActivity](/reference/android/app/Activity#startNextMatchingActivity\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

Same as calling `[startNextMatchingActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startNextMatchingActivity\(android.content.Intent,%20android.os.Bundle\))` with no options.

`void`

`[startPostponedEnterTransition](/reference/android/app/Activity#startPostponedEnterTransition\(\))()`

Begin postponed transitions after `[postponeEnterTransition()](/reference/android/app/Activity#postponeEnterTransition\(\))` was called.

`void`

`[startSearch](/reference/android/app/Activity#startSearch\(java.lang.String,%20boolean,%20android.os.Bundle,%20boolean\))([String](/reference/java/lang/String) initialQuery, boolean selectInitialQuery, [Bundle](/reference/android/os/Bundle) appSearchData, boolean globalSearch)`

This hook is called to launch the search UI.

`void`

`[stopLocalVoiceInteraction](/reference/android/app/Activity#stopLocalVoiceInteraction\(\))()`

Request to terminate the current voice interaction that was previously started using `[startLocalVoiceInteraction(android.os.Bundle)](/reference/android/app/Activity#startLocalVoiceInteraction\(android.os.Bundle\))`.

`void`

`[stopLockTask](/reference/android/app/Activity#stopLockTask\(\))()`

Stop the current task from being locked.

`void`

`[stopManagingCursor](/reference/android/app/Activity#stopManagingCursor\(android.database.Cursor\))([Cursor](/reference/android/database/Cursor) c)`

_This method was deprecated in API level 15. Use the new `[CursorLoader](/reference/android/content/CursorLoader)` class with `[LoaderManager](/reference/android/app/LoaderManager)` instead; this is also available on older platforms through the Android compatibility package._

`void`

`[takeKeyEvents](/reference/android/app/Activity#takeKeyEvents\(boolean\))(boolean get)`

Request that key events come to this activity.

`void`

`[triggerSearch](/reference/android/app/Activity#triggerSearch\(java.lang.String,%20android.os.Bundle\))([String](/reference/java/lang/String) query, [Bundle](/reference/android/os/Bundle) appSearchData)`

Similar to `[startSearch(String, boolean, Bundle, boolean)](/reference/android/app/Activity#startSearch\(java.lang.String,%20boolean,%20android.os.Bundle,%20boolean\))`, but actually fires off the search query after invoking the search dialog.

`void`

`[unregisterActivityLifecycleCallbacks](/reference/android/app/Activity#unregisterActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))([Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks) callback)`

Unregister an `[Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks)` previously registered with `[registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)](/reference/android/app/Activity#registerActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))`.

`void`

`[unregisterComponentCallbacks](/reference/android/app/Activity#unregisterComponentCallbacks\(android.content.ComponentCallbacks\))([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)`

Remove a `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` object that was previously registered with `[registerComponentCallbacks(android.content.ComponentCallbacks)](/reference/android/content/ContextWrapper#registerComponentCallbacks\(android.content.ComponentCallbacks\))`.

`void`

`[unregisterForContextMenu](/reference/android/app/Activity#unregisterForContextMenu\(android.view.View\))([View](/reference/android/view/View) view)`

Prevents a context menu to be shown for the given view.

`void`

`[unregisterScreenCaptureCallback](/reference/android/app/Activity#unregisterScreenCaptureCallback\(android.app.Activity.ScreenCaptureCallback\))([Activity.ScreenCaptureCallback](/reference/android/app/Activity.ScreenCaptureCallback) callback)`

Unregisters a screen capture callback for this surface.

### Protected methods

`void`

`[attachBaseContext](/reference/android/app/Activity#attachBaseContext\(android.content.Context\))([Context](/reference/android/content/Context) newBase)`

Set the base context for this ContextWrapper.

`void`

`[onActivityResult](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))(int requestCode, int resultCode, [Intent](/reference/android/content/Intent) data)`

Called when an activity you launched exits, giving you the requestCode you started it with, the resultCode it returned, and any additional data from it.

`void`

`[onApplyThemeResource](/reference/android/app/Activity#onApplyThemeResource\(android.content.res.Resources.Theme,%20int,%20boolean\))([Resources.Theme](/reference/android/content/res/Resources.Theme) theme, int resid, boolean first)`

Called by `[setTheme(Theme)](/reference/android/view/ContextThemeWrapper#setTheme\(android.content.res.Resources.Theme\))` and `[getTheme()](/reference/android/view/ContextThemeWrapper#getTheme\(\))` to apply a theme resource to the current Theme object.

`void`

`[onChildTitleChanged](/reference/android/app/Activity#onChildTitleChanged\(android.app.Activity,%20java.lang.CharSequence\))([Activity](/reference/android/app/Activity) childActivity, [CharSequence](/reference/java/lang/CharSequence) title)`

`void`

`[onCreate](/reference/android/app/Activity#onCreate\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) savedInstanceState)`

Called when the activity is starting.

`[Dialog](/reference/android/app/Dialog)`

`[onCreateDialog](/reference/android/app/Activity#onCreateDialog\(int\))(int id)`

_This method was deprecated in API level 15. Old no-arguments version of `[onCreateDialog(int, android.os.Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))`._

`[Dialog](/reference/android/app/Dialog)`

`[onCreateDialog](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))(int id, [Bundle](/reference/android/os/Bundle) args)`

_This method was deprecated in API level 15. Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package._

`void`

`[onDestroy](/reference/android/app/Activity#onDestroy\(\))()`

Perform any final cleanup before an activity is destroyed.

`void`

`[onNewIntent](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

This is called for activities that set launchMode to "singleTop" in their package, or if a client used the `[Intent.FLAG_ACTIVITY_SINGLE_TOP](/reference/android/content/Intent#FLAG_ACTIVITY_SINGLE_TOP)` flag when calling `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`.

`void`

`[onPause](/reference/android/app/Activity#onPause\(\))()`

Called as part of the activity lifecycle when the user no longer actively interacts with the activity, but it is still visible on screen.

`void`

`[onPostCreate](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) savedInstanceState)`

Called when activity start-up is complete (after `[onStart()](/reference/android/app/Activity#onStart\(\))` and `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` have been called).

`void`

`[onPostResume](/reference/android/app/Activity#onPostResume\(\))()`

Called when activity resume is complete (after `[onResume()](/reference/android/app/Activity#onResume\(\))` has been called).

`void`

`[onPrepareDialog](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))(int id, [Dialog](/reference/android/app/Dialog) dialog, [Bundle](/reference/android/os/Bundle) args)`

_This method was deprecated in API level 15. Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package._

`void`

`[onPrepareDialog](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog\))(int id, [Dialog](/reference/android/app/Dialog) dialog)`

_This method was deprecated in API level 15. Old no-arguments version of `[onPrepareDialog(int, android.app.Dialog, android.os.Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`._

`void`

`[onRestart](/reference/android/app/Activity#onRestart\(\))()`

Called after `[onStop()](/reference/android/app/Activity#onStop\(\))` when the current activity is being re-displayed to the user (the user has navigated back to it).

`void`

`[onRestoreInstanceState](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) savedInstanceState)`

This method is called after `[onStart()](/reference/android/app/Activity#onStart\(\))` when the activity is being re-initialized from a previously saved state, given here in savedInstanceState.

`void`

`[onResume](/reference/android/app/Activity#onResume\(\))()`

Called after `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`, `[onRestart()](/reference/android/app/Activity#onRestart\(\))`, or `[onPause()](/reference/android/app/Activity#onPause\(\))`.

`void`

`[onSaveInstanceState](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))([Bundle](/reference/android/os/Bundle) outState)`

Called to retrieve per-instance state from an activity before being killed so that the state can be restored in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` or `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` (the `[Bundle](/reference/android/os/Bundle)` populated by this method will be passed to both).

`void`

`[onStart](/reference/android/app/Activity#onStart\(\))()`

Called after `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` — or after `[onRestart()](/reference/android/app/Activity#onRestart\(\))` when the activity had been stopped, but is now again being displayed to the user.

`void`

`[onStop](/reference/android/app/Activity#onStop\(\))()`

Called when you are no longer visible to the user.

`void`

`[onTitleChanged](/reference/android/app/Activity#onTitleChanged\(java.lang.CharSequence,%20int\))([CharSequence](/reference/java/lang/CharSequence) title, int color)`

`void`

`[onUserLeaveHint](/reference/android/app/Activity#onUserLeaveHint\(\))()`

Called as part of the activity lifecycle when an activity is about to go into the background as the result of user choice.

### Inherited methods

From class `[android.view.ContextThemeWrapper](/reference/android/view/ContextThemeWrapper)`

`void`

`[applyOverrideConfiguration](/reference/android/view/ContextThemeWrapper#applyOverrideConfiguration\(android.content.res.Configuration\))([Configuration](/reference/android/content/res/Configuration) overrideConfiguration)`

Call to set an "override configuration" on this context -- this is a configuration that replies one or more values of the standard configuration that is applied to the context.

`void`

`[attachBaseContext](/reference/android/view/ContextThemeWrapper#attachBaseContext\(android.content.Context\))([Context](/reference/android/content/Context) newBase)`

Set the base context for this ContextWrapper.

`[AssetManager](/reference/android/content/res/AssetManager)`

`[getAssets](/reference/android/view/ContextThemeWrapper#getAssets\(\))()`

Returns an AssetManager instance for the application's package.

`[Resources](/reference/android/content/res/Resources)`

`[getResources](/reference/android/view/ContextThemeWrapper#getResources\(\))()`

Returns a Resources instance for the application's package.

`[Object](/reference/java/lang/Object)`

`[getSystemService](/reference/android/view/ContextThemeWrapper#getSystemService\(java.lang.String\))([String](/reference/java/lang/String) name)`

Return the handle to a system-level service by name.

`[Resources.Theme](/reference/android/content/res/Resources.Theme)`

`[getTheme](/reference/android/view/ContextThemeWrapper#getTheme\(\))()`

Return the Theme object associated with this Context.

`void`

`[onApplyThemeResource](/reference/android/view/ContextThemeWrapper#onApplyThemeResource\(android.content.res.Resources.Theme,%20int,%20boolean\))([Resources.Theme](/reference/android/content/res/Resources.Theme) theme, int resId, boolean first)`

Called by `[setTheme(Theme)](/reference/android/view/ContextThemeWrapper#setTheme\(android.content.res.Resources.Theme\))` and `[getTheme()](/reference/android/view/ContextThemeWrapper#getTheme\(\))` to apply a theme resource to the current Theme object.

`void`

`[setTheme](/reference/android/view/ContextThemeWrapper#setTheme\(android.content.res.Resources.Theme\))([Resources.Theme](/reference/android/content/res/Resources.Theme) theme)`

Set the configure the current theme.

`void`

`[setTheme](/reference/android/view/ContextThemeWrapper#setTheme\(int\))(int resid)`

Set the base theme for this context.

From class `[android.content.ContextWrapper](/reference/android/content/ContextWrapper)`

`void`

`[attachBaseContext](/reference/android/content/ContextWrapper#attachBaseContext\(android.content.Context\))([Context](/reference/android/content/Context) base)`

Set the base context for this ContextWrapper.

`boolean`

`[bindIsolatedService](/reference/android/content/ContextWrapper#bindIsolatedService\(android.content.Intent,%20int,%20java.lang.String,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, int flags, [String](/reference/java/lang/String) instanceName, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

Variation of `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` that, in the specific case of isolated services, allows the caller to generate multiple instances of a service from a single component declaration.

`boolean`

`[bindService](/reference/android/content/ContextWrapper#bindService\(android.content.Intent,%20int,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, int flags, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

Same as `[bindService(Intent, ServiceConnection, int)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20int\))` with executor to control ServiceConnection callbacks.

`boolean`

`[bindService](/reference/android/content/ContextWrapper#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20android.content.Context.BindServiceFlags\))([Intent](/reference/android/content/Intent) service, [ServiceConnection](/reference/android/content/ServiceConnection) conn, [Context.BindServiceFlags](/reference/android/content/Context.BindServiceFlags) flags)`

See `[bindService(android.content.Intent, android.content.ServiceConnection, int)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20int\))` Call `[BindServiceFlags.of(long)](/reference/android/content/Context.BindServiceFlags#of\(long\))` to obtain a BindServiceFlags object.

`boolean`

`[bindService](/reference/android/content/ContextWrapper#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20int\))([Intent](/reference/android/content/Intent) service, [ServiceConnection](/reference/android/content/ServiceConnection) conn, int flags)`

Connects to an application service, creating it if needed.

`boolean`

`[bindService](/reference/android/content/ContextWrapper#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, [Context.BindServiceFlags](/reference/android/content/Context.BindServiceFlags) flags, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

See `[bindService(android.content.Intent, int, java.util.concurrent.Executor, android.content.ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20int,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` Call `[BindServiceFlags.of(long)](/reference/android/content/Context.BindServiceFlags#of\(long\))` to obtain a BindServiceFlags object.

`int`

`[checkCallingOrSelfPermission](/reference/android/content/ContextWrapper#checkCallingOrSelfPermission\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Determine whether the calling process of an IPC _or you_ have been granted a particular permission.

`int`

`[checkCallingOrSelfUriPermission](/reference/android/content/ContextWrapper#checkCallingOrSelfUriPermission\(android.net.Uri,%20int\))([Uri](/reference/android/net/Uri) uri, int modeFlags)`

Determine whether the calling process of an IPC _or you_ has been granted permission to access a specific URI.

`int[]`

`[checkCallingOrSelfUriPermissions](/reference/android/content/ContextWrapper#checkCallingOrSelfUriPermissions\(java.util.List<android.net.Uri>,%20int\))([List](/reference/java/util/List)<[Uri](/reference/android/net/Uri)> uris, int modeFlags)`

Determine whether the calling process of an IPC _or you_ has been granted permission to access a list of URIs.

`int`

`[checkCallingPermission](/reference/android/content/ContextWrapper#checkCallingPermission\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Determine whether the calling process of an IPC you are handling has been granted a particular permission.

`int`

`[checkCallingUriPermission](/reference/android/content/ContextWrapper#checkCallingUriPermission\(android.net.Uri,%20int\))([Uri](/reference/android/net/Uri) uri, int modeFlags)`

Determine whether the calling process and uid has been granted permission to access a specific URI.

`int[]`

`[checkCallingUriPermissions](/reference/android/content/ContextWrapper#checkCallingUriPermissions\(java.util.List<android.net.Uri>,%20int\))([List](/reference/java/util/List)<[Uri](/reference/android/net/Uri)> uris, int modeFlags)`

Determine whether the calling process and uid has been granted permission to access a list of URIs.

`int`

`[checkContentUriPermissionFull](/reference/android/content/ContextWrapper#checkContentUriPermissionFull\(android.net.Uri,%20int,%20int,%20int\))([Uri](/reference/android/net/Uri) uri, int pid, int uid, int modeFlags)`

Determine whether a particular process and uid has been granted permission to access a specific content URI.

`int`

`[checkPermission](/reference/android/content/ContextWrapper#checkPermission\(java.lang.String,%20int,%20int\))([String](/reference/java/lang/String) permission, int pid, int uid)`

Determine whether the given permission is allowed for a particular process and user ID running in the system.

`int`

`[checkSelfPermission](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Determine whether _you_ have been granted a particular permission.

`int`

`[checkUriPermission](/reference/android/content/ContextWrapper#checkUriPermission\(android.net.Uri,%20java.lang.String,%20java.lang.String,%20int,%20int,%20int\))([Uri](/reference/android/net/Uri) uri, [String](/reference/java/lang/String) readPermission, [String](/reference/java/lang/String) writePermission, int pid, int uid, int modeFlags)`

Check both a Uri and normal permission.

`int`

`[checkUriPermission](/reference/android/content/ContextWrapper#checkUriPermission\(android.net.Uri,%20int,%20int,%20int\))([Uri](/reference/android/net/Uri) uri, int pid, int uid, int modeFlags)`

Determine whether a particular process and uid has been granted permission to access a specific URI.

`int[]`

`[checkUriPermissions](/reference/android/content/ContextWrapper#checkUriPermissions\(java.util.List<android.net.Uri>,%20int,%20int,%20int\))([List](/reference/java/util/List)<[Uri](/reference/android/net/Uri)> uris, int pid, int uid, int modeFlags)`

Determine whether a particular process and uid has been granted permission to access a list of URIs.

`void`

`[clearWallpaper](/reference/android/content/ContextWrapper#clearWallpaper\(\))()`

_This method is deprecated. Use `[WallpaperManager.clear()](/reference/android/app/WallpaperManager#clear\(\))` instead._

_This method requires the caller to hold the permission `[Manifest.permission.SET_WALLPAPER](/reference/android/Manifest.permission#SET_WALLPAPER)`._

`[Context](/reference/android/content/Context)`

`[createAttributionContext](/reference/android/content/ContextWrapper#createAttributionContext\(java.lang.String\))([String](/reference/java/lang/String) attributionTag)`

Return a new Context object for the current Context but attribute to a different tag.

`[Context](/reference/android/content/Context)`

`[createConfigurationContext](/reference/android/content/ContextWrapper#createConfigurationContext\(android.content.res.Configuration\))([Configuration](/reference/android/content/res/Configuration) overrideConfiguration)`

Return a new Context object for the current Context but whose resources are adjusted to match the given Configuration.

`[Context](/reference/android/content/Context)`

`[createContext](/reference/android/content/ContextWrapper#createContext\(android.content.ContextParams\))([ContextParams](/reference/android/content/ContextParams) contextParams)`

Creates a context with specific properties and behaviors.

`[Context](/reference/android/content/Context)`

`[createDeviceContext](/reference/android/content/ContextWrapper#createDeviceContext\(int\))(int deviceId)`

Returns a new `Context` object from the current context but with device association given by the `deviceId`.

`[Context](/reference/android/content/Context)`

`[createDeviceProtectedStorageContext](/reference/android/content/ContextWrapper#createDeviceProtectedStorageContext\(\))()`

Return a new Context object for the current Context but whose storage APIs are backed by device-protected storage.

`[Context](/reference/android/content/Context)`

`[createDisplayContext](/reference/android/content/ContextWrapper#createDisplayContext\(android.view.Display\))([Display](/reference/android/view/Display) display)`

Returns a new `Context` object from the current context but with resources adjusted to match the metrics of `display`.

`[Context](/reference/android/content/Context)`

`[createPackageContext](/reference/android/content/ContextWrapper#createPackageContext\(java.lang.String,%20int\))([String](/reference/java/lang/String) packageName, int flags)`

Return a new Context object for the given application name.

`[Context](/reference/android/content/Context)`

`[createWindowContext](/reference/android/content/ContextWrapper#createWindowContext\(int,%20android.os.Bundle\))(int type, [Bundle](/reference/android/os/Bundle) options)`

Creates a Context for a non-activity window.

`[Context](/reference/android/content/Context)`

`[createWindowContext](/reference/android/content/ContextWrapper#createWindowContext\(android.view.Display,%20int,%20android.os.Bundle\))([Display](/reference/android/view/Display) display, int type, [Bundle](/reference/android/os/Bundle) options)`

Creates a `Context` for a non-`[activity](/reference/android/app/Activity)` window on the given `[Display](/reference/android/view/Display)`.

`[String[]](/reference/java/lang/String)`

`[databaseList](/reference/android/content/ContextWrapper#databaseList\(\))()`

Returns an array of strings naming the private databases associated with this Context's application package.

`boolean`

`[deleteDatabase](/reference/android/content/ContextWrapper#deleteDatabase\(java.lang.String\))([String](/reference/java/lang/String) name)`

Delete an existing private SQLiteDatabase associated with this Context's application package.

`boolean`

`[deleteFile](/reference/android/content/ContextWrapper#deleteFile\(java.lang.String\))([String](/reference/java/lang/String) name)`

Delete the given private file associated with this Context's application package.

`boolean`

`[deleteSharedPreferences](/reference/android/content/ContextWrapper#deleteSharedPreferences\(java.lang.String\))([String](/reference/java/lang/String) name)`

Delete an existing shared preferences file.

`void`

`[enforceCallingOrSelfPermission](/reference/android/content/ContextWrapper#enforceCallingOrSelfPermission\(java.lang.String,%20java.lang.String\))([String](/reference/java/lang/String) permission, [String](/reference/java/lang/String) message)`

If neither you nor the calling process of an IPC you are handling has been granted a particular permission, throw a `[SecurityException](/reference/java/lang/SecurityException)`.

`void`

`[enforceCallingOrSelfUriPermission](/reference/android/content/ContextWrapper#enforceCallingOrSelfUriPermission\(android.net.Uri,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, int modeFlags, [String](/reference/java/lang/String) message)`

If the calling process of an IPC _or you_ has not been granted permission to access a specific URI, throw `[SecurityException](/reference/java/lang/SecurityException)`.

`void`

`[enforceCallingPermission](/reference/android/content/ContextWrapper#enforceCallingPermission\(java.lang.String,%20java.lang.String\))([String](/reference/java/lang/String) permission, [String](/reference/java/lang/String) message)`

If the calling process of an IPC you are handling has not been granted a particular permission, throw a `[SecurityException](/reference/java/lang/SecurityException)`.

`void`

`[enforceCallingUriPermission](/reference/android/content/ContextWrapper#enforceCallingUriPermission\(android.net.Uri,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, int modeFlags, [String](/reference/java/lang/String) message)`

If the calling process and uid has not been granted permission to access a specific URI, throw `[SecurityException](/reference/java/lang/SecurityException)`.

`void`

`[enforcePermission](/reference/android/content/ContextWrapper#enforcePermission\(java.lang.String,%20int,%20int,%20java.lang.String\))([String](/reference/java/lang/String) permission, int pid, int uid, [String](/reference/java/lang/String) message)`

If the given permission is not allowed for a particular process and user ID running in the system, throw a `[SecurityException](/reference/java/lang/SecurityException)`.

`void`

`[enforceUriPermission](/reference/android/content/ContextWrapper#enforceUriPermission\(android.net.Uri,%20java.lang.String,%20java.lang.String,%20int,%20int,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, [String](/reference/java/lang/String) readPermission, [String](/reference/java/lang/String) writePermission, int pid, int uid, int modeFlags, [String](/reference/java/lang/String) message)`

Enforce both a Uri and normal permission.

`void`

`[enforceUriPermission](/reference/android/content/ContextWrapper#enforceUriPermission\(android.net.Uri,%20int,%20int,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, int pid, int uid, int modeFlags, [String](/reference/java/lang/String) message)`

If a particular process and uid has not been granted permission to access a specific URI, throw `[SecurityException](/reference/java/lang/SecurityException)`.

`[String[]](/reference/java/lang/String)`

`[fileList](/reference/android/content/ContextWrapper#fileList\(\))()`

Returns an array of strings naming the private files associated with this Context's application package.

`[Context](/reference/android/content/Context)`

`[getApplicationContext](/reference/android/content/ContextWrapper#getApplicationContext\(\))()`

Return the context of the single, global Application object of the current process.

`[ApplicationInfo](/reference/android/content/pm/ApplicationInfo)`

`[getApplicationInfo](/reference/android/content/ContextWrapper#getApplicationInfo\(\))()`

Return the full application info for this context's package.

`[AssetManager](/reference/android/content/res/AssetManager)`

`[getAssets](/reference/android/content/ContextWrapper#getAssets\(\))()`

Returns an AssetManager instance for the application's package.

`[AttributionSource](/reference/android/content/AttributionSource)`

`[getAttributionSource](/reference/android/content/ContextWrapper#getAttributionSource\(\))()`

`[Context](/reference/android/content/Context)`

`[getBaseContext](/reference/android/content/ContextWrapper#getBaseContext\(\))()`

`[File](/reference/java/io/File)`

`[getCacheDir](/reference/android/content/ContextWrapper#getCacheDir\(\))()`

Returns the absolute path to the application specific cache directory on the filesystem.

`[ClassLoader](/reference/java/lang/ClassLoader)`

`[getClassLoader](/reference/android/content/ContextWrapper#getClassLoader\(\))()`

Return a class loader you can use to retrieve classes in this package.

`[File](/reference/java/io/File)`

`[getCodeCacheDir](/reference/android/content/ContextWrapper#getCodeCacheDir\(\))()`

Returns the absolute path to the application specific cache directory on the filesystem designed for storing cached code.

`[ContentResolver](/reference/android/content/ContentResolver)`

`[getContentResolver](/reference/android/content/ContextWrapper#getContentResolver\(\))()`

Return a ContentResolver instance for your application's package.

`[File](/reference/java/io/File)`

`[getDataDir](/reference/android/content/ContextWrapper#getDataDir\(\))()`

Returns the absolute path to the directory on the filesystem where all private files belonging to this app are stored.

`[File](/reference/java/io/File)`

`[getDatabasePath](/reference/android/content/ContextWrapper#getDatabasePath\(java.lang.String\))([String](/reference/java/lang/String) name)`

Returns the absolute path on the filesystem where a database created with `[openOrCreateDatabase(String, int, CursorFactory)](/reference/android/content/Context#openOrCreateDatabase\(java.lang.String,%20int,%20android.database.sqlite.SQLiteDatabase.CursorFactory\))` is stored.

`int`

`[getDeviceId](/reference/android/content/ContextWrapper#getDeviceId\(\))()`

Gets the device ID this context is associated with.

`[File](/reference/java/io/File)`

`[getDir](/reference/android/content/ContextWrapper#getDir\(java.lang.String,%20int\))([String](/reference/java/lang/String) name, int mode)`

Retrieve, creating if needed, a new directory in which the application can place its own custom data files.

`[Display](/reference/android/view/Display)`

`[getDisplay](/reference/android/content/ContextWrapper#getDisplay\(\))()`

Get the display this context is associated with.

`[File](/reference/java/io/File)`

`[getExternalCacheDir](/reference/android/content/ContextWrapper#getExternalCacheDir\(\))()`

Returns absolute path to application-specific directory on the primary shared/external storage device where the application can place cache files it owns.

`[File[]](/reference/java/io/File)`

`[getExternalCacheDirs](/reference/android/content/ContextWrapper#getExternalCacheDirs\(\))()`

Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place cache files it owns.

`[File](/reference/java/io/File)`

`[getExternalFilesDir](/reference/android/content/ContextWrapper#getExternalFilesDir\(java.lang.String\))([String](/reference/java/lang/String) type)`

Returns the absolute path to the directory on the primary shared/external storage device where the application can place persistent files it owns.

`[File[]](/reference/java/io/File)`

`[getExternalFilesDirs](/reference/android/content/ContextWrapper#getExternalFilesDirs\(java.lang.String\))([String](/reference/java/lang/String) type)`

Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place persistent files it owns.

`[File[]](/reference/java/io/File)`

`[getExternalMediaDirs](/reference/android/content/ContextWrapper#getExternalMediaDirs\(\))()`

_This method is deprecated. These directories still exist and are scanned, but developers are encouraged to migrate to inserting content into a `[MediaStore](/reference/android/provider/MediaStore)` collection directly, as any app can contribute new media to `[MediaStore](/reference/android/provider/MediaStore)` with no permissions required, starting in `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)`._

`[File](/reference/java/io/File)`

`[getFileStreamPath](/reference/android/content/ContextWrapper#getFileStreamPath\(java.lang.String\))([String](/reference/java/lang/String) name)`

Returns the absolute path on the filesystem where a file created with `[openFileOutput(String, int)](/reference/android/content/Context#openFileOutput\(java.lang.String,%20int\))` is stored.

`[File](/reference/java/io/File)`

`[getFilesDir](/reference/android/content/ContextWrapper#getFilesDir\(\))()`

Returns the absolute path to the directory on the filesystem where files created with `[openFileOutput(String, int)](/reference/android/content/Context#openFileOutput\(java.lang.String,%20int\))` are stored.

`[Executor](/reference/java/util/concurrent/Executor)`

`[getMainExecutor](/reference/android/content/ContextWrapper#getMainExecutor\(\))()`

Return an `[Executor](/reference/java/util/concurrent/Executor)` that will run enqueued tasks on the main thread associated with this context.

`[Looper](/reference/android/os/Looper)`

`[getMainLooper](/reference/android/content/ContextWrapper#getMainLooper\(\))()`

Return the Looper for the main thread of the current process.

`[File](/reference/java/io/File)`

`[getNoBackupFilesDir](/reference/android/content/ContextWrapper#getNoBackupFilesDir\(\))()`

Returns the absolute path to the directory on the filesystem similar to `[getFilesDir()](/reference/android/content/Context#getFilesDir\(\))`.

`[File](/reference/java/io/File)`

`[getObbDir](/reference/android/content/ContextWrapper#getObbDir\(\))()`

Return the primary shared/external storage directory where this application's OBB files (if there are any) can be found.

`[File[]](/reference/java/io/File)`

`[getObbDirs](/reference/android/content/ContextWrapper#getObbDirs\(\))()`

Returns absolute paths to application-specific directories on all shared/external storage devices where the application's OBB files (if there are any) can be found.

`[String](/reference/java/lang/String)`

`[getPackageCodePath](/reference/android/content/ContextWrapper#getPackageCodePath\(\))()`

Return the full path to this context's primary Android package.

`[PackageManager](/reference/android/content/pm/PackageManager)`

`[getPackageManager](/reference/android/content/ContextWrapper#getPackageManager\(\))()`

Return PackageManager instance to find global package information.

`[String](/reference/java/lang/String)`

`[getPackageName](/reference/android/content/ContextWrapper#getPackageName\(\))()`

Return the name of this application's package.

`[String](/reference/java/lang/String)`

`[getPackageResourcePath](/reference/android/content/ContextWrapper#getPackageResourcePath\(\))()`

Return the full path to this context's primary Android package.

`[ContextParams](/reference/android/content/ContextParams)`

`[getParams](/reference/android/content/ContextWrapper#getParams\(\))()`

Return the set of parameters which this Context was created with, if it was created via `[createContext(android.content.ContextParams)](/reference/android/content/Context#createContext\(android.content.ContextParams\))`.

`[Resources](/reference/android/content/res/Resources)`

`[getResources](/reference/android/content/ContextWrapper#getResources\(\))()`

Returns a Resources instance for the application's package.

`[SharedPreferences](/reference/android/content/SharedPreferences)`

`[getSharedPreferences](/reference/android/content/ContextWrapper#getSharedPreferences\(java.lang.String,%20int\))([String](/reference/java/lang/String) name, int mode)`

Retrieve and hold the contents of the preferences file 'name', returning a SharedPreferences through which you can retrieve and modify its values.

`[Object](/reference/java/lang/Object)`

`[getSystemService](/reference/android/content/ContextWrapper#getSystemService\(java.lang.String\))([String](/reference/java/lang/String) name)`

Return the handle to a system-level service by name.

`[String](/reference/java/lang/String)`

`[getSystemServiceName](/reference/android/content/ContextWrapper#getSystemServiceName\(java.lang.Class<?>\))([Class](/reference/java/lang/Class)<?> serviceClass)`

Gets the name of the system-level service that is represented by the specified class.

`[Resources.Theme](/reference/android/content/res/Resources.Theme)`

`[getTheme](/reference/android/content/ContextWrapper#getTheme\(\))()`

Return the Theme object associated with this Context.

`[Drawable](/reference/android/graphics/drawable/Drawable)`

`[getWallpaper](/reference/android/content/ContextWrapper#getWallpaper\(\))()`

_This method is deprecated. Use `[WallpaperManager.get()](/reference/android/app/WallpaperManager#getDrawable\(\))` instead._

`int`

`[getWallpaperDesiredMinimumHeight](/reference/android/content/ContextWrapper#getWallpaperDesiredMinimumHeight\(\))()`

_This method is deprecated. Use `[WallpaperManager.getDesiredMinimumHeight()](/reference/android/app/WallpaperManager#getDesiredMinimumHeight\(\))` instead._

`int`

`[getWallpaperDesiredMinimumWidth](/reference/android/content/ContextWrapper#getWallpaperDesiredMinimumWidth\(\))()`

_This method is deprecated. Use `[WallpaperManager.getDesiredMinimumWidth()](/reference/android/app/WallpaperManager#getDesiredMinimumWidth\(\))` instead._

`void`

`[grantUriPermission](/reference/android/content/ContextWrapper#grantUriPermission\(java.lang.String,%20android.net.Uri,%20int\))([String](/reference/java/lang/String) toPackage, [Uri](/reference/android/net/Uri) uri, int modeFlags)`

Grant permission to access a specific Uri to another package, regardless of whether that package has general permission to access the Uri's content provider.

`boolean`

`[isDeviceProtectedStorage](/reference/android/content/ContextWrapper#isDeviceProtectedStorage\(\))()`

Indicates if the storage APIs of this Context are backed by device-protected storage.

`boolean`

`[isRestricted](/reference/android/content/ContextWrapper#isRestricted\(\))()`

Indicates whether this Context is restricted.

`boolean`

`[moveDatabaseFrom](/reference/android/content/ContextWrapper#moveDatabaseFrom\(android.content.Context,%20java.lang.String\))([Context](/reference/android/content/Context) sourceContext, [String](/reference/java/lang/String) name)`

Move an existing database file from the given source storage context to this context.

`boolean`

`[moveSharedPreferencesFrom](/reference/android/content/ContextWrapper#moveSharedPreferencesFrom\(android.content.Context,%20java.lang.String\))([Context](/reference/android/content/Context) sourceContext, [String](/reference/java/lang/String) name)`

Move an existing shared preferences file from the given source storage context to this context.

`[FileInputStream](/reference/java/io/FileInputStream)`

`[openFileInput](/reference/android/content/ContextWrapper#openFileInput\(java.lang.String\))([String](/reference/java/lang/String) name)`

Open a private file associated with this Context's application package for reading.

`[FileOutputStream](/reference/java/io/FileOutputStream)`

`[openFileOutput](/reference/android/content/ContextWrapper#openFileOutput\(java.lang.String,%20int\))([String](/reference/java/lang/String) name, int mode)`

Open a private file associated with this Context's application package for writing.

`[SQLiteDatabase](/reference/android/database/sqlite/SQLiteDatabase)`

`[openOrCreateDatabase](/reference/android/content/ContextWrapper#openOrCreateDatabase\(java.lang.String,%20int,%20android.database.sqlite.SQLiteDatabase.CursorFactory,%20android.database.DatabaseErrorHandler\))([String](/reference/java/lang/String) name, int mode, [SQLiteDatabase.CursorFactory](/reference/android/database/sqlite/SQLiteDatabase.CursorFactory) factory, [DatabaseErrorHandler](/reference/android/database/DatabaseErrorHandler) errorHandler)`

Open a new private SQLiteDatabase associated with this Context's application package.

`[SQLiteDatabase](/reference/android/database/sqlite/SQLiteDatabase)`

`[openOrCreateDatabase](/reference/android/content/ContextWrapper#openOrCreateDatabase\(java.lang.String,%20int,%20android.database.sqlite.SQLiteDatabase.CursorFactory\))([String](/reference/java/lang/String) name, int mode, [SQLiteDatabase.CursorFactory](/reference/android/database/sqlite/SQLiteDatabase.CursorFactory) factory)`

Open a new private SQLiteDatabase associated with this Context's application package.

`[Drawable](/reference/android/graphics/drawable/Drawable)`

`[peekWallpaper](/reference/android/content/ContextWrapper#peekWallpaper\(\))()`

_This method is deprecated. Use `[WallpaperManager.peek()](/reference/android/app/WallpaperManager#peekDrawable\(\))` instead._

`void`

`[registerComponentCallbacks](/reference/android/content/ContextWrapper#registerComponentCallbacks\(android.content.ComponentCallbacks\))([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)`

Add a new `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` to the base application of the Context, which will be called at the same times as the ComponentCallbacks methods of activities and other components are called.

`void`

`[registerDeviceIdChangeListener](/reference/android/content/ContextWrapper#registerDeviceIdChangeListener\(java.util.concurrent.Executor,%20java.util.function.IntConsumer\))([Executor](/reference/java/util/concurrent/Executor) executor, [IntConsumer](/reference/java/util/function/IntConsumer) listener)`

Adds a new device ID changed listener to the `Context`, which will be called when the device association is changed by the system.

`[Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/ContextWrapper#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter)`

Register a BroadcastReceiver to be run in the main activity thread.

`[Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/ContextWrapper#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20int\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter, int flags)`

Register to receive intent broadcasts, with the receiver optionally being exposed to Instant Apps.

`[Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/ContextWrapper#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler,%20int\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter, [String](/reference/java/lang/String) broadcastPermission, [Handler](/reference/android/os/Handler) scheduler, int flags)`

Register to receive intent broadcasts, to run in the context of scheduler.

`[Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/ContextWrapper#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter, [String](/reference/java/lang/String) broadcastPermission, [Handler](/reference/android/os/Handler) scheduler)`

Register to receive intent broadcasts, to run in the context of scheduler.

`void`

`[removeStickyBroadcast](/reference/android/content/ContextWrapper#removeStickyBroadcast\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[removeStickyBroadcastAsUser](/reference/android/content/ContextWrapper#removeStickyBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[revokeSelfPermissionsOnKill](/reference/android/content/ContextWrapper#revokeSelfPermissionsOnKill\(java.util.Collection<java.lang.String>\))([Collection](/reference/java/util/Collection)<[String](/reference/java/lang/String)> permissions)`

Triggers the revocation of one or more permissions for the calling package.

`void`

`[revokeUriPermission](/reference/android/content/ContextWrapper#revokeUriPermission\(android.net.Uri,%20int\))([Uri](/reference/android/net/Uri) uri, int modeFlags)`

Remove all permissions to access a particular content provider Uri that were previously added with `[grantUriPermission(String, Uri, int)](/reference/android/content/Context#grantUriPermission\(java.lang.String,%20android.net.Uri,%20int\))` or _any other_ mechanism.

`void`

`[revokeUriPermission](/reference/android/content/ContextWrapper#revokeUriPermission\(java.lang.String,%20android.net.Uri,%20int\))([String](/reference/java/lang/String) targetPackage, [Uri](/reference/android/net/Uri) uri, int modeFlags)`

Remove permissions to access a particular content provider Uri that were previously added with `[grantUriPermission(String, Uri, int)](/reference/android/content/Context#grantUriPermission\(java.lang.String,%20android.net.Uri,%20int\))` for a specific target package.

`void`

`[sendBroadcast](/reference/android/content/ContextWrapper#sendBroadcast\(android.content.Intent,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [Bundle](/reference/android/os/Bundle) options)`

Broadcast the given intent to all interested BroadcastReceivers, allowing an optional required permission to be enforced.

`void`

`[sendBroadcast](/reference/android/content/ContextWrapper#sendBroadcast\(android.content.Intent,%20java.lang.String\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission)`

Broadcast the given intent to all interested BroadcastReceivers, allowing an optional required permission to be enforced.

`void`

`[sendBroadcast](/reference/android/content/ContextWrapper#sendBroadcast\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

Broadcast the given intent to all interested BroadcastReceivers.

`void`

`[sendBroadcastAsUser](/reference/android/content/ContextWrapper#sendBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user)`

Version of `[sendBroadcast(android.content.Intent)](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))` that allows you to specify the user the broadcast will be sent to.

`void`

`[sendBroadcastAsUser](/reference/android/content/ContextWrapper#sendBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle,%20java.lang.String\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user, [String](/reference/java/lang/String) receiverPermission)`

Version of `[sendBroadcast(android.content.Intent, java.lang.String)](/reference/android/content/Context#sendBroadcast\(android.content.Intent,%20java.lang.String\))` that allows you to specify the user the broadcast will be sent to.

`void`

`[sendOrderedBroadcast](/reference/android/content/ContextWrapper#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [String](/reference/java/lang/String) receiverAppOp, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendOrderedBroadcast(android.content.Intent, java.lang.String, android.content.BroadcastReceiver, android.os.Handler, int, java.lang.String, android.os.Bundle)](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))` that allows you to specify the App Op to enforce restrictions on which receivers the broadcast will be sent to.

`void`

`[sendOrderedBroadcast](/reference/android/content/ContextWrapper#sendOrderedBroadcast\(android.content.Intent,%20int,%20java.lang.String,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20java.lang.String,%20android.os.Bundle,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, int initialCode, [String](/reference/java/lang/String) receiverPermission, [String](/reference/java/lang/String) receiverAppOp, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras, [Bundle](/reference/android/os/Bundle) options)`

`void`

`[sendOrderedBroadcast](/reference/android/content/ContextWrapper#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendBroadcast(android.content.Intent)](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))` that allows you to receive data back from the broadcast.

`void`

`[sendOrderedBroadcast](/reference/android/content/ContextWrapper#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [Bundle](/reference/android/os/Bundle) options)`

Broadcast the given intent to all interested BroadcastReceivers, delivering them one at a time to allow more preferred receivers to consume the broadcast before it is delivered to less preferred receivers.

`void`

`[sendOrderedBroadcast](/reference/android/content/ContextWrapper#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.os.Bundle,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [Bundle](/reference/android/os/Bundle) options, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendBroadcast(android.content.Intent)](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))` that allows you to receive data back from the broadcast.

`void`

`[sendOrderedBroadcast](/reference/android/content/ContextWrapper#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission)`

Broadcast the given intent to all interested BroadcastReceivers, delivering them one at a time to allow more preferred receivers to consume the broadcast before it is delivered to less preferred receivers.

`void`

`[sendOrderedBroadcastAsUser](/reference/android/content/ContextWrapper#sendOrderedBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user, [String](/reference/java/lang/String) receiverPermission, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendOrderedBroadcast(android.content.Intent, java.lang.String, android.content.BroadcastReceiver, android.os.Handler, int, java.lang.String, android.os.Bundle)](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))` that allows you to specify the user the broadcast will be sent to.

`void`

`[sendStickyBroadcast](/reference/android/content/ContextWrapper#sendStickyBroadcast\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[sendStickyBroadcast](/reference/android/content/ContextWrapper#sendStickyBroadcast\(android.content.Intent,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [Bundle](/reference/android/os/Bundle) options)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[sendStickyBroadcastAsUser](/reference/android/content/ContextWrapper#sendStickyBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[sendStickyOrderedBroadcast](/reference/android/content/ContextWrapper#sendStickyOrderedBroadcast\(android.content.Intent,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[sendStickyOrderedBroadcastAsUser](/reference/android/content/ContextWrapper#sendStickyOrderedBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

_This method is deprecated. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[setTheme](/reference/android/content/ContextWrapper#setTheme\(int\))(int resid)`

Set the base theme for this context.

`void`

`[setWallpaper](/reference/android/content/ContextWrapper#setWallpaper\(android.graphics.Bitmap\))([Bitmap](/reference/android/graphics/Bitmap) bitmap)`

_This method is deprecated. Use `[WallpaperManager.set()](/reference/android/app/WallpaperManager#setBitmap\(android.graphics.Bitmap\))` instead._

_This method requires the caller to hold the permission `[Manifest.permission.SET_WALLPAPER](/reference/android/Manifest.permission#SET_WALLPAPER)`._

`void`

`[setWallpaper](/reference/android/content/ContextWrapper#setWallpaper\(java.io.InputStream\))([InputStream](/reference/java/io/InputStream) data)`

_This method is deprecated. Use `[WallpaperManager.set()](/reference/android/app/WallpaperManager#setStream\(java.io.InputStream\))` instead._

_This method requires the caller to hold the permission `[Manifest.permission.SET_WALLPAPER](/reference/android/Manifest.permission#SET_WALLPAPER)`._

`void`

`[startActivities](/reference/android/content/ContextWrapper#startActivities\(android.content.Intent[],%20android.os.Bundle\))([Intent[]](/reference/android/content/Intent) intents, [Bundle](/reference/android/os/Bundle) options)`

Launch multiple new activities.

`void`

`[startActivities](/reference/android/content/ContextWrapper#startActivities\(android.content.Intent[]\))([Intent[]](/reference/android/content/Intent) intents)`

Same as `[startActivities(android.content.Intent[], android.os.Bundle)](/reference/android/content/Context#startActivities\(android.content.Intent[],%20android.os.Bundle\))` with no options specified.

`void`

`[startActivity](/reference/android/content/ContextWrapper#startActivity\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

Same as `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` with no options specified.

`void`

`[startActivity](/reference/android/content/ContextWrapper#startActivity\(android.content.Intent,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [Bundle](/reference/android/os/Bundle) options)`

Launch a new activity.

`[ComponentName](/reference/android/content/ComponentName)`

`[startForegroundService](/reference/android/content/ContextWrapper#startForegroundService\(android.content.Intent\))([Intent](/reference/android/content/Intent) service)`

Similar to `[startService(android.content.Intent)](/reference/android/content/Context#startService\(android.content.Intent\))`, but with an implicit promise that the Service will call `[startForeground(int, android.app.Notification)](/reference/android/app/Service#startForeground\(int,%20android.app.Notification\))` once it begins running.

`boolean`

`[startInstrumentation](/reference/android/content/ContextWrapper#startInstrumentation\(android.content.ComponentName,%20java.lang.String,%20android.os.Bundle\))([ComponentName](/reference/android/content/ComponentName) className, [String](/reference/java/lang/String) profileFile, [Bundle](/reference/android/os/Bundle) arguments)`

Start executing an `[Instrumentation](/reference/android/app/Instrumentation)` class.

`void`

`[startIntentSender](/reference/android/content/ContextWrapper#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int\))([IntentSender](/reference/android/content/IntentSender) intent, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags)`

Same as `[startIntentSender(android.content.IntentSender, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/content/Context#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options specified.

`void`

`[startIntentSender](/reference/android/content/ContextWrapper#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))([IntentSender](/reference/android/content/IntentSender) intent, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags, [Bundle](/reference/android/os/Bundle) options)`

Like `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))`, but taking a IntentSender to start.

`[ComponentName](/reference/android/content/ComponentName)`

`[startService](/reference/android/content/ContextWrapper#startService\(android.content.Intent\))([Intent](/reference/android/content/Intent) service)`

Request that a given application service be started.

`boolean`

`[stopService](/reference/android/content/ContextWrapper#stopService\(android.content.Intent\))([Intent](/reference/android/content/Intent) name)`

Request that a given application service be stopped.

`void`

`[unbindService](/reference/android/content/ContextWrapper#unbindService\(android.content.ServiceConnection\))([ServiceConnection](/reference/android/content/ServiceConnection) conn)`

Disconnect from an application service.

`void`

`[unregisterComponentCallbacks](/reference/android/content/ContextWrapper#unregisterComponentCallbacks\(android.content.ComponentCallbacks\))([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)`

Remove a `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` object that was previously registered with `[registerComponentCallbacks(android.content.ComponentCallbacks)](/reference/android/content/ContextWrapper#registerComponentCallbacks\(android.content.ComponentCallbacks\))`.

`void`

`[unregisterDeviceIdChangeListener](/reference/android/content/ContextWrapper#unregisterDeviceIdChangeListener\(java.util.function.IntConsumer\))([IntConsumer](/reference/java/util/function/IntConsumer) listener)`

Removes a device ID changed listener from the Context.

`void`

`[unregisterReceiver](/reference/android/content/ContextWrapper#unregisterReceiver\(android.content.BroadcastReceiver\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver)`

Unregister a previously registered BroadcastReceiver.

`void`

`[updateServiceGroup](/reference/android/content/ContextWrapper#updateServiceGroup\(android.content.ServiceConnection,%20int,%20int\))([ServiceConnection](/reference/android/content/ServiceConnection) conn, int group, int importance)`

For a service previously bound with `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` or a related method, change how the system manages that service's process in relation to other processes.

From class `[android.content.Context](/reference/android/content/Context)`

`boolean`

`[bindIsolatedService](/reference/android/content/Context#bindIsolatedService\(android.content.Intent,%20int,%20java.lang.String,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, int flags, [String](/reference/java/lang/String) instanceName, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

Variation of `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` that, in the specific case of isolated services, allows the caller to generate multiple instances of a service from a single component declaration.

`boolean`

`[bindIsolatedService](/reference/android/content/Context#bindIsolatedService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.lang.String,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, [Context.BindServiceFlags](/reference/android/content/Context.BindServiceFlags) flags, [String](/reference/java/lang/String) instanceName, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

See `[bindIsolatedService(android.content.Intent, int, java.lang.String, java.util.concurrent.Executor, android.content.ServiceConnection)](/reference/android/content/Context#bindIsolatedService\(android.content.Intent,%20int,%20java.lang.String,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` Call `[BindServiceFlags.of(long)](/reference/android/content/Context.BindServiceFlags#of\(long\))` to obtain a BindServiceFlags object.

`boolean`

`[bindService](/reference/android/content/Context#bindService\(android.content.Intent,%20int,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, int flags, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

Same as `[bindService(Intent, ServiceConnection, int)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20int\))` with executor to control ServiceConnection callbacks.

`boolean`

`[bindService](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20android.content.Context.BindServiceFlags\))([Intent](/reference/android/content/Intent) service, [ServiceConnection](/reference/android/content/ServiceConnection) conn, [Context.BindServiceFlags](/reference/android/content/Context.BindServiceFlags) flags)`

See `[bindService(android.content.Intent, android.content.ServiceConnection, int)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20int\))` Call `[BindServiceFlags.of(long)](/reference/android/content/Context.BindServiceFlags#of\(long\))` to obtain a BindServiceFlags object.

`abstract boolean`

`[bindService](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.ServiceConnection,%20int\))([Intent](/reference/android/content/Intent) service, [ServiceConnection](/reference/android/content/ServiceConnection) conn, int flags)`

Connects to an application service, creating it if needed.

`boolean`

`[bindService](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))([Intent](/reference/android/content/Intent) service, [Context.BindServiceFlags](/reference/android/content/Context.BindServiceFlags) flags, [Executor](/reference/java/util/concurrent/Executor) executor, [ServiceConnection](/reference/android/content/ServiceConnection) conn)`

See `[bindService(android.content.Intent, int, java.util.concurrent.Executor, android.content.ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20int,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` Call `[BindServiceFlags.of(long)](/reference/android/content/Context.BindServiceFlags#of\(long\))` to obtain a BindServiceFlags object.

`boolean`

`[bindServiceAsUser](/reference/android/content/Context#bindServiceAsUser\(android.content.Intent,%20android.content.ServiceConnection,%20int,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) service, [ServiceConnection](/reference/android/content/ServiceConnection) conn, int flags, [UserHandle](/reference/android/os/UserHandle) user)`

Binds to a service in the given `user` in the same manner as `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))`.

`boolean`

`[bindServiceAsUser](/reference/android/content/Context#bindServiceAsUser\(android.content.Intent,%20android.content.ServiceConnection,%20android.content.Context.BindServiceFlags,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) service, [ServiceConnection](/reference/android/content/ServiceConnection) conn, [Context.BindServiceFlags](/reference/android/content/Context.BindServiceFlags) flags, [UserHandle](/reference/android/os/UserHandle) user)`

See `[bindServiceAsUser(android.content.Intent, android.content.ServiceConnection, int, android.os.UserHandle)](/reference/android/content/Context#bindServiceAsUser\(android.content.Intent,%20android.content.ServiceConnection,%20int,%20android.os.UserHandle\))` Call `[BindServiceFlags.of(long)](/reference/android/content/Context.BindServiceFlags#of\(long\))` to obtain a BindServiceFlags object.

`abstract int`

`[checkCallingOrSelfPermission](/reference/android/content/Context#checkCallingOrSelfPermission\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Determine whether the calling process of an IPC _or you_ have been granted a particular permission.

`abstract int`

`[checkCallingOrSelfUriPermission](/reference/android/content/Context#checkCallingOrSelfUriPermission\(android.net.Uri,%20int\))([Uri](/reference/android/net/Uri) uri, int modeFlags)`

Determine whether the calling process of an IPC _or you_ has been granted permission to access a specific URI.

`int[]`

`[checkCallingOrSelfUriPermissions](/reference/android/content/Context#checkCallingOrSelfUriPermissions\(java.util.List<android.net.Uri>,%20int\))([List](/reference/java/util/List)<[Uri](/reference/android/net/Uri)> uris, int modeFlags)`

Determine whether the calling process of an IPC _or you_ has been granted permission to access a list of URIs.

`abstract int`

`[checkCallingPermission](/reference/android/content/Context#checkCallingPermission\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Determine whether the calling process of an IPC you are handling has been granted a particular permission.

`abstract int`

`[checkCallingUriPermission](/reference/android/content/Context#checkCallingUriPermission\(android.net.Uri,%20int\))([Uri](/reference/android/net/Uri) uri, int modeFlags)`

Determine whether the calling process and uid has been granted permission to access a specific URI.

`int[]`

`[checkCallingUriPermissions](/reference/android/content/Context#checkCallingUriPermissions\(java.util.List<android.net.Uri>,%20int\))([List](/reference/java/util/List)<[Uri](/reference/android/net/Uri)> uris, int modeFlags)`

Determine whether the calling process and uid has been granted permission to access a list of URIs.

`int`

`[checkContentUriPermissionFull](/reference/android/content/Context#checkContentUriPermissionFull\(android.net.Uri,%20int,%20int,%20int\))([Uri](/reference/android/net/Uri) uri, int pid, int uid, int modeFlags)`

Determine whether a particular process and uid has been granted permission to access a specific content URI.

`abstract int`

`[checkPermission](/reference/android/content/Context#checkPermission\(java.lang.String,%20int,%20int\))([String](/reference/java/lang/String) permission, int pid, int uid)`

Determine whether the given permission is allowed for a particular process and user ID running in the system.

`abstract int`

`[checkSelfPermission](/reference/android/content/Context#checkSelfPermission\(java.lang.String\))([String](/reference/java/lang/String) permission)`

Determine whether _you_ have been granted a particular permission.

`abstract int`

`[checkUriPermission](/reference/android/content/Context#checkUriPermission\(android.net.Uri,%20java.lang.String,%20java.lang.String,%20int,%20int,%20int\))([Uri](/reference/android/net/Uri) uri, [String](/reference/java/lang/String) readPermission, [String](/reference/java/lang/String) writePermission, int pid, int uid, int modeFlags)`

Check both a Uri and normal permission.

`abstract int`

`[checkUriPermission](/reference/android/content/Context#checkUriPermission\(android.net.Uri,%20int,%20int,%20int\))([Uri](/reference/android/net/Uri) uri, int pid, int uid, int modeFlags)`

Determine whether a particular process and uid has been granted permission to access a specific URI.

`int[]`

`[checkUriPermissions](/reference/android/content/Context#checkUriPermissions\(java.util.List<android.net.Uri>,%20int,%20int,%20int\))([List](/reference/java/util/List)<[Uri](/reference/android/net/Uri)> uris, int pid, int uid, int modeFlags)`

Determine whether a particular process and uid has been granted permission to access a list of URIs.

`abstract void`

`[clearWallpaper](/reference/android/content/Context#clearWallpaper\(\))()`

_This method was deprecated in API level 15. Use `[WallpaperManager.clear()](/reference/android/app/WallpaperManager#clear\(\))` instead._

_This method requires the caller to hold the permission `[Manifest.permission.SET_WALLPAPER](/reference/android/Manifest.permission#SET_WALLPAPER)`._

`[Context](/reference/android/content/Context)`

`[createAttributionContext](/reference/android/content/Context#createAttributionContext\(java.lang.String\))([String](/reference/java/lang/String) attributionTag)`

Return a new Context object for the current Context but attribute to a different tag.

`abstract [Context](/reference/android/content/Context)`

`[createConfigurationContext](/reference/android/content/Context#createConfigurationContext\(android.content.res.Configuration\))([Configuration](/reference/android/content/res/Configuration) overrideConfiguration)`

Return a new Context object for the current Context but whose resources are adjusted to match the given Configuration.

`[Context](/reference/android/content/Context)`

`[createContext](/reference/android/content/Context#createContext\(android.content.ContextParams\))([ContextParams](/reference/android/content/ContextParams) contextParams)`

Creates a context with specific properties and behaviors.

`abstract [Context](/reference/android/content/Context)`

`[createContextForSplit](/reference/android/content/Context#createContextForSplit\(java.lang.String\))([String](/reference/java/lang/String) splitName)`

Return a new Context object for the given split name.

`[Context](/reference/android/content/Context)`

`[createDeviceContext](/reference/android/content/Context#createDeviceContext\(int\))(int deviceId)`

Returns a new `Context` object from the current context but with device association given by the `deviceId`.

`abstract [Context](/reference/android/content/Context)`

`[createDeviceProtectedStorageContext](/reference/android/content/Context#createDeviceProtectedStorageContext\(\))()`

Return a new Context object for the current Context but whose storage APIs are backed by device-protected storage.

`abstract [Context](/reference/android/content/Context)`

`[createDisplayContext](/reference/android/content/Context#createDisplayContext\(android.view.Display\))([Display](/reference/android/view/Display) display)`

Returns a new `Context` object from the current context but with resources adjusted to match the metrics of `display`.

`abstract [Context](/reference/android/content/Context)`

`[createPackageContext](/reference/android/content/Context#createPackageContext\(java.lang.String,%20int\))([String](/reference/java/lang/String) packageName, int flags)`

Return a new Context object for the given application name.

`[Context](/reference/android/content/Context)`

`[createWindowContext](/reference/android/content/Context#createWindowContext\(int,%20android.os.Bundle\))(int type, [Bundle](/reference/android/os/Bundle) options)`

Creates a Context for a non-activity window.

`[Context](/reference/android/content/Context)`

`[createWindowContext](/reference/android/content/Context#createWindowContext\(android.view.Display,%20int,%20android.os.Bundle\))([Display](/reference/android/view/Display) display, int type, [Bundle](/reference/android/os/Bundle) options)`

Creates a `Context` for a non-`[activity](/reference/android/app/Activity)` window on the given `[Display](/reference/android/view/Display)`.

`abstract [String[]](/reference/java/lang/String)`

`[databaseList](/reference/android/content/Context#databaseList\(\))()`

Returns an array of strings naming the private databases associated with this Context's application package.

`abstract boolean`

`[deleteDatabase](/reference/android/content/Context#deleteDatabase\(java.lang.String\))([String](/reference/java/lang/String) name)`

Delete an existing private SQLiteDatabase associated with this Context's application package.

`abstract boolean`

`[deleteFile](/reference/android/content/Context#deleteFile\(java.lang.String\))([String](/reference/java/lang/String) name)`

Delete the given private file associated with this Context's application package.

`abstract boolean`

`[deleteSharedPreferences](/reference/android/content/Context#deleteSharedPreferences\(java.lang.String\))([String](/reference/java/lang/String) name)`

Delete an existing shared preferences file.

`abstract void`

`[enforceCallingOrSelfPermission](/reference/android/content/Context#enforceCallingOrSelfPermission\(java.lang.String,%20java.lang.String\))([String](/reference/java/lang/String) permission, [String](/reference/java/lang/String) message)`

If neither you nor the calling process of an IPC you are handling has been granted a particular permission, throw a `[SecurityException](/reference/java/lang/SecurityException)`.

`abstract void`

`[enforceCallingOrSelfUriPermission](/reference/android/content/Context#enforceCallingOrSelfUriPermission\(android.net.Uri,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, int modeFlags, [String](/reference/java/lang/String) message)`

If the calling process of an IPC _or you_ has not been granted permission to access a specific URI, throw `[SecurityException](/reference/java/lang/SecurityException)`.

`abstract void`

`[enforceCallingPermission](/reference/android/content/Context#enforceCallingPermission\(java.lang.String,%20java.lang.String\))([String](/reference/java/lang/String) permission, [String](/reference/java/lang/String) message)`

If the calling process of an IPC you are handling has not been granted a particular permission, throw a `[SecurityException](/reference/java/lang/SecurityException)`.

`abstract void`

`[enforceCallingUriPermission](/reference/android/content/Context#enforceCallingUriPermission\(android.net.Uri,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, int modeFlags, [String](/reference/java/lang/String) message)`

If the calling process and uid has not been granted permission to access a specific URI, throw `[SecurityException](/reference/java/lang/SecurityException)`.

`abstract void`

`[enforcePermission](/reference/android/content/Context#enforcePermission\(java.lang.String,%20int,%20int,%20java.lang.String\))([String](/reference/java/lang/String) permission, int pid, int uid, [String](/reference/java/lang/String) message)`

If the given permission is not allowed for a particular process and user ID running in the system, throw a `[SecurityException](/reference/java/lang/SecurityException)`.

`abstract void`

`[enforceUriPermission](/reference/android/content/Context#enforceUriPermission\(android.net.Uri,%20java.lang.String,%20java.lang.String,%20int,%20int,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, [String](/reference/java/lang/String) readPermission, [String](/reference/java/lang/String) writePermission, int pid, int uid, int modeFlags, [String](/reference/java/lang/String) message)`

Enforce both a Uri and normal permission.

`abstract void`

`[enforceUriPermission](/reference/android/content/Context#enforceUriPermission\(android.net.Uri,%20int,%20int,%20int,%20java.lang.String\))([Uri](/reference/android/net/Uri) uri, int pid, int uid, int modeFlags, [String](/reference/java/lang/String) message)`

If a particular process and uid has not been granted permission to access a specific URI, throw `[SecurityException](/reference/java/lang/SecurityException)`.

`abstract [String[]](/reference/java/lang/String)`

`[fileList](/reference/android/content/Context#fileList\(\))()`

Returns an array of strings naming the private files associated with this Context's application package.

`abstract [Context](/reference/android/content/Context)`

`[getApplicationContext](/reference/android/content/Context#getApplicationContext\(\))()`

Return the context of the single, global Application object of the current process.

`abstract [ApplicationInfo](/reference/android/content/pm/ApplicationInfo)`

`[getApplicationInfo](/reference/android/content/Context#getApplicationInfo\(\))()`

Return the full application info for this context's package.

`abstract [AssetManager](/reference/android/content/res/AssetManager)`

`[getAssets](/reference/android/content/Context#getAssets\(\))()`

Returns an AssetManager instance for the application's package.

`[AttributionSource](/reference/android/content/AttributionSource)`

`[getAttributionSource](/reference/android/content/Context#getAttributionSource\(\))()`

`[String](/reference/java/lang/String)`

`[getAttributionTag](/reference/android/content/Context#getAttributionTag\(\))()`

Attribution can be used in complex apps to logically separate parts of the app.

`abstract [File](/reference/java/io/File)`

`[getCacheDir](/reference/android/content/Context#getCacheDir\(\))()`

Returns the absolute path to the application specific cache directory on the filesystem.

`abstract [ClassLoader](/reference/java/lang/ClassLoader)`

`[getClassLoader](/reference/android/content/Context#getClassLoader\(\))()`

Return a class loader you can use to retrieve classes in this package.

`abstract [File](/reference/java/io/File)`

`[getCodeCacheDir](/reference/android/content/Context#getCodeCacheDir\(\))()`

Returns the absolute path to the application specific cache directory on the filesystem designed for storing cached code.

`final int`

`[getColor](/reference/android/content/Context#getColor\(int\))(int id)`

Returns a color associated with a particular resource ID and styled for the current theme.

`final [ColorStateList](/reference/android/content/res/ColorStateList)`

`[getColorStateList](/reference/android/content/Context#getColorStateList\(int\))(int id)`

Returns a color state list associated with a particular resource ID and styled for the current theme.

`abstract [ContentResolver](/reference/android/content/ContentResolver)`

`[getContentResolver](/reference/android/content/Context#getContentResolver\(\))()`

Return a ContentResolver instance for your application's package.

`abstract [File](/reference/java/io/File)`

`[getDataDir](/reference/android/content/Context#getDataDir\(\))()`

Returns the absolute path to the directory on the filesystem where all private files belonging to this app are stored.

`abstract [File](/reference/java/io/File)`

`[getDatabasePath](/reference/android/content/Context#getDatabasePath\(java.lang.String\))([String](/reference/java/lang/String) name)`

Returns the absolute path on the filesystem where a database created with `[openOrCreateDatabase(String, int, CursorFactory)](/reference/android/content/Context#openOrCreateDatabase\(java.lang.String,%20int,%20android.database.sqlite.SQLiteDatabase.CursorFactory\))` is stored.

`int`

`[getDeviceId](/reference/android/content/Context#getDeviceId\(\))()`

Gets the device ID this context is associated with.

`abstract [File](/reference/java/io/File)`

`[getDir](/reference/android/content/Context#getDir\(java.lang.String,%20int\))([String](/reference/java/lang/String) name, int mode)`

Retrieve, creating if needed, a new directory in which the application can place its own custom data files.

`[Display](/reference/android/view/Display)`

`[getDisplay](/reference/android/content/Context#getDisplay\(\))()`

Get the display this context is associated with.

`final [Drawable](/reference/android/graphics/drawable/Drawable)`

`[getDrawable](/reference/android/content/Context#getDrawable\(int\))(int id)`

Returns a drawable object associated with a particular resource ID and styled for the current theme.

`abstract [File](/reference/java/io/File)`

`[getExternalCacheDir](/reference/android/content/Context#getExternalCacheDir\(\))()`

Returns absolute path to application-specific directory on the primary shared/external storage device where the application can place cache files it owns.

`abstract [File[]](/reference/java/io/File)`

`[getExternalCacheDirs](/reference/android/content/Context#getExternalCacheDirs\(\))()`

Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place cache files it owns.

`abstract [File](/reference/java/io/File)`

`[getExternalFilesDir](/reference/android/content/Context#getExternalFilesDir\(java.lang.String\))([String](/reference/java/lang/String) type)`

Returns the absolute path to the directory on the primary shared/external storage device where the application can place persistent files it owns.

`abstract [File[]](/reference/java/io/File)`

`[getExternalFilesDirs](/reference/android/content/Context#getExternalFilesDirs\(java.lang.String\))([String](/reference/java/lang/String) type)`

Returns absolute paths to application-specific directories on all shared/external storage devices where the application can place persistent files it owns.

`abstract [File[]](/reference/java/io/File)`

`[getExternalMediaDirs](/reference/android/content/Context#getExternalMediaDirs\(\))()`

_This method was deprecated in API level 30. These directories still exist and are scanned, but developers are encouraged to migrate to inserting content into a `[MediaStore](/reference/android/provider/MediaStore)` collection directly, as any app can contribute new media to `[MediaStore](/reference/android/provider/MediaStore)` with no permissions required, starting in `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)`._

`abstract [File](/reference/java/io/File)`

`[getFileStreamPath](/reference/android/content/Context#getFileStreamPath\(java.lang.String\))([String](/reference/java/lang/String) name)`

Returns the absolute path on the filesystem where a file created with `[openFileOutput(String, int)](/reference/android/content/Context#openFileOutput\(java.lang.String,%20int\))` is stored.

`abstract [File](/reference/java/io/File)`

`[getFilesDir](/reference/android/content/Context#getFilesDir\(\))()`

Returns the absolute path to the directory on the filesystem where files created with `[openFileOutput(String, int)](/reference/android/content/Context#openFileOutput\(java.lang.String,%20int\))` are stored.

`[Executor](/reference/java/util/concurrent/Executor)`

`[getMainExecutor](/reference/android/content/Context#getMainExecutor\(\))()`

Return an `[Executor](/reference/java/util/concurrent/Executor)` that will run enqueued tasks on the main thread associated with this context.

`abstract [Looper](/reference/android/os/Looper)`

`[getMainLooper](/reference/android/content/Context#getMainLooper\(\))()`

Return the Looper for the main thread of the current process.

`abstract [File](/reference/java/io/File)`

`[getNoBackupFilesDir](/reference/android/content/Context#getNoBackupFilesDir\(\))()`

Returns the absolute path to the directory on the filesystem similar to `[getFilesDir()](/reference/android/content/Context#getFilesDir\(\))`.

`abstract [File](/reference/java/io/File)`

`[getObbDir](/reference/android/content/Context#getObbDir\(\))()`

Return the primary shared/external storage directory where this application's OBB files (if there are any) can be found.

`abstract [File[]](/reference/java/io/File)`

`[getObbDirs](/reference/android/content/Context#getObbDirs\(\))()`

Returns absolute paths to application-specific directories on all shared/external storage devices where the application's OBB files (if there are any) can be found.

`[String](/reference/java/lang/String)`

`[getOpPackageName](/reference/android/content/Context#getOpPackageName\(\))()`

Return the package name that should be used for `[AppOpsManager](/reference/android/app/AppOpsManager)` calls from this context, so that app ops manager's uid verification will work with the name.

`abstract [String](/reference/java/lang/String)`

`[getPackageCodePath](/reference/android/content/Context#getPackageCodePath\(\))()`

Return the full path to this context's primary Android package.

`abstract [PackageManager](/reference/android/content/pm/PackageManager)`

`[getPackageManager](/reference/android/content/Context#getPackageManager\(\))()`

Return PackageManager instance to find global package information.

`abstract [String](/reference/java/lang/String)`

`[getPackageName](/reference/android/content/Context#getPackageName\(\))()`

Return the name of this application's package.

`abstract [String](/reference/java/lang/String)`

`[getPackageResourcePath](/reference/android/content/Context#getPackageResourcePath\(\))()`

Return the full path to this context's primary Android package.

`[ContextParams](/reference/android/content/ContextParams)`

`[getParams](/reference/android/content/Context#getParams\(\))()`

Return the set of parameters which this Context was created with, if it was created via `[createContext(android.content.ContextParams)](/reference/android/content/Context#createContext\(android.content.ContextParams\))`.

`abstract [Resources](/reference/android/content/res/Resources)`

`[getResources](/reference/android/content/Context#getResources\(\))()`

Returns a Resources instance for the application's package.

`abstract [SharedPreferences](/reference/android/content/SharedPreferences)`

`[getSharedPreferences](/reference/android/content/Context#getSharedPreferences\(java.lang.String,%20int\))([String](/reference/java/lang/String) name, int mode)`

Retrieve and hold the contents of the preferences file 'name', returning a SharedPreferences through which you can retrieve and modify its values.

`final [String](/reference/java/lang/String)`

`[getString](/reference/android/content/Context#getString\(int\))(int resId)`

Returns a localized string from the application's package's default string table.

`final [String](/reference/java/lang/String)`

`[getString](/reference/android/content/Context#getString\(int,%20java.lang.Object[]\))(int resId, [Object...](/reference/java/lang/Object) formatArgs)`

Returns a localized formatted string from the application's package's default string table, substituting the format arguments as defined in `[Formatter](/reference/java/util/Formatter)` and `[String.format(String, Object)](/reference/java/lang/String#format\(java.lang.String,%20java.lang.Object[]\))`.

`final <T> T`

`[getSystemService](/reference/android/content/Context#getSystemService\(java.lang.Class<T>\))([Class](/reference/java/lang/Class)<T> serviceClass)`

Return the handle to a system-level service by class.

`abstract [Object](/reference/java/lang/Object)`

`[getSystemService](/reference/android/content/Context#getSystemService\(java.lang.String\))([String](/reference/java/lang/String) name)`

Return the handle to a system-level service by name.

`abstract [String](/reference/java/lang/String)`

`[getSystemServiceName](/reference/android/content/Context#getSystemServiceName\(java.lang.Class<?>\))([Class](/reference/java/lang/Class)<?> serviceClass)`

Gets the name of the system-level service that is represented by the specified class.

`final [CharSequence](/reference/java/lang/CharSequence)`

`[getText](/reference/android/content/Context#getText\(int\))(int resId)`

Return a localized, styled CharSequence from the application's package's default string table.

`abstract [Resources.Theme](/reference/android/content/res/Resources.Theme)`

`[getTheme](/reference/android/content/Context#getTheme\(\))()`

Return the Theme object associated with this Context.

`abstract [Drawable](/reference/android/graphics/drawable/Drawable)`

`[getWallpaper](/reference/android/content/Context#getWallpaper\(\))()`

_This method was deprecated in API level 15. Use `[WallpaperManager.get()](/reference/android/app/WallpaperManager#getDrawable\(\))` instead._

`abstract int`

`[getWallpaperDesiredMinimumHeight](/reference/android/content/Context#getWallpaperDesiredMinimumHeight\(\))()`

_This method was deprecated in API level 15. Use `[WallpaperManager.getDesiredMinimumHeight()](/reference/android/app/WallpaperManager#getDesiredMinimumHeight\(\))` instead._

`abstract int`

`[getWallpaperDesiredMinimumWidth](/reference/android/content/Context#getWallpaperDesiredMinimumWidth\(\))()`

_This method was deprecated in API level 15. Use `[WallpaperManager.getDesiredMinimumWidth()](/reference/android/app/WallpaperManager#getDesiredMinimumWidth\(\))` instead._

`abstract void`

`[grantUriPermission](/reference/android/content/Context#grantUriPermission\(java.lang.String,%20android.net.Uri,%20int\))([String](/reference/java/lang/String) toPackage, [Uri](/reference/android/net/Uri) uri, int modeFlags)`

Grant permission to access a specific Uri to another package, regardless of whether that package has general permission to access the Uri's content provider.

`abstract boolean`

`[isDeviceProtectedStorage](/reference/android/content/Context#isDeviceProtectedStorage\(\))()`

Indicates if the storage APIs of this Context are backed by device-protected storage.

`boolean`

`[isRestricted](/reference/android/content/Context#isRestricted\(\))()`

Indicates whether this Context is restricted.

`boolean`

`[isUiContext](/reference/android/content/Context#isUiContext\(\))()`

Returns `true` if the context is a UI context which can access UI components such as `[WindowManager](/reference/android/view/WindowManager)`, `[LayoutInflater](/reference/android/view/LayoutInflater)` or `[WallpaperManager](/reference/android/app/WallpaperManager)`.

`abstract boolean`

`[moveDatabaseFrom](/reference/android/content/Context#moveDatabaseFrom\(android.content.Context,%20java.lang.String\))([Context](/reference/android/content/Context) sourceContext, [String](/reference/java/lang/String) name)`

Move an existing database file from the given source storage context to this context.

`abstract boolean`

`[moveSharedPreferencesFrom](/reference/android/content/Context#moveSharedPreferencesFrom\(android.content.Context,%20java.lang.String\))([Context](/reference/android/content/Context) sourceContext, [String](/reference/java/lang/String) name)`

Move an existing shared preferences file from the given source storage context to this context.

`final [TypedArray](/reference/android/content/res/TypedArray)`

`[obtainStyledAttributes](/reference/android/content/Context#obtainStyledAttributes\(android.util.AttributeSet,%20int[]\))([AttributeSet](/reference/android/util/AttributeSet) set, int[] attrs)`

Retrieve styled attribute information in this Context's theme.

`final [TypedArray](/reference/android/content/res/TypedArray)`

`[obtainStyledAttributes](/reference/android/content/Context#obtainStyledAttributes\(android.util.AttributeSet,%20int[],%20int,%20int\))([AttributeSet](/reference/android/util/AttributeSet) set, int[] attrs, int defStyleAttr, int defStyleRes)`

Retrieve styled attribute information in this Context's theme.

`final [TypedArray](/reference/android/content/res/TypedArray)`

`[obtainStyledAttributes](/reference/android/content/Context#obtainStyledAttributes\(int,%20int[]\))(int resid, int[] attrs)`

Retrieve styled attribute information in this Context's theme.

`final [TypedArray](/reference/android/content/res/TypedArray)`

`[obtainStyledAttributes](/reference/android/content/Context#obtainStyledAttributes\(int[]\))(int[] attrs)`

Retrieve styled attribute information in this Context's theme.

`abstract [FileInputStream](/reference/java/io/FileInputStream)`

`[openFileInput](/reference/android/content/Context#openFileInput\(java.lang.String\))([String](/reference/java/lang/String) name)`

Open a private file associated with this Context's application package for reading.

`abstract [FileOutputStream](/reference/java/io/FileOutputStream)`

`[openFileOutput](/reference/android/content/Context#openFileOutput\(java.lang.String,%20int\))([String](/reference/java/lang/String) name, int mode)`

Open a private file associated with this Context's application package for writing.

`abstract [SQLiteDatabase](/reference/android/database/sqlite/SQLiteDatabase)`

`[openOrCreateDatabase](/reference/android/content/Context#openOrCreateDatabase\(java.lang.String,%20int,%20android.database.sqlite.SQLiteDatabase.CursorFactory,%20android.database.DatabaseErrorHandler\))([String](/reference/java/lang/String) name, int mode, [SQLiteDatabase.CursorFactory](/reference/android/database/sqlite/SQLiteDatabase.CursorFactory) factory, [DatabaseErrorHandler](/reference/android/database/DatabaseErrorHandler) errorHandler)`

Open a new private SQLiteDatabase associated with this Context's application package.

`abstract [SQLiteDatabase](/reference/android/database/sqlite/SQLiteDatabase)`

`[openOrCreateDatabase](/reference/android/content/Context#openOrCreateDatabase\(java.lang.String,%20int,%20android.database.sqlite.SQLiteDatabase.CursorFactory\))([String](/reference/java/lang/String) name, int mode, [SQLiteDatabase.CursorFactory](/reference/android/database/sqlite/SQLiteDatabase.CursorFactory) factory)`

Open a new private SQLiteDatabase associated with this Context's application package.

`abstract [Drawable](/reference/android/graphics/drawable/Drawable)`

`[peekWallpaper](/reference/android/content/Context#peekWallpaper\(\))()`

_This method was deprecated in API level 15. Use `[WallpaperManager.peek()](/reference/android/app/WallpaperManager#peekDrawable\(\))` instead._

`void`

`[registerComponentCallbacks](/reference/android/content/Context#registerComponentCallbacks\(android.content.ComponentCallbacks\))([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)`

Add a new `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` to the base application of the Context, which will be called at the same times as the ComponentCallbacks methods of activities and other components are called.

`void`

`[registerDeviceIdChangeListener](/reference/android/content/Context#registerDeviceIdChangeListener\(java.util.concurrent.Executor,%20java.util.function.IntConsumer\))([Executor](/reference/java/util/concurrent/Executor) executor, [IntConsumer](/reference/java/util/function/IntConsumer) listener)`

Adds a new device ID changed listener to the `Context`, which will be called when the device association is changed by the system.

`abstract [Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter)`

Register a BroadcastReceiver to be run in the main activity thread.

`abstract [Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20int\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter, int flags)`

Register to receive intent broadcasts, with the receiver optionally being exposed to Instant Apps.

`abstract [Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler,%20int\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter, [String](/reference/java/lang/String) broadcastPermission, [Handler](/reference/android/os/Handler) scheduler, int flags)`

Register to receive intent broadcasts, to run in the context of scheduler.

`abstract [Intent](/reference/android/content/Intent)`

`[registerReceiver](/reference/android/content/Context#registerReceiver\(android.content.BroadcastReceiver,%20android.content.IntentFilter,%20java.lang.String,%20android.os.Handler\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver, [IntentFilter](/reference/android/content/IntentFilter) filter, [String](/reference/java/lang/String) broadcastPermission, [Handler](/reference/android/os/Handler) scheduler)`

Register to receive intent broadcasts, to run in the context of scheduler.

`abstract void`

`[removeStickyBroadcast](/reference/android/content/Context#removeStickyBroadcast\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

_This method was deprecated in API level 21. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`abstract void`

`[removeStickyBroadcastAsUser](/reference/android/content/Context#removeStickyBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user)`

_This method was deprecated in API level 21. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[revokeSelfPermissionOnKill](/reference/android/content/Context#revokeSelfPermissionOnKill\(java.lang.String\))([String](/reference/java/lang/String) permName)`

Triggers the asynchronous revocation of a runtime permission.

`void`

`[revokeSelfPermissionsOnKill](/reference/android/content/Context#revokeSelfPermissionsOnKill\(java.util.Collection<java.lang.String>\))([Collection](/reference/java/util/Collection)<[String](/reference/java/lang/String)> permissions)`

Triggers the revocation of one or more permissions for the calling package.

`abstract void`

`[revokeUriPermission](/reference/android/content/Context#revokeUriPermission\(android.net.Uri,%20int\))([Uri](/reference/android/net/Uri) uri, int modeFlags)`

Remove all permissions to access a particular content provider Uri that were previously added with `[grantUriPermission(String, Uri, int)](/reference/android/content/Context#grantUriPermission\(java.lang.String,%20android.net.Uri,%20int\))` or _any other_ mechanism.

`abstract void`

`[revokeUriPermission](/reference/android/content/Context#revokeUriPermission\(java.lang.String,%20android.net.Uri,%20int\))([String](/reference/java/lang/String) toPackage, [Uri](/reference/android/net/Uri) uri, int modeFlags)`

Remove permissions to access a particular content provider Uri that were previously added with `[grantUriPermission(String, Uri, int)](/reference/android/content/Context#grantUriPermission\(java.lang.String,%20android.net.Uri,%20int\))` for a specific target package.

`void`

`[sendBroadcast](/reference/android/content/Context#sendBroadcast\(android.content.Intent,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [Bundle](/reference/android/os/Bundle) options)`

Broadcast the given intent to all interested BroadcastReceivers, allowing an optional required permission to be enforced.

`abstract void`

`[sendBroadcast](/reference/android/content/Context#sendBroadcast\(android.content.Intent,%20java.lang.String\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission)`

Broadcast the given intent to all interested BroadcastReceivers, allowing an optional required permission to be enforced.

`abstract void`

`[sendBroadcast](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

Broadcast the given intent to all interested BroadcastReceivers.

`abstract void`

`[sendBroadcastAsUser](/reference/android/content/Context#sendBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user)`

Version of `[sendBroadcast(android.content.Intent)](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))` that allows you to specify the user the broadcast will be sent to.

`abstract void`

`[sendBroadcastAsUser](/reference/android/content/Context#sendBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle,%20java.lang.String\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user, [String](/reference/java/lang/String) receiverPermission)`

Version of `[sendBroadcast(android.content.Intent, java.lang.String)](/reference/android/content/Context#sendBroadcast\(android.content.Intent,%20java.lang.String\))` that allows you to specify the user the broadcast will be sent to.

`void`

`[sendBroadcastWithMultiplePermissions](/reference/android/content/Context#sendBroadcastWithMultiplePermissions\(android.content.Intent,%20java.lang.String[]\))([Intent](/reference/android/content/Intent) intent, [String[]](/reference/java/lang/String) receiverPermissions)`

Broadcast the given intent to all interested BroadcastReceivers, allowing an array of required permissions to be enforced.

`void`

`[sendOrderedBroadcast](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [String](/reference/java/lang/String) receiverAppOp, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendOrderedBroadcast(android.content.Intent, java.lang.String, android.content.BroadcastReceiver, android.os.Handler, int, java.lang.String, android.os.Bundle)](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))` that allows you to specify the App Op to enforce restrictions on which receivers the broadcast will be sent to.

`abstract void`

`[sendOrderedBroadcast](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendBroadcast(android.content.Intent)](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))` that allows you to receive data back from the broadcast.

`void`

`[sendOrderedBroadcast](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [Bundle](/reference/android/os/Bundle) options)`

Broadcast the given intent to all interested BroadcastReceivers, delivering them one at a time to allow more preferred receivers to consume the broadcast before it is delivered to less preferred receivers.

`void`

`[sendOrderedBroadcast](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.os.Bundle,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission, [Bundle](/reference/android/os/Bundle) options, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendBroadcast(android.content.Intent)](/reference/android/content/Context#sendBroadcast\(android.content.Intent\))` that allows you to receive data back from the broadcast.

`abstract void`

`[sendOrderedBroadcast](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String\))([Intent](/reference/android/content/Intent) intent, [String](/reference/java/lang/String) receiverPermission)`

Broadcast the given intent to all interested BroadcastReceivers, delivering them one at a time to allow more preferred receivers to consume the broadcast before it is delivered to less preferred receivers.

`abstract void`

`[sendOrderedBroadcastAsUser](/reference/android/content/Context#sendOrderedBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user, [String](/reference/java/lang/String) receiverPermission, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

Version of `[sendOrderedBroadcast(android.content.Intent, java.lang.String, android.content.BroadcastReceiver, android.os.Handler, int, java.lang.String, android.os.Bundle)](/reference/android/content/Context#sendOrderedBroadcast\(android.content.Intent,%20java.lang.String,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))` that allows you to specify the user the broadcast will be sent to.

`abstract void`

`[sendStickyBroadcast](/reference/android/content/Context#sendStickyBroadcast\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

_This method was deprecated in API level 21. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`void`

`[sendStickyBroadcast](/reference/android/content/Context#sendStickyBroadcast\(android.content.Intent,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [Bundle](/reference/android/os/Bundle) options)`

_This method was deprecated in API level 31. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`abstract void`

`[sendStickyBroadcastAsUser](/reference/android/content/Context#sendStickyBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user)`

_This method was deprecated in API level 21. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`abstract void`

`[sendStickyOrderedBroadcast](/reference/android/content/Context#sendStickyOrderedBroadcast\(android.content.Intent,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

_This method was deprecated in API level 21. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`abstract void`

`[sendStickyOrderedBroadcastAsUser](/reference/android/content/Context#sendStickyOrderedBroadcastAsUser\(android.content.Intent,%20android.os.UserHandle,%20android.content.BroadcastReceiver,%20android.os.Handler,%20int,%20java.lang.String,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [UserHandle](/reference/android/os/UserHandle) user, [BroadcastReceiver](/reference/android/content/BroadcastReceiver) resultReceiver, [Handler](/reference/android/os/Handler) scheduler, int initialCode, [String](/reference/java/lang/String) initialData, [Bundle](/reference/android/os/Bundle) initialExtras)`

_This method was deprecated in API level 21. Sticky broadcasts should not be used. They provide no security (anyone can access them), no protection (anyone can modify them), and many other problems. The recommended pattern is to use a non-sticky broadcast to report that _something_ has changed, with another mechanism for apps to retrieve the current value whenever desired._

`abstract void`

`[setTheme](/reference/android/content/Context#setTheme\(int\))(int resid)`

Set the base theme for this context.

`abstract void`

`[setWallpaper](/reference/android/content/Context#setWallpaper\(android.graphics.Bitmap\))([Bitmap](/reference/android/graphics/Bitmap) bitmap)`

_This method was deprecated in API level 15. Use `[WallpaperManager.set()](/reference/android/app/WallpaperManager#setBitmap\(android.graphics.Bitmap\))` instead._

_This method requires the caller to hold the permission `[Manifest.permission.SET_WALLPAPER](/reference/android/Manifest.permission#SET_WALLPAPER)`._

`abstract void`

`[setWallpaper](/reference/android/content/Context#setWallpaper\(java.io.InputStream\))([InputStream](/reference/java/io/InputStream) data)`

_This method was deprecated in API level 15. Use `[WallpaperManager.set()](/reference/android/app/WallpaperManager#setStream\(java.io.InputStream\))` instead._

_This method requires the caller to hold the permission `[Manifest.permission.SET_WALLPAPER](/reference/android/Manifest.permission#SET_WALLPAPER)`._

`abstract void`

`[startActivities](/reference/android/content/Context#startActivities\(android.content.Intent[],%20android.os.Bundle\))([Intent[]](/reference/android/content/Intent) intents, [Bundle](/reference/android/os/Bundle) options)`

Launch multiple new activities.

`abstract void`

`[startActivities](/reference/android/content/Context#startActivities\(android.content.Intent[]\))([Intent[]](/reference/android/content/Intent) intents)`

Same as `[startActivities(android.content.Intent[], android.os.Bundle)](/reference/android/content/Context#startActivities\(android.content.Intent[],%20android.os.Bundle\))` with no options specified.

`abstract void`

`[startActivity](/reference/android/content/Context#startActivity\(android.content.Intent\))([Intent](/reference/android/content/Intent) intent)`

Same as `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` with no options specified.

`abstract void`

`[startActivity](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))([Intent](/reference/android/content/Intent) intent, [Bundle](/reference/android/os/Bundle) options)`

Launch a new activity.

`abstract [ComponentName](/reference/android/content/ComponentName)`

`[startForegroundService](/reference/android/content/Context#startForegroundService\(android.content.Intent\))([Intent](/reference/android/content/Intent) service)`

Similar to `[startService(android.content.Intent)](/reference/android/content/Context#startService\(android.content.Intent\))`, but with an implicit promise that the Service will call `[startForeground(int, android.app.Notification)](/reference/android/app/Service#startForeground\(int,%20android.app.Notification\))` once it begins running.

`abstract boolean`

`[startInstrumentation](/reference/android/content/Context#startInstrumentation\(android.content.ComponentName,%20java.lang.String,%20android.os.Bundle\))([ComponentName](/reference/android/content/ComponentName) className, [String](/reference/java/lang/String) profileFile, [Bundle](/reference/android/os/Bundle) arguments)`

Start executing an `[Instrumentation](/reference/android/app/Instrumentation)` class.

`abstract void`

`[startIntentSender](/reference/android/content/Context#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int\))([IntentSender](/reference/android/content/IntentSender) intent, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags)`

Same as `[startIntentSender(android.content.IntentSender, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/content/Context#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options specified.

`abstract void`

`[startIntentSender](/reference/android/content/Context#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))([IntentSender](/reference/android/content/IntentSender) intent, [Intent](/reference/android/content/Intent) fillInIntent, int flagsMask, int flagsValues, int extraFlags, [Bundle](/reference/android/os/Bundle) options)`

Like `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))`, but taking a IntentSender to start.

`abstract [ComponentName](/reference/android/content/ComponentName)`

`[startService](/reference/android/content/Context#startService\(android.content.Intent\))([Intent](/reference/android/content/Intent) service)`

Request that a given application service be started.

`abstract boolean`

`[stopService](/reference/android/content/Context#stopService\(android.content.Intent\))([Intent](/reference/android/content/Intent) service)`

Request that a given application service be stopped.

`abstract void`

`[unbindService](/reference/android/content/Context#unbindService\(android.content.ServiceConnection\))([ServiceConnection](/reference/android/content/ServiceConnection) conn)`

Disconnect from an application service.

`void`

`[unregisterComponentCallbacks](/reference/android/content/Context#unregisterComponentCallbacks\(android.content.ComponentCallbacks\))([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)`

Remove a `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` object that was previously registered with `[registerComponentCallbacks(android.content.ComponentCallbacks)](/reference/android/content/Context#registerComponentCallbacks\(android.content.ComponentCallbacks\))`.

`void`

`[unregisterDeviceIdChangeListener](/reference/android/content/Context#unregisterDeviceIdChangeListener\(java.util.function.IntConsumer\))([IntConsumer](/reference/java/util/function/IntConsumer) listener)`

Removes a device ID changed listener from the Context.

`abstract void`

`[unregisterReceiver](/reference/android/content/Context#unregisterReceiver\(android.content.BroadcastReceiver\))([BroadcastReceiver](/reference/android/content/BroadcastReceiver) receiver)`

Unregister a previously registered BroadcastReceiver.

`void`

`[updateServiceGroup](/reference/android/content/Context#updateServiceGroup\(android.content.ServiceConnection,%20int,%20int\))([ServiceConnection](/reference/android/content/ServiceConnection) conn, int group, int importance)`

For a service previously bound with `[bindService(Intent, BindServiceFlags, Executor, ServiceConnection)](/reference/android/content/Context#bindService\(android.content.Intent,%20android.content.Context.BindServiceFlags,%20java.util.concurrent.Executor,%20android.content.ServiceConnection\))` or a related method, change how the system manages that service's process in relation to other processes.

From class `[java.lang.Object](/reference/java/lang/Object)`

`[Object](/reference/java/lang/Object)`

`[clone](/reference/java/lang/Object#clone\(\))()`

Creates and returns a copy of this object.

`boolean`

`[equals](/reference/java/lang/Object#equals\(java.lang.Object\))([Object](/reference/java/lang/Object) obj)`

Indicates whether some other object is "equal to" this one.

`void`

`[finalize](/reference/java/lang/Object#finalize\(\))()`

Called by the garbage collector on an object when garbage collection determines that there are no more references to the object.

`final [Class](/reference/java/lang/Class)<?>`

`[getClass](/reference/java/lang/Object#getClass\(\))()`

Returns the runtime class of this `Object`.

`int`

`[hashCode](/reference/java/lang/Object#hashCode\(\))()`

Returns a hash code value for the object.

`final void`

`[notify](/reference/java/lang/Object#notify\(\))()`

Wakes up a single thread that is waiting on this object's monitor.

`final void`

`[notifyAll](/reference/java/lang/Object#notifyAll\(\))()`

Wakes up all threads that are waiting on this object's monitor.

`[String](/reference/java/lang/String)`

`[toString](/reference/java/lang/Object#toString\(\))()`

Returns a string representation of the object.

`final void`

`[wait](/reference/java/lang/Object#wait\(long,%20int\))(long timeoutMillis, int nanos)`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_, or until a certain amount of real time has elapsed.

`final void`

`[wait](/reference/java/lang/Object#wait\(long\))(long timeoutMillis)`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_, or until a certain amount of real time has elapsed.

`final void`

`[wait](/reference/java/lang/Object#wait\(\))()`

Causes the current thread to wait until it is awakened, typically by being _notified_ or _interrupted_.

From interface `[android.content.ComponentCallbacks2](/reference/android/content/ComponentCallbacks2)`

`abstract void`

`[onTrimMemory](/reference/android/content/ComponentCallbacks2#onTrimMemory\(int\))(int level)`

Called when the operating system has determined that it is a good time for a process to trim unneeded memory from its process.

From interface `[android.view.KeyEvent.Callback](/reference/android/view/KeyEvent.Callback)`

`abstract boolean`

`[onKeyDown](/reference/android/view/KeyEvent.Callback#onKeyDown\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a key down event has occurred.

`abstract boolean`

`[onKeyLongPress](/reference/android/view/KeyEvent.Callback#onKeyLongPress\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a long press has occurred.

`abstract boolean`

`[onKeyMultiple](/reference/android/view/KeyEvent.Callback#onKeyMultiple\(int,%20int,%20android.view.KeyEvent\))(int keyCode, int count, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a user's interaction with an analog control, such as flinging a trackball, generates simulated down/up events for the same key multiple times in quick succession.

`abstract boolean`

`[onKeyUp](/reference/android/view/KeyEvent.Callback#onKeyUp\(int,%20android.view.KeyEvent\))(int keyCode, [KeyEvent](/reference/android/view/KeyEvent) event)`

Called when a key up event has occurred.

From interface `[android.view.LayoutInflater.Factory2](/reference/android/view/LayoutInflater.Factory2)`

`abstract [View](/reference/android/view/View)`

`[onCreateView](/reference/android/view/LayoutInflater.Factory2#onCreateView\(android.view.View,%20java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))([View](/reference/android/view/View) parent, [String](/reference/java/lang/String) name, [Context](/reference/android/content/Context) context, [AttributeSet](/reference/android/util/AttributeSet) attrs)`

Version of `[LayoutInflater.Factory.onCreateView(java.lang.String, android.content.Context, android.util.AttributeSet)](/reference/android/view/LayoutInflater.Factory#onCreateView\(java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))` that also supplies the parent that the view created view will be placed in.

From interface `[android.view.View.OnCreateContextMenuListener](/reference/android/view/View.OnCreateContextMenuListener)`

`abstract void`

`[onCreateContextMenu](/reference/android/view/View.OnCreateContextMenuListener#onCreateContextMenu\(android.view.ContextMenu,%20android.view.View,%20android.view.ContextMenu.ContextMenuInfo\))([ContextMenu](/reference/android/view/ContextMenu) menu, [View](/reference/android/view/View) v, [ContextMenu.ContextMenuInfo](/reference/android/view/ContextMenu.ContextMenuInfo) menuInfo)`

Called when the context menu for this view is being built.

From interface `[android.view.Window.Callback](/reference/android/view/Window.Callback)`

`abstract boolean`

`[dispatchGenericMotionEvent](/reference/android/view/Window.Callback#dispatchGenericMotionEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) event)`

Called to process generic motion events.

`abstract boolean`

`[dispatchKeyEvent](/reference/android/view/Window.Callback#dispatchKeyEvent\(android.view.KeyEvent\))([KeyEvent](/reference/android/view/KeyEvent) event)`

Called to process key events.

`abstract boolean`

`[dispatchKeyShortcutEvent](/reference/android/view/Window.Callback#dispatchKeyShortcutEvent\(android.view.KeyEvent\))([KeyEvent](/reference/android/view/KeyEvent) event)`

Called to process a key shortcut event.

`abstract boolean`

`[dispatchPopulateAccessibilityEvent](/reference/android/view/Window.Callback#dispatchPopulateAccessibilityEvent\(android.view.accessibility.AccessibilityEvent\))([AccessibilityEvent](/reference/android/view/accessibility/AccessibilityEvent) event)`

Called to process population of `[AccessibilityEvent](/reference/android/view/accessibility/AccessibilityEvent)`s.

`abstract boolean`

`[dispatchTouchEvent](/reference/android/view/Window.Callback#dispatchTouchEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) event)`

Called to process touch screen events.

`abstract boolean`

`[dispatchTrackballEvent](/reference/android/view/Window.Callback#dispatchTrackballEvent\(android.view.MotionEvent\))([MotionEvent](/reference/android/view/MotionEvent) event)`

Called to process trackball events.

`abstract void`

`[onActionModeFinished](/reference/android/view/Window.Callback#onActionModeFinished\(android.view.ActionMode\))([ActionMode](/reference/android/view/ActionMode) mode)`

Called when an action mode has been finished.

`abstract void`

`[onActionModeStarted](/reference/android/view/Window.Callback#onActionModeStarted\(android.view.ActionMode\))([ActionMode](/reference/android/view/ActionMode) mode)`

Called when an action mode has been started.

`abstract void`

`[onAttachedToWindow](/reference/android/view/Window.Callback#onAttachedToWindow\(\))()`

Called when the window has been attached to the window manager.

`abstract void`

`[onContentChanged](/reference/android/view/Window.Callback#onContentChanged\(\))()`

This hook is called whenever the content view of the screen changes (due to a call to `[Window.setContentView](/reference/android/view/Window#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))` or `[Window.addContentView](/reference/android/view/Window#addContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))`).

`abstract boolean`

`[onCreatePanelMenu](/reference/android/view/Window.Callback#onCreatePanelMenu\(int,%20android.view.Menu\))(int featureId, [Menu](/reference/android/view/Menu) menu)`

Initialize the contents of the menu for panel 'featureId'.

`abstract [View](/reference/android/view/View)`

`[onCreatePanelView](/reference/android/view/Window.Callback#onCreatePanelView\(int\))(int featureId)`

Instantiate the view to display in the panel for 'featureId'.

`abstract void`

`[onDetachedFromWindow](/reference/android/view/Window.Callback#onDetachedFromWindow\(\))()`

Called when the window has been detached from the window manager.

`abstract boolean`

`[onMenuItemSelected](/reference/android/view/Window.Callback#onMenuItemSelected\(int,%20android.view.MenuItem\))(int featureId, [MenuItem](/reference/android/view/MenuItem) item)`

Called when a panel's menu item has been selected by the user.

`abstract boolean`

`[onMenuOpened](/reference/android/view/Window.Callback#onMenuOpened\(int,%20android.view.Menu\))(int featureId, [Menu](/reference/android/view/Menu) menu)`

Called when a panel's menu is opened by the user.

`abstract void`

`[onPanelClosed](/reference/android/view/Window.Callback#onPanelClosed\(int,%20android.view.Menu\))(int featureId, [Menu](/reference/android/view/Menu) menu)`

Called when a panel is being closed.

`default void`

`[onPointerCaptureChanged](/reference/android/view/Window.Callback#onPointerCaptureChanged\(boolean\))(boolean hasCapture)`

Called when pointer capture is enabled or disabled for the current window.

`abstract boolean`

`[onPreparePanel](/reference/android/view/Window.Callback#onPreparePanel\(int,%20android.view.View,%20android.view.Menu\))(int featureId, [View](/reference/android/view/View) view, [Menu](/reference/android/view/Menu) menu)`

Prepare a panel to be displayed.

`default void`

`[onProvideKeyboardShortcuts](/reference/android/view/Window.Callback#onProvideKeyboardShortcuts\(java.util.List<android.view.KeyboardShortcutGroup>,%20android.view.Menu,%20int\))([List](/reference/java/util/List)<[KeyboardShortcutGroup](/reference/android/view/KeyboardShortcutGroup)> data, [Menu](/reference/android/view/Menu) menu, int deviceId)`

Called when Keyboard Shortcuts are requested for the current window.

`abstract boolean`

`[onSearchRequested](/reference/android/view/Window.Callback#onSearchRequested\(\))()`

Called when the user signals the desire to start a search.

`abstract boolean`

`[onSearchRequested](/reference/android/view/Window.Callback#onSearchRequested\(android.view.SearchEvent\))([SearchEvent](/reference/android/view/SearchEvent) searchEvent)`

Called when the user signals the desire to start a search.

`abstract void`

`[onWindowAttributesChanged](/reference/android/view/Window.Callback#onWindowAttributesChanged\(android.view.WindowManager.LayoutParams\))([WindowManager.LayoutParams](/reference/android/view/WindowManager.LayoutParams) attrs)`

This is called whenever the current window attributes change.

`abstract void`

`[onWindowFocusChanged](/reference/android/view/Window.Callback#onWindowFocusChanged\(boolean\))(boolean hasFocus)`

This hook is called whenever the window focus changes.

`abstract [ActionMode](/reference/android/view/ActionMode)`

`[onWindowStartingActionMode](/reference/android/view/Window.Callback#onWindowStartingActionMode\(android.view.ActionMode.Callback\))([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback)`

Called when an action mode is being started for this window.

`abstract [ActionMode](/reference/android/view/ActionMode)`

`[onWindowStartingActionMode](/reference/android/view/Window.Callback#onWindowStartingActionMode\(android.view.ActionMode.Callback,%20int\))([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback, int type)`

Called when an action mode is being started for this window.

From interface `[android.content.ComponentCallbacks](/reference/android/content/ComponentCallbacks)`

`abstract void`

`[onConfigurationChanged](/reference/android/content/ComponentCallbacks#onConfigurationChanged\(android.content.res.Configuration\))([Configuration](/reference/android/content/res/Configuration) newConfig)`

Called by the system when the device configuration changes while your component is running.

`abstract void`

`[onLowMemory](/reference/android/content/ComponentCallbacks#onLowMemory\(\))()`

_This method was deprecated in API level 35. Since API level 14 this is superseded by `[ComponentCallbacks2.onTrimMemory](/reference/android/content/ComponentCallbacks2#onTrimMemory\(int\))`. Since API level 34 this is never called. If you're overriding ComponentCallbacks2#onTrimMemory and your minSdkVersion is greater than API 14, you can provide an empty implementation for this method._

From interface `[android.view.LayoutInflater.Factory](/reference/android/view/LayoutInflater.Factory)`

`abstract [View](/reference/android/view/View)`

`[onCreateView](/reference/android/view/LayoutInflater.Factory#onCreateView\(java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))([String](/reference/java/lang/String) name, [Context](/reference/android/content/Context) context, [AttributeSet](/reference/android/util/AttributeSet) attrs)`

Hook you can supply that is called when inflating from a LayoutInflater.

## Constants

### DEFAULT\_KEYS\_DIALER

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int DEFAULT\_KEYS\_DIALER

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to launch the dialer during default key handling.

**See also:**

*   `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))`

Constant Value: 1 (0x00000001)

### DEFAULT\_KEYS\_DISABLE

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int DEFAULT\_KEYS\_DISABLE

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to turn off default handling of keys.

**See also:**

*   `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))`

Constant Value: 0 (0x00000000)

### DEFAULT\_KEYS\_SEARCH\_GLOBAL

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int DEFAULT\_KEYS\_SEARCH\_GLOBAL

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to specify that unhandled keystrokes will start a global search (typically web search, but some platforms may define alternate methods for global search)

See `[android.app.SearchManager](/reference/android/app/SearchManager)` for more details.

**See also:**

*   `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))`

Constant Value: 4 (0x00000004)

### DEFAULT\_KEYS\_SEARCH\_LOCAL

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int DEFAULT\_KEYS\_SEARCH\_LOCAL

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to specify that unhandled keystrokes will start an application-defined search. (If the application or activity does not actually define a search, the keys will be ignored.)

See `[android.app.SearchManager](/reference/android/app/SearchManager)` for more details.

**See also:**

*   `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))`

Constant Value: 3 (0x00000003)

### DEFAULT\_KEYS\_SHORTCUT

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int DEFAULT\_KEYS\_SHORTCUT

Use with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))` to execute a menu shortcut in default key handling.

That is, the user does not need to hold down the menu key to execute menu shortcuts.

**See also:**

*   `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))`

Constant Value: 2 (0x00000002)

### FULLSCREEN\_MODE\_REQUEST\_ENTER

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int FULLSCREEN\_MODE\_REQUEST\_ENTER

Request type of `[requestFullscreenMode(int, android.os.OutcomeReceiver)](/reference/android/app/Activity#requestFullscreenMode\(int,%20android.os.OutcomeReceiver<java.lang.Void,java.lang.Throwable>\))`, to request enter fullscreen mode from multi-window mode.

Constant Value: 1 (0x00000001)

### FULLSCREEN\_MODE\_REQUEST\_EXIT

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int FULLSCREEN\_MODE\_REQUEST\_EXIT

Request type of `[requestFullscreenMode(int, android.os.OutcomeReceiver)](/reference/android/app/Activity#requestFullscreenMode\(int,%20android.os.OutcomeReceiver<java.lang.Void,java.lang.Throwable>\))`, to request exiting the requested fullscreen mode and restore to the previous multi-window mode.

Constant Value: 0 (0x00000000)

### OVERRIDE\_TRANSITION\_CLOSE

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int OVERRIDE\_TRANSITION\_CLOSE

Request type of `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))` or `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`, to override the closing transition.

Constant Value: 1 (0x00000001)

### OVERRIDE\_TRANSITION\_OPEN

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int OVERRIDE\_TRANSITION\_OPEN

Request type of `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))` or `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`, to override the opening transition.

Constant Value: 0 (0x00000000)

### RESULT\_CANCELED

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int RESULT\_CANCELED

Standard activity result: operation canceled.

Constant Value: 0 (0x00000000)

### RESULT\_FIRST\_USER

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int RESULT\_FIRST\_USER

Start of user-defined activity results.

Constant Value: 1 (0x00000001)

### RESULT\_OK

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public static final int RESULT\_OK

Standard activity result: operation succeeded.

Constant Value: -1 (0xffffffff)

## Fields

### FOCUSED\_STATE\_SET

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected static final int\[\] FOCUSED\_STATE\_SET

## Public constructors

### Activity

public Activity ()

## Public methods

### addContentView

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void addContentView ([View](/reference/android/view/View) view, 
                [ViewGroup.LayoutParams](/reference/android/view/ViewGroup.LayoutParams) params)

Add an additional content view to the activity. Added after any existing ones in the activity -- existing views are NOT removed.

Parameters

`view`

`View`: The desired content to display.

`params`

`ViewGroup.LayoutParams`: Layout parameters for the view.

### clearOverrideActivityTransition

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void clearOverrideActivityTransition (int overrideType)

Clears the animations which are set from `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))`.

Parameters

`overrideType`

`int`: `OVERRIDE_TRANSITION_OPEN` clear the animation set for starting a new activity. `OVERRIDE_TRANSITION_CLOSE` clear the animation set for finishing an activity. Value is `[OVERRIDE_TRANSITION_OPEN](/reference/android/app/Activity#OVERRIDE_TRANSITION_OPEN)`, or `[OVERRIDE_TRANSITION_CLOSE](/reference/android/app/Activity#OVERRIDE_TRANSITION_CLOSE)`

**See also:**

*   `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))`
*   `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`

### closeContextMenu

Added in [API level 3](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void closeContextMenu ()

Programmatically closes the most recently opened context menu, if showing.

### closeOptionsMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void closeOptionsMenu ()

Progammatically closes the options menu. If the options menu is already closed, this method does nothing.

### createPendingResult

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [PendingIntent](/reference/android/app/PendingIntent) createPendingResult (int requestCode, 
                [Intent](/reference/android/content/Intent) data, 
                int flags)

Create a new PendingIntent object which you can hand to others for them to use to send result data back to your `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))` callback. The created object will be either one-shot (becoming invalid after a result is sent back) or multiple (allowing any number of results to be sent through it).

Parameters

`requestCode`

`int`: Private request code for the sender that will be associated with the result data when it is returned. The sender can not modify this value, allowing you to identify incoming results.

`data`

`Intent`: Default data to supply in the result, which may be modified by the sender. This value cannot be `null`.

`flags`

`int`: May be `[PendingIntent.FLAG_ONE_SHOT](/reference/android/app/PendingIntent#FLAG_ONE_SHOT)`, `[PendingIntent.FLAG_NO_CREATE](/reference/android/app/PendingIntent#FLAG_NO_CREATE)`, `[PendingIntent.FLAG_CANCEL_CURRENT](/reference/android/app/PendingIntent#FLAG_CANCEL_CURRENT)`, `[PendingIntent.FLAG_UPDATE_CURRENT](/reference/android/app/PendingIntent#FLAG_UPDATE_CURRENT)`, or any of the flags as supported by `[Intent.fillIn()](/reference/android/content/Intent#fillIn\(android.content.Intent,%20int\))` to control which unspecified parts of the intent that can be supplied when the actual send happens. Value is either `0` or a combination of `[PendingIntent.FLAG_ONE_SHOT](/reference/android/app/PendingIntent#FLAG_ONE_SHOT)`, `[PendingIntent.FLAG_NO_CREATE](/reference/android/app/PendingIntent#FLAG_NO_CREATE)`, `[PendingIntent.FLAG_CANCEL_CURRENT](/reference/android/app/PendingIntent#FLAG_CANCEL_CURRENT)`, `[PendingIntent.FLAG_UPDATE_CURRENT](/reference/android/app/PendingIntent#FLAG_UPDATE_CURRENT)`, `[PendingIntent.FLAG_IMMUTABLE](/reference/android/app/PendingIntent#FLAG_IMMUTABLE)`, `[PendingIntent.FLAG_MUTABLE](/reference/android/app/PendingIntent#FLAG_MUTABLE)`, android.app.PendingIntent.FLAG\_MUTABLE\_UNAUDITED, `[PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT](/reference/android/app/PendingIntent#FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT)`, `[Intent.FILL_IN_ACTION](/reference/android/content/Intent#FILL_IN_ACTION)`, `[Intent.FILL_IN_DATA](/reference/android/content/Intent#FILL_IN_DATA)`, `[Intent.FILL_IN_CATEGORIES](/reference/android/content/Intent#FILL_IN_CATEGORIES)`, `[Intent.FILL_IN_COMPONENT](/reference/android/content/Intent#FILL_IN_COMPONENT)`, `[Intent.FILL_IN_PACKAGE](/reference/android/content/Intent#FILL_IN_PACKAGE)`, `[Intent.FILL_IN_SOURCE_BOUNDS](/reference/android/content/Intent#FILL_IN_SOURCE_BOUNDS)`, `[Intent.FILL_IN_SELECTOR](/reference/android/content/Intent#FILL_IN_SELECTOR)`, and `[Intent.FILL_IN_CLIP_DATA](/reference/android/content/Intent#FILL_IN_CLIP_DATA)`

Returns

`[PendingIntent](/reference/android/app/PendingIntent)`

Returns an existing or new PendingIntent matching the given parameters. May return null only if `[PendingIntent.FLAG_NO_CREATE](/reference/android/app/PendingIntent#FLAG_NO_CREATE)` has been supplied.

**See also:**

*   `[PendingIntent](/reference/android/app/PendingIntent)`

### dismissDialog

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void dismissDialog (int id)

**This method was deprecated in API level 15.**  
Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package.

Dismiss a dialog that was previously shown via `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`.

Parameters

`id`

`int`: The id of the managed dialog.

Throws

`[IllegalArgumentException](/reference/java/lang/IllegalArgumentException)`

if the id was not previously shown via `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`.

**See also:**

*   `[onCreateDialog(int, Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))`
*   `[onPrepareDialog(int, Dialog, Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`
*   `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`
*   `[removeDialog(int)](/reference/android/app/Activity#removeDialog\(int\))`

### dismissKeyboardShortcutsHelper

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void dismissKeyboardShortcutsHelper ()

Dismiss the Keyboard Shortcuts screen.

### dispatchGenericMotionEvent

Added in [API level 12](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean dispatchGenericMotionEvent ([MotionEvent](/reference/android/view/MotionEvent) ev)

Called to process generic motion events. You can override this to intercept all generic motion events before they are dispatched to the window. Be sure to call this implementation for generic motion events that should be handled normally.

Parameters

`ev`

`MotionEvent`: The generic motion event.

Returns

`boolean`

boolean Return true if this event was consumed.

**See also:**

*   `[onGenericMotionEvent(MotionEvent)](/reference/android/app/Activity#onGenericMotionEvent\(android.view.MotionEvent\))`

### dispatchKeyEvent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean dispatchKeyEvent ([KeyEvent](/reference/android/view/KeyEvent) event)

Called to process key events. You can override this to intercept all key events before they are dispatched to the window. Be sure to call this implementation for key events that should be handled normally.

Parameters

`event`

`KeyEvent`: The key event.

Returns

`boolean`

boolean Return true if this event was consumed.

### dispatchKeyShortcutEvent

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean dispatchKeyShortcutEvent ([KeyEvent](/reference/android/view/KeyEvent) event)

Called to process a key shortcut event. You can override this to intercept all key shortcut events before they are dispatched to the window. Be sure to call this implementation for key shortcut events that should be handled normally.

Parameters

`event`

`KeyEvent`: The key shortcut event.

Returns

`boolean`

True if this event was consumed.

### dispatchPopulateAccessibilityEvent

Added in [API level 4](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean dispatchPopulateAccessibilityEvent ([AccessibilityEvent](/reference/android/view/accessibility/AccessibilityEvent) event)

Called to process population of `[AccessibilityEvent](/reference/android/view/accessibility/AccessibilityEvent)`s.

Parameters

`event`

`AccessibilityEvent`: The event.

Returns

`boolean`

boolean Return true if event population was completed.

### dispatchTouchEvent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean dispatchTouchEvent ([MotionEvent](/reference/android/view/MotionEvent) ev)

Called to process touch screen events. You can override this to intercept all touch screen events before they are dispatched to the window. Be sure to call this implementation for touch screen events that should be handled normally.

Parameters

`ev`

`MotionEvent`: The touch screen event.

Returns

`boolean`

boolean Return true if this event was consumed.

**See also:**

*   `[onTouchEvent(MotionEvent)](/reference/android/app/Activity#onTouchEvent\(android.view.MotionEvent\))`

### dispatchTrackballEvent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean dispatchTrackballEvent ([MotionEvent](/reference/android/view/MotionEvent) ev)

Called to process trackball events. You can override this to intercept all trackball events before they are dispatched to the window. Be sure to call this implementation for trackball events that should be handled normally.

Parameters

`ev`

`MotionEvent`: The trackball event.

Returns

`boolean`

boolean Return true if this event was consumed.

**See also:**

*   `[onTrackballEvent(MotionEvent)](/reference/android/app/Activity#onTrackballEvent\(android.view.MotionEvent\))`

### dump

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void dump ([String](/reference/java/lang/String) prefix, 
                [FileDescriptor](/reference/java/io/FileDescriptor) fd, 
                [PrintWriter](/reference/java/io/PrintWriter) writer, 
                [String\[\]](/reference/java/lang/String) args)

Print the Activity's state into the given stream. This gets invoked if you run `adb shell dumpsys activity <activity_component_name>`.

This method won't be called if the app targets `[Build.VERSION_CODES.TIRAMISU](/reference/android/os/Build.VERSION_CODES#TIRAMISU)` or later if the dump request starts with one of the following arguments:

*   \--autofill
*   \--contentcapture
*   \--translation
*   \--list-dumpables
*   \--dump-dumpable

Parameters

`prefix`

`String`: Desired prefix to prepend at each line of output. This value cannot be `null`.

`fd`

`FileDescriptor`: The raw file descriptor that the dump is being sent to. This value may be `null`.

`writer`

`PrintWriter`: The PrintWriter to which you should dump your state. This will be closed for you after you return. This value cannot be `null`.

`args`

`String`: additional arguments to the dump request. This value may be `null`.

### enterPictureInPictureMode

Added in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean enterPictureInPictureMode ([PictureInPictureParams](/reference/android/app/PictureInPictureParams) params)

Puts the activity in picture-in-picture mode if possible in the current system state. The set parameters in will be combined with the parameters from prior calls to `[setPictureInPictureParams(android.app.PictureInPictureParams)](/reference/android/app/Activity#setPictureInPictureParams\(android.app.PictureInPictureParams\))`. The system may disallow entering picture-in-picture in various cases, including when the activity is not visible, if the screen is locked or if the user has an activity pinned.

By default, system calculates the dimension of picture-in-picture window based on the given . See [Picture-in-picture Support](/guide/topics/ui/picture-in-picture) on how to override this behavior.

Returns

`boolean`

true if the system successfully put this activity into picture-in-picture mode or was already in picture-in-picture mode (see `[isInPictureInPictureMode()](/reference/android/app/Activity#isInPictureInPictureMode\(\))`). If the device does not support picture-in-picture, return false.

**See also:**

*   `[R.attr.supportsPictureInPicture](/reference/android/R.attr#supportsPictureInPicture)`
*   `[PictureInPictureParams](/reference/android/app/PictureInPictureParams)`

### enterPictureInPictureMode

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void enterPictureInPictureMode ()

Puts the activity in picture-in-picture mode if possible in the current system state. Any prior calls to `[setPictureInPictureParams(android.app.PictureInPictureParams)](/reference/android/app/Activity#setPictureInPictureParams\(android.app.PictureInPictureParams\))` will still apply when entering picture-in-picture through this call.

**See also:**

*   `[enterPictureInPictureMode(PictureInPictureParams)](/reference/android/app/Activity#enterPictureInPictureMode\(android.app.PictureInPictureParams\))`
*   `[R.attr.supportsPictureInPicture](/reference/android/R.attr#supportsPictureInPicture)`

### findViewById

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public T findViewById (int id)

Finds a view that was identified by the `android:id` XML attribute that was processed in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`.

**Note:** In most cases -- depending on compiler support -- the resulting view is automatically cast to the target class type. If the target class type is unconstrained, an explicit cast may be necessary.

Parameters

`id`

`int`: the ID to search for

Returns

`T`

a view with given ID if found, or `null` otherwise

**See also:**

*   `[View.findViewById(int)](/reference/android/view/View#findViewById\(int\))`
*   `[requireViewById(int)](/reference/android/app/Activity#requireViewById\(int\))`

### finish

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finish ()

Call this when your activity is done and should be closed. The ActivityResult is propagated back to whoever launched you via onActivityResult().

### finishActivity

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finishActivity (int requestCode)

Force finish another activity that you had previously started with `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`.

Parameters

`requestCode`

`int`: The request code of the activity that you had given to startActivityForResult(). If there are multiple activities started with this request code, they will all be finished.

### finishActivityFromChild

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finishActivityFromChild ([Activity](/reference/android/app/Activity) child, 
                int requestCode)

**This method was deprecated in API level 30.**  
Use `[finishActivity(int)](/reference/android/app/Activity#finishActivity\(int\))` instead.

This is called when a child activity of this one calls its finishActivity().

Parameters

`child`

`Activity`: The activity making the call. This value cannot be `null`.

`requestCode`

`int`: Request code that had been used to start the activity.

### finishAffinity

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finishAffinity ()

Finish this activity as well as all activities immediately below it in the current task that have the same affinity. This is typically used when an application can be launched on to another task (such as from an ACTION\_VIEW of a content type it understands) and the user has used the up navigation to switch out of the current task and in to its own task. In this case, if the user has navigated down into any other activities of the second application, all of those should be removed from the original task as part of the task switch.

Note that this finish does _not_ allow you to deliver results to the previous activity, and an exception will be thrown if you are trying to do so.

### finishAfterTransition

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finishAfterTransition ()

Reverses the Activity Scene entry Transition and triggers the calling Activity to reverse its exit Transition. When the exit Transition completes, `[finish()](/reference/android/app/Activity#finish\(\))` is called. If no entry Transition was used, finish() is called immediately and the Activity exit Transition is run.

**See also:**

*   `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`

### finishAndRemoveTask

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finishAndRemoveTask ()

Call this when your activity is done and should be closed and the task should be completely removed as a part of finishing the root activity of the task.

### finishFromChild

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void finishFromChild ([Activity](/reference/android/app/Activity) child)

**This method was deprecated in API level 30.**  
Use `[finish()](/reference/android/app/Activity#finish\(\))` instead.

This is called when a child activity of this one calls its `[finish()](/reference/android/app/Activity#finish\(\))` method. The default implementation simply calls finish() on this activity (the parent), finishing the entire group.

Parameters

`child`

`Activity`: The activity making the call.

**See also:**

*   `[finish()](/reference/android/app/Activity#finish\(\))`

### getActionBar

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ActionBar](/reference/android/app/ActionBar) getActionBar ()

Retrieve a reference to this activity's ActionBar.

Returns

`[ActionBar](/reference/android/app/ActionBar)`

The Activity's ActionBar, or null if it does not have one.

### getApplication

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [Application](/reference/android/app/Application) getApplication ()

Return the application that owns this activity.

Returns

`[Application](/reference/android/app/Application)`

### getCaller

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ComponentCaller](/reference/android/app/ComponentCaller) getCaller ()

Returns the ComponentCaller instance of the app that started this activity.

To keep the ComponentCaller instance for future use, call `[setIntent(android.content.Intent, android.app.ComponentCaller)](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))`, and use this method to retrieve it.

Note that in `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, this method will return the original ComponentCaller. You can use `[setIntent(android.content.Intent, android.app.ComponentCaller)](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))` to update it to the new ComponentCaller.

Returns

`[ComponentCaller](/reference/android/app/ComponentCaller)`

`[ComponentCaller](/reference/android/app/ComponentCaller)` instance corresponding to the intent from `[getIntent()](/reference/android/app/Activity#getIntent\(\))`, or `null` if the activity was not launched with that intent

**See also:**

*   `[ComponentCaller](/reference/android/app/ComponentCaller)`
*   `[getIntent()](/reference/android/app/Activity#getIntent\(\))`
*   `[setIntent(Intent, ComponentCaller)](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))`

### getCallingActivity

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ComponentName](/reference/android/content/ComponentName) getCallingActivity ()

Return the name of the activity that invoked this activity. This is who the data in `[setResult()](/reference/android/app/Activity#setResult\(int\))` will be sent to. You can use this information to validate that the recipient is allowed to receive the data.

Note: if the calling activity is not expecting a result (that is it did not use the `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))` form that includes a request code), then the calling package will be null.

Returns

`[ComponentName](/reference/android/content/ComponentName)`

The ComponentName of the activity that will receive your reply, or null if none.

### getCallingPackage

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [String](/reference/java/lang/String) getCallingPackage ()

Return the name of the package that invoked this activity. This is who the data in `[setResult()](/reference/android/app/Activity#setResult\(int\))` will be sent to. You can use this information to validate that the recipient is allowed to receive the data.

Note: if the calling activity is not expecting a result (that is it did not use the `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))` form that includes a request code), then the calling package will be null.

Note: prior to `[Build.VERSION_CODES.JELLY_BEAN_MR2](/reference/android/os/Build.VERSION_CODES#JELLY_BEAN_MR2)`, the result from this method was unstable. If the process hosting the calling package was no longer running, it would return null instead of the proper package name. You can use `[getCallingActivity()](/reference/android/app/Activity#getCallingActivity\(\))` and retrieve the package name from that instead.

Returns

`[String](/reference/java/lang/String)`

The package of the activity that will receive your reply, or null if none.

### getChangingConfigurations

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getChangingConfigurations ()

If this activity is being destroyed because it can not handle a configuration parameter being changed (and thus its `[onConfigurationChanged(android.content.res.Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` method is _not_ being called), then you can use this method to discover the set of changes that have occurred while in the process of being destroyed. Note that there is no guarantee that these will be accurate (other changes could have happened at any time), so you should only use this as an optimization hint.

Returns

`int`

Returns a bit field of the configuration parameters that are changing, as defined by the `[Configuration](/reference/android/content/res/Configuration)` class.

### getComponentName

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ComponentName](/reference/android/content/ComponentName) getComponentName ()

Returns the complete component name of this activity.

Returns

`[ComponentName](/reference/android/content/ComponentName)`

Returns the complete component name for this activity

### getContentScene

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Scene](/reference/android/transition/Scene) getContentScene ()

Retrieve the `[Scene](/reference/android/transition/Scene)` representing this window's current content. Requires `[Window.FEATURE_CONTENT_TRANSITIONS](/reference/android/view/Window#FEATURE_CONTENT_TRANSITIONS)`.

This method will return null if the current content is not represented by a Scene.

Returns

`[Scene](/reference/android/transition/Scene)`

Current Scene being shown or null

### getContentTransitionManager

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [TransitionManager](/reference/android/transition/TransitionManager) getContentTransitionManager ()

Retrieve the `[TransitionManager](/reference/android/transition/TransitionManager)` responsible for default transitions in this window. Requires `[Window.FEATURE_CONTENT_TRANSITIONS](/reference/android/view/Window#FEATURE_CONTENT_TRANSITIONS)`.

This method will return non-null after content has been initialized (e.g. by using `[setContentView(View)](/reference/android/app/Activity#setContentView\(android.view.View\))`) if `[Window.FEATURE_CONTENT_TRANSITIONS](/reference/android/view/Window#FEATURE_CONTENT_TRANSITIONS)` has been granted.

Returns

`[TransitionManager](/reference/android/transition/TransitionManager)`

This window's content TransitionManager or null if none is set.

### getCurrentCaller

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ComponentCaller](/reference/android/app/ComponentCaller) getCurrentCaller ()

Returns the ComponentCaller instance of the app that re-launched this activity with a new intent via `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` or `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`.

Note that this method only works within the `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` and `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))` methods. If you call this method outside `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` and `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`, it will throw an `[IllegalStateException](/reference/java/lang/IllegalStateException)`.

You can also retrieve the caller if you override `[onNewIntent(android.content.Intent, android.app.ComponentCaller)](/reference/android/app/Activity#onNewIntent\(android.content.Intent,%20android.app.ComponentCaller\))` or `[onActivityResult(int, int, android.content.Intent, android.app.ComponentCaller)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent,%20android.app.ComponentCaller\))`.

To keep the ComponentCaller instance for future use, call `[setIntent(android.content.Intent, android.app.ComponentCaller)](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))`, and use `[getCaller()](/reference/android/app/Activity#getCaller\(\))` to retrieve it.

Returns

`[ComponentCaller](/reference/android/app/ComponentCaller)`

`[ComponentCaller](/reference/android/app/ComponentCaller)` instance This value cannot be `null`.

Throws

`[IllegalStateException](/reference/java/lang/IllegalStateException)`

if the caller is `null`, indicating the method was called outside `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`

**See also:**

*   `[ComponentCaller](/reference/android/app/ComponentCaller)`
*   `[setIntent(Intent, ComponentCaller)](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))`
*   `[getCaller()](/reference/android/app/Activity#getCaller\(\))`

### getCurrentFocus

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [View](/reference/android/view/View) getCurrentFocus ()

Calls `[Window.getCurrentFocus()](/reference/android/view/Window#getCurrentFocus\(\))` on the Window of this Activity to return the currently focused view.

Returns

`[View](/reference/android/view/View)`

View The current View with focus or null.

**See also:**

*   `[getWindow()](/reference/android/app/Activity#getWindow\(\))`
*   `[Window.getCurrentFocus()](/reference/android/view/Window#getCurrentFocus\(\))`

### getFragmentManager

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [FragmentManager](/reference/android/app/FragmentManager) getFragmentManager ()

**This method was deprecated in API level 28.**  
Use `[FragmentActivity.getSupportFragmentManager()](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html#getSupportFragmentManager\(\))`

Return the FragmentManager for interacting with fragments associated with this activity.

Returns

`[FragmentManager](/reference/android/app/FragmentManager)`

### getInitialCaller

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ComponentCaller](/reference/android/app/ComponentCaller) getInitialCaller ()

Returns the ComponentCaller instance of the app that initially launched this activity.

Note that calls to `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` and `[setIntent(Intent)](/reference/android/app/Activity#setIntent\(android.content.Intent\))` have no effect on the returned value of this method.

Returns

`[ComponentCaller](/reference/android/app/ComponentCaller)`

`[ComponentCaller](/reference/android/app/ComponentCaller)` instance This value cannot be `null`.

**See also:**

*   `[ComponentCaller](/reference/android/app/ComponentCaller)`

### getIntent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Intent](/reference/android/content/Intent) getIntent ()

Returns the intent that started this activity.

To keep the Intent instance for future use, call `[setIntent(android.content.Intent)](/reference/android/app/Activity#setIntent\(android.content.Intent\))`, and use this method to retrieve it.

Note that in `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, this method will return the original Intent. You can use `[setIntent(android.content.Intent)](/reference/android/app/Activity#setIntent\(android.content.Intent\))` to update it to the new Intent.

Returns

`[Intent](/reference/android/content/Intent)`

`[Intent](/reference/android/content/Intent)` instance that started this activity, or that was kept for future use

### getLastNonConfigurationInstance

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Object](/reference/java/lang/Object) getLastNonConfigurationInstance ()

Retrieve the non-configuration instance data that was previously returned by `[onRetainNonConfigurationInstance()](/reference/android/app/Activity#onRetainNonConfigurationInstance\(\))`. This will be available from the initial `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` and `[onStart()](/reference/android/app/Activity#onStart\(\))` calls to the new instance, allowing you to extract any useful dynamic state from the previous instance.

Note that the data you retrieve here should _only_ be used as an optimization for handling configuration changes. You should always be able to handle getting a null pointer back, and an activity must still be able to restore itself to its previous state (through the normal `[onSaveInstanceState(android.os.Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` mechanism) even if this function returns null.

**Note:** For most cases you should use the `[Fragment](/reference/android/app/Fragment)` API `[Fragment.setRetainInstance(boolean)](/reference/android/app/Fragment#setRetainInstance\(boolean\))` instead; this is also available on older platforms through the Android support libraries.

Returns

`[Object](/reference/java/lang/Object)`

the object previously returned by `[onRetainNonConfigurationInstance()](/reference/android/app/Activity#onRetainNonConfigurationInstance\(\))`

### getLaunchedFromPackage

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [String](/reference/java/lang/String) getLaunchedFromPackage ()

Returns the package name of the app that initially launched this activity.

In order to receive the launching app's package name, at least one of the following has to be met:

*   The app must call `[ActivityOptions.setShareIdentityEnabled(boolean)](/reference/android/app/ActivityOptions#setShareIdentityEnabled\(boolean\))` with a value of `true` and launch this activity with the resulting `ActivityOptions`.
*   The launched activity has the same uid as the launching app.
*   The launched activity is running in a package that is signed with the same key used to sign the platform (typically only system packages such as Settings will meet this requirement).

. These are the same requirements for `[getLaunchedFromUid()](/reference/android/app/Activity#getLaunchedFromUid\(\))`; if any of these are met, then these methods can be used to obtain the uid and package name of the launching app. If none are met, then `null` is returned.

Note, even if the above conditions are not met, the launching app's identity may still be available from `[getCallingPackage()](/reference/android/app/Activity#getCallingPackage\(\))` if this activity was started with `Activity#startActivityForResult` to allow validation of the result's recipient.

Returns

`[String](/reference/java/lang/String)`

the package name of the launching app or null if the current activity cannot access the identity of the launching app

**See also:**

*   `[ActivityOptions.setShareIdentityEnabled(boolean)](/reference/android/app/ActivityOptions#setShareIdentityEnabled\(boolean\))`
*   `[getLaunchedFromUid()](/reference/android/app/Activity#getLaunchedFromUid\(\))`

### getLaunchedFromUid

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getLaunchedFromUid ()

Returns the uid of the app that initially launched this activity.

In order to receive the launching app's uid, at least one of the following has to be met:

*   The app must call `[ActivityOptions.setShareIdentityEnabled(boolean)](/reference/android/app/ActivityOptions#setShareIdentityEnabled\(boolean\))` with a value of `true` and launch this activity with the resulting `ActivityOptions`.
*   The launched activity has the same uid as the launching app.
*   The launched activity is running in a package that is signed with the same key used to sign the platform (typically only system packages such as Settings will meet this requirement).

. These are the same requirements for `[getLaunchedFromPackage()](/reference/android/app/Activity#getLaunchedFromPackage\(\))`; if any of these are met, then these methods can be used to obtain the uid and package name of the launching app. If none are met, then `[Process.INVALID_UID](/reference/android/os/Process#INVALID_UID)` is returned.

Note, even if the above conditions are not met, the launching app's identity may still be available from `[getCallingPackage()](/reference/android/app/Activity#getCallingPackage\(\))` if this activity was started with `Activity#startActivityForResult` to allow validation of the result's recipient.

Returns

`int`

the uid of the launching app or `[Process.INVALID_UID](/reference/android/os/Process#INVALID_UID)` if the current activity cannot access the identity of the launching app

**See also:**

*   `[ActivityOptions.setShareIdentityEnabled(boolean)](/reference/android/app/ActivityOptions#setShareIdentityEnabled\(boolean\))`
*   `[getLaunchedFromPackage()](/reference/android/app/Activity#getLaunchedFromPackage\(\))`

### getLayoutInflater

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [LayoutInflater](/reference/android/view/LayoutInflater) getLayoutInflater ()

Convenience for calling `[Window.getLayoutInflater()](/reference/android/view/Window#getLayoutInflater\(\))`.

Returns

`[LayoutInflater](/reference/android/view/LayoutInflater)`

This value cannot be `null`.

### getLoaderManager

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [LoaderManager](/reference/android/app/LoaderManager) getLoaderManager ()

**This method was deprecated in API level 28.**  
Use `[FragmentActivity.getSupportLoaderManager()](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html#getSupportLoaderManager\(\))`

Return the LoaderManager for this activity, creating it if needed.

Returns

`[LoaderManager](/reference/android/app/LoaderManager)`

### getLocalClassName

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [String](/reference/java/lang/String) getLocalClassName ()

Returns class name for this activity with the package prefix removed. This is the default name used to read and write settings.

Returns

`[String](/reference/java/lang/String)`

The local class name. This value cannot be `null`.

### getMaxNumPictureInPictureActions

Added in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getMaxNumPictureInPictureActions ()

Return the number of actions that will be displayed in the picture-in-picture UI when the user interacts with the activity currently in picture-in-picture mode. This number may change if the global configuration changes (ie. if the device is plugged into an external display), but will always be at least three.

Returns

`int`

### getMediaController

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [MediaController](/reference/android/media/session/MediaController) getMediaController ()

Gets the controller which should be receiving media key and volume events while this activity is in the foreground.

Returns

`[MediaController](/reference/android/media/session/MediaController)`

The controller which should receive events.

**See also:**

*   `[setMediaController(android.media.session.MediaController)](/reference/android/app/Activity#setMediaController\(android.media.session.MediaController\))`

### getMenuInflater

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [MenuInflater](/reference/android/view/MenuInflater) getMenuInflater ()

Returns a `[MenuInflater](/reference/android/view/MenuInflater)` with this context.

Returns

`[MenuInflater](/reference/android/view/MenuInflater)`

This value cannot be `null`.

### getOnBackInvokedDispatcher

Added in [API level 33](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [OnBackInvokedDispatcher](/reference/android/window/OnBackInvokedDispatcher) getOnBackInvokedDispatcher ()

Returns the `[OnBackInvokedDispatcher](/reference/android/window/OnBackInvokedDispatcher)` instance associated with the window that this activity is attached to.

Returns

`[OnBackInvokedDispatcher](/reference/android/window/OnBackInvokedDispatcher)`

This value cannot be `null`.

Throws

`[IllegalStateException](/reference/java/lang/IllegalStateException)`

if this Activity is not visual.

### getParent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [Activity](/reference/android/app/Activity) getParent ()

**This method was deprecated in API level 35.**  
`[ActivityGroup](/reference/android/app/ActivityGroup)` is deprecated.

Returns the parent `[Activity](/reference/android/app/Activity)` if this is a child `[Activity](/reference/android/app/Activity)` of an `[ActivityGroup](/reference/android/app/ActivityGroup)`.

Returns

`[Activity](/reference/android/app/Activity)`

### getParentActivityIntent

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Intent](/reference/android/content/Intent) getParentActivityIntent ()

Obtain an `[Intent](/reference/android/content/Intent)` that will launch an explicit target activity specified by this activity's logical parent. The logical parent is named in the application's manifest by the `[parentActivityName](/reference/android/R.attr#parentActivityName)` attribute. Activity subclasses may override this method to modify the Intent returned by super.getParentActivityIntent() or to implement a different mechanism of retrieving the parent intent entirely.

Returns

`[Intent](/reference/android/content/Intent)`

a new Intent targeting the defined parent of this activity or null if there is no valid parent.

### getPreferences

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [SharedPreferences](/reference/android/content/SharedPreferences) getPreferences (int mode)

Retrieve a `[SharedPreferences](/reference/android/content/SharedPreferences)` object for accessing preferences that are private to this activity. This simply calls the underlying `[ContextWrapper.getSharedPreferences(java.lang.String, int)](/reference/android/content/ContextWrapper#getSharedPreferences\(java.lang.String,%20int\))` method by passing in this activity's class name as the preferences name.

Parameters

`mode`

`int`: Operating mode. Use `[Context.MODE_PRIVATE](/reference/android/content/Context#MODE_PRIVATE)` for the default operation. Value is either `0` or a combination of `[Context.MODE_PRIVATE](/reference/android/content/Context#MODE_PRIVATE)`, `[Context.MODE_WORLD_READABLE](/reference/android/content/Context#MODE_WORLD_READABLE)`, `[Context.MODE_WORLD_WRITEABLE](/reference/android/content/Context#MODE_WORLD_WRITEABLE)`, and `[Context.MODE_MULTI_PROCESS](/reference/android/content/Context#MODE_MULTI_PROCESS)`

Returns

`[SharedPreferences](/reference/android/content/SharedPreferences)`

Returns the single SharedPreferences instance that can be used to retrieve and modify the preference values.

### getReferrer

Added in [API level 22](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Uri](/reference/android/net/Uri) getReferrer ()

Return information about who launched this activity. If the launching Intent contains an `[Intent.EXTRA_REFERRER](/reference/android/content/Intent#EXTRA_REFERRER)`, that will be returned as-is; otherwise, if known, an `[android-app:](/reference/android/content/Intent#URI_ANDROID_APP_SCHEME)` referrer URI containing the package name that started the Intent will be returned. This may return null if no referrer can be identified -- it is neither explicitly specified, nor is it known which application package was involved.

If called while inside the handling of `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, this function will return the referrer that submitted that new intent to the activity only after `[setIntent(android.content.Intent)](/reference/android/app/Activity#setIntent\(android.content.Intent\))` is called with the provided intent.

Note that this is _not_ a security feature -- you can not trust the referrer information, applications can spoof it.

Returns

`[Uri](/reference/android/net/Uri)`

### getRequestedOrientation

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getRequestedOrientation ()

Returns the current requested orientation of the activity, which is either the orientation requested in the app manifest or the last orientation given to `[setRequestedOrientation(int)](/reference/android/app/Activity#setRequestedOrientation\(int\))`.

**Note:**

*   To improve the layout of apps on form factors with smallest width >= 600dp, the system ignores calls to this method for apps that target Android 16 (API level 36) or higher.
*   Device manufacturers can configure devices to ignore calls to this method to improve the layout of orientation-restricted apps.
*   On devices with Android 16 (API level 36) or higher installed, virtual device owners (select trusted and privileged apps) can optimize app layout on displays they manage by ignoring calls to this method. See also [Companion app streaming](https://source.android.com/docs/core/permissions/app-streaming).

See [Device compatibility mode](/guide/practices/device-compatibility-mode).

Returns

`int`

Returns an orientation constant as used in `[ActivityInfo.screenOrientation](/reference/android/content/pm/ActivityInfo#screenOrientation)`. Value is android.content.pm.ActivityInfo.SCREEN\_ORIENTATION\_UNSET, `[ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_UNSPECIFIED)`, `[ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_USER](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_USER)`, `[ActivityInfo.SCREEN_ORIENTATION_BEHIND](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_BEHIND)`, `[ActivityInfo.SCREEN_ORIENTATION_SENSOR](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_SENSOR)`, `[ActivityInfo.SCREEN_ORIENTATION_NOSENSOR](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_NOSENSOR)`, `[ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_SENSOR_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_SENSOR_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_REVERSE_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_REVERSE_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_FULL_SENSOR)`, `[ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_USER_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_USER_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_FULL_USER](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_FULL_USER)`, or `[ActivityInfo.SCREEN_ORIENTATION_LOCKED](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_LOCKED)`

### getSearchEvent

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [SearchEvent](/reference/android/view/SearchEvent) getSearchEvent ()

During the onSearchRequested() callbacks, this function will return the `[SearchEvent](/reference/android/view/SearchEvent)` that triggered the callback, if it exists.

Returns

`[SearchEvent](/reference/android/view/SearchEvent)`

SearchEvent The SearchEvent that triggered the `[onSearchRequested()](/reference/android/app/Activity#onSearchRequested\(\))` callback.

### getSplashScreen

Added in [API level 31](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [SplashScreen](/reference/android/window/SplashScreen) getSplashScreen ()

Get the interface that activity use to talk to the splash screen.

Returns

`[SplashScreen](/reference/android/window/SplashScreen)`

This value cannot be `null`.

**See also:**

*   `[SplashScreen](/reference/android/window/SplashScreen)`

### getSystemService

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Object](/reference/java/lang/Object) getSystemService ([String](/reference/java/lang/String) name)

Return the handle to a system-level service by name. The class of the returned object varies by the requested name. Currently available names are:

`[WINDOW_SERVICE](/reference/android/content/Context#WINDOW_SERVICE)` ("window")

The top-level window manager in which you can place custom windows. The returned object is a `[WindowManager](/reference/android/view/WindowManager)`. Must only be obtained from a visual context such as Activity or a Context created with `[createWindowContext(int, android.os.Bundle)](/reference/android/content/Context#createWindowContext\(int,%20android.os.Bundle\))`, which are adjusted to the configuration and visual bounds of an area on screen.

`[LAYOUT_INFLATER_SERVICE](/reference/android/content/Context#LAYOUT_INFLATER_SERVICE)` ("layout\_inflater")

A `[LayoutInflater](/reference/android/view/LayoutInflater)` for inflating layout resources in this context. Must only be obtained from a visual context such as Activity or a Context created with `[createWindowContext(int, android.os.Bundle)](/reference/android/content/Context#createWindowContext\(int,%20android.os.Bundle\))`, which are adjusted to the configuration and visual bounds of an area on screen.

`[ACTIVITY_SERVICE](/reference/android/content/Context#ACTIVITY_SERVICE)` ("activity")

A `[ActivityManager](/reference/android/app/ActivityManager)` for interacting with the global activity state of the system.

`[WALLPAPER_SERVICE](/reference/android/content/Context#WALLPAPER_SERVICE)` ("wallpaper")

A `[WallpaperService](/reference/android/service/wallpaper/WallpaperService)` for accessing wallpapers in this context. Must only be obtained from a visual context such as Activity or a Context created with `[createWindowContext(int, android.os.Bundle)](/reference/android/content/Context#createWindowContext\(int,%20android.os.Bundle\))`, which are adjusted to the configuration and visual bounds of an area on screen.

`[POWER_SERVICE](/reference/android/content/Context#POWER_SERVICE)` ("power")

A `[PowerManager](/reference/android/os/PowerManager)` for controlling power management.

`[ALARM_SERVICE](/reference/android/content/Context#ALARM_SERVICE)` ("alarm")

A `[AlarmManager](/reference/android/app/AlarmManager)` for receiving intents at the time of your choosing.

`[NOTIFICATION_SERVICE](/reference/android/content/Context#NOTIFICATION_SERVICE)` ("notification")

A `[NotificationManager](/reference/android/app/NotificationManager)` for informing the user of background events.

`[KEYGUARD_SERVICE](/reference/android/content/Context#KEYGUARD_SERVICE)` ("keyguard")

A `[KeyguardManager](/reference/android/app/KeyguardManager)` for controlling keyguard.

`[LOCATION_SERVICE](/reference/android/content/Context#LOCATION_SERVICE)` ("location")

A `[LocationManager](/reference/android/location/LocationManager)` for controlling location (e.g., GPS) updates.

`[SEARCH_SERVICE](/reference/android/content/Context#SEARCH_SERVICE)` ("search")

A `[SearchManager](/reference/android/app/SearchManager)` for handling search.

`[VIBRATOR_MANAGER_SERVICE](/reference/android/content/Context#VIBRATOR_MANAGER_SERVICE)` ("vibrator\_manager")

A `[VibratorManager](/reference/android/os/VibratorManager)` for accessing the device vibrators, interacting with individual ones and playing synchronized effects on multiple vibrators.

`[VIBRATOR_SERVICE](/reference/android/content/Context#VIBRATOR_SERVICE)` ("vibrator")

A `[Vibrator](/reference/android/os/Vibrator)` for interacting with the vibrator hardware.

`[CONNECTIVITY_SERVICE](/reference/android/content/Context#CONNECTIVITY_SERVICE)` ("connectivity")

A `[ConnectivityManager](/reference/android/net/ConnectivityManager)` for handling management of network connections.

`[IPSEC_SERVICE](/reference/android/content/Context#IPSEC_SERVICE)` ("ipsec")

A `[IpSecManager](/reference/android/net/IpSecManager)` for managing IPSec on sockets and networks.

`[WIFI_SERVICE](/reference/android/content/Context#WIFI_SERVICE)` ("wifi")

A `[WifiManager](/reference/android/net/wifi/WifiManager)` for management of Wi-Fi connectivity. On releases before Android 7, it should only be obtained from an application context, and not from any other derived context to avoid memory leaks within the calling process.

`[WIFI_AWARE_SERVICE](/reference/android/content/Context#WIFI_AWARE_SERVICE)` ("wifiaware")

A `[WifiAwareManager](/reference/android/net/wifi/aware/WifiAwareManager)` for management of Wi-Fi Aware discovery and connectivity.

`[WIFI_P2P_SERVICE](/reference/android/content/Context#WIFI_P2P_SERVICE)` ("wifip2p")

A `[WifiP2pManager](/reference/android/net/wifi/p2p/WifiP2pManager)` for management of Wi-Fi Direct connectivity.

`[INPUT_METHOD_SERVICE](/reference/android/content/Context#INPUT_METHOD_SERVICE)` ("input\_method")

An `[InputMethodManager](/reference/android/view/inputmethod/InputMethodManager)` for management of input methods.

`[UI_MODE_SERVICE](/reference/android/content/Context#UI_MODE_SERVICE)` ("uimode")

An `[UiModeManager](/reference/android/app/UiModeManager)` for controlling UI modes.

`[DOWNLOAD_SERVICE](/reference/android/content/Context#DOWNLOAD_SERVICE)` ("download")

A `[DownloadManager](/reference/android/app/DownloadManager)` for requesting HTTP downloads

`[BATTERY_SERVICE](/reference/android/content/Context#BATTERY_SERVICE)` ("batterymanager")

A `[BatteryManager](/reference/android/os/BatteryManager)` for managing battery state

`[JOB_SCHEDULER_SERVICE](/reference/android/content/Context#JOB_SCHEDULER_SERVICE)` ("taskmanager")

A `[JobScheduler](/reference/android/app/job/JobScheduler)` for managing scheduled tasks

`[NETWORK_STATS_SERVICE](/reference/android/content/Context#NETWORK_STATS_SERVICE)` ("netstats")

A `[NetworkStatsManager](/reference/android/app/usage/NetworkStatsManager)` for querying network usage statistics.

`[HARDWARE_PROPERTIES_SERVICE](/reference/android/content/Context#HARDWARE_PROPERTIES_SERVICE)` ("hardware\_properties")

A `[HardwarePropertiesManager](/reference/android/os/HardwarePropertiesManager)` for accessing hardware properties.

`[DOMAIN_VERIFICATION_SERVICE](/reference/android/content/Context#DOMAIN_VERIFICATION_SERVICE)` ("domain\_verification")

A `[DomainVerificationManager](/reference/android/content/pm/verify/domain/DomainVerificationManager)` for accessing web domain approval state.

`[DISPLAY_HASH_SERVICE](/reference/android/content/Context#DISPLAY_HASH_SERVICE)` ("display\_hash")

A `[DisplayHashManager](/reference/android/view/displayhash/DisplayHashManager)` for management of display hashes.

`[ERROR(/#AUTHENTICATION_POLICY_SERVICE)](/)` ("authentication\_policy")

A `[ERROR(/android.security.authenticationpolicy.AuthenticationPolicyManager)](/)` for managing authentication related policies on the device.

Note: System services obtained via this API may be closely associated with the Context in which they are obtained from. In general, do not share the service objects between various different contexts (Activities, Applications, Services, Providers, etc.)

Note: Instant apps, for which `[PackageManager.isInstantApp()](/reference/android/content/pm/PackageManager#isInstantApp\(\))` returns true, don't have access to the following system services: `[DEVICE_POLICY_SERVICE](/reference/android/content/Context#DEVICE_POLICY_SERVICE)`, `[FINGERPRINT_SERVICE](/reference/android/content/Context#FINGERPRINT_SERVICE)`, `[KEYGUARD_SERVICE](/reference/android/content/Context#KEYGUARD_SERVICE)`, `[SHORTCUT_SERVICE](/reference/android/content/Context#SHORTCUT_SERVICE)`, `[USB_SERVICE](/reference/android/content/Context#USB_SERVICE)`, `[WALLPAPER_SERVICE](/reference/android/content/Context#WALLPAPER_SERVICE)`, `[WIFI_P2P_SERVICE](/reference/android/content/Context#WIFI_P2P_SERVICE)`, `[WIFI_SERVICE](/reference/android/content/Context#WIFI_SERVICE)`, `[WIFI_AWARE_SERVICE](/reference/android/content/Context#WIFI_AWARE_SERVICE)`. For these services this method will return `null`. Generally, if you are running as an instant app you should always check whether the result of this method is `null`.

Note: When implementing this method, keep in mind that new services can be added on newer Android releases, so if you're looking for just the explicit names mentioned above, make sure to return `null` when you don't recognize the name — if you throw a `[RuntimeException](/reference/java/lang/RuntimeException)` exception instead, your app might break on new Android releases.

Parameters

`name`

`String`: Value is `[Context.POWER_SERVICE](/reference/android/content/Context#POWER_SERVICE)`, `[Context.WINDOW_SERVICE](/reference/android/content/Context#WINDOW_SERVICE)`, `[Context.LAYOUT_INFLATER_SERVICE](/reference/android/content/Context#LAYOUT_INFLATER_SERVICE)`, `[Context.ACCOUNT_SERVICE](/reference/android/content/Context#ACCOUNT_SERVICE)`, `[Context.ACTIVITY_SERVICE](/reference/android/content/Context#ACTIVITY_SERVICE)`, `[Context.ALARM_SERVICE](/reference/android/content/Context#ALARM_SERVICE)`, `[Context.NOTIFICATION_SERVICE](/reference/android/content/Context#NOTIFICATION_SERVICE)`, `[Context.ACCESSIBILITY_SERVICE](/reference/android/content/Context#ACCESSIBILITY_SERVICE)`, `[Context.CAPTIONING_SERVICE](/reference/android/content/Context#CAPTIONING_SERVICE)`, `[Context.KEYGUARD_SERVICE](/reference/android/content/Context#KEYGUARD_SERVICE)`, `[Context.LOCATION_SERVICE](/reference/android/content/Context#LOCATION_SERVICE)`, `[Context.HEALTHCONNECT_SERVICE](/reference/android/content/Context#HEALTHCONNECT_SERVICE)`, `[Context.SEARCH_SERVICE](/reference/android/content/Context#SEARCH_SERVICE)`, `[Context.SENSOR_SERVICE](/reference/android/content/Context#SENSOR_SERVICE)`, android.content.Context.SENSOR\_PRIVACY\_SERVICE, `[Context.STORAGE_SERVICE](/reference/android/content/Context#STORAGE_SERVICE)`, `[Context.STORAGE_STATS_SERVICE](/reference/android/content/Context#STORAGE_STATS_SERVICE)`, `[Context.WALLPAPER_SERVICE](/reference/android/content/Context#WALLPAPER_SERVICE)`, `[Context.VIBRATOR_MANAGER_SERVICE](/reference/android/content/Context#VIBRATOR_MANAGER_SERVICE)`, `[Context.VIBRATOR_SERVICE](/reference/android/content/Context#VIBRATOR_SERVICE)`, android.content.Context.THREAD\_NETWORK\_SERVICE, `[Context.CONNECTIVITY_SERVICE](/reference/android/content/Context#CONNECTIVITY_SERVICE)`, `[Context.TETHERING_SERVICE](/reference/android/content/Context#TETHERING_SERVICE)`, android.content.Context.PAC\_PROXY\_SERVICE, android.content.Context.VCN\_MANAGEMENT\_SERVICE, `[Context.IPSEC_SERVICE](/reference/android/content/Context#IPSEC_SERVICE)`, `[Context.VPN_MANAGEMENT_SERVICE](/reference/android/content/Context#VPN_MANAGEMENT_SERVICE)`, android.content.Context.TEST\_NETWORK\_SERVICE, `[Context.NETWORK_STATS_SERVICE](/reference/android/content/Context#NETWORK_STATS_SERVICE)`, `[Context.WIFI_SERVICE](/reference/android/content/Context#WIFI_SERVICE)`, `[Context.WIFI_AWARE_SERVICE](/reference/android/content/Context#WIFI_AWARE_SERVICE)`, `[Context.WIFI_P2P_SERVICE](/reference/android/content/Context#WIFI_P2P_SERVICE)`, android.content.Context.WIFI\_SCANNING\_SERVICE, `[Context.WIFI_RTT_RANGING_SERVICE](/reference/android/content/Context#WIFI_RTT_RANGING_SERVICE)`, android.content.Context.WIFI\_USD\_SERVICE, `[Context.NSD_SERVICE](/reference/android/content/Context#NSD_SERVICE)`, `[Context.AUDIO_SERVICE](/reference/android/content/Context#AUDIO_SERVICE)`, android.content.Context.AUDIO\_DEVICE\_VOLUME\_SERVICE, android.content.Context.AUTH\_SERVICE, `[Context.FINGERPRINT_SERVICE](/reference/android/content/Context#FINGERPRINT_SERVICE)`, `[Context.BIOMETRIC_SERVICE](/reference/android/content/Context#BIOMETRIC_SERVICE)`, android.content.Context.AUTHENTICATION\_POLICY\_SERVICE, `[Context.MEDIA_ROUTER_SERVICE](/reference/android/content/Context#MEDIA_ROUTER_SERVICE)`, `[Context.TELEPHONY_SERVICE](/reference/android/content/Context#TELEPHONY_SERVICE)`, `[Context.TELEPHONY_SUBSCRIPTION_SERVICE](/reference/android/content/Context#TELEPHONY_SUBSCRIPTION_SERVICE)`, `[Context.TELEPHONY_PHONE_NUMBER_SERVICE](/reference/android/content/Context#TELEPHONY_PHONE_NUMBER_SERVICE)`, `[Context.CARRIER_CONFIG_SERVICE](/reference/android/content/Context#CARRIER_CONFIG_SERVICE)`, `[Context.EUICC_SERVICE](/reference/android/content/Context#EUICC_SERVICE)`, `[Context.TELECOM_SERVICE](/reference/android/content/Context#TELECOM_SERVICE)`, `[Context.CLIPBOARD_SERVICE](/reference/android/content/Context#CLIPBOARD_SERVICE)`, `[Context.INPUT_METHOD_SERVICE](/reference/android/content/Context#INPUT_METHOD_SERVICE)`, `[Context.TEXT_SERVICES_MANAGER_SERVICE](/reference/android/content/Context#TEXT_SERVICES_MANAGER_SERVICE)`, `[Context.TEXT_CLASSIFICATION_SERVICE](/reference/android/content/Context#TEXT_CLASSIFICATION_SERVICE)`, `[Context.APPWIDGET_SERVICE](/reference/android/content/Context#APPWIDGET_SERVICE)`, android.content.Context.REBOOT\_READINESS\_SERVICE, android.content.Context.ROLLBACK\_SERVICE, `[Context.DROPBOX_SERVICE](/reference/android/content/Context#DROPBOX_SERVICE)`, `[Context.DEVICE_POLICY_SERVICE](/reference/android/content/Context#DEVICE_POLICY_SERVICE)`, `[Context.UI_MODE_SERVICE](/reference/android/content/Context#UI_MODE_SERVICE)`, `[Context.DOWNLOAD_SERVICE](/reference/android/content/Context#DOWNLOAD_SERVICE)`, `[Context.NFC_SERVICE](/reference/android/content/Context#NFC_SERVICE)`, `[Context.BLUETOOTH_SERVICE](/reference/android/content/Context#BLUETOOTH_SERVICE)`, `[Context.USB_SERVICE](/reference/android/content/Context#USB_SERVICE)`, `[Context.LAUNCHER_APPS_SERVICE](/reference/android/content/Context#LAUNCHER_APPS_SERVICE)`, android.content.Context.SERIAL\_SERVICE, `[Context.INPUT_SERVICE](/reference/android/content/Context#INPUT_SERVICE)`, `[Context.DISPLAY_SERVICE](/reference/android/content/Context#DISPLAY_SERVICE)`, `[Context.USER_SERVICE](/reference/android/content/Context#USER_SERVICE)`, `[Context.RESTRICTIONS_SERVICE](/reference/android/content/Context#RESTRICTIONS_SERVICE)`, `[Context.APP_OPS_SERVICE](/reference/android/content/Context#APP_OPS_SERVICE)`, `[Context.ROLE_SERVICE](/reference/android/content/Context#ROLE_SERVICE)`, `[Context.CAMERA_SERVICE](/reference/android/content/Context#CAMERA_SERVICE)`, `[Context.PRINT_SERVICE](/reference/android/content/Context#PRINT_SERVICE)`, `[Context.CONSUMER_IR_SERVICE](/reference/android/content/Context#CONSUMER_IR_SERVICE)`, `[Context.TV_INTERACTIVE_APP_SERVICE](/reference/android/content/Context#TV_INTERACTIVE_APP_SERVICE)`, `[Context.TV_INPUT_SERVICE](/reference/android/content/Context#TV_INPUT_SERVICE)`, `[Context.USAGE_STATS_SERVICE](/reference/android/content/Context#USAGE_STATS_SERVICE)`, `[Context.MEDIA_SESSION_SERVICE](/reference/android/content/Context#MEDIA_SESSION_SERVICE)`, `[Context.MEDIA_COMMUNICATION_SERVICE](/reference/android/content/Context#MEDIA_COMMUNICATION_SERVICE)`, `[Context.BATTERY_SERVICE](/reference/android/content/Context#BATTERY_SERVICE)`, `[Context.JOB_SCHEDULER_SERVICE](/reference/android/content/Context#JOB_SCHEDULER_SERVICE)`, `[Context.PERSISTENT_DATA_BLOCK_SERVICE](/reference/android/content/Context#PERSISTENT_DATA_BLOCK_SERVICE)`, `[Context.MEDIA_PROJECTION_SERVICE](/reference/android/content/Context#MEDIA_PROJECTION_SERVICE)`, `[Context.MIDI_SERVICE](/reference/android/content/Context#MIDI_SERVICE)`, android.content.Context.RADIO\_SERVICE, `[Context.HARDWARE_PROPERTIES_SERVICE](/reference/android/content/Context#HARDWARE_PROPERTIES_SERVICE)`, `[Context.SHORTCUT_SERVICE](/reference/android/content/Context#SHORTCUT_SERVICE)`, `[Context.SYSTEM_HEALTH_SERVICE](/reference/android/content/Context#SYSTEM_HEALTH_SERVICE)`, `[Context.COMPANION_DEVICE_SERVICE](/reference/android/content/Context#COMPANION_DEVICE_SERVICE)`, `[Context.VIRTUAL_DEVICE_SERVICE](/reference/android/content/Context#VIRTUAL_DEVICE_SERVICE)`, `[Context.CROSS_PROFILE_APPS_SERVICE](/reference/android/content/Context#CROSS_PROFILE_APPS_SERVICE)`, android.content.Context.PERMISSION\_SERVICE, android.content.Context.LIGHTS\_SERVICE, `[Context.LOCALE_SERVICE](/reference/android/content/Context#LOCALE_SERVICE)`, android.content.Context.UWB\_SERVICE, `[Context.MEDIA_METRICS_SERVICE](/reference/android/content/Context#MEDIA_METRICS_SERVICE)`, `[Context.DISPLAY_HASH_SERVICE](/reference/android/content/Context#DISPLAY_HASH_SERVICE)`, `[Context.CREDENTIAL_SERVICE](/reference/android/content/Context#CREDENTIAL_SERVICE)`, `[Context.DEVICE_LOCK_SERVICE](/reference/android/content/Context#DEVICE_LOCK_SERVICE)`, android.content.Context.VIRTUALIZATION\_SERVICE, `[Context.GRAMMATICAL_INFLECTION_SERVICE](/reference/android/content/Context#GRAMMATICAL_INFLECTION_SERVICE)`, `[Context.SECURITY_STATE_SERVICE](/reference/android/content/Context#SECURITY_STATE_SERVICE)`, `[Context.CONTACT_KEYS_SERVICE](/reference/android/content/Context#CONTACT_KEYS_SERVICE)`, android.content.Context.RANGING\_SERVICE, `[Context.MEDIA_QUALITY_SERVICE](/reference/android/content/Context#MEDIA_QUALITY_SERVICE)`, `[Context.ADVANCED_PROTECTION_SERVICE](/reference/android/content/Context#ADVANCED_PROTECTION_SERVICE)`, or android.content.Context.ANOMALY\_DETECTOR\_SERVICE This value cannot be `null`.

Returns

`[Object](/reference/java/lang/Object)`

The service or `null` if the name does not exist.

### getTaskId

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public int getTaskId ()

Return the identifier of the task this activity is in. This identifier will remain the same for the lifetime of the activity.

Returns

`int`

Task identifier, an opaque integer.

### getTitle

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [CharSequence](/reference/java/lang/CharSequence) getTitle ()

Returns

`[CharSequence](/reference/java/lang/CharSequence)`

### getTitleColor

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final int getTitleColor ()

Returns

`int`

### getVoiceInteractor

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [VoiceInteractor](/reference/android/app/VoiceInteractor) getVoiceInteractor ()

Retrieve the active `[VoiceInteractor](/reference/android/app/VoiceInteractor)` that the user is going through to interact with this activity.

Returns

`[VoiceInteractor](/reference/android/app/VoiceInteractor)`

### getVolumeControlStream

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final int getVolumeControlStream ()

Gets the suggested audio stream whose volume should be changed by the hardware volume controls.

Returns

`int`

The suggested audio stream type whose volume should be changed by the hardware volume controls.

**See also:**

*   `[setVolumeControlStream(int)](/reference/android/app/Activity#setVolumeControlStream\(int\))`

### getWindow

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Window](/reference/android/view/Window) getWindow ()

Retrieve the current `[Window](/reference/android/view/Window)` for the activity. This can be used to directly access parts of the Window API that are not available through Activity/Screen.

Returns

`[Window](/reference/android/view/Window)`

Window The current window, or null if the activity is not visual.

### getWindowManager

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [WindowManager](/reference/android/view/WindowManager) getWindowManager ()

Retrieve the window manager for showing custom windows.

Returns

`[WindowManager](/reference/android/view/WindowManager)`

### hasWindowFocus

Added in [API level 3](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean hasWindowFocus ()

Returns true if this activity's _main_ window currently has window focus. Note that this is not the same as the view itself having focus.

Returns

`boolean`

True if this activity's main window currently has window focus.

**See also:**

*   `[onWindowAttributesChanged(android.view.WindowManager.LayoutParams)](/reference/android/app/Activity#onWindowAttributesChanged\(android.view.WindowManager.LayoutParams\))`

### invalidateOptionsMenu

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void invalidateOptionsMenu ()

Declare that the options menu has changed, so should be recreated. The `[onCreateOptionsMenu(android.view.Menu)](/reference/android/app/Activity#onCreateOptionsMenu\(android.view.Menu\))` method will be called the next time it needs to be displayed.

### isActivityTransitionRunning

Added in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isActivityTransitionRunning ()

Returns whether there are any activity transitions currently running on this activity. A return value of `true` can mean that either an enter or exit transition is running, including whether the background of the activity is animating as a part of that transition.

Returns

`boolean`

true if a transition is currently running on this activity, false otherwise.

### isChangingConfigurations

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isChangingConfigurations ()

Check to see whether this activity is in the process of being destroyed in order to be recreated with a new configuration.

This is often used in `[onStop()](/reference/android/app/Activity#onStop\(\))` to determine whether the state needs to be cleaned up or will be passed on to the next instance of the activity via `[onRetainNonConfigurationInstance()](/reference/android/app/Activity#onRetainNonConfigurationInstance\(\))`. However, if the activity has already been in the background as stopped, and then gets recreated with different configuration, there won't be another `[onPause()](/reference/android/app/Activity#onPause\(\))` or `[onStop()](/reference/android/app/Activity#onStop\(\))` with this API returning `true`.

For example, if an activity that is not handling size configuration change is first minimized, and then get rotated with the display, it should first receive `[onPause()](/reference/android/app/Activity#onPause\(\))` and `[onStop()](/reference/android/app/Activity#onStop\(\))` with this API returning `false`, and then receive `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` with this API returning `true`.

Returns

`boolean`

If the activity is being torn down in order to be recreated with a new configuration, returns true; else returns false.

### isChild

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final boolean isChild ()

**This method was deprecated in API level 35.**  
`[ActivityGroup](/reference/android/app/ActivityGroup)` is deprecated.

Whether this is a child `[Activity](/reference/android/app/Activity)` of an `[ActivityGroup](/reference/android/app/ActivityGroup)`.

Returns

`boolean`

### isDestroyed

Added in [API level 17](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isDestroyed ()

Returns true if the final `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` call has been made on the Activity, so this instance is now dead.

Returns

`boolean`

### isFinishing

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isFinishing ()

Check to see whether this activity is in the process of finishing, either because you called `[finish()](/reference/android/app/Activity#finish\(\))` on it or someone else has requested that it finished.

This is often used in `[onPause()](/reference/android/app/Activity#onPause\(\))` to determine whether the activity is simply pausing or completely finishing. However, if the finish request is made after the activity has already been paused/stopped, there won't be another `[onPause()](/reference/android/app/Activity#onPause\(\))` or `[onStop()](/reference/android/app/Activity#onStop\(\))` with this API returning `true`.

For example, if an activity is first minimized, and then gets killed in background, it should first receive `[onPause()](/reference/android/app/Activity#onPause\(\))` and `[onStop()](/reference/android/app/Activity#onStop\(\))` with this API returning `false`, and then receive `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` with this API returning `true`.

Returns

`boolean`

**See also:**

*   `[finish()](/reference/android/app/Activity#finish\(\))`

### isImmersive

Added in [API level 18](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isImmersive ()

Bit indicating that this activity is "immersive" and should not be interrupted by notifications if possible. This value is initially set by the manifest property `android:immersive` but may be changed at runtime by `[setImmersive(boolean)](/reference/android/app/Activity#setImmersive\(boolean\))`.

Returns

`boolean`

**See also:**

*   `[setImmersive(boolean)](/reference/android/app/Activity#setImmersive\(boolean\))`
*   `[ActivityInfo.FLAG_IMMERSIVE](/reference/android/content/pm/ActivityInfo#FLAG_IMMERSIVE)`

### isInMultiWindowMode

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isInMultiWindowMode ()

Returns true if the activity is currently in multi-window mode.

Returns

`boolean`

True if the activity is in multi-window mode.

**See also:**

*   `[R.attr.resizeableActivity](/reference/android/R.attr#resizeableActivity)`

### isInPictureInPictureMode

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isInPictureInPictureMode ()

Returns true if the activity is currently in picture-in-picture mode.

Returns

`boolean`

True if the activity is in picture-in-picture mode.

**See also:**

*   `[R.attr.supportsPictureInPicture](/reference/android/R.attr#supportsPictureInPicture)`

### isLaunchedFromBubble

Added in [API level 31](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isLaunchedFromBubble ()

Indicates whether this activity is launched from a bubble. A bubble is a floating shortcut on the screen that expands to show an activity. If your activity can be used normally or as a bubble, you might use this method to check if the activity is bubbled to modify any behaviour that might be different between the normal activity and the bubbled activity. For example, if you normally cancel the notification associated with the activity when you open the activity, you might not want to do that when you're bubbled as that would remove the bubble.

Returns

`boolean`

`true` if the activity is launched from a bubble.

**See also:**

*   `[Notification.Builder.setBubbleMetadata(Notification.BubbleMetadata)](/reference/android/app/Notification.Builder#setBubbleMetadata\(android.app.Notification.BubbleMetadata\))`
*   `[Notification.BubbleMetadata.Builder.Builder(String)](/reference/android/app/Notification.BubbleMetadata.Builder#Builder\(java.lang.String\))`

### isLocalVoiceInteractionSupported

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isLocalVoiceInteractionSupported ()

Queries whether the currently enabled voice interaction service supports returning a voice interactor for use by the activity. This is valid only for the duration of the activity.

Returns

`boolean`

whether the current voice interaction service supports local voice interaction

### isTaskRoot

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isTaskRoot ()

Return whether this activity is the root of a task. The root is the first activity in a task.

Returns

`boolean`

True if this is the root activity, else false.

### isVoiceInteraction

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isVoiceInteraction ()

Check whether this activity is running as part of a voice interaction with the user. If true, it should perform its interaction with the user through the `[VoiceInteractor](/reference/android/app/VoiceInteractor)` returned by `[getVoiceInteractor()](/reference/android/app/Activity#getVoiceInteractor\(\))`.

Returns

`boolean`

### isVoiceInteractionRoot

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean isVoiceInteractionRoot ()

Like `[isVoiceInteraction()](/reference/android/app/Activity#isVoiceInteraction\(\))`, but only returns `true` if this is also the root of a voice interaction. That is, returns `true` if this activity was directly started by the voice interaction service as the initiation of a voice interaction. Otherwise, for example if it was started by another activity while under voice interaction, returns `false`. If the activity `[launchMode](/reference/android/R.styleable#AndroidManifestActivity_launchMode)` is `singleTask`, it forces the activity to launch in a new task, separate from the one that started it. Therefore, there is no longer a relationship between them, and `[isVoiceInteractionRoot()](/reference/android/app/Activity#isVoiceInteractionRoot\(\))` return `false` in this case.

Returns

`boolean`

### managedQuery

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final [Cursor](/reference/android/database/Cursor) managedQuery ([Uri](/reference/android/net/Uri) uri, 
                [String\[\]](/reference/java/lang/String) projection, 
                [String](/reference/java/lang/String) selection, 
                [String\[\]](/reference/java/lang/String) selectionArgs, 
                [String](/reference/java/lang/String) sortOrder)

**This method was deprecated in API level 15.**  
Use `[CursorLoader](/reference/android/content/CursorLoader)` instead.

Wrapper around `[ContentResolver.query(android.net.Uri , String[], String, String[], String)](/reference/android/content/ContentResolver#query\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))` that gives the resulting `[Cursor](/reference/android/database/Cursor)` to call `[startManagingCursor(Cursor)](/reference/android/app/Activity#startManagingCursor\(android.database.Cursor\))` so that the activity will manage its lifecycle for you. _If you are targeting `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` or later, consider instead using `[LoaderManager](/reference/android/app/LoaderManager)` instead, available via `[getLoaderManager()](/reference/android/app/Activity#getLoaderManager\(\))`._

**Warning:** Do not call `[Cursor.close()](/reference/android/database/Cursor#close\(\))` on a cursor obtained using this method, because the activity will do that for you at the appropriate time. However, if you call `[stopManagingCursor(Cursor)](/reference/android/app/Activity#stopManagingCursor\(android.database.Cursor\))` on a cursor from a managed query, the system _will not_ automatically close the cursor and, in that case, you must call `[Cursor.close()](/reference/android/database/Cursor#close\(\))`.

Parameters

`uri`

`Uri`: The URI of the content provider to query.

`projection`

`String`: List of columns to return.

`selection`

`String`: SQL WHERE clause.

`selectionArgs`

`String`: The arguments to selection, if any ?s are pesent

`sortOrder`

`String`: SQL ORDER BY clause.

Returns

`[Cursor](/reference/android/database/Cursor)`

The Cursor that was returned by query().

**See also:**

*   `[ContentResolver.query(android.net.Uri, String[], String, String[], String)](/reference/android/content/ContentResolver#query\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))`
*   `[startManagingCursor(Cursor)](/reference/android/app/Activity#startManagingCursor\(android.database.Cursor\))`

### moveTaskToBack

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean moveTaskToBack (boolean nonRoot)

Move the task containing this activity to the back of the activity stack. The activity's order within the task is unchanged.

Parameters

`nonRoot`

`boolean`: If false then this only works if the activity is the root of a task; if true it will work for any activity in a task.

Returns

`boolean`

If the task was moved (or it was already at the back) true is returned, else false.

### navigateUpTo

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean navigateUpTo ([Intent](/reference/android/content/Intent) upIntent)

Navigate from this activity to the activity specified by upIntent, finishing this activity in the process. If the activity indicated by upIntent already exists in the task's history, this activity and all others before the indicated activity in the history stack will be finished.

If the indicated activity does not appear in the history stack, this will finish each activity in this task until the root activity of the task is reached, resulting in an "in-app home" behavior. This can be useful in apps with a complex navigation hierarchy when an activity may be reached by a path not passing through a canonical parent activity.

This method should be used when performing up navigation from within the same task as the destination. If up navigation should cross tasks in some cases, see `[shouldUpRecreateTask(android.content.Intent)](/reference/android/app/Activity#shouldUpRecreateTask\(android.content.Intent\))`.

Parameters

`upIntent`

`Intent`: An intent representing the target destination for up navigation

Returns

`boolean`

true if up navigation successfully reached the activity indicated by upIntent and upIntent was delivered to it. false if an instance of the indicated activity could not be found and this activity was simply finished normally.

### navigateUpToFromChild

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean navigateUpToFromChild ([Activity](/reference/android/app/Activity) child, 
                [Intent](/reference/android/content/Intent) upIntent)

**This method was deprecated in API level 30.**  
Use `[navigateUpTo(android.content.Intent)](/reference/android/app/Activity#navigateUpTo\(android.content.Intent\))` instead.

This is called when a child activity of this one calls its `[navigateUpTo(Intent)](/reference/android/app/Activity#navigateUpTo\(android.content.Intent\))` method. The default implementation simply calls navigateUpTo(upIntent) on this activity (the parent).

Parameters

`child`

`Activity`: The activity making the call.

`upIntent`

`Intent`: An intent representing the target destination for up navigation

Returns

`boolean`

true if up navigation successfully reached the activity indicated by upIntent and upIntent was delivered to it. false if an instance of the indicated activity could not be found and this activity was simply finished normally.

### onActionModeFinished

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onActionModeFinished ([ActionMode](/reference/android/view/ActionMode) mode)

Notifies the activity that an action mode has finished. Activity subclasses overriding this method should call the superclass implementation.  
If you override this method you _must_ call through to the superclass implementation.

Parameters

`mode`

`ActionMode`: The action mode that just finished.

### onActionModeStarted

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onActionModeStarted ([ActionMode](/reference/android/view/ActionMode) mode)

Notifies the Activity that an action mode has been started. Activity subclasses overriding this method should call the superclass implementation.  
If you override this method you _must_ call through to the superclass implementation.

Parameters

`mode`

`ActionMode`: The new action mode.

### onActivityReenter

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onActivityReenter (int resultCode, 
                [Intent](/reference/android/content/Intent) data)

Called when an activity you launched with an activity transition exposes this Activity through a returning activity transition, giving you the resultCode and any additional data from it. This method will only be called if the activity set a result code other than `[RESULT_CANCELED](/reference/android/app/Activity#RESULT_CANCELED)` and it supports activity transitions with `[Window.FEATURE_ACTIVITY_TRANSITIONS](/reference/android/view/Window#FEATURE_ACTIVITY_TRANSITIONS)`.

The purpose of this function is to let the called Activity send a hint about its state so that this underlying Activity can prepare to be exposed. A call to this method does not guarantee that the called Activity has or will be exiting soon. It only indicates that it will expose this Activity's Window and it has some data to pass to prepare it.

Parameters

`resultCode`

`int`: The integer result code returned by the child activity through its setResult().

`data`

`Intent`: An Intent, which can return result data to the caller (various data can be attached to Intent "extras").

### onActivityResult

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onActivityResult (int requestCode, 
                int resultCode, 
                [Intent](/reference/android/content/Intent) data, 
                [ComponentCaller](/reference/android/app/ComponentCaller) caller)

Same as `[onActivityResult(int, int, android.content.Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`, but with an extra parameter for the ComponentCaller instance associated with the app that sent the result.

If you want to retrieve the caller without overriding this method, call `[getCurrentCaller()](/reference/android/app/Activity#getCurrentCaller\(\))` inside your existing `[onActivityResult(int, int, android.content.Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`.

Note that you should only override one `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))` method.

Parameters

`requestCode`

`int`: The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.

`resultCode`

`int`: The integer result code returned by the child activity through its setResult().

`data`

`Intent`: An Intent, which can return result data to the caller (various data can be attached to Intent "extras"). This value may be `null`.

`caller`

`ComponentCaller`: The `[ComponentCaller](/reference/android/app/ComponentCaller)` instance associated with the app that sent the intent. This value cannot be `null`.

### onAttachFragment

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onAttachFragment ([Fragment](/reference/android/app/Fragment) fragment)

**This method was deprecated in API level 28.**  
Use `[FragmentActivity.onAttachFragment(androidx.fragment.app.Fragment)](https://developer.android.com/reference/androidx/fragment/app/FragmentActivity.html#onAttachFragment\(androidx.fragment.app.Fragment\))`

Called when a Fragment is being attached to this activity, immediately after the call to its `[Fragment.onAttach()](/reference/android/app/Fragment#onAttach\(android.app.Activity\))` method and before `[Fragment.onCreate()](/reference/android/app/Fragment#onCreate\(android.os.Bundle\))`.

Parameters

`fragment`

`Fragment`

### onAttachedToWindow

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onAttachedToWindow ()

Called when the main window associated with the activity has been attached to the window manager. See `[View.onAttachedToWindow()](/reference/android/view/View#onAttachedToWindow\(\))` for more information.

**See also:**

*   `[View.onAttachedToWindow()](/reference/android/view/View#onAttachedToWindow\(\))`

### onBackPressed

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 33](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onBackPressed ()

**This method was deprecated in API level 33.**  
Use `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)` or `androidx.activity.OnBackPressedCallback` to handle back navigation instead.

Starting from Android 13 (API level 33), back event handling is moving to an ahead-of-time model and `[Activity.onBackPressed()](/reference/android/app/Activity#onBackPressed\(\))` and `[KeyEvent.KEYCODE_BACK](/reference/android/view/KeyEvent#KEYCODE_BACK)` should not be used to handle back events (back gesture or back button click). Instead, an `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)` should be registered using `[Activity.getOnBackInvokedDispatcher()](/reference/android/app/Activity#getOnBackInvokedDispatcher\(\))` `[.registerOnBackInvokedCallback(priority, callback)](/reference/android/window/OnBackInvokedDispatcher#registerOnBackInvokedCallback\(int,%20android.window.OnBackInvokedCallback\))`.

Called when the activity has detected the user's press of the back key. The default implementation depends on the platform version:

*   On platform versions prior to `[Build.VERSION_CODES.S](/reference/android/os/Build.VERSION_CODES#S)`, it finishes the current activity, but you can override this to do whatever you want.
*   Starting with platform version `[Build.VERSION_CODES.S](/reference/android/os/Build.VERSION_CODES#S)`, for activities that are the root activity of the task and also declare an `[IntentFilter](/reference/android/content/IntentFilter)` with `[Intent.ACTION_MAIN](/reference/android/content/Intent#ACTION_MAIN)` and `[Intent.CATEGORY_LAUNCHER](/reference/android/content/Intent#CATEGORY_LAUNCHER)` in the manifest, the current activity and its task will be moved to the back of the activity stack instead of being finished. Other activities will simply be finished.
    
*   If you target version `[Build.VERSION_CODES.S](/reference/android/os/Build.VERSION_CODES#S)` and override this method, we strongly recommend to call through to the superclass implementation after you finish handling navigation within the app.
    
*   If you target version `[Build.VERSION_CODES.TIRAMISU](/reference/android/os/Build.VERSION_CODES#TIRAMISU)` or later, you should not use this method but register an `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)` on an `[OnBackInvokedDispatcher](/reference/android/window/OnBackInvokedDispatcher)` that you can retrieve using `[getOnBackInvokedDispatcher()](/reference/android/app/Activity#getOnBackInvokedDispatcher\(\))`. You should also set `android:enableOnBackInvokedCallback="true"` in the application manifest.
    
    Alternatively, you can use `androidx.activity.ComponentActivity#getOnBackPressedDispatcher()` for backward compatibility.
    

**See also:**

*   `[moveTaskToBack(boolean)](/reference/android/app/Activity#moveTaskToBack\(boolean\))`

### onConfigurationChanged

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onConfigurationChanged ([Configuration](/reference/android/content/res/Configuration) newConfig)

Called by the system when the device configuration changes while your activity is running. Note that this will only be called if you have selected configurations you would like to handle with the `[R.attr.configChanges](/reference/android/R.attr#configChanges)` attribute in your manifest. If any configuration change occurs that is not selected to be reported by that attribute, then instead of reporting it the system will stop and restart the activity (to have it launched with the new configuration). The only exception is if a size-based configuration is not large enough to be considered significant, in which case the system will not recreate the activity and will instead call this method. For details on this see the documentation on [size-based config change](/guide/topics/resources/runtime-changes).

At the time that this function has been called, your Resources object will have been updated to return resource values matching the new configuration.

Parameters

`newConfig`

`Configuration`: The new device configuration. This value cannot be `null`.

### onContentChanged

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onContentChanged ()

This hook is called whenever the content view of the screen changes (due to a call to `[Window.setContentView](/reference/android/view/Window#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))` or `[Window.addContentView](/reference/android/view/Window#addContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))`).

### onContextItemSelected

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onContextItemSelected ([MenuItem](/reference/android/view/MenuItem) item)

This hook is called whenever an item in a context menu is selected. The default implementation simply returns false to have the normal processing happen (calling the item's Runnable or sending a message to its Handler as appropriate). You can use this method for any items for which you would like to do processing without those other facilities.

Use `[MenuItem.getMenuInfo()](/reference/android/view/MenuItem#getMenuInfo\(\))` to get extra information set by the View that added this menu item.

Derived classes should call through to the base class for it to perform the default menu handling.

Parameters

`item`

`MenuItem`: The context menu item that was selected. This value cannot be `null`.

Returns

`boolean`

boolean Return false to allow normal context menu processing to proceed, true to consume it here.

### onContextMenuClosed

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onContextMenuClosed ([Menu](/reference/android/view/Menu) menu)

This hook is called whenever the context menu is being closed (either by the user canceling the menu with the back/menu button, or when an item is selected).

Parameters

`menu`

`Menu`: The context menu that is being closed. This value cannot be `null`.

### onCreate

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onCreate ([Bundle](/reference/android/os/Bundle) savedInstanceState, 
                [PersistableBundle](/reference/android/os/PersistableBundle) persistentState)

Same as `[onCreate(android.os.Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` but called for those activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`.

Parameters

`savedInstanceState`

`Bundle`: if the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`. **_Note: Otherwise it is null._**

`persistentState`

`PersistableBundle`: if the activity is being re-initialized after previously being shut down or powered off then this Bundle contains the data it most recently supplied to outPersistentState in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`. **_Note: Otherwise it is null._**

**See also:**

*   `[onCreate(android.os.Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`
*   `[onStart()](/reference/android/app/Activity#onStart\(\))`
*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`
*   `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`
*   `[onPostCreate(Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))`

### onCreateContextMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onCreateContextMenu ([ContextMenu](/reference/android/view/ContextMenu) menu, 
                [View](/reference/android/view/View) v, 
                [ContextMenu.ContextMenuInfo](/reference/android/view/ContextMenu.ContextMenuInfo) menuInfo)

Called when a context menu for the `view` is about to be shown. Unlike `[onCreateOptionsMenu(android.view.Menu)](/reference/android/app/Activity#onCreateOptionsMenu\(android.view.Menu\))`, this will be called every time the context menu is about to be shown and should be populated for the view (or item inside the view for `[AdapterView](/reference/android/widget/AdapterView)` subclasses, this can be found in the `menuInfo`)).

Use `[onContextItemSelected(android.view.MenuItem)](/reference/android/app/Activity#onContextItemSelected\(android.view.MenuItem\))` to know when an item has been selected.

It is not safe to hold onto the context menu after this method returns.

Parameters

`menu`

`ContextMenu`: The context menu that is being built

`v`

`View`: The view for which the context menu is being built

`menuInfo`

`ContextMenu.ContextMenuInfo`: Extra information about the item for which the context menu should be shown. This information will vary depending on the class of v.

### onCreateDescription

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [CharSequence](/reference/java/lang/CharSequence) onCreateDescription ()

Generate a new description for this activity. This method is called before stopping the activity and can, if desired, return some textual description of its current state to be displayed to the user.

The default implementation returns null, which will cause you to inherit the description from the previous activity. If all activities return null, generally the label of the top activity will be used as the description.

Returns

`[CharSequence](/reference/java/lang/CharSequence)`

A description of what the user is doing. It should be short and sweet (only a few words).

**See also:**

*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`
*   `[onStop()](/reference/android/app/Activity#onStop\(\))`

### onCreateNavigateUpTaskStack

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onCreateNavigateUpTaskStack ([TaskStackBuilder](/reference/android/app/TaskStackBuilder) builder)

Define the synthetic task stack that will be generated during Up navigation from a different task.

The default implementation of this method adds the parent chain of this activity as specified in the manifest to the supplied `[TaskStackBuilder](/reference/android/app/TaskStackBuilder)`. Applications may choose to override this method to construct the desired task stack in a different way.

This method will be invoked by the default implementation of `[onNavigateUp()](/reference/android/app/Activity#onNavigateUp\(\))` if `[shouldUpRecreateTask(android.content.Intent)](/reference/android/app/Activity#shouldUpRecreateTask\(android.content.Intent\))` returns true when supplied with the intent returned by `[getParentActivityIntent()](/reference/android/app/Activity#getParentActivityIntent\(\))`.

Applications that wish to supply extra Intent parameters to the parent stack defined by the manifest should override `[onPrepareNavigateUpTaskStack(android.app.TaskStackBuilder)](/reference/android/app/Activity#onPrepareNavigateUpTaskStack\(android.app.TaskStackBuilder\))`.

Parameters

`builder`

`TaskStackBuilder`: An empty TaskStackBuilder - the application should add intents representing the desired task stack

### onCreateOptionsMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onCreateOptionsMenu ([Menu](/reference/android/view/Menu) menu)

Initialize the contents of the Activity's standard options menu. You should place your menu items in to menu.

This is only called once, the first time the options menu is displayed. To update the menu every time it is displayed, see `[onPrepareOptionsMenu(Menu)](/reference/android/app/Activity#onPrepareOptionsMenu\(android.view.Menu\))`.

The default implementation populates the menu with standard system menu items. These are placed in the `[Menu.CATEGORY_SYSTEM](/reference/android/view/Menu#CATEGORY_SYSTEM)` group so that they will be correctly ordered with application-defined menu items. Deriving classes should always call through to the base implementation.

You can safely hold on to menu (and any items created from it), making modifications to it as desired, until the next time onCreateOptionsMenu() is called.

When you add items to the menu, you can implement the Activity's `[onOptionsItemSelected(MenuItem)](/reference/android/app/Activity#onOptionsItemSelected\(android.view.MenuItem\))` method to handle them there.

Parameters

`menu`

`Menu`: The options menu in which you place your items.

Returns

`boolean`

You must return true for the menu to be displayed; if you return false it will not be shown.

**See also:**

*   `[onPrepareOptionsMenu(Menu)](/reference/android/app/Activity#onPrepareOptionsMenu\(android.view.Menu\))`
*   `[onOptionsItemSelected(MenuItem)](/reference/android/app/Activity#onOptionsItemSelected\(android.view.MenuItem\))`

### onCreatePanelMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onCreatePanelMenu (int featureId, 
                [Menu](/reference/android/view/Menu) menu)

Default implementation of `[Window.Callback.onCreatePanelMenu(int, Menu)](/reference/android/view/Window.Callback#onCreatePanelMenu\(int,%20android.view.Menu\))` for activities. This calls through to the new `[onCreateOptionsMenu(Menu)](/reference/android/app/Activity#onCreateOptionsMenu\(android.view.Menu\))` method for the `[Window.FEATURE_OPTIONS_PANEL](/reference/android/view/Window#FEATURE_OPTIONS_PANEL)` panel, so that subclasses of Activity don't need to deal with feature codes.

Parameters

`featureId`

`int`: The panel being created.

`menu`

`Menu`: This value cannot be `null`.

Returns

`boolean`

boolean You must return true for the panel to be displayed; if you return false it will not be shown.

### onCreatePanelView

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [View](/reference/android/view/View) onCreatePanelView (int featureId)

Default implementation of `[Window.Callback.onCreatePanelView(int)](/reference/android/view/Window.Callback#onCreatePanelView\(int\))` for activities. This simply returns null so that all panel sub-windows will have the default menu behavior.

Parameters

`featureId`

`int`: Which panel is being created.

Returns

`[View](/reference/android/view/View)`

view The top-level view to place in the panel.

### onCreateThumbnail

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onCreateThumbnail ([Bitmap](/reference/android/graphics/Bitmap) outBitmap, 
                [Canvas](/reference/android/graphics/Canvas) canvas)

**This method was deprecated in API level 28.**  
Method doesn't do anything and will be removed in the future.

Parameters

`outBitmap`

`Bitmap`

`canvas`

`Canvas`

Returns

`boolean`

### onCreateView

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [View](/reference/android/view/View) onCreateView ([View](/reference/android/view/View) parent, 
                [String](/reference/java/lang/String) name, 
                [Context](/reference/android/content/Context) context, 
                [AttributeSet](/reference/android/util/AttributeSet) attrs)

Standard implementation of `[LayoutInflater.Factory2.onCreateView(View, String, Context, AttributeSet)](/reference/android/view/LayoutInflater.Factory2#onCreateView\(android.view.View,%20java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))` used when inflating with the LayoutInflater returned by `[Context.getSystemService(Class)](/reference/android/content/Context#getSystemService\(java.lang.Class<T>\))`. This implementation handles tags to embed fragments inside of the activity.

Parameters

`parent`

`View`: This value may be `null`.

`name`

`String`: This value cannot be `null`.

`context`

`Context`: This value cannot be `null`.

`attrs`

`AttributeSet`: This value cannot be `null`.

Returns

`[View](/reference/android/view/View)`

This value may be `null`.

**See also:**

*   `[LayoutInflater.createView(Context, String, String, AttributeSet)](/reference/android/view/LayoutInflater#createView\(android.content.Context,%20java.lang.String,%20java.lang.String,%20android.util.AttributeSet\))`
*   `[Window.getLayoutInflater()](/reference/android/view/Window#getLayoutInflater\(\))`

### onCreateView

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [View](/reference/android/view/View) onCreateView ([String](/reference/java/lang/String) name, 
                [Context](/reference/android/content/Context) context, 
                [AttributeSet](/reference/android/util/AttributeSet) attrs)

Standard implementation of `[LayoutInflater.Factory.onCreateView(String, Context, AttributeSet)](/reference/android/view/LayoutInflater.Factory#onCreateView\(java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))` used when inflating with the LayoutInflater returned by `[Context.getSystemService(Class)](/reference/android/content/Context#getSystemService\(java.lang.Class<T>\))`. This implementation does nothing and is for pre-`[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` apps. Newer apps should use `[onCreateView(android.view.View, java.lang.String, android.content.Context, android.util.AttributeSet)](/reference/android/app/Activity#onCreateView\(android.view.View,%20java.lang.String,%20android.content.Context,%20android.util.AttributeSet\))`.

Parameters

`name`

`String`: This value cannot be `null`.

`context`

`Context`: This value cannot be `null`.

`attrs`

`AttributeSet`: This value cannot be `null`.

Returns

`[View](/reference/android/view/View)`

This value may be `null`.

**See also:**

*   `[LayoutInflater.createView(Context, String, String, AttributeSet)](/reference/android/view/LayoutInflater#createView\(android.content.Context,%20java.lang.String,%20java.lang.String,%20android.util.AttributeSet\))`
*   `[Window.getLayoutInflater()](/reference/android/view/Window#getLayoutInflater\(\))`

### onDetachedFromWindow

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onDetachedFromWindow ()

Called when the main window associated with the activity has been detached from the window manager. See `[View.onDetachedFromWindow()](/reference/android/view/View#onDetachedFromWindow\(\))` for more information.

**See also:**

*   `[View.onDetachedFromWindow()](/reference/android/view/View#onDetachedFromWindow\(\))`

### onEnterAnimationComplete

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onEnterAnimationComplete ()

Activities cannot draw during the period that their windows are animating in. In order to know when it is safe to begin drawing they can override this method which will be called when the entering animation has completed.

### onGenericMotionEvent

Added in [API level 12](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onGenericMotionEvent ([MotionEvent](/reference/android/view/MotionEvent) event)

Called when a generic motion event was not handled by any of the views inside of the activity.

Generic motion events describe joystick movements, hover events from mouse or stylus devices, trackpad touches, scroll wheel movements and other motion events not handled by `[onTouchEvent(android.view.MotionEvent)](/reference/android/app/Activity#onTouchEvent\(android.view.MotionEvent\))` or `[onTrackballEvent(android.view.MotionEvent)](/reference/android/app/Activity#onTrackballEvent\(android.view.MotionEvent\))`. The `[source](/reference/android/view/MotionEvent#getSource\(\))` of the motion event specifies the class of input that was received. Implementations of this method must examine the bits in the source before processing the event.

Generic motion events with source class `[InputDevice.SOURCE_CLASS_POINTER](/reference/android/view/InputDevice#SOURCE_CLASS_POINTER)` are delivered to the view under the pointer. All other generic motion events are delivered to the focused view.

See `[View.onGenericMotionEvent(MotionEvent)](/reference/android/view/View#onGenericMotionEvent\(android.view.MotionEvent\))` for an example of how to handle this event.

Parameters

`event`

`MotionEvent`: The generic motion event being processed.

Returns

`boolean`

Return true if you have consumed the event, false if you haven't. The default implementation always returns false.

### onGetDirectActions

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onGetDirectActions ([CancellationSignal](/reference/android/os/CancellationSignal) cancellationSignal, 
                [Consumer](/reference/java/util/function/Consumer)<[List](/reference/java/util/List)<[DirectAction](/reference/android/app/DirectAction)\>> callback)

Returns the list of direct actions supported by the app.

You should return the list of actions that could be executed in the current context, which is in the current state of the app. If the actions that could be executed by the app changes you should report that via calling `[VoiceInteractor.notifyDirectActionsChanged()](/reference/android/app/VoiceInteractor#notifyDirectActionsChanged\(\))`.

To get the voice interactor you need to call `[getVoiceInteractor()](/reference/android/app/Activity#getVoiceInteractor\(\))` which would return non `null` only if there is an ongoing voice interaction session. You can also detect when the voice interactor is no longer valid because the voice interaction session that is backing is finished by calling `[VoiceInteractor.registerOnDestroyedCallback(Executor, Runnable)](/reference/android/app/VoiceInteractor#registerOnDestroyedCallback\(java.util.concurrent.Executor,%20java.lang.Runnable\))`.

This method will be called only after `[onStart()](/reference/android/app/Activity#onStart\(\))` and before `[onStop()](/reference/android/app/Activity#onStop\(\))`.

You should pass to the callback the currently supported direct actions which cannot be `null` or contain `null` elements.

You should return the action list as soon as possible to ensure the consumer, for example the assistant, is as responsive as possible which would improve user experience of your app.

Parameters

`cancellationSignal`

`CancellationSignal`: A signal to cancel the operation in progress. This value cannot be `null`.

`callback`

`Consumer`: The callback to send the action list. The actions list cannot contain `null` elements. You can call this on any thread.

### onKeyDown

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onKeyDown (int keyCode, 
                [KeyEvent](/reference/android/view/KeyEvent) event)

Called when a key was pressed down and not handled by any of the views inside of the activity. So, for example, key presses while the cursor is inside a TextView will not trigger the event (unless it is a navigation to another object) because TextView handles its own key presses.

If the focused view didn't want this event, this method is called.

The default implementation takes care of `[KeyEvent.KEYCODE_BACK](/reference/android/view/KeyEvent#KEYCODE_BACK)` by calling `[onBackPressed()](/reference/android/app/Activity#onBackPressed\(\))`, though the behavior varies based on the application compatibility mode: for `[Build.VERSION_CODES.ECLAIR](/reference/android/os/Build.VERSION_CODES#ECLAIR)` or later applications, it will set up the dispatch to call `[onKeyUp(int, KeyEvent)](/reference/android/app/Activity#onKeyUp\(int,%20android.view.KeyEvent\))` where the action will be performed; for earlier applications, it will perform the action immediately in on-down, as those versions of the platform behaved. This implementation will also take care of `[KeyEvent.KEYCODE_ESCAPE](/reference/android/view/KeyEvent#KEYCODE_ESCAPE)` by finishing the activity if it would be closed by touching outside of it.

Other additional default key handling may be performed if configured with `[setDefaultKeyMode(int)](/reference/android/app/Activity#setDefaultKeyMode\(int\))`.

Parameters

`keyCode`

`int`: The value in event.getKeyCode().

`event`

`KeyEvent`: Description of the key event.

Returns

`boolean`

Return `true` to prevent this event from being propagated further, or `false` to indicate that you have not handled this event and it should continue to be propagated.

**See also:**

*   `[onKeyUp(int, KeyEvent)](/reference/android/app/Activity#onKeyUp\(int,%20android.view.KeyEvent\))`
*   `[KeyEvent](/reference/android/view/KeyEvent)`

### onKeyLongPress

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onKeyLongPress (int keyCode, 
                [KeyEvent](/reference/android/view/KeyEvent) event)

Default implementation of `[KeyEvent.Callback.onKeyLongPress()](/reference/android/view/KeyEvent.Callback#onKeyLongPress\(int,%20android.view.KeyEvent\))`: always returns false (doesn't handle the event). To receive this callback, you must return true from onKeyDown for the current event stream.

Parameters

`keyCode`

`int`: The value in event.getKeyCode().

`event`

`KeyEvent`: Description of the key event.

Returns

`boolean`

If you handled the event, return true. If you want to allow the event to be handled by the next receiver, return false.

**See also:**

*   `[KeyEvent.Callback.onKeyLongPress(int, KeyEvent)](/reference/android/view/KeyEvent.Callback#onKeyLongPress\(int,%20android.view.KeyEvent\))`

### onKeyMultiple

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onKeyMultiple (int keyCode, 
                int repeatCount, 
                [KeyEvent](/reference/android/view/KeyEvent) event)

Default implementation of `[KeyEvent.Callback.onKeyMultiple()](/reference/android/view/KeyEvent.Callback#onKeyMultiple\(int,%20int,%20android.view.KeyEvent\))`: always returns false (doesn't handle the event).

Parameters

`keyCode`

`int`: The value in event.getKeyCode().

`repeatCount`

`int`: Number of pairs as returned by event.getRepeatCount().

`event`

`KeyEvent`: Description of the key event.

Returns

`boolean`

If you handled the event, return true. If you want to allow the event to be handled by the next receiver, return false.

### onKeyShortcut

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onKeyShortcut (int keyCode, 
                [KeyEvent](/reference/android/view/KeyEvent) event)

Called when a key shortcut event is not handled by any of the views in the Activity. Override this method to implement global key shortcuts for the Activity. Key shortcuts can also be implemented by setting the `[shortcut](/reference/android/view/MenuItem#setShortcut\(char,%20char\))` property of menu items.

Parameters

`keyCode`

`int`: The value in event.getKeyCode().

`event`

`KeyEvent`: Description of the key event.

Returns

`boolean`

True if the key shortcut was handled.

### onKeyUp

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onKeyUp (int keyCode, 
                [KeyEvent](/reference/android/view/KeyEvent) event)

Called when a key was released and not handled by any of the views inside of the activity. So, for example, key presses while the cursor is inside a TextView will not trigger the event (unless it is a navigation to another object) because TextView handles its own key presses.

The default implementation handles KEYCODE\_BACK to stop the activity and go back.

Parameters

`keyCode`

`int`: The value in event.getKeyCode().

`event`

`KeyEvent`: Description of the key event.

Returns

`boolean`

Return `true` to prevent this event from being propagated further, or `false` to indicate that you have not handled this event and it should continue to be propagated.

**See also:**

*   `[onKeyDown(int, KeyEvent)](/reference/android/app/Activity#onKeyDown\(int,%20android.view.KeyEvent\))`
*   `[KeyEvent](/reference/android/view/KeyEvent)`

### onLocalVoiceInteractionStarted

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onLocalVoiceInteractionStarted ()

Callback to indicate that `[startLocalVoiceInteraction(android.os.Bundle)](/reference/android/app/Activity#startLocalVoiceInteraction\(android.os.Bundle\))` has resulted in a voice interaction session being started. You can now retrieve a voice interactor using `[getVoiceInteractor()](/reference/android/app/Activity#getVoiceInteractor\(\))`.

### onLocalVoiceInteractionStopped

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onLocalVoiceInteractionStopped ()

Callback to indicate that the local voice interaction has stopped either because it was requested through a call to `[stopLocalVoiceInteraction()](/reference/android/app/Activity#stopLocalVoiceInteraction\(\))` or because it was canceled by the user. The previously acquired `[VoiceInteractor](/reference/android/app/VoiceInteractor)` is no longer valid after this.

### onLowMemory

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onLowMemory ()

This is called when the overall system is running low on memory, and actively running processes should trim their memory usage. While the exact point at which this will be called is not defined, generally it will happen when all background process have been killed. That is, before reaching the point of killing processes hosting service and foreground UI that we would like to avoid killing.

### onMenuItemSelected

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onMenuItemSelected (int featureId, 
                [MenuItem](/reference/android/view/MenuItem) item)

Default implementation of `[Window.Callback.onMenuItemSelected(int, MenuItem)](/reference/android/view/Window.Callback#onMenuItemSelected\(int,%20android.view.MenuItem\))` for activities. This calls through to the new `[onOptionsItemSelected(MenuItem)](/reference/android/app/Activity#onOptionsItemSelected\(android.view.MenuItem\))` method for the `[Window.FEATURE_OPTIONS_PANEL](/reference/android/view/Window#FEATURE_OPTIONS_PANEL)` panel, so that subclasses of Activity don't need to deal with feature codes.

Parameters

`featureId`

`int`: The panel that the menu is in.

`item`

`MenuItem`: This value cannot be `null`.

Returns

`boolean`

boolean Return true to finish processing of selection, or false to perform the normal menu handling (calling its Runnable or sending a Message to its target Handler).

### onMenuOpened

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onMenuOpened (int featureId, 
                [Menu](/reference/android/view/Menu) menu)

Called when a panel's menu is opened by the user. This may also be called when the menu is changing from one type to another (for example, from the icon menu to the expanded menu).

Parameters

`featureId`

`int`: The panel that the menu is in.

`menu`

`Menu`: This value cannot be `null`.

Returns

`boolean`

The default implementation returns true.

### onMultiWindowModeChanged

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onMultiWindowModeChanged (boolean isInMultiWindowMode)

**This method was deprecated in API level 26.**  
Use `[onMultiWindowModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onMultiWindowModeChanged\(boolean,%20android.content.res.Configuration\))` instead.

Called by the system when the activity changes from fullscreen mode to multi-window mode and visa-versa.

Parameters

`isInMultiWindowMode`

`boolean`: True if the activity is in multi-window mode.

**See also:**

*   `[R.attr.resizeableActivity](/reference/android/R.attr#resizeableActivity)`

### onMultiWindowModeChanged

Added in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onMultiWindowModeChanged (boolean isInMultiWindowMode, 
                [Configuration](/reference/android/content/res/Configuration) newConfig)

Called by the system when the activity changes from fullscreen mode to multi-window mode and visa-versa. This method provides the same configuration that will be sent in the following `[onConfigurationChanged(android.content.res.Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` call after the activity enters this mode.

Parameters

`isInMultiWindowMode`

`boolean`: True if the activity is in multi-window mode.

`newConfig`

`Configuration`: The new configuration of the activity with the state .

**See also:**

*   `[R.attr.resizeableActivity](/reference/android/R.attr#resizeableActivity)`

### onNavigateUp

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onNavigateUp ()

This method is called whenever the user chooses to navigate Up within your application's activity hierarchy from the action bar.

If the attribute `[parentActivityName](/reference/android/R.attr#parentActivityName)` was specified in the manifest for this activity or an activity-alias to it, default Up navigation will be handled automatically. If any activity along the parent chain requires extra Intent arguments, the Activity subclass should override the method `[onPrepareNavigateUpTaskStack(android.app.TaskStackBuilder)](/reference/android/app/Activity#onPrepareNavigateUpTaskStack\(android.app.TaskStackBuilder\))` to supply those arguments.

See [Tasks and Back Stack](/guide/components/tasks-and-back-stack) from the developer guide and [Navigation](/design/patterns/navigation) from the design guide for more information about navigating within your app.

See the `[TaskStackBuilder](/reference/android/app/TaskStackBuilder)` class and the Activity methods `[getParentActivityIntent()](/reference/android/app/Activity#getParentActivityIntent\(\))`, `[shouldUpRecreateTask(android.content.Intent)](/reference/android/app/Activity#shouldUpRecreateTask\(android.content.Intent\))`, and `[navigateUpTo(android.content.Intent)](/reference/android/app/Activity#navigateUpTo\(android.content.Intent\))` for help implementing custom Up navigation. The AppNavigation sample application in the Android SDK is also available for reference.

Returns

`boolean`

true if Up navigation completed successfully and this Activity was finished, false otherwise.

### onNavigateUpFromChild

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onNavigateUpFromChild ([Activity](/reference/android/app/Activity) child)

**This method was deprecated in API level 30.**  
Use `[onNavigateUp()](/reference/android/app/Activity#onNavigateUp\(\))` instead.

This is called when a child activity of this one attempts to navigate up. The default implementation simply calls onNavigateUp() on this activity (the parent).

Parameters

`child`

`Activity`: The activity making the call.

Returns

`boolean`

### onNewIntent

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onNewIntent ([Intent](/reference/android/content/Intent) intent, 
                [ComponentCaller](/reference/android/app/ComponentCaller) caller)

Same as `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, but with an extra parameter for the ComponentCaller instance associated with the app that sent the intent.

If you want to retrieve the caller without overriding this method, call `[getCurrentCaller()](/reference/android/app/Activity#getCurrentCaller\(\))` inside your existing `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`.

Note that you should only override one `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` method.

Parameters

`intent`

`Intent`: The new intent that was used to start the activity This value cannot be `null`.

`caller`

`ComponentCaller`: The `[ComponentCaller](/reference/android/app/ComponentCaller)` instance associated with the app that sent the intent This value cannot be `null`.

**See also:**

*   `[ComponentCaller](/reference/android/app/ComponentCaller)`
*   `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`
*   `[getCurrentCaller()](/reference/android/app/Activity#getCurrentCaller\(\))`
*   `[setIntent(Intent, ComponentCaller)](/reference/android/app/Activity#setIntent\(android.content.Intent,%20android.app.ComponentCaller\))`
*   `[getCaller()](/reference/android/app/Activity#getCaller\(\))`

### onOptionsItemSelected

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onOptionsItemSelected ([MenuItem](/reference/android/view/MenuItem) item)

This hook is called whenever an item in your options menu is selected. The default implementation simply returns false to have the normal processing happen (calling the item's Runnable or sending a message to its Handler as appropriate). You can use this method for any items for which you would like to do processing without those other facilities.

Derived classes should call through to the base class for it to perform the default menu handling.

Parameters

`item`

`MenuItem`: The menu item that was selected. This value cannot be `null`.

Returns

`boolean`

boolean Return false to allow normal menu processing to proceed, true to consume it here.

**See also:**

*   `[onCreateOptionsMenu(Menu)](/reference/android/app/Activity#onCreateOptionsMenu\(android.view.Menu\))`

### onOptionsMenuClosed

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onOptionsMenuClosed ([Menu](/reference/android/view/Menu) menu)

This hook is called whenever the options menu is being closed (either by the user canceling the menu with the back/menu button, or when an item is selected).

Parameters

`menu`

`Menu`: The options menu as last shown or first initialized by onCreateOptionsMenu().

### onPanelClosed

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPanelClosed (int featureId, 
                [Menu](/reference/android/view/Menu) menu)

Default implementation of `[Window.Callback.onPanelClosed(int, Menu)](/reference/android/view/Window.Callback#onPanelClosed\(int,%20android.view.Menu\))` for activities. This calls through to `[onOptionsMenuClosed(android.view.Menu)](/reference/android/app/Activity#onOptionsMenuClosed\(android.view.Menu\))` method for the `[Window.FEATURE_OPTIONS_PANEL](/reference/android/view/Window#FEATURE_OPTIONS_PANEL)` panel, so that subclasses of Activity don't need to deal with feature codes. For context menus (`[Window.FEATURE_CONTEXT_MENU](/reference/android/view/Window#FEATURE_CONTEXT_MENU)`), the `[onContextMenuClosed(android.view.Menu)](/reference/android/app/Activity#onContextMenuClosed\(android.view.Menu\))` will be called.

Parameters

`featureId`

`int`: The panel that is being displayed.

`menu`

`Menu`: This value cannot be `null`.

### onPerformDirectAction

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPerformDirectAction ([String](/reference/java/lang/String) actionId, 
                [Bundle](/reference/android/os/Bundle) arguments, 
                [CancellationSignal](/reference/android/os/CancellationSignal) cancellationSignal, 
                [Consumer](/reference/java/util/function/Consumer)<[Bundle](/reference/android/os/Bundle)\> resultListener)

This is called to perform an action previously defined by the app. Apps also have access to `[getVoiceInteractor()](/reference/android/app/Activity#getVoiceInteractor\(\))` to follow up on the action.

Parameters

`actionId`

`String`: The ID for the action you previously reported via `[onGetDirectActions(android.os.CancellationSignal, java.util.function.Consumer)](/reference/android/app/Activity#onGetDirectActions\(android.os.CancellationSignal,%20java.util.function.Consumer<java.util.List<android.app.DirectAction>>\))`. This value cannot be `null`.

`arguments`

`Bundle`: Any additional arguments provided by the caller that are specific to the given action. This value cannot be `null`.

`cancellationSignal`

`CancellationSignal`: A signal to cancel the operation in progress. This value cannot be `null`.

`resultListener`

`Consumer`: The callback to provide the result back to the caller. You can call this on any thread. The result bundle is action specific. This value cannot be `null`.

**See also:**

*   `[onGetDirectActions(CancellationSignal, Consumer)](/reference/android/app/Activity#onGetDirectActions\(android.os.CancellationSignal,%20java.util.function.Consumer<java.util.List<android.app.DirectAction>>\))`

### onPictureInPictureModeChanged

Added in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPictureInPictureModeChanged (boolean isInPictureInPictureMode, 
                [Configuration](/reference/android/content/res/Configuration) newConfig)

Called by the system when the activity changes to and from picture-in-picture mode. This method provides the same configuration that will be sent in the following `[onConfigurationChanged(android.content.res.Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` call after the activity enters this mode.

Parameters

`isInPictureInPictureMode`

`boolean`: True if the activity is in picture-in-picture mode.

`newConfig`

`Configuration`: The new configuration of the activity with the state .

**See also:**

*   `[R.attr.supportsPictureInPicture](/reference/android/R.attr#supportsPictureInPicture)`

### onPictureInPictureModeChanged

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPictureInPictureModeChanged (boolean isInPictureInPictureMode)

**This method was deprecated in API level 26.**  
Use `[onPictureInPictureModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))` instead.

Called by the system when the activity changes to and from picture-in-picture mode.

Parameters

`isInPictureInPictureMode`

`boolean`: True if the activity is in picture-in-picture mode.

**See also:**

*   `[R.attr.supportsPictureInPicture](/reference/android/R.attr#supportsPictureInPicture)`

### onPictureInPictureRequested

Added in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onPictureInPictureRequested ()

This method is called by the system in various cases where picture in picture mode should be entered if supported.

It is up to the app developer to choose whether to call `[enterPictureInPictureMode(android.app.PictureInPictureParams)](/reference/android/app/Activity#enterPictureInPictureMode\(android.app.PictureInPictureParams\))` at this time. For example, the system will call this method when the activity is being put into the background, so the app developer might want to switch an activity into PIP mode instead.

Returns

`boolean`

`true` if the activity received this callback regardless of if it acts on it or not. If `false`, the framework will assume the app hasn't been updated to leverage this callback and will in turn send a legacy callback of `[onUserLeaveHint()](/reference/android/app/Activity#onUserLeaveHint\(\))` for the app to enter picture-in-picture mode.

### onPictureInPictureUiStateChanged

Added in [API level 31](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPictureInPictureUiStateChanged ([PictureInPictureUiState](/reference/android/app/PictureInPictureUiState) pipState)

Called by the system when the activity is in PiP and has state changes. Compare to `[onPictureInPictureModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))`, which is only called when PiP mode changes (meaning, enters or exits PiP), this can be called at any time while the activity is in PiP mode. Therefore, all invocation can only happen after `[onPictureInPictureModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))` is called with true, and before `[onPictureInPictureModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))` is called with false. You would not need to worry about cases where this is called and the activity is not in Picture-In-Picture mode. For managing cases where the activity enters/exits Picture-in-Picture (e.g. resources clean-up on exit), use `[onPictureInPictureModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onPictureInPictureModeChanged\(boolean,%20android.content.res.Configuration\))`. The default state is everything declared in `[PictureInPictureUiState](/reference/android/app/PictureInPictureUiState)` is false, such as `[PictureInPictureUiState.isStashed()](/reference/android/app/PictureInPictureUiState#isStashed\(\))`.

Parameters

`pipState`

`PictureInPictureUiState`: the new Picture-in-Picture state. This value cannot be `null`.

### onPostCreate

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPostCreate ([Bundle](/reference/android/os/Bundle) savedInstanceState, 
                [PersistableBundle](/reference/android/os/PersistableBundle) persistentState)

This is the same as `[onPostCreate(android.os.Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))` but is called for activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`.

Parameters

`savedInstanceState`

`Bundle`: The data most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` This value may be `null`.

`persistentState`

`PersistableBundle`: The data coming from the PersistableBundle first saved in `[onSaveInstanceState(android.os.Bundle, android.os.PersistableBundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle,%20android.os.PersistableBundle\))`. This value may be `null`.

**See also:**

*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`

### onPrepareNavigateUpTaskStack

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onPrepareNavigateUpTaskStack ([TaskStackBuilder](/reference/android/app/TaskStackBuilder) builder)

Prepare the synthetic task stack that will be generated during Up navigation from a different task.

This method receives the `[TaskStackBuilder](/reference/android/app/TaskStackBuilder)` with the constructed series of Intents as generated by `[onCreateNavigateUpTaskStack(android.app.TaskStackBuilder)](/reference/android/app/Activity#onCreateNavigateUpTaskStack\(android.app.TaskStackBuilder\))`. If any extra data should be added to these intents before launching the new task, the application should override this method and add that data here.

Parameters

`builder`

`TaskStackBuilder`: A TaskStackBuilder that has been populated with Intents by onCreateNavigateUpTaskStack.

### onPrepareOptionsMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onPrepareOptionsMenu ([Menu](/reference/android/view/Menu) menu)

Prepare the Screen's standard options menu to be displayed. This is called right before the menu is shown, every time it is shown. You can use this method to efficiently enable/disable items or otherwise dynamically modify the contents.

The default implementation updates the system menu items based on the activity's state. Deriving classes should always call through to the base class implementation.

Parameters

`menu`

`Menu`: The options menu as last shown or first initialized by onCreateOptionsMenu().

Returns

`boolean`

You must return true for the menu to be displayed; if you return false it will not be shown.

**See also:**

*   `[onCreateOptionsMenu(Menu)](/reference/android/app/Activity#onCreateOptionsMenu\(android.view.Menu\))`

### onPreparePanel

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onPreparePanel (int featureId, 
                [View](/reference/android/view/View) view, 
                [Menu](/reference/android/view/Menu) menu)

Default implementation of `[Window.Callback.onPreparePanel(int, View, Menu)](/reference/android/view/Window.Callback#onPreparePanel\(int,%20android.view.View,%20android.view.Menu\))` for activities. This calls through to the new `[onPrepareOptionsMenu(Menu)](/reference/android/app/Activity#onPrepareOptionsMenu\(android.view.Menu\))` method for the `[Window.FEATURE_OPTIONS_PANEL](/reference/android/view/Window#FEATURE_OPTIONS_PANEL)` panel, so that subclasses of Activity don't need to deal with feature codes.

Parameters

`featureId`

`int`: The panel that is being displayed.

`view`

`View`: This value may be `null`.

`menu`

`Menu`: This value cannot be `null`.

Returns

`boolean`

boolean You must return true for the panel to be displayed; if you return false it will not be shown.

### onProvideAssistContent

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onProvideAssistContent ([AssistContent](/reference/android/app/assist/AssistContent) outContent)

This is called when the user is requesting an assist, to provide references to content related to the current activity. Before being called, the `outContent` Intent is filled with the base Intent of the activity (the Intent returned by `[getIntent()](/reference/android/app/Activity#getIntent\(\))`). The Intent's extras are stripped of any types that are not valid for `[PersistableBundle](/reference/android/os/PersistableBundle)` or non-framework Parcelables, and the flags `[Intent.FLAG_GRANT_WRITE_URI_PERMISSION](/reference/android/content/Intent#FLAG_GRANT_WRITE_URI_PERMISSION)` and `[Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION](/reference/android/content/Intent#FLAG_GRANT_PERSISTABLE_URI_PERMISSION)` are cleared from the Intent.

Custom implementation may adjust the content intent to better reflect the top-level context of the activity, and fill in its ClipData with additional content of interest that the user is currently viewing. For example, an image gallery application that has launched in to an activity allowing the user to swipe through pictures should modify the intent to reference the current image they are looking it; such an application when showing a list of pictures should add a ClipData that has references to all of the pictures currently visible on screen.

Parameters

`outContent`

`AssistContent`: The assist content to return.

### onProvideAssistData

Added in [API level 18](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onProvideAssistData ([Bundle](/reference/android/os/Bundle) data)

This is called when the user is requesting an assist, to build a full `[Intent.ACTION_ASSIST](/reference/android/content/Intent#ACTION_ASSIST)` Intent with all of the context of the current application. You can override this method to place into the bundle anything you would like to appear in the `[Intent.EXTRA_ASSIST_CONTEXT](/reference/android/content/Intent#EXTRA_ASSIST_CONTEXT)` part of the assist Intent.

This function will be called after any global assist callbacks that had been registered with `[Application.registerOnProvideAssistDataListener](/reference/android/app/Application#registerOnProvideAssistDataListener\(android.app.Application.OnProvideAssistDataListener\))`.

Parameters

`data`

`Bundle`

### onProvideKeyboardShortcuts

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onProvideKeyboardShortcuts ([List](/reference/java/util/List)<[KeyboardShortcutGroup](/reference/android/view/KeyboardShortcutGroup)\> data, 
                [Menu](/reference/android/view/Menu) menu, 
                int deviceId)

Called when Keyboard Shortcuts are requested for the current window.

Parameters

`data`

`List`: The data list to populate with shortcuts.

`menu`

`Menu`: The current menu, which may be null.

`deviceId`

`int`: The id for the connected device the shortcuts should be provided for.

### onProvideReferrer

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Uri](/reference/android/net/Uri) onProvideReferrer ()

Override to generate the desired referrer for the content currently being shown by the app. The default implementation returns null, meaning the referrer will simply be the android-app: of the package name of this activity. Return a non-null Uri to have that supplied as the `[Intent.EXTRA_REFERRER](/reference/android/content/Intent#EXTRA_REFERRER)` of any activities started from it.

Returns

`[Uri](/reference/android/net/Uri)`

### onRequestPermissionsResult

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onRequestPermissionsResult (int requestCode, 
                [String\[\]](/reference/java/lang/String) permissions, 
                int\[\] grantResults)

Callback for the result from requesting permissions. This method is invoked for every call on `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`

**Note:** It is possible that the permissions request interaction with the user is interrupted. In this case you will receive empty permissions and results arrays which should be treated as a cancellation.

Parameters

`requestCode`

`int`: The request code passed in `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`.

`permissions`

`String`: The requested permissions. Never null.

`grantResults`

`int`: The grant results for the corresponding permissions which is either `[PackageManager.PERMISSION_GRANTED](/reference/android/content/pm/PackageManager#PERMISSION_GRANTED)` or `[PackageManager.PERMISSION_DENIED](/reference/android/content/pm/PackageManager#PERMISSION_DENIED)`. Never null.

**See also:**

*   `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`

### onRequestPermissionsResult

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onRequestPermissionsResult (int requestCode, 
                [String\[\]](/reference/java/lang/String) permissions, 
                int\[\] grantResults, 
                int deviceId)

Callback for the result from requesting permissions. This method is invoked for every call on `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`.

**Note:** It is possible that the permissions request interaction with the user is interrupted. In this case you will receive empty permissions and results arrays which should be treated as a cancellation.

Parameters

`requestCode`

`int`: The request code passed in `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`.

`permissions`

`String`: The requested permissions. Never null.

`grantResults`

`int`: The grant results for the corresponding permissions which is either `[PackageManager.PERMISSION_GRANTED](/reference/android/content/pm/PackageManager#PERMISSION_GRANTED)` or `[PackageManager.PERMISSION_DENIED](/reference/android/content/pm/PackageManager#PERMISSION_DENIED)`. Never null.

`deviceId`

`int`: The deviceId for which permissions were requested. The primary/physical device is assigned `[Context.DEVICE_ID_DEFAULT](/reference/android/content/Context#DEVICE_ID_DEFAULT)`, and virtual devices are assigned unique device Ids.

**See also:**

*   `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`

### onRestoreInstanceState

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onRestoreInstanceState ([Bundle](/reference/android/os/Bundle) savedInstanceState, 
                [PersistableBundle](/reference/android/os/PersistableBundle) persistentState)

This is the same as `[onRestoreInstanceState(android.os.Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` but is called for activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`. The `[PersistableBundle](/reference/android/os/PersistableBundle)` passed came from the restored PersistableBundle first saved in `[onSaveInstanceState(android.os.Bundle, android.os.PersistableBundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle,%20android.os.PersistableBundle\))`.

This method is called between `[onStart()](/reference/android/app/Activity#onStart\(\))` and `[onPostCreate(Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))`.

If this method is called `[onRestoreInstanceState(android.os.Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` will not be called.

At least one of `savedInstanceState` or `persistentState` will not be null.

Parameters

`savedInstanceState`

`Bundle`: the data most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` or null.

`persistentState`

`PersistableBundle`: the data most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` or null.

**See also:**

*   `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`
*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`
*   `[onPostCreate(Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`
*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`

### onRetainNonConfigurationInstance

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [Object](/reference/java/lang/Object) onRetainNonConfigurationInstance ()

Called by the system, as part of destroying an activity due to a configuration change, when it is known that a new instance will immediately be created for the new configuration. You can return any object you like here, including the activity instance itself, which can later be retrieved by calling `[getLastNonConfigurationInstance()](/reference/android/app/Activity#getLastNonConfigurationInstance\(\))` in the new activity instance. _If you are targeting `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` or later, consider instead using a `[Fragment](/reference/android/app/Fragment)` with `[Fragment.setRetainInstance(boolean](/reference/android/app/Fragment#setRetainInstance\(boolean\))`._

This function is called purely as an optimization, and you must not rely on it being called. When it is called, a number of guarantees will be made to help optimize configuration switching:

*   The function will be called between `[onStop()](/reference/android/app/Activity#onStop\(\))` and `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))`.
*   A new instance of the activity will _always_ be immediately created after this one's `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` is called. In particular, _no_ messages will be dispatched during this time (when the returned object does not have an activity to be associated with).
*   The object you return here will _always_ be available from the `[getLastNonConfigurationInstance()](/reference/android/app/Activity#getLastNonConfigurationInstance\(\))` method of the following activity instance as described there.

These guarantees are designed so that an activity can use this API to propagate extensive state from the old to new activity instance, from loaded bitmaps, to network connections, to evenly actively running threads. Note that you should _not_ propagate any data that may change based on the configuration, including any data loaded from resources such as strings, layouts, or drawables.

The guarantee of no message handling during the switch to the next activity simplifies use with active objects. For example if your retained state is an `[AsyncTask](/reference/android/os/AsyncTask)` you are guaranteed that its call back functions (like `[AsyncTask.onPostExecute(Result)](/reference/android/os/AsyncTask#onPostExecute\(Result\))`) will not be called from the call here until you execute the next instance's `[onCreate(android.os.Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`. (Note however that there is of course no such guarantee for `[AsyncTask.doInBackground(Params)](/reference/android/os/AsyncTask#doInBackground\(Params[]\))` since that is running in a separate thread.)

**Note:** For most cases you should use the `[Fragment](/reference/android/app/Fragment)` API `[Fragment.setRetainInstance(boolean)](/reference/android/app/Fragment#setRetainInstance\(boolean\))` instead; this is also available on older platforms through the Android support libraries.

Returns

`[Object](/reference/java/lang/Object)`

any Object holding the desired state to propagate to the next activity instance

### onSaveInstanceState

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onSaveInstanceState ([Bundle](/reference/android/os/Bundle) outState, 
                [PersistableBundle](/reference/android/os/PersistableBundle) outPersistentState)

This is the same as `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` but is called for activities created with the attribute `[R.attr.persistableMode](/reference/android/R.attr#persistableMode)` set to `persistAcrossReboots`. The `[PersistableBundle](/reference/android/os/PersistableBundle)` passed in will be saved and presented in `[onCreate(android.os.Bundle, android.os.PersistableBundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle,%20android.os.PersistableBundle\))` the first time that this activity is restarted following the next device reboot.

Parameters

`outState`

`Bundle`: Bundle in which to place your saved state. This value cannot be `null`.

`outPersistentState`

`PersistableBundle`: State which will be saved across reboots. This value cannot be `null`.

**See also:**

*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`
*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`
*   `[onRestoreInstanceState(Bundle, PersistableBundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle,%20android.os.PersistableBundle\))`
*   `[onPause()](/reference/android/app/Activity#onPause\(\))`

### onSearchRequested

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onSearchRequested ([SearchEvent](/reference/android/view/SearchEvent) searchEvent)

This hook is called when the user signals the desire to start a search.

You can use this function as a simple way to launch the search UI, in response to a menu item, search button, or other widgets within your activity. Unless overridden, calling this function is the same as calling `[startSearch(null,false,null,false)](/reference/android/app/Activity#startSearch\(java.lang.String,%20boolean,%20android.os.Bundle,%20boolean\))`, which launches search for the current activity as specified in its manifest, see `[SearchManager](/reference/android/app/SearchManager)`.

You can override this function to force global search, e.g. in response to a dedicated search key, or to block search entirely (by simply returning false).

Note: when running in a `[Configuration.UI_MODE_TYPE_TELEVISION](/reference/android/content/res/Configuration#UI_MODE_TYPE_TELEVISION)` or `[Configuration.UI_MODE_TYPE_WATCH](/reference/android/content/res/Configuration#UI_MODE_TYPE_WATCH)`, the default implementation changes to simply return false and you must supply your own custom implementation if you want to support search.

Parameters

`searchEvent`

`SearchEvent`: The `[SearchEvent](/reference/android/view/SearchEvent)` that signaled this search. This value may be `null`.

Returns

`boolean`

Returns `true` if search launched, and `false` if the activity does not respond to search. The default implementation always returns `true`, except when in `[Configuration.UI_MODE_TYPE_TELEVISION](/reference/android/content/res/Configuration#UI_MODE_TYPE_TELEVISION)` mode where it returns false.

**See also:**

*   `[SearchManager](/reference/android/app/SearchManager)`

### onSearchRequested

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onSearchRequested ()

Called when the user signals the desire to start a search.

Returns

`boolean`

true if search launched, false if activity refuses (blocks)

**See also:**

*   `[onSearchRequested(SearchEvent)](/reference/android/app/Activity#onSearchRequested\(android.view.SearchEvent\))`

### onStateNotSaved

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onStateNotSaved ()

**This method was deprecated in API level 29.**  
starting with `[Build.VERSION_CODES.P](/reference/android/os/Build.VERSION_CODES#P)` onSaveInstanceState is called after `[onStop()](/reference/android/app/Activity#onStop\(\))`, so this hint isn't accurate anymore: you should consider your state not saved in between `onStart` and `onStop` callbacks inclusively.

Called when an `[onResume()](/reference/android/app/Activity#onResume\(\))` is coming up, prior to other pre-resume callbacks such as `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` and `[onActivityResult(int, int, Intent)](/reference/android/app/Activity#onActivityResult\(int,%20int,%20android.content.Intent\))`. This is primarily intended to give the activity a hint that its state is no longer saved -- it will generally be called after `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` and prior to the activity being resumed/started again.

### onTopResumedActivityChanged

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onTopResumedActivityChanged (boolean isTopResumedActivity)

Called when activity gets or loses the top resumed position in the system.

Starting with `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)` multiple activities can be resumed at the same time in multi-window and multi-display modes. This callback should be used instead of `[onResume()](/reference/android/app/Activity#onResume\(\))` as an indication that the activity can try to open exclusive-access devices like camera.

It will always be delivered after the activity was resumed and before it is paused. In some cases it might be skipped and activity can go straight from `[onResume()](/reference/android/app/Activity#onResume\(\))` to `[onPause()](/reference/android/app/Activity#onPause\(\))` without receiving the top resumed state.

Parameters

`isTopResumedActivity`

`boolean`: `true` if it's the topmost resumed activity in the system, `false` otherwise. A call with this as `true` will always be followed by another one with `false`.

**See also:**

*   `[onResume()](/reference/android/app/Activity#onResume\(\))`
*   `[onPause()](/reference/android/app/Activity#onPause\(\))`
*   `[onWindowFocusChanged(boolean)](/reference/android/app/Activity#onWindowFocusChanged\(boolean\))`

### onTouchEvent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onTouchEvent ([MotionEvent](/reference/android/view/MotionEvent) event)

Called when a touch screen event was not handled by any of the views inside of the activity. This is most useful to process touch events that happen outside of your window bounds, where there is no view to receive it.

Parameters

`event`

`MotionEvent`: The touch screen event being processed.

Returns

`boolean`

Return true if you have consumed the event, false if you haven't.

### onTrackballEvent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean onTrackballEvent ([MotionEvent](/reference/android/view/MotionEvent) event)

Called when the trackball was moved and not handled by any of the views inside of the activity. So, for example, if the trackball moves while focus is on a button, you will receive a call here because buttons do not normally do anything with trackball events. The call here happens _before_ trackball movements are converted to DPAD key events, which then get sent back to the view hierarchy, and will be processed at the point for things like focus navigation.

Parameters

`event`

`MotionEvent`: The trackball event being processed.

Returns

`boolean`

Return true if you have consumed the event, false if you haven't. The default implementation always returns false.

### onTrimMemory

Added in [API level 14](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onTrimMemory (int level)

Called when the operating system has determined that it is a good time for a process to trim unneeded memory from its process. You should never compare to exact values of the level, since new intermediate values may be added -- you will typically want to compare if the value is greater or equal to a level you are interested in.

To retrieve the processes current trim level at any point, you can use `[ActivityManager.getMyMemoryState(RunningAppProcessInfo)](/reference/android/app/ActivityManager#getMyMemoryState\(android.app.ActivityManager.RunningAppProcessInfo\))`.

Parameters

`level`

`int`: The context of the trim, giving a hint of the amount of trimming the application may like to perform. Value is `[ComponentCallbacks2.TRIM_MEMORY_COMPLETE](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_COMPLETE)`, `[ComponentCallbacks2.TRIM_MEMORY_MODERATE](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_MODERATE)`, `[ComponentCallbacks2.TRIM_MEMORY_BACKGROUND](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_BACKGROUND)`, `[ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_UI_HIDDEN)`, `[ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_RUNNING_CRITICAL)`, `[ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_RUNNING_LOW)`, or `[ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE](/reference/android/content/ComponentCallbacks2#TRIM_MEMORY_RUNNING_MODERATE)`

### onUserInteraction

Added in [API level 3](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onUserInteraction ()

Called whenever a key, touch, or trackball event is dispatched to the activity. Implement this method if you wish to know that the user has interacted with the device in some way while your activity is running. This callback and `[onUserLeaveHint()](/reference/android/app/Activity#onUserLeaveHint\(\))` are intended to help activities manage status bar notifications intelligently; specifically, for helping activities determine the proper time to cancel a notification.

All calls to your activity's `[onUserLeaveHint()](/reference/android/app/Activity#onUserLeaveHint\(\))` callback will be accompanied by calls to `[onUserInteraction()](/reference/android/app/Activity#onUserInteraction\(\))`. This ensures that your activity will be told of relevant user activity such as pulling down the notification pane and touching an item there.

Note that this callback will be invoked for the touch down action that begins a touch gesture, but may not be invoked for the touch-moved and touch-up actions that follow.

**See also:**

*   `[onUserLeaveHint()](/reference/android/app/Activity#onUserLeaveHint\(\))`

### onVisibleBehindCanceled

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onVisibleBehindCanceled ()

**This method was deprecated in API level 26.**  
This method's functionality is no longer supported as of `[Build.VERSION_CODES.O](/reference/android/os/Build.VERSION_CODES#O)` and will be removed in a future release.

Called when a translucent activity over this activity is becoming opaque or another activity is being launched. Activities that override this method must call `super.onVisibleBehindCanceled()` or a SuperNotCalledException will be thrown.

When this method is called the activity has 500 msec to release any resources it may be using while visible in the background. If the activity has not returned from this method in 500 msec the system will destroy the activity and kill the process in order to recover the resources for another process. Otherwise `[onStop()](/reference/android/app/Activity#onStop\(\))` will be called following return.  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[requestVisibleBehind(boolean)](/reference/android/app/Activity#requestVisibleBehind\(boolean\))`

### onWindowAttributesChanged

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onWindowAttributesChanged ([WindowManager.LayoutParams](/reference/android/view/WindowManager.LayoutParams) params)

This is called whenever the current window attributes change.

Parameters

`params`

`WindowManager.LayoutParams`

### onWindowFocusChanged

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void onWindowFocusChanged (boolean hasFocus)

Called when the current `[Window](/reference/android/view/Window)` of the activity gains or loses focus. This is the best indicator of whether this activity is the entity with which the user actively interacts. The default implementation clears the key tracking state, so should always be called.

Note that this provides information about global focus state, which is managed independently of activity lifecycle. As such, while focus changes will generally have some relation to lifecycle changes (an activity that is stopped will not generally get window focus), you should not rely on any particular order between the callbacks here and those in the other lifecycle methods such as `[onResume()](/reference/android/app/Activity#onResume\(\))`.

As a general rule, however, a foreground activity will have window focus... unless it has displayed other dialogs or popups that take input focus, in which case the activity itself will not have focus when the other windows have it. Likewise, the system may display system-level windows (such as the status bar notification panel or a system alert) which will temporarily take window input focus without pausing the foreground activity.

Starting with `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)` there can be multiple resumed activities at the same time in multi-window mode, so resumed state does not guarantee window focus even if there are no overlays above.

If the intent is to know when an activity is the topmost active, the one the user interacted with last among all activities but not including non-activity windows like dialogs and popups, then `[onTopResumedActivityChanged(boolean)](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))` should be used. On platform versions prior to `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)`, `[onResume()](/reference/android/app/Activity#onResume\(\))` is the best indicator.

Parameters

`hasFocus`

`boolean`: Whether the window of this activity has focus.

**See also:**

*   `[hasWindowFocus()](/reference/android/app/Activity#hasWindowFocus\(\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`
*   `[View.onWindowFocusChanged(boolean)](/reference/android/view/View#onWindowFocusChanged\(boolean\))`
*   `[onTopResumedActivityChanged(boolean)](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))`

### onWindowStartingActionMode

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ActionMode](/reference/android/view/ActionMode) onWindowStartingActionMode ([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback, 
                int type)

Called when an action mode is being started for this window. Gives the callback an opportunity to handle the action mode in its own unique and beautiful way. If this method returns null the system can choose a way to present the mode or choose not to start the mode at all.

Parameters

`callback`

`ActionMode.Callback`: Callback to control the lifecycle of this action mode

`type`

`int`: One of `[ActionMode.TYPE_PRIMARY](/reference/android/view/ActionMode#TYPE_PRIMARY)` or `[ActionMode.TYPE_FLOATING](/reference/android/view/ActionMode#TYPE_FLOATING)`.

Returns

`[ActionMode](/reference/android/view/ActionMode)`

This value may be `null`.

### onWindowStartingActionMode

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ActionMode](/reference/android/view/ActionMode) onWindowStartingActionMode ([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback)

Give the Activity a chance to control the UI for an action mode requested by the system.

Note: If you are looking for a notification callback that an action mode has been started for this activity, see `[onActionModeStarted(android.view.ActionMode)](/reference/android/app/Activity#onActionModeStarted\(android.view.ActionMode\))`.

Parameters

`callback`

`ActionMode.Callback`: The callback that should control the new action mode

Returns

`[ActionMode](/reference/android/view/ActionMode)`

The new action mode, or `null` if the activity does not want to provide special handling for this action mode. (It will be handled by the system.)

### openContextMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void openContextMenu ([View](/reference/android/view/View) view)

Programmatically opens the context menu for a particular `view`. The `view` should have been added via `[registerForContextMenu(android.view.View)](/reference/android/app/Activity#registerForContextMenu\(android.view.View\))`.

Parameters

`view`

`View`: The view to show the context menu for.

### openOptionsMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void openOptionsMenu ()

Programmatically opens the options menu. If the options menu is already open, this method does nothing.

### overrideActivityTransition

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void overrideActivityTransition (int overrideType, 
                int enterAnim, 
                int exitAnim, 
                int backgroundColor)

Customizes the animation and background color for activity transitions.

This method provides a robust way to override transition animations at runtime and can be called at any point in the activity's lifecycle. It is particularly useful for handling predictive back animations, for which `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))` is not suitable.

The specified animations will be applied only when this activity is at the top of the task stack during the transition. For example, to customize the opening animation for an Activity B started from Activity A, call this method in B's `onCreate` with `overrideType = OVERRIDE_TRANSITION_OPEN`. To customize the closing animation when returning from B to A, call this method in B with `overrideType = OVERRIDE_TRANSITION_CLOSE`.

**Animation Precedence:**

*   Animations set by `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))` have the highest priority.
*   Animations set by this method have higher priority than those defined by `[Window.setWindowAnimations(int)](/reference/android/view/Window#setWindowAnimations\(int\))`.

**Predictive Back Animation:**

When predictive back is enabled (see `enableOnBackInvokedCallback` in the manifest), the system may control the preview phase of the back animation. The custom transition defined by `enterAnim` and `exitAnim` is then applied only after the back gesture is committed.

**Limitations:**

*   This method, along with `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))` and `[Window.setWindowAnimations(int)](/reference/android/view/Window#setWindowAnimations\(int\))`, is ignored if the activity is started with `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`.
*   As of Android 11, this method cannot be used to customize cross-task transitions.

Parameters

`overrideType`

`int`: The type of transition to override. Must be either `OVERRIDE_TRANSITION_OPEN` for entering transitions or `OVERRIDE_TRANSITION_CLOSE` for exiting transitions. Value is `[OVERRIDE_TRANSITION_OPEN](/reference/android/app/Activity#OVERRIDE_TRANSITION_OPEN)`, or `[OVERRIDE_TRANSITION_CLOSE](/reference/android/app/Activity#OVERRIDE_TRANSITION_CLOSE)`

`enterAnim`

`int`: The resource ID of the animation for the incoming activity. Use 0 for no animation.

`exitAnim`

`int`: The resource ID of the animation for the outgoing activity. Use 0 for no animation.

`backgroundColor`

`int`: The background color to display during the animation. Set to `[Color.TRANSPARENT](/reference/android/graphics/Color#TRANSPARENT)` to use the default background color.

**See also:**

*   `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))`
*   `[clearOverrideActivityTransition(int)](/reference/android/app/Activity#clearOverrideActivityTransition\(int\))`
*   `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)`
*   `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))`
*   `[Window.setWindowAnimations(int)](/reference/android/view/Window#setWindowAnimations\(int\))`
*   `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`

### overrideActivityTransition

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void overrideActivityTransition (int overrideType, 
                int enterAnim, 
                int exitAnim)

Customizes the animation for activity transitions.

This method provides a robust way to override transition animations at runtime and can be called at any point in the activity's lifecycle. It is particularly useful for handling predictive back animations, for which `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))` is not suitable.

The specified animations will be applied only when this activity is at the top of the task stack during the transition. For example, to customize the opening animation for an Activity B started from Activity A, call this method in B's `onCreate` with `overrideType = OVERRIDE_TRANSITION_OPEN`. To customize the closing animation when returning from B to A, call this method in B with `overrideType = OVERRIDE_TRANSITION_CLOSE`.

**Animation Precedence:**

*   Animations set by `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))` have the highest priority.
*   Animations set by this method have higher priority than those defined by `[Window.setWindowAnimations(int)](/reference/android/view/Window#setWindowAnimations\(int\))`.

**Predictive Back Animation:**

When predictive back is enabled (see `enableOnBackInvokedCallback` in the manifest), the system may control the preview phase of the back animation. The custom transition defined by `enterAnim` and `exitAnim` is then applied only after the back gesture is committed.

**Limitations:**

*   This method, along with `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))` and `[Window.setWindowAnimations(int)](/reference/android/view/Window#setWindowAnimations\(int\))`, is ignored if the activity is started with `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`.
*   As of Android 11, this method cannot be used to customize cross-task transitions.

Parameters

`overrideType`

`int`: The type of transition to override. Must be either `OVERRIDE_TRANSITION_OPEN` for entering transitions or `OVERRIDE_TRANSITION_CLOSE` for exiting transitions. Value is `[OVERRIDE_TRANSITION_OPEN](/reference/android/app/Activity#OVERRIDE_TRANSITION_OPEN)`, or `[OVERRIDE_TRANSITION_CLOSE](/reference/android/app/Activity#OVERRIDE_TRANSITION_CLOSE)`

`enterAnim`

`int`: The resource ID of the animation for the incoming activity. Use 0 for no animation.

`exitAnim`

`int`: The resource ID of the animation for the outgoing activity. Use 0 for no animation.

**See also:**

*   `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`
*   `[clearOverrideActivityTransition(int)](/reference/android/app/Activity#clearOverrideActivityTransition\(int\))`
*   `[OnBackInvokedCallback](/reference/android/window/OnBackInvokedCallback)`
*   `[overridePendingTransition(int, int)](/reference/android/app/Activity#overridePendingTransition\(int,%20int\))`
*   `[Window.setWindowAnimations(int)](/reference/android/view/Window#setWindowAnimations\(int\))`
*   `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`

### overridePendingTransition

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void overridePendingTransition (int enterAnim, 
                int exitAnim)

**This method was deprecated in API level 34.**  
Use `[overrideActivityTransition(int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int\))`} instead.

Call immediately after one of the flavors of `[startActivity(android.content.Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))` or `[finish()](/reference/android/app/Activity#finish\(\))` to specify an explicit transition animation to perform next.

As of `[Build.VERSION_CODES.JELLY_BEAN](/reference/android/os/Build.VERSION_CODES#JELLY_BEAN)` an alternative to using this with starting activities is to supply the desired animation information through a `[ActivityOptions](/reference/android/app/ActivityOptions)` bundle to `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))` or a related function. This allows you to specify a custom animation even when starting an activity from outside the context of the current top activity.

Af of `[Build.VERSION_CODES.S](/reference/android/os/Build.VERSION_CODES#S)` application can only specify a transition animation when the transition happens within the same task. System default animation is used for cross-task transition animations.

Parameters

`enterAnim`

`int`: A resource ID of the animation resource to use for the incoming activity. Use 0 for no animation.

`exitAnim`

`int`: A resource ID of the animation resource to use for the outgoing activity. Use 0 for no animation.

### overridePendingTransition

Added in [API level 33](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void overridePendingTransition (int enterAnim, 
                int exitAnim, 
                int backgroundColor)

**This method was deprecated in API level 34.**  
Use `[overrideActivityTransition(int, int, int, int)](/reference/android/app/Activity#overrideActivityTransition\(int,%20int,%20int,%20int\))`} instead.

Call immediately after one of the flavors of `[startActivity(android.content.Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))` or `[finish()](/reference/android/app/Activity#finish\(\))` to specify an explicit transition animation to perform next.

As of `[Build.VERSION_CODES.JELLY_BEAN](/reference/android/os/Build.VERSION_CODES#JELLY_BEAN)` an alternative to using this with starting activities is to supply the desired animation information through a `[ActivityOptions](/reference/android/app/ActivityOptions)` bundle to `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))` or a related function. This allows you to specify a custom animation even when starting an activity from outside the context of the current top activity.

Parameters

`enterAnim`

`int`: A resource ID of the animation resource to use for the incoming activity. Use 0 for no animation.

`exitAnim`

`int`: A resource ID of the animation resource to use for the outgoing activity. Use 0 for no animation.

`backgroundColor`

`int`: The background color to use for the background during the animation if the animation requires a background. Set to 0 to not override the default color.

### postponeEnterTransition

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void postponeEnterTransition ()

Postpone the entering activity transition when Activity was started with `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`.

This method gives the Activity the ability to delay starting the entering and shared element transitions until all data is loaded. Until then, the Activity won't draw into its window, leaving the window transparent. This may also cause the returning animation to be delayed until data is ready. This method should be called in `[onCreate(android.os.Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` or in `[onActivityReenter(int, android.content.Intent)](/reference/android/app/Activity#onActivityReenter\(int,%20android.content.Intent\))`. `[startPostponedEnterTransition()](/reference/android/app/Activity#startPostponedEnterTransition\(\))` must be called to allow the Activity to start the transitions. If the Activity did not use `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.util.Pair[])](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.util.Pair<android.view.View,java.lang.String>[]\))`, then this method does nothing.

### recreate

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void recreate ()

Cause this Activity to be recreated with a new instance. This results in essentially the same flow as when the Activity is created due to a configuration change -- the current instance will go through its lifecycle to `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))` and a new instance then created after it.

### registerActivityLifecycleCallbacks

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void registerActivityLifecycleCallbacks ([Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks) callback)

Register an `[Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks)` instance that receives lifecycle callbacks for only this Activity.

In relation to any `[Application registered callbacks](/reference/android/app/Application#registerActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))`, the callbacks registered here will always occur nested within those callbacks. This means:

*   Pre events will first be sent to Application registered callbacks, then to callbacks registered here.
*   `[Application.ActivityLifecycleCallbacks.onActivityCreated(Activity, Bundle)](/reference/android/app/Application.ActivityLifecycleCallbacks#onActivityCreated\(android.app.Activity,%20android.os.Bundle\))`, `[Application.ActivityLifecycleCallbacks.onActivityStarted(Activity)](/reference/android/app/Application.ActivityLifecycleCallbacks#onActivityStarted\(android.app.Activity\))`, and `[Application.ActivityLifecycleCallbacks.onActivityResumed(Activity)](/reference/android/app/Application.ActivityLifecycleCallbacks#onActivityResumed\(android.app.Activity\))` will be sent first to Application registered callbacks, then to callbacks registered here. For all other events, callbacks registered here will be sent first.
*   Post events will first be sent to callbacks registered here, then to Application registered callbacks.

If multiple callbacks are registered here, they receive events in a first in (up through `[Application.ActivityLifecycleCallbacks.onActivityPostResumed](/reference/android/app/Application.ActivityLifecycleCallbacks#onActivityPostResumed\(android.app.Activity\))`, last out ordering.

It is strongly recommended to register this in the constructor of your Activity to ensure you get all available callbacks. As this callback is associated with only this Activity, it is not usually necessary to `[unregister](/reference/android/app/Activity#unregisterActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))` it unless you specifically do not want to receive further lifecycle callbacks.

Parameters

`callback`

`Application.ActivityLifecycleCallbacks`: The callback instance to register This value cannot be `null`.

### registerComponentCallbacks

Added in [API level 14](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void registerComponentCallbacks ([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)

Add a new `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` to the base application of the Context, which will be called at the same times as the ComponentCallbacks methods of activities and other components are called. Note that you _must_ be sure to use `[unregisterComponentCallbacks(ComponentCallbacks)](/reference/android/content/ContextWrapper#unregisterComponentCallbacks\(android.content.ComponentCallbacks\))` when appropriate in the future; this will not be removed for you.

After `[Build.VERSION_CODES.TIRAMISU](/reference/android/os/Build.VERSION_CODES#TIRAMISU)`, the `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` will be registered to `[the base Context](/reference/android/content/ContextWrapper#getBaseContext\(\))`, and can be only used after `[attachBaseContext(android.content.Context)](/reference/android/content/ContextWrapper#attachBaseContext\(android.content.Context\))`. Users can still call to `getApplicationContext().registerComponentCallbacks(ComponentCallbacks)` to add `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` to the base application.

Parameters

`callback`

`ComponentCallbacks`: The interface to call. This can be either a `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` or `[ComponentCallbacks2](/reference/android/content/ComponentCallbacks2)` interface.

### registerForContextMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void registerForContextMenu ([View](/reference/android/view/View) view)

Registers a context menu to be shown for the given view (multiple views can show the context menu). This method will set the `[OnCreateContextMenuListener](/reference/android/view/View.OnCreateContextMenuListener)` on the view to this activity, so `[onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)](/reference/android/app/Activity#onCreateContextMenu\(android.view.ContextMenu,%20android.view.View,%20android.view.ContextMenu.ContextMenuInfo\))` will be called when it is time to show the context menu.

Parameters

`view`

`View`: The view that should show a context menu.

**See also:**

*   `[unregisterForContextMenu(View)](/reference/android/app/Activity#unregisterForContextMenu\(android.view.View\))`

### registerScreenCaptureCallback

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void registerScreenCaptureCallback ([Executor](/reference/java/util/concurrent/Executor) executor, 
                [Activity.ScreenCaptureCallback](/reference/android/app/Activity.ScreenCaptureCallback) callback)

Registers a screen capture callback for this activity. The callback will be triggered when a screen capture of this activity is attempted. This callback will be executed on the thread of the passed `executor`. For details, see `[ScreenCaptureCallback.onScreenCaptured](/reference/android/app/Activity.ScreenCaptureCallback#onScreenCaptured\(\))`.  
Requires `[Manifest.permission.DETECT_SCREEN_CAPTURE](/reference/android/Manifest.permission#DETECT_SCREEN_CAPTURE)`

Parameters

`executor`

`Executor`: This value cannot be `null`. Callback and listener events are dispatched through this `[Executor](/reference/java/util/concurrent/Executor)`, providing an easy way to control which thread is used. To dispatch events through the main thread of your application, you can use `[Context.getMainExecutor()](/reference/android/content/Context#getMainExecutor\(\))`. Otherwise, provide an `[Executor](/reference/java/util/concurrent/Executor)` that dispatches to an appropriate thread.

`callback`

`Activity.ScreenCaptureCallback`: This value cannot be `null`.

### releaseInstance

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean releaseInstance ()

Ask that the local app instance of this activity be released to free up its memory. This is asking for the activity to be destroyed, but does **not** finish the activity -- a new instance of the activity will later be re-created if needed due to the user navigating back to it.

Returns

`boolean`

Returns true if the activity was in a state that it has started the process of destroying its current instance; returns false if for any reason this could not be done: it is currently visible to the user, it is already being destroyed, it is being finished, it hasn't yet saved its state, etc.

### removeDialog

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void removeDialog (int id)

**This method was deprecated in API level 15.**  
Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package.

Removes any internal references to a dialog managed by this Activity. If the dialog is showing, it will dismiss it as part of the clean up.

This can be useful if you know that you will never show a dialog again and want to avoid the overhead of saving and restoring it in the future.

As of `[Build.VERSION_CODES.GINGERBREAD](/reference/android/os/Build.VERSION_CODES#GINGERBREAD)`, this function will not throw an exception if you try to remove an ID that does not currently have an associated dialog.

Parameters

`id`

`int`: The id of the managed dialog.

**See also:**

*   `[onCreateDialog(int, Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))`
*   `[onPrepareDialog(int, Dialog, Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`
*   `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`
*   `[dismissDialog(int)](/reference/android/app/Activity#dismissDialog\(int\))`

### reportFullyDrawn

Added in [API level 19](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void reportFullyDrawn ()

Report to the system that your app is now fully drawn, for diagnostic and optimization purposes. The system may adjust optimizations to prioritize work that happens before reportFullyDrawn is called, to improve app startup. Misrepresenting the startup window by calling reportFullyDrawn too late or too early may decrease application and startup performance.

This is also used to help instrument application launch times, so that the app can report when it is fully in a usable state; without this, the only thing the system itself can determine is the point at which the activity's window is _first_ drawn and displayed. To participate in app launch time measurement, you should always call this method after first launch (when `[onCreate(android.os.Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` is called), at the point where you have entirely drawn your UI and populated with all of the significant data. You can safely call this method any time after first launch as well, in which case it will simply be ignored.

If this method is called before the activity's window is _first_ drawn and displayed as measured by the system, the reported time here will be shifted to the system measured time.

### requestDragAndDropPermissions

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [DragAndDropPermissions](/reference/android/view/DragAndDropPermissions) requestDragAndDropPermissions ([DragEvent](/reference/android/view/DragEvent) event)

Create `[DragAndDropPermissions](/reference/android/view/DragAndDropPermissions)` object bound to this activity and controlling the access permissions for content URIs associated with the `[DragEvent](/reference/android/view/DragEvent)`.

Parameters

`event`

`DragEvent`: Drag event

Returns

`[DragAndDropPermissions](/reference/android/view/DragAndDropPermissions)`

The `[DragAndDropPermissions](/reference/android/view/DragAndDropPermissions)` object used to control access to the content URIs. Null if no content URIs are associated with the event or if permissions could not be granted.

### requestFullscreenMode

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void requestFullscreenMode (int request, 
                [OutcomeReceiver](/reference/android/os/OutcomeReceiver)<[Void](/reference/java/lang/Void), [Throwable](/reference/java/lang/Throwable)\> approvalCallback)

Request to put the activity into fullscreen. The requester must be pinned or the top-most activity of the focused display which can be verified using `[onTopResumedActivityChanged(boolean)](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))`. The request should also be a response to a user input. When getting fullscreen and receiving corresponding `[onConfigurationChanged(android.content.res.Configuration)](/reference/android/app/Activity#onConfigurationChanged\(android.content.res.Configuration\))` and `[onMultiWindowModeChanged(boolean, android.content.res.Configuration)](/reference/android/app/Activity#onMultiWindowModeChanged\(boolean,%20android.content.res.Configuration\))`, the activity should relayout itself and the system bars' visibilities can be controlled as usual fullscreen apps. Calling it again with the exit request can restore the activity to the previous status. This will only happen when it got into fullscreen through this API.

Parameters

`request`

`int`: Can be `[FULLSCREEN_MODE_REQUEST_ENTER](/reference/android/app/Activity#FULLSCREEN_MODE_REQUEST_ENTER)` or `[FULLSCREEN_MODE_REQUEST_EXIT](/reference/android/app/Activity#FULLSCREEN_MODE_REQUEST_EXIT)` to indicate this request is to get fullscreen or get restored. Value is `[FULLSCREEN_MODE_REQUEST_EXIT](/reference/android/app/Activity#FULLSCREEN_MODE_REQUEST_EXIT)`, or `[FULLSCREEN_MODE_REQUEST_ENTER](/reference/android/app/Activity#FULLSCREEN_MODE_REQUEST_ENTER)`

`approvalCallback`

`OutcomeReceiver`: Optional callback, use `null` when not necessary. When the request is approved or rejected, the callback will be triggered. This will happen before any configuration change. The callback will be dispatched on the main thread. If the request is rejected, the Throwable provided will be an `[IllegalStateException](/reference/java/lang/IllegalStateException)` with a detailed message can be retrieved by `[Throwable.getMessage()](/reference/java/lang/Throwable#getMessage\(\))`.

### requestOpenInBrowserEducation

Added in [API level 36](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void requestOpenInBrowserEducation ()

Requests to show the \\u201cOpen in browser\\u201d education. \\u201cOpen in browser\\u201d is a feature within the app header that allows users to switch from an app to the web. The feature is made available when an application is opened by a user clicking a link or when a link is provided by an application. Links can be provided by calling `[AssistContent.setSessionTransferUri](/reference/android/app/assist/AssistContent#setSessionTransferUri\(android.net.Uri\))` or `[AssistContent.setWebUri](/reference/android/app/assist/AssistContent#setWebUri\(android.net.Uri\))`.

This method should be utilized when an activity wants to nudge the user to switch to the web application in cases where the web may provide the user with a better experience. Note that this method does not guarantee that the education will be shown.

The number of times that the "Open in browser" education can be triggered by this method is limited per application, and, when shown, the education appears above the app's content. For these reasons, developers should use this method sparingly when it is least disruptive to the user to show the education and when it is optimal to switch the user to a browser session. Before requesting to show the education, developers should assert that they have set a link that can be used by the "Open in browser" feature through either `[AssistContent.setSessionTransferUri](/reference/android/app/assist/AssistContent#setSessionTransferUri\(android.net.Uri\))` or `[AssistContent.setWebUri](/reference/android/app/assist/AssistContent#setWebUri\(android.net.Uri\))` so that users are navigated to a relevant page if they choose to switch to the browser. If a URI is not set using either method, "Open in browser" will utilize a generic link if available which will direct users to the homepage of the site associated with the app. The generic link is provided for a limited number of applications by the system and cannot be edited by developers. If none of these options contains a valid URI, the user will not be provided with the option to switch to the browser and the education will not be shown if requested.

**See also:**

*   `[AssistContent.setSessionTransferUri(Uri)](/reference/android/app/assist/AssistContent#setSessionTransferUri\(android.net.Uri\))`

### requestPermissions

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void requestPermissions ([String\[\]](/reference/java/lang/String) permissions, 
                int requestCode, 
                int deviceId)

Requests permissions to be granted to this application. These permissions must be requested in your manifest, they should not be granted to your app, and they should have protection level `[dangerous](/reference/android/content/pm/PermissionInfo#PROTECTION_DANGEROUS)`, regardless whether they are declared by the platform or a third-party app.

Normal permissions `[PermissionInfo.PROTECTION_NORMAL](/reference/android/content/pm/PermissionInfo#PROTECTION_NORMAL)` are granted at install time if requested in the manifest. Signature permissions `[PermissionInfo.PROTECTION_SIGNATURE](/reference/android/content/pm/PermissionInfo#PROTECTION_SIGNATURE)` are granted at install time if requested in the manifest and the signature of your app matches the signature of the app declaring the permissions.

Call `[shouldShowRequestPermissionRationale(String)](/reference/android/app/Activity#shouldShowRequestPermissionRationale\(java.lang.String\))` before calling this API to check if the system recommends to show a rationale UI before asking for a permission.

If your app does not have the requested permissions the user will be presented with UI for accepting them. After the user has accepted or rejected the requested permissions you will receive a callback on `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))` reporting whether the permissions were granted or not.

Note that requesting a permission does not guarantee it will be granted and your app should be able to run without having this permission.

This method may start an activity allowing the user to choose which permissions to grant and which to reject. Hence, you should be prepared that your activity may be paused and resumed. Further, granting some permissions may require a restart of you application. In such a case, the system will recreate the activity stack before delivering the result to `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`.

When checking whether you have a permission you should use `[ContextWrapper.checkSelfPermission(java.lang.String)](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))`.

You cannot request a permission if your activity sets `[noHistory](/reference/android/R.styleable#AndroidManifestActivity_noHistory)` to `true` because in this case the activity would not receive result callbacks including `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`.

The [permissions samples](https://github.com/android/platform-samples/tree/main/samples/privacy/permissions) repo demonstrates how to use this method to request permissions at run time.

Parameters

`permissions`

`String`: The requested permissions. Must be non-null and not empty.

`requestCode`

`int`: Application specific request code to match with a result reported to `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`. Should be >= 0.

`deviceId`

`int`: The app is requesting permissions for this device. The primary/physical device is assigned `[Context.DEVICE_ID_DEFAULT](/reference/android/content/Context#DEVICE_ID_DEFAULT)`, and virtual devices are assigned unique device Ids.

Throws

`[IllegalArgumentException](/reference/java/lang/IllegalArgumentException)`

if requestCode is negative.

**See also:**

*   `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`
*   `[ContextWrapper.checkSelfPermission(String)](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))`
*   `[shouldShowRequestPermissionRationale(String)](/reference/android/app/Activity#shouldShowRequestPermissionRationale\(java.lang.String\))`
*   `[Context.DEVICE_ID_DEFAULT](/reference/android/content/Context#DEVICE_ID_DEFAULT)`

### requestPermissions

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void requestPermissions ([String\[\]](/reference/java/lang/String) permissions, 
                int requestCode)

Requests permissions to be granted to this application. These permissions must be requested in your manifest, they should not be granted to your app, and they should have protection level `[dangerous](/reference/android/content/pm/PermissionInfo#PROTECTION_DANGEROUS)`, regardless whether they are declared by the platform or a third-party app.

Normal permissions `[PermissionInfo.PROTECTION_NORMAL](/reference/android/content/pm/PermissionInfo#PROTECTION_NORMAL)` are granted at install time if requested in the manifest. Signature permissions `[PermissionInfo.PROTECTION_SIGNATURE](/reference/android/content/pm/PermissionInfo#PROTECTION_SIGNATURE)` are granted at install time if requested in the manifest and the signature of your app matches the signature of the app declaring the permissions.

Call `[shouldShowRequestPermissionRationale(String)](/reference/android/app/Activity#shouldShowRequestPermissionRationale\(java.lang.String\))` before calling this API to check if the system recommends to show a rationale UI before asking for a permission.

If your app does not have the requested permissions the user will be presented with UI for accepting them. After the user has accepted or rejected the requested permissions you will receive a callback on `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))` reporting whether the permissions were granted or not.

Note that requesting a permission does not guarantee it will be granted and your app should be able to run without having this permission.

This method may start an activity allowing the user to choose which permissions to grant and which to reject. Hence, you should be prepared that your activity may be paused and resumed. Further, granting some permissions may require a restart of you application. In such a case, the system will recreate the activity stack before delivering the result to `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`.

When checking whether you have a permission you should use `[ContextWrapper.checkSelfPermission(java.lang.String)](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))`.

You cannot request a permission if your activity sets `[noHistory](/reference/android/R.styleable#AndroidManifestActivity_noHistory)` to `true` because in this case the activity would not receive result callbacks including `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`.

The [permissions samples](https://github.com/android/platform-samples/tree/main/samples/privacy/permissions) repo demonstrates how to use this method to request permissions at run time.

Parameters

`permissions`

`String`: The requested permissions. Must be non-null and not empty.

`requestCode`

`int`: Application specific request code to match with a result reported to `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`. Should be >= 0.

Throws

`[IllegalArgumentException](/reference/java/lang/IllegalArgumentException)`

if requestCode is negative.

**See also:**

*   `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`
*   `[ContextWrapper.checkSelfPermission(String)](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))`
*   `[shouldShowRequestPermissionRationale(String)](/reference/android/app/Activity#shouldShowRequestPermissionRationale\(java.lang.String\))`

### requestShowKeyboardShortcuts

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void requestShowKeyboardShortcuts ()

Request the Keyboard Shortcuts screen to show up. This will trigger `[onProvideKeyboardShortcuts(List, Menu, int)](/reference/android/app/Activity#onProvideKeyboardShortcuts\(java.util.List<android.view.KeyboardShortcutGroup>,%20android.view.Menu,%20int\))` to retrieve the shortcuts for the foreground activity.

### requestVisibleBehind

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean requestVisibleBehind (boolean visible)

**This method was deprecated in API level 26.**  
This method's functionality is no longer supported as of `[Build.VERSION_CODES.O](/reference/android/os/Build.VERSION_CODES#O)` and will be removed in a future release.

Activities that want to remain visible behind a translucent activity above them must call this method anytime between the start of `[onResume()](/reference/android/app/Activity#onResume\(\))` and the return from `[onPause()](/reference/android/app/Activity#onPause\(\))`. If this call is successful then the activity will remain visible after `[onPause()](/reference/android/app/Activity#onPause\(\))` is called, and is allowed to continue playing media in the background.

The actions of this call are reset each time that this activity is brought to the front. That is, every time `[onResume()](/reference/android/app/Activity#onResume\(\))` is called the activity will be assumed to not have requested visible behind. Therefore, if you want this activity to continue to be visible in the background you must call this method again.

Only fullscreen opaque activities may make this call. I.e. this call is a nop for dialog and translucent activities.

Under all circumstances, the activity must stop playing and release resources prior to or within a call to `[onVisibleBehindCanceled()](/reference/android/app/Activity#onVisibleBehindCanceled\(\))` or if this call returns false.

False will be returned any time this method is called between the return of onPause and the next call to onResume.

Parameters

`visible`

`boolean`: true to notify the system that the activity wishes to be visible behind other translucent activities, false to indicate otherwise. Resources must be released when passing false to this method.

Returns

`boolean`

the resulting visibiity state. If true the activity will remain visible beyond `[onPause()](/reference/android/app/Activity#onPause\(\))` if the next activity is translucent or not fullscreen. If false then the activity may not count on being visible behind other translucent activities, and must stop any media playback and release resources. Returning false may occur in lieu of a call to `[onVisibleBehindCanceled()](/reference/android/app/Activity#onVisibleBehindCanceled\(\))` so the return value must be checked.

**See also:**

*   `[onVisibleBehindCanceled()](/reference/android/app/Activity#onVisibleBehindCanceled\(\))`

### requestWindowFeature

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final boolean requestWindowFeature (int featureId)

Enable extended window features. This is a convenience for calling `[getWindow().requestFeature()](/reference/android/view/Window#requestFeature\(int\))`.

Parameters

`featureId`

`int`: The desired feature as defined in `[Window](/reference/android/view/Window)`.

Returns

`boolean`

Returns true if the requested feature is supported and now enabled.

**See also:**

*   `[Window.requestFeature(int)](/reference/android/view/Window#requestFeature\(int\))`

### requireViewById

Added in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final T requireViewById (int id)

Finds a view that was identified by the `android:id` XML attribute that was processed in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`, or throws an IllegalArgumentException if the ID is invalid, or there is no matching view in the hierarchy.

**Note:** In most cases -- depending on compiler support -- the resulting view is automatically cast to the target class type. If the target class type is unconstrained, an explicit cast may be necessary.

Parameters

`id`

`int`: the ID to search for

Returns

`T`

a view with given ID This value cannot be `null`.

**See also:**

*   `[View.requireViewById(int)](/reference/android/view/View#requireViewById\(int\))`
*   `[findViewById(int)](/reference/android/app/Activity#findViewById\(int\))`

### runOnUiThread

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void runOnUiThread ([Runnable](/reference/java/lang/Runnable) action)

Runs the specified action on the UI thread. If the current thread is the UI thread, then the action is executed immediately. If the current thread is not the UI thread, the action is posted to the event queue of the UI thread.

Parameters

`action`

`Runnable`: the action to run on the UI thread

### setActionBar

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setActionBar ([Toolbar](/reference/android/widget/Toolbar) toolbar)

Set a `[Toolbar](/reference/android/widget/Toolbar)` to act as the `[ActionBar](/reference/android/app/ActionBar)` for this Activity window.

When set to a non-null value the `[getActionBar()](/reference/android/app/Activity#getActionBar\(\))` method will return an `[ActionBar](/reference/android/app/ActionBar)` object that can be used to control the given toolbar as if it were a traditional window decor action bar. The toolbar's menu will be populated with the Activity's options menu and the navigation button will be wired through the standard `[home](/reference/android/R.id#home)` menu select action.

In order to use a Toolbar within the Activity's window content the application must not request the window feature `[FEATURE_ACTION_BAR](/reference/android/view/Window#FEATURE_ACTION_BAR)`.

Parameters

`toolbar`

`Toolbar`: Toolbar to set as the Activity's action bar, or `null` to clear it

### setAllowCrossUidActivitySwitchFromBelow

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setAllowCrossUidActivitySwitchFromBelow (boolean allowed)

Specifies whether the activities below this one in the task can also start other activities or finish the task.

Starting from Target SDK Level `[Build.VERSION_CODES.VANILLA_ICE_CREAM](/reference/android/os/Build.VERSION_CODES#VANILLA_ICE_CREAM)`, apps may be blocked from starting new activities or finishing their task unless the top activity of such task belong to the same UID for security reasons.

Setting this flag to `true` will allow the launching app to ignore the restriction if this activity is on top. Apps matching the UID of this activity are always exempt.

Parameters

`allowed`

`boolean`: `true` to disable the UID restrictions; `false` to revert back to the default behaviour

### setContentTransitionManager

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setContentTransitionManager ([TransitionManager](/reference/android/transition/TransitionManager) tm)

Set the `[TransitionManager](/reference/android/transition/TransitionManager)` to use for default transitions in this window. Requires `[Window.FEATURE_CONTENT_TRANSITIONS](/reference/android/view/Window#FEATURE_CONTENT_TRANSITIONS)`.

Parameters

`tm`

`TransitionManager`: The TransitionManager to use for scene changes.

### setContentView

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setContentView ([View](/reference/android/view/View) view, 
                [ViewGroup.LayoutParams](/reference/android/view/ViewGroup.LayoutParams) params)

Set the activity content to an explicit view. This view is placed directly into the activity's view hierarchy. It can itself be a complex view hierarchy.

Parameters

`view`

`View`: The desired content to display.

`params`

`ViewGroup.LayoutParams`: Layout parameters for the view.

**See also:**

*   `[setContentView(android.view.View)](/reference/android/app/Activity#setContentView\(android.view.View\))`
*   `[setContentView(int)](/reference/android/app/Activity#setContentView\(int\))`

### setContentView

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setContentView ([View](/reference/android/view/View) view)

Set the activity content to an explicit view. This view is placed directly into the activity's view hierarchy. It can itself be a complex view hierarchy. When calling this method, the layout parameters of the specified view are ignored. Both the width and the height of the view are set by default to `[ViewGroup.LayoutParams.MATCH_PARENT](/reference/android/view/ViewGroup.LayoutParams#MATCH_PARENT)`. To use your own layout parameters, invoke `[setContentView(android.view.View, android.view.ViewGroup.LayoutParams)](/reference/android/app/Activity#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))` instead.

Parameters

`view`

`View`: The desired content to display.

**See also:**

*   `[setContentView(int)](/reference/android/app/Activity#setContentView\(int\))`
*   `[setContentView(android.view.View, android.view.ViewGroup.LayoutParams)](/reference/android/app/Activity#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))`

### setContentView

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setContentView (int layoutResID)

Set the activity content from a layout resource. The resource will be inflated, adding all top-level views to the activity.

Parameters

`layoutResID`

`int`: Resource ID to be inflated.

**See also:**

*   `[setContentView(android.view.View)](/reference/android/app/Activity#setContentView\(android.view.View\))`
*   `[setContentView(android.view.View, android.view.ViewGroup.LayoutParams)](/reference/android/app/Activity#setContentView\(android.view.View,%20android.view.ViewGroup.LayoutParams\))`

### setDefaultKeyMode

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setDefaultKeyMode (int mode)

Select the default key handling for this activity. This controls what will happen to key events that are not otherwise handled. The default mode (`[DEFAULT_KEYS_DISABLE](/reference/android/app/Activity#DEFAULT_KEYS_DISABLE)`) will simply drop them on the floor. Other modes allow you to launch the dialer (`[DEFAULT_KEYS_DIALER](/reference/android/app/Activity#DEFAULT_KEYS_DIALER)`), execute a shortcut in your options menu without requiring the menu key be held down (`[DEFAULT_KEYS_SHORTCUT](/reference/android/app/Activity#DEFAULT_KEYS_SHORTCUT)`), or launch a search (`[DEFAULT_KEYS_SEARCH_LOCAL](/reference/android/app/Activity#DEFAULT_KEYS_SEARCH_LOCAL)` and `[DEFAULT_KEYS_SEARCH_GLOBAL](/reference/android/app/Activity#DEFAULT_KEYS_SEARCH_GLOBAL)`).

Note that the mode selected here does not impact the default handling of system keys, such as the "back" and "menu" keys, and your activity and its views always get a first chance to receive and handle all application keys.

Parameters

`mode`

`int`: The desired default key mode constant. Value is `[DEFAULT_KEYS_DISABLE](/reference/android/app/Activity#DEFAULT_KEYS_DISABLE)`, `[DEFAULT_KEYS_DIALER](/reference/android/app/Activity#DEFAULT_KEYS_DIALER)`, `[DEFAULT_KEYS_SHORTCUT](/reference/android/app/Activity#DEFAULT_KEYS_SHORTCUT)`, `[DEFAULT_KEYS_SEARCH_LOCAL](/reference/android/app/Activity#DEFAULT_KEYS_SEARCH_LOCAL)`, or `[DEFAULT_KEYS_SEARCH_GLOBAL](/reference/android/app/Activity#DEFAULT_KEYS_SEARCH_GLOBAL)`

**See also:**

*   `[onKeyDown(int, KeyEvent)](/reference/android/app/Activity#onKeyDown\(int,%20android.view.KeyEvent\))`

### setEnterSharedElementCallback

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setEnterSharedElementCallback ([SharedElementCallback](/reference/android/app/SharedElementCallback) callback)

When `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.view.View, String)](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.view.View,%20java.lang.String\))` was used to start an Activity, callback will be called to handle shared elements on the _launched_ Activity. This requires `[Window.FEATURE_ACTIVITY_TRANSITIONS](/reference/android/view/Window#FEATURE_ACTIVITY_TRANSITIONS)`.

Parameters

`callback`

`SharedElementCallback`: Used to manipulate shared element transitions on the launched Activity.

### setExitSharedElementCallback

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setExitSharedElementCallback ([SharedElementCallback](/reference/android/app/SharedElementCallback) callback)

When `[ActivityOptions.makeSceneTransitionAnimation(Activity, android.view.View, String)](/reference/android/app/ActivityOptions#makeSceneTransitionAnimation\(android.app.Activity,%20android.view.View,%20java.lang.String\))` was used to start an Activity, callback will be called to handle shared elements on the _launching_ Activity. Most calls will only come when returning from the started Activity. This requires `[Window.FEATURE_ACTIVITY_TRANSITIONS](/reference/android/view/Window#FEATURE_ACTIVITY_TRANSITIONS)`.

Parameters

`callback`

`SharedElementCallback`: Used to manipulate shared element transitions on the launching Activity.

### setFeatureDrawable

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setFeatureDrawable (int featureId, 
                [Drawable](/reference/android/graphics/drawable/Drawable) drawable)

Convenience for calling `[Window.setFeatureDrawable(int, Drawable)](/reference/android/view/Window#setFeatureDrawable\(int,%20android.graphics.drawable.Drawable\))`.

Parameters

`featureId`

`int`

`drawable`

`Drawable`

### setFeatureDrawableAlpha

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setFeatureDrawableAlpha (int featureId, 
                int alpha)

Convenience for calling `[Window.setFeatureDrawableAlpha(int, int)](/reference/android/view/Window#setFeatureDrawableAlpha\(int,%20int\))`.

Parameters

`featureId`

`int`

`alpha`

`int`

### setFeatureDrawableResource

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setFeatureDrawableResource (int featureId, 
                int resId)

Convenience for calling `[Window.setFeatureDrawableResource(int, int)](/reference/android/view/Window#setFeatureDrawableResource\(int,%20int\))`.

Parameters

`featureId`

`int`

`resId`

`int`

### setFeatureDrawableUri

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setFeatureDrawableUri (int featureId, 
                [Uri](/reference/android/net/Uri) uri)

Convenience for calling `[Window.setFeatureDrawableUri(int, Uri)](/reference/android/view/Window#setFeatureDrawableUri\(int,%20android.net.Uri\))`.

Parameters

`featureId`

`int`

`uri`

`Uri`

### setFinishOnTouchOutside

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setFinishOnTouchOutside (boolean finish)

Sets whether this activity is finished when touched outside its window's bounds.

Parameters

`finish`

`boolean`

### setImmersive

Added in [API level 18](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setImmersive (boolean i)

Adjust the current immersive mode setting. Note that changing this value will have no effect on the activity's `[ActivityInfo](/reference/android/content/pm/ActivityInfo)` structure; that is, if `android:immersive` is set to `true` in the application's manifest entry for this activity, the `[ActivityInfo.flags](/reference/android/content/pm/ActivityInfo#flags)` member will always have its `[FLAG_IMMERSIVE](/reference/android/content/pm/ActivityInfo#FLAG_IMMERSIVE)` bit set.

Parameters

`i`

`boolean`

**See also:**

*   `[isImmersive()](/reference/android/app/Activity#isImmersive\(\))`
*   `[ActivityInfo.FLAG_IMMERSIVE](/reference/android/content/pm/ActivityInfo#FLAG_IMMERSIVE)`

### setInheritShowWhenLocked

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setInheritShowWhenLocked (boolean inheritShowWhenLocked)

Specifies whether this `[Activity](/reference/android/app/Activity)` should be shown on top of the lock screen whenever the lockscreen is up and this activity has another activity behind it with the showWhenLock attribute set. That is, this activity is only visible on the lock screen if there is another activity with the showWhenLock attribute visible at the same time on the lock screen. A use case for this is permission dialogs, that should only be visible on the lock screen if their requesting activity is also visible. This value can be set as a manifest attribute using android.R.attr#inheritShowWhenLocked.

Parameters

`inheritShowWhenLocked`

`boolean`: `true` to show the `[Activity](/reference/android/app/Activity)` on top of the lock screen when this activity has another activity behind it with the showWhenLock attribute set; `false` otherwise.

**See also:**

*   `[setShowWhenLocked(boolean)](/reference/android/app/Activity#setShowWhenLocked\(boolean\))`
*   `[R.attr.inheritShowWhenLocked](/reference/android/R.attr#inheritShowWhenLocked)`

### setIntent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setIntent ([Intent](/reference/android/content/Intent) newIntent)

Changes the intent returned by `[getIntent()](/reference/android/app/Activity#getIntent\(\))`. This holds a reference to the given intent; it does not copy it. Often used in conjunction with `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`.

Parameters

`newIntent`

`Intent`: The new Intent object to return from `[getIntent()](/reference/android/app/Activity#getIntent\(\))`

**See also:**

*   `[getIntent()](/reference/android/app/Activity#getIntent\(\))`
*   `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`

### setIntent

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setIntent ([Intent](/reference/android/content/Intent) newIntent, 
                [ComponentCaller](/reference/android/app/ComponentCaller) newCaller)

Changes the intent returned by `[getIntent()](/reference/android/app/Activity#getIntent\(\))`, and ComponentCaller returned by `[getCaller()](/reference/android/app/Activity#getCaller\(\))`. This holds references to the given intent, and ComponentCaller; it does not copy them. Often used in conjunction with `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`. To retrieve the caller from `[onNewIntent(android.content.Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))`, use `[getCurrentCaller()](/reference/android/app/Activity#getCurrentCaller\(\))`, otherwise override `[onNewIntent(android.content.Intent, android.app.ComponentCaller)](/reference/android/app/Activity#onNewIntent\(android.content.Intent,%20android.app.ComponentCaller\))`.

Parameters

`newIntent`

`Intent`: The new Intent object to return from `[getIntent()](/reference/android/app/Activity#getIntent\(\))` This value may be `null`.

`newCaller`

`ComponentCaller`: The new `[ComponentCaller](/reference/android/app/ComponentCaller)` object to return from `[getCaller()](/reference/android/app/Activity#getCaller\(\))` This value may be `null`.

**See also:**

*   `[getIntent()](/reference/android/app/Activity#getIntent\(\))`
*   `[onNewIntent(Intent, ComponentCaller)](/reference/android/app/Activity#onNewIntent\(android.content.Intent,%20android.app.ComponentCaller\))`
*   `[getCaller()](/reference/android/app/Activity#getCaller\(\))`

### setLocusContext

Added in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setLocusContext ([LocusId](/reference/android/content/LocusId) locusId, 
                [Bundle](/reference/android/os/Bundle) bundle)

Sets the `[LocusId](/reference/android/content/LocusId)` for this activity. The locus id helps identify different instances of the same `Activity` class.

For example, a locus id based on a specific conversation could be set on a conversation app's chat `Activity`. The system can then use this locus id along with app's contents to provide ranking signals in various UI surfaces including sharing, notifications, shortcuts and so on.

It is recommended to set the same locus id in the shortcut's locus id using `[setLocusId](/reference/android/content/pm/ShortcutInfo.Builder#setLocusId\(android.content.LocusId\))` so that the system can learn appropriate ranking signals linking the activity's locus id with the matching shortcut.

Parameters

`locusId`

`LocusId`: a unique, stable id that identifies this `Activity` instance. LocusId is an opaque ID that links this Activity's state to different Android concepts: `[setLocusId](/reference/android/content/pm/ShortcutInfo.Builder#setLocusId\(android.content.LocusId\))`. LocusID is null by default or if you explicitly reset it.

`bundle`

`Bundle`: extras set or updated as part of this locus context. This may help provide additional metadata such as URLs, conversation participants specific to this `Activity`'s context. Bundle can be null if additional metadata is not needed. Bundle should always be null for null locusId.

**See also:**

*   `[ContentCaptureManager](/reference/android/view/contentcapture/ContentCaptureManager)`
*   `[ContentCaptureContext](/reference/android/view/contentcapture/ContentCaptureContext)`

### setMediaController

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setMediaController ([MediaController](/reference/android/media/session/MediaController) controller)

Sets a `[MediaController](/reference/android/media/session/MediaController)` to send media keys and volume changes to.

The controller will be tied to the window of this Activity. Media key and volume events which are received while the Activity is in the foreground will be forwarded to the controller and used to invoke transport controls or adjust the volume. This may be used instead of or in addition to `[setVolumeControlStream(int)](/reference/android/app/Activity#setVolumeControlStream\(int\))` to affect a specific session instead of a specific stream.

It is not guaranteed that the hardware volume controls will always change this session's volume (for example, if a call is in progress, its stream's volume may be changed instead). To reset back to the default use null as the controller.

Parameters

`controller`

`MediaController`: The controller for the session which should receive media keys and volume changes.

### setPictureInPictureParams

Added in [API level 26](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setPictureInPictureParams ([PictureInPictureParams](/reference/android/app/PictureInPictureParams) params)

Updates the properties of the picture-in-picture activity, or sets it to be used later when `[enterPictureInPictureMode()](/reference/android/app/Activity#enterPictureInPictureMode\(\))` is called.

Parameters

`params`

`PictureInPictureParams`: the new parameters for the picture-in-picture. This value cannot be `null`.

### setProgress

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setProgress (int progress)

**This method was deprecated in API level 24.**  
No longer supported starting in API 21.

Sets the progress for the progress bars in the title.

In order for the progress bar to be shown, the feature must be requested via `[requestWindowFeature(int)](/reference/android/app/Activity#requestWindowFeature\(int\))`.

Parameters

`progress`

`int`: The progress for the progress bar. Valid ranges are from 0 to 10000 (both inclusive). If 10000 is given, the progress bar will be completely filled and will fade out.

### setProgressBarIndeterminate

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setProgressBarIndeterminate (boolean indeterminate)

**This method was deprecated in API level 24.**  
No longer supported starting in API 21.

Sets whether the horizontal progress bar in the title should be indeterminate (the circular is always indeterminate).

In order for the progress bar to be shown, the feature must be requested via `[requestWindowFeature(int)](/reference/android/app/Activity#requestWindowFeature\(int\))`.

Parameters

`indeterminate`

`boolean`: Whether the horizontal progress bar should be indeterminate.

### setProgressBarIndeterminateVisibility

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setProgressBarIndeterminateVisibility (boolean visible)

**This method was deprecated in API level 24.**  
No longer supported starting in API 21.

Sets the visibility of the indeterminate progress bar in the title.

In order for the progress bar to be shown, the feature must be requested via `[requestWindowFeature(int)](/reference/android/app/Activity#requestWindowFeature\(int\))`.

Parameters

`visible`

`boolean`: Whether to show the progress bars in the title.

### setProgressBarVisibility

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setProgressBarVisibility (boolean visible)

**This method was deprecated in API level 24.**  
No longer supported starting in API 21.

Sets the visibility of the progress bar in the title.

In order for the progress bar to be shown, the feature must be requested via `[requestWindowFeature(int)](/reference/android/app/Activity#requestWindowFeature\(int\))`.

Parameters

`visible`

`boolean`: Whether to show the progress bars in the title.

### setRecentsScreenshotEnabled

Added in [API level 33](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setRecentsScreenshotEnabled (boolean enabled)

If set to false, this indicates to the system that it should never take a screenshot of the activity to be used as a representation in recents screen. By default, this value is `true`.

Note that the system may use the window background of the theme instead to represent the window when it is not running.

Also note that in comparison to `[WindowManager.LayoutParams.FLAG_SECURE](/reference/android/view/WindowManager.LayoutParams#FLAG_SECURE)`, this only affects the behavior when the activity's screenshot would be used as a representation when the activity is not in a started state, i.e. in Overview. The system may still take screenshots of the activity in other contexts; for example, when the user takes a screenshot of the entire screen, or when the active `[VoiceInteractionService](/reference/android/service/voice/VoiceInteractionService)` requests a screenshot via `[VoiceInteractionSession.SHOW_WITH_SCREENSHOT](/reference/android/service/voice/VoiceInteractionSession#SHOW_WITH_SCREENSHOT)`.

Parameters

`enabled`

`boolean`: `true` to enable recents screenshots; `false` otherwise.

### setRequestedOrientation

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setRequestedOrientation (int requestedOrientation)

Change the desired orientation of this activity. If the activity is currently in the foreground or otherwise impacting the screen orientation, the screen is immediately changed (possibly causing the activity to be restarted). Otherwise, the new orientation is used the next time the activity is visible.

**Note:**

*   To improve the layout of apps on form factors with smallest width >= 600dp, the system ignores calls to this method for apps that target Android 16 (API level 36) or higher.
*   Device manufacturers can configure devices to ignore calls to this method to improve the layout of orientation-restricted apps.
*   On devices with Android 16 (API level 36) or higher installed, virtual device owners (select trusted and privileged apps) can optimize app layout on displays they manage by ignoring calls to this method. See also [Companion app streaming](https://source.android.com/docs/core/permissions/app-streaming).

See [Device compatibility mode](/guide/practices/device-compatibility-mode).

Parameters

`requestedOrientation`

`int`: An orientation constant as used in `[ActivityInfo.screenOrientation](/reference/android/content/pm/ActivityInfo#screenOrientation)`. Value is android.content.pm.ActivityInfo.SCREEN\_ORIENTATION\_UNSET, `[ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_UNSPECIFIED)`, `[ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_USER](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_USER)`, `[ActivityInfo.SCREEN_ORIENTATION_BEHIND](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_BEHIND)`, `[ActivityInfo.SCREEN_ORIENTATION_SENSOR](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_SENSOR)`, `[ActivityInfo.SCREEN_ORIENTATION_NOSENSOR](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_NOSENSOR)`, `[ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_SENSOR_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_SENSOR_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_REVERSE_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_REVERSE_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_FULL_SENSOR)`, `[ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_USER_LANDSCAPE)`, `[ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_USER_PORTRAIT)`, `[ActivityInfo.SCREEN_ORIENTATION_FULL_USER](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_FULL_USER)`, or `[ActivityInfo.SCREEN_ORIENTATION_LOCKED](/reference/android/content/pm/ActivityInfo#SCREEN_ORIENTATION_LOCKED)`

### setResult

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setResult (int resultCode, 
                [Intent](/reference/android/content/Intent) data)

Call this to set the result that your activity will return to its caller.

As of `[Build.VERSION_CODES.GINGERBREAD](/reference/android/os/Build.VERSION_CODES#GINGERBREAD)`, the Intent you supply here can have `[Intent.FLAG_GRANT_READ_URI_PERMISSION](/reference/android/content/Intent#FLAG_GRANT_READ_URI_PERMISSION)` and/or `[Intent.FLAG_GRANT_WRITE_URI_PERMISSION](/reference/android/content/Intent#FLAG_GRANT_WRITE_URI_PERMISSION)` set. This will grant the Activity receiving the result access to the specific URIs in the Intent. Access will remain until the Activity has finished (it will remain across the hosting process being killed and other temporary destruction) and will be added to any existing set of URI permissions it already holds.

Parameters

`resultCode`

`int`: The result code to propagate back to the originating activity, often RESULT\_CANCELED or RESULT\_OK

`data`

`Intent`: The data to propagate back to the originating activity.

**See also:**

*   `[RESULT_CANCELED](/reference/android/app/Activity#RESULT_CANCELED)`
*   `[RESULT_OK](/reference/android/app/Activity#RESULT_OK)`
*   `[RESULT_FIRST_USER](/reference/android/app/Activity#RESULT_FIRST_USER)`
*   `[setResult(int)](/reference/android/app/Activity#setResult\(int\))`

### setResult

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setResult (int resultCode)

Call this to set the result that your activity will return to its caller.

Parameters

`resultCode`

`int`: The result code to propagate back to the originating activity, often RESULT\_CANCELED or RESULT\_OK

**See also:**

*   `[RESULT_CANCELED](/reference/android/app/Activity#RESULT_CANCELED)`
*   `[RESULT_OK](/reference/android/app/Activity#RESULT_OK)`
*   `[RESULT_FIRST_USER](/reference/android/app/Activity#RESULT_FIRST_USER)`
*   `[setResult(int, Intent)](/reference/android/app/Activity#setResult\(int,%20android.content.Intent\))`

### setSecondaryProgress

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setSecondaryProgress (int secondaryProgress)

**This method was deprecated in API level 24.**  
No longer supported starting in API 21.

Sets the secondary progress for the progress bar in the title. This progress is drawn between the primary progress (set via `[setProgress(int)](/reference/android/app/Activity#setProgress\(int\))` and the background. It can be ideal for media scenarios such as showing the buffering progress while the default progress shows the play progress.

In order for the progress bar to be shown, the feature must be requested via `[requestWindowFeature(int)](/reference/android/app/Activity#requestWindowFeature\(int\))`.

Parameters

`secondaryProgress`

`int`: The secondary progress for the progress bar. Valid ranges are from 0 to 10000 (both inclusive).

### setShouldDockBigOverlays

Added in [API level 33](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setShouldDockBigOverlays (boolean shouldDockBigOverlays)

Specifies a preference to dock big overlays like the expanded picture-in-picture on TV (see `[PictureInPictureParams.Builder.setExpandedAspectRatio](/reference/android/app/PictureInPictureParams.Builder#setExpandedAspectRatio\(android.util.Rational\))`). Docking puts the big overlay side-by-side next to this activity, so that both windows are fully visible to the user.

If unspecified, whether the overlay window will be docked or not, will be defined by the system.

If specified, the system will try to respect the preference, but it may be overridden by a user preference.

Parameters

`shouldDockBigOverlays`

`boolean`: indicates that big overlays should be docked next to the activity instead of overlay its content

**See also:**

*   `[PictureInPictureParams.Builder.setExpandedAspectRatio(Rational)](/reference/android/app/PictureInPictureParams.Builder#setExpandedAspectRatio\(android.util.Rational\))`
*   `[shouldDockBigOverlays()](/reference/android/app/Activity#shouldDockBigOverlays\(\))`

### setShowWhenLocked

Added in [API level 27](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setShowWhenLocked (boolean showWhenLocked)

Specifies whether an `[Activity](/reference/android/app/Activity)` should be shown on top of the lock screen whenever the lockscreen is up and the activity is resumed. Normally an activity will be transitioned to the stopped state if it is started while the lockscreen is up, but with this flag set the activity will remain in the resumed state visible on-top of the lock screen. This value can be set as a manifest attribute using `[R.attr.showWhenLocked](/reference/android/R.attr#showWhenLocked)`.

Parameters

`showWhenLocked`

`boolean`: `true` to show the `[Activity](/reference/android/app/Activity)` on top of the lock screen; `false` otherwise.

**See also:**

*   `[setTurnScreenOn(boolean)](/reference/android/app/Activity#setTurnScreenOn\(boolean\))`
*   `[R.attr.turnScreenOn](/reference/android/R.attr#turnScreenOn)`
*   `[R.attr.showWhenLocked](/reference/android/R.attr#showWhenLocked)`

### setTaskDescription

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setTaskDescription ([ActivityManager.TaskDescription](/reference/android/app/ActivityManager.TaskDescription) taskDescription)

Sets information describing the task with this activity for presentation inside the Recents System UI. When `[ActivityManager.getRecentTasks](/reference/android/app/ActivityManager#getRecentTasks\(int,%20int\))` is called, the activities of each task are traversed in order from the topmost activity to the bottommost. The traversal continues for each property until a suitable value is found. For each task the taskDescription will be returned in `[ActivityManager.TaskDescription](/reference/android/app/ActivityManager.TaskDescription)`.

Parameters

`taskDescription`

`ActivityManager.TaskDescription`: The TaskDescription properties that describe the task with this activity

**See also:**

*   `[ActivityManager.getRecentTasks(int, int)](/reference/android/app/ActivityManager#getRecentTasks\(int,%20int\))`
*   `[ActivityManager.TaskDescription](/reference/android/app/ActivityManager.TaskDescription)`

### setTheme

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setTheme (int resid)

Set the base theme for this context. Note that this should be called before any views are instantiated in the Context (for example before calling `[Activity.setContentView(View)](/reference/android/app/Activity#setContentView\(android.view.View\))` or `[LayoutInflater.inflate(int, ViewGroup)](/reference/android/view/LayoutInflater#inflate\(int,%20android.view.ViewGroup\))`).

Parameters

`resid`

`int`: The style resource describing the theme.

### setTitle

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setTitle ([CharSequence](/reference/java/lang/CharSequence) title)

Change the title associated with this activity. If this is a top-level activity, the title for its window will change. If it is an embedded activity, the parent can do whatever it wants with it.

Parameters

`title`

`CharSequence`

### setTitle

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setTitle (int titleId)

Change the title associated with this activity. If this is a top-level activity, the title for its window will change. If it is an embedded activity, the parent can do whatever it wants with it.

Parameters

`titleId`

`int`

### setTitleColor

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setTitleColor (int textColor)

**This method was deprecated in API level 21.**  
Use action bar styles instead.

Change the color of the title associated with this activity.

This method is deprecated starting in API Level 11 and replaced by action bar styles. For information on styling the Action Bar, read the [Action Bar](/ guide/topics/ui/actionbar) developer guide.

Parameters

`textColor`

`int`

### setTranslucent

Added in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean setTranslucent (boolean translucent)

Convert an activity, which particularly with `[R.attr.windowIsTranslucent](/reference/android/R.attr#windowIsTranslucent)` or `[R.attr.windowIsFloating](/reference/android/R.attr#windowIsFloating)` attribute, to a fullscreen opaque activity, or convert it from opaque back to translucent.

Parameters

`translucent`

`boolean`: `true` convert from opaque to translucent. `false` convert from translucent to opaque.

Returns

`boolean`

The result of setting translucency. Return `true` if set successfully, `false` otherwise.

### setTurnScreenOn

Added in [API level 27](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setTurnScreenOn (boolean turnScreenOn)

Specifies whether the screen should be turned on when the `[Activity](/reference/android/app/Activity)` is resumed. Normally an activity will be transitioned to the stopped state if it is started while the screen if off, but with this flag set the activity will cause the screen to turn on if the activity will be visible and resumed due to the screen coming on. The screen will not be turned on if the activity won't be visible after the screen is turned on. This flag is normally used in conjunction with the `[R.attr.showWhenLocked](/reference/android/R.attr#showWhenLocked)` flag to make sure the activity is visible after the screen is turned on when the lockscreen is up. In addition, if this flag is set and the activity calls `[KeyguardManager.requestDismissKeyguard(android.app.Activity, android.app.KeyguardManager.KeyguardDismissCallback)](/reference/android/app/KeyguardManager#requestDismissKeyguard\(android.app.Activity,%20android.app.KeyguardManager.KeyguardDismissCallback\))` the screen will turn on.

Parameters

`turnScreenOn`

`boolean`: `true` to turn on the screen; `false` otherwise.

**See also:**

*   `[setShowWhenLocked(boolean)](/reference/android/app/Activity#setShowWhenLocked\(boolean\))`
*   `[R.attr.turnScreenOn](/reference/android/R.attr#turnScreenOn)`
*   `[R.attr.showWhenLocked](/reference/android/R.attr#showWhenLocked)`
*   `[KeyguardManager.isDeviceSecure()](/reference/android/app/KeyguardManager#isDeviceSecure\(\))`

### setVisible

Added in [API level 3](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setVisible (boolean visible)

Control whether this activity's main window is visible. This is intended only for the special case of an activity that is not going to show a UI itself, but can't just finish prior to onResume() because it needs to wait for a service binding or such. Setting this to false allows you to prevent your UI from being shown during that time.

The default value for this is taken from the `[R.attr.windowNoDisplay](/reference/android/R.attr#windowNoDisplay)` attribute of the activity's theme.

Parameters

`visible`

`boolean`

### setVolumeControlStream

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void setVolumeControlStream (int streamType)

Suggests an audio stream whose volume should be changed by the hardware volume controls.

The suggested audio stream will be tied to the window of this Activity. Volume requests which are received while the Activity is in the foreground will affect this stream.

It is not guaranteed that the hardware volume controls will always change this stream's volume (for example, if a call is in progress, its stream's volume may be changed instead). To reset back to the default, use `[AudioManager.USE_DEFAULT_STREAM_TYPE](/reference/android/media/AudioManager#USE_DEFAULT_STREAM_TYPE)`.

Parameters

`streamType`

`int`: The type of the audio stream whose volume should be changed by the hardware volume controls.

### setVrModeEnabled

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void setVrModeEnabled (boolean enabled, 
                [ComponentName](/reference/android/content/ComponentName) requestedComponent)

Enable or disable virtual reality (VR) mode for this Activity.

VR mode is a hint to Android system to switch to a mode optimized for VR applications while this Activity has user focus.

It is recommended that applications additionally declare `[R.attr.enableVrMode](/reference/android/R.attr#enableVrMode)` in their manifest to allow for smooth activity transitions when switching between VR activities.

If the requested `[VrListenerService](/reference/android/service/vr/VrListenerService)` component is not available, VR mode will not be started. Developers can handle this case as follows:

 String servicePackage = "com.whatever.app";
 String serviceClass = "com.whatever.app.MyVrListenerService";

 // Name of the component of the VrListenerService to start.
 ComponentName serviceComponent = new ComponentName(servicePackage, serviceClass);

 try {
    setVrModeEnabled(true, myComponentName);
 } catch (PackageManager.NameNotFoundException e) {
        List<ApplicationInfo> installed = getPackageManager().getInstalledApplications(0);
        boolean isInstalled = false;
        for (ApplicationInfo app : installed) {
            if (app.packageName.equals(servicePackage)) {
                isInstalled = true;
                break;
            }
        }
        if (isInstalled) {
            // Package is installed, but not enabled in Settings.  Let user enable it.
            startActivity(new Intent(Settings.ACTION\_VR\_LISTENER\_SETTINGS));
        } else {
            // Package is not installed.  Send an intent to download this.
            sentIntentToLaunchAppStore(servicePackage);
        }
 }
 

Parameters

`enabled`

`boolean`: `true` to enable this mode.

`requestedComponent`

`ComponentName`: the name of the component to use as a `[VrListenerService](/reference/android/service/vr/VrListenerService)` while VR mode is enabled. This value cannot be `null`.

Throws

`[PackageManager.NameNotFoundException](/reference/android/content/pm/PackageManager.NameNotFoundException)`

if the given component to run as a `[VrListenerService](/reference/android/service/vr/VrListenerService)` is not installed, or has not been enabled in user settings.

**See also:**

*   `[PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE](/reference/android/content/pm/PackageManager#FEATURE_VR_MODE_HIGH_PERFORMANCE)`
*   `[VrListenerService](/reference/android/service/vr/VrListenerService)`
*   `[Settings.ACTION_VR_LISTENER_SETTINGS](/reference/android/provider/Settings#ACTION_VR_LISTENER_SETTINGS)`
*   `[R.attr.enableVrMode](/reference/android/R.attr#enableVrMode)`

### shouldDockBigOverlays

Added in [API level 33](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean shouldDockBigOverlays ()

Returns whether big overlays should be docked next to the activity as set by `[setShouldDockBigOverlays(boolean)](/reference/android/app/Activity#setShouldDockBigOverlays\(boolean\))`.

Returns

`boolean`

`true` if big overlays should be docked next to the activity instead of overlay its content

**See also:**

*   `[setShouldDockBigOverlays(boolean)](/reference/android/app/Activity#setShouldDockBigOverlays\(boolean\))`

### shouldShowRequestPermissionRationale

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean shouldShowRequestPermissionRationale ([String](/reference/java/lang/String) permission)

Gets whether you should show UI with rationale before requesting a permission.

Parameters

`permission`

`String`: A permission your app wants to request. This value cannot be `null`.

Returns

`boolean`

Whether you should show permission rationale UI.

**See also:**

*   `[ContextWrapper.checkSelfPermission(String)](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))`
*   `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`
*   `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`

### shouldShowRequestPermissionRationale

Added in [API level 35](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean shouldShowRequestPermissionRationale ([String](/reference/java/lang/String) permission, 
                int deviceId)

Gets whether you should show UI with rationale before requesting a permission.

Parameters

`permission`

`String`: A permission your app wants to request. This value cannot be `null`.

`deviceId`

`int`: The app is requesting permissions for this device. The primary/physical device is assigned `[Context.DEVICE_ID_DEFAULT](/reference/android/content/Context#DEVICE_ID_DEFAULT)`, and virtual devices are assigned unique device Ids.

Returns

`boolean`

Whether you should show permission rationale UI.

**See also:**

*   `[ContextWrapper.checkSelfPermission(String)](/reference/android/content/ContextWrapper#checkSelfPermission\(java.lang.String\))`
*   `[requestPermissions(String, int)](/reference/android/app/Activity#requestPermissions\(java.lang.String[],%20int\))`
*   `[onRequestPermissionsResult(int, String, int)](/reference/android/app/Activity#onRequestPermissionsResult\(int,%20java.lang.String[],%20int[]\))`

### shouldUpRecreateTask

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean shouldUpRecreateTask ([Intent](/reference/android/content/Intent) targetIntent)

Returns true if the app should recreate the task when navigating 'up' from this activity by using targetIntent.

If this method returns false the app can trivially call `[navigateUpTo(android.content.Intent)](/reference/android/app/Activity#navigateUpTo\(android.content.Intent\))` using the same parameters to correctly perform up navigation. If this method returns false, the app should synthesize a new task stack by using `[TaskStackBuilder](/reference/android/app/TaskStackBuilder)` or another similar mechanism to perform up navigation.

Parameters

`targetIntent`

`Intent`: An intent representing the target destination for up navigation

Returns

`boolean`

true if navigating up should recreate a new task stack, false if the same task should be used for the destination

### showAssist

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean showAssist ([Bundle](/reference/android/os/Bundle) args)

Ask to have the current assistant shown to the user. This only works if the calling activity is the current foreground activity. It is the same as calling `[VoiceInteractionService.showSession](/reference/android/service/voice/VoiceInteractionService#showSession\(android.os.Bundle,%20int\))` and requesting all of the possible context. The receiver will always see `[VoiceInteractionSession.SHOW_SOURCE_APPLICATION](/reference/android/service/voice/VoiceInteractionSession#SHOW_SOURCE_APPLICATION)` set.

Parameters

`args`

`Bundle`

Returns

`boolean`

Returns true if the assistant was successfully invoked, else false. For example false will be returned if the caller is not the current top activity.

### showDialog

Added in [API level 8](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final boolean showDialog (int id, 
                [Bundle](/reference/android/os/Bundle) args)

**This method was deprecated in API level 15.**  
Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package.

Show a dialog managed by this activity. A call to `[onCreateDialog(int, android.os.Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))` will be made with the same id the first time this is called for a given id. From thereafter, the dialog will be automatically saved and restored. _If you are targeting `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` or later, consider instead using a `[DialogFragment](/reference/android/app/DialogFragment)` instead._

Each time a dialog is shown, `[onPrepareDialog(int, android.app.Dialog, android.os.Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))` will be made to provide an opportunity to do any timely preparation.

Parameters

`id`

`int`: The id of the managed dialog.

`args`

`Bundle`: Arguments to pass through to the dialog. These will be saved and restored for you. Note that if the dialog is already created, `[onCreateDialog(int, android.os.Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))` will not be called with the new arguments but `[onPrepareDialog(int, android.app.Dialog, android.os.Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))` will be. If you need to rebuild the dialog, call `[removeDialog(int)](/reference/android/app/Activity#removeDialog\(int\))` first.

Returns

`boolean`

Returns true if the Dialog was created; false is returned if it is not created because `[onCreateDialog(int, android.os.Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))` returns false.

**See also:**

*   `[Dialog](/reference/android/app/Dialog)`
*   `[onCreateDialog(int, Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))`
*   `[onPrepareDialog(int, Dialog, Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`
*   `[dismissDialog(int)](/reference/android/app/Activity#dismissDialog\(int\))`
*   `[removeDialog(int)](/reference/android/app/Activity#removeDialog\(int\))`

### showDialog

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public final void showDialog (int id)

**This method was deprecated in API level 15.**  
Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package.

Simple version of `[showDialog(int, android.os.Bundle)](/reference/android/app/Activity#showDialog\(int,%20android.os.Bundle\))` that does not take any arguments. Simply calls `[showDialog(int, android.os.Bundle)](/reference/android/app/Activity#showDialog\(int,%20android.os.Bundle\))` with null arguments.

Parameters

`id`

`int`

### showLockTaskEscapeMessage

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void showLockTaskEscapeMessage ()

Shows the user the system defined message for telling the user how to exit lock task mode. The task containing this activity must be in lock task mode at the time of this call for the message to be displayed.

### startActionMode

Added in [API level 23](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ActionMode](/reference/android/view/ActionMode) startActionMode ([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback, 
                int type)

Start an action mode of the given type.

Parameters

`callback`

`ActionMode.Callback`: Callback that will manage lifecycle events for this action mode

`type`

`int`: One of `[ActionMode.TYPE_PRIMARY](/reference/android/view/ActionMode#TYPE_PRIMARY)` or `[ActionMode.TYPE_FLOATING](/reference/android/view/ActionMode#TYPE_FLOATING)`.

Returns

`[ActionMode](/reference/android/view/ActionMode)`

The ActionMode that was started, or null if it was canceled

**See also:**

*   `[ActionMode](/reference/android/view/ActionMode)`

### startActionMode

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public [ActionMode](/reference/android/view/ActionMode) startActionMode ([ActionMode.Callback](/reference/android/view/ActionMode.Callback) callback)

Start an action mode of the default type `[ActionMode.TYPE_PRIMARY](/reference/android/view/ActionMode#TYPE_PRIMARY)`.

Parameters

`callback`

`ActionMode.Callback`: Callback that will manage lifecycle events for this action mode

Returns

`[ActionMode](/reference/android/view/ActionMode)`

The ActionMode that was started, or null if it was canceled

**See also:**

*   `[ActionMode](/reference/android/view/ActionMode)`

### startActivities

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivities ([Intent\[\]](/reference/android/content/Intent) intents, 
                [Bundle](/reference/android/os/Bundle) options)

Launch a new activity. You will not receive any information about when the activity exits. This implementation overrides the base version, providing information about the activity performing the launch. Because of this additional information, the `[Intent.FLAG_ACTIVITY_NEW_TASK](/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK)` launch flag is not required; if not specified, the new activity will be added to the task of the caller.

This method throws `[ActivityNotFoundException](/reference/android/content/ActivityNotFoundException)` if there was no Activity found to run the given Intent.

Parameters

`intents`

`Intent`: The intents to start.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivities(Intent[])](/reference/android/app/Activity#startActivities\(android.content.Intent[]\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivities

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivities ([Intent\[\]](/reference/android/content/Intent) intents)

Same as `[startActivities(android.content.Intent[], android.os.Bundle)](/reference/android/app/Activity#startActivities\(android.content.Intent[],%20android.os.Bundle\))` with no options specified.

Parameters

`intents`

`Intent`: The intents to start.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivities(Intent[], Bundle)](/reference/android/app/Activity#startActivities\(android.content.Intent[],%20android.os.Bundle\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivity

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivity ([Intent](/reference/android/content/Intent) intent)

Same as `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))` with no options specified.

Parameters

`intent`

`Intent`: The intent to start.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivity(Intent, Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivity

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivity ([Intent](/reference/android/content/Intent) intent, 
                [Bundle](/reference/android/os/Bundle) options)

Launch a new activity. You will not receive any information about when the activity exits. This implementation overrides the base version, providing information about the activity performing the launch. Because of this additional information, the `[Intent.FLAG_ACTIVITY_NEW_TASK](/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK)` launch flag is not required; if not specified, the new activity will be added to the task of the caller.

This method throws `[ActivityNotFoundException](/reference/android/content/ActivityNotFoundException)` if there was no Activity found to run the given Intent.

Parameters

`intent`

`Intent`: The intent to start.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivityForResult

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivityForResult ([Intent](/reference/android/content/Intent) intent, 
                int requestCode)

Same as calling `[startActivityForResult(android.content.Intent, int, android.os.Bundle)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int,%20android.os.Bundle\))` with no options.

Parameters

`intent`

`Intent`: The intent to start.

`requestCode`

`int`: If >= 0, this code will be returned in onActivityResult() when the activity exits; If < 0, no result will return when the activity exits.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`

### startActivityForResult

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivityForResult ([Intent](/reference/android/content/Intent) intent, 
                int requestCode, 
                [Bundle](/reference/android/os/Bundle) options)

Launch an activity for which you would like a result when it finished. When this activity exits, your onActivityResult() method will be called with the given requestCode. Using a negative requestCode is the same as calling `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))` (the activity is not launched as a sub-activity).

Note that this method should only be used with Intent protocols that are defined to return a result. In other protocols (such as `[Intent.ACTION_MAIN](/reference/android/content/Intent#ACTION_MAIN)` or `[Intent.ACTION_VIEW](/reference/android/content/Intent#ACTION_VIEW)`), you may not get the result when you expect. For example, if the activity you are launching uses `[Intent.FLAG_ACTIVITY_NEW_TASK](/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK)`, it will not run in your task and thus you will immediately receive a cancel result.

As a special case, if you call startActivityForResult() with a requestCode >= 0 during the initial onCreate(Bundle savedInstanceState)/onResume() of your activity, then your window will not be displayed until a result is returned back from the started activity. This is to avoid visible flickering when redirecting to another activity.

This method throws `[ActivityNotFoundException](/reference/android/content/ActivityNotFoundException)` if there was no Activity found to run the given Intent.

Parameters

`intent`

`Intent`: The intent to start.

`requestCode`

`int`: If >= 0, this code will be returned in onActivityResult() when the activity exits; If < 0, no result will return when the activity exits.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`

### startActivityFromChild

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivityFromChild ([Activity](/reference/android/app/Activity) child, 
                [Intent](/reference/android/content/Intent) intent, 
                int requestCode)

**This method was deprecated in API level 30.**  
Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int)`

Same as calling `[startActivityFromChild(android.app.Activity, android.content.Intent, int, android.os.Bundle)](/reference/android/app/Activity#startActivityFromChild\(android.app.Activity,%20android.content.Intent,%20int,%20android.os.Bundle\))` with no options.

Parameters

`child`

`Activity`: The activity making the call. This value cannot be `null`.

`intent`

`Intent`: The intent to start.

`requestCode`

`int`: Reply request code. < 0 if reply is not requested.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivityFromChild

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivityFromChild ([Activity](/reference/android/app/Activity) child, 
                [Intent](/reference/android/content/Intent) intent, 
                int requestCode, 
                [Bundle](/reference/android/os/Bundle) options)

**This method was deprecated in API level 30.**  
Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int,Bundle)`

This is called when a child activity of this one calls its `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))` or `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))` method.

This method throws `[ActivityNotFoundException](/reference/android/content/ActivityNotFoundException)` if there was no Activity found to run the given Intent.

Parameters

`child`

`Activity`: The activity making the call. This value cannot be `null`.

`intent`

`Intent`: The intent to start.

`requestCode`

`int`: Reply request code. < 0 if reply is not requested.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivityFromFragment

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivityFromFragment ([Fragment](/reference/android/app/Fragment) fragment, 
                [Intent](/reference/android/content/Intent) intent, 
                int requestCode, 
                [Bundle](/reference/android/os/Bundle) options)

**This method was deprecated in API level 28.**  
Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int,Bundle)`

This is called when a Fragment in this activity calls its `[Fragment.startActivity](/reference/android/app/Fragment#startActivity\(android.content.Intent\))` or `[Fragment.startActivityForResult](/reference/android/app/Fragment#startActivityForResult\(android.content.Intent,%20int\))` method.

This method throws `[ActivityNotFoundException](/reference/android/content/ActivityNotFoundException)` if there was no Activity found to run the given Intent.

Parameters

`fragment`

`Fragment`: The fragment making the call. This value cannot be `null`.

`intent`

`Intent`: The intent to start.

`requestCode`

`int`: Reply request code. < 0 if reply is not requested.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[Fragment.startActivity(Intent)](/reference/android/app/Fragment#startActivity\(android.content.Intent\))`
*   `[Fragment.startActivityForResult(Intent, int)](/reference/android/app/Fragment#startActivityForResult\(android.content.Intent,%20int\))`

### startActivityFromFragment

Added in [API level 11](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 28](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startActivityFromFragment ([Fragment](/reference/android/app/Fragment) fragment, 
                [Intent](/reference/android/content/Intent) intent, 
                int requestCode)

**This method was deprecated in API level 28.**  
Use `androidx.fragment.app.FragmentActivity#startActivityFromFragment( androidx.fragment.app.Fragment,Intent,int)`

Same as calling `[startActivityFromFragment(android.app.Fragment, android.content.Intent, int, android.os.Bundle)](/reference/android/app/Activity#startActivityFromFragment\(android.app.Fragment,%20android.content.Intent,%20int,%20android.os.Bundle\))` with no options.

Parameters

`fragment`

`Fragment`: The fragment making the call. This value cannot be `null`.

`intent`

`Intent`: The intent to start.

`requestCode`

`int`: Reply request code. < 0 if reply is not requested.

Throws

android.content.ActivityNotFoundException

**See also:**

*   `[Fragment.startActivity(Intent)](/reference/android/app/Fragment#startActivity\(android.content.Intent\))`
*   `[Fragment.startActivityForResult(Intent, int)](/reference/android/app/Fragment#startActivityForResult\(android.content.Intent,%20int\))`

### startActivityIfNeeded

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean startActivityIfNeeded ([Intent](/reference/android/content/Intent) intent, 
                int requestCode, 
                [Bundle](/reference/android/os/Bundle) options)

A special variation to launch an activity only if a new activity instance is needed to handle the given Intent. In other words, this is just like `[startActivityForResult(android.content.Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))` except: if you are using the `[Intent.FLAG_ACTIVITY_SINGLE_TOP](/reference/android/content/Intent#FLAG_ACTIVITY_SINGLE_TOP)` flag, or singleTask or singleTop `[launchMode](/reference/android/R.styleable#AndroidManifestActivity_launchMode)`, and the activity that handles intent is the same as your currently running activity, then a new instance is not needed. In this case, instead of the normal behavior of calling `[onNewIntent(Intent)](/reference/android/app/Activity#onNewIntent\(android.content.Intent\))` this function will return and you can handle the Intent yourself.

This function can only be called from a top-level activity; if it is called from a child activity, a runtime exception will be thrown.

Parameters

`intent`

`Intent`: The intent to start. This value cannot be `null`.

`requestCode`

`int`: If >= 0, this code will be returned in onActivityResult() when the activity exits; If < 0, no result will return when the activity exits, as described in `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Returns

`boolean`

If a new activity was launched then true is returned; otherwise false is returned and you must handle the Intent yourself.

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startActivityIfNeeded

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean startActivityIfNeeded ([Intent](/reference/android/content/Intent) intent, 
                int requestCode)

Same as calling `[startActivityIfNeeded(android.content.Intent, int, android.os.Bundle)](/reference/android/app/Activity#startActivityIfNeeded\(android.content.Intent,%20int,%20android.os.Bundle\))` with no options.

Parameters

`intent`

`Intent`: The intent to start. This value cannot be `null`.

`requestCode`

`int`: If >= 0, this code will be returned in onActivityResult() when the activity exits; If < 0, no result will return when the activity exits, as described in `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`.

Returns

`boolean`

If a new activity was launched then true is returned; otherwise false is returned and you must handle the Intent yourself.

**See also:**

*   `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`
*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`

### startIntentSender

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startIntentSender ([IntentSender](/reference/android/content/IntentSender) intent, 
                [Intent](/reference/android/content/Intent) fillInIntent, 
                int flagsMask, 
                int flagsValues, 
                int extraFlags)

Same as calling `[startIntentSender(android.content.IntentSender, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSender\(android.content.IntentSender,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options.

Parameters

`intent`

`IntentSender`: The IntentSender to launch.

`fillInIntent`

`Intent`: If non-null, this will be provided as the intent parameter to `[IntentSender.sendIntent](/reference/android/content/IntentSender#sendIntent\(android.content.Context,%20int,%20android.content.Intent,%20android.content.IntentSender.OnFinished,%20android.os.Handler\))`.

`flagsMask`

`int`: Intent flags in the original IntentSender that you would like to change.

`flagsValues`

`int`: Desired values for any bits set in flagsMask

`extraFlags`

`int`: Always set to 0.

Throws

`[IntentSender.SendIntentException](/reference/android/content/IntentSender.SendIntentException)`

### startIntentSender

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startIntentSender ([IntentSender](/reference/android/content/IntentSender) intent, 
                [Intent](/reference/android/content/Intent) fillInIntent, 
                int flagsMask, 
                int flagsValues, 
                int extraFlags, 
                [Bundle](/reference/android/os/Bundle) options)

Like `[startActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startActivity\(android.content.Intent,%20android.os.Bundle\))`, but taking a IntentSender to start; see `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` for more information.

Parameters

`intent`

`IntentSender`: The IntentSender to launch.

`fillInIntent`

`Intent`: If non-null, this will be provided as the intent parameter to `[IntentSender.sendIntent](/reference/android/content/IntentSender#sendIntent\(android.content.Context,%20int,%20android.content.Intent,%20android.content.IntentSender.OnFinished,%20android.os.Handler\))`.

`flagsMask`

`int`: Intent flags in the original IntentSender that you would like to change.

`flagsValues`

`int`: Desired values for any bits set in flagsMask

`extraFlags`

`int`: Always set to 0.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. If options have also been supplied by the IntentSender, options given here will override any that conflict with those given by the IntentSender. This value may be `null`.

Throws

`[IntentSender.SendIntentException](/reference/android/content/IntentSender.SendIntentException)`

### startIntentSenderForResult

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startIntentSenderForResult ([IntentSender](/reference/android/content/IntentSender) intent, 
                int requestCode, 
                [Intent](/reference/android/content/Intent) fillInIntent, 
                int flagsMask, 
                int flagsValues, 
                int extraFlags)

Same as calling `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options.

Parameters

`intent`

`IntentSender`: The IntentSender to launch.

`requestCode`

`int`: If >= 0, this code will be returned in onActivityResult() when the activity exits; If < 0, no result will return when the activity exits.

`fillInIntent`

`Intent`: If non-null, this will be provided as the intent parameter to `[IntentSender.sendIntent](/reference/android/content/IntentSender#sendIntent\(android.content.Context,%20int,%20android.content.Intent,%20android.content.IntentSender.OnFinished,%20android.os.Handler\))`.

`flagsMask`

`int`: Intent flags in the original IntentSender that you would like to change.

`flagsValues`

`int`: Desired values for any bits set in flagsMask

`extraFlags`

`int`: Always set to 0.

Throws

`[IntentSender.SendIntentException](/reference/android/content/IntentSender.SendIntentException)`

### startIntentSenderForResult

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startIntentSenderForResult ([IntentSender](/reference/android/content/IntentSender) intent, 
                int requestCode, 
                [Intent](/reference/android/content/Intent) fillInIntent, 
                int flagsMask, 
                int flagsValues, 
                int extraFlags, 
                [Bundle](/reference/android/os/Bundle) options)

Like `[startActivityForResult(android.content.Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`, but allowing you to use a IntentSender to describe the activity to be started. If the IntentSender is for an activity, that activity will be started as if you had called the regular `[startActivityForResult(android.content.Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))` here; otherwise, its associated action will be executed (such as sending a broadcast) as if you had called `[IntentSender.sendIntent](/reference/android/content/IntentSender#sendIntent\(android.content.Context,%20int,%20android.content.Intent,%20android.content.IntentSender.OnFinished,%20android.os.Handler\))` on it.

Parameters

`intent`

`IntentSender`: The IntentSender to launch.

`requestCode`

`int`: If >= 0, this code will be returned in onActivityResult() when the activity exits; If < 0, no result will return when the activity exits.

`fillInIntent`

`Intent`: If non-null, this will be provided as the intent parameter to `[IntentSender.sendIntent](/reference/android/content/IntentSender#sendIntent\(android.content.Context,%20int,%20android.content.Intent,%20android.content.IntentSender.OnFinished,%20android.os.Handler\))`.

`flagsMask`

`int`: Intent flags in the original IntentSender that you would like to change.

`flagsValues`

`int`: Desired values for any bits set in flagsMask

`extraFlags`

`int`: Always set to 0.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. If options have also been supplied by the IntentSender, options given here will override any that conflict with those given by the IntentSender. This value may be `null`.

Throws

`[IntentSender.SendIntentException](/reference/android/content/IntentSender.SendIntentException)`

### startIntentSenderFromChild

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startIntentSenderFromChild ([Activity](/reference/android/app/Activity) child, 
                [IntentSender](/reference/android/content/IntentSender) intent, 
                int requestCode, 
                [Intent](/reference/android/content/Intent) fillInIntent, 
                int flagsMask, 
                int flagsValues, 
                int extraFlags, 
                [Bundle](/reference/android/os/Bundle) options)

**This method was deprecated in API level 30.**  
Use `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` instead.

Like `[startActivityFromChild(android.app.Activity, android.content.Intent, int)](/reference/android/app/Activity#startActivityFromChild\(android.app.Activity,%20android.content.Intent,%20int\))`, but taking a IntentSender; see `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int\))` for more information.

Parameters

`child`

`Activity`

`intent`

`IntentSender`

`requestCode`

`int`

`fillInIntent`

`Intent`

`flagsMask`

`int`

`flagsValues`

`int`

`extraFlags`

`int`

`options`

`Bundle`: This value may be `null`.

Throws

`[IntentSender.SendIntentException](/reference/android/content/IntentSender.SendIntentException)`

### startIntentSenderFromChild

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 30](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startIntentSenderFromChild ([Activity](/reference/android/app/Activity) child, 
                [IntentSender](/reference/android/content/IntentSender) intent, 
                int requestCode, 
                [Intent](/reference/android/content/Intent) fillInIntent, 
                int flagsMask, 
                int flagsValues, 
                int extraFlags)

**This method was deprecated in API level 30.**  
Use `[startIntentSenderForResult(android.content.IntentSender, int, android.content.Intent, int, int, int)](/reference/android/app/Activity#startIntentSenderForResult\(android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int\))` instead.

Same as calling `[startIntentSenderFromChild(android.app.Activity, android.content.IntentSender, int, android.content.Intent, int, int, int, android.os.Bundle)](/reference/android/app/Activity#startIntentSenderFromChild\(android.app.Activity,%20android.content.IntentSender,%20int,%20android.content.Intent,%20int,%20int,%20int,%20android.os.Bundle\))` with no options.

Parameters

`child`

`Activity`

`intent`

`IntentSender`

`requestCode`

`int`

`fillInIntent`

`Intent`

`flagsMask`

`int`

`flagsValues`

`int`

`extraFlags`

`int`

Throws

`[IntentSender.SendIntentException](/reference/android/content/IntentSender.SendIntentException)`

### startLocalVoiceInteraction

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startLocalVoiceInteraction ([Bundle](/reference/android/os/Bundle) privateOptions)

Starts a local voice interaction session. When ready, `[onLocalVoiceInteractionStarted()](/reference/android/app/Activity#onLocalVoiceInteractionStarted\(\))` is called. You can pass a bundle of private options to the registered voice interaction service.

Parameters

`privateOptions`

`Bundle`: a Bundle of private arguments to the current voice interaction service

### startLockTask

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startLockTask ()

Request to put this activity in a mode where the user is locked to a restricted set of applications.

If `[DevicePolicyManager.isLockTaskPermitted(String)](/reference/android/app/admin/DevicePolicyManager#isLockTaskPermitted\(java.lang.String\))` returns `true` for this component, the current task will be launched directly into LockTask mode. Only apps allowlisted by `[DevicePolicyManager.setLockTaskPackages(ComponentName, String[])](/reference/android/app/admin/DevicePolicyManager#setLockTaskPackages\(android.content.ComponentName,%20java.lang.String[]\))` can be launched while LockTask mode is active. The user will not be able to leave this mode until this activity calls `[stopLockTask()](/reference/android/app/Activity#stopLockTask\(\))`. Calling this method while the device is already in LockTask mode has no effect.

Otherwise, the current task will be launched into screen pinning mode. In this case, the system will prompt the user with a dialog requesting permission to use this mode. The user can exit at any time through instructions shown on the request dialog. Calling `[stopLockTask()](/reference/android/app/Activity#stopLockTask\(\))` will also terminate this mode.

**Note:** this method can only be called when the activity is foreground. That is, between `[onResume()](/reference/android/app/Activity#onResume\(\))` and `[onPause()](/reference/android/app/Activity#onPause\(\))`.

**See also:**

*   `[stopLockTask()](/reference/android/app/Activity#stopLockTask\(\))`
*   `[R.attr.lockTaskMode](/reference/android/R.attr#lockTaskMode)`

### startManagingCursor

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startManagingCursor ([Cursor](/reference/android/database/Cursor) c)

**This method was deprecated in API level 15.**  
Use the new `[CursorLoader](/reference/android/content/CursorLoader)` class with `[LoaderManager](/reference/android/app/LoaderManager)` instead; this is also available on older platforms through the Android compatibility package.

This method allows the activity to take care of managing the given `[Cursor](/reference/android/database/Cursor)`'s lifecycle for you based on the activity's lifecycle. That is, when the activity is stopped it will automatically call `[Cursor.deactivate](/reference/android/database/Cursor#deactivate\(\))` on the given Cursor, and when it is later restarted it will call `[Cursor.requery](/reference/android/database/Cursor#requery\(\))` for you. When the activity is destroyed, all managed Cursors will be closed automatically. _If you are targeting `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` or later, consider instead using `[LoaderManager](/reference/android/app/LoaderManager)` instead, available via `[getLoaderManager()](/reference/android/app/Activity#getLoaderManager\(\))`._

**Warning:** Do not call `[Cursor.close()](/reference/android/database/Cursor#close\(\))` on cursor obtained from `[managedQuery(Uri, String, String, String, String)](/reference/android/app/Activity#managedQuery\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))`, because the activity will do that for you at the appropriate time. However, if you call `[stopManagingCursor(Cursor)](/reference/android/app/Activity#stopManagingCursor\(android.database.Cursor\))` on a cursor from a managed query, the system _will not_ automatically close the cursor and, in that case, you must call `[Cursor.close()](/reference/android/database/Cursor#close\(\))`.

Parameters

`c`

`Cursor`: The Cursor to be managed.

**See also:**

*   `[managedQuery(android.net.Uri, String[], String, String[], String)](/reference/android/app/Activity#managedQuery\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))`
*   `[stopManagingCursor(Cursor)](/reference/android/app/Activity#stopManagingCursor\(android.database.Cursor\))`

### startNextMatchingActivity

Added in [API level 16](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean startNextMatchingActivity ([Intent](/reference/android/content/Intent) intent, 
                [Bundle](/reference/android/os/Bundle) options)

Special version of starting an activity, for use when you are replacing other activity components. You can use this to hand the Intent off to the next Activity that can handle it. You typically call this in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` with the Intent returned by `[getIntent()](/reference/android/app/Activity#getIntent\(\))`.

Parameters

`intent`

`Intent`: The intent to dispatch to the next activity. For correct behavior, this must be the same as the Intent that started your own activity; the only changes you can make are to the extras inside of it. This value cannot be `null`.

`options`

`Bundle`: Additional options for how the Activity should be started. See `[Context.startActivity(Intent, Bundle)](/reference/android/content/Context#startActivity\(android.content.Intent,%20android.os.Bundle\))` Context.startActivity(Intent, Bundle)} for more details. This value may be `null`.

Returns

`boolean`

Returns a boolean indicating whether there was another Activity to start: true if there was a next activity to start, false if there wasn't. In general, if true is returned you will then want to call finish() on yourself.

### startNextMatchingActivity

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public boolean startNextMatchingActivity ([Intent](/reference/android/content/Intent) intent)

Same as calling `[startNextMatchingActivity(android.content.Intent, android.os.Bundle)](/reference/android/app/Activity#startNextMatchingActivity\(android.content.Intent,%20android.os.Bundle\))` with no options.

Parameters

`intent`

`Intent`: The intent to dispatch to the next activity. For correct behavior, this must be the same as the Intent that started your own activity; the only changes you can make are to the extras inside of it. This value cannot be `null`.

Returns

`boolean`

Returns a boolean indicating whether there was another Activity to start: true if there was a next activity to start, false if there wasn't. In general, if true is returned you will then want to call finish() on yourself.

### startPostponedEnterTransition

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startPostponedEnterTransition ()

Begin postponed transitions after `[postponeEnterTransition()](/reference/android/app/Activity#postponeEnterTransition\(\))` was called. If postponeEnterTransition() was called, you must call startPostponedEnterTransition() to have your Activity start drawing.

### startSearch

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void startSearch ([String](/reference/java/lang/String) initialQuery, 
                boolean selectInitialQuery, 
                [Bundle](/reference/android/os/Bundle) appSearchData, 
                boolean globalSearch)

This hook is called to launch the search UI.

It is typically called from onSearchRequested(), either directly from Activity.onSearchRequested() or from an overridden version in any given Activity. If your goal is simply to activate search, it is preferred to call onSearchRequested(), which may have been overridden elsewhere in your Activity. If your goal is to inject specific data such as context data, it is preferred to _override_ onSearchRequested(), so that any callers to it will benefit from the override.

Note: when running in a `[Configuration.UI_MODE_TYPE_WATCH](/reference/android/content/res/Configuration#UI_MODE_TYPE_WATCH)`, use of this API is not supported.

Parameters

`initialQuery`

`String`: Any non-null non-empty string will be inserted as pre-entered text in the search query box.

`selectInitialQuery`

`boolean`: If true, the initial query will be preselected, which means that any further typing will replace it. This is useful for cases where an entire pre-formed query is being inserted. If false, the selection point will be placed at the end of the inserted query. This is useful when the inserted query is text that the user entered, and the user would expect to be able to keep typing. _This parameter is only meaningful if initialQuery is a non-empty string._

`appSearchData`

`Bundle`: An application can insert application-specific context here, in order to improve quality or specificity of its own searches. This data will be returned with SEARCH intent(s). Null if no extra data is required. This value may be `null`.

`globalSearch`

`boolean`: If false, this will only launch the search that has been specifically defined by the application (which is usually defined as a local search). If no default search is defined in the current application or activity, global search will be launched. If true, this will always launch a platform-global (e.g. web-based) search instead.

**See also:**

*   `[SearchManager](/reference/android/app/SearchManager)`
*   `[onSearchRequested()](/reference/android/app/Activity#onSearchRequested\(\))`

### stopLocalVoiceInteraction

Added in [API level 24](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void stopLocalVoiceInteraction ()

Request to terminate the current voice interaction that was previously started using `[startLocalVoiceInteraction(android.os.Bundle)](/reference/android/app/Activity#startLocalVoiceInteraction\(android.os.Bundle\))`. When the interaction is terminated, `[onLocalVoiceInteractionStopped()](/reference/android/app/Activity#onLocalVoiceInteractionStopped\(\))` will be called.

### stopLockTask

Added in [API level 21](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void stopLockTask ()

Stop the current task from being locked.

Called to end the LockTask or screen pinning mode started by `[startLockTask()](/reference/android/app/Activity#startLockTask\(\))`. This can only be called by activities that have called `[startLockTask()](/reference/android/app/Activity#startLockTask\(\))` previously.

**Note:** If the device is in LockTask mode that is not initially started by this activity, then calling this method will not terminate the LockTask mode, but only finish its own task. The device will remain in LockTask mode, until the activity which started the LockTask mode calls this method, or until its allowlist authorization is revoked by `[DevicePolicyManager.setLockTaskPackages(ComponentName, String[])](/reference/android/app/admin/DevicePolicyManager#setLockTaskPackages\(android.content.ComponentName,%20java.lang.String[]\))`.

**See also:**

*   `[startLockTask()](/reference/android/app/Activity#startLockTask\(\))`
*   `[R.attr.lockTaskMode](/reference/android/R.attr#lockTaskMode)`
*   `[ActivityManager.getLockTaskModeState()](/reference/android/app/ActivityManager#getLockTaskModeState\(\))`

### stopManagingCursor

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void stopManagingCursor ([Cursor](/reference/android/database/Cursor) c)

**This method was deprecated in API level 15.**  
Use the new `[CursorLoader](/reference/android/content/CursorLoader)` class with `[LoaderManager](/reference/android/app/LoaderManager)` instead; this is also available on older platforms through the Android compatibility package.

Given a Cursor that was previously given to `[startManagingCursor(Cursor)](/reference/android/app/Activity#startManagingCursor\(android.database.Cursor\))`, stop the activity's management of that cursor.

**Warning:** After calling this method on a cursor from a managed query, the system _will not_ automatically close the cursor and you must call `[Cursor.close()](/reference/android/database/Cursor#close\(\))`.

Parameters

`c`

`Cursor`: The Cursor that was being managed.

**See also:**

*   `[startManagingCursor(Cursor)](/reference/android/app/Activity#startManagingCursor\(android.database.Cursor\))`

### takeKeyEvents

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void takeKeyEvents (boolean get)

Request that key events come to this activity. Use this if your activity has no views with focus, but the activity still wants a chance to process key events.

Parameters

`get`

`boolean`

**See also:**

*   `[Window.takeKeyEvents(boolean)](/reference/android/view/Window#takeKeyEvents\(boolean\))`

### triggerSearch

Added in [API level 5](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void triggerSearch ([String](/reference/java/lang/String) query, 
                [Bundle](/reference/android/os/Bundle) appSearchData)

Similar to `[startSearch(String, boolean, Bundle, boolean)](/reference/android/app/Activity#startSearch\(java.lang.String,%20boolean,%20android.os.Bundle,%20boolean\))`, but actually fires off the search query after invoking the search dialog. Made available for testing purposes.

Parameters

`query`

`String`: The query to trigger. If empty, the request will be ignored.

`appSearchData`

`Bundle`: An application can insert application-specific context here, in order to improve quality or specificity of its own searches. This data will be returned with SEARCH intent(s). Null if no extra data is required. This value may be `null`.

### unregisterActivityLifecycleCallbacks

Added in [API level 29](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void unregisterActivityLifecycleCallbacks ([Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks) callback)

Unregister an `[Application.ActivityLifecycleCallbacks](/reference/android/app/Application.ActivityLifecycleCallbacks)` previously registered with `[registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)](/reference/android/app/Activity#registerActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))`. It will not receive any further callbacks.

Parameters

`callback`

`Application.ActivityLifecycleCallbacks`: The callback instance to unregister This value cannot be `null`.

**See also:**

*   `[registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks)](/reference/android/app/Activity#registerActivityLifecycleCallbacks\(android.app.Application.ActivityLifecycleCallbacks\))`

### unregisterComponentCallbacks

Added in [API level 14](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void unregisterComponentCallbacks ([ComponentCallbacks](/reference/android/content/ComponentCallbacks) callback)

Remove a `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` object that was previously registered with `[registerComponentCallbacks(android.content.ComponentCallbacks)](/reference/android/content/ContextWrapper#registerComponentCallbacks\(android.content.ComponentCallbacks\))`.

After `[Build.VERSION_CODES.TIRAMISU](/reference/android/os/Build.VERSION_CODES#TIRAMISU)`, the `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` will be unregistered to `[the base Context](/reference/android/content/ContextWrapper#getBaseContext\(\))`, and can be only used after `[attachBaseContext(android.content.Context)](/reference/android/content/ContextWrapper#attachBaseContext\(android.content.Context\))`

Parameters

`callback`

`ComponentCallbacks`: The interface to call. This can be either a `[ComponentCallbacks](/reference/android/content/ComponentCallbacks)` or `[ComponentCallbacks2](/reference/android/content/ComponentCallbacks2)` interface.

### unregisterForContextMenu

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void unregisterForContextMenu ([View](/reference/android/view/View) view)

Prevents a context menu to be shown for the given view. This method will remove the `[OnCreateContextMenuListener](/reference/android/view/View.OnCreateContextMenuListener)` on the view.

Parameters

`view`

`View`: The view that should stop showing a context menu.

**See also:**

*   `[registerForContextMenu(View)](/reference/android/app/Activity#registerForContextMenu\(android.view.View\))`

### unregisterScreenCaptureCallback

Added in [API level 34](/guide/topics/manifest/uses-sdk-element#ApiLevels)

public void unregisterScreenCaptureCallback ([Activity.ScreenCaptureCallback](/reference/android/app/Activity.ScreenCaptureCallback) callback)

Unregisters a screen capture callback for this surface.  
Requires `[Manifest.permission.DETECT_SCREEN_CAPTURE](/reference/android/Manifest.permission#DETECT_SCREEN_CAPTURE)`

Parameters

`callback`

`Activity.ScreenCaptureCallback`: This value cannot be `null`.

## Protected methods

### attachBaseContext

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void attachBaseContext ([Context](/reference/android/content/Context) newBase)

Set the base context for this ContextWrapper. All calls will then be delegated to the base context. Throws IllegalStateException if a base context has already been set.

Parameters

`newBase`

`Context`: The new base context for this wrapper.

### onActivityResult

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onActivityResult (int requestCode, 
                int resultCode, 
                [Intent](/reference/android/content/Intent) data)

Called when an activity you launched exits, giving you the requestCode you started it with, the resultCode it returned, and any additional data from it. The resultCode will be `[RESULT_CANCELED](/reference/android/app/Activity#RESULT_CANCELED)` if the activity explicitly returned that, didn't return any result, or crashed during its operation.

An activity can never receive a result in the resumed state. You can count on `[onResume()](/reference/android/app/Activity#onResume\(\))` being called after this method, though not necessarily immediately after. If the activity was resumed, it will be paused and the result will be delivered, followed by `[onResume()](/reference/android/app/Activity#onResume\(\))`. If the activity wasn't in the resumed state, then the result will be delivered, with `[onResume()](/reference/android/app/Activity#onResume\(\))` called sometime later when the activity becomes active again.

This method is never invoked if your activity sets `[noHistory](/reference/android/R.styleable#AndroidManifestActivity_noHistory)` to `true`.

Parameters

`requestCode`

`int`: The integer request code originally supplied to startActivityForResult(), allowing you to identify who this result came from.

`resultCode`

`int`: The integer result code returned by the child activity through its setResult().

`data`

`Intent`: An Intent, which can return result data to the caller (various data can be attached to Intent "extras").

**See also:**

*   `[startActivityForResult(Intent, int)](/reference/android/app/Activity#startActivityForResult\(android.content.Intent,%20int\))`
*   `[createPendingResult(int, Intent, int)](/reference/android/app/Activity#createPendingResult\(int,%20android.content.Intent,%20int\))`
*   `[setResult(int)](/reference/android/app/Activity#setResult\(int\))`

### onApplyThemeResource

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onApplyThemeResource ([Resources.Theme](/reference/android/content/res/Resources.Theme) theme, 
                int resid, 
                boolean first)

Called by `[setTheme(Theme)](/reference/android/view/ContextThemeWrapper#setTheme\(android.content.res.Resources.Theme\))` and `[getTheme()](/reference/android/view/ContextThemeWrapper#getTheme\(\))` to apply a theme resource to the current Theme object. May be overridden to change the default (simple) behavior. This method will not be called in multiple threads simultaneously.

Parameters

`theme`

`Resources.Theme`: the theme being modified

`resid`

`int`: the style resource being applied to theme

`first`

`boolean`: `true` if this is the first time a style is being applied to theme

### onChildTitleChanged

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onChildTitleChanged ([Activity](/reference/android/app/Activity) childActivity, 
                [CharSequence](/reference/java/lang/CharSequence) title)

Parameters

`childActivity`

`Activity`

`title`

`CharSequence`

### onCreate

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onCreate ([Bundle](/reference/android/os/Bundle) savedInstanceState)

Called when the activity is starting. This is where most initialization should go: calling `[setContentView(int)](/reference/android/app/Activity#setContentView\(int\))` to inflate the activity's UI, using `[findViewById(int)](/reference/android/app/Activity#findViewById\(int\))` to programmatically interact with widgets in the UI, calling `[managedQuery(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)](/reference/android/app/Activity#managedQuery\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))` to retrieve cursors for data being displayed, etc.

You can call `[finish()](/reference/android/app/Activity#finish\(\))` from within this function, in which case onDestroy() will be immediately called after `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` without any of the rest of the activity lifecycle (`[onStart()](/reference/android/app/Activity#onStart\(\))`, `[onResume()](/reference/android/app/Activity#onResume\(\))`, `[onPause()](/reference/android/app/Activity#onPause\(\))`, etc.) executing.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
This method must be called from the main thread of your app.  
If you override this method you _must_ call through to the superclass implementation.

Parameters

`savedInstanceState`

`Bundle`: If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`. **_Note: Otherwise it is null._**

**See also:**

*   `[onStart()](/reference/android/app/Activity#onStart\(\))`
*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`
*   `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`
*   `[onPostCreate(Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))`

### onCreateDialog

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected [Dialog](/reference/android/app/Dialog) onCreateDialog (int id)

**This method was deprecated in API level 15.**  
Old no-arguments version of `[onCreateDialog(int, android.os.Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))`.

Parameters

`id`

`int`

Returns

`[Dialog](/reference/android/app/Dialog)`

### onCreateDialog

Added in [API level 8](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected [Dialog](/reference/android/app/Dialog) onCreateDialog (int id, 
                [Bundle](/reference/android/os/Bundle) args)

**This method was deprecated in API level 15.**  
Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package.

Callback for creating dialogs that are managed (saved and restored) for you by the activity. The default implementation calls through to `[onCreateDialog(int)](/reference/android/app/Activity#onCreateDialog\(int\))` for compatibility. _If you are targeting `[Build.VERSION_CODES.HONEYCOMB](/reference/android/os/Build.VERSION_CODES#HONEYCOMB)` or later, consider instead using a `[DialogFragment](/reference/android/app/DialogFragment)` instead._

If you use `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`, the activity will call through to this method the first time, and hang onto it thereafter. Any dialog that is created by this method will automatically be saved and restored for you, including whether it is showing.

If you would like the activity to manage saving and restoring dialogs for you, you should override this method and handle any ids that are passed to `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`.

If you would like an opportunity to prepare your dialog before it is shown, override `[onPrepareDialog(int, android.app.Dialog, android.os.Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`.

Parameters

`id`

`int`: The id of the dialog.

`args`

`Bundle`: The dialog arguments provided to `[showDialog(int, android.os.Bundle)](/reference/android/app/Activity#showDialog\(int,%20android.os.Bundle\))`.

Returns

`[Dialog](/reference/android/app/Dialog)`

The dialog. If you return null, the dialog will not be created.

**See also:**

*   `[onPrepareDialog(int, Dialog, Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`
*   `[showDialog(int, Bundle)](/reference/android/app/Activity#showDialog\(int,%20android.os.Bundle\))`
*   `[dismissDialog(int)](/reference/android/app/Activity#dismissDialog\(int\))`
*   `[removeDialog(int)](/reference/android/app/Activity#removeDialog\(int\))`

### onDestroy

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onDestroy ()

Perform any final cleanup before an activity is destroyed. This can happen either because the activity is finishing (someone called `[finish()](/reference/android/app/Activity#finish\(\))` on it), or because the system is temporarily destroying this instance of the activity to save space. You can distinguish between these two scenarios with the `[isFinishing()](/reference/android/app/Activity#isFinishing\(\))` method.

_Note: do not count on this method being called as a place for saving data! For example, if an activity is editing data in a content provider, those edits should be committed in either `[onPause()](/reference/android/app/Activity#onPause\(\))` or `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`, not here._ This method is usually implemented to free resources like threads that are associated with an activity, so that a destroyed activity does not leave such things around while the rest of its application is still running. There are situations where the system will simply kill the activity's hosting process without calling this method (or any others) in it, so it should not be used to do things that are intended to remain around after the process goes away.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onPause()](/reference/android/app/Activity#onPause\(\))`
*   `[onStop()](/reference/android/app/Activity#onStop\(\))`
*   `[finish()](/reference/android/app/Activity#finish\(\))`
*   `[isFinishing()](/reference/android/app/Activity#isFinishing\(\))`

### onNewIntent

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onNewIntent ([Intent](/reference/android/content/Intent) intent)

This is called for activities that set launchMode to "singleTop" in their package, or if a client used the `[Intent.FLAG_ACTIVITY_SINGLE_TOP](/reference/android/content/Intent#FLAG_ACTIVITY_SINGLE_TOP)` flag when calling `[startActivity(Intent)](/reference/android/app/Activity#startActivity\(android.content.Intent\))`. In either case, when the activity is re-launched while at the top of the activity stack instead of a new instance of the activity being started, onNewIntent() will be called on the existing instance with the Intent that was used to re-launch it.

An activity can never receive a new intent in the resumed state. You can count on `[onResume()](/reference/android/app/Activity#onResume\(\))` being called after this method, though not necessarily immediately after the completion of this callback. If the activity was resumed, it will be paused and new intent will be delivered, followed by `[onResume()](/reference/android/app/Activity#onResume\(\))`. If the activity wasn't in the resumed state, then new intent can be delivered immediately, with `[onResume()](/reference/android/app/Activity#onResume\(\))` called sometime later when activity becomes active again.

Note that `[getIntent()](/reference/android/app/Activity#getIntent\(\))` still returns the original Intent. You can use `[setIntent(android.content.Intent)](/reference/android/app/Activity#setIntent\(android.content.Intent\))` to update it to this new Intent.

Parameters

`intent`

`Intent`: The new intent that was used to start the activity

**See also:**

*   `[getIntent()](/reference/android/app/Activity#getIntent\(\))`
*   `[setIntent(Intent)](/reference/android/app/Activity#setIntent\(android.content.Intent\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`

### onPause

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onPause ()

Called as part of the activity lifecycle when the user no longer actively interacts with the activity, but it is still visible on screen. The counterpart to `[onResume()](/reference/android/app/Activity#onResume\(\))`.

When activity B is launched in front of activity A, this callback will be invoked on A. B will not be created until A's `[onPause()](/reference/android/app/Activity#onPause\(\))` returns, so be sure to not do anything lengthy here.

This callback is mostly used for saving any persistent state the activity is editing, to present a "edit in place" model to the user and making sure nothing is lost if there are not enough resources to start the new activity without first killing this one. This is also a good place to stop things that consume a noticeable amount of CPU in order to make the switch to the next activity as fast as possible.

On platform versions prior to `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)` this is also a good place to try to close exclusive-access devices or to release access to singleton resources. Starting with `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)` there can be multiple resumed activities in the system at the same time, so `[onTopResumedActivityChanged(boolean)](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))` should be used for that purpose instead.

If an activity is launched on top, after receiving this call you will usually receive a following call to `[onStop()](/reference/android/app/Activity#onStop\(\))` (after the next activity has been resumed and displayed above). However in some cases there will be a direct call back to `[onResume()](/reference/android/app/Activity#onResume\(\))` without going through the stopped state. An activity can also rest in paused state in some cases when in multi-window mode, still visible to user.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onResume()](/reference/android/app/Activity#onResume\(\))`
*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`
*   `[onStop()](/reference/android/app/Activity#onStop\(\))`

### onPostCreate

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onPostCreate ([Bundle](/reference/android/os/Bundle) savedInstanceState)

Called when activity start-up is complete (after `[onStart()](/reference/android/app/Activity#onStart\(\))` and `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` have been called). Applications will generally not implement this method; it is intended for system classes to do final initialization after application code has run.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

Parameters

`savedInstanceState`

`Bundle`: If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`. **_Note: Otherwise it is null._**

**See also:**

*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`

### onPostResume

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onPostResume ()

Called when activity resume is complete (after `[onResume()](/reference/android/app/Activity#onResume\(\))` has been called). Applications will generally not implement this method; it is intended for system classes to do final setup after application resume code has run.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onResume()](/reference/android/app/Activity#onResume\(\))`

### onPrepareDialog

Added in [API level 8](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onPrepareDialog (int id, 
                [Dialog](/reference/android/app/Dialog) dialog, 
                [Bundle](/reference/android/os/Bundle) args)

**This method was deprecated in API level 15.**  
Use the new `[DialogFragment](/reference/android/app/DialogFragment)` class with `[FragmentManager](/reference/android/app/FragmentManager)` instead; this is also available on older platforms through the Android compatibility package.

Provides an opportunity to prepare a managed dialog before it is being shown. The default implementation calls through to `[onPrepareDialog(int, android.app.Dialog)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog\))` for compatibility.

Override this if you need to update a managed dialog based on the state of the application each time it is shown. For example, a time picker dialog might want to be updated with the current time. You should call through to the superclass's implementation. The default implementation will set this Activity as the owner activity on the Dialog.

Parameters

`id`

`int`: The id of the managed dialog.

`dialog`

`Dialog`: The dialog.

`args`

`Bundle`: The dialog arguments provided to `[showDialog(int, android.os.Bundle)](/reference/android/app/Activity#showDialog\(int,%20android.os.Bundle\))`.

**See also:**

*   `[onCreateDialog(int, Bundle)](/reference/android/app/Activity#onCreateDialog\(int,%20android.os.Bundle\))`
*   `[showDialog(int)](/reference/android/app/Activity#showDialog\(int\))`
*   `[dismissDialog(int)](/reference/android/app/Activity#dismissDialog\(int\))`
*   `[removeDialog(int)](/reference/android/app/Activity#removeDialog\(int\))`

### onPrepareDialog

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)  
Deprecated in [API level 15](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onPrepareDialog (int id, 
                [Dialog](/reference/android/app/Dialog) dialog)

**This method was deprecated in API level 15.**  
Old no-arguments version of `[onPrepareDialog(int, android.app.Dialog, android.os.Bundle)](/reference/android/app/Activity#onPrepareDialog\(int,%20android.app.Dialog,%20android.os.Bundle\))`.

Parameters

`id`

`int`

`dialog`

`Dialog`

### onRestart

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onRestart ()

Called after `[onStop()](/reference/android/app/Activity#onStop\(\))` when the current activity is being re-displayed to the user (the user has navigated back to it). It will be followed by `[onStart()](/reference/android/app/Activity#onStart\(\))` and then `[onResume()](/reference/android/app/Activity#onResume\(\))`.

For activities that are using raw `[Cursor](/reference/android/database/Cursor)` objects (instead of creating them through `[managedQuery(android.net.Uri, java.lang.String[], java.lang.String, java.lang.String[], java.lang.String)](/reference/android/app/Activity#managedQuery\(android.net.Uri,%20java.lang.String[],%20java.lang.String,%20java.lang.String[],%20java.lang.String\))`, this is usually the place where the cursor should be requeried (because you had deactivated it in `[onStop()](/reference/android/app/Activity#onStop\(\))`.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onStop()](/reference/android/app/Activity#onStop\(\))`
*   `[onStart()](/reference/android/app/Activity#onStart\(\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`

### onRestoreInstanceState

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onRestoreInstanceState ([Bundle](/reference/android/os/Bundle) savedInstanceState)

This method is called after `[onStart()](/reference/android/app/Activity#onStart\(\))` when the activity is being re-initialized from a previously saved state, given here in savedInstanceState. Most implementations will simply use `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` to restore their state, but it is sometimes convenient to do it here after all of the initialization has been done or to allow subclasses to decide whether to use your default implementation. The default implementation of this method performs a restore of any view state that had previously been frozen by `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`.

This method is called between `[onStart()](/reference/android/app/Activity#onStart\(\))` and `[onPostCreate(Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))`. This method is called only when recreating an activity; the method isn't invoked if `[onStart()](/reference/android/app/Activity#onStart\(\))` is called for any other reason.

Parameters

`savedInstanceState`

`Bundle`: the data most recently supplied in `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`. This value cannot be `null`.

**See also:**

*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`
*   `[onPostCreate(Bundle)](/reference/android/app/Activity#onPostCreate\(android.os.Bundle\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`
*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`

### onResume

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onResume ()

Called after `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`, `[onRestart()](/reference/android/app/Activity#onRestart\(\))`, or `[onPause()](/reference/android/app/Activity#onPause\(\))`. This is usually a hint for your activity to start interacting with the user, which is a good indicator that the activity became active and ready to receive input. This sometimes could also be a transit state toward another resting state. For instance, an activity may be relaunched to `[onPause()](/reference/android/app/Activity#onPause\(\))` due to configuration changes and the activity was visible, but wasn't the top-most activity of an activity task. `[onResume()](/reference/android/app/Activity#onResume\(\))` is guaranteed to be called before `[onPause()](/reference/android/app/Activity#onPause\(\))` in this case which honors the activity lifecycle policy and the activity eventually rests in `[onPause()](/reference/android/app/Activity#onPause\(\))`.

On platform versions prior to `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)` this is also a good place to try to open exclusive-access devices or to get access to singleton resources. Starting with `[Build.VERSION_CODES.Q](/reference/android/os/Build.VERSION_CODES#Q)` there can be multiple resumed activities in the system simultaneously, so `[onTopResumedActivityChanged(boolean)](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))` should be used for that purpose instead.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`
*   `[onRestart()](/reference/android/app/Activity#onRestart\(\))`
*   `[onPostResume()](/reference/android/app/Activity#onPostResume\(\))`
*   `[onPause()](/reference/android/app/Activity#onPause\(\))`
*   `[onTopResumedActivityChanged(boolean)](/reference/android/app/Activity#onTopResumedActivityChanged\(boolean\))`

### onSaveInstanceState

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onSaveInstanceState ([Bundle](/reference/android/os/Bundle) outState)

Called to retrieve per-instance state from an activity before being killed so that the state can be restored in `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` or `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))` (the `[Bundle](/reference/android/os/Bundle)` populated by this method will be passed to both).

This method is called before an activity may be killed so that when it comes back some time in the future it can restore its state. For example, if activity B is launched in front of activity A, and at some point activity A is killed to reclaim resources, activity A will have a chance to save the current state of its user interface via this method so that when the user returns to activity A, the state of the user interface can be restored via `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` or `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`.

Do not confuse this method with activity lifecycle callbacks such as `[onPause()](/reference/android/app/Activity#onPause\(\))`, which is always called when the user no longer actively interacts with an activity, or `[onStop()](/reference/android/app/Activity#onStop\(\))` which is called when activity becomes invisible. One example of when `[onPause()](/reference/android/app/Activity#onPause\(\))` and `[onStop()](/reference/android/app/Activity#onStop\(\))` is called and not this method is when a user navigates back from activity B to activity A: there is no need to call `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` on B because that particular instance will never be restored, so the system avoids calling it. An example when `[onPause()](/reference/android/app/Activity#onPause\(\))` is called and not `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` is when activity B is launched in front of activity A: the system may avoid calling `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))` on activity A if it isn't killed during the lifetime of B since the state of the user interface of A will stay intact.

The default implementation takes care of most of the UI per-instance state for you by calling `[View.onSaveInstanceState()](/reference/android/view/View#onSaveInstanceState\(\))` on each view in the hierarchy that has an id, and by saving the id of the currently focused view (all of which is restored by the default implementation of `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`). If you override this method to save additional information not captured by each individual view, you will likely want to call through to the default implementation, otherwise be prepared to save all of the state of each view yourself.

If called, this method will occur after `[onStop()](/reference/android/app/Activity#onStop\(\))` for applications targeting platforms starting with `[Build.VERSION_CODES.P](/reference/android/os/Build.VERSION_CODES#P)`. For applications targeting earlier platform versions this method will occur before `[onStop()](/reference/android/app/Activity#onStop\(\))` and there are no guarantees about whether it will occur before or after `[onPause()](/reference/android/app/Activity#onPause\(\))`.

Parameters

`outState`

`Bundle`: Bundle in which to place your saved state. This value cannot be `null`.

**See also:**

*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`
*   `[onRestoreInstanceState(Bundle)](/reference/android/app/Activity#onRestoreInstanceState\(android.os.Bundle\))`
*   `[onPause()](/reference/android/app/Activity#onPause\(\))`

### onStart

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onStart ()

Called after `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))` — or after `[onRestart()](/reference/android/app/Activity#onRestart\(\))` when the activity had been stopped, but is now again being displayed to the user. It will usually be followed by `[onResume()](/reference/android/app/Activity#onResume\(\))`. This is a good place to begin drawing visual elements, running animations, etc.

You can call `[finish()](/reference/android/app/Activity#finish\(\))` from within this function, in which case `[onStop()](/reference/android/app/Activity#onStop\(\))` will be immediately called after `[onStart()](/reference/android/app/Activity#onStart\(\))` without the lifecycle transitions in-between (`[onResume()](/reference/android/app/Activity#onResume\(\))`, `[onPause()](/reference/android/app/Activity#onPause\(\))`, etc.) executing.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onCreate(Bundle)](/reference/android/app/Activity#onCreate\(android.os.Bundle\))`
*   `[onStop()](/reference/android/app/Activity#onStop\(\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`

### onStop

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onStop ()

Called when you are no longer visible to the user. You will next receive either `[onRestart()](/reference/android/app/Activity#onRestart\(\))`, `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))`, or nothing, depending on later user activity. This is a good place to stop refreshing UI, running animations and other visual things.

_Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown._

  
If you override this method you _must_ call through to the superclass implementation.

**See also:**

*   `[onRestart()](/reference/android/app/Activity#onRestart\(\))`
*   `[onResume()](/reference/android/app/Activity#onResume\(\))`
*   `[onSaveInstanceState(Bundle)](/reference/android/app/Activity#onSaveInstanceState\(android.os.Bundle\))`
*   `[onDestroy()](/reference/android/app/Activity#onDestroy\(\))`

### onTitleChanged

Added in [API level 1](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onTitleChanged ([CharSequence](/reference/java/lang/CharSequence) title, 
                int color)

Parameters

`title`

`CharSequence`

`color`

`int`

### onUserLeaveHint

Added in [API level 3](/guide/topics/manifest/uses-sdk-element#ApiLevels)

protected void onUserLeaveHint ()

Called as part of the activity lifecycle when an activity is about to go into the background as the result of user choice. For example, when the user presses the Home key, `[onUserLeaveHint()](/reference/android/app/Activity#onUserLeaveHint\(\))` will be called, but when an incoming phone call causes the in-call Activity to be automatically brought to the foreground, `[onUserLeaveHint()](/reference/android/app/Activity#onUserLeaveHint\(\))` will not be called on the activity being interrupted. In cases when it is invoked, this method is called right before the activity's `[onPause()](/reference/android/app/Activity#onPause\(\))` callback.

This callback and `[onUserInteraction()](/reference/android/app/Activity#onUserInteraction\(\))` are intended to help activities manage status bar notifications intelligently; specifically, for helping activities determine the proper time to cancel a notification.

**See also:**

*   `[onUserInteraction()](/reference/android/app/Activity#onUserInteraction\(\))`
*   `[Intent.FLAG_ACTIVITY_NO_USER_ACTION](/reference/android/content/Intent#FLAG_ACTIVITY_NO_USER_ACTION)`

android:allowEmbedded
Indicates that the activity can be launched as the embedded child of another activity, particularly in the case where the child lives in a container, such as a Display owned by another activity. For example, activities that are used for Wear custom notifications declare this so Wear can display the activity in its context stream, which resides in another process.

The default value of this attribute is false.

android:resizeableActivity
Specifies whether the app supports multi-window mode.

Warning: To improve the layout of apps on form factors with smallest width >= 600dp, the system ignores this attribute for apps that target Android 16 (API level 36). Your app can opt out of the Android 16 behavior, but the opt out will be eliminated in a future release. See Device compatibility mode.
You can set this attribute in either the <activity> or <application> element.

If you set this attribute to "true", the user can launch the activity in split-screen and free-form modes. If you set the attribute to "false", the app can't be tested or optimized for a multi-window environment. The system can still put the activity in multi-window mode with compatibility mode applied.

Setting this attribute to "false" doesn't guarantee that there are no other apps in multi-window mode visible on screen, such as in a picture-in-picture, or on other displays. Therefore, setting this flag doesn't mean that your app has exclusive resource access.

If your app targets API level 24 or higher and you do not specify a value for this attribute, the attribute's value defaults to "true".

If your app targets API level 31 or higher, this attribute works differently on small and large screens:

Large screens (sw >= 600dp): all apps support multi-window mode. The attribute indicates whether an app can be resized, not whether the app supports multi-window mode. If resizeableActivity="false", the app is put into compatibility mode when necessary to conform to display dimensions.
Small screens (sw < 600dp): if resizeableActivity="true" and the minimum width and minimum height of the activity are within the multi-window requirements, the app supports multi-window mode. If resizeableActivity="false", the app doesn't support multi-window mode regardless of the activity minimum width and height.
Note:
Device manufacturers can override the API level 31 behavior to improve the layout of apps.
On devices with Android 16 (API level 36) or higher installed, virtual device owners (select trusted and privileged apps) can configure devices they manage to override (ignore) this attribute to improve app layout. See also Companion app streaming.
See Device compatibility mode.

A task's root activity value is applied to all additional activities launched in the task. That is, if the root activity of a task is resizable, then the system treats all other activities in the task as resizable. If the root activity isn't resizable, the other activities in the task aren't resizable.

Note: A task's root activity value is applied to all additional activities launched in the task. That is, if the root activity of a task is resizable, then the system treats all other activities in the task as resizable. If the root activity isn't resizable, the other activities in the task aren't resizable.
