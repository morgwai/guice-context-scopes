// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.io.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.inject.*;
import com.google.inject.name.Named;
import org.junit.Test;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static com.google.inject.name.Names.named;
import static org.junit.Assert.*;



public class InjectionContextTests {



	public static class TestContext extends InjectionContext {
		private static final long serialVersionUID = 647980192537461531L;
	}

	final TestContext ctx = new TestContext();



	@Test
	public void testStoringAndRemovingObjects() {
		final var producerCallCount = new AtomicInteger(0);
		final Provider<String> producer = () -> String.valueOf(producerCallCount.incrementAndGet());
		final var key = Key.get(String.class);

		final var firstScopedString = ctx.produceIfAbsent(key, producer);
		assertSame("firstScopedString should be stored in ctx for subsequent calls",
				firstScopedString, ctx.produceIfAbsent(key, producer));
		assertEquals("producer should be called only once",
				1, producerCallCount.get());
		assertEquals("sanity check",
				"1", firstScopedString);

		ctx.removeScopedObject(key);
		assertNotEquals("after removing firstScopedString, a new String should be produced",
				firstScopedString, ctx.produceIfAbsent(key, producer));
		assertEquals("after removing firstScopedString, producer should be called for the 2nd time",
				2, producerCallCount.get());
	}



	@Test
	public void testStoringNulls() {
		assertNull("scoping null should return null",
				ctx.produceIfAbsent(Key.get(Long.class), () -> null));
		assertNull("a Key bound to null should retain null",
				ctx.produceIfAbsent(Key.get(Long.class), () -> 666L));
	}



	public static class SerializableNamedObject implements Serializable {

		final String name;
		final Object ref;

		SerializableNamedObject(String name) {
			this(name, null);
		}

		SerializableNamedObject(String name, Object toRef) {
			this.name = name;
			this.ref = toRef;
		}

		@Override public String toString() {
			return "SerializableNamedObject { name = \"" + name + "\", ref = " + ref + " }";
		}

		private static final long serialVersionUID = -750527283388014106L;
	}



	public static class NonSerializableNamedObject {
		final String name;
		NonSerializableNamedObject(String name) { this.name = name; }
		@Override public String toString() {
			return "NonSerializableNamedObject { name=\"" + name + "\" }";
		}
	}



	@Retention(RUNTIME)
	@Target({ FIELD, PARAMETER, LOCAL_VARIABLE })
	@BindingAnnotation
	public @interface TheChosenOne {}



	public static class TheChosenOneImpl implements TheChosenOne, Serializable {
		@Override public Class<? extends Annotation> annotationType() { return TheChosenOne.class; }
		private static final long serialVersionUID = 4422834049166922562L;
	}



	@TheChosenOne final String theChosenString = "theChosenString";

	static final String STRING_NAME = "theString";
	@Named(STRING_NAME) final String namedString = "namedString";



	public void testSerialization(boolean checkIdempotence)
			throws IOException, ClassNotFoundException, NoSuchFieldException {

		final var nonSerializableObject = new NonSerializableNamedObject("nonSerializableObject");
		ctx.produceIfAbsent(Key.get(NonSerializableNamedObject.class), () -> nonSerializableObject);
		final var objectWithRefToNonSerializable1Name = "objectWithRefToNonSerializable1";
		final var objectWithRefToNonSerializable1 = new SerializableNamedObject(
				objectWithRefToNonSerializable1Name, nonSerializableObject);
		final var objectWithRefToNonSerializable1Key =
				Key.get(SerializableNamedObject.class, named(objectWithRefToNonSerializable1Name));
		ctx.produceIfAbsent(
				objectWithRefToNonSerializable1Key, () -> objectWithRefToNonSerializable1);

		final var serializableObject = new SerializableNamedObject(namedString);
		final var objectWithRefToAnotherScopedSerializableName =
				"objectWithRefToAnotherScopedSerializable";
		final var objectWithRefToAnotherScopedSerializable = new SerializableNamedObject(
				objectWithRefToAnotherScopedSerializableName, serializableObject);
		final var objectWithRefToAnotherScopedSerializableKey = Key.get(
				SerializableNamedObject.class, named(objectWithRefToAnotherScopedSerializableName));
		ctx.produceIfAbsent(
				Key.get(String.class, named(STRING_NAME)), () -> namedString);
		ctx.produceIfAbsent(
				Key.get(SerializableNamedObject.class), () -> serializableObject);
		ctx.produceIfAbsent(
			objectWithRefToAnotherScopedSerializableKey,
			() -> objectWithRefToAnotherScopedSerializable
		);

		final var objectWithRefToNonSerializable2Name = "objectWithRefToNonSerializable2";
		final var objectWithRefToNonSerializable2 = new SerializableNamedObject(
				objectWithRefToNonSerializable2Name, nonSerializableObject);
		final var objectWithRefToNonSerializable2Key =
				Key.get(SerializableNamedObject.class, named(objectWithRefToNonSerializable2Name));
		ctx.produceIfAbsent(
				objectWithRefToNonSerializable2Key, () -> objectWithRefToNonSerializable2);

		final var theChosenNumber = Integer.valueOf(666);
		final var anotherNumber = Integer.valueOf(13);
		final var listOfInts = new ArrayList<Integer>(1);
		listOfInts.add(theChosenNumber);
		final TypeLiteral<List<Integer>> listType = new TypeLiteral<>() {};
		final Integer[] arrayOfInts = {theChosenNumber};
		final TypeLiteral<Integer[]> arrayType = new TypeLiteral<>() {};
		final String anotherString = "anotherString";
		ctx.produceIfAbsent(
				Key.get(String.class, TheChosenOne.class), () -> theChosenString);
		ctx.produceIfAbsent(
				Key.get(Integer.class, new TheChosenOneImpl()), () -> theChosenNumber);
		ctx.produceIfAbsent(Key.get(listType), () -> listOfInts);
		ctx.produceIfAbsent(Key.get(arrayType), () -> arrayOfInts);
		ctx.produceIfAbsent(Key.get(Long.class), () -> null);

		final var objectWithRefToNonSerializable3Name = "objectWithRefToNonSerializable3";
		final var objectWithRefToNonSerializable3 = new SerializableNamedObject(
			objectWithRefToNonSerializable3Name, nonSerializableObject);
		final var objectWithRefToNonSerializable3Key =
			Key.get(SerializableNamedObject.class, named(objectWithRefToNonSerializable3Name));
		ctx.produceIfAbsent(
			objectWithRefToNonSerializable3Key, () -> objectWithRefToNonSerializable3);

		if (checkIdempotence) ctx.prepareForSerialization();
		final var serializedBytesOutput = new ByteArrayOutputStream(500);
		try (
			serializedBytesOutput;
			final var serializedObjects = new ObjectOutputStream(serializedBytesOutput);
		) {
			serializedObjects.writeObject(ctx);
		}
		final TestContext deserializedCtx;
		try (
			final var serializedBytesInput =
					new ByteArrayInputStream(serializedBytesOutput.toByteArray());
			final var serializedObjects = new ObjectInputStream(serializedBytesInput);
		) {
			deserializedCtx = (TestContext) serializedObjects.readObject();
		}
		if (checkIdempotence) ctx.restoreAfterDeserialization();

		assertNotEquals(
			"nonSerializableObject should NOT (de)serialize",
			nonSerializableObject.name,
			deserializedCtx.produceIfAbsent(
				Key.get(NonSerializableNamedObject.class),
				() -> new NonSerializableNamedObject(anotherString)
			).name
		);
		assertNotEquals(
			"objectWithRefToNonSerializable1 should NOT (de)serialize",
			objectWithRefToNonSerializable1Name,
			deserializedCtx.produceIfAbsent(
				objectWithRefToNonSerializable1Key,
				() -> new SerializableNamedObject(anotherString)
			).name
		);
		assertNotEquals(
			"objectWithRefToNonSerializable2 should NOT (de)serialize",
			objectWithRefToNonSerializable2Name,
			deserializedCtx.produceIfAbsent(
				objectWithRefToNonSerializable2Key,
				() -> new SerializableNamedObject(anotherString)
			).name
		);
		assertNotEquals(
			"objectWithRefToNonSerializable3 should NOT (de)serialize",
			objectWithRefToNonSerializable3Name,
			deserializedCtx.produceIfAbsent(
				objectWithRefToNonSerializable3Key,
				() -> new SerializableNamedObject(anotherString)
			).name
		);

		assertEquals(
			"unannotated serializableObject should (de)serialize",
			serializableObject.name,
			deserializedCtx.produceIfAbsent(
				Key.get(SerializableNamedObject.class),
				() -> new SerializableNamedObject(anotherString)
			).name
		);
		assertEquals(
			"namedString annotated with @Named(stringName) should (de)serialize",
			namedString,
			deserializedCtx.produceIfAbsent(
					Key.get(String.class, named(STRING_NAME)), () -> anotherString)
		);
		final Named namedReflectiveAnnotation =
				InjectionContextTests.class.getDeclaredField(namedString)
						.getAnnotation(Named.class);
		assertEquals(
			"namedString should be obtainable by @Named reflective instance",
			namedString,
			deserializedCtx.produceIfAbsent(
					Key.get(String.class, namedReflectiveAnnotation), () -> anotherString)
		);
		assertSame(
			"deserialized name of serializableObject and namedString should be the same object",
			deserializedCtx.produceIfAbsent(
				Key.get(SerializableNamedObject.class),
				() -> new SerializableNamedObject(anotherString)
			).name,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, named(STRING_NAME)),
				() -> "yetAnotherString"
			)
		);
		assertEquals(
			"objectWithRefToAnotherScopedSerializable should (de)serialize",
			objectWithRefToAnotherScopedSerializableName,
			deserializedCtx.produceIfAbsent(
				objectWithRefToAnotherScopedSerializableKey,
				() -> new SerializableNamedObject(anotherString)
			).name
		);
		assertSame(
			"deserialized ref of objectWithRefToAnotherScopedSerializable and deserialized "
					+ "serializableObject should be the same object",
			deserializedCtx.produceIfAbsent(
				Key.get(SerializableNamedObject.class),
				() -> new SerializableNamedObject(anotherString)
			),
			deserializedCtx.produceIfAbsent(
				objectWithRefToAnotherScopedSerializableKey,
				() -> new SerializableNamedObject(anotherString)
			).ref
		);

		assertEquals(
			"theChosenString annotated with @TheChosenOne should (de)serialize",
			theChosenString,
			deserializedCtx.produceIfAbsent(
					Key.get(String.class, TheChosenOne.class), () -> anotherString)
		);
		assertEquals(
			"theChosenString should be obtainable by @TheChosenOne anonymous instance",
			theChosenString,
			deserializedCtx.produceIfAbsent(
					Key.get(String.class, () -> TheChosenOne.class), () -> anotherString)
		);
		final TheChosenOne theChosenOneReflectiveAnnotation =
				InjectionContextTests.class.getDeclaredField(theChosenString)
						.getAnnotation(TheChosenOne.class);
		assertEquals(
			"theChosenString should be obtainable by @TheChosenOne reflective instance",
			theChosenString,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, theChosenOneReflectiveAnnotation), () -> anotherString)
		);
		assertEquals(
			"theChosenNumber should be obtainable by @TheChosenOne type",
			theChosenNumber,
			deserializedCtx.produceIfAbsent(
					Key.get(Integer.class, TheChosenOne.class), () -> anotherNumber)
		);
		assertEquals(
			"listOfInts should (de)serialize and have the same size",
			listOfInts.size(),
			deserializedCtx.produceIfAbsent(Key.get(listType), List::of).size()
		);
		assertEquals(
			"listOfInts should (de)serialize and have the same content",
			listOfInts.get(0),
			deserializedCtx.produceIfAbsent(Key.get(listType), List::of).get(0)
		);
		assertEquals(
			"arrayOfInts should (de)serialize and have the same size",
			arrayOfInts.length,
			deserializedCtx.produceIfAbsent(
					Key.get(arrayType), () -> new Integer[arrayOfInts.length + 1]).length
		);
		assertEquals(
			"arrayOfInts should (de)serialize and have the same content",
			arrayOfInts[0],
			deserializedCtx.produceIfAbsent(Key.get(arrayType), () -> new Integer[1])[0]
		);
		assertNull("a Key bound to null should retain null",
				deserializedCtx.produceIfAbsent(Key.get(Long.class), () -> 666L));
	}

	@Test
	public void testSerialization()
		throws IOException, ClassNotFoundException, NoSuchFieldException {
		testSerialization(false);
	}

	@Test
	public void testSerializationAndIdempotence()
			throws IOException, ClassNotFoundException, NoSuchFieldException {
		testSerialization(true);
	}
}
