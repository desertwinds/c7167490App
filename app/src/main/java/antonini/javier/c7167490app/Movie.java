package antonini.javier.c7167490app;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Javier on 30-04-2016.
 */
public class Movie {

    private String poster_url;
    private String title;
    private int id;
    private double vote_average;
    private int duration;
    private Date release_date;
    private String overview;
    private String imdb_id;
    private String homepage;
    private ArrayList<String> trailers;

    public Movie(int id, String poster, String title){
        this.title = title;
        this.poster_url = poster;
        this.id = id;
    }

    public Movie(int id, String poster, String title, double vote_average, int duration,
                 Date release, String overview, String imdb_id, String homepage){
        this.id = id;
        this.poster_url = poster;
        this.title = title;
        this.vote_average = vote_average;
        this.duration = duration;
        this.release_date = release;
        this.overview = overview;
        this.imdb_id = imdb_id;
        this.homepage = homepage;
        this.trailers = new ArrayList<String>(0);
    }

    public Movie(int id, String poster, String title, double vote_average, int duration,
                 Date release, String overview, String imdb_id, String homepage, ArrayList<String> trailers){
        this.id = id;
        this.poster_url = poster;
        this.title = title;
        this.vote_average = vote_average;
        this.duration = duration;
        this.release_date = release;
        this.overview = overview;
        this.imdb_id = imdb_id;
        this.homepage = homepage;
        this.trailers = trailers;
    }

    public Movie(int id, String poster){
        this.id = id;
        this.poster_url = poster;
    }

    public String getPoster_url(){
        return this.poster_url;
    }

    public String getTitle(){
        return this.title;
    }

    public int getId(){ return this.id;}

    public void addTrailer(String trailer){
        this.trailers.add(trailer);
    }

    public int getDuration(){return this.duration;}

    public String getOverview(){return  this.overview;}

    public String getImdb_id(){return this.imdb_id;}

    public String getHomepage(){return  this.homepage;}

    public double getVote_average(){return this.vote_average;}

    public ArrayList<String> getTrailers(){return this.trailers;}

    public Date getRelease_date(){return this.release_date;}
}
