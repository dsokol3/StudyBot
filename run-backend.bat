@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set GEMINI_API_KEY=AIzaSyCeG98K5FRScf8_-4oiqaCrNKC8ShKVclg
set LLM_API_KEY=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
set LLM_API_URL=https://api.groq.com/openai/v1
set LLM_MODEL=llama-3.1-8b-instant

cd /d C:\GitHub\ChatBot
"%JAVA_HOME%\bin\java.exe" -jar target\chatbot-1.0-SNAPSHOT.jar > backend.log 2>&1
