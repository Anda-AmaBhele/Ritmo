package com.example.ritmo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.example.ritmo.ui.theme.RitmoSage

// Recreates the pin + soundwave mark from the Canva concept, built from
// basic shapes so it needs no image asset.
@Composable
fun RitmoLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.Bottom) {
            val heights = listOf(10, 18, 26, 34, 26, 18, 10)
            heights.forEach { h ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(4.dp)
                        .height(h.dp)
                        .background(RitmoSage, RoundedCornerShape(2.dp))
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Ritmo",
            tint = RitmoSage,
            modifier = Modifier
                .padding(bottom = 34.dp)
                .height(30.dp)
        )
    }
}