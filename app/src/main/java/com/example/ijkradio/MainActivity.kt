package com.example.ijkradio

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ijkradio.data.Station
import com.example.ijkradio.data.StationStorage
import com.example.ijkradio.player.IjkPlayerManager
import com.example.ijkradio.ui.PlaybackState
import com.example.ijkradio.ui.StationAdapter
import com.example.ijkradio.utils.NetworkHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.slider.Slider

/**
 * 主界面Activity
 * 管理电台列表、播放器控制和UI交互
 */
class MainActivity : AppCompatActivity(), NetworkHelper.NetworkStateListener {

    companion object {
        private const val TAG = "MainActivity"
    }

    // UI组件
    private lateinit var recyclerView: RecyclerView
    private lateinit var playPauseButton: FloatingActionButton
    private lateinit var statusTextView: TextView
    private lateinit var volumeSlider: Slider
    private lateinit var volumeIcon: ImageView
    private lateinit var networkStatusView: TextView
    private lateinit var emptyView: TextView

    // 适配器
    private lateinit var stationAdapter: StationAdapter

    // 数据和播放器
    private lateinit var stationStorage: StationStorage
    private lateinit var playerManager: IjkPlayerManager
    private lateinit var networkHelper: NetworkHelper

    // 电台列表
    private var stations: MutableList<Station> = mutableListOf()
    private var selectedStation: Station? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "onCreate: Initializing activity")

        // 初始化组件
        initViews()
        initStorage()
        initPlayer()
        initNetwork()
        initRecyclerView()
        setupListeners()

        // 加载电台列表
        loadStations()

        // 恢复上次播放
        restoreLastPlayed()
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        recyclerView = findViewById(R.id.stations_recycler_view)
        playPauseButton = findViewById(R.id.play_pause_button)
        statusTextView = findViewById(R.id.status_text_view)
        volumeSlider = findViewById(R.id.volume_slider)
        volumeIcon = findViewById(R.id.volume_icon)
        networkStatusView = findViewById(R.id.network_status_view)
        emptyView = findViewById(R.id.empty_view)

        // 设置音量滑块范围
        volumeSlider.valueFrom = 0f
        volumeSlider.valueTo = 1f

        // 恢复保存的音量
        volumeSlider.value = stationStorage.getVolume()
    }

    /**
     * 初始化存储
     */
    private fun initStorage() {
        stationStorage = StationStorage(this)
    }

    /**
     * 初始化播放器
     */
    private fun initPlayer() {
        playerManager = IjkPlayerManager.getInstance(this)
        playerManager.initialize()

        // 设置保存的音量
        playerManager.setVolume(stationStorage.getVolume())
        volumeSlider.value = stationStorage.getVolume()
    }

    /**
     * 初始化网络监听
     */
    private fun initNetwork() {
        networkHelper = NetworkHelper(this)
        networkHelper.setNetworkStateListener(this)
        networkHelper.startListening()
        updateNetworkStatus()
    }

    /**
     * 初始化RecyclerView
     */
    private fun initRecyclerView() {
        stationAdapter = StationAdapter(
            onStationClick = { station -> onStationClicked(station) },
            onDeleteClick = { station -> showDeleteDialog(station) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = stationAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * 设置监听器
     */
    private fun setupListeners() {
        // 播放/暂停按钮
        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        // 音量滑块
        volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                playerManager.setVolume(value)
                stationStorage.saveVolume(value)
                updateVolumeIcon(value)
            }
        }

        // 添加电台按钮
        findViewById<FloatingActionButton>(R.id.add_station_button).setOnClickListener {
            showAddStationDialog()
        }

        // 监听播放器状态
        playerManager.playbackState.observe(this) { state ->
            updatePlaybackUI(state)
        }
    }

    /**
     * 加载电台列表
     */
    private fun loadStations() {
        stations = stationStorage.getStations().toMutableList()
        stationAdapter.submitList(stations.toList())
        updateEmptyView()
    }

    /**
     * 恢复上次播放
     */
    private fun restoreLastPlayed() {
        val lastPlayed = stationStorage.getLastPlayed()
        if (lastPlayed != null) {
            selectedStation = lastPlayed
            stationAdapter.setSelectedStation(lastPlayed)
        }
    }

    /**
     * 电台点击事件
     */
    private fun onStationClicked(station: Station) {
        Log.d(TAG, "Station clicked: ${station.name}")
        selectedStation = station
        stationAdapter.setSelectedStation(station)
        stationStorage.saveLastPlayed(station)

        // 如果当前正在播放其他电台，切换到新电台
        val currentStation = playerManager.getCurrentStation()
        if (currentStation?.id != station.id) {
            playerManager.playStation(station)
        }
    }

    /**
     * 切换播放/暂停
     */
    private fun togglePlayPause() {
        val currentStation = selectedStation ?: return

        when {
            playerManager.isPlaying() -> {
                playerManager.pause()
            }
            playerManager.getCurrentStation()?.id == currentStation.id -> {
                playerManager.resume()
            }
            else -> {
                playerManager.playStation(currentStation)
            }
        }
    }

    /**
     * 更新播放UI
     */
    private fun updatePlaybackUI(state: PlaybackState) {
        when (state) {
            is PlaybackState.Stopped -> {
                statusTextView.text = getString(R.string.status_stopped)
                playPauseButton.setImageResource(R.drawable.ic_play)
                stationAdapter.setPlayingStation(null)
            }
            is PlaybackState.Buffering -> {
                statusTextView.text = getString(R.string.status_buffering)
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
            is PlaybackState.Playing -> {
                statusTextView.text = getString(R.string.status_playing, state.stationName)
                playPauseButton.setImageResource(R.drawable.ic_pause)
                val station = stations.find { it.name == state.stationName }
                stationAdapter.setPlayingStation(station)
            }
            is PlaybackState.Paused -> {
                statusTextView.text = getString(R.string.status_paused)
                playPauseButton.setImageResource(R.drawable.ic_play)
            }
            is PlaybackState.Error -> {
                statusTextView.text = getString(R.string.status_error, state.message)
                playPauseButton.setImageResource(R.drawable.ic_play)
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 显示添加电台对话框
     */
    private fun showAddStationDialog() {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_add_station, null)

        val nameInput = dialogView.findViewById<EditText>(R.id.station_name_input)
        val urlInput = dialogView.findViewById<EditText>(R.id.station_url_input)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.station_description_input)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_station_title)
            .setView(dialogView)
            .setPositiveButton(R.string.dialog_add) { _, _ ->
                val name = nameInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                val description = descriptionInput.text.toString().trim()

                if (name.isNotEmpty() && url.isNotEmpty()) {
                    val station = Station(
                        name = name,
                        url = if (url.startsWith("http")) url else "http://$url",
                        description = description
                    )
                    addStation(station)
                } else {
                    Toast.makeText(this, R.string.error_invalid_input, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 显示删除确认对话框
     */
    private fun showDeleteDialog(station: Station) {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, station.name))
            .setPositiveButton(R.string.dialog_delete) { _, _ ->
                deleteStation(station)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    /**
     * 添加电台
     */
    private fun addStation(station: Station) {
        if (station.isValid()) {
            stationStorage.addStation(station)
            loadStations()
            Toast.makeText(this, R.string.station_added, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, R.string.error_invalid_station, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 删除电台
     */
    private fun deleteStation(station: Station) {
        // 如果删除的是当前播放的电台，停止播放
        if (playerManager.getCurrentStation()?.id == station.id) {
            playerManager.stop()
        }

        // 如果删除的是选中的电台，清除选中
        if (selectedStation?.id == station.id) {
            selectedStation = null
        }

        stationStorage.removeStation(station)
        loadStations()
        Toast.makeText(this, R.string.station_deleted, Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新音量图标
     */
    private fun updateVolumeIcon(volume: Float) {
        val iconRes = when {
            volume <= 0f -> R.drawable.ic_volume_off
            volume < 0.5f -> R.drawable.ic_volume_down
            else -> R.drawable.ic_volume_up
        }
        volumeIcon.setImageResource(iconRes)
    }

    /**
     * 更新网络状态显示
     */
    private fun updateNetworkStatus() {
        val networkType = networkHelper.getNetworkTypeDescription()
        networkStatusView.text = getString(R.string.network_status, networkType)
        networkStatusView.visibility = View.VISIBLE
    }

    /**
     * 更新空状态视图
     */
    private fun updateEmptyView() {
        if (stations.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // ==================== 网络状态回调 ====================

    override fun onNetworkAvailable() {
        runOnUiThread {
            updateNetworkStatus()
            Toast.makeText(this, R.string.network_available, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onNetworkLost() {
        runOnUiThread {
            updateNetworkStatus()
            Toast.makeText(this, R.string.network_lost, Toast.LENGTH_LONG).show()
        }
    }

    // ==================== 生命周期管理 ====================

    override fun onPause() {
        super.onPause()
        // 保存当前状态
        stationStorage.saveVolume(volumeSlider.value)
        playerManager.getCurrentStation()?.let {
            stationStorage.saveLastPlayed(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止网络监听
        networkHelper.stopListening()

        // 释放播放器资源
        if (::playerManager.isInitialized) {
            playerManager.release()
        }
    }

    override fun onBackPressed() {
        // 最小化应用而不是退出
        moveTaskToBack(true)
    }
}
