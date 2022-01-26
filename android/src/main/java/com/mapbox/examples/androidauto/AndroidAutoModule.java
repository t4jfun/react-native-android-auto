//package com.mapbox.examples.androidauto;
//
//import android.content.Intent;
//import android.net.Uri;
//import android.util.Log;
//import android.os.Handler;
//import android.os.Looper;
//import androidx.activity.OnBackPressedCallback;
//import com.facebook.react.bridge.Callback;
//import com.facebook.react.bridge.ReactApplicationContext;
//import com.facebook.react.bridge.ReactContextBaseJavaModule;
//import com.facebook.react.bridge.ReactMethod;
//import com.facebook.react.bridge.ReadableMap;
//import com.facebook.react.bridge.WritableNativeMap;
//import com.facebook.react.module.annotations.ReactModule;
//import com.facebook.react.modules.core.DeviceEventManagerModule;
//import com.facebook.react.modules.debug.DevSettingsModule;
//import com.mapbox.api.directions.v5.DirectionsCriteria;
//import com.mapbox.api.directions.v5.MapboxDirections;
//import com.mapbox.api.directions.v5.models.DirectionsResponse;
//import com.mapbox.api.directions.v5.models.DirectionsRoute;
//import com.mapbox.api.directions.v5.models.RouteOptions;
//import com.mapbox.examples.androidauto.car.MainCarContext;
//import com.mapbox.examples.androidauto.car.MainCarScreen;
//import com.mapbox.examples.androidauto.car.MainScreenManager;
//import com.mapbox.examples.androidauto.car.preview.CarRoutePreviewScreen;
//import com.mapbox.examples.androidauto.car.preview.CarRouteRequest;
//import com.mapbox.examples.androidauto.car.preview.CarRouteRequestCallback;
//import com.mapbox.examples.androidauto.car.preview.RoutePreviewCarContext;
//import com.mapbox.examples.androidauto.car.search.PlaceRecord;
//import com.mapbox.examples.androidauto.car.search.SearchCarContext;
//import com.mapbox.geojson.Point;
//import com.mapbox.search.MapboxSearchSdk;
//import com.mapbox.search.ResponseInfo;
//import com.mapbox.search.ReverseGeoOptions;
//import com.mapbox.search.ReverseGeocodingSearchEngine;
//import com.mapbox.search.SearchCallback;
//import com.mapbox.search.SearchRequestTask;
//import com.mapbox.search.result.SearchResult;
//
//import androidx.annotation.NonNull;
//import androidx.car.app.CarContext;
//import androidx.car.app.CarToast;
//import androidx.car.app.Screen;
//import androidx.car.app.ScreenManager;
//import androidx.car.app.model.Template;
//
//import java.lang.reflect.Array;
//import java.util.Arrays;
//import java.util.List;
//import java.util.WeakHashMap;
//
//import kotlin.coroutines.CoroutineContext;
//import kotlinx.coroutines.BuildersKt;
//import kotlinx.coroutines.GlobalScope;
//import okhttp3.Route;
//import retrofit2.Call;
//import retrofit2.*;
//import retrofit2.Response;
//
//
//@ReactModule(name = AndroidAutoModule.MODULE_NAME)
//public class AndroidAutoModule extends ReactContextBaseJavaModule {
//
//  static final String MODULE_NAME = "CarModule";
//
//  private final Handler mHandler = new Handler(Looper.getMainLooper());
//  private static ReactApplicationContext mReactContext;
//  private MainCarContext mCarContext;
//  private MainScreenManager mMainScreenManager;
//  private ReactCarScreen mReactCarScreen;
//  private Screen mCurrentCarScreen;
//  private ScreenManager mScreenManager;
//  private String lastname;
//  private ReactCarRenderContext lastReactCarRenderContext;
//  private MapboxDirections client;
//
//
//  //@NonNull
//  //private SurfaceRenderer mSurfaceRenderer;
//
//  private WeakHashMap<String, Screen> carScreens;
//  private WeakHashMap<Screen, ReactCarRenderContext> reactCarRenderContextMap;
//
//  public String getName() {
//    return MODULE_NAME;
//  }
//
//  AndroidAutoModule(ReactApplicationContext context) {
//    super(context);
//    carScreens = new WeakHashMap();
//    reactCarRenderContextMap = new WeakHashMap();
//
//    mReactContext = context;
//  }
//
//  private ReverseGeocodingSearchEngine reverseGeocoding;
//  private SearchRequestTask searchRequestTask;
//
//  private final SearchCallback searchCallback = new SearchCallback() {
//
//    @Override
//    public void onResults(@NonNull List<? extends SearchResult> results, @NonNull ResponseInfo responseInfo) {
//      if (results.isEmpty()) {
//        Log.i("ReactAUTO", "No reverse geocoding results");
//      } else {
//        Log.i("ReactAUTO", "Reverse geocoding results: " + results);
//      }
//    }
//
//    @Override
//    public void onError(@NonNull Exception e) {
//      Log.i("ReactAUTO", "Reverse geocoding error", e);
//    }
//  };
//
//
//  private final CarRouteRequestCallback carRouteRequestCallback = new CarRouteRequestCallback() {
//    @Override
//    public void onRoutesReady(@NonNull PlaceRecord placeRecord, @NonNull List<? extends DirectionsRoute> routes) {
//      Log.d("ReactAUTO", "OnRoutesReady " + routes.toString());
//
//      mHandler.post(new Runnable() {
//        @Override
//        public void run() {
//          //mScreenManager.push(screen);
//          Screen screen = new CarRoutePreviewScreen(
//                  new RoutePreviewCarContext(mCarContext),
//                  placeRecord,
//                  (List<? extends DirectionsRoute>) routes.get(0)
//          );
//
//          reactCarRenderContextMap.remove(screen);
//          reactCarRenderContextMap.put(screen, lastReactCarRenderContext);
//          screen.setMarker(lastname);
//
//          carScreens.put(lastname, screen);
//          Log.d("ReactAUTO", carScreens.toString());
//          mCurrentCarScreen = screen;
//          Log.i("ReactAUTO", "pushScreen " + screen.toString());
//          mScreenManager.push(screen);
//        }
//      });
//    }
//
//    public void onUnknownCurrentLocation() {
//      Log.d("ReactAUTO", "onUnknownCurrentLocation");
//    }
//
//    public void onDestinationLocationUnknown() {
//      Log.d("ReactAUTO", "onDestinationLocationUnknown");
//    }
//
//    public void onNoRoutesFound() {
//      Log.d("ReactAUTO", "onNoRoutesFound");
//    }
//  };
//
//  @ReactMethod
//  public void invalidate(String name) {
//    Log.i("ReactAUTO", "invalidate AA Module");
//
//    Screen screen = getScreen(name);
//    mHandler.post(new Runnable() {
//      @Override
//      public void run() {
//        if (screen == mScreenManager.getTop()) {
//          screen.invalidate();
//        }
//      }
//    });
//  }
//
//  @ReactMethod
//  public void setTemplate(
//    String name,
//    ReadableMap renderMap,
//    Callback callback
//  ) {
//    ReactCarRenderContext reactCarRenderContext = new ReactCarRenderContext();
//    Screen screen = getScreen(name);
//    if (screen == null) {
//      screen = mCurrentCarScreen;
//      Log.i("ReactAUTO", "Screen " + name + " not found!");
//    }
//
//    reactCarRenderContext
//      .setEventCallback(callback)
//      .setScreenMarker(screen.getMarker());
//
//    Template template = parseTemplate(renderMap, reactCarRenderContext);
//    reactCarRenderContextMap.remove(screen);
//    reactCarRenderContextMap.put(screen, reactCarRenderContext);
//    Log.i("ReactAUTO", "Screen setTemplate" + template.toString());
//
//    ((MainCarScreen) screen).setTemplate(template);
//  }
//
//  @ReactMethod
//  public void pushScreen(
//    String name,
//    ReadableMap renderMap,
//    Callback callback
//  ) {
//    mHandler.post(new Runnable() {
//      @Override
//      public void run() {
//        ReactCarRenderContext reactCarRenderContext = new ReactCarRenderContext();
//        reactCarRenderContext.setEventCallback(callback).setScreenMarker(name);
//        
//        lastname = name;
//        lastReactCarRenderContext = reactCarRenderContext;
//
//         // TODO: parse template only if template provided
//        String type = renderMap.getString("type");
//        Screen screen;
//        // if type endswith template :
//        if(type.endsWith("template")){
//          Template template = parseTemplate(renderMap, reactCarRenderContext);
//          screen = mMainScreenManager.getNewScreen(false);
//          if(screen.getClass() == MainCarScreen.class) {
//            ((MainCarScreen) screen).setTemplate(template);
//          }
//        } else {
////          reverseGeocoding = MapboxSearchSdk.getReverseGeocodingSearchEngine();
////
////          final ReverseGeoOptions options = new ReverseGeoOptions.Builder(Point.fromLngLat(2.294434, 48.858349))
////                  .limit(1)
////                  .build();
////          searchRequestTask = reverseGeocoding.search(options, searchCallback);
//          Point origin = Point.fromLngLat(19.0, 47.0);
//          Point destination = Point.fromLngLat(19.2, 47.2);
//          RouteOptions routeOptions = RouteOptions.builder()
//                  .applyLanguageAndVoiceUnitOptions(this)
//// lists the coordinate pair i.e. origin and destination
//// If you want to specify waypoints you can pass list of points instead of null
//                  .coordinatesList(listOf(originPoint, destination))
//// set it to true if you want to receive alternate routes to your destination
//                  .alternatives(false)
//// provide the bearing for the origin of the request to ensure
//// that the returned route faces in the direction of the current user movement
//                  .bearingsList(
//                          listOf(
//                                  Bearing.builder()
//                                          .angle(originLocation.bearing.toDouble())
//                                          .degrees(45.0)
//                                          .build(),
//                                  null
//                          )
//                  )
//                  .build()
//          client = MapboxDirections.builder()
//                  .routeOptions()
//                  .destination(destination)
//                  .overview(DirectionsCriteria.OVERVIEW_FULL)
//                  .profile(DirectionsCriteria.PROFILE_DRIVING)
//                  .accessToken(getString(R.string.mapbox_access_token))
//                  .build();
//          client.enqueueCall(new retrofit2.Callback<DirectionsResponse>() {
//            @Override
//            public void onResponse(retrofit2.Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
//              if (response.body() == null) {
//                Log.d("ReactAUTO", "No routes found, make sure you set the right user and access token.");
//                return;
//              } else if (response.body().routes().size() < 1) {
//                Log.d("ReactAUTO", "No routes found");
//
//                return;
//              }
//
//              DirectionsRoute currentRoute = response.body().routes().get(0);
//
//
//            }
//
//            @Override
//            public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
//              Log.d("ReactAUTO", "Error " + throwable.getMessage());
//            }
//          });
//
//
////          SearchCarContext searchCarContext = new SearchCarContext(mCarContext);
////          Point point = Point.fromLngLat(19.021870, 47.493226);
////          PlaceRecord placeRecord = new PlaceRecord(
////                  "some_id",
////                  "some_name",
////                  point,
////                  "some_description",
////                  Arrays.asList()
////          );
////          searchCarContext.getCarRouteRequest().request(placeRecord, carRouteRequestCallback);
//
////          ScreenParser screenParser = new ScreenParser();
////          screen = screenParser.getRoutePreviewScreen(mCarContext, renderMap, type);
//
//
//          return;
//
//
//        }
//        // else if endswith screen:
//
//        // else raise error
//
//
//
//        reactCarRenderContextMap.remove(screen);
//        reactCarRenderContextMap.put(screen, reactCarRenderContext);
//        screen.setMarker(name);
//
//        carScreens.put(name, screen);
//        Log.d("ReactAUTO", carScreens.toString());
//        mCurrentCarScreen = screen;
//        Log.i("ReactAUTO", "pushScreen " + screen.toString());
//        mScreenManager.push(screen);
//      }
//    });
//
//
//
//    mHandler.post(new Runnable() {
//      @Override
//      public void run() {
//        //mScreenManager.push(screen);
//      }
//    });
//  }
//
//  @ReactMethod
//  public void popScreen() {
//    mHandler.post(new Runnable() {
//      @Override
//      public void run() {
//        mScreenManager.pop();
//        removeScreen(mCurrentCarScreen);
//        mCurrentCarScreen = (MainCarScreen) mScreenManager.getTop();
//      }
//    });
//  }
//
//  @ReactMethod
//  public void mapNavigate(String address) {
//    mCarContext.getCarContext().startCarApp(
//      new Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:0,0?q=" + address))
//    );
//  }
//
//  @ReactMethod
//  public void toast(String text, int duration) {
//    CarToast.makeText(mCarContext.getCarContext(), text, duration).show();
//  }
//
//  @ReactMethod
//  public void reload() {
//    DevSettingsModule devSettingsModule = mReactContext.getNativeModule(
//      DevSettingsModule.class
//    );
//    if (devSettingsModule != null) {
//      devSettingsModule.reload();
//    }
//  }
//
//  @ReactMethod
//  public void finishCarApp() {
//    mCarContext.getCarContext().finishCarApp();
//  }
//
//  @ReactMethod
//  public void setEventCallback(String name, Callback callback) {
//    MainCarScreen screen = (MainCarScreen) getScreen(name);
//
//    Log.d("ReactAUTO", "Set callback 1 for " + name);
//    if (screen == null) {
//      return;
//    }
//
//    Log.d("ReactAUTO", "Set callback 2 for " + name);
//    ReactCarRenderContext reactCarRenderContext = reactCarRenderContextMap.get(
//      screen
//    );
//
//    if (reactCarRenderContext == null) {
//      return;
//    }
//
//    Log.d("ReactAUTO", "Set callback 3 for " + name);
//    reactCarRenderContext.setEventCallback(callback);
//  }
//
//  public void setCarContext(MainCarContext carContext, MainScreenManager mainScreenManager) {
//    mCarContext = carContext;
//    mMainScreenManager = mainScreenManager;
//    mReactCarScreen = new ReactCarScreen(mCarContext);
//    mCurrentCarScreen = mainScreenManager.currentScreen(false);
//    mScreenManager = mCurrentCarScreen.getScreenManager();
//    carScreens.put("root", mCurrentCarScreen);
//
//    Log.d("ReactAUTO", carScreens.toString());
//
//    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
//      @Override
//      public void handleOnBackPressed() {
//        Log.d("ReactAUTO", "Back button pressed");
//        mScreenManager.pop();
//        sendEvent("android_auto:back_button", new WritableNativeMap());
//      }
//    };
//
//    carContext.getCarContext().getOnBackPressedDispatcher().addCallback(callback);
//
//    sendEvent("android_auto:ready", new WritableNativeMap());
//  }
//
//  private Template parseTemplate(
//    ReadableMap renderMap,
//    ReactCarRenderContext reactCarRenderContext
//  ) {
//    TemplateParser templateParser = new TemplateParser(reactCarRenderContext);
//    return templateParser.parseTemplate(renderMap);
//  }
//
//  private Screen getScreen(String name) {
//    return carScreens.get(name);
//  }
//
//  private void removeScreen(Screen screen) {
//    WritableNativeMap params = new WritableNativeMap();
//    params.putString("screen", screen.getMarker());
//
//    sendEvent("android_auto:remove_screen", params);
//
//    carScreens.values().remove(screen);
//  }
//
//  private void sendEvent(String eventName, Object params) {
//    mReactContext
//      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
//      .emit(eventName, params);
//  }
//}