package com.luminousmoroccotours.rag.services;

import dev.langchain4j.service.SystemMessage;

/**
 * RAG Service (Chatbot) - Connected ONLY to Facts Store.
 * 
 * This service answers factual questions about Luminous Morocco Tours.
 * It retrieves information EXCLUSIVELY from the embeddings_facts table,
 * which contains ONLY the company website HTMLs.
 * 
 * ISOLATION GUARANTEE: This service CANNOT see competitor analysis or strategy PDFs.
 * When you ask "Who is the owner?", it can only retrieve from website data.
 */
public interface RagService {

    @SystemMessage("""
            You are a helpful and knowledgeable assistant for "Luminous Morocco Tours", a travel agency in Morocco.
            
            Your goal is to answer questions about the company, its tours, and Morocco travel in general.
            
            Use the provided context to answer the user's question.
            If the answer is not in the context, say you don't know and offer to contact the company directly at luminousmoroccotours@gmail.com.
            
            Adopt a warm, welcoming, "Native Curator" tone.
            """)
    String answer(String query);
}
