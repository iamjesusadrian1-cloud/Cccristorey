package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BrightGold
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RoyalBlue
import kotlinx.coroutines.delay

@Composable
fun SplashOverlay(
    visible: Boolean,
    onFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            startAnim = true
            delay(1300)
            onFinished()
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.86f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "splash_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        label = "splash_alpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DeepNavy, RoyalBlue, DeepNavy)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .scale(scale)
                    .alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(164.dp)
                        .border(2.5.dp, BrightGold, RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    color = PureWhite,
                    shadowElevation = 12.dp
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cristo_rey_logo),
                        contentDescription = stringResource(id = R.string.logo_content_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.displayMedium,
                    color = PureWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.welcome_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrightGold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
