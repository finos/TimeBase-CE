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

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Implements serialization of objects using Timebase codecs.
 *
 * <p>
 * The class implement serialization for schema with single object. It can serialize only object
 * of one type and does not write information about type of the serialized object.
 * </p>
 */
class ObjectSerializer implements Serializer {

	private final FixedBoundEncoder _encoder;

	public ObjectSerializer(FixedBoundEncoder encoder) {
		_encoder = encoder;
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
			writeMessageHeader(message, output);
			writeMessageBody(message, output);
		}
	}

	/**
	 * Writes header of the message to the provided IDataWriter
	 *
	 * @param message The message to serialize
	 * @param output An instance of MemoryDataOutput to write the header to
	 */
	protected void writeMessageHeader(Object message, MemoryDataOutput output) {

		if (message instanceof InstrumentMessage) {
			InstrumentMessage instrument = (InstrumentMessage)message;
			TimeCodec.writeNanoTime(instrument.getNanoTime(), output);
			output.writeString(instrument.getSymbol());
		}
	}

	/**
	 * Writes body of the message to the provided IDataWriter
	 *
	 * @param message The message to serialize
	 */
	protected void writeMessageBody(Object message, MemoryDataOutput output) {
		_encoder.encode(message, output);
	}
}