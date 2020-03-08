package com.example.googlemapexample.placesearch;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface PlaceSearchAPI {

    @GET
    Call<PlaceSearchResponseBody> getPlaces(@Url String endUrl);
}
