package antonini.javier.c7167490app.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.CancellationSignal;
import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import antonini.javier.c7167490app.data.MovieContract;

/**
 * Created by Javier on 03-05-2016.
 */
public class MovieProvider extends ContentProvider {

    static final int MOVIES = 1;
    static final int MOVIE = 2;
    static final int TRAILERS = 3;
    static final int TRAILERS_FROM_MOVIE = 4;

    private MovieDBHelper mOpenHelper;

    private static final UriMatcher mURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final String authority = MovieContract.CONTENT_AUTHORITY;

    //Here I set the possible paths for my uri matcher
    static
    {
        mURIMatcher.addURI(authority, MovieContract.PATH_MOVIES, 1);
        mURIMatcher.addURI(authority, MovieContract.PATH_MOVIES + "/#", 2);
        mURIMatcher.addURI(authority, MovieContract.PATH_TRAILERS, 3);
        mURIMatcher.addURI(authority, MovieContract.PATH_MOVIES + "/#/" +
                MovieContract.PATH_TRAILERS, 4);
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        final int match = mURIMatcher.match(uri);

        switch (match){
            case MOVIES:
                return MovieContract.MovieEntry.CONTENT_TYPE;
            case MOVIE:
                return MovieContract.MovieEntry.CONTENT_ITEM_TYPE;
            case TRAILERS:
                return MovieContract.TrailerEntry.CONTENT_TYPE;
            case TRAILERS_FROM_MOVIE:
                return MovieContract.TrailerEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private void normalizeDate(ContentValues values){
        if (values.containsKey(MovieContract.MovieEntry.COLUMN_DATE)){
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String date = df.format(values.get(MovieContract.MovieEntry.COLUMN_DATE));
            values.put(MovieContract.MovieEntry.COLUMN_DATE, date);
        }
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = mURIMatcher.match(uri);
        Uri returnUri;

        switch (match){
            case MOVIES: {
                long _id = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MovieContract.MovieEntry.buildMovieUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            case TRAILERS: {
                long _id = db.insert(MovieContract.TrailerEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = MovieContract.TrailerEntry.buildTrailerUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return returnUri;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDBHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = mURIMatcher.match(uri);
        int rowsDeleted;

        if ( null == selection ) selection = "1";
        switch (match){
            case MOVIE:{
                rowsDeleted = db.delete(
                        MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs
                );
                break;
            }
            case TRAILERS_FROM_MOVIE:
                rowsDeleted = db.delete(
                        MovieContract.TrailerEntry.TABLE_NAME,
                        MovieContract.TrailerEntry.TABLE_NAME + "." +
                                MovieContract.TrailerEntry.COLUMN_MOVIE_ID + " = ?",
                        selectionArgs
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }

    private Cursor getMovie(int id, String[] projection, String sortOrder){
        String selection =
                MovieContract.MovieEntry.TABLE_NAME + "." +
                        MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[]{Integer.toString(id)};

        return mOpenHelper.getReadableDatabase().query(
                MovieContract.MovieEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    private Cursor getTrailersFromMovie(int id, String[] projection, String sortOrder){
        String selection =
                MovieContract.TrailerEntry.TABLE_NAME + "." +
                        MovieContract.TrailerEntry.COLUMN_MOVIE_ID + " = ? ";
        String[] selectionArgs = new String[]{Integer.toString(id)};

        return mOpenHelper.getReadableDatabase().query(
                MovieContract.MovieEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        Cursor retCursor;
        final int match = mURIMatcher.match(uri);
        int movie_id;

        switch (match){
            case MOVIES:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case MOVIE:
                movie_id = MovieContract.MovieEntry.getMovieIdFromUri(uri);
                retCursor = getMovie(movie_id, projection, sortOrder);
                break;
            case TRAILERS:
                retCursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.TrailerEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            case TRAILERS_FROM_MOVIE:
                movie_id = MovieContract.MovieEntry.getMovieIdFromUri(uri);
                retCursor = getTrailersFromMovie(movie_id, projection, sortOrder);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }


        return retCursor;
    }
}
