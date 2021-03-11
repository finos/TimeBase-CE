package com.epam.deltix.qsrv.solgen.base;

public enum Language {
    JAVA("-java", "java", "Java"),
    NET("-net", "net", ".NET"),
    PYTHON("-python", "python", "Python"),
    CPP("-cpp", "cpp", "c++"),
    GO("-go", "go", "GoLang");

    private final String cmdOption;
    private final String setOption;
    private final String title;

    Language(String cmdOption, String setOption, String title) {
        this.cmdOption = cmdOption;
        this.setOption = setOption;
        this.title = title;
    }

    public String getCmdOption() {
        return cmdOption;
    }

    public String getTitle() {
        return title;
    }

    public String getSetOption() {
        return setOption;
    }
}
