package com.travels.searchtravels

import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.api.services.vision.v1.model.LatLng
import com.preview.planner.prefs.AppPreferences
import com.travels.searchtravels.api.OnVisionApiListener
import com.travels.searchtravels.api.VisionApi
import com.travels.searchtravels.utils.Constants
import com.travels.searchtravels.utils.ImageHelper.resizeBitmap
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MainActivity : AppCompatActivity() {
    //проверка категорий картиночек (море, океан, пляж и т.д.)
    @Test
    fun categories_check() {
        for (i in 1..6) {
            var linkS = ""
            linkS = when (i) {
                1 -> "https://w-dog.ru/wallpapers/10/12/434755555995923/pejzazh-zakat-plyazh-pesok-more-bereg.jpg" //море
                2 -> "https://i.artfile.me/wallpaper/23-06-2014/2048x1382/priroda-morya-okeany-plyazh-839901.jpg" //океан
                3 -> "https://img4.goodfon.ru/original/2560x1440/0/c4/tropiki-more-pliazh-kariby.jpg" //пляж
                4 -> "https://www.ejin.ru/wp-content/uploads/2019/05/gora-13.jpg" //горы
                5 -> "https://img2.goodfon.ru/original/2560x1600/7/49/quebec-canada-kvebek-kanada-les-reka-zima-sneg-derevia.jpg" //снег
                else -> {
                    "https://get.pxhere.com/photo/animal-pet-cat-mammal-fauna-close-up-cats-nose-whiskers-snout-eye-vertebrate-macro-photography-tabby-cat-european-shorthair-wild-cat-small-to-medium-sized-cats-cat-like-mammal-domestic-short-haired-cat-pixie-bob-rusty-spotted-cat-1106838.jpg" //прочее
                }
            }
            val uri =
                Uri.parse(linkS)
            try {
                val bitmap = resizeBitmap(
                    MediaStore.Images.Media.getBitmap(
                        contentResolver,
                        uri
                    )
                )
                Constants.PICKED_BITMAP = bitmap
                VisionApi.findLocation(
                    bitmap,
                    AppPreferences.getToken(applicationContext),
                    object : OnVisionApiListener {
                        override fun onSuccess(latLng: LatLng) {
                            throw Exception("this test is not for places")
                        }

                        override fun onErrorPlace(category: String) {
                            when (category) {
                                "sea" -> {
                                    assertEquals(i, 1)
                                }
                                "ocean" -> {
                                    assertEquals(i, 2)
                                }
                                "beach" -> {
                                    assertEquals(i, 3)
                                }
                                "mountain" -> {
                                    assertEquals(i, 4)
                                }
                                "snow" -> {
                                    assertEquals(i, 5)
                                }
                                else -> {
                                    assertEquals(i, 1)
                                }
                            }
                        }

                        override fun onError() {
                            throw Exception("error")
                        }
                    })
            } catch (e: Exception) {
                assertEquals(0, 1)
            }
        }
    }

    //проверка цен проживания в городах
    @Test
    fun test_prices() {
        for (i in 1..5) {
            val city = when(i){
                1 -> "Moscow" //данные для москвы
                2 -> "Paris" //для парижа
                3 -> "Berlin" //для берлина
                4 -> "Kiev" //для киева
                else -> {
                    "London" //прочее
                }
            }
            try {
                val obj = URL("https://autocomplete.travelpayouts.com/places2?locale=en&types[]=city&term=$city")
                val connection = obj.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                connection.setRequestProperty("Content-Type", "application/json")
                val bufferedReader = BufferedReader(InputStreamReader(connection.inputStream))
                var inputLine: String?
                val response = StringBuffer()
                while (bufferedReader.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                bufferedReader.close()
                val responseJSON = JSONArray(response.toString())
                Log.d("myLogs", "responseJSON = $responseJSON")
                val code = responseJSON.getJSONObject(0).getString("code")
                val obj2 = URL("https://api.travelpayouts.com/v1/prices/cheap?origin=LED&depart_date=2019-12&return_date=2019-12&token=471ae7d420d82eb92428018ec458623b&destination=$code")
                val connection2 = obj2.openConnection() as HttpURLConnection
                connection2.requestMethod = "GET"
                connection2.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection2.setRequestProperty("Accept-Language", "en-US,en;q=0.5")
                connection2.setRequestProperty("Content-Type", "application/json")
                val bufferedReader2 = BufferedReader(InputStreamReader(connection2.inputStream))
                var inputLine2: String?
                val response2 = StringBuffer()
                while (bufferedReader2.readLine().also { inputLine2 = it } != null) {
                    response2.append(inputLine2)
                }
                bufferedReader2.close()
                val responseJSON2 = JSONObject(response2.toString())
                try {
                    val ticketPrice = responseJSON2.getJSONObject("data").getJSONObject(code).getJSONObject("1").getString("price").toInt() //получили данные о цене
                    //по условию задания не совсем понятно откуда мы должны брать цены для сравнения (из такого же api? сравнивать два одинаковых запроса?)
                    //поэтому я решил просто захардкодить сравнение цены с "примерными" (хаха, возможно даже не рядом) ценами для этих городов
                    when(i){
                        1 -> assertEquals(12000, ticketPrice) //данные для москвы
                        2 -> assertEquals(17000, ticketPrice) //для парижа
                        3 -> assertEquals(15000, ticketPrice) //для берлина
                        4 -> assertEquals(8000, ticketPrice) //для киева
                        else -> {
                            assertEquals(20000, ticketPrice) //прочее
                        }
                        //Пы.Сы.: можно добавить целочисленное деление для сравнения тысяч или десятков тысяч, чтобы увеличить допустимый разброс цен на путешествие
                    }
                } catch (e: java.lang.Exception) {
                    assertEquals(0, 1)
                }
            } catch (e: Exception) {
                assertEquals(0, 1)
            }
        }
    }
}
