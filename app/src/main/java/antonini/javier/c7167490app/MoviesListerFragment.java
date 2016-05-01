package antonini.javier.c7167490app;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
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
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A placeholder fragment containing a simple view.
 */
public class MoviesListerFragment extends Fragment {

    private final String API_KEY = "21d19ee5393617c36f10cb8f2c175376";
    private String LOG_TAG = "MainFragment";
    movieAdapter adapter;

    private OnMovieSelectedListener mCallback;

    // The container Activity must implement this interface so the frag can deliver messages
    public interface OnMovieSelectedListener {
        /** Called by HeadlinesFragment when a list item is selected */
        public void onMovieSelected(int id);
    }

    public MoviesListerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.movies_list_fragment, container, false);
        ArrayList<Movie> movies = new ArrayList<Movie>(0);
        adapter = new movieAdapter(
                getActivity(),
                R.layout.posters_gridview,
                movies);
        GridView moviesView = (GridView) rootView.findViewById(R.id.postersGridView);
        moviesView.setAdapter(adapter);
        moviesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int movieId = adapter.getItem(position).getId();
                mCallback.onMovieSelected(movieId);

            }
        });
        new getMovies().execute();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMovieSelectedListener) {
            mCallback = (OnMovieSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMovieSelectedListener");
        }
    }

    private class getMovies extends AsyncTask<Void, Void, JSONObject> {
        private String LOG_TAG = "getMovies AsyncTask";
        private String base_url = "api.themoviedb.org";
        private String api_version = "3";
        private String movies_endpoint = "movie";
        private String popular_endpoint = "popular";
        private String top_endpoint = "top_rated";
        private String key_parameter = "api_key";
        private boolean popular = true;
        private boolean top = false;
        public getMovies(){
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
                        .appendPath(movies_endpoint);
                if(popular)
                    builder.appendPath(popular_endpoint);
                if(top)
                    builder.appendPath(top_endpoint);
                builder.appendQueryParameter(key_parameter, API_KEY);
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

        protected void onPostExecute(JSONObject moviesData){
            JSONArray results;
            JSONObject data;
            String page;
            int id;
            String title;
            String poster_url;
            ArrayList<Movie> moviesList = new ArrayList<Movie>();
            Movie movie;
            try {
                if (moviesData != null){
                    page = moviesData.getString("page");
                    results = moviesData.getJSONArray("results");
                    for(int i = 0; i < results.length(); i++){
                        data = results.getJSONObject(i);
                        id = data.getInt("id");
                        title = data.getString("original_title");
                        poster_url = data.getString("poster_path");
                        movie = new Movie(id, poster_url, title);
                        moviesList.add(movie);
                    }
                    adapter.clear();
                    adapter.addAll(moviesList);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    public class movieAdapter extends ArrayAdapter<Movie>{
        Context context;
        ArrayList<Movie> movies;
        int resource;
        private String base_url = "image.tmdb.org";
        private String t_endpoint = "t";
        private String p_endpoint = "p";
        private String format = "w185";

        public movieAdapter(Context context, int resource, ArrayList<Movie> movies){
            super(context, resource, movies);
            this.context = context;
            this.movies = movies;
            this.resource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if(v == null){
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(resource, parent, false);
            }
            Movie movie =  getItem(position);

            if(movie != null){
                ImageView poster = (ImageView) v.findViewById(R.id.posterView);
                poster.setMinimumHeight((int) (context.getResources().getDisplayMetrics().heightPixels * 0.5));
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
                int width= context.getResources().getDisplayMetrics().widthPixels;
                int height = context.getResources().getDisplayMetrics().heightPixels;
                Picasso.with(context).load(poster_url).fit().into(poster);
            }

            return v;
        }
    }
}
