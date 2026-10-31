package com.example.ui.floating

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.PandaCyanAccent
import com.example.ui.theme.PandaCyanGlow
import com.example.ui.theme.PandaPurplePrimary

@Composable
fun FloatingBubbleContent(
    isGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = modifier
            .size(62.dp)
            .shadow(12.dp, CircleShape, spotColor = PandaCyanGlow)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PandaPurplePrimary,
                        Color(0xFF1E1B33),
                        Color(0xFF0F0E17)
                    )
                )
            )
            .border(
                width = 2.5.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        PandaCyanAccent,
                        PandaPurplePrimary,
                        PandaCyanGlow,
                        PandaCyanAccent
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Inner mascot image
        Image(
            painter = painterResource(id = R.drawable.panda_icon),
            contentDescription = "Panda AI Floating Assistant",
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isGenerating) glowScale else 1f)
                .clip(CircleShape)
        )

        // Pulsing active dot when generating
        if (isGenerating) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(PandaCyanGlow)
                    .border(1.5.dp, Color.Black, CircleShape)
            )
        }
    }
}
