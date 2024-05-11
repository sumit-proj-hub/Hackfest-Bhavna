package com.example.bhavna.ui

import android.app.Activity
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.ui.PlayerView
import co.yml.charts.axis.AxisData
import co.yml.charts.axis.DataCategoryOptions
import co.yml.charts.common.model.Point
import co.yml.charts.ui.barchart.BarChart
import co.yml.charts.ui.barchart.models.BarChartData
import co.yml.charts.ui.barchart.models.BarChartType
import co.yml.charts.ui.barchart.models.BarData
import co.yml.charts.ui.barchart.models.BarStyle
import co.yml.charts.ui.barchart.models.SelectionHighlightData
import com.example.bhavna.ui.theme.orangeColor
import com.example.bhavna.viewmodel.MediaType
import com.example.bhavna.viewmodel.ResultScreenViewModel
import com.example.bhavna.viewmodel.ResultScreenViewModelFactory
import me.saket.telephoto.zoomable.glide.ZoomableGlideImage
import kotlin.math.roundToInt

@Composable
fun ResultScreen(
    dirName: String,
    modifier: Modifier = Modifier,
    viewModel: ResultScreenViewModel = viewModel(factory = ResultScreenViewModelFactory(dirName)),
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        val context = LocalContext.current
        DisposableEffect(Unit) {
            val window = (context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            val handler = Handler(Looper.getMainLooper())
            var repeat = true
            val runnable = object : Runnable {
                override fun run() {
                    if (repeat) {
                        insetsController.hide(WindowInsetsCompat.Type.statusBars())
                        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                        handler.postDelayed(this, 8000)
                    }
                }
            }
            handler.post(runnable)
            onDispose {
                repeat = false
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                insetsController.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (!uiState.isInitialized) {
        viewModel.initialize(context)
        return
    }

    if (isLandscape) LandscapeResultScreen(uiState, viewModel, modifier)
    else PortraitResultScreen(uiState, viewModel, modifier)
}

@Composable
fun PortraitResultScreen(
    uiState: ResultScreenState,
    viewModel: ResultScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            when (viewModel.mediaType) {
                MediaType.Image -> ZoomableGlideImage(
                    model = viewModel.mediaFile,
                    contentDescription = "Media",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(backgroundColor)
                        .weight(1f)
                )

                MediaType.Video -> AndroidView(
                    factory = {
                        PlayerView(it).apply { player = viewModel.player }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .background(backgroundColor)
                        .weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalBarChart(
                data = viewModel.getEmotionValues(uiState.videoPosition),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun LandscapeResultScreen(
    uiState: ResultScreenState,
    viewModel: ResultScreenViewModel,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val backgroundColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
            when (viewModel.mediaType) {
                MediaType.Image -> ZoomableGlideImage(
                    model = viewModel.mediaFile,
                    contentDescription = "Media",
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.large)
                        .background(backgroundColor)
                        .weight(0.6f)
                )

                MediaType.Video -> AndroidView(
                    factory = {
                        PlayerView(it).apply { player = viewModel.player }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.large)
                        .background(backgroundColor)
                        .weight(0.6f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalBarChart(
                data = viewModel.getEmotionValues(uiState.videoPosition),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
            )
        }
    }
}

@Composable
fun HorizontalBarChart(data: Map<String, Float>, modifier: Modifier = Modifier) {
    val contentColor = LocalContentColor.current
    val barData = data.toList().mapIndexed { index, (key, value) ->
        BarData(
            point = Point(value / 10, index.toFloat()),
            label = key,
            color = orangeColor
        )
    }
    val xAxisData = AxisData.Builder()
        .steps(10)
        .bottomPadding(12.dp)
        .endPadding(40.dp)
        .labelData { index -> (index * 10).toString() }
        .axisLabelColor(contentColor)
        .axisLineColor(contentColor)
        .build()
    val yAxisData = AxisData.Builder()
        .axisStepSize(30.dp)
        .steps(barData.size - 1)
        .labelAndAxisLinePadding(0.dp)
        .axisOffset(20.dp)
        .axisLabelColor(contentColor)
        .axisLineColor(contentColor)
        .setDataCategoryOptions(
            DataCategoryOptions(
                isDataCategoryInYAxis = true,
                isDataCategoryStartFromBottom = false
            )
        )
        .startDrawPadding(12.dp)
        .labelData { index -> barData[index].label }
        .build()
    val barChartData = BarChartData(
        chartData = barData,
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        barStyle = BarStyle(
            isGradientEnabled = false,
            paddingBetweenBars = 12.dp,
            barWidth = 20.dp,
            selectionHighlightData = SelectionHighlightData(
                highlightBarColor = Color.Red,
                highlightTextBackgroundColor = Color.Green,
                highlightTextOffset = 2.dp,
                highlightTextColor = LocalContentColor.current,
                popUpLabel = { x, _ -> " ${(x * 10).roundToInt()} " },
                barChartType = BarChartType.HORIZONTAL
            ),
        ),
        showYAxis = true,
        showXAxis = true,
        horizontalExtraSpace = 20.dp,
        backgroundColor = Color.Transparent,
        barChartType = BarChartType.HORIZONTAL
    )
    BarChart(
        modifier = modifier.height((69 + 32 * data.size).dp),
        barChartData = barChartData
    )
}