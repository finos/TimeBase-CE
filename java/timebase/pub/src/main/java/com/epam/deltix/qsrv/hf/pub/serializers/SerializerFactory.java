/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.pub.serializers;

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 * A factory for creation of instances of ISerializer and IDeserializer
 */
public class SerializerFactory {

	public enum SerializerKind {
		CompiledSerializer,
		CompiledDeserializer,
		BothCompiled,
		InterpretedSerializer,
		InterpretedDeserializer,
		BothInterpreted
	}

	/**
	 * Creates instances of ISerializer and/or IDeserializer.
	 *
	 * @param schema Descriptor of the type to be serialized/deserialized
	 * @param kind Kind of instances to create:  Serializer, Deserializer or Both
	 * @param typeLoader TypeLoader for the type described by schema; mustn't be null
	 * @return Serializers object with serializer or deserializer, or both
	 * @throws java.lang.ClassNotFoundException when the typeLoader cannot load the class described in the schema
	 * @throws java.lang.IllegalArgumentException when any of the method's parameters is null
	 */
	public static Serializers getSerializers(RecordClassDescriptor schema, SerializerKind kind,
											 TypeLoader typeLoader) throws ClassNotFoundException, IllegalArgumentException {
		if (schema == null)
			throw new IllegalArgumentException("schema");
		if (kind == null)
			throw new IllegalArgumentException("kind");
		if (typeLoader == null)
			throw new IllegalArgumentException("typeLoader");

		Serializer serializer = null;
		Deserializer deserializer = null;

		if (kind == SerializerKind.CompiledSerializer || kind == SerializerKind.BothCompiled)
			serializer = createSerializer(schema, typeLoader, true);
		else if (kind == SerializerKind.InterpretedSerializer || kind == SerializerKind.BothInterpreted)
			serializer = createSerializer(schema, typeLoader, false);

		if (kind == SerializerKind.CompiledDeserializer || kind == SerializerKind.BothCompiled)
			deserializer = createDeserializer(schema, typeLoader.load(schema), typeLoader, true);
		else if (kind == SerializerKind.InterpretedDeserializer || kind == SerializerKind.BothInterpreted)
			deserializer = createDeserializer(schema, typeLoader.load(schema), typeLoader, false);

		return new Serializers(serializer, deserializer);
	}

	/**
	 * Creates instances of ISerializer and/or IDeserializer.
	 *
	 * @param schema Array of descriptors of types to be serialized/deserialized
	 * @param kind Kind of instances to create:  Serializer, Deserializer or Both
	 * @param typeLoader ITypeLoader for serializer and deserializer; mustn't be null
	 * @return Serializers object with serializer or deserializer, or both
	 * @throws java.lang.ClassNotFoundException when the typeLoader cannot load the class described in the schema
	 * @throws java.lang.IllegalArgumentException when any of the method's parameters is null
	 */
	public static Serializers getSerializers(RecordClassDescriptor[] schema, SerializerKind kind,
											 TypeLoader typeLoader) throws ClassNotFoundException, IllegalArgumentException {
		if (schema == null || schema.length == 0)
			throw new IllegalArgumentException("schema");
		if (kind == null)
			throw new IllegalArgumentException("kind");
		if (typeLoader == null)
			throw new IllegalArgumentException("typeLoader");

		if (schema.length == 1)
			return getSerializers(schema[0],  kind, typeLoader);

		Serializer serializer = null;
		Deserializer deserializer = null;

		Class<?>[] objectTypes = new Class<?>[schema.length];
		for (int i = 0; i < schema.length; i++)
			objectTypes[i] = typeLoader.load(schema[i]);

		if (kind == SerializerKind.CompiledSerializer || kind == SerializerKind.BothCompiled)
			serializer = createSerializer(schema, objectTypes, typeLoader, true);
		else if (kind == SerializerKind.InterpretedSerializer || kind == SerializerKind.BothInterpreted)
			serializer = createSerializer(schema, objectTypes, typeLoader, false);

		if (kind == SerializerKind.CompiledDeserializer || kind == SerializerKind.BothCompiled)
			deserializer = createDeserializer(schema, objectTypes, typeLoader, true);
		else if (kind == SerializerKind.InterpretedDeserializer || kind == SerializerKind.BothInterpreted)
			deserializer = createDeserializer(schema, objectTypes, typeLoader, false);

		return new Serializers(serializer, deserializer);
	}

	private static Serializer createSerializer(RecordClassDescriptor schema, TypeLoader typeLoader, boolean compiled) {

		CodecFactory factory = compiled ? CodecFactory.COMPILED : CodecFactory.INTERPRETED;
		FixedBoundEncoder encoder = factory.createFixedBoundEncoder(typeLoader, schema);
		return new ObjectSerializer(encoder);
	}

	private static Deserializer createDeserializer(RecordClassDescriptor schema, Class<?> objectType, TypeLoader typeLoader, boolean compiled) {

		CodecFactory factory = compiled ? CodecFactory.COMPILED : CodecFactory.INTERPRETED;
		BoundDecoder decoder = factory.createFixedBoundDecoder(typeLoader, schema);
		return new ObjectDeserializer(decoder, objectType);
	}

	private static Serializer createSerializer(RecordClassDescriptor[] schema, Class<?>[] objectTypes, TypeLoader typeLoader, boolean compiled) {

		CodecFactory factory = compiled ? CodecFactory.COMPILED : CodecFactory.INTERPRETED;
		FixedBoundEncoder[] encoders = new FixedBoundEncoder[schema.length];

		for (int i = 0; i < schema.length; i++) {
			RecordClassDescriptor descriptor = schema[i];
			encoders[i] = factory.createFixedBoundEncoder(typeLoader, descriptor);
		}
		return new PolyObjectSerializer(encoders, objectTypes);
	}

	private static Deserializer createDeserializer(RecordClassDescriptor[] schema, Class<?>[] objectTypes, TypeLoader typeLoader, boolean compiled) {

		CodecFactory factory = compiled ? CodecFactory.COMPILED : CodecFactory.INTERPRETED;
		BoundDecoder[] decoders = new BoundDecoder[schema.length];

		for (int i = 0; i < schema.length; i++) {
			RecordClassDescriptor descriptor = schema[i];
			decoders[i] = factory.createFixedBoundDecoder(typeLoader, descriptor);
		}
		return new PolyObjectDeserializer(decoders, objectTypes);
	}
}