package eu.tricht.gamesense.events

import eu.tricht.gamesense.*
import eu.tricht.gamesense.com.steelseries.ApiClientFactory
import eu.tricht.gamesense.com.steelseries.model.Data
import eu.tricht.gamesense.com.steelseries.model.Event
import eu.tricht.gamesense.com.steelseries.model.Frame
import eu.tricht.gamesense.model.SongInformation
import java.net.ConnectException
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt
import eu.tricht.gamesense.i18n.LanguageManager
import eu.tricht.gamesense.model.ScrollingText

class EventProducer : TimerTask() {
    private val dataFetcher = DataFetcher()
    private var client = ApiClientFactory().createApiClient()
    private val dateFormat = DateFormat.getTimeInstance(0)
    private var volume: Int? = null
    private var waitTicks = 0
    private var displayClockPeriodically = 0
    private var currentSong: SongInformation? = null
    private var masterVolumeTimeout = 0
    private var scrollingClock: ScrollingText? = null

    override fun run() {
        try {
            handleTick()
        } catch (e: ConnectException) {
            client = ApiClientFactory().createApiClient()
        }
    }

    private fun handleTick() {
        val oldVolume = this.volume
        this.volume = getVolume()
        if (oldVolume != null && this.volume != oldVolume) {
            sendVolumeEvent()
            return
        }
        if (displayClockPeriodically > 0) {
            --displayClockPeriodically
        }
        if (waitTicks > 0) {
            --waitTicks
            return
        }
        if (preferences.get("clockPeriodically", "false").toBoolean() && displayClockPeriodically == 0) {
            sendClockEvent()
            displayClockPeriodically = Tick.msToTicks(10000)
            waitTicks = Tick.msToTicks(2000)
            return
        }
        val potentialSong = dataFetcher.getCurrentSong()
        if (preferences.get("songInfo", "true").toBoolean() && potentialSong != null && potentialSong != "") {
            if (currentSong == null || potentialSong != currentSong!!.fullSongName) {
                currentSong = SongInformation(potentialSong)
            }
            sendSongEvent()
            return
        }
        sendClockEvent()
    }

    private fun getVolume(): Int {
        if (masterVolumeTimeout == 25) {
            masterVolumeTimeout = 0
        }
        masterVolumeTimeout++
        return (SoundUtil.getMasterVolumeLevel() * 100).roundToInt()
    }

    private fun sendClockEvent() {
        if (!preferences.get("clock", "true").toBoolean()) {
            return
        }
        val showDate = preferences.get("clockShowDate", "false").toBoolean()
        val showSeconds = preferences.get("clockShowSeconds", "false").toBoolean()
        val is12h = preferences.get("clock12h", "false").toBoolean()
        val lang = LanguageManager.currentLanguage
        val datePattern = when (lang) {
            "tr" -> "dd.MM.yyyy"
            "en" -> "MM/dd/yyyy"
            "de" -> "dd.MM.yyyy"
            "fr" -> "dd/MM/yyyy"
            "it" -> "dd/MM/yyyy"
            "es" -> "dd/MM/yyyy"
            "ru" -> "dd.MM.yyyy"
            "zh" -> "yyyy/MM/dd"
            "ja" -> "yyyy/MM/dd"
            else -> "yyyy-MM-dd"
        }
        val amPmText = when (lang) {
            "tr" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "ÖÖ" else "ÖS"
            "en" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            "de" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "VM" else "NM"
            "fr" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            "it" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
            "es" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "a. m." else "p. m."
            "ru" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "ДП" else "ПП"
            "zh" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "上午" else "下午"
            "ja" -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "午前" else "午後"
            else -> if (Calendar.getInstance().get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        }
        val timePattern = buildString {
            append(if (is12h) "hh:mm" else "HH:mm")
            if (showSeconds) append(":ss")
        }
        val dateFormat = java.text.SimpleDateFormat(datePattern)
        val timeFormat = java.text.SimpleDateFormat(timePattern)
        val now = Date()
        val text = if (showDate) {
            dateFormat.format(now)
        } else {
            timeFormat.format(now) + if (is12h) " " + amPmText else ""
        }
        val output = if (showDate) {
            text
        } else if (showSeconds) {
            if (scrollingClock == null || scrollingClock?._text != text) {
                scrollingClock = ScrollingText(text)
            }
            scrollingClock!!.text
        } else {
            text
        }
        client.sendEvent(
            Event(
                GAME_NAME,
                CLOCK_EVENT,
                Data(
                    output
                )
            )
        ).execute()
        waitTicks = Tick.msToTicks(200)
    }

    private fun sendSongEvent() {
        val songName = currentSong!!.song()
        client.sendEvent(
            Event(
                GAME_NAME,
                SONG_EVENT,
                Data(
                    // This is unused, but Steelseries 'caches' the value. So we have to change it.
                    songName + System.currentTimeMillis(),
                    Frame(
                        songName,
                        currentSong!!.artist()
                    )
                )
            )
        ).execute()
        waitTicks = Tick.msToTicks(200)
    }

    private fun sendVolumeEvent() {
        if (this.volume == null || !preferences.get("volume", "true").toBoolean()) {
            return
        }
        waitTicks = Tick.msToTicks(1000)
        client.sendEvent(
            Event(
                GAME_NAME,
                VOLUME_EVENT,
                Data(
                    this.volume!!
                )
            )
        ).execute()
    }
}