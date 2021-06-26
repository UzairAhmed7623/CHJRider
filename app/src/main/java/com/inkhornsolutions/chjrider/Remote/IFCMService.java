package com.inkhornsolutions.chjrider.Remote;
import com.inkhornsolutions.chjrider.Models.FCMResponse;
import com.inkhornsolutions.chjrider.Models.FCMSendData;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    String SERVER_KEY = "Authorization:key=AAAAZf4rA1M:APA91bHhfDIUOkoU40DcfB4Cwov1ACfVj_aS8MPILy9p9u8UYHksFSJPuExNyfj3ESJE4M0qVn4YVc-dPZaJPOzHcZhCxC9dn1c33JvP297Zcsgx4ZhPkHuvymFa0TMOLYdK5seSjJDh";

    @Headers({
            "Content-Type:application/json",
            SERVER_KEY
    })
    @POST("fcm/send")
    Observable<FCMResponse> sendNotification(@Body FCMSendData body);
}
