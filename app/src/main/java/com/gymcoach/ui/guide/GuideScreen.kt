package com.gymcoach.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gymcoach.ui.GymTopBar
import com.gymcoach.ui.theme.RetroCard

/** Text from the onboard training plan: how it works, watch times, safety, warm-up and recovery. */
object Guide {
    val WARM_UP = listOf(
        "5 min easy rowing or crosstrainer.",
        "10 bodyweight squats, 10 hip hinges, 10 arm circles each way.",
        "One light set of the first exercise at about half your working weight.",
    )

    data class Section(val title: String, val lines: List<String>)

    val sections = listOf(
        Section(
            "How the plan works",
            listOf(
                "5 sessions every 7 days for 6 weeks: 3 strength and 2 cardio, each 40–60 minutes.",
                "Two days are rest or easy movement, because a 12-hour watch is already a lot of load.",
                "Strength A and B alternate: week 1 is A-B-A, week 2 is B-A-B, and so on.",
                "Every exercise has a rep range. Hit the top of it on all sets with good form and the coach adds the smallest step next time (2 kg per dumbbell).",
                "Weeks 1–2 Learn: 2 sets, 3–4 reps left in the tank. Weeks 3–4 Build: 3 sets, 2–3 reps left. Weeks 5–6 Push: 3–4 sets, 1–2 reps left.",
                "\"Reps left in the tank\" means you stop the set when you could still do that many clean reps. Never train to failure on a moving ship.",
            ),
        ),
        Section(
            "Weekly cycle",
            listOf(
                "Day 1: Strength A (odd weeks) / B (even weeks), 45–60 min",
                "Day 2: Cardio 1, rower intervals, 25–35 min",
                "Day 3: Strength B (odd weeks) / A (even weeks), 45–60 min",
                "Day 4: Rest, or 15–20 min easy crosstrainer and stretching",
                "Day 5: Strength A (odd weeks) / B (even weeks), 45–60 min",
                "Day 6: Cardio 2, crosstrainer steady, 30–45 min",
                "Day 7: Full rest",
                "The cycle repeats every 7 days from your first day onboard. If a watch switch lands on a training day, use \"Shift one day\" on the plan screen.",
            ),
        ),
        Section(
            "When to train on each watch",
            listOf(
                "Train once per day in your off-watch time and protect at least 7 hours of sleep.",
                "Finish at least 2 hours before you go to bed.",
                "Day watch (06:00–18:00): train 18:30–19:30, then dinner. Your body is warm after the watch and there are still 2+ hours before sleep.",
                "Night watch (18:00–06:00): train 14:00–15:00, after waking and a meal. Training right after a night watch makes it harder to fall asleep.",
                "Switching watches: make the switch day a rest day.",
                "Eat a small meal or snack 1–2 hours before, and a proper meal with protein within 2 hours after.",
                "Bad sleep or a rough watch? Do the session at Learn-phase effort, or swap it for 20 minutes easy crosstrainer. Missing one session costs nothing; training exhausted is how people get hurt.",
            ),
        ),
        Section("Warm-up (before every strength session, 8 min)", WARM_UP),
        Section(
            "Training on a moving ship",
            listOf(
                "Put dumbbells back in the rack after every set; never leave them loose on the floor or bench.",
                "When the ship is rolling noticeably, skip standing overhead work and heavy squats. Do seated and supported exercises, or switch to the crosstrainer with fixed handles.",
                "Wear closed training shoes with grip, not socks or slippers.",
                "Follow the vessel's gym rules, including any rule about not training alone or telling someone where you are.",
            ),
        ),
        Section(
            "Every lift",
            listOf(
                "Brace your stomach as if someone is about to poke it.",
                "Move slowly on the way down (2–3 seconds) and breathe out on the way up.",
                "In week 1, pick a weight you can lift for 12 clean reps with 4 left in the tank, even if it feels too light.",
                "Typical start: goblet squat 12–16 kg, bench press 10–14 kg per hand, rows 12–16 kg, shoulder press 6–10 kg per hand.",
            ),
        ),
        Section(
            "Recovery",
            listOf(
                "Sleep: 7+ hours in your off-watch block. This matters more than any exercise.",
                "Protein: around 1.6 g per kg of bodyweight per day, spread over your meals (about 115–130 g at 75–80 kg).",
                "Water: drink regularly through the watch, more on training days.",
                "Soreness: stiff muscles 1–2 days after training is normal, especially in week 1. Sharp pain in a joint is not: stop that exercise and use a swap.",
            ),
        ),
    )
}

@Composable
fun GuideScreen(back: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        GymTopBar("Plan guide", back = back)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Guide.sections) { section ->
                RetroCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(section.title, style = MaterialTheme.typography.titleMedium)
                        section.lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}
