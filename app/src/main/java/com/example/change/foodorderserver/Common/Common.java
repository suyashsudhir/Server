package com.example.change.foodorderserver.Common;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.example.change.foodorderserver.Model.Request;
import com.example.change.foodorderserver.Model.User;
import com.example.change.foodorderserver.Remote.APIService;
import com.example.change.foodorderserver.Remote.FCMRetrofitClient;
import com.example.change.foodorderserver.Remote.IGeoCoordinates;
import com.example.change.foodorderserver.Remote.RetrofitClient;

import retrofit2.Retrofit;

public class Common {
    public static User currentUser;
    public static Request currentRequest;
    public static final String UPDATE = "Update";
    public static final String DELETE = "Delete";
    public static final int PICK_IMAGE_REQUEST = 71;
    public static final String baseUrl = "https://maps.googleapis.com";
    private static final String BASE_URL = "https://fcm.googleapis.com/";
    public static String PHONE = "userPhone";

    public static APIService getApiService(){
        return FCMRetrofitClient.getClient(BASE_URL).create(APIService.class);
    }

    public static String codeToStatus(String status) {
        if (status.equals("0"))
            return "Placed";
        else if (status.equals("1"))
            return "Preparing";
        else if (status.equals("2"))
            return "Shipped";
        else
            return "Out For Delivery";

    }

    public static IGeoCoordinates getGeoCodeService() {
        return RetrofitClient.getClient(baseUrl).create(IGeoCoordinates.class);

    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int newWidth, int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float scaleX = newWidth / (float) bitmap.getWidth();
        float scaleY = newHeight / (float) bitmap.getHeight();

        float pivotX=0,pivotY=0;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(scaleX,scaleY,pivotX,pivotY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap,0,0,new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;

    }
}