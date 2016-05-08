package antonini.javier.c7167490app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.paginate.Paginate;
import com.paginate.abslistview.LoadingListItemCreator;
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

import antonini.javier.c7167490app.data.MovieContract;
/**
 * A placeholder fragment containing a simple view.
 */
public class MoviesListerFragment extends Fragment implements
        Paginate.Callbacks{

    private final String API_KEY = "21d19ee5393617c36f10cb8f2c175376";
    private String LOG_TAG = "MainFragment";
    movieAdapter adapter;
    private int page;
    private boolean loading = false;
    private final String sort_preference = "sort";
    private Paginate paginate;

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
        if (paginate != null) {
            paginate.unbind();
        }
        View rootView = inflater.inflate(R.layout.movies_list_fragment, container, false);
        ArrayList<Movie> movies = new ArrayList<Movie>(0);
        page = 1;
        loading = false;
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        String sort_by = prefs.getString(sort_preference, "popular");
        if (sort_by.equals("favorites")){
            retrieveFavorites();
        }
        else{
            paginate = Paginate.with(moviesView, this)
                    .setLoadingTriggerThreshold(2)
                    .addLoadingListItem(true)
                    .setLoadingListItemCreator(new CustomLoadingListItemCreator())
                    .build();
        }
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        setHasOptionsMenu(true);
        if (context instanceof OnMovieSelectedListener) {
            mCallback = (OnMovieSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnMovieSelectedListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.movie_list_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //In case the first time the app is opened is in Favorites mode, and then it is
        //switched to a different list, the paginate must be set before that.
        if(paginate != null)
            paginate.unbind();

        if(id == R.id.sort_popular){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(sort_preference, "popular");
            editor.commit();
            adapter.clear();
            page = 1;
            GridView moviesView = (GridView) getActivity().findViewById(R.id.postersGridView);
            paginate = Paginate.with(moviesView, this)
                    .setLoadingTriggerThreshold(2)
                    .addLoadingListItem(true)
                    .setLoadingListItemCreator(new CustomLoadingListItemCreator())
                    .build();
        }
        else if (id == R.id.sort_rating){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(sort_preference, "top_rated");
            editor.commit();
            adapter.clear();
            page = 1;
            GridView moviesView = (GridView) getActivity().findViewById(R.id.postersGridView);
            paginate = Paginate.with(moviesView, this)
                    .setLoadingTriggerThreshold(2)
                    .addLoadingListItem(true)
                    .setLoadingListItemCreator(new CustomLoadingListItemCreator())
                    .build();
        }
        else if (id == R.id.sort_favorites){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(sort_preference, "favorites");
            editor.commit();
            adapter.clear();
            retrieveFavorites();
        }

        return super.onOptionsItemSelected(item);
    }

    private void retrieveFavorites(){
        Cursor movieCursor = getContext().getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                null,
                null,
                null,
                null);
        int idIndex;
        int posterIndex;
        int id;
        String poster;
        ArrayList<Movie> movies = new ArrayList<Movie>(0);
        Movie movie;
        try{
            while(movieCursor.moveToNext()){
                idIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_MOVIE_ID);
                posterIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry.COLUMN_POSTER_URL);
                id = movieCursor.getInt(idIndex);
                poster = movieCursor.getString(posterIndex);
                movie = new Movie(id, poster);
                movies.add(movie);
            }
            adapter.addAll(movies);
        }
        finally {
            movieCursor.close();
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
        private String page_parameter = "page";
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

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                String sort_by = prefs.getString(sort_preference, "popular");
                if(sort_by.equals("popular"))
                    builder.appendPath(popular_endpoint);
                if(sort_by.equals("top_rated"))
                    builder.appendPath(top_endpoint);
                builder.appendQueryParameter(key_parameter, API_KEY)
                        .appendQueryParameter(page_parameter, Integer.toString(page));
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
            int id;
            String title;
            String poster_url;
            ArrayList<Movie> moviesList = new ArrayList<Movie>();
            Movie movie;
            try {
                if (moviesData != null){
                    results = moviesData.getJSONArray("results");
                    for(int i = 0; i < results.length(); i++){
                        data = results.getJSONObject(i);
                        id = data.getInt("id");
                        title = data.getString("original_title");
                        poster_url = data.getString("poster_path");
                        movie = new Movie(id, poster_url, title);
                        moviesList.add(movie);
                    }
                    adapter.addAll(moviesList);
                    page++;
                    loading = false;
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

    @Override
    public void onLoadMore() {
        Log.d("Paginate", "onLoadMore");
        loading = true;
        new getMovies().execute();
    }

    @Override
    public boolean isLoading() {
        return loading; // Return boolean weather data is already loading or not
    }

    @Override
    public boolean hasLoadedAllItems() {
        return page >= 1000; // If all pages are loaded return true
    }


    private class CustomLoadingListItemCreator implements LoadingListItemCreator {
        @Override
        public View newView(int position, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.loading_item, parent, false);
            view.setTag(new VH(view));
            return view;
        }

        @Override
        public void bindView(int position, View view) {
            VH vh = (VH) view.getTag();
            vh.tvLoading.setText(String.format("Total items loaded: %d.\nLoading more...", adapter.getCount()));
        }
    }

    static class VH {
        TextView tvLoading;

        public VH(View itemView) {
            tvLoading = (TextView) itemView.findViewById(R.id.tv_loading_text);
        }
    }

}
