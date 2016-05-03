package antonini.javier.c7167490app.data;

import android.provider.BaseColumns;
import android.text.format.Time;

/**
 * Created by Javier on 01-05-2016.
 */
public class MovieContract {

    // To make it easy to query for the exact date, we normalize all dates that go into
    // the database to the start of the the Julian day at UTC.
    public static long normalizeDate(long startDate) {
        // normalize the start date to the beginning of the (UTC) day
        Time time = new Time();
        time.set(startDate);
        int julianDay = Time.getJulianDay(startDate, time.gmtoff);
        return time.setJulianDay(julianDay);
    }

    public static final class MovieEntry implements BaseColumns {

        public static final String TABLE_NAME = "movies";

        public static final String COLUMN_MOVIE_ID = "movie_id";
        public static final String COLUMN_DATE = "date";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_POSTER_URL = "poster_url";
        public static final String COLUMN_VOTE_AVERAGE = "vote_average";
        public static final String COLUMN_DURATION = "duration";
        public static final String COLUMN_OVERVIEW = "overview";
        public static final String COLUMN_IMDB_ID = "imdb_id";
        public static final String COLUMN_HOMEPAGE = "homepage";
        public static final String COLUMN_TRAILERS = "trailers";

    }
}
