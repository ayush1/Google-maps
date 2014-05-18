package com.example.googlemap;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class MainActivity extends FragmentActivity {

	private GoogleMap gMap;
	
	String[] mPlaceType=null;
	String[] mPlaceTypeName=null;
	
	Spinner mSprPlaceType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setUpMapIfNeeded();
		
		mPlaceType=getResources().getStringArray(R.array.place_type);
		mPlaceTypeName=getResources().getStringArray(R.array.place_type_name);
		
		ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,mPlaceTypeName);
		mSprPlaceType=(Spinner)findViewById(R.id.spr_place_type);
		mSprPlaceType.setAdapter(adapter);
	}

	private void setUpMapIfNeeded() {
		// TODO Auto-generated method stub
		if(gMap==null){
			gMap=((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.the_map)).getMap();
			
			if(gMap!=null){
				setUpMap();
			}
		}
	}

	private void setUpMap() {
		// TODO Auto-generated method stub
		gMap.setMyLocationEnabled(true);
		
		LocationManager locationManager=(LocationManager)getSystemService(LOCATION_SERVICE);
		
		Criteria criteria=new Criteria();
		String provider=locationManager.getBestProvider(criteria, true);
		Location myLocation=locationManager.getLastKnownLocation(provider);
		
		gMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		
		final double latitude=myLocation.getLatitude();
		final double longitude=myLocation.getLongitude();
		
		LatLng latLng=new LatLng(latitude,longitude);
		
		gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
		gMap.animateCamera(CameraUpdateFactory.zoomTo(20));
		gMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude)).title("You are here"));
		
		Button btnFind;
		btnFind=(Button) findViewById(R.id.btn_find);
		
		btnFind.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				int selectedPosition=mSprPlaceType.getSelectedItemPosition();
				String type=mPlaceType[selectedPosition];
				
				StringBuilder sb=new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?");
				sb.append("location="+latitude+","+ longitude);
				sb.append("&radius=5000");
				sb.append("&types="+type);
				sb.append("&sensor=true");
				sb.append("&key=AIzaSyDg8P5nIdKdxTNMiw_gf2j_9rXFk7AF-4c");
				
				PlacesTask placesTask=new PlacesTask();
				placesTask.execute(sb.toString());
			}	
		});
	}
	
	
	private class PlacesTask extends AsyncTask<String,Integer,String>{
		String data=null;
		protected String doInBackground(String... url){
			try{
				data=downloadUrl(url[0]);
			}catch(Exception e){
				Log.d("Background Task",e.toString());
			}
			return data;
		}
		
		protected void onPostExecute(String result){
			ParserTask parserTask=new ParserTask();
			parserTask.execute(result);
		}
	}
	
	private String downloadUrl(String strUrl)throws IOException{
		String data="";
		InputStream iStream=null;
		HttpURLConnection urlConnection=null;
		
		try{
			URL url=new URL(strUrl);
			
			urlConnection=(HttpURLConnection)url.openConnection();
			urlConnection.connect();
			iStream=urlConnection.getInputStream();
			
			BufferedReader br=new BufferedReader(new InputStreamReader(iStream));
			StringBuffer sb=new StringBuffer();
			
			String line="";
			while((line=br.readLine())!=null){
				sb.append(line);
			}
			
			data=sb.toString();
			br.close();
		}catch(Exception e){
			Log.d("Exception While downloading url",e.toString());
		}finally{
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}
	
	
	private class ParserTask extends AsyncTask<String,Integer,List<HashMap<String,String>>>{
		JSONObject jObject;
		
		protected List<HashMap<String,String>>doInBackground(String... jsonData){
			List<HashMap<String,String>>places=null;
			PlaceJSONParser placeJsonParser=new PlaceJSONParser();
			
			try{
				jObject=new JSONObject(jsonData[0]);
				places=placeJsonParser.parse(jObject);
			}catch(Exception e){
				Log.d("Exception",e.toString());
			}
			return places;
		}
		
		protected void onPostExecute(List<HashMap<String,String>>list){
			gMap.clear();
			
			for(int i=0;i<list.size();i++){
				
				MarkerOptions markerOptions=new MarkerOptions();
				HashMap<String,String> hmPlace=list.get(i);
				
				double lat=Double.parseDouble(hmPlace.get("lat"));
				double lng=Double.parseDouble(hmPlace.get("lng"));
				
				String name=hmPlace.get("place_name");
				String vicinity=hmPlace.get("vicinity");
				
				LatLng latLng=new LatLng(lat,lng);
				markerOptions.position(latLng);
				markerOptions.title(name+":"+vicinity);
				gMap.addMarker(markerOptions);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
