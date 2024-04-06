package com.example.bakis.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.example.bakis.R
import com.example.bakis.presentation.FitnessViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun HomeScreen(viewModel: FitnessViewModel, navController: NavController) {
    val stepsToday by viewModel.stepCount.collectAsState()
    val sleepToday by viewModel.sleepCount.collectAsState()
    val listState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {

        val focusRequester = rememberActiveFocusRequester()
        val coroutineScope = rememberCoroutineScope()

        ScalingLazyColumn(
            modifier = Modifier
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(-10.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                Text(text = "Health Application", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(20.dp))
            }
            // Steps Today
            item {
                NavigationBox(
                    navController = navController,
                    navigateTo = "home",
                    titleText = "Steps today:",
                    valueText = stepsToday,
                    iconColor = Color.Green,
                    iconResId = R.drawable.footsteps,
                    iconSize = 30
                )
            }
            // Sleep Today
            item {
                NavigationBox(
                    navController = navController,
                    navigateTo = "home",
                    titleText = "Sleep today:",
                    valueText = sleepToday,
                    iconColor = Color(0xFF09bfe8),
                    iconResId = R.drawable.bed,
                    iconSize = 30
                )
            }
            // BPM
            item {
                NavigationBox(
                    navController = navController,
                    navigateTo = "bpmCalculator",
                    titleText = "Bpm:",
                    valueText = "0",
                    iconColor = Color(0xFFFF3131),
                    iconResId = R.drawable.heart_beat,
                    iconSize = 30
                )
            }
            // Calories
            item {
                NavigationBox(
                    navController = navController,
                    navigateTo = "home",
                    titleText = "Calories:",
                    valueText = stepsToday,
                    iconColor = Color(0xFFf52749),
                    iconResId = R.drawable.calories_svgrepo_com,
                    iconSize = 30
                )
            }
            item { 
                Button(onClick = {navController.navigate("bpmCalculator")},
                ) {
                    Text(text = "Measure BPM")
                }
            }
        }
    }
}

@Composable
fun NavigationBox(
    navController: NavController,
    navigateTo: String,
    iconResId: Int,
    titleText: String,
    valueText: String,
    iconColor: Color,
    iconSize: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(top = 10.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(40.dp))
            .background(Color.DarkGray)
            .clickable { navController.navigate(navigateTo) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(20.dp))
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = "Navigation icon",
                tint = iconColor,
                modifier = Modifier.size(iconSize.dp)
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(text = titleText, fontSize = 14.sp)
                Text(text = valueText, fontSize = 25.sp, color = Color.White)
            }
        }
    }
}
@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun HeartRateScreen(viewModel: FitnessViewModel, navController: NavController) {

    val listState = rememberScalingLazyListState()

    Scaffold(
        positionIndicator = {
            PositionIndicator(scalingLazyListState = listState)
        }
    ) {

        val focusRequester = rememberActiveFocusRequester()
        val coroutineScope = rememberCoroutineScope()

        LazyColumn(
            modifier = Modifier
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
            verticalArrangement = Arrangement.spacedBy(-10.dp)
        ) {
            item { 
                Text(text = "13:10", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Red)
            }
            item {
                Spacer(modifier = Modifier.height(50.dp))
                Text(text = "78", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 40.sp)
                Text(text = "Bpm", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(30.dp))
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(), 
                    horizontalArrangement = Arrangement.Center 
                ) {
                    Button(
                        modifier = Modifier.width(80.dp), 
                        onClick = { navController.navigate("bpmTest") }
                    ) {
                        Text(text = "Measure")
                    }
                }
            }
        }
    }
}

@Composable
fun HeartRateCalculator(viewModel: FitnessViewModel, navController: NavController) {
    // State to manage progress
    var progress by remember { mutableStateOf(0f) } // 0 to 1
    val animatedProgress = animateFloatAsState(targetValue = progress)

    // Observe changes to the BPM ready state and value
    val bpmReady by viewModel.bpmReady.observeAsState()
    val bpmValue by viewModel.bpmValue.observeAsState()

    BackHandler {
        navController.navigateUp() // Navigate back in the navigation stack
    }

    // Initiate data collection when the screen is first shown

    LaunchedEffect(key1 = true) {
        viewModel.collectHeartRateFor30Seconds() // Start collecting data
        // Simulate progress update alongside data collection
        for (i in 1..100) {
            delay(300) // 30 seconds total for 1 to 100, adjust as needed
            progress = i / 100f
        }
    }

    // Displaying the progress indicator
    if(progress != 1f)
    CircularProgressIndicator(progress = animatedProgress.value)

    // Display the BPM value if ready
    if (bpmReady == true) {
        Text(text = "BPM: ${bpmValue ?: "Calculating..."}")
    }
}