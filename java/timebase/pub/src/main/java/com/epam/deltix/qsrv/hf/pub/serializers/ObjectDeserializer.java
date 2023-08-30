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

import com.epam.deltix.qsrv.hf.pub.codec.BoundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataInput;


/**
 * Implements deserialization of objects using Timebase codecs.
 *
 * <p>
 * The class implements deserialization for schema with single object. It can deserialize only object
 * of one type and does not red information about type of the deserialized object.
 * </p>
 */
class ObjectDeserializer implements Deserializer {

	private final BoundDecoder _decoder;
	private final Class<?> _objectType;


	/**
	 * Constructor.
	 * @param decoder An instance of BoundDecoder to decode objects.
	 * @param objectType Type of objects to deserialize.
	 */
	public ObjectDeserializer(BoundDecoder decoder, Class<?> objectType) {
		_decoder = decoder;
		_objectType = objectType;
	}

	/**
	 * Deserializes an object using the IDataReader as an input stream.
	 *
	 * @param input An instance of MemoryDataInput to read the serialized object.
	 * @return The instance of deserialized object, or null if null was serialized.
	 */
	public Object deserialize(MemoryDataInput input) {

		if (!input.hasAvail() || input.readByte() != BooleanDataType.TRUE)
			return null;

		long messageTime = 0;
		CharSequence symbol = null;

		if (InstrumentMessage.class.isAssignableFrom(_objectType)) {
			messageTime = TimeCodec.readNanoTime(input);
			symbol = input.readString();
		}

		Object message = readMessageBody(input);

		if (message instanceof InstrumentMessage) {
			InstrumentMessage instrument = (InstrumentMessage)message;
			instrument.setNanoTime(messageTime);
			instrument.setSymbol(symbol);
		}
		return message;
	}

	/**
	 * Reads body of the message from the provided IDataReader
	 *
	 * @param input An instance of MemoryDataInput to read the object from
	 */
	protected Object readMessageBody(MemoryDataInput input) {
		return _decoder.decode(input);
	}
}