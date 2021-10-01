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

import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Implements deserialization of objects using Timebase codecs.
 * <p>
 * The class implements serialization for schema with multiple objects. It writes the object type identifier,
 * and uses the codec for this type of object.
 * </p>
 */
class PolyObjectSerializer implements Serializer {

	private final FixedBoundEncoder[] _encoders;
	private final Class<?>[] _typesTable;

	public PolyObjectSerializer(FixedBoundEncoder[] encoders, Class<?>[] types) {
		_encoders = encoders.clone();
		_typesTable = types.clone();
	}

	/**
	 * Serializes specified object using the MemoryDataOutput as an output stream.
	 *
	 * @param message The object to serialize. Can be null.
	 * @param output An instance of MemoryDataOutput to write the serialized object.
	 */
	public void serialize(Object message, MemoryDataOutput output)
	{
		if (message == null) {
			output.writeByte(BooleanDataType.NULL);
		}
		else {
			output.writeByte(BooleanDataType.TRUE);
			int codecId = writeMessageHeader(message, output);
			writeMessageBody(message, output, codecId);
		}
	}

	protected FixedBoundEncoder getEncoder(int id) {
		assert id < _encoders.length;
		FixedBoundEncoder encoder = _encoders[id];
		assert encoder != null;
		return encoder;
	}

	protected int writeMessageHeader(Object message, MemoryDataOutput output) {
		// Choose the appropriate encoder
		int codecId = getTypeId(message.getClass());
		// Write type of the object
		output.writeUnsignedShort(codecId);
		// Write other parts of the header
		if (message instanceof InstrumentMessage) {
			InstrumentMessage instrument = (InstrumentMessage)message;
			TimeCodec.writeNanoTime(instrument.getNanoTime(), output);
			output.writeString(instrument.getSymbol());
		}
		return codecId;
	}

	/**
	 * Writes body of the message to the provided MemoryDataOutput
	 */
	protected void writeMessageBody(Object message, MemoryDataOutput output, int codecId) {
		getEncoder(codecId).encode(message, output);
	}

	protected int getTypeId(Class<?> type) {
		for (int id = 0; id < _typesTable.length; id++) {
			if (type.equals(_typesTable[id]))
				return id;
		}

		throw new IllegalArgumentException ("Message type [" + type.getName() + "] is not allowed.");
	}
}