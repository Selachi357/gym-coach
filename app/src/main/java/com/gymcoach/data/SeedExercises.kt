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
                Sit on the bench with a dumbbell on each thigh, then lie back and bring them to chest level.
                Press the dumbbells up until your arms are straight, palms facing forward.
                Lower them slowly until your elbows are just below the bench.
                Press back up, bringing the dumbbells slightly together at the top.
            """.trimIndent(),
            tips = """
                The weight shown is per dumbbell.
                Control the way down; that's where a lot of the growth comes from.
                Keep your wrists straight, not bent back.
            """.trimIndent(),
            easierName = "Push-Up", harderName = "Bench Press", startWeightKg = 10.0,
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
                Start in a high plank, hands just wider than your shoulders.
                Keep your body in a straight line from head to heels.
                Lower your chest to just above the floor.
                Push back up to straight arms.
            """.trimIndent(),
            tips = """
                Squeeze your glutes and abs so your hips don't sag.
                Elbows point back at about 45 degrees, not straight out.
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
                Put one knee and one hand on a bench, other foot on the floor.
                Hold the dumbbell with your free hand, arm straight.
                Pull the dumbbell to your hip.
                Lower it slowly. Do all reps, then switch sides.
            """.trimIndent(),
            tips = """
                Keep your back flat and parallel to the floor.
                Pull with your elbow, not your hand.
            """.trimIndent(),
            harderName = "Barbell Row", startWeightKg = 12.0,
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
                Hold one dumbbell vertically against your chest.
                Stand with feet shoulder-width apart.
                Squat down between your knees, keeping your chest up.
                Stand back up.
            """.trimIndent(),
            tips = """
                Elbows go inside your knees at the bottom.
                Great for learning squat form.
            """.trimIndent(),
            easierName = "Bodyweight Squat", harderName = "Back Squat", startWeightKg = 12.0,
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
                Hold a dumbbell in each hand in front of your thighs.
                Keep a slight knee bend and push your hips back.
                Lower the dumbbells along your legs until you feel the hamstring stretch.
                Squeeze your glutes to stand up.
            """.trimIndent(),
            tips = """
                Weight is per dumbbell.
                Back flat, chest proud.
            """.trimIndent(),
            harderName = "Romanian Deadlift", startWeightKg = 10.0,
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
                Sit on a bench with back support, dumbbells at shoulder height.
                Press the dumbbells up until your arms are straight.
                Lower them back to shoulder height slowly.
            """.trimIndent(),
            tips = """
                Weight is per dumbbell.
                Keep your lower back against the pad.
            """.trimIndent(),
            harderName = "Overhead Press", startWeightKg = 8.0,
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
                Lie on your back with arms pointing at the ceiling and knees bent at 90 degrees above your hips.
                Press your lower back into the floor.
                Slowly lower your right arm and left leg towards the floor.
                Return, then switch sides. Count one rep per side.
            """.trimIndent(),
            tips = """
                Your lower back must stay flat on the floor.
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
            name = "Rowing Machine", muscleGroup = "Cardio", equipment = "Rower", isBodyweight = true, isCardio = true,
            instructions = """
                Strap your feet in so the strap crosses the widest part of your foot.
                Drive: push with your legs first, then lean back slightly, then pull the handle to your lower ribs.
                Recovery: arms forward first, then lean forward, then bend your knees to slide back.
                Keep a steady rhythm, about 20 to 26 strokes per minute.
                Log the total minutes and the distance on the monitor.
            """.trimIndent(),
            tips = """
                It's about 60% legs, 20% body, 20% arms.
                Damper around 3 to 5 is plenty; higher is not better.
                Keep your back straight, never hunched.
            """.trimIndent(),
            startReps = 15, startSets = 1,
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
    )
}
