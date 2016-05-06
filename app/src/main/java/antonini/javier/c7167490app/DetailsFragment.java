package antonini.javier.c7167490app;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import antonini.javier.c7167490app.data.MovieContract;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link DetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class DetailsFragment extends Fragment {

    static final String ARG_ID = "id";
    static final String LOG_TAG = "DetailsFragment";
    private String base_url = "image.tmdb.org";
    private String t_endpoint = "t";
    private String p_endpoint = "p";
    private String format = "w185";
    private trailerAdapter my_trailers;

    private int id;

    private final String API_KEY = "21d19ee5393617c36f10cb8f2c175376";


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment DetailsFragment.
     */
    public static DetailsFragment newInstance(int param1) {
        DetailsFragment fragment = new DetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, param1);
        fragment.setArguments(args);
        return fragment;
    }
    public DetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            id = getArguments().getInt(ARG_ID);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        updateDetailsView(id);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.details_fragment, container, false);
    }



    public void updateDetailsView(int id){
        //TextView titleText = (TextView) getActivity().findViewById(R.id.hello);
        //titleText.setText("The title selected was " + id);
        Movie favorite = findMovie();
        if (favorite != null){
            ImageView favorite_view = (ImageView) getActivity().findViewById(R.id.mark_as_favorite);
            favorite_view.setVisibility(View.GONE);
            ImageView unmark_favorite = (ImageView) getActivity().findViewById(R.id.unmark_as_favorite);
            unmark_favorite.setVisibility(View.VISIBLE);
            updateDetailsWithMovie(favorite);
        }
        else{
            new getMovieDetails().execute();
            ImageView favorite_view = (ImageView) getActivity().findViewById(R.id.mark_as_favorite);
            favorite_view.setVisibility(View.VISIBLE);
            ImageView unmark_favorite = (ImageView) getActivity().findViewById(R.id.unmark_as_favorite);
            unmark_favorite.setVisibility(View.GONE);
        }
    }

    public void updateDetailsWithMovie(Movie movie){
        TextView movieTitle = (TextView) getActivity().findViewById(R.id.movie_title);
        ImageView moviePoster = (ImageView) getActivity().findViewById(R.id.movie_poster);
        TextView movieDate = (TextView) getActivity().findViewById(R.id.movie_release_date);
        TextView movieDuration = (TextView) getActivity().findViewById(R.id.movie_duration);
        TextView movieScore = (TextView) getActivity().findViewById(R.id.movie_score);
        TextView movieOverview = (TextView) getActivity().findViewById(R.id.movie_overview);

        Picasso.with(getContext()).load(getFormatedPosterUrl(movie)).fit().into(moviePoster);
        movieTitle.setText(movie.getTitle());

        DateFormat df = new SimpleDateFormat("yyyy");
        movieDate.setText(df.format(movie.getRelease_date()));
        movieDuration.setText(movie.getDuration() + "min");
        movieScore.setText(movie.getVote_average() + "/10");
        movieOverview.setText(movie.getOverview());

        ArrayList<String> trailers = movie.getTrailers();
        ListView trailers_list = (ListView) getActivity().findViewById(R.id.trailers_list);
        my_trailers = new trailerAdapter(getContext(), R.layout.trailers_listview, trailers);

        trailers_list.setAdapter(my_trailers);

        trailers_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = my_trailers.getItem(position);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + url)));
            }
        });

        //Set the listeners for the mark or unmark as favorite icons.
        ImageView favorite_view = (ImageView) getActivity().findViewById(R.id.mark_as_favorite);
        favorite_view.setOnClickListener(new favoriteListener(movie));
        ImageView unmark_favorite = (ImageView) getActivity().findViewById(R.id.unmark_as_favorite);
        unmark_favorite.setOnClickListener(new unfavoriteListener(movie));

        //Set the launcher and visibility for the imdb icon.
        ImageView imdb_view = (ImageView) getActivity().findViewById(R.id.imdb_image);
        if(movie.getImdb_id().length() > 0){
            imdb_view.setVisibility(View.VISIBLE);
            imdb_view.setOnClickListener(new imdbListener(movie));
        }

        //Set the launcher and visibility for the homepage icon.
        ImageView homepage_view = (ImageView) getActivity().findViewById(R.id.homepage_icon);
        if(movie.getHomepage().length() > 0){
            homepage_view.setVisibility(View.VISIBLE);
            homepage_view.setOnClickListener(new homepageListener(movie));
        }

        trailers_list.setFocusable(false);
        setListViewHeightBasedOnChildren(trailers_list);

    }

    public class imdbListener implements View.OnClickListener{
        Movie movie;

        public imdbListener(Movie movie){ this.movie = movie; }

        @Override
        public void onClick(View v) {
            String url = movie.getImdb_id();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.imdb.com/title/" + url)));
        }
    }

    public class homepageListener implements View.OnClickListener{
        Movie movie;

        public homepageListener(Movie movie){ this.movie = movie; }

        @Override
        public void onClick(View v) {
            String url = movie.getHomepage();
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        }
    }


    public class favoriteListener implements View.OnClickListener{
        Movie movie;

        public favoriteListener(Movie movie){
            this.movie = movie;
        }

        @Override
        public void onClick(View v) {
            addMovie(movie);
            v.setVisibility(View.GONE);
            getActivity().findViewById(R.id.unmark_as_favorite).setVisibility(View.VISIBLE);
        }

    }

    public class unfavoriteListener implements View.OnClickListener{
        Movie movie;

        public unfavoriteListener(Movie movie){
            this.movie = movie;
        }

        @Override
        public void onClick(View v) {
            removeMovie(movie);
            v.setVisibility(View.GONE);
            getActivity().findViewById(R.id.mark_as_favorite).setVisibility(View.VISIBLE);
        }

    }

    public String getFormatedPosterUrl(Movie movie){

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(base_url)
                .appendPath(t_endpoint)
                .appendPath(p_endpoint)
                .appendPath(format)
                .appendPath(movie.getPoster_url());
        String poster_url;
        try{
            poster_url = URLDecoder.decode(builder.build().toString(), "UTF-8").toString();
        }
        catch (IOException e){
            Log.e(LOG_TAG, "Error", e);
            return null;
        }

        return poster_url;
    }

    private class getMovieDetails extends AsyncTask<Void , Void, JSONObject> {
        private String LOG_TAG = "getMovieDetails AsyncTask";
        private String base_url = "api.themoviedb.org";
        private String api_version = "3";
        private String movies_endpoint = "movie";
        private String trailers_parameter = "append_to_response";
        private String trailers_value = "videos";
        private String key_parameter = "api_key";


        public getMovieDetails(){
        }

        protected JSONObject doInBackground(Void... a){

            JSONObject result = null;
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String moviesJsonStr = null;

            try{
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("https")
                        .authority(base_url)
                        .appendPath(api_version)
                        .appendPath(movies_endpoint)
                        .appendPath(Integer.toString(id))
                        .appendQueryParameter(key_parameter, API_KEY)
                        .appendQueryParameter(trailers_parameter, trailers_value);
                URL url = new URL(URLDecoder.decode(builder.build().toString(), "UTF-8"));
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                moviesJsonStr = buffer.toString();
            }
            catch (IOException e){
                Log.e(LOG_TAG, "Error", e);
                return null;
            }
            finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                result = new JSONObject(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }

            return result;
        }

        protected void onPostExecute(JSONObject movieData){
            Movie movie;

            try {
                if (movieData != null){
                    String imdb_id = movieData.getString("imdb_id");
                    String title = movieData.getString("title");
                    int duration = movieData.getInt("runtime");
                    String poster_url = movieData.getString("poster_path");
                    String overview = movieData.getString("overview");
                    String homepage = movieData.getString("homepage");
                    String date = movieData.getString("release_date");
                    Date releaseDate = new Date();
                    try{
                        DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                        releaseDate = format.parse(date);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    double average = movieData.getDouble("vote_average");
                    JSONArray videos = movieData.getJSONObject("videos").getJSONArray("results");
                    movie = new Movie(id, poster_url, title, average, duration, releaseDate, overview,
                            imdb_id, homepage);
                    for (int i = 0; i < videos.length(); i++){
                        movie.addTrailer(videos.getJSONObject(i).getString("key"));
                    }
                    updateDetailsWithMovie(movie);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    class trailerAdapter extends ArrayAdapter<String>{
        Context context;
        ArrayList<String> trailers;
        int resource;

        public trailerAdapter(Context context, int resource, ArrayList<String> trailers){
            super(context, resource, trailers);
            this.context = context;
            this.resource = resource;
            this.trailers = trailers;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null){
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(resource, parent, false);
            }

            String trailer = getItem(position);

            if(trailer != null){
                TextView t = (TextView) v.findViewById(R.id.trailer_text);
                t.setText("Trailer " + position);
            }
            return v;
        }
    }

    Movie findMovie(){
        Movie result = null;
        Cursor movieCursor = getContext().getContentResolver().query(
                MovieContract.MovieEntry.buildMovieUri(id),
                null,
                null,
                null,
                null
        );
        try{
            if (movieCursor.moveToFirst()){
                int poster_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER_URL);
                int title_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_TITLE);
                int vote_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE);
                int duration_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_DURATION);
                int release_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_DATE);
                int overview_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_OVERVIEW);
                int imdb_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_IMDB_ID);
                int homepage_index = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_HOMEPAGE);
                String poster_url = movieCursor.getString(poster_index);
                String title = movieCursor.getString(title_index);
                double vote_average = movieCursor.getDouble(vote_index);
                int duration = movieCursor.getInt(duration_index);
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                String date = movieCursor.getString(release_index);
                Date release_date = new Date();
                try {
                    release_date = df.parse(date);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String overview = movieCursor.getString(overview_index);
                String imdb_id = movieCursor.getString(imdb_index);
                String homepage = movieCursor.getString(homepage_index);
                ArrayList<String> trailers = getTrailers();
                result = new Movie(id, poster_url, title, vote_average, duration, release_date, overview,
                        imdb_id, homepage, trailers);
            }
        }
        finally {
            movieCursor.close();
        }

        return result;
    }

    ArrayList<String> getTrailers(){
        ArrayList<String> result = new ArrayList<String>(0);
        Cursor trailerCursor = getContext().getContentResolver().query(
                MovieContract.TrailerEntry.buildTrailersFromMovieUri(id),
                null,
                null,
                null,
                null
        );
        try{
            int url_index;
            String url;
            while (trailerCursor.moveToNext()){
                url_index = trailerCursor.getColumnIndex(MovieContract.TrailerEntry.COLUMN_TRAILER_URL);
                url = trailerCursor.getString(url_index);
                result.add(url);
            }
        }
        finally {
            trailerCursor.close();
        }
        return result;
    }

    int removeMovie(Movie movie){
        int movie_count = 0;

        movie_count = getContext().getContentResolver().delete(
                MovieContract.MovieEntry.buildMovieUri(movie.getId()),
                MovieContract.MovieEntry.TABLE_NAME + "." +
                        MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ? ",
                new String[]{Integer.toString(id)}
        );
        return movie_count;
    }


    long addMovie(Movie movie){
        long movie_id;
        Cursor movieCursor = getContext().getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                new String[]{Integer.toString(movie.getId())},
                null);
        if (movieCursor.moveToFirst()){
            int movieIdIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
            movie_id = movieCursor.getInt(movieIdIndex);
        }
        else {
            ContentValues movieValues= new ContentValues();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String date = df.format(movie.getRelease_date());

            movieValues.put(MovieContract.MovieEntry.COLUMN_MOVIE_ID, movie.getId());
            movieValues.put(MovieContract.MovieEntry.COLUMN_IMDB_ID, movie.getImdb_id());
            movieValues.put(MovieContract.MovieEntry.COLUMN_DURATION, movie.getDuration());
            movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, movie.getTitle());
            movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_URL, movie.getPoster_url());
            movieValues.put(MovieContract.MovieEntry.COLUMN_OVERVIEW, movie.getOverview());
            movieValues.put(MovieContract.MovieEntry.COLUMN_HOMEPAGE, movie.getHomepage());
            movieValues.put(MovieContract.MovieEntry.COLUMN_DURATION, movie.getDuration());
            movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, movie.getVote_average());
            movieValues.put(MovieContract.MovieEntry.COLUMN_DATE, date);

            Uri insertedUri = getContext().getContentResolver().insert(
                    MovieContract.MovieEntry.CONTENT_URI,
                    movieValues
            );

            movie_id = ContentUris.parseId(insertedUri);
            if(movie_id >0)
                addTrailers(movie);
        }

        movieCursor.close();

        return movie_id;
    }

    int addTrailers(Movie movie){
        int trailersCount = 0;
        ArrayList<String> trailers_url = movie.getTrailers();
        ContentValues[] trailers = new ContentValues[trailers_url.size()];

        for(int i = 0; i < trailers_url.size(); i++){
            ContentValues trailer_values = new ContentValues();

            trailer_values.put(MovieContract.TrailerEntry.COLUMN_MOVIE_ID, id);
            trailer_values.put(MovieContract.TrailerEntry.COLUMN_TRAILER_URL,trailers_url.get(i));

            trailers[i] = (trailer_values);
        }

        trailersCount = getContext().getContentResolver().bulkInsert(MovieContract.TrailerEntry.CONTENT_URI, trailers);
        Log.e(LOG_TAG, "Trailers added " + trailersCount);
        return trailersCount;
    }

    /**** Method for Setting the Height of the ListView dynamically.
     **** Hack to fix the issue of not showing all the items of the ListView
     **** when placed inside a ScrollView  ****/
    public void setListViewHeightBasedOnChildren(ListView listView) {
        if (my_trailers == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < my_trailers.getCount(); i++) {
            view = my_trailers.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (my_trailers.getCount() - 1));
        listView.setLayoutParams(params);
    }


}
