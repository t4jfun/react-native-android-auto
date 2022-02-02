//package com.mapbox.examples.androidauto;
//
//import android.text.SpannableString;
//import android.text.Spanned;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import com.facebook.react.bridge.NoSuchKeyException;
//import com.facebook.react.bridge.ReadableArray;
//import com.facebook.react.bridge.ReadableMap;
//import com.facebook.react.bridge.WritableNativeMap;
//
//import androidx.car.app.model.Action;
//import androidx.car.app.model.ActionStrip;
//import androidx.car.app.model.CarColor;
//import androidx.car.app.model.Distance;
//import androidx.car.app.model.DistanceSpan;
//import androidx.car.app.model.ForegroundCarColorSpan;
//import androidx.car.app.model.ItemList;
//import androidx.car.app.model.CarLocation;
//import androidx.car.app.model.ListTemplate;
//import androidx.car.app.model.Metadata;
//import androidx.car.app.model.Pane;
//import androidx.car.app.model.PaneTemplate;
//import androidx.car.app.model.Place;
//import androidx.car.app.model.PlaceListMapTemplate;
//import androidx.car.app.model.SectionedItemList;
//import androidx.car.app.model.Row;
//import androidx.car.app.model.Template;
//import androidx.car.app.model.PlaceMarker;
//import androidx.car.app.navigation.model.NavigationTemplate;
//import androidx.car.app.navigation.model.RoutePreviewNavigationTemplate;
//
//import java.util.ArrayList;
//
//import kotlin.Unit;
//import kotlin.jvm.functions.Function0;
//
//public class TemplateParser {
//  static final String TAG = "ReactAuto";
//  private ReactCarRenderContext mReactCarRenderContext;
//  private Function0<Unit> mSetOnBackPressedCallback;
//
//  public TemplateParser(ReactCarRenderContext reactCarRenderContext, Function0<Unit> setOnBackPressedCallback) {
//    mReactCarRenderContext = reactCarRenderContext;
//    mSetOnBackPressedCallback = setOnBackPressedCallback;
//  }
//
//  public Template parseTemplate(ReadableMap renderMap) {
//    String type = renderMap.getString("type");
//
//    switch (type) {
//      case "navigation-template":
//        return parseNavigationTemplate(renderMap);
//      case "list-template":
//        return parseListTemplateChildren(renderMap);
//      case "place-list-map-template":
//        return parsePlaceListMapTemplate(renderMap);
//      case "pane-template":
//        return parsePaneTemplate(renderMap);
//      default:
//        return new PaneTemplate
//          .Builder(new Pane.Builder().setLoading(true).build())
//          .setTitle("Pane Template")
//          .build();
//    }
//  }
//
//  private PaneTemplate parsePaneTemplate(ReadableMap map) {
//    Pane.Builder paneBuilder = new Pane.Builder();
//
//    ReadableArray children = map.getArray("children");
//
//    boolean loading;
//
//    try {
//      loading = map.getBoolean("isLoading");
//    } catch (NoSuchKeyException e) {
//      loading = children == null || children.size() == 0;
//    }
//
//    paneBuilder.setLoading(loading);
//
//    if (!loading) {
//      ArrayList<Action> actions = new ArrayList();
//      Log.d("ReactAUTO", "Found children: " + children);
//
//      for (int i = 0; i < children.size(); i++) {
//        ReadableMap child = children.getMap(i);
//        String type = child.getString("type");
//        Log.d("ReactAUTO", "Adding child with type " + type + " to row");
//
//        if (type.equals("row")) {
//          Log.d("ReactAUTO", "Parsing row");
//          paneBuilder.addRow(buildRow(child));
//        } else if (type.equals("action")) {
//          Log.d("ReactAUTO", "Parsing action");
//          actions.add(parseAction(child));
//        } else {
//          Log.d("ReactAUTO", "Unknown type " + type);
//        }
//      }
//
//      if (actions.size() > 0) {
//        Log.d("ReactAUTO", "Setting actions to pane: " + actions);
//        paneBuilder.addAction(actions.get(0));
//      }
//    }
//
//    PaneTemplate.Builder builder = new PaneTemplate.Builder(paneBuilder.build());
//
//    String title = map.getString("title");
//    if (title == null || title.length() == 0) {
//      builder.setTitle("<No Title>");
//    } else {
//      builder.setTitle(title);
//    }
//
//    try {
//      builder.setHeaderAction(getHeaderAction(map.getString("headerAction")));
//    } catch (NoSuchKeyException e) {}
//
//    try {
//      ReadableMap actionStripMap = map.getMap("actionStrip");
//
//      builder.setActionStrip(parseActionStrip(actionStripMap));
//      builder.setActionStrip(new ActionStrip.Builder().addAction(new Action.Builder().setTitle("More").build()).build()).build();
//    } catch (NoSuchKeyException e) {
//      Log.d("ReactAuto", "no such key " + e);
//    }
//
//    return builder.build();
//  }
//
//  private ActionStrip parseActionStrip(ReadableMap map) {
//    ActionStrip.Builder builder = new ActionStrip.Builder();
//
//    if (map != null) {
//      ReadableArray actions = map.getArray("actions");
//
//      for (int i = 0; i < actions.size(); i++) {
//        ReadableMap actionMap = actions.getMap(i);
//        Action action = parseAction(actionMap);
//        builder.addAction(action);
//      }
//      return builder.build();
//    } else {
//      return null;
//    }
//  }
//
//  private Action parseAction(ReadableMap map) {
//    Action.Builder builder = new Action.Builder();
//
//    if (map != null) {
//      String title = map.getString("title");
//      builder.setTitle(title);
//      try {
//        builder.setBackgroundColor(getColor(map.getString("backgroundColor")));
//      } catch (NoSuchKeyException e) {
//        Log.d("ReactAUTO", "Couldn't set background color", e);
//      }
//
//      try {
//        int onPress = map.getInt("onPress");
//        if(title == "Back"){
////          mSetOnBackPressedCallback.invoke(() -> {
////            invokeCallback(onPress);
////          });
//        }
//        builder.setOnClickListener(
//          () -> {
//            invokeCallback(onPress);
//          }
//        );
//      } catch (NoSuchKeyException e) {
//        Log.d("ReactAUTO", "Couldn't parseAction", e);
//      }
//    } else {
//      Log.d("ReactAUTO", "No readable map supplied to parseAction");
//    }
//
//    return builder.build();
//  }
//
//  private CarColor getColor(String colorName) {
//    if (colorName != null) {
//      switch (colorName) {
//        case "blue":
//          return CarColor.BLUE;
//        case "green":
//          return CarColor.GREEN;
//        case "primary":
//          return CarColor.PRIMARY;
//        case "red":
//          return CarColor.RED;
//        case "secondary":
//          return CarColor.SECONDARY;
//        case "yellow":
//          return CarColor.YELLOW;
//        default:
//        case "default":
//          return CarColor.DEFAULT;
//      }
//    } else {
//      return CarColor.DEFAULT;
//    }
//  }
//
//  private PlaceListMapTemplate parsePlaceListMapTemplate(ReadableMap map) {
//    PlaceListMapTemplate.Builder builder = new PlaceListMapTemplate.Builder();
//
//    builder.setTitle(map.getString("title"));
//    ReadableArray children = map.getArray("children");
//
//    try {
//      builder.setHeaderAction(getHeaderAction(map.getString("headerAction")));
//    } catch (NoSuchKeyException e) {}
//
//    boolean loading;
//
//    try {
//      loading = map.getBoolean("isLoading");
//    } catch (NoSuchKeyException e) {
//      loading = children == null || children.size() == 0;
//    }
//
//    Log.d("ReactAUTO", "Rendering " + (loading ? "Yes" : "No"));
//    builder.setLoading(loading);
//
//    if (!loading) {
//      ItemList.Builder itemListBuilder = new ItemList.Builder();
//
//      for (int i = 0; i < children.size(); i++) {
//        ReadableMap child = children.getMap(i);
//        String type = child.getString("type");
//        Log.d("ReactAUTO", "Adding " + type + " to row");
//
//        if (type.equals("row") || type.equals("place")) {
//          itemListBuilder.addItem(buildRow(child));
//        }
//      }
//
//      builder.setItemList(itemListBuilder.build());
//    }
//
//    try {
//      ReadableMap actionStripMap = map.getMap("actionStrip");
//      builder.setActionStrip(parseActionStrip(actionStripMap));
//    } catch (NoSuchKeyException e) {}
//
//    return builder.build();
//  }
//
//  private RoutePreviewNavigationTemplate parsePreviewMapTemplate(ReadableMap map) {
//    RoutePreviewNavigationTemplate.Builder builder = new RoutePreviewNavigationTemplate.Builder();
//
//    builder.setTitle("RoutePreview Title");
//    ReadableArray children = map.getArray("children");
//
//    try {
//      builder.setHeaderAction(getHeaderAction(map.getString("headerAction")));
//    } catch (NoSuchKeyException e) {}
//
//    boolean loading;
//
//    try {
//      loading = map.getBoolean("isLoading");
//    } catch (NoSuchKeyException e) {
//      loading = children == null || children.size() == 0;
//    }
//
//    Log.d("ReactAUTO", "Rendering " + (loading ? "Yes" : "No"));
//    builder.setLoading(loading);
//
//    if (!loading) {
//      ItemList.Builder itemListBuilder = new ItemList.Builder();
//
//      for (int i = 0; i < children.size(); i++) {
//        ReadableMap child = children.getMap(i);
//        String type = child.getString("type");
//        Log.d("ReactAUTO", "Adding " + type + " to row");
//
//        if (type.equals("row") || type.equals("place")) {
//          itemListBuilder.addItem(buildRow(child));
//        }
//      }
//
//      builder.setItemList(itemListBuilder.build());
//    }
//
//    try {
//      ReadableMap actionStripMap = map.getMap("actionStrip");
//      builder.setActionStrip(parseActionStrip(actionStripMap));
//    } catch (NoSuchKeyException e) {}
//
//    return builder.build();
//  }
//
//  private NavigationTemplate parseNavigationTemplate(ReadableMap map) {
//    
//      NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
//
//      try {
//          ReadableMap actionStripMap = map.getMap("actionStrip");
//          builder.setActionStrip(parseActionStrip(actionStripMap));
//      } catch (NoSuchKeyException e) {
//          Log.d(TAG, "NavigationTemplate error " +  e);
//      }
//
//      return builder.build();
//  };
//
//  private ListTemplate parseListTemplateChildren(ReadableMap map) {
//    ReadableArray children = map.getArray("children");
//
//    ListTemplate.Builder builder = new ListTemplate.Builder();
//
//    boolean loading;
//
//    try {
//      loading = map.getBoolean("isLoading");
//    } catch (NoSuchKeyException e) {
//      loading = children.size() == 0;
//    }
//
//    builder.setLoading(loading);
//
//    if (!loading) {
//      for (int i = 0; i < children.size(); i++) {
//        ReadableMap child = children.getMap(i);
//        String type = child.getString("type");
//        if (type.equals("item-list")) {
//          builder.addSectionedList(SectionedItemList.create(parseItemListChildren(child), child.getString("header")));
//        }
//      }
//    }
//
//    try {
//      builder.setHeaderAction(getHeaderAction(map.getString("headerAction")));
//    } catch (NoSuchKeyException e) {}
//
//    try {
//      ReadableMap actionStripMap = map.getMap("actionStrip");
//      builder.setActionStrip(parseActionStrip(actionStripMap));
//    } catch (NoSuchKeyException e) {}
//
//    builder.setTitle(map.getString("title"));
//
//    return builder.build();
//  }
//
//  private ItemList parseItemListChildren(ReadableMap itemList) {
//    ItemList.Builder builder = new ItemList.Builder();
//
//    if (itemList != null) {
//      ReadableArray children = itemList.getArray("children");
//      for (int i = 0; i < children.size(); i++) {
//        ReadableMap child = children.getMap(i);
//        String type = child.getString("type");
//        if (type.equals("row") ) {
//          builder.addItem(buildRow(child));
//        }
//      }
//    }
//
//    try {
//      // TODO - figure out what this is for
//      // it currently throws a null error
//      // builder.setNoItemsMessage(itemList.getString("noItemsMessage"));
//      builder.setNoItemsMessage("No results");
//    } catch (NoSuchKeyException e) {
//      Log.d("setNoItemsMessage", "error: " + e);
//    }
//
//    return builder.build();
//  }
//
//  @NonNull
//  private Row buildRow(ReadableMap rowRenderMap) {
//    Row.Builder builder = new Row.Builder();
//
//    builder.setTitle(rowRenderMap.getString("title"));
//
//    try {
//      ReadableArray texts = rowRenderMap.getArray("texts");
//
//      for (int i = 0; texts != null && i < texts.size(); i++) {
//         if (rowRenderMap.getString("type").equals("place")) {
//          int distanceKm = 1000;
//          SpannableString description = new SpannableString("   \u00b7 " + texts.getString(i));
//          description.setSpan(
//                  DistanceSpan.create(Distance.create(distanceKm, Distance.UNIT_MILES)),
//                  0,
//                  1,
//                  Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//          description.setSpan(
//                  ForegroundCarColorSpan.create(CarColor.BLUE), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
//          builder.addText(description);
//        } else {
//          builder.addText(texts.getString(i));
//        }
//      }
//    } catch (NoSuchKeyException e) {}
//
//    try {
//      int onPress = rowRenderMap.getInt("onPress");
//
//      builder.setBrowsable(true);
//
//      builder.setOnClickListener(
//        () -> {
//          invokeCallback(onPress);
//        }
//      );
//    } catch (NoSuchKeyException e) {}
//
//    try {
//      builder.setMetadata(parseMetaData(rowRenderMap.getMap("metadata")));
//    } catch (NoSuchKeyException e) {}
//
//    return builder.build();
//  }
//
//
//  private Metadata parseMetaData(ReadableMap map) {
//    if (map != null) {
//      switch (map.getString("type")) {
//        case "place":
//          return new Metadata.Builder(
//
//          ).setPlace(
//                  new Place.Builder(
//                          CarLocation.create(
//                                  map.getDouble("latitude"), map.getDouble("longitude")
//                          )
//                  ).setMarker(new PlaceMarker.Builder().setColor(CarColor.BLUE).build()).build()
//          ).build();
//
//        default:
//          return null;
//      }
//    } else {
//      return null;
//    }
//  }
//
//  private Action getHeaderAction(String actionName) {
//    Log.d("ReactAUTO", "actionName " + actionName);
//    if (actionName == null) {
//      return null;
//    } else {
//      switch (actionName) {
//        case "back":
//          return Action.BACK;
//        case "app_icon":
//          return Action.APP_ICON;
//        default:
//          return null;
//      }
//    }
//  }
//
//  private void invokeCallback(int callbackId) {
//    invokeCallback(callbackId, null);
//  }
//
//  private void invokeCallback(int callbackId, WritableNativeMap params) {
//    if (params == null) {
//      params = new WritableNativeMap();
//    }
//
//    params.putInt("id", callbackId);
//    params.putString("screen", mReactCarRenderContext.getScreenMarker());
//
//    mReactCarRenderContext.getEventCallback().invoke(params);
//  }
//}