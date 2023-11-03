// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import com.google.inject.*;



/**
 * Stores objects {@link com.google.inject.Scope scoped} to the context of some
 * processing/call/request/session, such as an RPC, a servlet request processing, a session
 * combining several calls etc. Each concrete subclass corresponds to a specific type of events and
 * each instance corresponds to a single such event. For example an instance of
 * {@code HttpRequestContext} may correspond to the processing of a single HTTP request. Creation of
 * the instance must be hooked at the beginning of a given processing: for example in Java Servlet
 * environment, a {@code HttpRequestContext} may be created in a {@code Filter}.
 * <p>
 * Note: most context classes should rather extend {@link TrackableContext} subclass instead of
 * this one. The main exception are context types that are
 * {@link InducedContextScope induced by other contexts}.</p>
 * <p>
 * Subclasses usually add properties and methods specific to their type, like
 * their call's arguments, a reference to their session object etc.</p>
 * <p>
 * Multiple threads may run within the same context, but the scoped objects that they access must be
 * thread-safe or properly synchronized.</p>
 * <p>
 * During the standard {@link Serializable Java serialization}, non-serializable scoped objects will
 * be filtered out and the serializable part will be properly serialized.<br/>
 * Methods {@link #prepareForSerialization()} and {@link #restoreAfterDeserialization()} are
 * provided for other serialization mechanisms.</p>
 */
public abstract class InjectionContext implements Serializable {



	final boolean disableCircularProxies;
	private transient ConcurrentMap<Key<?>, Object> scopedObjects;



	/**
	 * Constructs a new instance.
	 * @param disableCircularProxies tells this {@code Context} if
	 *     {@link Binder#disableCircularProxies() Guice CircularProxies are disabled}. If so, then a
	 *     {@link ConcurrentHashMap} may be used instead of locking the whole {@code Context}, which
	 *     may greatly improve performance. Furthermore, {@code CircularProxies} are not
	 *     {@link Serializable} and may lead to
	 *     {@link Scopes#isCircularProxy(Object) scoping errors}.
	 */
	protected InjectionContext(boolean disableCircularProxies) {
		this.disableCircularProxies = disableCircularProxies;
		scopedObjects = createScopedObjectsMap(disableCircularProxies);
	}

	static ConcurrentMap<Key<?>, Object> createScopedObjectsMap(boolean disableCircularProxies) {
		if (disableCircularProxies) {
			return new ConcurrentHashMap<>();
		} else {
			return new ConcurrentHashMap<>() {
				@Override
				public Object computeIfAbsent(Key<?> key, Function<? super Key<?>, ?> provider) {
					synchronized (this) {
						var scopedObject = get(key);
						if (scopedObject == null) {
							scopedObject = provider.apply(key);
							if ( !Scopes.isCircularProxy(scopedObject)) put(key, scopedObject);
						}
						return scopedObject;
					}
				}
			};
		}
	}



	/**
	 * Removes the object given by {@code key} from this {@code Context}. This forces production of
	 * a new instance during the {@link #produceIfAbsent(Key, Provider) next provisioning} within
	 * the {@link ContextScope Scope of this Context}. This is useful if the currently stored
	 * instance is not usable anymore (for example a timed-out connection, expired token, etc).<br>
	 * If there's no object stored under {@code key} in this (@code Context}, this method has no
	 * effect.
	 * <p>
	 * <b>Note:</b> If multiple threads run within the same context, care must be taken to prevent
	 * some of them from retaining the old stale instances.</p>
	 */
	public void removeScopedObject(Key<?> key) {
		scopedObjects.remove(key);
	}



	/**
	 * Provides the object given by {@code key}. If there already is an instance scoped to this
	 * {@code Context}, it is returned immediately. Otherwise, a new instance is obtained
	 * from {@code producer} and stored for subsequent calls.
	 */
	protected <T> T produceIfAbsent(Key<T> key, Provider<T> producer) {
		@SuppressWarnings("unchecked")
		final var result = (T) scopedObjects.computeIfAbsent(key, (ignored) -> producer.get());
		return result;
	}



	/**
	 * Filled with the {@link Serializable} part of {@link #scopedObjects} content right before
	 * serialization occurs.
	 */
	private ArrayList<SerializableScopedObjectEntry> serializableScopedObjectEntries;



	/**
	 * Data of {@link Key} and the corresponding object from {@link #scopedObjects} in a
	 * {@link Serializable} form.
	 */
	static class SerializableScopedObjectEntry implements Serializable {

		final Type type;
		final String annotationTypeName;
		final Annotation annotation;
		final Serializable scopedObject;

		SerializableScopedObjectEntry(
			Type type,
			String annotationTypeName,
			Annotation annotation,
			Serializable scopedObject
		) {
			this.type = type;
			this.annotationTypeName = annotationTypeName;
			this.annotation = annotation;
			this.scopedObject = scopedObject;
		}

		private static final long serialVersionUID = 7633750187480552805L;
	}



	/**
	 * Stores {@link Serializable} entries from {@link #scopedObjects} into a fully
	 * {@link Serializable} private {@code List}.
	 * This method is called automatically during the standard Java serialization. It may be called
	 * manually if some other serialization mechanism is used.
	 * <p>
	 * This method is idempotent between the last invocation of
	 * {@link #produceIfAbsent(Key, Provider)} and the actual serialization, so it is safe to call
	 * it manually if it is unknown whether the standard Java serialization or some other mechanism
	 * will be used.</p>
	 */
	protected void prepareForSerialization() {
		if (serializableScopedObjectEntries != null) return;
		serializableScopedObjectEntries = new ArrayList<>(scopedObjects.size());
		for (var scopedObjectEntry: scopedObjects.entrySet()) {
			final var key = scopedObjectEntry.getKey();
			final var scopedObject = scopedObjectEntry.getValue();

			// omit entries of non-Serializable objects
			if ( !(scopedObject instanceof Serializable)) continue;

			// verify that the object actually serializes
			try (
				final var buffer = new ByteArrayOutputStream(64);
				final var objectOutputStream = new ObjectOutputStream(buffer);
			) {
				objectOutputStream.writeObject(scopedObject);
			} catch (IOException e) {
				continue;
			}

			// add SerializableScopedObjectEntry for the given scopedObjectEntry
			serializableScopedObjectEntries.add(new SerializableScopedObjectEntry(
				key.getTypeLiteral().getType(),
				key.getAnnotationType() != null ? key.getAnnotationType().getName() : null,
				key.getAnnotation(),
				(Serializable) scopedObject
			));
		}
	}



	private void writeObject(ObjectOutputStream serializedObjects) throws IOException {
		prepareForSerialization();
		serializedObjects.defaultWriteObject();
	}



	/**
	 * Restores the state of {@link #scopedObjects} from the deserialized data from the private
	 * {@code List} that was filled before serialization with {@link #prepareForSerialization()}.
	 * This method is called automatically during the standard Java deserialization. It may be
	 * called manually if some other deserialization mechanism is used.
	 * <p>
	 * This method is idempotent between the actual deserialization and the first invocation of
	 * {@link #produceIfAbsent(Key, Provider)}, so it is safe to call it manually if it is unknown
	 * whether the standard Java deserialization or some other mechanism will be used.</p>
	 */
	protected void restoreAfterDeserialization() throws ClassNotFoundException {
		if (serializableScopedObjectEntries == null) return;
		scopedObjects = createScopedObjectsMap(disableCircularProxies);
		for (var deserializedEntry: serializableScopedObjectEntries) {
			scopedObjects.put(constructKey(deserializedEntry), deserializedEntry.scopedObject);
		}
		serializableScopedObjectEntries = null;
	}

	static Key<?> constructKey(SerializableScopedObjectEntry deserializedEntry)
			throws ClassNotFoundException {
		final var type = deserializedEntry.type;
		final var annotationTypeName = deserializedEntry.annotationTypeName;
		final var annotation = deserializedEntry.annotation;
		final Key<?> key;
		if (annotationTypeName != null && annotation == null) {
			@SuppressWarnings("unchecked")
			final var annotationClass =
					(Class<? extends Annotation>) Class.forName(annotationTypeName);
			key = Key.get(type, annotationClass);
		} else if (annotation != null) {
			key = Key.get(type, annotation);
		} else {
			key = Key.get(type);
		}
		return key;
	}



	private void readObject(ObjectInputStream serializedObjects)
			throws IOException, ClassNotFoundException {
		serializedObjects.defaultReadObject();
		restoreAfterDeserialization();
	}



	private static final long serialVersionUID = 2834461348587890572L;
}
