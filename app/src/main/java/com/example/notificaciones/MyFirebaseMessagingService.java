package com.example.notificaciones;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "default_channel";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "Mensaje recibido: " + remoteMessage.getData());

        // Notificación con título y cuerpo
        String title = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getTitle() : "Nuevo mensaje";
        String body = remoteMessage.getNotification() != null ?
                remoteMessage.getNotification().getBody() : "Tienes una nueva notificación";

        showNotification(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token: " + token);

        // Generar un userId de prueba (en una app real vendría del login del usuario)
        String userId = "user_" + 1;

        sendTokenToServer(userId, token);
    }

    private void sendTokenToServer(String userId, String token) {
        new Thread(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

                String json = "{"
                        + "\"userId\":\"" + userId + "\","
                        + "\"token\":\"" + token + "\""
                        + "}";

                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                        json,
                        okhttp3.MediaType.parse("application/json; charset=utf-8")
                );

                okhttp3.Request request = new okhttp3.Request.Builder()
                        .url("https://0cjt4rn5-3000.use2.devtunnels.ms/notificacion/tokens/register")
                        .post(body)
                        .build();

                okhttp3.Response response = client.newCall(request).execute();
                String respString = response.body().string();

                Log.d(TAG, "Token enviado, respuesta: " + respString);

                // ✅ Mostrar en Toast (desde UI thread)
                new android.os.Handler(getMainLooper()).post(() ->
                        Toast.makeText(getApplicationContext(),
                                "Token enviado ✅\nResp: " + respString,
                                Toast.LENGTH_LONG).show()
                );

            } catch (Exception e) {
                Log.e(TAG, "Error enviando token: ", e);

                new android.os.Handler(getMainLooper()).post(() ->
                        Toast.makeText(getApplicationContext(),
                                "❌ Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }


    private void showNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Canal Notificaciones";
            String description = "Canal para notificaciones FCM";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        .setContentText(body)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify(0, builder.build());
        }

    }
}
