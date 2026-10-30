package io.github.mfedenko09.roastlands.game;

import java.util.List;

final class CodeWindow {

    private static final String TAB_AS_SPACES = "    ";

    private final List<String> lines;
    private final int minVisibleCount;
    private int visibleCount;
    private int firstLine;

    CodeWindow(String code, int minVisibleCount) {
        this.lines = code == null ? List.of() : code.stripTrailing().replace("\t", TAB_AS_SPACES).lines().toList();
        this.minVisibleCount = minVisibleCount;
        this.visibleCount = minVisibleCount;
    }

    List<String> visibleLines() {
        return lines.subList(firstLine, Math.min(lines.size(), firstLine + visibleCount));
    }

    boolean isScrollable() {
        return lines.size() > visibleCount;
    }

    int firstLineNumber() {
        return firstLine + 1;
    }

    int lastLineNumber() {
        return firstLine + visibleLines().size();
    }

    int totalLines() {
        return lines.size();
    }

    void scroll(int delta) {
        int maxFirstLine = Math.max(0, lines.size() - visibleCount);
        firstLine = Math.max(0, Math.min(maxFirstLine, firstLine + delta));
    }

    void fitInto(float freeHeight, float lineHeight) {
        int spareLines = (int) Math.floor(freeHeight / lineHeight);
        visibleCount = Math.max(minVisibleCount, visibleLines().size() + spareLines);
        scroll(0);
    }
}
