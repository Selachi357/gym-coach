package com.gymcoach.data

/** Built-in exercise library, inserted the first time the app starts. */
object SeedExercises {
    val all: List<Exercise> = listOf(
        // Chest
        Exercise(
            name = "Bench Press", muscleGroup = "Chest", equipment = "Barbell", isBodyweight = false,
            instructions = """
                Lie on the bench with your eyes under the bar and feet flat on the floor.
                Grip the bar a bit wider than shoulder width.
                Squeeze your shoulder blades together and unrack the bar over your chest.
                Lower the bar slowly to your mid-chest, elbows at about 45 degrees.
                Press the bar back up until your arms are straight.
            """.trimIndent(),
            tips = """
                Keep your butt on the bench and feet planted.
                Don't bounce the bar off your chest.
                Use a spotter or safety bars when going heavy.
            """.trimIndent(),
            easierName = "Dumbbell Bench Press", startWeightKg = 30.0,
        ),
        Exercise(
            name = "Dumbbell Bench Press", muscleGroup = "Chest", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Sit on the end of the bench with dumbbells on your thighs, then lie back and bring them to your chest.
                Feet flat on the floor, shoulder blades pinched together and down.
                Press up until your arms are straight above your chest.
                Lower until the dumbbells are beside your chest, elbows at about 45° from your body, not flared out to 90°.
            """.trimIndent(),
            tips = """
                The weight shown is per dumbbell.
                Control the way down (2–3 seconds) and breathe out as you press.
                Put the dumbbells back in the rack after every set; never leave them loose.
            """.trimIndent(),
            easierName = "Push-Up", harderName = "Bench Press", startWeightKg = 12.0, startReps = 12,
        ),
        Exercise(
            name = "Incline Dumbbell Press", muscleGroup = "Chest", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Set the bench to about 30 degrees.
                Hold the dumbbells at upper-chest level, palms forward.
                Press up until your arms are straight.
                Lower slowly back to the upper chest.
            """.trimIndent(),
            tips = """
                Weight is per dumbbell.
                A steeper bench shifts work to the shoulders; keep it low.
            """.trimIndent(),
            easierName = "Dumbbell Bench Press", startWeightKg = 8.0,
        ),
        Exercise(
            name = "Push-Up", muscleGroup = "Chest", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Hands just wider than shoulders, body straight like a plank.
                Lower until your chest is a fist's height from the floor.
                Push back up to straight arms.
            """.trimIndent(),
            tips = """
                Too hard? Put your hands on the bench, or do knee push-ups.
                Too easy? Slow the way down to 3 seconds.
                Squeeze your glutes and stomach so your hips don't sag.
            """.trimIndent(),
            easierName = "Knee Push-Up", harderName = "Decline Push-Up", startReps = 8,
        ),
        Exercise(
            name = "Knee Push-Up", muscleGroup = "Chest", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Kneel and place your hands just wider than your shoulders.
                Keep a straight line from your head to your knees.
                Lower your chest to just above the floor, then push back up.
            """.trimIndent(),
            tips = """
                Don't stick your hips up in the air.
                Once you reach 15 easy reps, move on to full push-ups.
            """.trimIndent(),
            harderName = "Push-Up", startReps = 8,
        ),
        Exercise(
            name = "Decline Push-Up", muscleGroup = "Chest", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Put your feet on a bench or box and your hands on the floor.
                Keep your body straight and lower your chest towards the floor.
                Push back up to straight arms.
            """.trimIndent(),
            tips = """
                The higher your feet, the harder it gets.
                Keep your core tight so your lower back doesn't sag.
            """.trimIndent(),
            easierName = "Push-Up", startReps = 6,
        ),

        // Back
        Exercise(
            name = "Barbell Row", muscleGroup = "Back", equipment = "Barbell", isBodyweight = false,
            instructions = """
                Hold the bar with hands shoulder-width apart.
                Hinge at the hips until your torso is at about 45 degrees, knees slightly bent.
                Pull the bar to your lower ribs.
                Lower it slowly until your arms are straight.
            """.trimIndent(),
            tips = """
                Keep your back flat, never rounded.
                If your torso bounces up and down, the weight is too heavy.
            """.trimIndent(),
            easierName = "One-Arm Dumbbell Row", startWeightKg = 30.0,
        ),
        Exercise(
            name = "One-Arm Dumbbell Row", muscleGroup = "Back", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Put one knee and the same-side hand on the bench, back flat like a table.
                Hold the dumbbell in the other hand, arm hanging straight.
                Pull the dumbbell to your hip, elbow close to your body.
                Lower slowly until the arm is straight again. Do all reps, then switch sides.
            """.trimIndent(),
            tips = """
                Don't twist your torso.
                Pull with your elbow, not your hand.
            """.trimIndent(),
            harderName = "Barbell Row", startWeightKg = 14.0, startReps = 12,
        ),
        Exercise(
            name = "Chest-Supported Dumbbell Row", muscleGroup = "Back", equipment = "Dumbbells + incline bench", isBodyweight = false,
            instructions = """
                Set the bench to about 30–45°. Lie face-down on it with your chest on the pad.
                Let the dumbbells hang straight down.
                Pull them up toward your lower ribs, squeezing your shoulder blades together, then lower.
            """.trimIndent(),
            tips = """
                The bench supports you, which makes this one of the safest rows on a moving ship.
                Weight is per dumbbell. Keep your chest on the pad.
            """.trimIndent(),
            easierName = "One-Arm Dumbbell Row", startWeightKg = 10.0, startReps = 12,
        ),
        Exercise(
            name = "Dumbbell Pullover", muscleGroup = "Back", equipment = "Dumbbell + bench", isBodyweight = false,
            instructions = """
                Lie across or along the bench holding one dumbbell with both hands above your chest.
                Keep a slight bend in your elbows.
                Lower the dumbbell back over your head in an arc until you feel a stretch in your lats and chest.
                Pull it back over your chest using your lats.
            """.trimIndent(),
            tips = """
                Keep your elbow angle the same the whole time.
                Go only as far back as your shoulders feel comfortable.
            """.trimIndent(),
            startWeightKg = 10.0, startReps = 10,
        ),

        // Legs
        Exercise(
            name = "Back Squat", muscleGroup = "Legs", equipment = "Barbell", isBodyweight = false,
            instructions = """
                Place the bar on your upper back, not your neck.
                Stand with feet shoulder-width apart, toes turned out a little.
                Brace your core, then sit down and back as if into a chair.
                Go down until your thighs are at least parallel to the floor.
                Drive up through your whole foot to stand.
            """.trimIndent(),
            tips = """
                Knees follow the direction of your toes.
                Keep your chest up and back neutral.
                Always use safety bars in the rack.
            """.trimIndent(),
            easierName = "Goblet Squat", startWeightKg = 30.0,
        ),
        Exercise(
            name = "Goblet Squat", muscleGroup = "Legs", equipment = "Dumbbell", isBodyweight = false,
            instructions = """
                Hold one dumbbell vertically against your chest, both hands under the top end.
                Feet shoulder-width, toes turned out slightly.
                Sit down between your heels, chest up, until your thighs are at least parallel to the floor.
                Push through your whole foot to stand up.
            """.trimIndent(),
            tips = """
                Mistakes: knees caving inward, heels lifting. Push your knees out over your toes.
                Move slowly on the way down (2–3 seconds) and breathe out on the way up.
                When the ship is rolling noticeably, skip heavy squats.
            """.trimIndent(),
            easierName = "Bodyweight Squat", harderName = "Back Squat", startWeightKg = 14.0, startReps = 12,
        ),
        Exercise(
            name = "Bodyweight Squat", muscleGroup = "Legs", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Stand with feet shoulder-width apart, arms in front of you.
                Sit down and back until your thighs are about parallel to the floor.
                Stand back up.
            """.trimIndent(),
            tips = """
                Keep your heels on the floor.
                Use a box or bench behind you to sit on if balance is hard.
            """.trimIndent(),
            harderName = "Goblet Squat", startReps = 12,
        ),
        Exercise(
            name = "Leg Press", muscleGroup = "Legs", equipment = "Machine", isBodyweight = false,
            instructions = """
                Sit in the machine with your back flat against the pad.
                Place your feet shoulder-width apart in the middle of the platform.
                Release the safety and lower the platform until your knees are at about 90 degrees.
                Push the platform back up without locking your knees.
            """.trimIndent(),
            tips = """
                Don't let your lower back lift off the seat.
                Don't lock your knees at the top.
            """.trimIndent(),
            startWeightKg = 60.0, startReps = 10,
        ),
        Exercise(
            name = "Romanian Deadlift", muscleGroup = "Legs", equipment = "Barbell", isBodyweight = false,
            instructions = """
                Stand holding the bar at hip height, hands shoulder-width apart.
                Keep a slight bend in your knees.
                Push your hips back and slide the bar down your thighs.
                Stop when you feel a strong stretch in your hamstrings, usually just below the knee.
                Drive your hips forward to stand back up.
            """.trimIndent(),
            tips = """
                Keep the bar close to your legs the whole time.
                Your back stays flat; the movement comes from your hips.
            """.trimIndent(),
            easierName = "Dumbbell Romanian Deadlift", startWeightKg = 30.0,
        ),
        Exercise(
            name = "Dumbbell Romanian Deadlift", muscleGroup = "Legs", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Stand tall holding dumbbells in front of your thighs, knees slightly bent.
                Push your hips backward as if closing a car door with your backside. The dumbbells slide down your thighs.
                Keep your back flat; stop when you feel a strong stretch in your hamstrings, usually around mid-shin.
                Drive your hips forward to stand back up.
            """.trimIndent(),
            tips = """
                Mistake: rounding your back or turning it into a squat. Knees stay almost still.
                Weight is per dumbbell.
            """.trimIndent(),
            harderName = "Romanian Deadlift", startWeightKg = 12.0, startReps = 12,
        ),
        Exercise(
            name = "Deadlift", muscleGroup = "Legs", equipment = "Barbell", isBodyweight = false,
            instructions = """
                Stand with the bar over the middle of your feet.
                Bend down and grip the bar just outside your legs.
                Drop your hips, flatten your back and pull your chest up.
                Push the floor away with your legs and stand up tall with the bar.
                Lower the bar by pushing your hips back, then bending your knees.
            """.trimIndent(),
            tips = """
                Never round your lower back.
                Keep the bar touching your legs on the way up.
                Start light; form first.
            """.trimIndent(),
            easierName = "Romanian Deadlift", startWeightKg = 40.0, startReps = 5,
        ),
        Exercise(
            name = "Walking Lunge", muscleGroup = "Legs", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Hold a dumbbell in each hand at your sides.
                Take a big step forward and lower your back knee towards the floor.
                Push through your front heel and step the back foot forward into the next lunge.
                Count one rep per leg.
            """.trimIndent(),
            tips = """
                Keep your torso upright.
                Front knee stays over your foot, not caving in.
            """.trimIndent(),
            easierName = "Bodyweight Squat", startWeightKg = 6.0, startReps = 10,
        ),
        Exercise(
            name = "Calf Raise", muscleGroup = "Legs", equipment = "Leg press or dumbbells", isBodyweight = false,
            instructions = """
                On the leg press: put the balls of your feet on the bottom edge of the platform, legs straight.
                With dumbbells: stand with the balls of your feet on a step or plate.
                Lower your heels as far as you can.
                Rise up onto your toes as high as possible and pause.
                Lower slowly.
            """.trimIndent(),
            tips = """
                Use a full range of motion; no bouncing.
            """.trimIndent(),
            startWeightKg = 20.0, startReps = 12,
        ),

        Exercise(
            name = "Glute Bridge", muscleGroup = "Legs", equipment = "Dumbbell + floor", isBodyweight = false,
            instructions = """
                Lie on your back, knees bent, feet flat and hip-width apart, dumbbell resting on your hip bones.
                Push through your heels and lift your hips until your body is straight from knees to shoulders.
                Squeeze your glutes for 1 second at the top, then lower.
            """.trimIndent(),
            tips = """
                Mistake: arching your lower back at the top. The movement comes from the hips.
                Hold the dumbbell with both hands so it can't roll.
            """.trimIndent(),
            startWeightKg = 10.0, startReps = 15,
        ),
        Exercise(
            name = "Split Squat", muscleGroup = "Legs", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Take a long step forward and stay in that position, back heel lifted. Hold dumbbells at your sides.
                Lower straight down until your back knee nearly touches the floor.
                Push through the front foot to come back up. Do all reps, then switch legs.
            """.trimIndent(),
            tips = """
                In a swell, hold the bench for balance instead of holding dumbbells.
                Keep your torso upright and the front knee over your foot.
            """.trimIndent(),
            easierName = "Bodyweight Squat", harderName = "Walking Lunge", startWeightKg = 6.0, startReps = 10,
        ),
        // Shoulders
        Exercise(
            name = "Overhead Press", muscleGroup = "Shoulders", equipment = "Barbell", isBodyweight = false,
            instructions = """
                Stand with the bar at your collarbone, hands just outside your shoulders.
                Brace your core and squeeze your glutes.
                Press the bar straight up, moving your head back slightly to let it pass.
                Lock out overhead, then lower back to your collarbone.
            """.trimIndent(),
            tips = """
                Don't lean back to push the bar up.
                The bar should finish over the middle of your feet.
            """.trimIndent(),
            easierName = "Dumbbell Shoulder Press", startWeightKg = 20.0,
        ),
        Exercise(
            name = "Dumbbell Shoulder Press", muscleGroup = "Shoulders", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Sit on a bench with an upright back support, dumbbells at shoulder height, palms forward.
                Press up until your arms are straight, without banging the dumbbells together.
                Lower back to shoulder height.
            """.trimIndent(),
            tips = """
                Keep your lower back against the support.
                Weight is per dumbbell.
                Seated is the safe choice when the ship is rolling; skip standing overhead work then.
            """.trimIndent(),
            harderName = "Overhead Press", startWeightKg = 8.0, startReps = 12,
        ),
        Exercise(
            name = "Lateral Raise", muscleGroup = "Shoulders", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Stand holding light dumbbells at your sides.
                With a slight bend in your elbows, raise your arms out to the side.
                Stop at shoulder height.
                Lower slowly.
            """.trimIndent(),
            tips = """
                Go light; this muscle is small.
                Lead with your elbows and don't shrug.
            """.trimIndent(),
            startWeightKg = 4.0, startReps = 12,
        ),

        // Arms
        Exercise(
            name = "Dumbbell Curl", muscleGroup = "Arms", equipment = "Dumbbells", isBodyweight = false,
            instructions = """
                Stand holding dumbbells at your sides, palms facing forward.
                Keep your elbows pinned to your sides and curl the weights up.
                Squeeze at the top, then lower slowly.
            """.trimIndent(),
            tips = """
                Weight is per dumbbell.
                No swinging your body to lift the weight.
            """.trimIndent(),
            startWeightKg = 8.0, startReps = 10,
        ),
        Exercise(
            name = "Bench Dip", muscleGroup = "Arms", equipment = "Bench", isBodyweight = true,
            instructions = """
                Sit on the edge of a bench, hands next to your hips.
                Slide your butt off the bench, legs out in front.
                Bend your elbows to lower yourself until they're at about 90 degrees.
                Push back up.
            """.trimIndent(),
            tips = """
                Keep your back close to the bench.
                Bend your knees to make it easier.
            """.trimIndent(),
            startReps = 8,
        ),

        Exercise(
            name = "Overhead Triceps Extension", muscleGroup = "Arms", equipment = "Dumbbell", isBodyweight = false,
            instructions = """
                Sit on a bench with back support, holding one dumbbell with both hands above your head.
                Keep your upper arms next to your ears.
                Lower the dumbbell behind your head by bending your elbows.
                Straighten your arms to lift it back up.
            """.trimIndent(),
            tips = """
                Only your forearms move.
                Keep your core tight so your lower back doesn't arch.
            """.trimIndent(),
            easierName = "Bench Dip", startWeightKg = 10.0, startReps = 10,
        ),

        // Core
        Exercise(
            name = "Dead Bug", muscleGroup = "Core", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Lie on your back, arms pointing at the ceiling, knees bent at 90° above your hips.
                Press your lower back into the floor.
                Slowly stretch out the opposite arm and leg until just above the floor, then return.
                Alternate sides. Count one rep per side.
            """.trimIndent(),
            tips = """
                If your lower back lifts off the floor, don't stretch out as far.
                Move slowly; this is about control.
            """.trimIndent(),
            harderName = "Lying Leg Raise", startReps = 8,
        ),
        Exercise(
            name = "Lying Leg Raise", muscleGroup = "Core", equipment = "Bench or floor", isBodyweight = true,
            instructions = """
                Lie on your back on a bench or the floor, hands under your hips or holding the bench behind your head.
                Keep your legs straight and together.
                Raise your legs until they point at the ceiling.
                Lower them slowly until just above the floor or bench.
            """.trimIndent(),
            tips = """
                Keep your lower back pressed down.
                Bend your knees a little to make it easier.
            """.trimIndent(),
            easierName = "Dead Bug", startReps = 8,
        ),

        Exercise(
            name = "Plank", muscleGroup = "Core", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Forearms on the floor, elbows under shoulders.
                Body in a straight line from head to heels.
                Squeeze glutes and stomach and hold.
                Log the seconds you held.
            """.trimIndent(),
            tips = """
                Stop when your hips start to sag.
                Aim for 20–45 seconds per set.
            """.trimIndent(),
            easierName = "Dead Bug", startReps = 20,
        ),
        // Mobility: timed holds, logged in seconds. They train AGI in the hero game.
        Exercise(
            name = "Deep Squat Hold", muscleGroup = "Mobility", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Feet a bit wider than your hips, toes slightly out.
                Sink as low as you can with your heels down and chest up.
                Push your knees out with your elbows and breathe slowly.
                Log the seconds you held.
            """.trimIndent(),
            tips = """
                Hold a rack or doorframe for balance, especially at sea.
                Put small plates under your heels if they lift.
            """.trimIndent(),
            startReps = 30, startSets = 2,
        ),
        Exercise(
            name = "World's Greatest Stretch", muscleGroup = "Mobility", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Step into a long lunge and put both hands on the floor inside your front foot.
                Drop your front elbow towards the floor, then rotate that arm up to the ceiling.
                Hold, breathe, then switch sides halfway through the set.
                Log the seconds for the whole set.
            """.trimIndent(),
            tips = """
                Keep the back knee straight or rest it on the floor.
                Move slowly; this is a stretch, not a race.
            """.trimIndent(),
            startReps = 40, startSets = 2,
        ),
        Exercise(
            name = "Couch Stretch", muscleGroup = "Mobility", equipment = "Bench or wall", isBodyweight = true,
            instructions = """
                Kneel with one shin up against a bench or wall behind you.
                Bring the other foot forward into a lunge.
                Squeeze your glute and lift your chest until you feel the front of the hip stretch.
                Log the seconds per side.
            """.trimIndent(),
            tips = """
                Pad the knee with a towel.
                Great after long watches standing or sitting.
            """.trimIndent(),
            startReps = 30, startSets = 2,
        ),
        Exercise(
            name = "Thoracic Rotation", muscleGroup = "Mobility", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Lie on your side with knees bent to 90° and arms straight out in front.
                Keep your knees down and open the top arm across to the other side.
                Follow your hand with your eyes and breathe out as you rotate.
                Log the seconds per side.
            """.trimIndent(),
            tips = """
                Keep the knees together on the floor.
                Only go as far as your back allows without pain.
            """.trimIndent(),
            startReps = 30, startSets = 2,
        ),
        Exercise(
            name = "Hip Flexor Stretch", muscleGroup = "Mobility", equipment = "Bodyweight", isBodyweight = true,
            instructions = """
                Half-kneel with one knee down and the other foot forward.
                Tuck your pelvis under and shift forward until the front of the back hip stretches.
                Raise the arm on the kneeling side for a deeper stretch.
                Log the seconds per side.
            """.trimIndent(),
            tips = """
                Don't arch your lower back.
                Use it as an easier version of the couch stretch.
            """.trimIndent(),
            harderName = "Couch Stretch", startReps = 30, startSets = 2,
        ),
        Exercise(
            name = "Shoulder Dislocates", muscleGroup = "Mobility", equipment = "Resistance band or towel", isBodyweight = true,
            instructions = """
                Hold a band or towel wide with straight arms in front of you.
                Slowly lift it over your head and behind you, then back.
                Keep moving smoothly for the whole set.
                Log the seconds.
            """.trimIndent(),
            tips = """
                Widen your grip if it pinches.
                A good warm-up before pressing.
            """.trimIndent(),
            startReps = 30, startSets = 2,
        ),
        // Cardio: minutes are stored as reps, the coach adds or cuts time.
        Exercise(
            name = "Treadmill Run", muscleGroup = "Cardio", equipment = "Treadmill", isBodyweight = true, isCardio = true,
            instructions = """
                Start with 3 to 5 minutes of walking to warm up.
                Increase the speed to a pace where you can still talk in short sentences.
                Land softly under your hips with short, quick steps.
                Finish with a few minutes of walking to cool down.
                Log the total minutes and the distance on the display.
            """.trimIndent(),
            tips = """
                Set the incline to 1% to make it feel more like running outside.
                Don't hold the handrails while running.
                On a moving ship, keep the speed a bit lower and use the safety clip.
            """.trimIndent(),
            startReps = 20, startSets = 1,
        ),
        Exercise(
            name = "Rowing Machine", muscleGroup = "Cardio", equipment = "Rower", isBodyweight = true,
            instructions = """
                Setup: feet strapped in with the strap over the widest part of your foot. Damper (the lever on the fan) at 3–5.
                Catch: knees bent, shins near vertical, arms straight, leaning slightly forward.
                Drive: push hard with your legs first. When the legs are almost straight, lean back slightly, then pull the handle to your lower ribs.
                Recovery: arms straight first, lean forward, then bend the knees and slide back to the catch.
                Log the total minutes and the distance on the monitor.
            """.trimIndent(),
            tips = """
                Power is about 60% legs, 20% back, 20% arms. Legs, body, arms on the way back; arms, body, legs on the way forward.
                Stroke rate (SPM): 20–24 easy, 26–30 for hard intervals. Recovery takes about twice as long as the drive.
                Mistakes: pulling with the arms before the legs are done; bending the knees before the handle has passed them.
                A higher damper does not mean a better workout.
            """.trimIndent(),
            isCardio = true, startReps = 28, startSets = 1,
        ),
        Exercise(
            name = "Zwift Bike", muscleGroup = "Cardio", equipment = "Indoor bike", isBodyweight = true, isCardio = true,
            instructions = """
                Set the saddle so your knee is slightly bent at the bottom of the pedal stroke.
                Warm up for 5 minutes at an easy effort.
                Ride a route or workout at a pace where you can still talk in short sentences.
                Cool down for a few minutes, then log the total minutes and distance from Zwift.
            """.trimIndent(),
            tips = """
                Keep a cadence of around 80 to 95 rpm.
                Use a fan and a towel; indoor riding gets hot fast.
                Zwift's structured workouts are a good way to add intervals later.
            """.trimIndent(),
            startReps = 30, startSets = 1,
        ),
        Exercise(
            name = "Crosstrainer", muscleGroup = "Cardio", equipment = "Crosstrainer", isBodyweight = true,
            instructions = """
                Stand tall, feet flat on the pedals, holding the moving handles.
                Push and pull the handles while you stride, so arms and legs both work.
                Keep your weight over your feet, not leaning on the handles.
                Log the total minutes and the distance on the display.
            """.trimIndent(),
            tips = """
                Use resistance, not only speed: if you are spinning fast with no effort, increase the level.
                In a heavy swell, hold the fixed handles and lower the speed. Balance first, speed second.
                Steady pace: you can talk in full sentences but would rather not sing.
            """.trimIndent(),
            isCardio = true, startReps = 25, startSets = 1,
        ),
    )

    /** Holds that log seconds instead of reps. */
    val TIMED = setOf(
        "Plank", "Deep Squat Hold", "World's Greatest Stretch", "Couch Stretch", "Thoracic Rotation",
        "Hip Flexor Stretch", "Shoulder Dislocates",
    )
}
