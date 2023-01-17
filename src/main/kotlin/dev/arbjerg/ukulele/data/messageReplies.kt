package dev.arbjerg.ukulele.data

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Duration
import javax.annotation.Generated

@Service
class MessageRepliesService(private val repo: MessageRepliesRepository) {
    private val log: Logger = LoggerFactory.getLogger(MessageRepliesService::class.java)

    private val cache: AsyncLoadingCache<Long, List<MessageReply>> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .buildAsync { id, _ ->
            repo.findAll().filter{  it.triggerId == id }.collectList().defaultIfEmpty(listOf()).toMono().toFuture() }

    fun get(triggerId: Long) = cache[triggerId].toMono().defaultIfEmpty(
        listOf()
    )
    suspend fun getAwait(guildId: Long): List<MessageReply> = get(guildId).awaitSingle()

    fun insert(triggerId: Long, msg: MessageReply){repo.save(msg).block() }

//    fun transform(guildId: Long, func: (List<MessageReplies>) -> Unit): Mono<List<MessageReplies>> = cache[guildId]
//        .toMono()
//        .map { func(it); it }
//        .flatMap { repo.save(it) }
//        .map {
//            log.info("Updated guild properties: {}", it)
//            it
//        }
//        .doOnSuccess { cache.synchronous().putAll(it.filter { it.guildId == guildId }, it) }
//
//    suspend fun transformAwait(guildId: Long, func: (List<MessageReplies>) -> Unit): MessageReplies = transform(guildId, func).awaitSingle()
}

@Table("message_replies")
data class MessageReply(
    @Id
    val id: Long,
    var triggerId: Long? = null,
    var message: String? = null
) : Persistable<Long> {
    @Transient var new: Boolean = false
    override fun getId() = id
    override fun isNew() = new
}

interface MessageRepliesRepository : ReactiveCrudRepository<MessageReply, Long>