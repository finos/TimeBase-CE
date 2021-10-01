/*
 * Copyright 2021 EPAM Systems, Inc
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


import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataInput;

class PolyObjectDeserializer implements Deserializer {

	private final BoundDecoder[] _decoders;
	private final Class<?>[] _types;

	/**
	 * Constructor.
	 * @param decoders An array of BoundDecoder instances.
	 * @param types An array of types of objects to deserialize.
	 */
	public PolyObjectDeserializer(BoundDecoder[] decoders, Class<?>[] types) {
		_decoders = decoders.clone();
		_types = types.clone();
	}

	/**
	 * Deserializes an object using the IDataReader as an input stream.
	 *
	 * @param input An instance of MemoryDataInput to read the serialized object
	 * @return The instance of deserialized object, or null if null was serialized.
	 */
	public Object deserialize(MemoryDataInput input) {

		if (!input.hasAvail() || input.readByte() != BooleanDataType.TRUE)
			return null;

		long messageTime = 0;
		CharSequence symbol = null;

		int codecId = input.readUnsignedShort();
		Class<?> objectType = _types[codecId];

		if (InstrumentMessage.class.isAssignableFrom(objectType)) {
			messageTime = TimeCodec.readNanoTime(input);
			symbol = input.readString();
		}

		Object message = readMessageBody(input, codecId);

		if (message instanceof InstrumentMessage) {
			InstrumentMessage instrument = (InstrumentMessage)message;
			instrument.setNanoTime(messageTime);
			instrument.setSymbol(symbol);
		}
		return message;
	}

	protected Object readMessageBody(MemoryDataInput input, int codecId) {
		BoundDecoder decoder = getDecoder(codecId);
		return decoder.decode(input);
	}

	protected BoundDecoder getDecoder(int codecId) {
		assert codecId < _decoders.length;
		BoundDecoder decoder = _decoders[codecId];
		assert decoder != null;
		return decoder;
	}
}