package com.example.weatherapp.Fragments

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherapp.MainViewModel
import com.example.weatherapp.adapters.VpAdapter
import com.example.weatherapp.adapters.WeatherModel
import com.example.weatherapp.databinding.FragmentMainBinding
import com.example.weatherapp.isPermissionGranted
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

const val BASE_RECOURSE = "https://api.weatherapi.com/v1/forecast.json?key="
const val API_KEY = "2d9e9fac3423456f83b143311222511"
const val COUNT_OF_DAYS = "7"
const val PARAMETERS = "&aqi=no&alerts=no"

@Suppress("DEPRECATION")
class MainFragment : Fragment() {

    private lateinit var fLocationClient: FusedLocationProviderClient

    private val fList = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private val tList = listOf(
        "Hours",
        "Days"
    )
    private lateinit var pLauncher: ActivityResultLauncher<String> //Создание лаунчера
    private lateinit var binding: FragmentMainBinding
    private val model: MainViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        getLocation()
        updateCurrentCard()
    }

    private fun init() = with(binding) {
        fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = VpAdapter(activity as FragmentActivity, fList) //создание адаптера
        vp.adapter = adapter //Присвоение адаптера к ViewPager
        //Переключение ViewPager с помощью TabLayout
        TabLayoutMediator(tabLayout, vp) { tab, position ->
            tab.text = tList[position]
        }.attach()
        btnSync.setOnClickListener {
            tabLayout.selectTab(tabLayout.getTabAt(0))
            getLocation()
        }
    }

    private fun getLocation() {
        val cancellationT = CancellationTokenSource()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fLocationClient
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationT.token)
            .addOnCompleteListener {
                request("${it.result.latitude},${it.result.longitude}")
            }
    }

    //Создание обсервера
    private fun updateCurrentCard() = with(binding) {
        model.liveDataCurrent.observe(viewLifecycleOwner) {
            val maxMinTemp = "${it.maxTemp}°C / ${it.minTemp}°C"
            textDate.text = it.time
            textCity.text = it.city
            textCurrentTemp.text = it.currentTemp.ifEmpty { maxMinTemp }
            textConditions.text = it.condition
            textMaxMinTem.text = if (it.currentTemp.isEmpty()) "" else maxMinTemp
            Picasso.get().load("https:" + it.imgUrl).into(imgWeatherType)
        }
    }

    //Инициализация лаунчера
    private fun permissionListener() {
        //Создание callback
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_SHORT).show()
            getLocation()
        }
    }

    //Проверка на выдачу разрешения
    private fun checkPermission() {
        if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    //Получение данных с сервера
    private fun request(city: String) {
        val url = BASE_RECOURSE + API_KEY + "&q=" + city + "&days=" + COUNT_OF_DAYS + PARAMETERS
        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            { result ->
                parseWeatherData(result)

            },
            { error ->
                Log.d("MyLog", "Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String) {
        val mainObj = JSONObject(result)
        val list = parseDays(mainObj)
        parseCurrentData(mainObj, list[0])
    }

    //Получение списка по часам
    private fun parseDays(mainObj: JSONObject): List<WeatherModel> {
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObj.getJSONObject("forecast")
            .getJSONArray("forecastday")
        val name = mainObj.getJSONObject("location").getString("name")
        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject
            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                "",
                day.getJSONObject("day").getString("maxtemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getString("mintemp_c").toFloat().toInt().toString(),
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }

    //Получение информации о погоде на текущий день
    private fun parseCurrentData(mainObj: JSONObject, weatherItem: WeatherModel) {
        val item = WeatherModel(
            mainObj.getJSONObject("location").getString("name"),
            mainObj.getJSONObject("current").getString("last_updated"),
            mainObj.getJSONObject("current")
                .getJSONObject("condition")
                .getString("text"),
            mainObj.getJSONObject("current").getString("temp_c"),
            weatherItem.maxTemp,
            weatherItem.minTemp,
            mainObj.getJSONObject("current")
                .getJSONObject("condition")
                .getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value = item
    }

    companion object {
        fun newInstance() = MainFragment()
    }
}