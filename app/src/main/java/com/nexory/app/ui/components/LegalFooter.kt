package com.nexory.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexory.app.NexoryConfig
import com.nexory.app.ui.theme.NexoryColors

/**
 * Подвал со ссылками на юридические документы.
 *
 * Размещается на экранах входа и регистрации. Модерация RuStore проверяет наличие
 * доступной ссылки на политику конфиденциальности именно на этапе авторизации,
 * поэтому дублируем её здесь помимо отдельного экрана согласия.
 */
@Composable
fun LegalFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Продолжая, вы соглашаетесь с документами:",
            color = NexoryColors.TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Политика конфиденциальности",
                color = NexoryColors.PrimaryBlue,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { openExternalUrl(context, NexoryConfig.PRIVACY_URL) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            )
            Text(" · ", color = NexoryColors.TextSecondary, fontSize = 12.sp)
            Text(
                "Соглашение",
                color = NexoryColors.PrimaryBlue,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable { openExternalUrl(context, NexoryConfig.TERMS_URL) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
            )
        }
    }
}
