package com.gotofinal.darkrise.plots.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Contains time utility methods such as converting milliseconds to a single time unit.
 */
public final class TimeUtil
{

    private TimeUtil()
    {
    }

    /**
     * Converts milliseconds into a single time unit smaller than days.
     *
     * @param milliseconds milliseconds to convert
     *
     * @return the converted String
     */
    public static String getSingleTimeUnit(final long milliseconds)
    {
        final StringBuilder sb = new StringBuilder();
        if (TimeUnit.MILLISECONDS.toDays(milliseconds) > 0)
        {
            final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            sb.append(sdf.format(new Date(System.currentTimeMillis() + milliseconds)));
        }
        else
        {
            long time = TimeUnit.MILLISECONDS.toHours(milliseconds) % 24;
            if (time > 0)
            {
                sb.append(time).append(" hour");
                if (time != 1)
                {
                    sb.append("s");
                }
            }
            else
            {
                time = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
                if (time > 0)
                {
                    sb.append(time).append(" minute");
                    if (time != 1)
                    {
                        sb.append("s");
                    }
                }
                else
                {
                    time = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;
                    if (time > 0)
                    {
                        sb.append(time).append(" second");
                        if (time != 1)
                        {
                            sb.append("s");
                        }
                    }
                    else
                    {
                        sb.append("now");
                    }
                }
            }
        }

        return sb.toString();
    }
}
