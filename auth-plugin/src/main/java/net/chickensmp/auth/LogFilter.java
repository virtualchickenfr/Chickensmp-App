package net.chickensmp.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import java.util.regex.Pattern;

/** Keeps passwords out of the console/logs ("X issued server command: /login secret"). */
final class LogFilter extends AbstractFilter {
    private static final Pattern P = Pattern.compile("issued server command: /(chickensmpauth:)?(login|l|register|reg|changepassword|changepw)( |$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern A = Pattern.compile("issued server command: /(chickensmpauth:)?(chickenauth|cauth) setpassword", Pattern.CASE_INSENSITIVE);

    static void install() {
        ((Logger) LogManager.getRootLogger()).addFilter(new LogFilter());
    }

    @Override
    public Result filter(LogEvent event) {
        if (event == null || event.getMessage() == null) return Result.NEUTRAL;
        String m = event.getMessage().getFormattedMessage();
        if (m != null && (P.matcher(m).find() || A.matcher(m).find())) return Result.DENY;
        return Result.NEUTRAL;
    }
}
