package com.gotofinal.darkrise.plots.util;

import java.util.Random;

/**
 * Contains methods related to numbers. such as rounding decimal places.
 */
public final class NumberUtil
{

    private static final Random random = new Random();

    private NumberUtil()
    {
    }

    /**
     * Parse string to int, if string can't be parsed to int, then it will return null. <br>
     * Based on {@link Integer#parseInt(String)}
     *
     * @param str string to parse
     *
     * @return parsed value or null.
     */
    public static Integer asInt(final String str)
    {
        int result = 0;
        boolean negative = false;
        int i = 0;
        final int len = str.length();
        int limit = - Integer.MAX_VALUE;
        final int multmin;
        int digit;

        if (len > 11) // integer number can't have more than 11 chars -> -2 147 483 648
        {
            return null;
        }
        if (len > 0)
        {
            final char firstChar = str.charAt(0);
            if (firstChar < '0')
            { // Possible leading "+" or "-"
                if (firstChar == '-')
                {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                }
                else if (firstChar != '+')
                {
                    return null;
                }

                if (len == 1) // Cannot have lone "+" or "-"
                {
                    return null;
                }
                i++;
            }
            multmin = limit / 10;
            while (i < len)
            {
                // Accumulating negatively avoids surprises near MAX_VALUE
                final char digitChar = str.charAt(i++);
                if ((digitChar > '9') || (digitChar < '0'))
                {
                    return null;
                }
                digit = digitChar - '0';
                if (result < multmin)
                {
                    return null;
                }
                result *= 10;
                if (result < (limit + digit))
                {
                    return null;
                }
                result -= digit;
            }
        }
        else
        {
            return null;
        }
        return negative ? result : - result;
    }

    /**
     * Checks if the {@code input} is numeric.
     *
     * @param input the input to check
     *
     * @return true if the {@code input} is numeric, otherwise false
     */
    public static boolean isNumeric(final String input)
    {
        return asInt(input) != null;
    }
}
