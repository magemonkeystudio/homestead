/*
 * CommandBook
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package studio.magemonkey.homestead.util.pagination;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.bukkit.command.CommandSender;
import studio.magemonkey.homestead.util.bungee.BungeeCommandException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Commands that wish to display a paginated list of results can use this class to do
 * the actual pagination, giving a list of items, a page number, and basic formatting information.
 */
public abstract class PaginatedResult<T> {
    protected final int resultsPerPage;

    public PaginatedResult() {
        this(6);
    }

    public PaginatedResult(final int resultsPerPage) {
        assert resultsPerPage > 0;
        this.resultsPerPage = resultsPerPage;
    }

    public void display(final CommandSender sender, final Collection<? extends T> results, final int page) throws
            BungeeCommandException {
        this.display(sender, new ArrayList<>(results), page);
    }

    public void display(final CommandSender sender, final List<? extends T> results, final int page) throws
            BungeeCommandException {
        if (results.isEmpty()) {
            sender.sendMessage("No results found.");
            return;
        }

        final int maxPages = (results.size() / this.resultsPerPage) + 1;
        if ((page <= 0) || (page > maxPages)) {
            throw new BungeeCommandException("Unknown page selected! " + maxPages + " total pages.");
        }

        sender.sendMessage(this.formatHeader(page, maxPages));

        for (int i = this.resultsPerPage * (page - 1);
             (i < (this.resultsPerPage * page)) && (i < results.size());
             i++) {
            sender.sendMessage(this.format(results.get(i), i));
        }
    }

    public abstract String formatHeader(int page, int maxPages);

    public abstract String format(T entry, int index);

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).appendSuper(super.toString())
                .append("resultsPerPage", this.resultsPerPage)
                .toString();
    }
}