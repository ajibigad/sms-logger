package com.ajibigad.smslogger.smslogger.network;

import com.ajibigad.smslogger.smslogger.models.Message;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by Julius on 07/08/2016.
 */
public interface MessageApiEndpointInterface {

    @POST("messages")
    Call<ResponseBody> sendMessages(@Body List<Message> messages);

}
