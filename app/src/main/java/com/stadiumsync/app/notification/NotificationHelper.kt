package com.stadiumsync.app.notification

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.stadiumsync.app.MainActivity
import com.stadiumsync.app.StadiumSyncApp
import com.stadiumsync.app.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showAlert(alert: StadiumAlert) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val channelId = when (alert.priority) {
            AlertPriority.CRITICAL -> StadiumSyncApp.CHANNEL_CRITICAL
            AlertPriority.WARNING -> StadiumSyncApp.CHANNEL_WARNING
            AlertPriority.INFO -> StadiumSyncApp.CHANNEL_INFO
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setPriority(when (alert.priority) {
                AlertPriority.CRITICAL -> NotificationCompat.PRIORITY_HIGH
                AlertPriority.WARNING -> NotificationCompat.PRIORITY_DEFAULT
                AlertPriority.INFO -> NotificationCompat.PRIORITY_LOW
            })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notifManager.notify(alert.id.hashCode(), notification)
    }

    fun showCrowdAlert(gateName: String, density: Int) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "High Crowd Alert",
            message = "$gateName is at ${density}% capacity. Consider transit adjustments.",
            priority = if (density > 85) AlertPriority.CRITICAL else AlertPriority.WARNING,
            type = AlertType.CROWD_SURGE
        ))
    }

    fun showMatchEndingAlert(minutesLeft: Int) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "Match Ending Soon",
            message = "Estimated $minutesLeft minutes remaining. Prepare transit operations.",
            priority = AlertPriority.CRITICAL,
            type = AlertType.MATCH_ENDING
        ))
    }

    fun showOfflineAlert() {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "Network Disconnected",
            message = "Operating in offline mode. Actions will sync when network returns.",
            priority = AlertPriority.WARNING,
            type = AlertType.NETWORK_FAILURE
        ))
    }

    fun showSyncCompleteAlert(count: Int) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "Sync Complete",
            message = "$count actions synced successfully.",
            priority = AlertPriority.INFO,
            type = AlertType.GENERAL
        ))
    }

    fun showTicketScannedAlert(holderName: String, pavilion: String, section: String, gate: String) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "🎫 Ticket Verified",
            message = "$holderName → $pavilion ($section) | Enter via $gate",
            priority = AlertPriority.INFO,
            type = AlertType.GENERAL
        ))
    }

    fun showMetroCoordinationAlert(routeName: String, action: String) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "🚇 Metro Coordination Alert",
            message = "$action for $routeName.\n📞 Contact: 9359947410\n📧 harshalpaltse@gmail.com",
            priority = AlertPriority.CRITICAL,
            type = AlertType.TRANSPORT_DELAY
        ))
    }

    fun showLiveScoreAlert(team1: String, score1: String, team2: String, phase: String) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "\uD83C\uDFCF Live Score Update",
            message = "$team1: $score1 | Phase: $phase vs $team2",
            priority = AlertPriority.INFO,
            type = AlertType.GENERAL
        ))
    }

    fun showWicketAlert(batsmanName: String, score: String, wicketNum: Int) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "\uD83D\uDD34 WICKET! Out!",
            message = "$batsmanName out! Score: $score | Wicket #$wicketNum\n\uD83D\uDCDE Metro Alert: 9359947410",
            priority = AlertPriority.WARNING,
            type = AlertType.WICKET
        ))
    }

    fun showMilestoneAlert(team: String, milestone: Int, score: String) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "\uD83C\uDF89 Milestone! $team crosses $milestone!",
            message = "$team reaches $milestone runs! Current: $score\n\uD83D\uDCE7 harshalpaltse@gmail.com",
            priority = AlertPriority.INFO,
            type = AlertType.MILESTONE
        ))
    }

    fun showRainDelayAlert(estimatedDelay: Int) {
        showAlert(StadiumAlert(
            id = UUID.randomUUID().toString(),
            title = "\uD83C\uDF27\uFE0F Rain Delay!",
            message = "Match delayed due to rain. Est. wait: ${estimatedDelay} min.\n\uD83D\uDCDE Metro: 9359947410 | \uD83D\uDCE7 harshalpaltse@gmail.com",
            priority = AlertPriority.CRITICAL,
            type = AlertType.RAIN_DELAY
        ))
    }
}

/**
 * Rules engine for auto-triggering local notifications based on cached data.
 * Runs without network — uses last known match/crowd state.
 */
@Singleton
class NotificationRulesEngine @Inject constructor(
    private val notifHelper: NotificationHelper
) {
    private var lastWicketCount = 0
    private var lastMilestone = 0

    fun evaluateMatchState(match: Match?, prediction: MatchPrediction?) {
        if (match == null || prediction == null) return
        if (prediction.estimatedMinutesLeft in 1..10 && prediction.confidencePercent > 70) {
            notifHelper.showMatchEndingAlert(prediction.estimatedMinutesLeft)
        }
    }

    fun evaluateMatchEvents(match: Match?) {
        if (match == null) return
        val scoreParts = match.score1.split("/")
        val runs = scoreParts.getOrNull(0)?.toIntOrNull() ?: 0
        val wickets = scoreParts.getOrNull(1)?.toIntOrNull() ?: 0

        if (wickets > lastWicketCount && lastWicketCount > 0) {
            val batsmen = listOf("Rohit Sharma", "Virat Kohli", "Suryakumar Yadav", "Ishan Kishan",
                "Hardik Pandya", "Tim David", "Tilak Varma", "Jasprit Bumrah", "Cameron Green", "Nehal Wadhera")
            notifHelper.showWicketAlert(batsmen.getOrElse(wickets - 1) { "Batsman" }, match.score1, wickets)
        }
        lastWicketCount = wickets

        val milestone = (runs / 50) * 50
        if (milestone > lastMilestone && milestone > 0) {
            notifHelper.showMilestoneAlert(match.team1, milestone, match.score1)
        }
        lastMilestone = milestone
    }

    fun evaluateCrowdState(crowdData: List<CrowdPressure>) {
        crowdData.filter { it.densityPercent > 80 }.forEach {
            notifHelper.showCrowdAlert(it.gateName, it.densityPercent)
        }
    }

    fun evaluateNetworkState(isOnline: Boolean, wasOnline: Boolean) {
        if (!isOnline && wasOnline) notifHelper.showOfflineAlert()
    }

    fun evaluateTransitUrgency(urgencyScore: Double) {
        if (urgencyScore > 75.0) {
            notifHelper.showMetroCoordinationAlert(
                "Metro Blue Line - Churchgate",
                "Hold departures for 5 min \u2014 crowd wave incoming"
            )
        }
    }

    fun onTicketScanned(ticket: Ticket) {
        notifHelper.showTicketScannedAlert(
            ticket.holderName, ticket.pavilion.displayName, ticket.section, ticket.gate
        )
    }
}
