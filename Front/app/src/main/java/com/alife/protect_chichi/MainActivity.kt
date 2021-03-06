package com.alife.protect_chichi

import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.NumberPicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.alife.protect_chichi.Service.GetHeartResponse
import com.alife.protect_chichi.Service.GetHeartService
import com.alife.protect_chichi.Service.getHeartView
import com.alife.protect_chichi.databinding.ActivityMainBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.hookedonplay.decoviewlib.charts.SeriesItem
import com.hookedonplay.decoviewlib.events.DecoEvent
import org.json.JSONException

import org.json.JSONObject

import org.json.JSONArray
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate

import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.lang.Math.round
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(), getHeartView {

    val heartService = GetHeartService()
    private var heartRate = 0f
    private var hasweekdataLoad = false
    private var getheartRateThread = false
    private var weeklyHeartrateaverage = 0f
    lateinit var getHeartThread : GetHeartThread
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        heartService.setgetisLikeView(this)
        getHeartThread = GetHeartThread()
        getHeartThread.start()
        initView()
        setContentView(binding.root)
    }

    override fun onDestroy() {
        super.onDestroy()
        getHeartThread.interrupt()
    }

    inner class GetHeartThread: Thread(){
        override fun run() {
            super.run()
            while(true){
                if(getheartRateThread){
                    sleep(10000)
                    runOnUiThread{
                        getHeartRate()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getHeartRate() {
        val current = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formatted = current.format(formatter)
        heartService.getHeartRate("{\"time\":\"${formatted}\"}")
    }

    private fun initView() {
        binding.mainChart.addSeries(SeriesItem.Builder(Color.parseColor("#20000000"))
            .setRange(0f, 200f, 200f)
            .setInitialVisibility(true)
            .setLineWidth(50f)
            .build())
        binding.mainChart.configureAngles(360, -40)

        val seriesItem1 = SeriesItem.Builder(Color.parseColor("#9b111e"))
            .setRange(0f, 200f, 0f)
            .setLineWidth(50f)
            .build()

//        val series1Index: Int = binding.mainChart.addSeries(seriesItem1)
        // ???????????? ?????? ????????? ????????? ?????????
        binding.heartSignalLayout.visibility = View.GONE
        binding.mainHeartBt.setOnClickListener {
            if(!getheartRateThread){
                binding.mainHeartBt.text = "????????? ?????? ?????? ??? ????????????"
                binding.heartSignalLayout.visibility = View.VISIBLE
                heartService.sendSignal(1)
                getheartRateThread = true
            }else{
                binding.mainHeartBt.text = "????????? ?????? ?????? ??? ?????????"
                binding.heartSignalLayout.visibility = View.GONE
                heartService.sendSignal(0)
                getheartRateThread = false
            }
        }

        // ??? ?????? ?????????
        val timeHourList = arrayListOf<NumberPicker>()
        timeHourList.add(binding.timeHourPicker1)
        timeHourList.add(binding.timeHourPicker2)
        timeHourList.add(binding.timeHourPicker3)
        timeHourList.add(binding.timeHourPicker4)
        timeHourList.add(binding.timeHourPicker5)
        val timeMinuteList = arrayListOf<NumberPicker>()
        timeMinuteList.add(binding.timeMinutePicker1)
        timeMinuteList.add(binding.timeMinutePicker2)
        timeMinuteList.add(binding.timeMinutePicker3)
        timeMinuteList.add(binding.timeMinutePicker4)
        timeMinuteList.add(binding.timeMinutePicker5)

        val cbList = arrayListOf<CheckBox>()
        cbList.add(binding.mainAlarm1Cb)
        cbList.add(binding.mainAlarm2Cb)
        cbList.add(binding.mainAlarm3Cb)
        cbList.add(binding.mainAlarm4Cb)
        cbList.add(binding.mainAlarm5Cb)
        val foodTimeDataList = arrayListOf<Int>()
        for(i in timeHourList){
            i.minValue = 0
            i.maxValue = 23
            i.setOnValueChangedListener { picker, oldVal, newVal ->
            }
        }
        for(i in timeMinuteList){
            i.minValue = 0
            i.maxValue = 59
            i.setOnValueChangedListener { picker, oldVal, newVal ->
            }
        }
        binding.mainAlarmSetBt.setOnClickListener {
            for(i in 0 until cbList.size){
                if(cbList[i].isChecked){
                    foodTimeDataList.add(timeHourList[i].value*60 + timeMinuteList[i].value)
                }else{
                    foodTimeDataList.add(-1)
                }
            }
            heartService.sendFoodTime("{\n" +
                    "  \"foodTime1\": ${foodTimeDataList[0]},\n" +
                    "  \"foodTime2\": ${foodTimeDataList[1]},\n" +
                    "  \"foodTime3\": ${foodTimeDataList[2]},\n" +
                    "  \"foodTime4\": ${foodTimeDataList[3]},\n" +
                    "  \"foodTime5\": ${foodTimeDataList[4]}\n" +
                    "}")
            foodTimeDataList.clear()
            Toast.makeText(this,"????????? ?????????????????????.",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSuccess(result: GetHeartResponse) {
        try {
            val jsonArray = JSONArray(result.body)
            // ?????? ????????? ???????????? ????????????
            if(!hasweekdataLoad){
                val dataList = arrayListOf<String>()
                val cal = Calendar.getInstance()
                val week = ArrayList<String>()
                cal.time = Date()
                val df: DateFormat = SimpleDateFormat("yyyy-MM-dd")
                val weekdata = arrayListOf<String>()
                for (i in 0..7) {
                    week.add(df.format(cal.time).toString())
                    cal.add(Calendar.DATE, -1)
                }
                for(i in 0 until jsonArray.length()){
                    val subJsonObject = jsonArray.getJSONObject(i)
                    for(j in week){
                        if(subJsonObject.getString("date").equals("${j} 21:00:00")){
                            dataList.add(subJsonObject.getString("bpm"))
                            weeklyHeartrateaverage += subJsonObject.getString("bpm").toFloat()
                            weekdata.add(j.substring(5,10))
                        }
                    }
                }
                weeklyHeartrateaverage /= weekdata.size
                setWeek(binding.mainWeekChart, dataList,weekdata)
                hasweekdataLoad= true
            }

            // ?????? ?????? 5??? ?????? ??? ???????????? ?????????
            val entries: ArrayList<Entry> = ArrayList()
            for(i in jsonArray.length()-8 until jsonArray.length()){
                val subJsonObject = jsonArray.getJSONObject(i)
                entries.add(Entry(i.toFloat(),subJsonObject.getString("bpm").toFloat()))
            }
            val dataset = LineDataSet(entries, "?????? ???")

            val lineDataSet = LineDataSet(entries, "?????? ???")
            lineDataSet.color = Color.parseColor("#9b111e")

            val dataSet = ArrayList<ILineDataSet>()
            dataSet.add(dataset)
            var data = LineData(dataSet)
            binding.mainRecentChart.data = data
            binding.mainRecentChart.setBackgroundColor(Color.WHITE)
            binding.mainRecentChart.setScaleEnabled(false) //Zoom In/Out
            binding.mainRecentChart.axisRight.isEnabled = false
            binding.mainRecentChart.invalidate()


            // ????????? ?????? ???
            val subJsonObject = jsonArray.getJSONObject(jsonArray.length()-1)
            var nowheartRate = subJsonObject.getString("bpm")
            if(nowheartRate.toFloat() >=199f){
                nowheartRate= 199.toString()
            }
            binding.mainHeartTv.text = "${nowheartRate} bpm"
            binding.mainTempTv.text = "${subJsonObject.getString("temp")}??C"
            binding.mainHeartStateTv.text = " ${round(subJsonObject.getString("bpm").toFloat() / (subJsonObject.getString("bpm").toFloat() + weeklyHeartrateaverage) * 100f)}%"
            binding.mainHeartLastweekTv.text ="????????? ??????(${weeklyHeartrateaverage.toFloat().roundToInt()})??? ????????? ?????? "
            
            if(nowheartRate.toInt()<100){
                binding.mainNormalTv.text = "?????? ?????????"
            }else if(nowheartRate.toInt()<140){
                binding.mainNormalTv.text = "?????? ??????"
            }else{
                binding.mainNormalTv.text = "?????? ?????????"
            }
            val seriesItem1 = SeriesItem.Builder(Color.parseColor("#FE848A"))
                .setRange(0f, 200f, heartRate)
                .setLineWidth(50f)
                .build()
            val series1Index: Int = binding.mainChart.addSeries(seriesItem1)
            binding.mainChart.addEvent(DecoEvent.Builder(nowheartRate.toFloat())
                .setIndex(series1Index)
                .build())
            heartRate = nowheartRate.toFloat()

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onFailure(code: Int, message: String) {
        Log.d("test", message.toString())
    }

    override fun onSendSignalSuccess(result: GetHeartResponse) {
        Log.d("test", result.toString())
    }

    override fun onSendSignalFailure(code: Int, result: String) {
        Log.d("test", result.toString())
    }

    override fun onSendFoodTimeSuccess(result: GetHeartResponse) {
        Log.d("FoodTimetest", result.toString())
    }

    override fun onSendFoodTimeFailure(code: Int, message: String) {
        Log.d("FoodTimetest", message.toString())
    }


    private fun setWeek(barChart: BarChart, resultdata: ArrayList<String>, weekdata: ArrayList<String>) {
        initBarChart(barChart,weekdata)
        barChart.setScaleEnabled(false) //Zoom In/Out
        val valueList = ArrayList<Double>()
        val entries: ArrayList<BarEntry> = ArrayList()
        val title = "?????? ?????? ???"

        for (i in resultdata) {
            valueList.add(i.toDouble())
        }

        // ??????????????? ????????? ?????????
        for (i in 0 until valueList.size) {
            val barEntry = BarEntry(i.toFloat(), valueList[i].toFloat())
            entries.add(barEntry)
        }

        val barDataSet = BarDataSet(entries, title)
        barDataSet.color = Color.parseColor("#F8C4C4")
        val data = BarData(barDataSet)
        barChart.data = data
        barChart.invalidate()
        binding.mainWeekChart.setBackgroundColor(Color.WHITE)
        binding.mainWeekChart.axisRight.isEnabled = false
    }

    private fun initBarChart(barChart: BarChart, weekdata : ArrayList<String>) {
        //hiding the grey background of the chart, default false if not set
        barChart.setDrawGridBackground(false)
        //remove the bar shadow, default false if not set
        barChart.setDrawBarShadow(false)
        //remove border of the chart, default false if not set
        barChart.setDrawBorders(false)

        //remove the description label text located at the lower right corner
        val description = Description()
        description.setEnabled(false)
        barChart.setDescription(description)

        //X, Y ?????? ??????????????? ??????
        barChart.animateY(1000)
        barChart.animateX(1000)

        //?????? ?????? ???
        val xAxis: XAxis = barChart.getXAxis()
        //change the position of x-axis to the bottom
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        //set the horizontal distance of the grid line
        xAxis.granularity = 1f
        xAxis.textColor = Color.BLACK
        xAxis.textSize = 12f
        //hiding the x-axis line, default true if not set
        //xAxis.setDrawAxisLine(false)
        //hiding the vertical grid lines, default true if not set
        val week = ArrayList<String>()
//        val cal = Calendar.getInstance()
//        cal.time = Date()
//        val df: DateFormat = SimpleDateFormat("yyyy.MM.dd")
        for (i in 0 until weekdata.size) {
            // ????????? 05.02 ???????????? ???????????? week??? ?????????
            week.add(weekdata[i].toString())
//            cal.add(Calendar.DATE, -1)
        }
        // xAxis??? ??????
        xAxis.valueFormatter = IndexAxisValueFormatter(week)

        //?????? ??? hiding the left y-axis line, default true if not set
        val leftAxis: YAxis = barChart.getAxisLeft()
        leftAxis.setDrawAxisLine(false)
        leftAxis.textColor = Color.BLACK

        //?????? ??? hiding the right y-axis line, default true if not set
        val rightAxis: YAxis = barChart.getAxisRight()
        rightAxis.setDrawAxisLine(true)
        rightAxis.textColor = Color.BLACK

        //???????????? ?????????
        val legend: Legend = barChart.getLegend()
        //setting the shape of the legend form to line, default square shape
        legend.form = Legend.LegendForm.LINE
        //setting the text size of the legend
        legend.textSize = 12f
        legend.textColor = android.graphics.Color.BLACK
        //setting the alignment of legend toward the chart
        legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
        //setting the stacking direction of legend
        legend.orientation = Legend.LegendOrientation.HORIZONTAL
        //setting the location of legend outside the chart, default false if not set
        legend.setDrawInside(true)
    }
}