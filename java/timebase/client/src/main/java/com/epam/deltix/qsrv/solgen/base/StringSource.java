package com.epam.deltix.qsrv.solgen.base;

public class StringSource implements Source {

    private final String relativePath;
    private final String content;

    public StringSource(String relativePath, String content) {
        this.relativePath = relativePath;
        this.content = content;
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public String getContent() {
        return content;
    }
}
