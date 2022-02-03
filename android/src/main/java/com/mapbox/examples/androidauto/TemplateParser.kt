package com.mapbox.examples.androidauto

import android.text.SpannableString
import android.text.Spanned
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.model.*
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import com.facebook.react.bridge.NoSuchKeyException
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableNativeMap
import android.graphics.BitmapFactory

import android.graphics.Bitmap




class TemplateParser(
    private val carContext: CarContext,
    private val mReactCarRenderContext: ReactCarRenderContext,
    private val mSetOnBackPressedCallback: (() -> Unit) -> Unit
) {

    companion object {
        const val TAG = "ReactAuto"
    }

    fun parseTemplate(renderMap: ReadableMap): Template {
        val type = renderMap.getString("type")
        return when (type) {
            "navigation-template" -> parseNavigationTemplate(renderMap)
            "list-template" -> parseListTemplateChildren(renderMap)
            "grid-template" -> parseGridTemplateChildren(renderMap)
            "place-list-map-template" -> parsePlaceListMapTemplate(renderMap)
            "pane-template" -> parsePaneTemplate(renderMap)
            else -> PaneTemplate.Builder(Pane.Builder().setLoading(true).build())
                .setTitle("Pane Template")
                .build()
        }
    }

    private fun parsePaneTemplate(map: ReadableMap): PaneTemplate {
        val paneBuilder = Pane.Builder()
        val children = map.getArray("children")
        val loading: Boolean
        loading = try {
            map.getBoolean("isLoading")
        } catch (e: NoSuchKeyException) {
            children == null || children.size() == 0
        }
        paneBuilder.setLoading(loading)
        if (!loading) {
            val actions: ArrayList<Action?> = ArrayList<Action?>()
            Log.d("ReactAUTO", "Found children: $children")
            for (i in 0 until children!!.size()) {
                val child = children.getMap(i)
                val type = child.getString("type")
                Log.d("ReactAUTO", "Adding child with type $type to row")
                if (type == "row") {
                    Log.d("ReactAUTO", "Parsing row")
                    paneBuilder.addRow(buildRow(child))
                } else if (type == "action") {
                    Log.d("ReactAUTO", "Parsing action")
                    actions.add(parseAction(child))
                } else {
                    Log.d("ReactAUTO", "Unknown type $type")
                }
            }
            if (actions.size > 0) {
                Log.d("ReactAUTO", "Setting actions to pane: $actions")
                paneBuilder.addAction(actions[0]!!)
            }
        }
        val builder = PaneTemplate.Builder(paneBuilder.build())
        val title = map.getString("title")
        if (title == null || title.length == 0) {
            builder.setTitle("<No Title>")
        } else {
            builder.setTitle(title)
        }
        try {
            builder.setHeaderAction(getHeaderAction(map.getString("headerAction"))!!)
        } catch (e: NoSuchKeyException) {
        }
        try {
            val actionStripMap = map.getMap("actionStrip")
            builder.setActionStrip(parseActionStrip(actionStripMap)!!)
            builder.setActionStrip(
                ActionStrip.Builder().addAction(Action.Builder().setTitle("More").build()).build()
            ).build()
        } catch (e: NoSuchKeyException) {
            Log.d("ReactAuto", "no such key $e")
        }
        return builder.build()
    }

    private fun parseActionStrip(map: ReadableMap?): ActionStrip? {
        val builder = ActionStrip.Builder()
        return if (map != null) {
            val actions = map.getArray("actions")
            for (i in 0 until actions?.size()!!) {
                val actionMap = actions?.getMap(i)
                val action = parseAction(actionMap)
                builder.addAction(action)
            }
            builder.build()
        } else {
            null
        }
    }

    private fun parseAction(map: ReadableMap?): Action {
        val builder = Action.Builder()
        if (map != null) {
            val title = map.getString("title")
            builder.setTitle(title!!)
            try {
                builder.setBackgroundColor(getColor(map.getString("backgroundColor")))
            } catch (e: NoSuchKeyException) {
                Log.d("ReactAUTO", "Couldn't set background color", e)
            }
            try {
                val onPress = map.getInt("onPress")
                if (title.lowercase() == "back") {
                    mSetOnBackPressedCallback {
                        Log.d("ReactAUTO", "invokeCallback(onPress)")

                        invokeCallback(onPress)
                    }
//          mSetOnBackPressedCallback.invoke(() -> {
//            invokeCallback(onPress);
//          });
                }
                builder.setOnClickListener { invokeCallback(onPress) }
            } catch (e: NoSuchKeyException) {
                Log.d("ReactAUTO", "Couldn't parseAction", e)
            }
        } else {
            Log.d("ReactAUTO", "No readable map supplied to parseAction")
        }
        return builder.build()
    }

    private fun getColor(colorName: String?): CarColor {
        return if (colorName != null) {
            when (colorName) {
                "blue" -> CarColor.BLUE
                "green" -> CarColor.GREEN
                "primary" -> CarColor.PRIMARY
                "red" -> CarColor.RED
                "secondary" -> CarColor.SECONDARY
                "yellow" -> CarColor.YELLOW
                "default" -> CarColor.DEFAULT
                else -> CarColor.DEFAULT
            }
        } else {
            CarColor.DEFAULT
        }
    }

    private fun parsePlaceListMapTemplate(map: ReadableMap): PlaceListMapTemplate {
        val builder = PlaceListMapTemplate.Builder()
        builder.setTitle(map.getString("title")!!)
        val children = map.getArray("children")
        try {
            builder.setHeaderAction(getHeaderAction(map.getString("headerAction"))!!)
        } catch (e: NoSuchKeyException) {
        }
        val loading: Boolean
        loading = try {
            map.getBoolean("isLoading")
        } catch (e: NoSuchKeyException) {
            children == null || children.size() == 0
        }
        Log.d("ReactAUTO", "Rendering " + if (loading) "Yes" else "No")
        builder.setLoading(loading)
        if (!loading) {
            val itemListBuilder = ItemList.Builder()
            for (i in 0 until children!!.size()) {
                val child = children.getMap(i)
                val type = child.getString("type")
                Log.d("ReactAUTO", "Adding $type to row")
                if (type == "row" || type == "place") {
                    itemListBuilder.addItem(buildRow(child))
                }
            }
            builder.setItemList(itemListBuilder.build())
        }
        try {
            val actionStripMap = map.getMap("actionStrip")
            builder.setActionStrip(parseActionStrip(actionStripMap)!!)
        } catch (e: NoSuchKeyException) {
        }
        return builder.build()
    }

    private fun parseNavigationTemplate(map: ReadableMap): NavigationTemplate {
        val builder = NavigationTemplate.Builder()
        try {
            val actionStripMap = map.getMap("actionStrip")
            builder.setActionStrip(parseActionStrip(actionStripMap)!!)
        } catch (e: NoSuchKeyException) {
            Log.d(
                TAG,
                "NavigationTemplate error $e"
            )
        }
        return builder.build()
    }

    private fun parseGridTemplateChildren(map: ReadableMap): GridTemplate {
        val children = map.getArray("children")
        val builder = GridTemplate.Builder()
        val loading: Boolean
        loading = try {
            map.getBoolean("isLoading")
        } catch (e: NoSuchKeyException) {
            children?.size() == 0
        }
        builder.setLoading(loading)
        if (!loading) {
            for (i in 0 until children?.size()!!) {
                val child = children?.getMap(i)
                val type = child?.getString("type")
                if (type == "item-list") {
                    builder.setSingleList(
                        parseGridItemListChildren(child)
                    )
                }
            }
        }
        try {
            builder.setHeaderAction(getHeaderAction(map.getString("headerAction"))!!)
        } catch (e: NoSuchKeyException) {
        }
        try {
            val actionStripMap = map.getMap("actionStrip")
            builder.setActionStrip(parseActionStrip(actionStripMap)!!)
        } catch (e: Exception) {
        }
        builder.setTitle(map.getString("title")!!)
        return builder.build()
    }

    private fun parseGridItemListChildren(itemList: ReadableMap?): ItemList {
        val builder = ItemList.Builder()
        if (itemList != null) {
            val children = itemList.getArray("children")
            for (i in 0 until children?.size()!!) {
                val child = children?.getMap(i)
                val type = child?.getString("type")
                if (type == "row") {
                    val gridItem = GridItem.Builder()
                    val title = child.getString("title")
                    if(title != null) gridItem.setTitle(title)
                    val text = child.getString("texts")
                    if(text != null) gridItem.setText(text)
                    val icon = child.getString("icon")
                    if(icon != null) {
                        val bitmap =
                            BitmapFactory.decodeResource(carContext.resources, carContext.resources.getIdentifier(icon, "drawable", carContext.packageName))
                        gridItem.setImage(CarIcon.Builder(IconCompat.createWithBitmap(bitmap)).build())
                    } else {
                        // Set default icon - mandatory to have icon
                        gridItem.setImage(
                            CarIcon.APP_ICON
                        )
                    }

                    val onPress = child.getInt("onPress")
                    gridItem.setOnClickListener {
                        invokeCallback(onPress)
                    }

                    builder.addItem(
                        gridItem.build()
                    )
                }
            }
        }
        try {
            // TODO - figure out what this is for
            // it currently throws a null error
            // builder.setNoItemsMessage(itemList.getString("noItemsMessage"));
            builder.setNoItemsMessage("No results")
        } catch (e: NoSuchKeyException) {
            Log.d("setNoItemsMessage", "error: $e")
        }
        return builder.build()
    }

    private fun parseListTemplateChildren(map: ReadableMap): ListTemplate {
        val children = map.getArray("children")
        val builder = ListTemplate.Builder()
        val loading: Boolean
        loading = try {
            map.getBoolean("isLoading")
        } catch (e: NoSuchKeyException) {
            children?.size() == 0
        }
        builder.setLoading(loading)
        if (!loading) {
            for (i in 0 until children?.size()!!) {
                val child = children?.getMap(i)
                val type = child?.getString("type")
                if (type == "item-list") {
                    builder.addSectionedList(
                        SectionedItemList.create(
                            parseItemListChildren(child),
                            child?.getString("header")!!
                        )
                    )
                }
            }
        }
        try {
            builder.setHeaderAction(getHeaderAction(map.getString("headerAction"))!!)
        } catch (e: NoSuchKeyException) {
        }
        try {
            val actionStripMap = map.getMap("actionStrip")
            builder.setActionStrip(parseActionStrip(actionStripMap)!!)
        } catch (e: Exception) {
        }
        builder.setTitle(map.getString("title")!!)
        return builder.build()
    }

    private fun parseItemListChildren(itemList: ReadableMap?): ItemList {
        val builder = ItemList.Builder()
        if (itemList != null) {
            val children = itemList.getArray("children")
            for (i in 0 until children?.size()!!) {
                val child = children?.getMap(i)
                val type = child?.getString("type")
                if (type == "row") {
                    builder.addItem(buildRow(child!!))
                }
            }
        }
        try {
            // TODO - figure out what this is for
            // it currently throws a null error
            // builder.setNoItemsMessage(itemList.getString("noItemsMessage"));
            builder.setNoItemsMessage("No results")
        } catch (e: NoSuchKeyException) {
            Log.d("setNoItemsMessage", "error: $e")
        }
        return builder.build()
    }

    private fun buildRow(rowRenderMap: ReadableMap): Row {
        val builder = Row.Builder()
        builder.setTitle(rowRenderMap.getString("title")!!)
        try {
            val texts = rowRenderMap.getArray("texts")
            var i = 0
            while (texts != null && i < texts.size()) {
                if (rowRenderMap.getString("type") == "place") {
                    val distanceKm = 1000
                    val description = SpannableString("   \u00b7 " + texts.getString(i))
                    description.setSpan(
                        DistanceSpan.create(
                            Distance.create(
                                distanceKm.toDouble(),
                                Distance.UNIT_MILES
                            )
                        ),
                        0,
                        1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    description.setSpan(
                        ForegroundCarColorSpan.create(CarColor.BLUE),
                        0,
                        1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.addText(description)
                } else {
                    builder.addText(texts.getString(i))
                }
                i++
            }
        } catch (e: NoSuchKeyException) {
        }
        try {
            val onPress = rowRenderMap.getInt("onPress")
            builder.setBrowsable(true)
            builder.setOnClickListener { invokeCallback(onPress) }
        } catch (e: NoSuchKeyException) {
        }
        try {
            builder.setMetadata(parseMetaData(rowRenderMap.getMap("metadata"))!!)
        } catch (e: NullPointerException) {
        }
        return builder.build()
    }

    private fun parseMetaData(map: ReadableMap?): Metadata? {
        return if (map != null) {
            when (map.getString("type")) {
                "place" -> Metadata.Builder().setPlace(
                    Place.Builder(
                        CarLocation.create(
                            map.getDouble("latitude"), map.getDouble("longitude")
                        )
                    ).setMarker(PlaceMarker.Builder().setColor(CarColor.BLUE).build()).build()
                ).build()
                else -> null
            }
        } else {
            null
        }
    }

    private fun getHeaderAction(actionName: String?): Action? {
        Log.d("ReactAUTO", "actionName $actionName")
        return if (actionName == null) {
            null
        } else {
            when (actionName) {
                "back" -> Action.BACK
                "app_icon" -> Action.APP_ICON
                else -> null
            }
        }
    }

    private fun invokeCallback(callbackId: Int, params: WritableNativeMap? = null) {
        var params = params
        if (params == null) {
            params = WritableNativeMap()
        }
        params.putInt("id", callbackId)
        params.putString("screen", mReactCarRenderContext.screenMarker)
        mReactCarRenderContext.eventCallback.invoke(params)
    }
}