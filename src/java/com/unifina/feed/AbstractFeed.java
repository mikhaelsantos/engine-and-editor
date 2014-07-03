package com.unifina.feed;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import com.unifina.data.IEventQueue;
import com.unifina.data.IEventRecipient;
import com.unifina.data.IFeed;
import com.unifina.data.IStreamRequirement;
import com.unifina.domain.data.Feed;
import com.unifina.domain.data.Stream;
import com.unifina.utils.Globals;

/**
 * An AbstractFeed is responsible for starting and stopping an event
 * stream as well as matching the events and their IEventRecipients.
 * @author Henri
 */
public abstract class AbstractFeed implements IFeed {

	protected IEventQueue eventQueue;
	
	protected Set<Object> subscribers = new HashSet<Object>();
	protected HashMap<Object,IEventRecipient> eventRecipientsByKey = new HashMap<>();
//	protected List<Class> validSubscribeTypes;
	
	protected Globals globals;
	protected TimeZone timeZone;

	protected Feed domainObject;
	protected AbstractKeyProvider keyProvider;
	
	public AbstractFeed(Globals globals, Feed domainObject) {
		this.globals = globals;
		this.domainObject = domainObject;
//		validSubscribeTypes = getValidSubscriberClasses();
		keyProvider = createKeyProvider();
	}
	
	/**
	 * This should return a list of classes that are valid parameters
	 * for a call to subscribe(Object)
	 * @return
	 */
//	protected abstract List<Class> getValidSubscriberClasses();
	

	protected AbstractKeyProvider createKeyProvider() {
		try {
			Class keyProviderClass = this.getClass().getClassLoader().loadClass(domainObject.getKeyProviderClass());
			Constructor constructor = keyProviderClass.getConstructor(Globals.class, Feed.class);
			return (AbstractKeyProvider) constructor.newInstance(globals, domainObject);
		} catch (Exception e) {
			throw new RuntimeException("Could not create key provider of class "+domainObject.getKeyProviderClass(),e);
		}
	}
	
	/**
	 * Creates an IEventRecipient that should be set on FeedEvents 
	 * created for this subscriber. 
	 * @param subscriber
	 * @return
	 */
	protected IEventRecipient createEventRecipient(Object subscriber) {
		try {
			Class eventRecipientClass = this.getClass().getClassLoader().loadClass(domainObject.getEventRecipientClass());

			// Construction of StreamEventRecipients (represented by a Stream object in the database)
			if (subscriber instanceof IStreamRequirement && StreamEventRecipient.class.isAssignableFrom(eventRecipientClass)) {
				Constructor constructor = eventRecipientClass.getConstructor(Globals.class, Stream.class);
				return (IEventRecipient) constructor.newInstance(globals, ((IStreamRequirement)subscriber).getStream());
			}
			// Construction of other IEventRecipients (legacy/hardwired)
			else {
				Constructor constructor = eventRecipientClass.getConstructor(Globals.class);
				return (IEventRecipient) constructor.newInstance(globals);
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not create an EventRecipient of class "+domainObject.getEventRecipientClass()+" for subscriber "+subscriber,e);
		}
	}
	
	protected IEventRecipient getEventRecipientForMessage(Object message) {
		return eventRecipientsByKey.get(keyProvider.getMessageKey(message));
	}
	
	public boolean isSubscribed(Object item) {
		return subscribers.contains(item);
//		return canSubscribe(item) && (!subscriptionsByKey.containsKey(getSubscriptionKey(item)) || subscriptionsByKey.get(getSubscriptionKey(item))!=item);
	}
	
//	public boolean canSubscribe(Object item) {
//		boolean valid = false;
//		for (Class c : validSubscribeTypes) {
//			if (c.isAssignableFrom(item.getClass())) {
//				valid = true;
//				break;
//			}
//		}
//		return valid;
//	}
	
	
	/**
	 * Creates a new subscription on this feed and creates an event handler for
	 * this subscription. Returns true if the subscription was successful,
	 * false if the object was already subscribed. Throws an IllegalArgumentException
	 * if the subscription is of the wrong type.
	 */
	public boolean subscribe(Object subscriber) {
//		if (!canSubscribe(subscriber)) {
//			throw new IllegalArgumentException("This feed can only subscribe items of the following classes: "+validSubscribeTypes);
//		}

		// Don't subscribe the same object twice
		if (!isSubscribed(subscriber)) {

			subscribers.add(subscriber);

			// Create and register the event recipient for this subscription if it doesn't already exist
			IEventRecipient recipient;
			Object key = keyProvider.getEventRecipientKey(subscriber);
			if (!eventRecipientsByKey.containsKey(key)) {
				recipient = createEventRecipient(subscriber);
				eventRecipientsByKey.put(key, recipient);
				globals.getDataSource().register(recipient);
			}
			else recipient = eventRecipientsByKey.get(key);
			
			// Register the subscription with the event recipient
			if (recipient instanceof AbstractEventRecipient) {
				((AbstractEventRecipient)recipient).register(subscriber);
			}
			
		}
		return true;
	}
	
//	@Override
//	public IEventRecipient getEventRecipient(Object sub) {
//		Object key = getSubscriptionKey(sub);
//		return eventRecipientsByKey.get(key);
//	}

	@Override
	public void setEventQueue(IEventQueue queue) {
		this.eventQueue = queue;
	}


	@Override
	public void setTimeZone(TimeZone tz) {
		this.timeZone = tz;
	}
	
}