
package com.example.camply.user.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.camply.R
import com.example.camply.shared.view.CamplyScreen
import com.example.camply.shared.view.CardLabel
import com.example.camply.shared.view.OrangeCard
import com.example.camply.shared.view.SecondaryButton
import com.example.camply.user.model.User
/**
 * Displays every campsite owner so an admin can choose whose campsites to inspect.
 *
 * Covering US 03.04.01
 */
class AdminOwnerListView {
    /**
     * Displays the owner list and the total number of registered campsites.
     *
     * @param owners Campsite owners loaded from Firestore.
     * @param campsiteCounts Number of campsites for each owner, keyed by the owner's UID.
     * @param onOwnerClick Called when an owner card is selected.
     */
    @Composable
    fun Display(
        owners: List<User>,
        campsiteCounts: Map<String, Int>,
        onOwnerClick: (User) -> Unit,
        onBack: ()-> Unit
    ) {
        val totalCampsites = campsiteCounts.values.sum()

        CamplyScreen(title = "Campsite owners") {
            Text(
                text = "$totalCampsites registered campsites",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            if (owners.isEmpty()) {
                Text(
                    text = "No campsite owners yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    owners.forEach { owner ->
                        val count = campsiteCounts[owner.getUid()] ?: 0
                        OwnerCard(
                            name = owner.getProfile().getName(),
                            campsiteCount = count,
                            onClick = { onOwnerClick(owner) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SecondaryButton(text = "Back", onClick = onBack)
        }
    }

    /**
     * Displays one campsite owner and how many campsites that owner has registered.
     *
     * @param name Name shown on the card.
     * @param campsiteCount Number of campsites registered by this owner.
     * @param onClick Called when the card is selected.
     */
    @Composable
    private fun OwnerCard(
        name: String,
        campsiteCount: Int,
        onClick: () -> Unit
    ) {
        OrangeCard(
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.profile_icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    CardLabel("Owner")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (campsiteCount == 1) "1 campsite" else "$campsiteCount campsites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = campsiteCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}