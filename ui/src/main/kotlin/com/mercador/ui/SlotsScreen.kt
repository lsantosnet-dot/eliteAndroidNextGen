package com.mercador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mercador.core.game.SlotInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun whenSaved(ms: Long): String =
    if (ms <= 0) "—" else SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(ms))

/** Jogos salvos: continuar, começar outro e apagar (com confirmação). */
@Composable
fun SlotsScreen(
    slots: List<SlotInfo>, activeId: Int?, pendingDelete: Int?, notice: String?,
    onLoad: (Int) -> Unit = {}, onDelete: (Int) -> Unit = {}, onNew: () -> Unit = {}, onBack: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(Noir.bg)) {
        Column(Modifier.fillMaxWidth().background(Noir.panel).padding(16.dp)) {
            T("JOGOS SALVOS", 22f, Noir.text, Noir.serif, spacing = 2f)
            Spacer(Modifier.height(4.dp))
            T("um mercador, um universo, um livro de contas", 11.5f, Noir.muted, Noir.mono)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(Noir.line))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (slots.isEmpty()) T("Nenhum jogo salvo ainda.", 13f, Noir.sub, Noir.serif, italic = true)
            for (s in slots) SlotCard(s, s.id == activeId, pendingDelete == s.id, { onLoad(s.id) }, { onDelete(s.id) })
        }
        notice?.let { T(it, 12f, Noir.salmon, italic = true, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlineButton("Voltar", Modifier.weight(1f), Noir.sub, onBack)
            PrimaryButton("Novo jogo", Modifier.weight(1f), onNew)
        }
    }
}

@Composable
private fun SlotCard(s: SlotInfo, active: Boolean, confirming: Boolean, onLoad: () -> Unit, onDelete: () -> Unit) {
    val border = when { !s.playable -> Noir.dangerLine; active -> Noir.amber; else -> Noir.line }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (s.playable) Noir.panel else Noir.dangerBg)
            .border(1.dp, border, RoundedCornerShape(10.dp)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(enabled = s.playable) { onLoad() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    T(s.name, 17f, if (s.playable) Noir.text else Noir.salmon, Noir.serif)
                    if (active) { Spacer(Modifier.width(8.dp)); T("EM JOGO", 9f, Noir.amber, spacing = 1.5f) }
                }
                Spacer(Modifier.height(3.dp))
                if (s.playable) T("dia ${s.day} · ${credits(s.credits)} · ${s.location}", 11.5f, Noir.muted, Noir.mono)
                else T(s.problem ?: "ilegível", 11f, Noir.salmon)
                T("salvo em ${whenSaved(s.savedAt)}", 10.5f, Noir.muted, Noir.mono)
            }
            Box(
                Modifier.clip(RoundedCornerShape(6.dp)).border(1.dp, if (confirming) Noir.red else Noir.line, RoundedCornerShape(6.dp))
                    .background(if (confirming) Noir.red else Noir.panel).clickable { onDelete() }.padding(horizontal = 10.dp, vertical = 6.dp),
            ) { T(if (confirming) "CONFIRMAR" else "APAGAR", 10f, if (confirming) Noir.text else Noir.muted, weight = FontWeight.Bold, spacing = 1f) }
        }
    }
}
