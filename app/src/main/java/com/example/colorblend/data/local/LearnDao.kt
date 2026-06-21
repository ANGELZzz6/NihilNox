package com.example.colorblend.data.local

import androidx.room.*
import com.example.colorblend.domain.model.LearnCard
import com.example.colorblend.domain.model.LearnQuizQuestion
import com.example.colorblend.domain.model.LearnTopic
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnDao {

    // ── Topics ─────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: LearnTopic): Long

    @Update
    suspend fun updateTopic(topic: LearnTopic)

    @Query("SELECT * FROM learn_topics WHERE activo = 1 ORDER BY ultimaRepaso ASC")
    fun getAllTopics(): Flow<List<LearnTopic>>

    @Query("SELECT * FROM learn_topics WHERE id = :id")
    suspend fun getTopicById(id: Int): LearnTopic?

    @Query("UPDATE learn_topics SET activo = 0 WHERE id = :id")
    suspend fun archivarTopic(id: Int)

    // ── Cards ───────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<LearnCard>)

    @Update
    suspend fun updateCard(card: LearnCard)

    @Query("SELECT * FROM learn_cards WHERE topicId = :topicId")
    suspend fun getCardsByTopic(topicId: Int): List<LearnCard>

    @Query("""
        SELECT * FROM learn_cards 
        WHERE topicId = :topicId 
        AND proximoRepaso <= :ahora 
        ORDER BY proximoRepaso ASC
        LIMIT :limite
    """)
    suspend fun getCardsParaRepasar(topicId: Int, ahora: Long, limite: Int = 20): List<LearnCard>

    @Query("SELECT COUNT(*) FROM learn_cards WHERE topicId = :topicId")
    suspend fun contarCards(topicId: Int): Int

    @Query("""
        SELECT COUNT(*) FROM learn_cards 
        WHERE topicId = :topicId AND ultimaCalificacion >= 2
    """)
    suspend fun contarCardsDominadas(topicId: Int): Int

    // ── Quiz ────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<LearnQuizQuestion>)

    @Query("SELECT * FROM learn_quiz_questions WHERE topicId = :topicId ORDER BY RANDOM() LIMIT 5")
    suspend fun getQuizAleatorio(topicId: Int): List<LearnQuizQuestion>

    @Query("SELECT * FROM learn_quiz_questions WHERE topicId = :topicId")
    suspend fun getAllQuestions(topicId: Int): List<LearnQuizQuestion>

    @Query("DELETE FROM learn_topics WHERE id = :id")
    suspend fun deleteTopic(id: Int)

    @Query("DELETE FROM learn_cards WHERE topicId = :topicId")
    suspend fun deleteCardsByTopic(topicId: Int)

    @Query("DELETE FROM learn_quiz_questions WHERE topicId = :topicId")
    suspend fun deleteQuestionsByTopic(topicId: Int)
}
