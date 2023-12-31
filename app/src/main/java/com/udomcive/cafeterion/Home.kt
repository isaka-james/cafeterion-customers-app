package com.udomcive.cafeterion

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import com.google.gson.Gson
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.gson.reflect.TypeToken
import org.json.JSONArray

import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast


import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONException


import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


class Home : AppCompatActivity() {

    // the splash for fetching datas
    private val SPLASH_DELAY: Long = 3000


    // handles the onback press
    private var doubleBackToExitPressedOnce = false
    private val doubleBackToExitToastDuration = 2000 // 2 seconds


    private var jsonTrending: String? = null
    private var jsonRecently: String? = null
    private var jsonCategories: String? = null
    private var jsonOrders: String? = null
    private var jsonUser: String? = null





    /*
    *
    *
    *   FETCHING THE DATA FROM THE API HERE
    *
    *
    */

    //private var selectedTime: String = ""
    //private val configureTimeButton: Button







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        // Use a Handler to switch to the splash screen layout after a delay
        // Use a Handler to switch to the splash screen layout after a delay
        Handler(Looper.getMainLooper()).postDelayed({
            setContentView(R.layout.splash_screen)





            // Start a coroutine to fetch data asynchronously
            CoroutineScope(Dispatchers.Main).launch {

                GlobalScope.launch(Dispatchers.IO) {
                    val isConnected = testInternet()

                    withContext(Dispatchers.Main) {
                        if (!isConnected) {
                            goOfflinePage()
                        }else{
                            val deferredTrend = async(Dispatchers.IO) { fetchData("trending","normal") }
                            val deferredRecent = async(Dispatchers.IO) { fetchData("recent","normal") }
                            val deferredCategory = async(Dispatchers.IO) { fetchData("category","normal") }
                            val deferredUser = async(Dispatchers.IO) { fetchData("user","normal") }


                            // Wait for all data fetching tasks to complete
                            jsonTrending = deferredTrend.await()
                            jsonRecently = deferredRecent.await()
                            jsonCategories = deferredCategory.await()
                            jsonUser = deferredUser.await()

                            // Now you have the fetched data, you can use it as needed
                            // For example, you can update UI elements with this data
                            // ...

                            // Switch to the main activity layout on the UI thread
                            HomePage()

                        }
                    }
                }

            }
        }, SPLASH_DELAY)










    }



    /* The side navigation start functions */

    // share function
    private fun shareApp() {
        val appLink = "https://cafeterion.000webhostapp.com/app.php" // The link of the app
        val message = "Check out this awesome ordering  app: $appLink"

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, message)

        startActivity(Intent.createChooser(intent, "Share the App"))

    }






    private fun parseJsonToProductList(json: String): List<Product> {
        val gson = Gson()
        val itemType = object : TypeToken<List<Product>>() {}.type
        return gson.fromJson(json, itemType)
    }



    private fun parseOrdersFromJSON(): List<Order> {
        val orders = mutableListOf<Order>()

        try {
            val jsonArray = JSONArray(jsonOrders)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val orderId = jsonObject.getString("orderId") // Parse orderId
                val imageResource = jsonObject.getString("imageResource")
                val restaurantName = jsonObject.getString("restaurantName")
                val orderDate = jsonObject.getString("orderDate")
                val orderStatus = jsonObject.getString("orderStatus")
                val deliveryTime = jsonObject.getString("deliveryTime")

                val order = Order(orderId, imageResource, restaurantName, orderDate, orderStatus,deliveryTime)
                orders.add(order)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return orders
    }


    private fun orders(outerLayout: View) {


        CoroutineScope(Dispatchers.Main).launch {

            GlobalScope.launch(Dispatchers.IO) {
                val isConnected = testInternet()

                withContext(Dispatchers.Main) {
                    if (!isConnected) {
                        goOfflinePage()
                    }else{
                        val deferredOrder = async(Dispatchers.IO) { fetchData("orders","normal") }

                        // Wait for all data fetching tasks to complete
                        jsonOrders = deferredOrder.await()

                        // Now you have the fetched data, you can use it as needed
                        val outerLinearLayout = outerLayout.findViewById<LinearLayout>(R.id.outerLinearLayout)

                        // Get the list of orders (you can use your JSON parsing logic here)
                        val orders = parseOrdersFromJSON()

                        // Iterate through the orders and create order items dynamically
                        for (order in orders) {
                            // Inflate the order item layout
                            val orderItemLayout = layoutInflater.inflate(R.layout.order_card, null)

                            // Find views inside the order item layout and set their values
                            val orderImageView = orderItemLayout.findViewById<ImageView>(R.id.orderImageView)
                            val restaurantNameTextView = orderItemLayout.findViewById<TextView>(R.id.restaurantNameTextView)
                            val orderDateTextView = orderItemLayout.findViewById<TextView>(R.id.orderDateTextView)
                            val orderStatusTextView = orderItemLayout.findViewById<TextView>(R.id.orderStatusTextView)
                            val deliveryTimeTextView = orderItemLayout.findViewById<TextView>(R.id.orderDeliveryTime)


                            // val cancelButton = orderItemLayout.findViewById<Button>(R.id.cancelButton)

                            // Load image using Glide (replace with your image loading logic)
                            Glide.with(this@Home)
                                .load(order.imageResource) // Use the appropriate image resource
                                .placeholder(R.drawable.logo_login) // Replace with your placeholder image
                                .error(R.drawable.error_image) // Replace with your error image
                                .into(orderImageView)


                            restaurantNameTextView.text = order.restaurantName
                            //restaurantNameTextView.typeface = Typeface.createFromAsset(assets, "caveat_bush.ttf")
                            orderDateTextView.text = "Time Ordered: ${order.orderDate}"
                            orderStatusTextView.text = "Status: ${order.orderStatus}"
                            deliveryTimeTextView.text = "Time of Delivery: ${order.deliveryTime}"

                            // Set the ID of the order item based on the orderId from JSON
                            orderItemLayout.id = order.orderId.hashCode()

                            // Add the order item to the outer LinearLayout
                            outerLinearLayout.addView(orderItemLayout)

                            // ADD LATER THE CANCEL BUTTON FOR CANCELLING ORDERS


                        }

                    }
                }
            }

        }


    }



    private fun feedback(){
        val frameLayout = findViewById<FrameLayout>(R.id.container)
        frameLayout.removeAllViews()

        // Inflate the XML layout (a.xml)
        val inflater = LayoutInflater.from(this)
        val aLayout = inflater.inflate(R.layout.feedback, null)

        // Add the inflated layout to the FrameLayout
        frameLayout.addView(aLayout)


    }


    private fun account() {
        /**********************************************************************
         *        X X X X X X X X X X X X X X X X X X X X
         *        X HANDLING THE ACCOUNT HERE  X X
         *        X X X X X X X X X X X X X X X X X X X
         */
        val gson = Gson()
        val user = gson.fromJson(jsonUser, User::class.java)

        val frameLayout = findViewById<FrameLayout>(R.id.container)
        frameLayout.removeAllViews()

        // Inflate the XML layout (activity_account.xml)
        val inflater = LayoutInflater.from(this)
        val aLayout = inflater.inflate(R.layout.activity_account, null)

        // Find TextViews and set user information
        val usernameTextView = aLayout.findViewById<TextView>(R.id.usernameTextView)
        val emailTextView = aLayout.findViewById<TextView>(R.id.emailTextView)
        val phoneTextView = aLayout.findViewById<TextView>(R.id.phoneTextView)
        val locationTextView = aLayout.findViewById<TextView>(R.id.locationTextView)

        usernameTextView.text = user.username
        emailTextView.text = user.email
        phoneTextView.text = user.phone
        locationTextView.text = "Location: " + user.location

        // Add the inflated layout to the FrameLayout
        frameLayout.addView(aLayout)

        // Now, the contents of a.xml are added to the container
    }




    private fun createCardViewCategory(category: Category): CardView {
        val cardView = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.card_width_category), // 150dp width
            resources.getDimensionPixelSize(R.dimen.card_height_category) // 200dp height
        )
        layoutParams.setMargins(10, 10, 10, 10)
        cardView.layoutParams = layoutParams

        cardView.radius = resources.getDimension(R.dimen.card_corner_radius_categories)

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.VERTICAL

        val imageView = ImageView(this)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT , // 150dp width
            resources.getDimensionPixelSize(R.dimen.image_height_category) // 150dp height
        )
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        Glide.with(this)
            .load(category.image_url)
            .placeholder(R.drawable.logo_login)
            .into(imageView)

        val titleTextView = TextView(this)
        titleTextView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        titleTextView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER
        titleTextView.textSize = resources.getDimension(R.dimen.title_text_size_category)
        titleTextView.typeface =  Typeface.createFromAsset(assets, "caveat_brush.ttf")
        titleTextView.setPadding(8, 8, 8, 8)
        titleTextView.text = category.title


        linearLayout.addView(imageView)
        linearLayout.addView(titleTextView)
        cardView.addView(linearLayout)

        cardView.id = category.id.hashCode()

        cardView.setOnClickListener {
            onCardViewClick(category)
        }

        return cardView
    }


    private fun onCardViewClick(category: Category) {
        CoroutineScope(Dispatchers.Main).launch {

            GlobalScope.launch(Dispatchers.IO) {
                val isConnected = testInternet()

                withContext(Dispatchers.Main) {
                    if (!isConnected) {
                        goOfflinePage()
                    }else{
                        val outerCatLayout = LayoutInflater.from(this@Home).inflate(R.layout.outer_cover_orders, null)


                        // Find the FrameLayout in your activity_home.xml where you want to add the outer LinearLayout
                        val frameLayout = findViewById<FrameLayout>(R.id.container)
                        frameLayout.removeAllViews()


                        // Add the outer LinearLayout to the FrameLayout
                        frameLayout.addView(outerCatLayout)

                        val categoryString = category.id.toString()
                        val deferredCat = async(Dispatchers.IO) { fetchData(categoryString,"category") }

                        // Wait for all data fetching tasks to complete
                        val jsonCat = deferredCat.await()

                        // Now you have the fetched data, you can use it as needed
                        val gson = Gson()


                        // Get the list of orders (you can use your JSON parsing logic here)
                        val categoriEs = gson.fromJson(jsonCat, Array<SingleProduct>::class.java)

                        /*
                        * now we have all the data we need
                        *
                        */


                        val outerLinearLayout = outerCatLayout.findViewById<LinearLayout>(R.id.outerLinearLayout)


                        categoriEs.forEach { trending ->
                            // Create a CardView
                            val cardView = createCardViewTrending(trending)
                            outerLinearLayout.addView(cardView)
                        }


                    }
                }
            }

        }

    }














    private fun createCardViewTrending(trending: SingleProduct): CardView {
        val cardView = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(10, 10, 10, 10)
        cardView.layoutParams = layoutParams

        val cardContent = layoutInflater.inflate(R.layout.trending_foods, null)

        val imageView = cardContent.findViewById<ImageView>(R.id.image_view_trending)
        val titleTextView = cardContent.findViewById<TextView>(R.id.titleTextViewTrending)
        val descriptionTextView = cardContent.findViewById<TextView>(R.id.descriptionTextViewTrending)
        val priceTextView = cardContent.findViewById<TextView>(R.id.priceTextViewTrending)
        val orderButton = cardContent.findViewById<MaterialButton>(R.id.buyItem1)
        val timeText = cardContent.findViewById<TextView>(R.id.delivering_time)

        // Load image using Glide (replace with your image loading logic)
        Glide.with(this)
            .load(trending.image_url) // Use the appropriate image resource
            .placeholder(R.drawable.logo_login) // Replace with your placeholder image
            .error(R.drawable.error_image) // Replace with your error image
            .into(imageView)

        // Populate views with order data
        titleTextView.text = "${trending.name} "
        descriptionTextView.text = "${trending.from} - ${trending.supplier}"
        descriptionTextView.textSize = resources.getDimension(R.dimen.description_text)
        descriptionTextView.typeface= Typeface.createFromAsset(assets, "dancing_script.ttf")
        timeText.text= "deliver starts ${trending.start}:00 to ${trending.end}:00"
        timeText.typeface= Typeface.createFromAsset(assets, "dancing_script.ttf")
        priceTextView.text = "Tsh. ${trending.price} /="

        // Set click listener for the order button
        orderButton.setOnClickListener {
            // Handle the order button click here

            showBottomSheet(trending)

        }

        // Set the cardView's id using the "id" from the JSON
        cardView.id = trending.id.hashCode()

        cardView.addView(cardContent)

        return cardView
    }

    private fun createCardViewCat(trending: SingleProduct): CardView {
        val cardView = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(10, 10, 10, 10)
        cardView.layoutParams = layoutParams

        val cardContent = layoutInflater.inflate(R.layout.trending_foods, null)

        val imageView = cardContent.findViewById<ImageView>(R.id.image_view_trending)
        val titleTextView = cardContent.findViewById<TextView>(R.id.titleTextViewTrending)
        val descriptionTextView = cardContent.findViewById<TextView>(R.id.descriptionTextViewTrending)
        val priceTextView = cardContent.findViewById<TextView>(R.id.priceTextViewTrending)
        val orderButton = cardContent.findViewById<MaterialButton>(R.id.buyItem1)
        val timeText = cardContent.findViewById<TextView>(R.id.delivering_time)

        // Load image using Glide (replace with your image loading logic)
        Glide.with(this)
            .load(trending.image_url) // Use the appropriate image resource
            .placeholder(R.drawable.logo_login) // Replace with your placeholder image
            .error(R.drawable.error_image) // Replace with your error image
            .into(imageView)

        // Populate views with order data
        titleTextView.text = "${trending.name} "
        descriptionTextView.text = "${trending.from} - ${trending.supplier}"
        descriptionTextView.textSize = resources.getDimension(R.dimen.description_text)
        descriptionTextView.typeface= Typeface.createFromAsset(assets, "dancing_script.ttf")
        timeText.text= "deliver starts ${trending.start}:00 to ${trending.end}:00"
        timeText.typeface= Typeface.createFromAsset(assets, "dancing_script.ttf")
        priceTextView.text = "Tsh. ${trending.price} /="

        // Set click listener for the order button
        orderButton.setOnClickListener {
            // Handle the order button click here

            showBottomSheet(trending)

        }

        // Set the cardView's id using the "id" from the JSON
        cardView.id = trending.id.hashCode()

        cardView.addView(cardContent)

        return cardView
    }














    private fun createCardViewRecently(recently: SingleProduct): CardView {
        val cardView = CardView(this)
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(10, 10, 10, 10)
        cardView.layoutParams = layoutParams

        val cardContent = layoutInflater.inflate(R.layout.recently_foods, null)

        val imageView = cardContent.findViewById<ImageView>(R.id.image_view_recently)
        val titleTextView = cardContent.findViewById<TextView>(R.id.titleTextViewRecently)
        val descriptionTextView = cardContent.findViewById<TextView>(R.id.descriptionTextViewRecently)
        val priceTextView = cardContent.findViewById<TextView>(R.id.priceTextViewRecently)
        val orderButton = cardContent.findViewById<MaterialButton>(R.id.buyItem3)
        val timeText = cardContent.findViewById<TextView>(R.id.delivering_time)

        // Load image using Glide (replace with your image loading logic)
        Glide.with(this)
            .load(recently.image_url) // Use the appropriate image resource
            .placeholder(R.drawable.logo_login) // Replace with your placeholder image
            .error(R.drawable.error_image) // Replace with your error image
            .into(imageView)

        // Populate views with order data
        titleTextView.text = "${recently.name} "
        descriptionTextView.text = "${recently.from} - ${recently.supplier}"
        descriptionTextView.textSize = resources.getDimension(R.dimen.description_text)
        descriptionTextView.typeface= Typeface.createFromAsset(assets, "dancing_script.ttf")
        timeText.text= "deliver starts ${recently.start}:00 to ${recently.end}:00"
        timeText.typeface= Typeface.createFromAsset(assets, "dancing_script.ttf")
        priceTextView.text = "Tsh. ${recently.price} /="

        // Set click listener for the order button
        orderButton.setOnClickListener {
            // Handle the order button click here

            showBottomSheet(recently)


        }

        // Set the cardView's id using the "id" from the JSON
        cardView.id = recently.id.hashCode()

        cardView.addView(cardContent)

        return cardView
    }













    private fun showBottomSheet(product: SingleProduct) {
        val bottomSheetDialog = BottomSheetDialog(this)

        // Inflate the bottom sheet layout
        val bottomSheetView = layoutInflater.inflate(R.layout.product_bottom_sheet_layout, null)

        // Find views in the bottom sheet layout
        val productImageView = bottomSheetView.findViewById<ImageView>(R.id.productImageView)
        val productNameTextView = bottomSheetView.findViewById<TextView>(R.id.productNameTextView)
        val productDescriptionTextView = bottomSheetView.findViewById<TextView>(R.id.productDescriptionTextView)
        val productPriceTextView = bottomSheetView.findViewById<TextView>(R.id.productPriceTextView)
        val deliveryTime = bottomSheetView.findViewById<TextView>(R.id.delivering_time)
        val placeOrderButton = bottomSheetView.findViewById<Button>(R.id.placeOrderButton)

        // Load product image using Glide (replace with your image loading logic)
        Glide.with(this)
            .load(product.image_url) // Replace with the appropriate image URL
            .placeholder(R.drawable.logo_login) // Replace with a placeholder image
            .error(R.drawable.error_image) // Replace with an error image
            .into(productImageView)

        // Populate views with product data
        productNameTextView.text = product.name
        productNameTextView.typeface = Typeface.createFromAsset(assets, "caveat_brush.ttf")
        productDescriptionTextView.text = product.description
        productDescriptionTextView.typeface = Typeface.createFromAsset(assets, "dancing_script.ttf")
        productPriceTextView.text = "Tsh."+product.price+" /="
        deliveryTime.text = "(Default) Time Of Delivery: ${product.start}:00"
        //producttime.typeface = Typeface.createFromAsset(assets, "pacifico_brush.ttf")

        var selectedTime = product.start

        var selectedHour: Int? = product.start.toInt()
        var selectedMinute: Int? = 0
        var showMinutes: String = ""

        val configureTimeButton = bottomSheetView.findViewById<Button>(R.id.configureTimeButton)
        configureTimeButton.setOnClickListener{

            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(12) // Initial hour
                .setMinute(0) // Initial minute
                .setTitleText("Select Time")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                selectedHour = timePicker.hour
                selectedMinute = timePicker.minute

                selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

                /* Check if the selected time is within your desired range
                if (selectedHour >= 9 && selectedHour <= 21) {
                    // The selected time is within the range (9:00 AM to 9:00 PM)
                    // Handle the selected time
                } else {
                    // The selected time is outside the allowed range
                    // You can display an error message or take appropriate action
                }

                 */
                deliveryTime.text = "(Custom) Time Of Delivery: ${selectedTime}"
            }

            timePicker.show(supportFragmentManager, "timePicker")


        }


        // Set click listener for the "Place Order" button
        placeOrderButton.setOnClickListener {
            // Handle the "Place Order" button click here
            if(selectedHour!! >= product.start.toInt() && selectedHour!! <= product.end.toInt() ){
                if(selectedMinute!! < 10){
                    showMinutes = "0$selectedMinute"
                }


                // here check the internet first!!


                // make sure all the variables are accessible


                val userId = getUserId()
                val productId = product.id
                val editTextComment = findViewById<TextInputEditText>(R.id.EditTextComment)
                val editTextLocation = findViewById<TextInputEditText>(R.id.EditTextLocation)

                // Check if EditText elements are not null before accessing their text property
                val comment = editTextComment?.text?.toString() ?: ""
                val location = editTextLocation?.text?.toString() ?: ""

                val hourDelivery = selectedHour
                val minuteDelivery = selectedMinute


                CoroutineScope(Dispatchers.Main).launch {

                    GlobalScope.launch(Dispatchers.IO) {
                        val isConnected = sendOrderToServer(
                            userId,
                            productId,
                            comment,
                            location,
                            hourDelivery,
                            minuteDelivery
                        )

                        withContext(Dispatchers.Main) {
                            if (!isConnected) {
                                Toast.makeText(this@Home, "It seems You're offline, Check your network!", Toast.LENGTH_LONG).show()
                            }else{
                                // start sending the data to the server
                                        Toast.makeText(
                                            this@Home,
                                            "Order Placed.. ( Delivery at $selectedHour:$showMinutes )",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        bottomSheetDialog.dismiss()
                            }
                        }
                    }

                }




            }else{
                Toast.makeText(this, "Time You Selected is out of Delivery Valid Delivery Time ( ${product.start}:00 - ${product.end}:00 )", Toast.LENGTH_LONG).show()
            }

        }

        // Set the content view of the bottom sheet
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }














    private fun testInternet(): Boolean {
        val clientAuth = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val url = "https://cafeterion.000webhostapp.com/trend.php"

        val requestAuth = Request.Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/json"), ""))
            .build()

        try {
            val responseAuth = clientAuth.newCall(requestAuth).execute()

            return when {
                responseAuth.isSuccessful -> {
                    true
                }
                else -> false
            }
        } catch (e: IOException) {
            val userid = getUserId()
            Log.e("Bad Internet", "Error: ${e.message} User = $userid")
            return false
        }
    }


    private fun goOfflinePage(){
        setContentView(R.layout.no_internet)
        val retry = findViewById<Button>(R.id.retryButton)
        val loadingAnime = findViewById<ImageView>(R.id.noInternetPic)
        val message = findViewById<TextView>(R.id.usernameTextView)

        retry.setOnClickListener{

            message.text = "loading .."
            val rotateAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_animation)
            loadingAnime.startAnimation(rotateAnimation)


            CoroutineScope(Dispatchers.Main).launch {

                GlobalScope.launch(Dispatchers.IO) {
                    val isConnected = testInternet()

                    withContext(Dispatchers.Main) {
                        if (!isConnected) {
                            goOfflinePage()
                        }else{
                            val deferredTrend = async(Dispatchers.IO) { fetchData("trending","normal") }
                            val deferredRecent = async(Dispatchers.IO) { fetchData("recent","normal") }
                            val deferredCategory = async(Dispatchers.IO) { fetchData("category","normal") }
                            val deferredUser = async(Dispatchers.IO) { fetchData("user","normal")}

                            // Wait for all data fetching tasks to complete
                            jsonTrending = deferredTrend.await()
                            jsonRecently = deferredRecent.await()
                            jsonCategories = deferredCategory.await()
                            jsonUser = deferredUser.await()

                            // Now you have the fetched data, you can use it as needed
                            // For example, you can update UI elements with this data
                            // ...

                            // Switch to the main activity layout on the UI thread
                            HomePage()

                        }
                    }
                }

            }

        }

    }





    private suspend fun fetchData(type: String,which: String): String {

        val userId = getUserId()

        val client = OkHttpClient()

        var url: String? = null

        if(which == "normal"){
            url = when (type) {
                "trending" -> "https://cafeterion.000webhostapp.com/trend.php"
                "recent" -> "https://cafeterion.000webhostapp.com/recent.php"
                "category" -> "https://cafeterion.000webhostapp.com/category.php"
                "orders" -> "https://cafeterion.000webhostapp.com/orders.php?userid=$userId"
                "user" -> "https://cafeterion.000webhostapp.com/user_info.php?userid=$userId"
                else -> ""
            }
        }else if(which == "search"){
            val search = URLEncoder.encode(type, "UTF-8")
            url =  "https://cafeterion.000webhostapp.com/search.php?query=$search"

        }else if(which=="category"){
            Log.i("Category Fetching","We are fetching the category with id=$type")
            url =  "https://cafeterion.000webhostapp.com/cat.php?id=$type"
        }


        val jsonBody = """
        {
            "type": "$type",
            "key": "mynameismasterplancafeterion"
        }
    """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/json"), jsonBody))
            .build()

        val response = client.newCall(request).execute()

        return if (response.isSuccessful) {
            val jsonData = response.body()?.string().toString()
            Log.i("Data received: ", "Data for $type fetched successfully")
            jsonData // Return the fetched data as a non-nullable String
        } else {
            Log.e("Data not received: ", "Failed to fetch data for $type")
            "" // Return an empty string in case of failure, but it should be a valid String
        }
    }



    private fun sendOrderToServer(user: Int, id: String, comment: String,location: String,hour: Int?, minute: Int?): Boolean{
        // now it is time of sending the data to the server


        val clientAuth = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val url =  "https://cafeterion.000webhostapp.com/make_orders.php?userid=$user&hour=$hour&minutes=$minute&menuid=$id&comment=${URLEncoder.encode(comment, "UTF-8")}&location=${URLEncoder.encode(location, "UTF-8")}"

        val requestAuth = Request.Builder()
            .url(url)
            .post(RequestBody.create(MediaType.parse("application/json"), ""))
            .build()

        try {
            val responseAuth = clientAuth.newCall(requestAuth).execute()

            return when {
                responseAuth.isSuccessful -> {
                    Log.i("Order Placed: ", "Client placed the order successfully!")
                    true
                }
                else -> false
            }
        } catch (e: IOException) {
            val userid = getUserId()
            Log.e("Bad Internet", "Error: ${e.message} User = $userid")
            return false
        }

    }







    private fun HomePage() {
        setContentView(R.layout.activity_home)
        // Find the LinearLayout for them
        val trendingFoodsLayout = findViewById<LinearLayout>(R.id.trending_foods)
        val recentlyFoodsLayout = findViewById<LinearLayout>(R.id.recently_foods)
        val categoriesFoodLayout = findViewById<LinearLayout>(R.id.horizontal_layout_categories)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        val headerView = navigationView.getHeaderView(0)
        val usernameOfUser = headerView.findViewById<TextView>(R.id.usernameCurrentUser)
        val phoneOfUser = headerView.findViewById<TextView>(R.id.phoneCurrentUser)

        val gson = Gson()

        /*
         *   THE
         *   CATEGORIES
         */
        // Parse the JSON data into an array of Category objects
        val categories = gson.fromJson(jsonCategories, Array<Category>::class.java)

        // Create and add CardViews for each Category
        categories.forEach { category ->
            // Create a CardView
            val cardView = createCardViewCategory(category)
            categoriesFoodLayout.addView(cardView)
        }

        /*
         *   THE
         *   TRENDING
         */
        // Parse the JSON data into an array of Category objects
        val trendings = gson.fromJson(jsonTrending, Array<SingleProduct>::class.java)

        // Create and add CardViews for each Category
        trendings.forEach { trending ->
            // Create a CardView
            val cardView = createCardViewTrending(trending)
            trendingFoodsLayout.addView(cardView)
        }

        /*
         *   THE
         *   RECENTLY
         */

        // let's go for the recently
        val recently = gson.fromJson(jsonRecently, Array<SingleProduct>::class.java)

        // Create and add CardViews for each Category
        recently.forEach { recently ->
            // Create a CardView
            val cardView = createCardViewRecently(recently)
            recentlyFoodsLayout.addView(cardView)
        }

        // NAVIGATION ANIMATIONS
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)

        // Toggle for the navigation drawer
        val toggle = ActionBarDrawerToggle(
            this@Home, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Handle navigation item clicks here
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home_side_nav -> {
                    HomePage()
                }
                R.id.account_side_nav -> {
                    account()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.orders_side_nav -> {
                    // Inflate the outer_order.xml layout
                    val outerOrderLayout = LayoutInflater.from(this@Home).inflate(R.layout.outer_cover_orders, null)


                    // Find the FrameLayout in your activity_home.xml where you want to add the outer LinearLayout
                    val frameLayout = findViewById<FrameLayout>(R.id.container)
                    frameLayout.removeAllViews()

                    // Add the outer LinearLayout to the FrameLayout
                    frameLayout.addView(outerOrderLayout)

                    orders(outerOrderLayout)
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.feedback_side_nav -> {
                    feedback()
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                R.id.share_side_nav -> {
                    shareApp()
                }
                // Add cases for other items as needed
            }
            true
        }

        // when menu button is clicked it opens navigation
        val menuIcon: ImageView = findViewById(R.id.menu_icon)
        menuIcon.setOnClickListener {
            // Open the navigation drawer when the menu button is clicked
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // when the search is clicked
        val searchIcon = findViewById<ImageView>(R.id.search_icon)
        searchIcon.setOnClickListener {
            searchPage()
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Set the listener for item selection
        bottomNavigation.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_item_home -> {
                    HomePage()
                    true
                }
                R.id.menu_item_orders -> {
                    // Inflate the outer_order.xml layout
                    val outerOrderLayout = LayoutInflater.from(this@Home).inflate(R.layout.outer_cover_orders, null)

                    // Find the FrameLayout in your activity_home.xml where you want to add the outer LinearLayout
                    val frameLayout = findViewById<FrameLayout>(R.id.container)
                    frameLayout.removeAllViews()

                    // Add the outer LinearLayout to the FrameLayout
                    frameLayout.addView(outerOrderLayout)

                    orders(outerOrderLayout)
                    true
                }
                R.id.menu_item_account -> {
                    account()
                    true
                }
                else -> false
            }
        }


        // for the side view inplace the variables
        val gsonUser = Gson()
        val user = gsonUser.fromJson(jsonUser, User::class.java)
        usernameOfUser.text = user.username
        phoneOfUser.text = user.phone

    }















    private fun searchPage(){
        setContentView(R.layout.search_activity)

        val searchBar = findViewById<EditText>(R.id.searchBar)
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val searchTop = findViewById<TextView>(R.id.trending_text)

        searchBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                // Perform the search here
                val search = searchBar.text.toString()

                // fetch results and display them here
                CoroutineScope(Dispatchers.Main).launch {

                    GlobalScope.launch(Dispatchers.IO) {
                        val isConnected = testInternet()

                        withContext(Dispatchers.Main) {
                            if (!isConnected) {
                                Toast.makeText(this@Home, "It seems You are offline, Check your network and Try again!", Toast.LENGTH_LONG).show()
                            }else{
                                val deferredSearch = async(Dispatchers.IO) { fetchData(search,"search") }


                                // Wait for all data fetching tasks to complete
                                val jsonSearch = deferredSearch.await()

                                searchTop.text = "Result For: $search"


                                val gson = Gson()
                                val searchResults = gson.fromJson(jsonSearch, Array<SingleProduct>::class.java)
                                val trendingFoodsLayout = findViewById<LinearLayout>(R.id.trending_foods)

                                searchResults.forEach { trending ->
                                    // Create a CardView
                                    val cardView = createCardViewTrending(trending)
                                    trendingFoodsLayout.addView(cardView)
                                }

                            }
                        }
                    }

                }




                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }



        /*
        // Listen for the Enter key press in the search bar
        searchBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                // Perform the search here

                search = searchBar.text.toString()
                val search_text = findViewById<TextView>(R.id.trending_text)
                search_text.text = "Result for : $search"


                performSearch(searchBar.text.toString())


                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

         */

        // Handle back button click
        backButton.setOnClickListener {
            HomePage()
        }
    }









    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            // Pressed twice, exit the app
            finishAffinity() // Finish all activities in the task
            System.exit(0) // Exit the app
            return
        }

        this.doubleBackToExitPressedOnce = true
        //Toast.makeText(this, "Press BACK again to go to the home page", Toast.LENGTH_SHORT).show()

        // Reset the flag after a delay if not pressed again
        Handler().postDelayed({
            doubleBackToExitPressedOnce = false
        }, doubleBackToExitToastDuration.toLong())

        // go home if pressed once
        HomePage()
    }


    private fun getUserId(): Int {
        val sharedPreferences: SharedPreferences =
            getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)

        // Default value is set to 0 in case "userid" preference doesn't exist
        return sharedPreferences.getInt("userid", 0)
    }










}
