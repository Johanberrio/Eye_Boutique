package com.example.lentespro.data

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content

class GeminiRepository(private val apiKey: String) {

    // ✅ Usamos gemini-1.5-flash (o gemini-3.5-flash si está habilitado en tu región)
    // Nota: El SDK oficial actualmente usa 1.5-flash o 1.5-pro como nombres estándar.
    private val model = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = apiKey
    )

    private val chat = model.startChat(
        history = listOf(
            content(role = "user") {
                text("Eres el asistente experto de LentesPro. Ayuda al usuario con dudas de inventario y ventas. " +
                     "IMPORTANTE: SOLO usa comandos de navegación como [NAV:INVENTORY] si el usuario te pide EXPLÍCITAMENTE ir a esa sección. " +
                     "Si solo te hace una pregunta, responde normalmente sin el comando de navegación. " +
                     "Responde de forma breve.")
            },
            content(role = "model") {
                text("Entendido. Responderé a tus dudas y solo te llevaré a otras secciones si me lo pides específicamente.")
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

    /**
     * ✅ Analiza una imagen (Multimodal) para extraer datos automáticamente.
     */
    /**
     * Analiza una imagen (Multimodal) para extraer datos automáticamente.
     */
    suspend fun analyzeImage(prompt: String, bitmap: Bitmap): String {
        return try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = model.generateContent(inputContent)
            response.text ?: throw Exception("Respuesta vacía de la IA.")
        } catch (e: Exception) {
            // 🔥 AHORA SÍ lanzamos el error para que el ViewModel se entere
            throw Exception(e.message)
        }
    }
}
