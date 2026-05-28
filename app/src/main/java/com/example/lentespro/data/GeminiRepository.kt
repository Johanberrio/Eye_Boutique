package com.example.lentespro.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class GeminiRepository(private val apiKey: String) {

    private val model = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = apiKey
    )

    // Instrucciones estrictas para evitar navegaciones automáticas molestas
    private val chat = model.startChat(
        history = listOf(
            content(role = "user") {
                text("Eres el asistente experto de LentesPro. Tienes acceso a los datos de inventario y ventas. " +
                     "REGLA DE ORO DE NAVEGACIÓN: SOLO incluye comandos como [NAV:INVENTORY], [NAV:ROUTES] o [NAV:HISTORY] " +
                     "si el usuario te pide EXPLÍCITAMENTE ir a esa sección o abrir esa pantalla. " +
                     "Si el usuario solo hace una pregunta analítica o de datos, responde con texto normal y QUÉDATE en el chat. " +
                     "Nunca uses comandos de navegación por iniciativa propia.")
            },
            content(role = "model") {
                text("Entendido. Responderé a tus dudas sobre el negocio y solo te llevaré a otras secciones si me lo pides específicamente. ¿En qué puedo ayudarte?")
            }
        )
    )

    suspend fun sendMessage(prompt: String): String {
        return try {
            val response = chat.sendMessage(prompt)
            response.text ?: "No recibí respuesta de Gemini."
        } catch (e: Exception) {
            "Error al conectar con Gemini: ${e.message}"
        }
    }
}
