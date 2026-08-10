package com.jake.popupschool.data.remote

import com.google.gson.JsonObject
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * NEIS(나이스) Open API. Retrofit default arguments are intentionally avoided:
 * they don't resolve correctly through Retrofit's dynamic proxy.
 */
interface NeisApi {

    @GET("hub/mealServiceDietInfo")
    suspend fun getMeals(
        @Query("KEY") key: String,
        @Query("Type") type: String,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("MLSV_FROM_YMD") fromDate: String,
        @Query("MLSV_TO_YMD") toDate: String
    ): JsonObject

    @GET("hub/{endpoint}")
    suspend fun getTimetable(
        @Path("endpoint") endpoint: String,
        @Query("KEY") key: String,
        @Query("Type") type: String,
        @Query("ATPT_OFCDC_SC_CODE") officeCode: String,
        @Query("SD_SCHUL_CODE") schoolCode: String,
        @Query("AY") academicYear: String,
        @Query("SEM") semester: String,
        @Query("GRADE") grade: String,
        @Query("CLASS_NM") classNm: String,
        @Query("ALL_TI_YMD") date: String
    ): JsonObject

    @GET("hub/schoolInfo")
    suspend fun searchSchool(
        @Query("KEY") key: String,
        @Query("Type") type: String,
        @Query("SCHUL_NM") schoolName: String
    ): JsonObject
}
