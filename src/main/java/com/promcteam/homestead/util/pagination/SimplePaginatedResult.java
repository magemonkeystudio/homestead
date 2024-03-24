package com.promcteam.homestead.util.pagination;

import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * An abstract class for paginating a Collection.
 * <p>
 * If you are to paginate a Collection{@literal <String>} with {"SupaHam",
 * "Coolkid", "thatguy"} the output will be something like: <br/>
 * <pre>
 *      Friend list (page 1/1)
 *      SupaHam
 *      Coolkid
 *      thatguy
 * </pre>
 * <p>
 * If the Collection was bigger than the resultsPerPage variable, which is specified in the
 * {@link #SimplePaginatedResult(String, int)} constructor then the maxPages variable in
 * {@link #formatHeader(int, int)} would return the maximum amount of pages, which is simple math:
 * {@code collection size / resultsPerPage}.
 *
 * @param <T> type of the Collection.
 */
public abstract class SimplePaginatedResult<T> extends PaginatedResult<T> {
    protected final String header;

    /**
     * Constructs a new SimplePaginatedResult with a header.
     *
     * @param header header to format this paginator with
     */
    public SimplePaginatedResult(final String header) {
        super();
        this.header = header;
    }

    /**
     * Constructs a new SimplePaginatedResult with a header and results per page.
     *
     * @param header         header to format this paginator with
     * @param resultsPerPage amount of entries to display per page
     */
    public SimplePaginatedResult(final String header, final int resultsPerPage) {
        super(resultsPerPage);
        this.header = header;
    }

    @Override
    public String formatHeader(final int page, final int maxPages) {
        return ChatColor.YELLOW + this.header + " (page " + page + "/" + maxPages + ")";
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("header", this.header)
                .toString();
    }
}
