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
package com.epam.deltix.qsrv.solgen.cpp;

import com.epam.deltix.qsrv.solgen.CodegenUtils;
import com.epam.deltix.qsrv.solgen.SolgenUtils;
import com.epam.deltix.qsrv.solgen.StreamMetaData;
import com.epam.deltix.qsrv.solgen.base.*;

import java.util.*;
import java.util.stream.Collectors;

public class MainSample implements Sample {

    static final List<Property> PROPERTIES = Collections.emptyList();

    private static final String SAMPLE_NAME = "main";
    private static final String SCRIPT_NAME = SAMPLE_NAME + ".cpp";
    private static final String TEMPLATE = SAMPLE_NAME + ".cpp-template";

    private static final String STREAM_CODEC_TEMPLATE = "stream-codec.cpp-template";
    private static final String NATIVE_MESSAGE_TEMPLATE = "NativeMessage.cpp-template";
    private static final String NATIVE_MESSAGE_FILE = "codecs/NativeMessage.h";

    private final Source source;

    private final List<Sample> samples = new ArrayList<>();

    private final List<Source> codecSources = new ArrayList<>();

    public MainSample(Properties properties, CppSample... samples) {
        this(properties, Arrays.asList(samples));
    }

    public MainSample(Properties properties, List<CppSample> samples) {
        this(samples, properties.getProperty(CppSampleFactory.TB_URL.getName()));
    }

    public MainSample(List<CppSample> samples, String tbUrl) {
        this.samples.addAll(samples);

        Map<String, String> params = new HashMap<>();
        params.put("cpp.main.declarations", CppCodegenUtils.mainForwardDeclarations(samples));
        params.put("cpp.main.dispatch", CppCodegenUtils.mainDispatch(samples));

        source = new StringSource(
            SCRIPT_NAME,
            SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), TEMPLATE, params)
        );

        generateNativeMessage();
        samples.stream()
            .map(CppSample::getStream)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet())
            .forEach(s -> generateCodecs(tbUrl, s));
    }

    private void generateNativeMessage() {
        codecSources.add(
            new StringSource(
                NATIVE_MESSAGE_FILE,
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), NATIVE_MESSAGE_TEMPLATE, new HashMap<>())
            )
        );
    }

    private void generateCodecs(String tbUrl, String stream) {
        StreamMetaData metaData = CodegenUtils.getStreamMetadata(tbUrl, stream);
        metaData.getAllTypes().forEach(rcd -> {
            CppCodecGenerator codeGenerator = new CppCodecGenerator(metaData.getNameSpace(), rcd);
            codecSources.add(new StringSource(codeGenerator.getFileName(), codeGenerator.getCodec()));
        });

        generateStreamCodec(metaData);
    }

    private void generateStreamCodec(StreamMetaData metaData) {
        Map<String, String> params = new HashMap<>();
        params.put("cpp.streamCodec.header", CppCodegenUtils.streamCodecHeader(metaData));
        params.put("cpp.streamCodec.footer", CppCodegenUtils.streamCodecFooter(metaData));
        params.put("cpp.streamCodec.decoderClassName", CppCodecGenerator.getStreamDecoder(metaData.getNameSpace(), null));
        params.put("cpp.streamCodec.encoderClassName", CppCodecGenerator.getStreamEncoder(metaData.getNameSpace(), null));
        params.put("cpp.streamCodec.namespace", CppCodecGenerator.getNamespace(metaData.getNameSpace(), metaData.getNameSpace()));
        params.put("cpp.streamCodec.declareMessages", CppCodegenUtils.streamCodecDeclareMessages(metaData));
        params.put("cpp.streamCodec.messageTypes", CppCodegenUtils.streamCodecMessageTypes(metaData));
        params.put("cpp.streamCodec.initGuids", CppCodegenUtils.streamCodecInitGuids(metaData));
        params.put("cpp.streamCodec.detectType", CppCodegenUtils.streamCodecDetectType(metaData));
        params.put("cpp.streamCodec.decodeMessage", CppCodegenUtils.streamCodecDecodeMessage(metaData));
        params.put("cpp.streamCodec.registerMessages", CppCodegenUtils.streamCodecRegisterMessages(metaData));

        String fileName =  "codecs/" + metaData.getNameSpace() + "/" +
            CppCodecGenerator.getStreamCodecName(metaData.getNameSpace(), null) + ".h";

        codecSources.add(
            new StringSource(
                fileName,
                SolgenUtils.readTemplateFromClassPath(this.getClass().getPackage(), STREAM_CODEC_TEMPLATE, params)
            )
        );
    }

    @Override
    public void addToProject(Project project) {
        project.addSource(source);
        codecSources.forEach(project::addSource);

        samples.forEach(s -> s.addToProject(project));
    }

}
