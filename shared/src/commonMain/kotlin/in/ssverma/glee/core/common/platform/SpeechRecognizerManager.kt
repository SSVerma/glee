package `in`.ssverma.glee.core.common.platform

import kotlinx.coroutines.flow.Flow

interface SpeechRecognizerManager {
    val isSupported: Boolean
    
    /**
     * Starts listening to voice input and emits recognized text dynamically.
     * The Flow completes when listening stops.
     */
    fun startListening(): Flow<String>
    
    fun stopListening()
}


