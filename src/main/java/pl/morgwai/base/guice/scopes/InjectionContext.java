// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.inject.*;

import static pl.morgwai.base.guice.scopes.InjectionContext.Null.NULL;



/**
 * Stores objects {@link com.google.inject.Scope scoped} to the context of some
 * processing/call/request/session, such as an RPC, a servlet request processing, a session
 * combining several calls etc.
 * Each concrete subclass corresponds to a specific type of events and each instance corresponds to
 * a single such event. For example an instance of {@code HttpRequestContext} may correspond to the
 * processing of a single HTTP request. Creation of instances must be hooked at the beginning of a
 * given processing: for example in Java Servlet environment, a {@code HttpRequestContext} may be
 * created in a {@code Filter}.
 * <p>
 * Note: most {@code Context} classes should rather extend {@link TrackableContext} subclass instead
 * of this one. The main exception are {@code Context} types that are
 * {@link InducedContextScope induced by other Contexts}.</p>
 * <p>
 * Subclasses usually add properties and methods specific to their types, like
 * their call's arguments, a reference to their event objects etc.</p>
 * <p>
 * Multiple threads may run within the same {@code Context} and access or remove the same scoped
 * objects as the state is backed by a {@link ConcurrentMap}. Nevertheless, concurrently accessed
 * scoped objects themself must be thread-safe or accessing them must be properly synchronized in
 * such case.</p>
 * <p>
 * During the standard {@link Serializable Java serialization}, non-serializable scoped objects will
 * be filtered out and the remaining part will be properly serialized.<br/>
 * Methods {@link #prepareForSerialization()} and {@link #restoreAfterDeserialization()} are
 * provided for other serialization mechanisms.<br/>
 * The serialization is <b>not</b> thread-safe, so a {@code Context} that is being serialized must
 * not be accessed by other threads in any way during the process.</p>
 */
public abstract class InjectionContext implements Serializable {



	private transient ConcurrentMap<Key<?>, Object> scopedObjects = new ConcurrentHashMap<>();



	/**
	 * Removes the object given by {@code key} from this {@code Context}.
	 * This forces production of a new instance during the {@link #produceIfAbsent(Key, Provider)
	 * next provisioning} within the {@link ContextScope Scope of this Context}. This is useful if
	 * the currently stored instance is not usable anymore (for example a timed-out connection,
	 * expired token, etc).<br>
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
	 * Provides the scoped object given by {@code key}.
	 * If there already is an instance scoped to this {@code Context}, it is returned immediately.
	 * Otherwise, a new instance is first obtained from {@code producer}, stored for subsequent
	 * calls and returned.
	 */
	protected <T> T produceIfAbsent(Key<T> key, Provider<T> producer) {
		final var stored = scopedObjects.computeIfAbsent(
			key,
			(ignored) -> {
				final var fresh = producer.get();
				return fresh == null ? NULL : fresh;
			}
		);
		@SuppressWarnings("unchecked")
		final var result = (T) (stored == NULL ? null : stored);
		return result;
	}

	enum Null { NULL };



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
	 * This method is idempotent between the most recent modification of this {@code Context}'s
	 * state and the actual serialization, so it is safe to call it manually if it is unknown
	 * whether the standard Java serialization or some other mechanism will be used.</p>
	 * <p>
	 * It must be ensured, that no other threads may access a given {@code Context} between the call
	 * to this method and the actual serialization.</p>
	 */
	protected void prepareForSerialization() {
		serializableScopedObjectEntries = new ArrayList<>(scopedObjects.size());
		try (
			final var buffer = new ByteArrayOutputStream(64);
			final var objectOutputStream = new ObjectOutputStream(buffer);
		) {
			for (var scopedObjectEntry: scopedObjects.entrySet()) {
				final var key = scopedObjectEntry.getKey();
				final var scopedObject = scopedObjectEntry.getValue();

				// omit entries of non-Serializable objects
				if ( !(scopedObject instanceof Serializable)) continue;

				// verify that the object actually serializes
				try {
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
		} catch (IOException ignored) {}
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
	 * This method is idempotent between the actual deserialization and the next invocation of
	 * {@link #prepareForSerialization()}, so it is safe to call it manually right after
	 * deserialization if it is unknown whether the standard Java deserialization or some other
	 * mechanism was used.</p>
	 */
	protected void restoreAfterDeserialization() throws ClassNotFoundException {
		if (serializableScopedObjectEntries == null) return;
		scopedObjects = new ConcurrentHashMap<>();
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



	private static final long serialVersionUID = -638657699306072517L;
}
