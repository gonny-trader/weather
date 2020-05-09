package com.example.myapplication;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;

import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity {
    private GpsTracker gpsTracker;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    NetworkImageView symbolView;
    TextView temperatureView;
    TextView upView;
    TextView downView;
    RecyclerView recyclerView;

    MyAdapter adapter;
    ArrayList<ItemData> list;

    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // -------------------------------GPS
        if(!checkLocationServicesStatus()){
            showDialogForLocationServiceSetting();
        }
        else{
            checkRunTimePermission();
        }
        gpsTracker = new GpsTracker(MainActivity.this);
        double latitude = gpsTracker.getLatitude(); //위도 가져옴
        double longitude = gpsTracker.getLongitude(); //경도 가져옴
        Log.d("위도", Double.toString(latitude));
        Log.d("위도", Double.toString(longitude));
        //----------------------------------


        temperatureView=(TextView)findViewById(R.id.mission1_temperature);
        upView=(TextView)findViewById(R.id.mission1_up_text);
        downView=(TextView)findViewById(R.id.mission1_down_text);
        symbolView=(NetworkImageView)findViewById(R.id.mission1_symbol);
        recyclerView=(RecyclerView)findViewById(R.id.mission1_recycler);

        list=new ArrayList<>();
        adapter=new MyAdapter(list);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new MyItemDecoration());
        recyclerView.setAdapter(adapter);

        queue= Volley.newRequestQueue(this);

        StringRequest currentRequest=new StringRequest(Request.Method.POST, "http://api.openweathermap.org/data/2.5/weather?lat="+Double.toString(latitude)+"&lon="+Double.toString(longitude)+"&mode=xml&units=metric&appid=018efd0cb973e7486e988b05eda9a125", new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        parseXMLCurrent(response);
            }
        }, new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        StringRequest forecastRequest=new StringRequest(Request.Method.POST, "http://api.openweathermap.org/data/2.5/forecast/?lat="+Double.toString(latitude)+"&lon="+Double.toString(longitude)+"&mode=xml&units=metric&appid=018efd0cb973e7486e988b05eda9a125", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                parseXMLForecast(response);
                Log.d("d","앗");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(currentRequest);
        queue.add(forecastRequest);
    }

    private class ItemData {
        public String max;
        public String min;
        public String day;
        public Bitmap image;
    }

    private class MyViewHolder extends RecyclerView.ViewHolder{
        public TextView dayView;
        public TextView maxView;
        public TextView minView;
        public ImageView imageView;

        public MyViewHolder(View itemView){
            super(itemView);
            dayView=(TextView)itemView.findViewById(R.id.mission1_item_day);
            maxView=(TextView)itemView.findViewById(R.id.mission1_item_max);
            minView=(TextView)itemView.findViewById(R.id.mission1_item_min);
            imageView=(ImageView)itemView.findViewById(R.id.mission1_item_image);

        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder>{
        private final List<ItemData> list;
        public MyAdapter(List<ItemData> list){
            this.list=list;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.mission1_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            ItemData vo=list.get(position);
            holder.dayView.setText(vo.day);
            holder.maxView.setText(vo.max);
            holder.minView.setText(vo.min);
            holder.imageView.setImageBitmap(vo.image);

        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    class MyItemDecoration extends RecyclerView.ItemDecoration {
        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(10, 10, 10, 10);
            view.setBackgroundColor(0x88929090);
        }
    }

    private void parseXMLCurrent(String response){
        try{
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            DocumentBuilder builder=factory.newDocumentBuilder();
            Document doc=builder.parse(new InputSource(new StringReader(response)));
            doc.getDocumentElement().normalize();

            Element tempElement=(Element)(doc.getElementsByTagName("temperature").item(0));


            String temperature=tempElement.getAttribute("value");
            String min=tempElement.getAttribute("min");
            String max=tempElement.getAttribute("max");

            temperatureView.setText(temperature);
            upView.setText(max);
            downView.setText(min);

            Element weatherElement=(Element)(doc.getElementsByTagName("weather").item(0));
            String symbol=weatherElement.getAttribute("icon");

            ImageLoader imageLoader=new ImageLoader(queue, new ImageLoader.ImageCache() {
                @Override
                public Bitmap getBitmap(String url) {
                    return null;
                }

                @Override
                public void putBitmap(String url, Bitmap bitmap) {

                }
            });
            symbolView.setImageUrl("http://openweathermap.org/img/w/"+symbol+".png", imageLoader);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void parseXMLForecast(String response){
        try{
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            DocumentBuilder builder=factory.newDocumentBuilder();
            Document doc=builder.parse(new InputSource(new StringReader(response)));
            doc.getDocumentElement().normalize();

            NodeList nodeList=doc.getElementsByTagName("time");
            for(int i=0; i<nodeList.getLength(); i++){

                final ItemData vo=new ItemData();

                Element timeNode=(Element)nodeList.item(i);
                vo.day=timeNode.getAttribute("from").substring(5);


                Element temperatureNode=(Element)timeNode.getElementsByTagName("temperature").item(0);
                vo.max=temperatureNode.getAttribute("max");
                vo.min=temperatureNode.getAttribute("min");


                Element symbolNode=(Element)timeNode.getElementsByTagName("symbol").item(0);
                String symbol=symbolNode.getAttribute("var");

                String url="http://openweathermap.org/img/w/"+symbol+".png";
                ImageRequest imageRequest=new ImageRequest(url, new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        vo.image = response;
                        adapter.notifyDataSetChanged();
                    }
                }, 0, 0, ImageView.ScaleType.CENTER_CROP, null, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });

                queue.add(imageRequest);
                list.add(vo);
            }
            adapter.notifyDataSetChanged();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //GPS 관련 함수들 --------------------------------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        //requestCode가 PERMISSIONS_REQUEST_CODE 이고, permission 개수만큼 수신되었다면
        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length){
            boolean check_result = true; //permission check를 위한 변수

            for(int result : grantResults){
                if(result != PackageManager.PERMISSION_GRANTED){
                    check_result = false;
                    break;
                }
            }

            if(check_result){} // 성공
            else{ // 실패
                if(ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])){
                    Toast.makeText(MainActivity.this, "권한이 거부되었습니다. 앱을 다시 실행하여 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();
                }
                else{
                    Toast.makeText(MainActivity.this, "권한이 거부되었습니다. 설정->앱에서 권한을 허용해야 함니다.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void checkRunTimePermission() {
        //실행중 허가

        //check
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED && hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            //Permission 가지고 있다
        }
        else{
            //permission 거부한적 있음
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])){
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
            else{//거부한적 없음
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }
    }


    //gps활성화
    private void showDialogForLocationServiceSetting(){
        AlertDialog.Builder builer = new AlertDialog.Builder(MainActivity.this);
        builer.setTitle("위치 서비스 비활성화");
        builer.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" + "위치 설정을 수정하시겠습니까?");
        builer.setCancelable(true);
        builer.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builer.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builer.create().show();
    }

    public boolean checkLocationServicesStatus(){
        LocationManager locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


}
