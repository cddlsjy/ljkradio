package com.example.ijkradio.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 电台存储管理器
 * 使用 SharedPreferences 存储电台列表和播放状态
 */
class StationStorage(context: Context) {

    companion object {
        private const val PREFS_NAME = "ijk_radio_prefs"
        private const val KEY_STATIONS = "stations"
        private const val KEY_LAST_PLAYED_ID = "last_played_id"
        private const val KEY_LAST_VOLUME = "last_volume"
        private const val KEY_LAST_POSITION = "last_position"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * 保存电台列表
     */
    fun saveStations(stations: List<Station>) {
        val json = gson.toJson(stations)
        prefs.edit().putString(KEY_STATIONS, json).apply()
    }

    /**
     * 获取电台列表
     */
    fun getStations(): List<Station> {
        val json = prefs.getString(KEY_STATIONS, null) ?: return getDefaultStations()
        return try {
            val type = object : TypeToken<List<Station>>() {}.type
            gson.fromJson(json, type) ?: getDefaultStations()
        } catch (e: Exception) {
            getDefaultStations()
        }
    }

    /**
     * 添加电台
     */
    fun addStation(station: Station) {
        val stations = getStations().toMutableList()
        // 检查是否已存在同名电台
        if (stations.none { it.name == station.name && it.url == station.url }) {
            stations.add(station)
            saveStations(stations)
        }
    }

    /**
     * 删除电台
     */
    fun removeStation(station: Station) {
        val stations = getStations().toMutableList()
        stations.removeIf { it.id == station.id }
        saveStations(stations)
    }

    /**
     * 删除电台 by ID
     */
    fun removeStationById(stationId: String) {
        val stations = getStations().toMutableList()
        stations.removeIf { it.id == stationId }
        saveStations(stations)
    }

    /**
     * 更新电台
     */
    fun updateStation(station: Station) {
        val stations = getStations().toMutableList()
        val index = stations.indexOfFirst { it.id == station.id }
        if (index != -1) {
            stations[index] = station
            saveStations(stations)
        }
    }

    /**
     * 保存上次播放的电台ID
     */
    fun saveLastPlayed(station: Station) {
        prefs.edit()
            .putString(KEY_LAST_PLAYED_ID, station.id)
            .apply()
    }

    /**
     * 获取上次播放的电台ID
     */
    fun getLastPlayedId(): String? {
        return prefs.getString(KEY_LAST_PLAYED_ID, null)
    }

    /**
     * 获取上次播放的电台
     */
    fun getLastPlayed(): Station? {
        val lastId = getLastPlayedId() ?: return null
        return getStations().find { it.id == lastId }
    }

    /**
     * 保存音量
     */
    fun saveVolume(volume: Float) {
        prefs.edit().putFloat(KEY_LAST_VOLUME, volume).apply()
    }

    /**
     * 获取音量
     */
    fun getVolume(): Float {
        return prefs.getFloat(KEY_LAST_VOLUME, 1.0f)
    }

    /**
     * 保存播放位置（毫秒）
     */
    fun savePosition(position: Long) {
        prefs.edit().putLong(KEY_LAST_POSITION, position).apply()
    }

    /**
     * 获取播放位置（毫秒）
     */
    fun getPosition(): Long {
        return prefs.getLong(KEY_LAST_POSITION, 0L)
    }

    /**
     * 清空所有数据
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * 获取默认电台列表
     */
    private fun getDefaultStations(): List<Station> {
        return listOf(
            Station(
                name = "BBC World Service",
                url = "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service",
                description = "BBC 国际广播"
            ),
            Station(
                name = "Radio Paradise Main Mix",
                url = "http://stream.radioparadise.com/mp3-192",
                description = "Radio Paradise 主频道"
            ),
            Station(
                name = "SomaFM Groove Salad",
                url = "http://ice2.somafm.com/groovesalad-128-mp3",
                description = "SomaFM 氛围音乐"
            ),
            Station(
                name = "Jazz24",
                url = "https://live.amperwave.net/direct/ppm-jazz24mp3-ibc1",
                description = "爵士乐24小时"
            ),
            Station(
                name = "Classic FM",
                url = "http://media-ice.musicradio.com/ClassicFMMP3",
                description = "经典音乐"
            )
        )
    }

    /**
     * 检查是否为首次运行
     */
    fun isFirstRun(): Boolean {
        return !prefs.contains(KEY_STATIONS)
    }

    /**
     * 标记首次运行已完成
     */
    fun markFirstRunComplete() {
        // 首次运行后会保存电台数据，所以只需检查KEY_STATIONS即可
    }
}
