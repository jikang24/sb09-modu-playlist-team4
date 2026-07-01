package com.mopl.infra.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class TmdbResponse {

  public record TmdbPageResponse<T>(
      List<T> results,
      @JsonProperty("total_pages") int totalPages,
      @JsonProperty("total_results") int totalResults
  ) {

  }

  public record TmdbMovieResult(
      Long id, String title, String overview,
      @JsonProperty("poster_path") String posterPath,
      @JsonProperty("genre_ids") List<Integer> genreIds,
      @JsonProperty("release_date") String releaseDate,
      @JsonProperty("vote_average") double voteAverage
  ){

  }

  public record TmdbTvResult(
      Long id, String name, String overview,
      @JsonProperty("poster_path") String posterPath,
      @JsonProperty("genre_ids") List<Integer> genreIds,
      @JsonProperty("release_date") String releaseDate,
      @JsonProperty("vote_average") double voteAverage
  ){}


}