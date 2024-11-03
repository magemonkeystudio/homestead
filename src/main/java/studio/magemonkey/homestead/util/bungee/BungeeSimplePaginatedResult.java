package studio.magemonkey.homestead.util.bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import studio.magemonkey.homestead.bungee.util.CordUtil;
import studio.magemonkey.homestead.util.pagination.PaginatedResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public abstract class BungeeSimplePaginatedResult<T> extends PaginatedResult<T> {
    protected final String header;

    /**
     * Constructs a new SimplePaginatedResult with a header.
     *
     * @param header header to format this paginator with
     */
    public BungeeSimplePaginatedResult(final String header) {
        super();
        this.header = header;
    }

    /**
     * Constructs a new SimplePaginatedResult with a header and results per page.
     *
     * @param header         header to format this paginator with
     * @param resultsPerPage amount of entries to display per page
     */
    public BungeeSimplePaginatedResult(final String header, final int resultsPerPage) {
        super(resultsPerPage);
        this.header = header;
    }

    public void display(ProxiedPlayer sender, Collection<? extends T> results, int page) throws BungeeCommandException {
        display(sender, new ArrayList<>(results), page);
    }

    public void display(ProxiedPlayer sender, final List<? extends T> results, final int page) throws
            BungeeCommandException {
        if (results.isEmpty()) {
            CordUtil.sendMessage(sender, "No results found.");
            return;
        }

        final int maxPages = (results.size() / this.resultsPerPage) + 1;
        if ((page <= 0) || (page > maxPages)) {
            throw new BungeeCommandException("Unknown page selected! " + maxPages + " total pages.");
        }

        String message = this.formatHeader(page, maxPages);

        for (int i = this.resultsPerPage * (page - 1);
             (i < (this.resultsPerPage * page)) && (i < results.size());
             i++) {
            message += "\n" + this.format(results.get(i), i);
        }
        CordUtil.sendMessage(sender, message);
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
