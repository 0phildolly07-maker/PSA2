package com.philapp.psa2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Interests
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.philapp.psa2.model.ServiceType
import com.philapp.psa2.screens.SupportType

fun SupportType.cardTitle(): String = when (this) {
    SupportType.PEER_SUPPORT -> "Peer Support"
    SupportType.SOCIAL -> "Social Activities"
    SupportType.MENTAL_HEALTH -> "Mental Health"
    SupportType.RECOVERY -> "Recovery"
    SupportType.SPORT_AND_FITNESS -> "Sport & Fitness"
    SupportType.SKILL_BUILDING -> "Skill Building"
    SupportType.EMPLOYMENT -> "Employment"
    SupportType.EDUCATION -> "Education"
    SupportType.PRACTICAL -> "Practical"
    SupportType.HOUSING -> "Housing"
    SupportType.FOOD_BANKS -> "Food Banks"
    SupportType.COMMUNITY_INTEREST_GROUPS -> "Community Groups"
}

fun SupportType.icon(): ImageVector = when (this) {
    SupportType.PEER_SUPPORT -> Icons.Filled.Groups
    SupportType.SOCIAL -> Icons.Filled.Diversity3
    SupportType.MENTAL_HEALTH -> Icons.Filled.Favorite
    SupportType.RECOVERY -> Icons.Filled.Spa
    SupportType.SPORT_AND_FITNESS -> Icons.Filled.DirectionsRun
    SupportType.SKILL_BUILDING -> Icons.Filled.School
    SupportType.EMPLOYMENT -> Icons.Filled.Work
    SupportType.EDUCATION -> Icons.Filled.MenuBook
    SupportType.PRACTICAL -> Icons.Filled.Handyman
    SupportType.HOUSING -> Icons.Filled.Home
    SupportType.FOOD_BANKS -> Icons.Filled.Restaurant
    SupportType.COMMUNITY_INTEREST_GROUPS -> Icons.Filled.Interests
}

fun ServiceType.icon(): ImageVector = when (this) {
    ServiceType.PEER_SUPPORT -> Icons.Filled.Groups
    ServiceType.SOCIAL -> Icons.Filled.Diversity3
    ServiceType.MENTAL_HEALTH -> Icons.Filled.Favorite
    ServiceType.RECOVERY -> Icons.Filled.Spa
    ServiceType.SPORT_AND_FITNESS -> Icons.Filled.DirectionsRun
    ServiceType.SKILL_BUILDING -> Icons.Filled.School
    ServiceType.EMPLOYMENT -> Icons.Filled.Work
    ServiceType.EDUCATION -> Icons.Filled.MenuBook
    ServiceType.PRACTICAL -> Icons.Filled.Handyman
    ServiceType.HOUSING -> Icons.Filled.Home
    ServiceType.FOOD_BANKS -> Icons.Filled.Restaurant
    ServiceType.COMMUNITY_INTEREST_GROUPS -> Icons.Filled.Interests
}

val OtherSearchIcon: ImageVector = Icons.Filled.Search

@Composable
fun CategoryIconWell(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 56.dp,
    iconSize: Dp = 28.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun psaHomeTopAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        actionIconContentColor = MaterialTheme.colorScheme.primary,
        navigationIconContentColor = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun psaInnerTopAppBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primary,
        titleContentColor = MaterialTheme.colorScheme.onPrimary,
        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
        scrolledContainerColor = MaterialTheme.colorScheme.primary
    )
}
