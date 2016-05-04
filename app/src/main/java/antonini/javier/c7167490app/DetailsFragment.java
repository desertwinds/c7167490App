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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
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

    private int id;

    private final String API_KEY = "21d19ee5393617c36f10cb8f2c175376";


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment DetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
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
        new getMovieDetails().execute();
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
        final trailerAdapter adapter = new trailerAdapter(getContext(), R.layout.trailers_listview, trailers);

        trailers_list.setAdapter(adapter);

        trailers_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = adapter.getItem(position);
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=" + url)));
            }
        });

        Button favorite = (Button) getActivity().findViewById(R.id.mark_as_favorite);
        favorite.setOnClickListener(new favoriteListener(movie));


    }

    public class favoriteListener implements View.OnClickListener{
        Movie movie;

        public favoriteListener(Movie movie){
            this.movie = movie;
        }

        @Override
        public void onClick(View v) {
            addMovie(movie);
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

    long addMovie(Movie movie){
        long movie_id;
        Cursor movieCursor = getContext().getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                MovieContract.MovieEntry.COLUMN_MOVIE_ID + " = ?",
                new String[] {Integer.toString(movie.getId())},
                null);
        if (movieCursor.moveToFirst()){
            int movieIdIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
            movie_id = movieCursor.getInt(movieIdIndex);
        }
        else {
            ContentValues movieValues= new ContentValues();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            String date = df.format(movie.getRelease_date());

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
        }

        movieCursor.close();

        return movie_id;

    }
}
