package jp.igapyon.mikudocx2md.core;

class TextStyle {
    boolean bold;
    boolean italic;
    boolean strike;
    boolean underline;

    TextStyle copy() {
        TextStyle copy = new TextStyle();
        copy.bold = bold;
        copy.italic = italic;
        copy.strike = strike;
        copy.underline = underline;
        return copy;
    }
}
