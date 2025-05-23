package com.wukiki.givu.views.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.wukiki.givu.R
import com.wukiki.givu.ui.suit
import com.wukiki.givu.views.auth.viewmodel.AuthUiEvent
import com.wukiki.givu.views.auth.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val authUiEvent = authViewModel.authUiEvent

    LaunchedEffect(Unit) {
        authUiEvent.collect { event ->
            when (event) {
                is AuthUiEvent.LoginSuccess -> {
                    navController.popBackStack()
                }

                is AuthUiEvent.LoginFail -> {
                    Toast.makeText(context, "로그인 실패!", Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(context, "로그아웃 완료!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.ic_back),
                "뒤로 가기",
                modifier = Modifier.size(28.dp).
                clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        navController.popBackStack()
                    }
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo),
                "로고",
                modifier = Modifier.width(160.dp)
            )

            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = stringResource(R.string.text_login_description),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = suit,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Image(
                painter = painterResource(id = R.drawable.ic_kakao),
                contentDescription = "카카오 로그인",
                modifier = Modifier.fillMaxWidth()
                    .height(64.dp)
                    .clickable { authViewModel.loginWithKakao(context) }
            )
        }
    }
}