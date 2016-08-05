package com.gotofinal.darkrise.plots.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Contains String utility methods such as wrapping String.
 */
public final class StringUtil
{

    private StringUtil()
    {
    }

    /**
     * Wraps {@code string} every {@code lineLength} characters.
     *
     * @param string     string to wrap
     * @param lineLength length to wrap at
     */
    public static List<String> wrapWords(final String string, final int lineLength)
    {

        final String[] intendedLines = StringUtils.split(string, "\\n");
        final ArrayList<String> lines = new ArrayList<>(intendedLines.length);
        for (final String intendedLine : intendedLines)
        {
            final String[] words = intendedLine.split(" ");
            StringBuilder buffer = new StringBuilder();

            for (final String word : words)
            {
                if (word.length() >= lineLength)
                {
                    if (buffer.length() != 0)
                    {
                        lines.add(buffer.toString());
                    }
                    lines.add(word);
                    buffer = new StringBuilder();
                    continue;
                }
                if ((buffer.length() + word.length()) >= lineLength)
                {
                    lines.add(buffer.toString());
                    buffer = new StringBuilder();
                }
                if (buffer.length() != 0)
                {
                    buffer.append(' ');
                }
                buffer.append(word);
            }
            lines.add(buffer.toString());
        }

        return lines;
    }
}
