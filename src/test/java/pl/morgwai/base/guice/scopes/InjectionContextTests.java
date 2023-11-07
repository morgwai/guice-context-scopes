// Copyright (c) Piotr Morgwai Kotarbinski, Licensed under the Apache License, Version 2.0
package pl.morgwai.base.guice.scopes;

import java.io.*;
import java.lang.annotation.*;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.*;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.junit.Test;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.junit.Assert.*;



public class InjectionContextTests {



	public static class TestContext extends InjectionContext {
		private static final long serialVersionUID = 647980192537461531L;
	}



	@Test
	public void testScopingNulls() {
		final var ctx = new TestContext();
		assertNull("scoping null should return null",
				ctx.produceIfAbsent(Key.get(Long.class), () -> null));
		assertNull("a Key bound to null should retain null",
				ctx.produceIfAbsent(Key.get(Long.class), () -> 666L));
	}



	public static class SerializableNamedObject implements Serializable {
		final String name;
		SerializableNamedObject(String name) { this.name = name; }
		private static final long serialVersionUID = -1453519357553231861L;
	}



	public static class NonSerializableNamedObject {
		final String name;
		NonSerializableNamedObject(String name) { this.name = name; }
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
		final var origin = new TestContext();
		final var serializableObject = new SerializableNamedObject(namedString);
		final var theChosenNumber = Integer.valueOf(666);
		final var anotherNumber = Integer.valueOf(13);
		final var nonSerializableObject = new NonSerializableNamedObject("nonSerializableObject");
		final var listOfInts = new ArrayList<Integer>(1);
		listOfInts.add(theChosenNumber);
		final TypeLiteral<List<Integer>> listType = new TypeLiteral<>() {};
		final Integer[] arrayOfInts = {theChosenNumber};
		final TypeLiteral<Integer[]> arrayType = new TypeLiteral<>() {};
		final String anotherString = "anotherString";
		origin.produceIfAbsent(
				Key.get(SerializableNamedObject.class), () -> serializableObject);
		origin.produceIfAbsent(
				Key.get(String.class, Names.named(STRING_NAME)), () -> namedString);
		origin.produceIfAbsent(
				Key.get(String.class, TheChosenOne.class), () -> theChosenString);
		origin.produceIfAbsent(
				Key.get(Integer.class, new TheChosenOneImpl()), () -> theChosenNumber);
		origin.produceIfAbsent(
				Key.get(NonSerializableNamedObject.class), () -> nonSerializableObject);
		origin.produceIfAbsent(Key.get(listType), () -> listOfInts);
		origin.produceIfAbsent(Key.get(arrayType), () -> arrayOfInts);
		origin.produceIfAbsent(Key.get(Long.class), () -> null);

		if (checkIdempotence) origin.prepareForSerialization();
		final var serializedBytesOutput = new ByteArrayOutputStream(500);
		try (
			serializedBytesOutput;
			final var serializedObjects = new ObjectOutputStream(serializedBytesOutput);
		) {
			serializedObjects.writeObject(origin);
		}
		final TestContext deserialized;
		try (
			final var serializedBytesInput =
				new ByteArrayInputStream(serializedBytesOutput.toByteArray());
			final var serializedObjects = new ObjectInputStream(serializedBytesInput);
		) {
			deserialized = (TestContext) serializedObjects.readObject();
		}
		if (checkIdempotence) origin.restoreAfterDeserialization();

		assertEquals(
			"unannotated serializableObject should (de)serialize",
			serializableObject.name,
			deserialized.produceIfAbsent(
				Key.get(SerializableNamedObject.class),
				() -> new SerializableNamedObject(anotherString)
			).name
		);
		assertEquals(
			"namedString annotated with @Named(stringName) should (de)serialize",
			namedString,
			deserialized.produceIfAbsent(
					Key.get(String.class, Names.named(STRING_NAME)), () -> anotherString)
		);
		final Named namedReflectiveAnnotation =
				InjectionContextTests.class.getDeclaredField(namedString)
						.getAnnotation(Named.class);
		assertEquals(
			"namedString should be obtainable by @Named reflective instance",
			namedString,
			deserialized.produceIfAbsent(
					Key.get(String.class, namedReflectiveAnnotation), () -> anotherString)
		);
		assertEquals(
			"theChosenString annotated with @TheChosenOne should (de)serialize",
			theChosenString,
			deserialized.produceIfAbsent(
					Key.get(String.class, TheChosenOne.class), () -> anotherString)
		);
		assertEquals(
			"theChosenString should be obtainable by @TheChosenOne anonymous instance",
			theChosenString,
			deserialized.produceIfAbsent(
					Key.get(String.class, () -> TheChosenOne.class), () -> anotherString)
		);
		final TheChosenOne theChosenOneReflectiveAnnotation =
				InjectionContextTests.class.getDeclaredField(theChosenString)
					.getAnnotation(TheChosenOne.class);
		assertEquals(
			"theChosenString should be obtainable by @TheChosenOne reflective instance",
			theChosenString,
			deserialized.produceIfAbsent(
				Key.get(String.class, theChosenOneReflectiveAnnotation), () -> anotherString)
		);
		assertEquals(
			"theChosenNumber should be obtainable by @TheChosenOne type",
			theChosenNumber,
			deserialized.produceIfAbsent(
					Key.get(Integer.class, TheChosenOne.class), () -> anotherNumber)
		);
		assertNotEquals(
			"nonSerializableObject should NOT (de)serialize",
			nonSerializableObject.name,
			deserialized.produceIfAbsent(
				Key.get(NonSerializableNamedObject.class),
				() -> new NonSerializableNamedObject(anotherString)
			).name
		);
		assertEquals(
			"listOfInts should (de)serialize and have the same size",
			listOfInts.size(),
			deserialized.produceIfAbsent(Key.get(listType), List::of).size()
		);
		assertEquals(
			"listOfInts should (de)serialize and have the same content",
			listOfInts.get(0),
			deserialized.produceIfAbsent(Key.get(listType), List::of).get(0)
		);
		assertEquals(
			"arrayOfInts should (de)serialize and have the same size",
			arrayOfInts.length,
			deserialized.produceIfAbsent(
					Key.get(arrayType), () -> new Integer[arrayOfInts.length + 1]).length
		);
		assertEquals(
			"arrayOfInts should (de)serialize and have the same content",
			arrayOfInts[0],
			deserialized.produceIfAbsent(Key.get(arrayType), () -> new Integer[1])[0]
		);
		assertSame(
			"deserialized name of serializableObject and namedString should be the same object",
			deserialized.produceIfAbsent(
				Key.get(SerializableNamedObject.class),
				() -> new SerializableNamedObject(anotherString)
			).name,
			deserialized.produceIfAbsent(
				Key.get(String.class, Names.named(STRING_NAME)),
				() -> "yetAnotherString"
			)
		);
		assertNull("a Key bound to null should retain null",
				deserialized.produceIfAbsent(Key.get(Long.class), () -> 666L));
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
