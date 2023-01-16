package dev.arbjerg.ukulele.data

import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.core.publisher.toMono
import java.time.Duration

@Service
class MessageTriggersService(private val repo: MessageTriggersRepository) {
    private val log: Logger = LoggerFactory.getLogger(MessageTriggersService::class.java)

    private val cache: AsyncLoadingCache<Long, List<MessageTriggers>> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(10))
        .buildAsync { id, _ ->
            repo.findAll().filter{  it.guildId == id }.collectList().defaultIfEmpty(listOf()).toMono().toFuture() }

    fun get(guildId: Long, msg: String) = cache[guildId].toMono().filter { it -> it.any { Regex(it.regex!!).containsMatchIn(msg) } }.defaultIfEmpty(
        listOf()
    )
    suspend fun getAwait(guildId: Long, msg: String): List<MessageTriggers> = get(guildId, msg).awaitSingle()

//    fun transform(guildId: Long, func: (List<MessageTriggers>) -> Unit): Mono<List<MessageTriggers>> = cache[guildId]
//        .toMono()
//        .map { func(it); it }
//        .flatMap { repo.save(it) }
//        .map {
//            log.info("Updated guild properties: {}", it)
//            it
//        }
//        .doOnSuccess { cache.synchronous().putAll(it.filter { it.guildId == guildId }, it) }
//
//    suspend fun transformAwait(guildId: Long, func: (List<MessageTriggers>) -> Unit): MessageTriggers = transform(guildId, func).awaitSingle()
}

@Table("message_triggers")
data class MessageTriggers(
    @Id val id: Long,
    var guildId: Long? = null,
    var regex: String? = null
) : Persistable<Long> {
    @Transient var new: Boolean = false
    override fun getId() = guildId
    override fun isNew() = new
}

interface MessageTriggersRepository : ReactiveCrudRepository<MessageTriggers, Long>