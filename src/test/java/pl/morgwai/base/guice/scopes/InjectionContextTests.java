// Copyright 2023 Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
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



	/** Serializes and then de-serializes {@code ctx}. */
	static TestContext serialize(TestContext ctx) throws IOException {
		final var serializedBytesOutput = new ByteArrayOutputStream(500);
		try (
			serializedBytesOutput;
			final var serializedObjects = new ObjectOutputStream(serializedBytesOutput);
		) {
			serializedObjects.writeObject(ctx);
		}
		try (
			final var serializedBytesInput =
					new ByteArrayInputStream(serializedBytesOutput.toByteArray());
			final var serializedObjects = new ObjectInputStream(serializedBytesInput);
		) {
			return  (TestContext) serializedObjects.readObject();
		} catch (ClassNotFoundException neverHappens) {
			throw new RuntimeException(neverHappens);
		}
	}



	@Test
	public void testDoubleSerializationWithStoringInBetween() throws IOException {
		final var scopedString = "scopedString";
		ctx.produceIfAbsent(Key.get(Integer.class), () -> 666);
		ctx.prepareForSerialization();
		ctx.produceIfAbsent(Key.get(String.class), () -> scopedString);
		final var deserializedCtx = serialize(ctx);
		assertEquals(
			"ctx should be re-prepared after storing a new object",
			scopedString,
			deserializedCtx.produceIfAbsent(Key.get(String.class), () -> "anotherString")
		);
	}



	@Test
	public void testDoubleSerializationWithRemovingInBetween() throws IOException {
		final var scopedString = "scopedString";
		ctx.produceIfAbsent(Key.get(Integer.class), () -> 666);
		ctx.produceIfAbsent(Key.get(String.class), () -> scopedString);
		ctx.prepareForSerialization();
		ctx.removeScopedObject(Key.get(String.class));
		final var deserializedCtx = serialize(ctx);
		assertNotEquals(
			"ctx should be re-prepared after removing a scoped object",
			scopedString,
			deserializedCtx.produceIfAbsent(Key.get(String.class), () -> "anotherString")
		);
	}



	public static class NonSerializableObject {
		final String value;
		NonSerializableObject(String value) { this.value = value; }
		@Override public String toString() {
			return "NonSerializableObject { value = \"" + value + "\" }";
		}
	}



	@Test
	public void testDoubleSerializationWithModifyingScopedObjectInBetween() throws IOException {
		final var listKey = Key.get(new TypeLiteral<List<Object>>() {});
		final var scopedList = new ArrayList<Object>(2);
		final var nonSerializableObject = new NonSerializableObject("whatever");
		scopedList.add(nonSerializableObject);
		ctx.produceIfAbsent(listKey, () -> scopedList);
		ctx.prepareForSerialization();
		scopedList.remove(nonSerializableObject);
		final var deserializedCtx = serialize(ctx);
		final var anotherList = new ArrayList<Object>(2);
		assertNotSame(
			"scopedList should be serialized after removing nonSerializableObject from it",
			anotherList,
			deserializedCtx.produceIfAbsent(listKey, () -> anotherList)
		);
	}



	public static class SerializableObject implements Serializable {

		final String value;
		final Object ref;

		SerializableObject(String value) {
			this(value, null);
		}

		SerializableObject(String value, Object toRef) {
			this.value = value;
			this.ref = toRef;
		}

		@Override public String toString() {
			return "SerializableObject { value = \"" + value + "\", ref = " + ref + " }";
		}

		private static final long serialVersionUID = 3633217419563701352L;
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

		final var nonSerializableObject = new NonSerializableObject("nonSerializableObject");
		ctx.produceIfAbsent(
			Key.get(NonSerializableObject.class),
			() -> nonSerializableObject
		);
		final var objectWithRefToNonSerializable1Name = "objectWithRefToNonSerializable1";
		final var objectWithRefToNonSerializable1 = new SerializableObject(
			objectWithRefToNonSerializable1Name,
			nonSerializableObject
		);
		final var objectWithRefToNonSerializable1Key =
				Key.get(SerializableObject.class, named(objectWithRefToNonSerializable1Name));
		ctx.produceIfAbsent(
			objectWithRefToNonSerializable1Key,
			() -> objectWithRefToNonSerializable1
		);

		final var serializableObject = new SerializableObject(namedString);
		final var objectWithRefToAnotherScopedSerializableName =
				"objectWithRefToAnotherScopedSerializable";
		final var objectWithRefToAnotherScopedSerializable = new SerializableObject(
			objectWithRefToAnotherScopedSerializableName,
			serializableObject
		);
		final var objectWithRefToAnotherScopedSerializableKey = Key.get(
				SerializableObject.class, named(objectWithRefToAnotherScopedSerializableName));
		ctx.produceIfAbsent(Key.get(String.class, named(STRING_NAME)), () -> namedString);
		ctx.produceIfAbsent(Key.get(SerializableObject.class), () -> serializableObject);
		ctx.produceIfAbsent(
			objectWithRefToAnotherScopedSerializableKey,
			() -> objectWithRefToAnotherScopedSerializable
		);

		final var objectWithRefToNonSerializable2Name = "objectWithRefToNonSerializable2";
		final var objectWithRefToNonSerializable2 = new SerializableObject(
			objectWithRefToNonSerializable2Name,
			nonSerializableObject
		);
		final var objectWithRefToNonSerializable2Key =
				Key.get(SerializableObject.class, named(objectWithRefToNonSerializable2Name));
		ctx.produceIfAbsent(
			objectWithRefToNonSerializable2Key,
			() -> objectWithRefToNonSerializable2
		);

		final var theChosenNumber = Integer.valueOf(666);
		final var anotherNumber = Integer.valueOf(13);
		final var listOfInts = new ArrayList<Integer>(1);
		listOfInts.add(theChosenNumber);
		final TypeLiteral<List<Integer>> listType = new TypeLiteral<>() {};
		final Integer[] arrayOfInts = {theChosenNumber};
		final TypeLiteral<Integer[]> arrayType = new TypeLiteral<>() {};
		final String anotherString = "anotherString";
		ctx.produceIfAbsent(Key.get(String.class, TheChosenOne.class), () -> theChosenString);
		ctx.produceIfAbsent(Key.get(Integer.class, new TheChosenOneImpl()), () -> theChosenNumber);
		ctx.produceIfAbsent(Key.get(listType), () -> listOfInts);
		ctx.produceIfAbsent(Key.get(arrayType), () -> arrayOfInts);
		ctx.produceIfAbsent(Key.get(Long.class), () -> null);

		final var objectWithRefToNonSerializable3Name = "objectWithRefToNonSerializable3";
		final var objectWithRefToNonSerializable3 = new SerializableObject(
			objectWithRefToNonSerializable3Name,
			nonSerializableObject
		);
		final var objectWithRefToNonSerializable3Key =
				Key.get(SerializableObject.class, named(objectWithRefToNonSerializable3Name));
		ctx.produceIfAbsent(
			objectWithRefToNonSerializable3Key,
			() -> objectWithRefToNonSerializable3
		);

		if (checkIdempotence) ctx.prepareForSerialization();
		final var deserializedCtx = serialize(ctx);
		if (checkIdempotence) deserializedCtx.restoreAfterDeserialization();

		assertNotEquals(
			"nonSerializableObject should NOT (de)serialize",
			nonSerializableObject.value,
			deserializedCtx.produceIfAbsent(
				Key.get(NonSerializableObject.class),
				() -> new NonSerializableObject(anotherString)
			).value
		);
		assertNotEquals(
			"objectWithRefToNonSerializable1 should NOT (de)serialize",
			objectWithRefToNonSerializable1Name,
			deserializedCtx.produceIfAbsent(
				objectWithRefToNonSerializable1Key,
				() -> new SerializableObject(anotherString)
			).value
		);
		assertNotEquals(
			"objectWithRefToNonSerializable2 should NOT (de)serialize",
			objectWithRefToNonSerializable2Name,
			deserializedCtx.produceIfAbsent(
				objectWithRefToNonSerializable2Key,
				() -> new SerializableObject(anotherString)
			).value
		);
		assertNotEquals(
			"objectWithRefToNonSerializable3 should NOT (de)serialize",
			objectWithRefToNonSerializable3Name,
			deserializedCtx.produceIfAbsent(
				objectWithRefToNonSerializable3Key,
				() -> new SerializableObject(anotherString)
			).value
		);

		assertEquals(
			"unannotated serializableObject should (de)serialize",
			serializableObject.value,
			deserializedCtx.produceIfAbsent(
				Key.get(SerializableObject.class),
				() -> new SerializableObject(anotherString)
			).value
		);
		assertEquals(
			"namedString annotated with @Named(stringName) should (de)serialize",
			namedString,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, named(STRING_NAME)),
				() -> anotherString
			)
		);
		final Named namedReflectiveAnnotation =
				InjectionContextTests.class.getDeclaredField(namedString)
						.getAnnotation(Named.class);
		assertEquals(
			"namedString should be obtainable by @Named reflective instance",
			namedString,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, namedReflectiveAnnotation),
				() -> anotherString
			)
		);
		assertSame(
			"deserialized name of serializableObject and namedString should be the same object",
			deserializedCtx.produceIfAbsent(
				Key.get(SerializableObject.class),
				() -> new SerializableObject(anotherString)
			).value,
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
				() -> new SerializableObject(anotherString)
			).value
		);
		assertSame(
			"deserialized ref of objectWithRefToAnotherScopedSerializable and deserialized "
					+ "serializableObject should be the same object",
			deserializedCtx.produceIfAbsent(
				Key.get(SerializableObject.class),
				() -> new SerializableObject(anotherString)
			),
			deserializedCtx.produceIfAbsent(
				objectWithRefToAnotherScopedSerializableKey,
				() -> new SerializableObject(anotherString)
			).ref
		);

		assertEquals(
			"theChosenString annotated with @TheChosenOne should (de)serialize",
			theChosenString,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, TheChosenOne.class),
				() -> anotherString
			)
		);
		assertEquals(
			"theChosenString should be obtainable by @TheChosenOne anonymous instance",
			theChosenString,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, () -> TheChosenOne.class),
				() -> anotherString
			)
		);
		final TheChosenOne theChosenOneReflectiveAnnotation =
				InjectionContextTests.class.getDeclaredField(theChosenString)
						.getAnnotation(TheChosenOne.class);
		assertEquals(
			"theChosenString should be obtainable by @TheChosenOne reflective instance",
			theChosenString,
			deserializedCtx.produceIfAbsent(
				Key.get(String.class, theChosenOneReflectiveAnnotation),
				() -> anotherString
			)
		);
		assertEquals(
			"theChosenNumber should be obtainable by @TheChosenOne type",
			theChosenNumber,
			deserializedCtx.produceIfAbsent(
				Key.get(Integer.class, TheChosenOne.class),
				() -> anotherNumber
			)
		);
		assertEquals(
			"listOfInts should (de)serialize and have the same size",
			listOfInts.size(),
			deserializedCtx.produceIfAbsent(
				Key.get(listType),
				List::of
			).size()
		);
		assertEquals(
			"listOfInts should (de)serialize and have the same content",
			listOfInts.get(0),
			deserializedCtx.produceIfAbsent(
				Key.get(listType),
				List::of
			).get(0)
		);
		assertEquals(
			"arrayOfInts should (de)serialize and have the same size",
			arrayOfInts.length,
			deserializedCtx.produceIfAbsent(
				Key.get(arrayType),
				() -> new Integer[arrayOfInts.length + 1]
			).length
		);
		assertEquals(
			"arrayOfInts should (de)serialize and have the same content",
			arrayOfInts[0],
			deserializedCtx.produceIfAbsent(
				Key.get(arrayType),
				() -> new Integer[1]
			)[0]
		);
		assertNull(
			"a Key bound to null should retain null",
			deserializedCtx.produceIfAbsent(
				Key.get(Long.class),
				() -> 666L
			)
		);
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
