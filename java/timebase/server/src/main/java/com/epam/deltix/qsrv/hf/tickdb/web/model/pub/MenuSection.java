package com.epam.deltix.qsrv.hf.tickdb.web.model.pub;

/**
 *
 */
public enum MenuSection {
    Cursors("Cursors", "cursors"),
    Loaders("Loaders", "loaders"),
    Connections("Connections", "connections"),
    Locks("Locks", "locks");

    private String text;
    private String url;

    MenuSection(String text, String url) {
        this.text = text;
        this.url = url;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }
}
